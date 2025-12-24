package net.nathcat.peoplecat.protocol.packets;

import net.nathcat.peoplecat.protocol.Packet;
import net.nathcat.peoplecat.protocol.PacketBody;
import net.nathcat.peoplecat.protocol.Type;

public class ClosePacket extends Packet<PacketBody.Empty> {
  public ClosePacket() {
    super(Type.Close);
  }
}
