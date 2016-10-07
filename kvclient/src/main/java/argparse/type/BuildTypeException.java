package argparse.type;

/**
 * Indicates failures while build a type inside TypeBuilder
 */
public class BuildTypeException extends RuntimeException {
  public BuildTypeException(String msg) {
    super(msg);
  }

  public BuildTypeException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
