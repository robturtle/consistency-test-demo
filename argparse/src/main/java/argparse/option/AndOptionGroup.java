package argparse.option;

import argparse.ArgumentParseException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This option group is valid only if all required options are settled
 */
public class AndOptionGroup extends AbstractOptionGroup {

  private final Map<Option, Boolean> required = new HashMap<>();

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

  @Override
  public AndOptionGroup addOption(Option option) {
    super.addOption(option);
    optional(false);
    return this;
  }

  public void optional(boolean isOptional) {
    if (last == null) {
      throw new AssertionError("There is no previous selected Option");
    }

    if (!isOptional) {
      required.put(last, false);
    } else {
      required.remove(last);
    }
  }

  @Override
  public Collection<String> exampleUsage() {
    return opts
      .stream()
      .flatMap(opt -> {
        if (required.containsKey(opt)) {
          return opt.exampleUsage().stream();
        } else {
          Collection<String> parts = new ArrayList<>();
          parts.add("[");
          parts.addAll(opt.exampleUsage());
          parts.add("]");
          return parts.stream();
        }
      })
      .collect(Collectors.toList());
  }

  public Collection<Option> getOptions() {
    return opts;
  }

  public boolean isRequired(Option option) {
    return required.get(option);
  }

}
