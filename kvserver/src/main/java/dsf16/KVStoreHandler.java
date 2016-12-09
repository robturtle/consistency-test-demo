package dsf16;

import kvstore.KVStore;
import kvstore.Result;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static kvstore.ErrorCode.*;
import static org.slf4j.event.Level.*;

/**
 * It implements the interface provided by the kvstore.thrift.
 * It provides a key-value store for RPC callers.
 */
class KVStoreHandler implements KVStore.Iface {

  // TODO log incoming request's IP in every function call
  private static final Logger logger = LoggerFactory.getLogger(KVStoreHandler.class);

  private static final ConcurrentMap<String, String> map = new ConcurrentHashMap<>(); // TODO wrap file store

  private static final ErrorResultMaker keyNotFound = new ErrorResultMaker(logger, WARN, kKeyNotFound, "%s: key '%s' not found");

  private static final ErrorResultMaker paramIsNull = new ErrorResultMaker(logger, ERROR, kError, "%s cannot be null");

  @Override
  public Result kvset(String key, String value) throws TException {
    logger.info("kvset: {} = {}", key, value);

    if (key == null) { return paramIsNull.make("key"); }
    if (value == null) { return paramIsNull.make("value"); }

    map.put(key, value);
    return new Result("", kSuccess, "");
  }

  @Override
  public Result kvget(String key) throws TException {
    if (key == null) { return paramIsNull.make("key"); }
    if (!map.containsKey(key)) { return keyNotFound.make("kvget", key); }

    String value = map.get(key);
    logger.info("kvget: {} = {}", key, value);
    return new Result(value, kSuccess, "");
  }

  @Override
  public Result kvdelete(String key) throws TException {
    logger.info("kvdelete: {}", key);

    if (key == null) { return paramIsNull.make("key"); }
    if (!map.containsKey(key)) { return keyNotFound.make("kvdelete", key); }

    map.remove(key);
    return new Result("", kSuccess, "");
  }

}
