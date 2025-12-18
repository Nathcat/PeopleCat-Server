package net.nathcat.peoplecat.protocol.packets;

import net.nathcat.peoplecat.protocol.Packet;
import net.nathcat.peoplecat.protocol.PacketBody;
import net.nathcat.peoplecat.protocol.ProtocolError;
import net.nathcat.peoplecat.protocol.Type;

public class ErrorPacket extends Packet {
  public static class Body extends PacketBody {
    public final String name;
    public final String message;

    public Body(String n, String m) {
      name = n;
      message = m;
    }
  }

  public ErrorPacket(ProtocolError e) {
    super(Type.Error);
    body = new Body(e.getName(), e.getMessage());
  }
}
