This parsing library is yet another command line argument parser originally for my own project's needs. I just got surprised by the existed parsing libraries which either presumes a strict format (like `CLI`) or reject to bring some complex concepts such as "mutually exclusive options" (like `args4j`).

This library brings all the features below:

- Value injection letting you use user options like local instance memebers (This gives you the flexibility of shifting a field between hardcode value and user inputs without changing the code when using it);
- Type build allowing you inject and register any type of variables;
- Customized value validations;
- Inter-option/group contrains like, "and", "or", and "exclusive or";
- Customized option cusumers to take complex actions but not just injecting values.


## Get started
The simplest usage is as follows:
```java
import argparse.ArgumentParseException;
import argparse.ArgumentParser;
import argparse.argument.FieldSetter;
import argparse.option.SingleOption;

class CLI {
  private static final ArgumentParser parser = new ArgumentParser();
  static {
    parser
      .addOption(new SingleOption("-server", new FieldSetter("server")))
      .argPlaceholder("HOST:PORT")
      .description("Specify the location of the server");

    parser
      .addOption(new SingleOption("-conntimeout", new FieldSetter("connectionTimeoutSeconds")))
      .optional(true)
      .argPlaceholder("SECS")
      .description("Set the socket connection timeout in seconds");
  }
  
  private String server;
  
  private int connectionTimeoutSeconds;
  
  public static void main(String[] args) {
    new CLI().doMain(args);
  }

  private void doMain(String[] args) {
    try {
      parser.parse(this, args);
    } catch (ArgumentParseException e) {
      System.err.println("ERROR: " + e.getMessage() + "\n");
      parser.printUsage("USAGE: command-name");
      System.exit(-1);
    }
    System.out.println("server is " + server);
    System.out.println("connectionTimeoutSeconds is " + connectionTimeoutSeconds);
  }
}
```
By default `argparse` knows how to inject String values and other primitive values. In the demo code above the `new FieldSetter("fieldName")` will inject the value into the `CLI` instance with proper type.

## Type registry
`argparse` also support injecting values of custom types. Say we want our CLI accepts a option named `server` of type `URI`, the code below will make it.
```java
import argparse.type.TypeBuilderRegistry;

class CLI {
  static {
    TypeBuilderRegistry.register(URI.class, s -> URI.create("my://" + s));
    
    parser
      .addOption(new SingleOption("-server", new FieldSetter("server")))
      .argPlaceholder("HOST:PORT")
      .description("Specify the location of the server");
  }
  
  private URI server;
  
  ...
}
```

## Validation
A `FieldSetter` can take a lambda as its validator:

```java
    FieldSetter serverSetter = new FieldSetter("server",
      o -> ((URI)o).getHost() != null && ((URI)o).getPort() != -1);

    parser
      .addOption(new SingleOption("-server", serverSetter))
      .argPlaceholder("HOST:PORT")
      .description("Specify the location of the server");
      
    parser
      .addOption(new SingleOption("-conntimeout", new FieldSetter("connectionTimeoutSeconds", o -> ((int)o) > 0)))
      .optional(true)
      .argPlaceholder("SECS")
      .description("Set the socket connection timeout in seconds");
```

## Mutally Exclusive Option Group
Some times we want a set of options being mutally exclusive. The `ExclusiveOptionGroup` we support that:

```java
    parser.addOption(
      new ExclusiveOptionGroup("operation")
        .addOption(new SingleOption("-set", new FieldSetter("key"), new FieldSetter("value")))
        .argPlaceholder("KEY VALUE")
        .description("Set a new KEY-VALUE pair onto the store")

        .addOption(new SingleOption("-get", new FieldSetter("key")))
        .argPlaceholder("KEY")
        .description("Get the value of the KEY from the store")

        .addOption(new SingleOption("-del", new FieldSetter("key")))
        .argPlaceholder("KEY")
        .description("Delete the KEY-value pair from the store")
    );
```

## Customize Field Consumers
Note for the `SingleOption("-set")`, we can pass more than one `FieldSetter`s to it. In fact, we can pass consumers other than just setting the field values. The consumer is a lambda interface with signature `Object -> void` where the object passing in is the CLI instance.

## Full examples
Some full examples in the practice can be found [here](https://github.com/robturtle/consistency-test-demo/blob/master/kvclient/src/main/java/dsf16/KVStoreClient.java) and [here](https://github.com/robturtle/consistency-test-demo/blob/master/consistency-check/src/main/java/dsf16/KVStoreConsistencyTester.java).
