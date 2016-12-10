package argparse.option;

import argparse.ArgumentParseException;
import argparse.argument.ArgumentConsumer;
import argparse.argument.TooFewArgumentException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

/**
 * An option in a command line is a predefined string pattern,
 * following by zero or more option arguments
 */
public class SingleOption extends AbstractOption {

  private String descriptionStr = "";

  private String argPlaceholderStr = "";

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

  @Override
  public Collection<String> exampleUsage() {
    String example = getName();
    if (!argPlaceholderStr.isEmpty()) { example += ' ' + argPlaceholderStr; }
    return Collections.singleton(example);
  }

  @Override
  public String descriptionLine(int depth, int nameWidth) {
    String indentation = depth > 0 ? String.format("%" + depth * indentWidthPerDepth + "s", "") : "";
    String name = nameWidth <= 0 ? getName() : String.format("%-" + nameWidth + "s", getName());
    return (new StringBuilder()
      .append(indentation)
      .append(name).append(' ')
      .append(descriptionStr)
      .append('\n')
    ).toString();
  }

  public String argPlaceholder() {
    return argPlaceholderStr;
  }

  public void argPlaceholder(String placeholder) {
    argPlaceholderStr = placeholder;
  }

  public String description() {
    return descriptionStr;
  }

  public void description(String str) {
    descriptionStr = str;
  }
}
