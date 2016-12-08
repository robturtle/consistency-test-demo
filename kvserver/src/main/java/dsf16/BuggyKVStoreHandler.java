package dsf16;

import kvstore.ErrorCode;
import kvstore.KVStore;
import kvstore.Result;
import org.apache.thrift.TException;

import java.util.Random;
import java.util.function.Function;


/**
 * A buggy server that is easy to generate observable inconsistent readings
 */
class BuggyKVStoreHandler implements KVStore.Iface {
  @FunctionalInterface
  interface ThriftRunnable {
    void run() throws TException;
  }

  private static class Delayer {
    static final Random RANDOM = new Random();

    final int minDelayMillis;
    final int maxDelayMillis;

    Function<Void, Void> task;

    private Delayer(int minDelayMillis, int maxDelayMillis) {
      this.minDelayMillis = minDelayMillis;
      this.maxDelayMillis = maxDelayMillis;
    }

    Runnable delayed(ThriftRunnable task) {
      int sleepMillis = minDelayMillis + RANDOM.nextInt(maxDelayMillis - minDelayMillis);
      return () -> {
        try {
          Thread.sleep(sleepMillis);
          task.run();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (TException e) {
          e.printStackTrace();
        }
      };
    }
  }

  private final Delayer delayer;

  private final KVStoreHandler delegate = new KVStoreHandler();

  BuggyKVStoreHandler(int minDelayMillis, int maxDelayMillis)
  {
    delayer = new Delayer(minDelayMillis, maxDelayMillis);
  }

  @Override
  public Result kvset(String key, String value) throws TException {
    new Thread(delayer.delayed(() -> delegate.kvset(key, value))).start();
    return new Result("", ErrorCode.kSuccess, "");
  }

  @Override
  public Result kvget(String key) throws TException {
    return delegate.kvget(key);
  }

  @Override
  public Result kvdelete(String key) throws TException {
    return delegate.kvdelete(key);
  }
}
