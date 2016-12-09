package argparse.option;

/**
 * Default implementation of Option
 */
abstract class AbstractOption implements Option {

  private final String name;

  AbstractOption(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }
}
