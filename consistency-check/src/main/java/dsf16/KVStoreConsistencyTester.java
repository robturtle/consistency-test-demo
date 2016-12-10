package dsf16;

import argparse.ArgumentParseException;
import argparse.ArgumentParser;
import argparse.argument.ArgumentConsumer;
import argparse.argument.FieldSetter;
import argparse.option.SingleOption;
import argparse.type.TypeBuilderRegistry;
import ch.qos.logback.classic.Level;
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

    FieldSetter serverSetter = new FieldSetter("server",
      o -> ((URI)o).getHost() != null && ((URI)o).getPort() != -1);

    parser.addOption(new SingleOption("-server", serverSetter));
    parser.addOption(new SingleOption("-conntimeout", new FieldSetter("connectionTimeoutSeconds"))).optional(true);
    parser.addOption(new SingleOption("-sendtime", new FieldSetter("sendingTimeSeconds"))).optional(true);
    parser.addOption(new SingleOption("-j", new FieldSetter("threadNumber"))).optional(true);

    ArgumentConsumer debugModeSetter = (o, args) -> new FieldSetter("isDebug").set(o, true);
    parser.addOption(new SingleOption("-debug", debugModeSetter)).optional(true);
  }

  @FunctionalInterface
  interface ClientInvocation {
    void apply(KVStore.Client client) throws TException;
  }

  private static final AtomicLong sequence = new AtomicLong();

  private static final AtomicLong writeValue = new AtomicLong();

  private final String testKey = "test";

  private URI server;

  private boolean isDebug = false;

  private int threadNumber = 20;

  private int connectionTimeoutSeconds = 10;

  private int sendingTimeSeconds = 10;

  public static void main(String[] args) {
    new KVStoreConsistencyTester().doMain(args);
  }

  private void doMain(String[] args) {
    try {
      parser.parse(this, args);
    } catch (ArgumentParseException e) {
      System.err.println(e.getMessage());
      System.out.println("USAGE: consistency_test -server HOST:PORT [-j THREAD_NUMBER] [-sendtime TIME] [-debug] [-conntimeout TIMEOUT]");
      System.out.println("  -sendtime: In seconds, how much time to send requests");
      System.out.println("  -conntimeout: In seconds, set socket connection timeout");
      System.exit(-1);
    }

    if (!isDebug) {
      ((ch.qos.logback.classic.Logger) logger).setLevel(Level.INFO);
    }

    initializeKVStore();
    sendTestingRequests();
  }

  private void sendTestingRequests() {
    ExecutorService executorService = Executors.newFixedThreadPool(threadNumber);
    Collection<Future<?>> tasks = new LinkedList<>();

    for (int i = 0; i < threadNumber; i++) {
      tasks.add(executorService.submit(() -> withClientOpened(client -> {
        while (!Thread.currentThread().isInterrupted()) {
          sendRequest(client);
        }
      })));
    }

    logger.info("Sending requests...");
    logger.info("Threads: {}, Sending Time: {} sec", threadNumber, sendingTimeSeconds);
    try {
      Thread.sleep(sendingTimeSeconds * 1000);
    } catch (InterruptedException ie) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    } finally {
      for (Future<?> task : tasks) { task.cancel(true); }
      executorService.shutdown();
      logger.info("Stop sending requests...");
    }
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
    boolean isSet = ThreadLocalRandom.current().nextBoolean();
    long before = sequence.incrementAndGet();
    Result result;
    if (isSet) {
      value = String.valueOf(writeValue.incrementAndGet());
      result = client.kvset(testKey, value);
    } else {
      result = client.kvget(testKey);
    }
    long after = sequence.incrementAndGet();
    if (result.error == kSuccess) {
      logger.debug("send: {}, receive: {}, method: {}",
        before, after,
        isSet ? "set, value: " + value : "get, value: " + result.value);
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
