package argparse;

/**
 * Indicates something wrong happened when parsing arguments
 */
public class ArgumentParseException extends RuntimeException {
  public ArgumentParseException(String msg) {
    super(msg);
  }

  public ArgumentParseException(String what, Throwable cause) {
    super(what, cause);
  }
}
