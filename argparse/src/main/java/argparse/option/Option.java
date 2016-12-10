package argparse.option;

import argparse.ArgumentParseException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Deque;

/**
 * The logic parsing unit in a command line which convey a meaning in the whole
 */
public interface Option {

  /// format of usage information
  int indentWidthPerDepth = 2;

  /**
   * Parse and consume passing arguments if the pattern matched.
   * Also allows actions take place when consuming arguments.
   *
   * @param target The destination where the parser record their changes. Typically setting a field in it.
   * @param args The remaining argument stack.
   * @return Whether the parsing has took place.
   * @throws ArgumentParseException If encountered unexpected argument format.
   */
  boolean parse(@NotNull Object target, @NotNull Deque<String> args) throws ArgumentParseException;

  /**
   * @return The name of the option. If it's a SingleOption, the name is the pattern, "-a" for instance.
   */
  String getName(); // TODO use Collection<String> to support alias

  /**
   * @return The example usage of an option. e.g. ["-n RUN_TIME", "[-help]"]
   */
  Collection<String> exampleUsage();

  /**
   * @param indent The indentation of this line
   * @param nameWidth The total width of the name column
   * @return The description line of this option. e.g. "-server Specify the server location"
   */
  String descriptionLine(int indent, int nameWidth);

}
