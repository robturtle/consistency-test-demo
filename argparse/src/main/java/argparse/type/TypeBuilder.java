package argparse.type;


/**
 * convert a String argument into another type instance
 */
@FunctionalInterface
public interface TypeBuilder {
  Object build(String repr) throws BuildTypeException;
}
