package dsf16;

import kvstore.Result;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import java.net.URI;

import static kvstore.ErrorCode.kError;
import static kvstore.KVStore.*;

/**
 * CLI interface for KVStore Thrift service
 */
public class KVStoreClient {

  @Option(name = "-server", usage = "specify the URI of the kvstore server")
  private String address; // TODO set default from property

  private URI server;

  @Option(name = "-set", usage = "set a new KEY-VALUE pair", metaVar = "KEY VALUE",
          handler = StringArrayOptionHandler.class)
  private String[] setArgs = new String[0];

  @Option(name = "-get", usage = "get value of that KEY", metaVar = "KEY")
  private String getKey;

  @Option(name = "-del", usage = "delete that KEY", metaVar = "KEY")
  private String delKey;

  private Object operation;

  public static void main(String[] args) {
    new KVStoreClient().doMain(args);
  }

  private void validateArgs() throws IllegalArgumentException {
    if (address == null) {
      throw new IllegalArgumentException("must set server address");
    }
    server = URI.create("my://" + address);
    if (server.getHost() == null || server.getPort() == -1) {
      throw new IllegalArgumentException("bad URI format");
    }

    int n = 0;
    if (setArgs.length != 0) { operation = setArgs; n++; }
    if (getKey != null) { operation = getKey; n++; }
    if (delKey != null) { operation = delKey; n++; }
    if (n != 1) {
      throw new IllegalArgumentException("must specify only one operation");
    }

    if (operation == setArgs && setArgs.length != 2) {
      throw new IllegalArgumentException("-set expect exactly 2 arguments");
    }

  }

  private void doMain(String[] args) {

    CmdLineParser parser = new CmdLineParser(this);
    try {

      parser.parseArgument(args);
      validateArgs();

    } catch (CmdLineException | IllegalArgumentException e) {
      System.err.println("ERROR: " + e.getMessage());
      System.err.println(
        "\nkvclient [-server URI] " +
        "{ -set KEY VALUE | -get KEY | -del KEY }\n"
      );
      parser.printUsage(System.err);
      System.exit(kError.ordinal());
    }

    try {
      final int milliTimeout = 1000; // TODO set to 5000
      TTransport transport = new TSocket(server.getHost(), server.getPort(), milliTimeout);
      transport.open();

      TProtocol protocol = new TBinaryProtocol(transport);
      Client client = new Client(protocol);

      perform(client, args);

      transport.close();
    } catch (TException x) {
      x.printStackTrace();
      System.exit(kError.ordinal());
    }

  }

  private void perform(Client client, String[] args) throws TException {
    printResult(client.kvget("Yang"));
    printResult(client.kvset("Yang", "Liu"));
    printResult(client.kvget("Yang"));
    printResult(client.kvget("Yang"));
    printResult(client.kvset("Yang", "Ming"));
    printResult(client.kvdelete("Yang"));
    printResult(client.kvdelete("Yang"));
    printResult(client.kvget("Yang"));
  }

  private void printResult(Result result) {
    System.out.printf("'%s', %s, '%s'%n", result.value, result.error, result.errortext);
  }

}
