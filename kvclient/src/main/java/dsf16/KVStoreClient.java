package dsf16;

import argparse.ArgumentParseException;
import argparse.ArgumentParser;
import argparse.argument.ArgumentConsumer;
import argparse.argument.FieldSetter;
import argparse.option.ExclusiveOptionGroup;
import argparse.option.SingleOption;
import argparse.type.TypeBuilderRegistry;
import kvstore.Result;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static kvstore.ErrorCode.kError;
import static kvstore.ErrorCode.kSuccess;
import static kvstore.KVStore.*;

/**
 * CLI interface for KVStore Thrift service
 */
public class KVStoreClient {

  @FunctionalInterface
  private interface ResultQuery {
    Result apply(Client client) throws TException;
  }

  private static final ArgumentParser parser = new ArgumentParser();

  private static final
  Map<String, Function<KVStoreClient, ResultQuery>> operations = new HashMap<>();

  static {
    TypeBuilderRegistry.register(URI.class, s -> URI.create("my://" + s));

    Function<String, ArgumentConsumer> opSetter = operationType -> (o, args) -> {
      ResultQuery query = operations.get(operationType).apply((KVStoreClient) o);
      new FieldSetter("operation").set(o, query);
    };

    FieldSetter serverSetter = new FieldSetter("server",
      o -> ((URI)o).getHost() != null && ((URI)o).getPort() != -1);

    parser.addOption(new SingleOption("-server", serverSetter));
    parser.addOption(
      new ExclusiveOptionGroup("operations")
      .addOption(new SingleOption("-set", new FieldSetter("key"), new FieldSetter("value"), opSetter.apply("-set")))
      .addOption(new SingleOption("-get", new FieldSetter("key"), opSetter.apply("-get")))
      .addOption(new SingleOption("-del", new FieldSetter("key"), opSetter.apply("-del")))
    );

    operations.put("-get", o -> client -> client.kvget(o.key));
    operations.put("-set", o -> client -> client.kvset(o.key, o.value));
    operations.put("-del", o -> client -> client.kvdelete(o.key));
  }

  private URI server;

  private String key;

  private String value;

  private ResultQuery operation;

  public static void main(String[] args) {
    new KVStoreClient().doMain(args);
  }

  private void doMain(String[] args) {
    try {
      parser.parse(this, args);
    } catch (ArgumentParseException e) {
      System.err.println("ERROR: " + e.getMessage());
      System.exit(-1);
    }

    try {
      final int milliTimeout = 3000;
      TTransport transport = new TSocket(server.getHost(), server.getPort(), milliTimeout);
      transport.open();

      TProtocol protocol = new TBinaryProtocol(transport);
      Client client = new Client(protocol);

      Result result = operation.apply(client);
      printResult(result);

      transport.close();
    } catch (TException x) {
      System.err.println("ERROR during transmission: " + x.getMessage());
      System.exit(kError.ordinal());
    }

  }

  private void printResult(Result result) {

    if (result.error == kSuccess) {
      if (!result.value.isEmpty()) { System.out.println(result.value); }

    } else {
      System.err.println(result.errortext);
      System.exit(result.error.ordinal());
    }

  }

}
