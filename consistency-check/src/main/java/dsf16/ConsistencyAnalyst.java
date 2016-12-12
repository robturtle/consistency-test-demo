package dsf16;

import ch.qos.logback.classic.Level;
import graph.CycleDetectedException;
import graph.Graph;
import graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Check consistency for RPCEntry graph. If some inconsistency is observed, CycleDetectedException
 * is to be thrown.
 */
class ConsistencyAnalyst {
  private static final Logger logger = LoggerFactory.getLogger(ConsistencyAnalyst.class);

  private final Graph<RPCEntry> precedingGraph = new Graph<>();

  void addEntry(RPCEntry entry) {
    precedingGraph.newVertex(entry);
  }

  void analyze() {
    ((ch.qos.logback.classic.Logger) logger).setLevel(Level.INFO);
    Map<String, Vertex<RPCEntry>> dictatorMap = new HashMap<>();

    ArrayList<Vertex<RPCEntry>> startTimeIncreasingEntries =
      new ArrayList<>(precedingGraph.getVertices());

    ArrayList<Vertex<RPCEntry>> endTimeDecreasingEntries =
      new ArrayList<>(precedingGraph.getVertices());

    startTimeIncreasingEntries.sort((a, b) -> Long.compare(a.getValue().start, b.getValue().start));
    endTimeDecreasingEntries.sort((a, b) -> Long.compare(b.getValue().end, a.getValue().end));

    logger.info("Adding time edges...");
    for (Vertex<RPCEntry> last : startTimeIncreasingEntries) {
      RPCEntry lastEntry = last.getValue();
      if (!lastEntry.isRead) { dictatorMap.put(lastEntry.value, last); }

      long leftBound = Long.MIN_VALUE;
      for (Vertex<RPCEntry> preceding : endTimeDecreasingEntries) {
        RPCEntry entry = preceding.getValue();
        if (!entry.happenBefore(lastEntry)) { continue; }
        if (leftBound < entry.end) {
          preceding.add_edge_to(last);
          leftBound = Math.max(leftBound, entry.start);
        } else break;
      }
    }

    logger.info("Adding data edges...");
    for (Vertex<RPCEntry> readerVertex : startTimeIncreasingEntries) {
      RPCEntry readEntry = readerVertex.getValue();
      if (readEntry.isRead) {
        Vertex<RPCEntry> writerVertex = dictatorMap.get(readEntry.value);
        if (writerVertex == null) {
          logger.error("Read value with no dictator!");
          throw new CycleDetectedException();
        }
        writerVertex.add_edge_to(readerVertex);
      }
    }

    logger.info("Adding hybrid edges...");
    Deque<Vertex<RPCEntry>> writerStack = new ArrayDeque<>();
    Set<Vertex<RPCEntry>> metWriters = new HashSet<>();

    for (Vertex<RPCEntry> writer : startTimeIncreasingEntries) {
      if (writer.getValue().isRead || metWriters.contains(writer)) { continue; }
      precedingGraph.DepthFirstTraversal(
        (Graph.Vertex<RPCEntry>) writer,
        (map, v) -> {
          if (v.getValue().isRead) {
            Vertex<RPCEntry> dictator = dictatorMap.get(v.getValue().value);
            for (Vertex<RPCEntry> w : writerStack) if (w != dictator) {
              ((Graph.Vertex<RPCEntry>)w).add_hybrid_edge_to(dictator);
            }
          } else {
            metWriters.add(v);
            writerStack.push(v);
          }
          return null;
        },
        (map, v) -> {
          if (!v.getValue().isRead) {
            writerStack.pop();
          }
          return null;
        });
    }

    logger.info("Finding cycle...");
    for (Vertex<RPCEntry> vertex : precedingGraph.getVertices()) {
      ((Graph.Vertex<RPCEntry>)vertex).combineNormalAndHybridEdges();
    }

    // Find cycle
    precedingGraph.DepthFirstTraversal(null);
  }
}
