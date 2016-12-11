package graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Graph<T> {

  @SuppressWarnings("WeakerAccess")
  public static class Vertex<T> implements graph.Vertex<T> {
    private final Set<graph.Vertex<T>> adjacencies = new HashSet<>();
    private T value;

    Vertex(T value) {
      this.value = value;
    }

    @Override
    public boolean adjacent(graph.Vertex other) {
      return adjacencies.contains(other);
    }

    @Override
    public Set<graph.Vertex<T>> neighbours() {
      return adjacencies;
    }

    @Override
    public void add_edge_to(graph.Vertex<T> other) {
      adjacencies.add(other);
    }

    @Override
    public void remove_edge_to(graph.Vertex<T> other) {
      adjacencies.remove(other);
    }

    @Override
    public T getValue() {
      return value;
    }

    @Override
    public void setValue(T value) {
      this.value = value;
    }
  }

  private final List<Vertex<T>> vertices = new ArrayList<>();

  public Vertex<T> newVertex(T value) {
    Vertex<T> v = new Vertex<T>(value);
    vertices.add(v);
    return v;
  }

  public List<Vertex<T>> getVertices() {
    return vertices;
  }
}
