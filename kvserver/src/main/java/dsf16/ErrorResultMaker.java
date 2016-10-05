package dsf16;

import kvstore.ErrorCode;
import kvstore.Result;
import org.slf4j.Logger;
import org.slf4j.event.Level;


/**
 * Assemble error informed {@link kvstore.Result} along with logging
 */
public class ErrorResultMaker {

  private final Logger logger;

  public final Level level;

  public final ErrorCode code;

  public final String format;

  public ErrorResultMaker(Logger logger, Level level, ErrorCode code, String format) {
    if (code == ErrorCode.kSuccess) {
      throw new AssertionError("code cannot be kSuccess");
    }
    this.logger = logger;
    this.level = level;
    this.code = code;
    this.format = format;
  }

  public Result make(Object ... args) {
    String msg = String.format(format, args);
    log(msg);
    return new Result("", code, msg);
  }

  private void log(String msg) {
    switch (level) {
      case DEBUG:
        logger.debug(msg); break;
      case ERROR:
        logger.error(msg); break;
      case TRACE:
        logger.trace(msg); break;
      case WARN:
        logger.warn(msg); break;
      case INFO:
      default:
        logger.info(msg);
    }
  }
}
