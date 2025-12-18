package net.nathcat.peoplecat.protocol;

/**
 * Specifies how a specific packet type should be handled
 *
 */
public abstract class PacketHandler<P extends Packet> {
  /**
   * Handle a packet stream of the type specified by this handler
   *
   * @param packets The stream of packets which were received.
   * @return The stream of packets to be sent back to the client
   */
  abstract public PacketStream<? extends Packet> handle(PacketStream<P> packets);
}
