package net.nathcat.peoplecat.protocol.packets;

import net.nathcat.peoplecat.protocol.Packet;
import net.nathcat.peoplecat.protocol.PacketBody;
import net.nathcat.peoplecat.protocol.Type;

/**
 * <h3>Purpose</h3>
 * <p>
 * A simple ping packet to test basic protocol implementation
 * </p>
 * <h3>Payload format</h3>
 * <p>
 * No payload required
 * </p>
 * <h3>Response format</h3>
 * <p>
 * The server will reply with a ping packet
 * </p>
 *
 */
public class PingPacket extends Packet<PacketBody.Empty> {
  public PingPacket() {
    super(Type.Ping);
  }
}
