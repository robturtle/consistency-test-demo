package dsf16;

import kvstore.KVStore;
import kvstore.KVStore.Processor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The server hosting "kvstore" thrift service for RPC callers
 */
public class KVStoreServer {

  private static final Logger logger = LoggerFactory.getLogger(KVStoreServer.class);

  private static Processor<KVStore.Iface> processor;

  public static void main(String[] args) {
    KVStore.Iface handler;
    // example ./kvserver 9091 buggy 1000 2000
    if (args.length == 4 && args[1].equals("buggy")) {
      handler = new BuggyKVStoreHandler(
        Integer.parseInt(args[2]),
        Integer.parseInt(args[3]));
    } else {
      handler = new KVStoreHandler();
    }
    processor = new Processor<>(handler);
    final int port = Integer.parseInt(args[0]);
    new Thread(() -> serve(processor, port)).start();
  }

  private static void serve(Processor<KVStore.Iface> processor, int port) {
    try {
      TServerTransport transport = new TServerSocket(port); // TODO specify from command line
      TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(transport).processor(processor));

      logger.info(String.format("Starting kvstore server at %d ...", port));
      server.serve();

    } catch (TTransportException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
    }
  }
}
