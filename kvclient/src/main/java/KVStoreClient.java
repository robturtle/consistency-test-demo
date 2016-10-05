import kvstore.KVStore;
import kvstore.Result;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import static kvstore.ErrorCode.kError;
import static kvstore.KVStore.*;

/**
 * CLI interface for KVStore Thrift service
 */
public class KVStoreClient {
  public static void main(String[] args) {
    // TODO read host & port from property file
    final String host = "localhost";
    final int port = 9090;

    try {
      TTransport transport = new TSocket(host, port);
      transport.open();

      TProtocol protocol = new TBinaryProtocol(transport);
      Client client = new Client(protocol);

      perform(client, args);

      transport.close();
    } catch (TException x) {
      System.err.println(x.getMessage());
      System.exit(kError.ordinal());
    }
  }

  private static void perform(Client client, String[] args) throws TException {
    printResult(client.kvget("Yang"));
    printResult(client.kvset("Yang", "Liu"));
    printResult(client.kvget("Yang"));
    printResult(client.kvget("Yang"));
    printResult(client.kvset("Yang", "Ming"));
    printResult(client.kvdelete("Yang"));
    printResult(client.kvdelete("Yang"));
    printResult(client.kvget("Yang"));
  }

  private static void printResult(Result result) {
    System.out.printf("'%s', %s, '%s'%n", result.value, result.error, result.errortext);
  }

}
