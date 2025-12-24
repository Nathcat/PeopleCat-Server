package net.nathcat.peoplecat.protocol.packets;

import net.nathcat.peoplecat.protocol.Packet;
import net.nathcat.peoplecat.protocol.PacketBody;
import net.nathcat.peoplecat.protocol.Type;

/**
 * <h3>Purpose</h3>
 * <p>
 * Allows one user to add another to a chat. Note that the user which sends this
 * request to the server
 * must be a member of the chat they are trying to add a user to.
 * </p>
 * <h3>Payload format</h3>
 * 
 * <pre>
 *  {
 *    "id": Integer - The ID of the user being added to the chat.
 *    "chatId": Integer - The ID of the chat the user is being added to.
 *  }
 * </pre>
 * 
 * <h3>Response format</h3>
 * <p>
 * The server will reply with either a success packet, or an error packet.</h3>
 *
 */
public class AddToChatPacket extends Packet<AddToChatPacket.Body> {
  public static class Body extends PacketBody {
    public int id;
    public int chatId;

    public Body(int id, int chatId) {
      this.id = id;
      this.chatId = chatId;
    }
  }

  public AddToChatPacket(int id, int chatId) {
    super(Type.AddToChat);
    body = new Body(id, chatId);
  }
}
