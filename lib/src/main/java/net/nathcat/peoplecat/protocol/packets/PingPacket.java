package net.nathcat.peoplecat.protocol.packets;

import net.nathcat.peoplecat.protocol.Packet;
import net.nathcat.peoplecat.protocol.PacketBody;
import net.nathcat.peoplecat.protocol.Type;

public class PingPacket extends Packet<PacketBody.Empty> {
  public PingPacket() {
    super(Type.Ping);
  }
}
