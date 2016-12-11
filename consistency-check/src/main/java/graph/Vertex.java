package graph;

import java.util.Set;

/**
 * An vertex in a graph
 */
public interface Vertex<T> {
  boolean adjacent(Vertex<T> other);

  Set<Vertex<T>> neighbours();

  void add_edge_to(Vertex<T> other);

  void remove_edge_to(Vertex<T> other);

  T getValue();

  void setValue(T value);
}
