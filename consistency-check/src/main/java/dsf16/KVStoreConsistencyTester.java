package dsf16;

import argparse.ArgumentParseException;
import argparse.ArgumentParser;
import argparse.argument.FieldSetter;
import argparse.option.SingleOption;
import argparse.type.TypeBuilderRegistry;
import ch.qos.logback.classic.Level;
import graph.CycleDetectedException;
import kvstore.KVStore;
import kvstore.Result;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static kvstore.ErrorCode.kSuccess;

/**
 * Launch a test to determine whether the KVStoreServer
 * is atomic consistent
 */

@SuppressWarnings("FieldCanBeLocal")
public class KVStoreConsistencyTester {
  private static final Logger logger = LoggerFactory.getLogger(KVStoreConsistencyTester.class);

  private static final ArgumentParser parser = new ArgumentParser();
  static {
    TypeBuilderRegistry.register(URI.class, s -> URI.create("my://" + s));
    TypeBuilderRegistry.register(CountDownLatch.class, s -> new CountDownLatch(Integer.parseInt(s)));

    FieldSetter serverSetter = new FieldSetter("server",
      o -> ((URI)o).getHost() != null && ((URI)o).getPort() != -1);

    parser
      .addOption(new SingleOption("-server", serverSetter))
      .argPlaceholder("HOST:PORT")
      .description("Specify the location of the server");

    parser
      .addOption(new SingleOption("-conntimeout", new FieldSetter("connectionTimeoutSeconds")))
      .optional(true)
      .argPlaceholder("SECS")
      .description("Specify socket connection timeout in seconds");

    parser
      .addOption(new SingleOption("-sendtime", new FieldSetter("sendingTimeSeconds")))
      .optional(true)
      .argPlaceholder("SECS").description("Set max time duration of request sending period in seconds");

    // TODO 用 AtomicLong 来保存 requestNumber, 条件变量等待它变成 0
    parser
      .addOption(new SingleOption("-n", new FieldSetter("remainingRequestNumber")))
      .optional(true)
      .argPlaceholder("NUM").description("Set max count of sending requests");

    parser.addOption(new SingleOption("-j", new FieldSetter("threadNumber")))
      .optional(true)
      .argPlaceholder("THREAD_NUM").description("Set the number of threads");

    parser.addOption(new SingleOption("-timeout", new FieldSetter("programTimeoutSeconds")))
      .optional(true)
      .argPlaceholder("SECS").description("Set the running time of the whole program");

    parser.addOption(new SingleOption("-no-fastcheck", new FieldSetter("useFastChecker").set(false)))
      .optional(true)
      .description("Do not use fast checker (A single thread tester testing if it reads most recent writes)");

    parser.addOption(new SingleOption("-debug", new FieldSetter("isDebug").set(true)))
      .optional(true)
      .description("Show debug logs");
  }

  @FunctionalInterface
  interface ClientInvocation {
    void apply(KVStore.Client client) throws TException;
  }

  private static final AtomicLong sequence = new AtomicLong();

  private static final AtomicLong writeValue = new AtomicLong();

  private final ExecutorService requestSenderTimeoutStopper = Executors.newSingleThreadExecutor();

  private final ExecutorService programRunningTimeoutStopper = Executors.newSingleThreadExecutor();

  private final String testKey = "yangliu";

  private URI server;

  private boolean isDebug = false;

  private CountDownLatch remainingRequestNumber = new CountDownLatch(25000);

  private int threadNumber = 20;

  private int connectionTimeoutSeconds = 10;

  private int sendingTimeSeconds = 10;

  private int programTimeoutSeconds = 50;

  private boolean useFastChecker = true;

  private final ConsistencyAnalyst analyst = new ConsistencyAnalyst();
  private final ReentrantLock addingEntry = new ReentrantLock();

  public static void main(String[] args) {
    new KVStoreConsistencyTester().doMain(args);
  }

