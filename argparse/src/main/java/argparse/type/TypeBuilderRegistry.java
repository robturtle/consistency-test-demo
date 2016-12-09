package argparse.type;

import argparse.ArgumentParseException;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Type builders will be registered here
 */
public class TypeBuilderRegistry {
  private static final Map<Class<?>, TypeBuilder> registry = new HashMap<>();

  static {
    registry.put(int.class, Integer::parseInt);
    registry.put(byte.class, Byte::parseByte);
    registry.put(short.class, Short::parseShort);
    registry.put(long.class, Long::parseLong);
    registry.put(float.class, Float::parseFloat);
    registry.put(double.class, Double::parseDouble);
    registry.put(boolean.class, Boolean::parseBoolean);
    registry.put(char.class, s -> {
      if (s == null || s.length() != 0) {
        throw new BuildTypeException("requiring one char but got a string instead");
      }
      return s.charAt(0);
    });
    registry.put(String.class, s -> s);
  }

  public static Object build(Class<?> type, @NotNull String repr) throws BuildTypeException {
    if (!registry.containsKey(type)) {
      throw new BuildTypeException(String.format("no TypeBuilder registered for '%s'", type));
    }

    try {
      return registry.get(type).build(repr);

    } catch (NumberFormatException e) {
      throw new BuildTypeException(String.format("not a valid '%s' type", type));
    }
  }

  public static void register(@NotNull Class<?> type, @NotNull TypeBuilder builder) {
    registry.put(type, builder);
  }

}
