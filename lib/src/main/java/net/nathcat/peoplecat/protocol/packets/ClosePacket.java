package net.nathcat.peoplecat.protocol.packets;

import net.nathcat.peoplecat.protocol.Packet;
import net.nathcat.peoplecat.protocol.Type;

public class ClosePacket extends Packet {
  public ClosePacket() {
    super(Type.Close);
  }
}
