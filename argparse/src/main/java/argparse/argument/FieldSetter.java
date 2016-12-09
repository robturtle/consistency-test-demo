package argparse.argument;

import argparse.ArgumentParseException;
import argparse.type.BuildTypeException;
import argparse.type.TypeBuilderRegistry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Deque;

/**
 * Set a field for the target object using the top argument
 */
public class FieldSetter implements ArgumentConsumer {

  private final String fieldName;
  private final Validator[] validators;

  public FieldSetter(String fieldName, Validator ... validators) {
    this.fieldName = fieldName;
    this.validators = validators;
  }

  @Override
  public void consume(@NotNull Object target, @NotNull Deque<String> args) {
    if (args.isEmpty()) {
      throw new TooFewArgumentException(String.format("too few argument for field '%s'", fieldName));
    }

    String repr = args.pop();

    Class<?> type = target.getClass();
    Field field = findField(target);

    try {
      Object value = TypeBuilderRegistry.build(field.getType(), repr);
      setField(field, type, target, value);

    } catch (BuildTypeException e) {
      throw new ArgumentParseException(String.format("failed to build type '%s'", type), e);
    }

  }

  public void set(@NotNull Object target, @NotNull Object value) {
    Class<?> type = target.getClass();
    Field field = findField(target);
    setField(field, type, target, value);
  }

  private Field findField(@NotNull Object target) {
    Class<?> type = target.getClass();
    Field field = null;
    while (type != null) {
      try {
        field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        break;

      } catch (NoSuchFieldException e) {
        type = type.getSuperclass();

      } catch (ArgumentParseException e) {
        throw new ArgumentParseException(String.format("failed to build type '%s'", type), e);
      }
    }

    if (field == null) {
      throw new ArgumentParseException(String.format("target has no field named '%s'", fieldName));
    }
    return field;
  }

  private void setField(Field field, Class<?> type, @NotNull Object target, @NotNull Object value) {
    try {
      for (Validator v : validators) if (!v.validate(value)) {
        throw new ArgumentParseException("failed validator test");
      }
      field.set(target, value);

    } catch (IllegalAccessException e) {
      throw new ArgumentParseException(String.format("failed to build type '%s'", type), e);
    }
  }

}
