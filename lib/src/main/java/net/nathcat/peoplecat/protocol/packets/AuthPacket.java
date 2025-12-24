package net.nathcat.peoplecat.protocol.packets;

import net.nathcat.peoplecat.database.types.User;
import net.nathcat.peoplecat.protocol.Packet;
import net.nathcat.peoplecat.protocol.PacketBody;
import net.nathcat.peoplecat.protocol.Type;

/**
 * <h3>Purpose</h3>
 * <p>
 * Request authentication of a set of credentials via AuthCat. If successful,
 * the active connection will be
 * updated as authenticated, and the client will be allowed access to requests
 * which require an authenticated user.
 * </p>
 * <p>
 * The server will remember the user data of the user which was authenticated,
 * if successful, and will use this data
 * in future requests which require user data
 * </p>
 * <h3>Payload format</h3>
 * 
 * <pre>
 *  {
 *    "username": String,
 *    "password": String
 *  }
 * </pre>
 *
 * <h3>Response format</h3>
 * 
 * <pre>
 *    {
 *      "id": Integer,
 *      "username": String,
 *      "email": String,
 *      "password": String,
 *      "fullName": String,
 *      "pfpPath": String,
 *      "verified": Boolean
 *    }
 * </pre>
 *
 */
public class AuthPacket extends Packet<AuthPacket.Body> {
  public static class Body extends PacketBody {
    public String username;
    public String password;

    public User user;

    public Body(String username, String password) {
      this.username = username;
      this.password = password;
    }

    public Body(User user) {
      this.user = user;
    }
  }

  public AuthPacket(String username, String password) {
    super(Type.Auth);
    body = new Body(username, password);
  }

  public AuthPacket(User user) {
    super(Type.Auth);
    body = new Body(user);
  }
}
