package dsf16;

import kvstore.KVStore.Processor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
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

  private static Processor<KVStoreHandler> processor;

  public static void main(String[] args) {
    processor = new Processor<>(new KVStoreHandler());
    new Thread(() -> serve(processor)).start();
  }

  private static void serve(Processor<KVStoreHandler> processor) {
    try {
      TServerTransport transport = new TServerSocket(9090); // TODO read port from property file
      TServer server = new TSimpleServer(new Args(transport).processor(processor));

      logger.info("Starting kvstore server...");
      server.serve();

    } catch (TTransportException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
    }
  }
}
