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

    // Adding time edges
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
          preceding.add_edge_to(last);
          rightBound = Math.max(rightBound, entry.start);
        } else break;
      }
    }

    // Adding data edges
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
        writerVertex.add_edge_to(readerVertex);
      }
    }

    // Adding hybrid edges
    for (Vertex<RPCEntry> writer : startTimeIncreasingEntries) {
      if (writer.getValue().isRead) { continue; }
      for (Vertex<RPCEntry> reader : startTimeIncreasingEntries) if (reader.getValue().isRead) {
        Boolean connected = precedingGraph.<Boolean>DepthFirstTraversal(
          (Graph.Vertex<RPCEntry>) writer, (map, v) -> (v == reader) ? Boolean.TRUE : null);

        if (connected != null) {
          Vertex<RPCEntry> dictator = dictatorMap.get(reader.getValue().value);
          if (dictator == writer) { continue; }
          logger.debug("hybrid edge {} -> {}",
            writer.getValue().toString(),
            dictator.getValue().toString());
          ((Graph.Vertex<RPCEntry>)writer).add_hybrid_edge_to(dictator);
        }
      }
    }

    for (Vertex<RPCEntry> vertex : precedingGraph.getVertices()) {
      ((Graph.Vertex<RPCEntry>)vertex).combine();
    }

    // Find cycle
    logger.info("Finding cycle...");
    precedingGraph.DepthFirstTraversal((map, v) -> {
      logger.debug("checking {}...", v.getValue().toString());
      return null;
    });
  }
}
