package argparse.option;

import argparse.ArgumentParseException;
import argparse.argument.ArgumentConsumer;
import argparse.argument.TooFewArgumentException;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;

/**
 * An option in a command line is a predefined string pattern,
 * following by zero or more option arguments
 */
public class SingleOption extends AbstractOption {

  private final ArgumentConsumer[] consumers;

  public SingleOption(String name, ArgumentConsumer ... consumers) {
    super(name);
    this.consumers = consumers;
  }

  @Override
  public boolean parse(@NotNull Object target, @NotNull Deque<String> args) throws ArgumentParseException {

    if (args.isEmpty()) {
      throw new AssertionError(String.format("no arguments passed for option '%s'", getName()));
    }

    if (!getName().equals(args.peekFirst())) { return false; }

    args.pop();
    try {
      for (ArgumentConsumer c : consumers) {
        c.consume(target, args);
      }

    } catch (ArgumentParseException e) {
      throw new ArgumentParseException(String.format("not an valid option '%s'", getName()), e);

    } catch (TooFewArgumentException e) {
      throw new ArgumentParseException(String.format("missing argument for option '%s'", getName()), e);
    }

    return true;
  }
}
