package dsf16;

import kvstore.KVStore;
import kvstore.KVStore.Processor;
import org.apache.thrift.server.TServer;
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
    boolean isBuggy = false;
    // example ./kvserver 9091 buggy
    if (args.length >= 2 && args[1].equals("buggy")) {
      logger.info("Running in buggy mode");
      isBuggy = true;
    }
    handler = new KVStoreHandler(isBuggy);
    processor = new Processor<>(handler);
    final int port = args.length > 0 ? Integer.parseInt(args[0]) : 9090;
    new Thread(() -> serve(processor, port)).start();
  }

  private static void serve(Processor<KVStore.Iface> processor, int port) {
    try {
      TServerTransport transport = new TServerSocket(port); // TODO specify from command line
      TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(transport).processor(processor));

      logger.info("Starting kvstore server at {} ...", port);
      server.serve();

    } catch (TTransportException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
    }
  }
}
