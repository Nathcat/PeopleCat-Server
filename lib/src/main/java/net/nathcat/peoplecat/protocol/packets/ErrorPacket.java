package net.nathcat.peoplecat.protocol.packets;

import net.nathcat.peoplecat.protocol.Packet;
import net.nathcat.peoplecat.protocol.PacketBody;
import net.nathcat.peoplecat.protocol.ProtocolError;
import net.nathcat.peoplecat.protocol.Type;

/**
 * <h3>Purpose</h3>
 * <p>
 * Indicates an error has occurred, and provides details of that error.
 * </p>
 * <h3>Payload format</h3>
 * 
 * <pre>
 *   {
 *    "name": String - The name of the error which occurred.
 *    "message": String - The message provided which details the error.
 *   }
 * </pre>
 *
 * <h3>Response format</h3>
 * <p>
 * Handling this packet should be made specific to the context in which it is
 * received, and the error
 * which occurred. A common pattern might be to close the connection upon
 * receiving an error packet, by creating a new
 * request with a close packet
 * </p>
 *
 */
public class ErrorPacket extends Packet<ErrorPacket.Body> {
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
