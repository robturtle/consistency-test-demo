package dsf16;

/**
 * identifies one request/response event, with its
 * start/end logic time and the passing value
 */
@SuppressWarnings("WeakerAccess")
public class RPCEntry {
  public final long start, end;
  public final String value;
  public final boolean isRead;

  public RPCEntry(long start, long end, String value, boolean isRead) {
    this.start = start;
    this.end = end;
    this.value = value;
    this.isRead = isRead;
  }

  public boolean happenBefore(RPCEntry other) {
    return end < other.start;
  }

  @Override
  public String toString() {
    return String.format("[%d-%d]%c(%s)", start, end, isRead ? 'R' : 'W', value);
  }
}
