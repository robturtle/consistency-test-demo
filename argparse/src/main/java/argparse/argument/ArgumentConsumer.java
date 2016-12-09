package argparse.argument;

import argparse.ArgumentParseException;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;

/**
 * consume an argument and perform an operation
 */
@FunctionalInterface
public interface ArgumentConsumer {

  void consume(@NotNull Object target, @NotNull Deque<String> args) throws ArgumentParseException;

}
