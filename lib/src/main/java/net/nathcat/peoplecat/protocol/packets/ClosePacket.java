package net.nathcat.peoplecat.protocol.packets;

import net.nathcat.peoplecat.protocol.Packet;
import net.nathcat.peoplecat.protocol.PacketBody;
import net.nathcat.peoplecat.protocol.Type;

/**
 * <h3>Purpose</h3>
 * <p>
 * Indicates that the connection is to be closed
 * </p>
 * <h3>Payload format</h3>
 * <p>
 * None
 * </p>
 * <h3>Response format</h3>
 * <p>
 * The connection should be closed upon receipt of this packet, no other
 * responses should be made
 * </p>
 *
 */
public class ClosePacket extends Packet<PacketBody.Empty> {
  public ClosePacket() {
    super(Type.Close);
  }
}
