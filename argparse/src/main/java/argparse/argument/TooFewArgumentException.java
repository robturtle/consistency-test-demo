package argparse.argument;

/**
 * Indicates lack of argument
 */
public class TooFewArgumentException extends RuntimeException {
  public TooFewArgumentException(String msg) {
    super(msg);
  }

  public TooFewArgumentException(String what, Throwable cause) {
    super(what, cause);
  }
}
