package dsf16;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.ServerContext;
import org.apache.thrift.server.TServerEventHandler;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by haozhang on 10/11/16.
 */
class ServerEventHandlerForIP implements TServerEventHandler {

  private static final Logger logger = LoggerFactory.getLogger(KVStoreServer.class);

  /**
   * Called before the server begins.
   */
  @Override
  public void preServe() {

  }

  /**
   * Called when a new client has connected and is about to being processing.
   *
   * @param input
   * @param output
   */
  @Override
  public ServerContext createContext(TProtocol input, TProtocol output) {
    return null;
  }

  /**
   * Called when a client has finished request-handling to delete server
   * context.
   *
   * @param serverContext
   * @param input
   * @param output
   */
  @Override
  public void deleteContext(ServerContext serverContext, TProtocol input, TProtocol output) {

  }

  /**
   * Called when a client is about to call the processor.
   *
   * @param serverContext
   * @param inputTransport
   * @param outputTransport
   */
  @Override
  public void processContext(ServerContext serverContext, TTransport inputTransport, TTransport outputTransport) {
    TSocket socket = (TSocket) inputTransport;
    logger.info(String.format("Client address for this call is %s", socket.getSocket().getRemoteSocketAddress()));
  }

}
