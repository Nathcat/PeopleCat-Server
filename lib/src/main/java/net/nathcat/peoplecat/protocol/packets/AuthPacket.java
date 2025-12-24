package net.nathcat.peoplecat.protocol.packets;

import net.nathcat.peoplecat.database.types.User;
import net.nathcat.peoplecat.protocol.Packet;
import net.nathcat.peoplecat.protocol.PacketBody;
import net.nathcat.peoplecat.protocol.Type;

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
