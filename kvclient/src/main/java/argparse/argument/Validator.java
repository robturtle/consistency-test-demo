package argparse.argument;

import argparse.ArgumentParseException;
import org.jetbrains.annotations.NotNull;

/**
 * Validate whether an argument is legal
 */
@FunctionalInterface
public interface Validator {
  boolean validate(@NotNull Object repr);
}
