package argparse.option;

import argparse.ArgumentParseException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The parsing is valid if only one of its options were parsed
 */
public class ExclusiveOptionGroup extends AbstractOption {

  private final List<Option> opts = new ArrayList<>();

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

  public ExclusiveOptionGroup addOption(Option option) {
    opts.add(option);
    return this;
  }

  public Option getSelected() {
    return selected;
  }

}
