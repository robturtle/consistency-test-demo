package argparse;

import argparse.option.AndOptionGroup;
import argparse.option.Option;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * An argument parser contains a set of parsing rules.
 * It altered changes on a specific object.
 */
public class ArgumentParser {

  private final AndOptionGroup group = new AndOptionGroup("whole");

  public ArgumentParser addOption(Option opt) {
    group.addOption(opt);
    return this;
  }

  public ArgumentParser optional(boolean isOptional) {
    group.optional(isOptional);
    return this;
  }

  public ArgumentParser argPlaceholder(String placeholder) {
    group.argPlaceholder(placeholder);
    return this;
  }

  public ArgumentParser description(String desc) {
    group.description(desc);
    return this;
  }

  private void parse(@NotNull Object target, @NotNull Deque<String> args) {

    if (!group.parse(target, args)) {
      throw new ArgumentParseException(String.format("not recognized option '%s'", args.peekFirst()));
    }

    if (!args.isEmpty()) {
      throw new ArgumentParseException("too many arguments were provided");
    }

  }

  public void parse(@NotNull Object target, @NotNull String[] args) {
    Deque<String> stack = new ArrayDeque<>();
    for (int i = args.length - 1; i >= 0; i--) {
      stack.push(args[i]);
    }
    parse(target, stack);
  }

  public void printUsage(String appname) {
    System.out.print(appname + " ");
    System.out.println(String.join(" ", group.exampleUsage()));
    System.out.println(group.descriptionLine(-1, 0));
  }

}
