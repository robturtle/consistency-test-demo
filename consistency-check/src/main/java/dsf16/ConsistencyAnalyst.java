package dsf16;

import ch.qos.logback.classic.Level;
import graph.CycleDetectedException;
import graph.Graph;
import graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Check consistency for RPCEntry graph. If some inconsistency is observed, CycleDetectedException
 * is to be thrown.
 */
class ConsistencyAnalyst {
  private static final Logger logger = LoggerFactory.getLogger(ConsistencyAnalyst.class);

  public final Graph<RPCEntry> precedingGraph = new Graph<>();

  public void addEntry(RPCEntry entry) {
    precedingGraph.newVertex(entry);
  }

  public void analysis() {
    ((ch.qos.logback.classic.Logger) logger).setLevel(Level.INFO);
    Map<String, Vertex<RPCEntry>> dictatorMap = new HashMap<>();

    ArrayList<Vertex<RPCEntry>> startTimeIncreasingEntries =
      new ArrayList<>(precedingGraph.getVertices());

    ArrayList<Vertex<RPCEntry>> endTimeDecreasingEntries =
      new ArrayList<>(precedingGraph.getVertices());

    startTimeIncreasingEntries.sort((a, b) -> Long.compare(a.getValue().start, b.getValue().start));
    endTimeDecreasingEntries.sort((a, b) -> Long.compare(b.getValue().end, a.getValue().end));

    logger.info("Adding time edges...");
    int timeEdgeNumber = 0;
    for (Vertex<RPCEntry> last : startTimeIncreasingEntries) {
      RPCEntry lastEntry = last.getValue();
      if (!lastEntry.isRead) { dictatorMap.put(lastEntry.value, last); }

      long rightBound = Long.MIN_VALUE;
      for (Vertex<RPCEntry> preceding : endTimeDecreasingEntries) {
        RPCEntry entry = preceding.getValue();
        if (!entry.happenBefore(lastEntry)) { continue; }
        if (rightBound < entry.end) {
          logger.debug("time edge   {} -> {}",
            entry.toString(),
            lastEntry.toString());
          ++timeEdgeNumber;
          preceding.add_edge_to(last);
          rightBound = Math.max(rightBound, entry.start);
        } else break;
      }
    }
    logger.info("{} time edges added", timeEdgeNumber);

    logger.info("Adding data edges...");
    int dataEdgeNumber = 0;
    for (Vertex<RPCEntry> readerVertex : startTimeIncreasingEntries) {
      RPCEntry readEntry = readerVertex.getValue();
      if (readEntry.isRead) {
        Vertex<RPCEntry> writerVertex = dictatorMap.get(readEntry.value);
        if (writerVertex == null) {
          logger.error("Read value with no dictator!");
          throw new CycleDetectedException();
        }
        logger.debug("data edge   {} -> {}",
          writerVertex.getValue().toString(),
          readEntry.toString());
        ++dataEdgeNumber;
        writerVertex.add_edge_to(readerVertex);
      }
    }
    logger.info("{} data edges added", dataEdgeNumber);

    // Adding hybrid edges
    logger.info("Adding hybrid edges...");
    AtomicInteger hybridEdgeNumber = new AtomicInteger();
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
              logger.debug("hybrid edge {} -> {}",
                w.getValue().toString(),
                dictator.getValue());
              hybridEdgeNumber.incrementAndGet();
              ((Graph.Vertex<RPCEntry>)w).add_hybrid_edge_to(dictator);
            }
          } else {
            logger.debug("=> do writer {}", v.getValue().toString());
            metWriters.add(v);
            writerStack.push(v);
          }
          return null;
        },
        (map, v) -> {
          if (!v.getValue().isRead) {
            if (v != writerStack.peek()) {
              logger.error("Wanna pop {}, but pop {} instead", v.getValue().toString(), writerStack.peek().getValue());
              throw new AssertionError();
            }
            logger.debug("<=out writer {}", v.getValue().toString());
            writerStack.pop();
          }
          return null;
        });
    }
    logger.info("{} hybrid edges added", hybridEdgeNumber.get());

    logger.info("Finding cycle...");
    for (Vertex<RPCEntry> vertex : precedingGraph.getVertices()) {
      ((Graph.Vertex<RPCEntry>)vertex).combine();
    }

    // Find cycle
    precedingGraph.DepthFirstTraversal((map, v) -> {
      logger.debug("checking {}...", v.getValue().toString());
      return null;
    });
  }
}
