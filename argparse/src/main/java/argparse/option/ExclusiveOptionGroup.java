package argparse.option;

import argparse.ArgumentParseException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

/**
 * The parsing is valid if only one of its options were parsed
 */
public class ExclusiveOptionGroup extends AbstractOptionGroup {

  private Option selected;

  public ExclusiveOptionGroup(String name) {
    super(name);
  }

  @Override
  public boolean parse(@NotNull Object target, @NotNull Deque<String> args) throws ArgumentParseException {
    if (args.isEmpty()) {
      throw new AssertionError("no argument passed to the option group");
    }

    for (Option opt : opts) if (opt.parse(target, args)) {
      selected = opt;
      return true;
    }

    return false;
  }

  @Override
  public Collection<String> exampleUsage() {
    Collection<String> parts = new ArrayList<>();
    parts.add("{");
    int size = opts.size();
    for (int i = 0; i < size; i++) {
      parts.addAll(opts.get(i).exampleUsage());
      if (i != size - 1) { parts.add("|"); }
    }
    parts.add("}");
    return parts;
  }

  public Option getSelected() {
    return selected;
  }

}