  private void doMain(String[] args) {
    try {
      parser.parse(this, args);
    } catch (ArgumentParseException e) {
      System.err.println("ERROR: " + e.getMessage() + "\n");
      parser.printUsage("USAGE: consistency-tester");
      System.exit(-1);
    }

    if (!isDebug) {
      ((ch.qos.logback.classic.Logger) logger).setLevel(Level.INFO);
    }

    programRunningTimeoutStopper.submit(() -> {
      try {
        logger.info("Set program timeout = {} sec", programTimeoutSeconds);
        Thread.sleep(programTimeoutSeconds * 1000);
        logger.info("program timed out");
        logger.info("No inconsistency caught...");
        System.exit(0);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    });

    initializeKVStore();

    sendTestingRequests();
    try {
      remainingRequestNumber.await();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
    requestSenderTimeoutStopper.shutdownNow();

    logger.info("Start analysing...");
    Future<?> fastChecker = null;
    if (useFastChecker) {
      logger.info("Starting fast checker...");
      fastChecker = Executors.newSingleThreadExecutor().submit(this::fastCheck);
    }
    try {
      analyst.analysis();
      logger.info("No inconsistency detected...");
      logger.info("To try harder please check -j and -n options");
      System.exit(0);
    } catch (CycleDetectedException e) {
      logger.info("Inconsistency detected!");
      System.exit(1);
    } finally {
      if (fastChecker != null) {
        fastChecker.cancel(true);
      }
    }
  }

  private void fastCheck() {
    final String key = "fastYang";
    withClientOpened(client -> {
      long v = 0;
      while (!Thread.currentThread().isInterrupted()) {
        Result result = client.kvset(key, String.valueOf(v++));
        if (result.error == kSuccess) {
          result = client.kvget(key);
          if (!result.value.equals(String.valueOf(v - 1))) {
            logger.info("Inconsistency caught by fast checker");
            logger.info("set: {}, get: {}", v - 1, result.value);
            System.exit(1);
          }
        }
      }
    });
  }

  private void sendTestingRequests() {
    ExecutorService executorService = Executors.newFixedThreadPool(threadNumber);
    Collection<Future<?>> tasks = new LinkedList<>();

    logger.info("Sending requests...");
    logger.info("Request number: {}", remainingRequestNumber.getCount());
    logger.info("Threads: {}, Sending Timeout: {} sec", threadNumber, sendingTimeSeconds);

    for (int i = 0; i < threadNumber; i++) {
      tasks.add(executorService.submit(() -> withClientOpened(client -> {
        while (!Thread.currentThread().isInterrupted() && remainingRequestNumber.getCount() != 0) {
          remainingRequestNumber.countDown();
          sendRequest(client);
          /*int millis = ThreadLocalRandom.current().nextInt(10);
          try {
            Thread.sleep(millis);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
          }*/
        }
      })));
    }

    requestSenderTimeoutStopper.submit(() -> {
      try {
        Thread.sleep(sendingTimeSeconds * 1000);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      } finally {
        while (remainingRequestNumber.getCount() > 0) remainingRequestNumber.countDown(); // TODO use condition var
        for (Future<?> task : tasks) {
          task.cancel(true);
        }
        executorService.shutdown();
        logger.info("Stop sending requests...");
      }
    });
  }

  private void initializeKVStore() {
    final String initValue = "initValue";

    Future<?> initialized = Executors.newSingleThreadExecutor().submit(() -> {
      logger.info("Setting initial value for testing keys...");
      withClientOpened(client -> {
        client.kvset(testKey, initValue);
        Result result;
        do {
          result = client.kvget(testKey);
        } while (!initValue.equals(result.value) && !Thread.currentThread().isInterrupted());
      });
      logger.info("initialization completed");
      analyst.precedingGraph.newVertex(new RPCEntry(0, 0, initValue, false));
    });

    try {
      initialized.get(connectionTimeoutSeconds + 2, TimeUnit.SECONDS);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException|TimeoutException e) {
      logger.error("Failed to set up initial values. abort.");
      initialized.cancel(true);
      System.exit(2);
    }
  }

  private void sendRequest(KVStore.Client client) throws TException {
    String value = "";
    boolean isRead = ThreadLocalRandom.current().nextBoolean();
    long before = sequence.incrementAndGet();
    Result result;
    if (isRead) {
      result = client.kvget(testKey);
    } else {
      value = String.valueOf(writeValue.incrementAndGet());
      result = client.kvset(testKey, value);
    }
    long after = sequence.incrementAndGet();
    if (result.error == kSuccess) {
      value = isRead ? result.value : value;
      logger.debug("send: {}, receive: {}, method: {}, value: {}",
        before, after, isRead ? "get" : "set", value);
      RPCEntry entry = new RPCEntry(before, after, value, isRead);
      addingEntry.lock();
      analyst.precedingGraph.newVertex(entry);
      addingEntry.unlock();
    } else {
      logger.warn("bad Result received");
    }
  }

  private void withClientOpened(ClientInvocation invocation) {
    try {
      TTransport transport = new TSocket(server.getHost(), server.getPort(), connectionTimeoutSeconds * 1000);
      transport.open();

      TProtocol protocol = new TBinaryProtocol(transport);
      KVStore.Client client = new KVStore.Client(protocol);

      invocation.apply(client);

      transport.close();
    } catch (TException x) {
      logger.error(x.getMessage());
      x.printStackTrace();
      System.exit(2);
    }
  }
}
