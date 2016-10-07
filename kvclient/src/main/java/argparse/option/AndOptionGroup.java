package argparse.option;

import argparse.ArgumentParseException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * This option group is valid only if all required options are settled
 */
public class AndOptionGroup extends AbstractOption {

  private final List<Option> opts = new ArrayList<>();

  private final Map<Option, Boolean> required = new HashMap<>();

  private Option last;

  public AndOptionGroup(String name) {
    super(name);
  }

  @Override
  public boolean parse(@NotNull Object target, @NotNull Deque<String> args) throws ArgumentParseException {
    Set<Option> unmet = new HashSet<>(opts);
    Map<Option, Boolean> settled = new HashMap<>(required);

    while (!args.isEmpty()) {

      boolean processed = false;

      for (Option opt : unmet) if (opt.parse(target, args)) {
          settled.put(opt, true);
          unmet.remove(opt);
          processed = true;
          break;
      }

      if (!processed) {
        throw new ArgumentParseException(String.format("can't recognize option '%s'", args.peekFirst()));
      }
    }

    for (Option opt : settled.keySet()) if (!settled.get(opt)) {
      throw new ArgumentParseException(String.format("missing required option '%s'", opt.getName()));
    }

    return true;
  }

  public AndOptionGroup addOption(Option option) {
    last = option;
    opts.add(option);
    require(true);
    return this;
  }

  public AndOptionGroup require(boolean isRequired) {
    if (last == null) {
      throw new AssertionError("There is no previous selected Option");
    }

    if (isRequired) {
      required.put(last, false);
    } else {
      required.remove(last);
    }

    return this;
  }

}
