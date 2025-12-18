package net.nathcat.peoplecat.protocol.packets;

import net.nathcat.peoplecat.protocol.Packet;
import net.nathcat.peoplecat.protocol.Type;

public class PingPacket extends Packet {
  public PingPacket() {
    super(Type.Ping);
  }
}
