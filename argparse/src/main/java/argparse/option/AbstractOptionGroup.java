package argparse.option;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements some general operations for an option group
 */
public abstract class AbstractOptionGroup extends AbstractOption {

  protected final List<Option> opts = new ArrayList<>();

  protected Option last;

  AbstractOptionGroup(String name) {
    super(name);
  }

  public AbstractOptionGroup addOption(Option option) {
    last = option;
    opts.add(option);
    return this;
  }

  public AbstractOptionGroup argPlaceholder(String placeholder) {
    if (last == null) {
      throw new AssertionError("There is no previous selected Option");
    }
    if (last instanceof SingleOption) {
      ((SingleOption) last).argPlaceholder(placeholder);
    }
    return this;
  }

  public AbstractOptionGroup description(String desc) {
    if (last == null) {
      throw new AssertionError("There is no previous selected Option");
    }
    if (last instanceof SingleOption) {
      ((SingleOption) last).description(desc);
    }
    return this;
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public String descriptionLine(int depth, int nameWidth) {
    int thisNameWidth = opts
      .stream()
      .map(opt -> opt.getName().length())
      .max(Integer::compare)
      .get();

    StringBuilder builder = new StringBuilder();
    if (depth >= 0) { builder.append(getName()).append('\n'); } // no display group name for root group
    for (Option opt : opts) {
      builder.append(opt.descriptionLine(depth + 1, thisNameWidth)).append('\n');
    }
    return builder.toString();
  }

}
