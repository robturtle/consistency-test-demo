package graph;

import java.util.*;
import java.util.function.Function;

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

  @FunctionalInterface
  public interface VertexVisitor<T, R> {
    R visit(Map<graph.Vertex<T>, VisitStatus> visitStatusMap, graph.Vertex<T> vertex);
  }

  private enum VisitStatus { Visited, Done }

  private final List<Vertex<T>> vertices = new ArrayList<>();

  public Vertex<T> newVertex(T value) {
    Vertex<T> v = new Vertex<>(value);
    vertices.add(v);
    return v;
  }

  public List<Vertex<T>> getVertices() {
    return vertices;
  }

  public <R> R DepthFirstTraversal(VertexVisitor<T, R> visitor) {
    Map<graph.Vertex<T>, VisitStatus> visitStatusMap = new HashMap<>();
    for (Vertex<T> vertex : vertices) if (!visitStatusMap.containsKey(vertex)) {
      R result = DFSIterating(visitStatusMap, visitor, vertex);
      if (result != null) { return result; }
    }
    return null;
  }

  private <R> R DFSIterating(Map<graph.Vertex<T>, VisitStatus> visitStatusMap,
                             VertexVisitor<T, R> visitor,
                             graph.Vertex<T> vertex) {
    visitStatusMap.put(vertex, VisitStatus.Visited);
    R result = visitor.visit(visitStatusMap, vertex);
    if (result != null) {
      return result;
    }
    for (graph.Vertex<T> neighbour : vertex.neighbours()) {
      if (visitStatusMap.get(neighbour) != VisitStatus.Done) {
        if (visitStatusMap.get(neighbour) == VisitStatus.Visited) {
          throw new CycleDetectedException();
        } else {
          result = DFSIterating(visitStatusMap, visitor, neighbour);
          if (result != null) {
            return result;
          }
        }
      }
    }
    visitStatusMap.put(vertex, VisitStatus.Done);
    return null;
  }
}
