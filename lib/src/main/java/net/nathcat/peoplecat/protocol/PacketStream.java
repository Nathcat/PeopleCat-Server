package net.nathcat.peoplecat.protocol;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public final class PacketStream<P extends Packet<? extends PacketBody>> {
  private final List<P> packets = new ArrayList<>();
  private final Class<P> c;
  public final int id;

  /**
   * Create an empty packet stream.
   */
  public PacketStream(Class<P> c, int id) {
    this.c = c;
    this.id = id;
  }

  /**
   * Create a packet stream with the given packets
   */
  public PacketStream(Class<P> c, int id, P... p) {
    this.c = c;
    this.id = id;
    for (P p2 : p) {
      add(p2);
    }
  }

  /**
   * Produce an array from this packet stream.
   *
   * @return The array of packets contained by this stream.
   */
  @SuppressWarnings("unchecked")
  public P[] toArray() {
    P[] a = packets.toArray((P[]) Array.newInstance(c, 0));
    a[a.length - 1].isFinal = true;
    return a;
  }

  /**
   * Adds a new packet to the stream. The new packet must have the same ID as the
   * ID given to the stream.
   *
   * @param p The new packet to add
   */
  public void add(P p) {
    p.id = id;
    packets.add(p);
  }

  public int length() {
    return packets.size();
  }

  public P get(int index) {
    return packets.get(index);
  }
}
