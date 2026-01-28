package net.nathcat.peoplecat_server.handlers;

import net.nathcat.peoplecat.protocol.ConnectionState;
import net.nathcat.peoplecat.protocol.PacketHandler;
import net.nathcat.peoplecat.protocol.PacketStream;
import net.nathcat.peoplecat.protocol.packets.PingPacket;

public class PingHandler extends PacketHandler<PingPacket> {
  /**
   * Handles a {@link PingPacket}.
   *
   * @param packets The stream of {@link PingPacket} which was received
   * @return A new stream of {@link PingPacket} to reply with
   */
  @Override
  public PacketStream<PingPacket> handle(PacketStream<PingPacket> packets, ConnectionState state) {
    return new PacketStream<PingPacket>(PingPacket.class, packets.id, new PingPacket());
  }
}
