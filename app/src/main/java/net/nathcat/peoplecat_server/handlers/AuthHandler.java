package net.nathcat.peoplecat_server.handlers;

import net.nathcat.peoplecat.protocol.PacketHandler;
import net.nathcat.peoplecat.protocol.PacketStream;
import net.nathcat.peoplecat.protocol.packets.AuthPacket;
import net.nathcat.peoplecat.protocol.packets.ErrorPacket;
import net.nathcat.peoplecat_server.exceptions.AuthFailed;
import net.nathcat.peoplecat_server.exceptions.InvalidAuthCatResponse;
import net.nathcat.peoplecat_server.exceptions.InvalidPacketCount;
import net.nathcat.peoplecat_server.exceptions.UnspecifiedException;
import net.nathcat.peoplecat.protocol.PacketBody;
import net.nathcat.authcat.AuthCat;
import net.nathcat.authcat.AuthResult;
import net.nathcat.authcat.Exceptions.InvalidResponse;
import net.nathcat.peoplecat.database.Utils;
import net.nathcat.peoplecat.database.types.User;
import net.nathcat.peoplecat.protocol.ConnectionState;
import net.nathcat.peoplecat.protocol.Packet;

public class AuthHandler extends PacketHandler<AuthPacket> {
  /**
   * Handles an {@link AuthPacket}. Note that only one packet is accepted, if
   * more are sent, a {@link InvalidPacketCount} will be thrown.
   *
   * @param packets The stream of {@link AuthPacket} sent to the server.
   * @return A packet stream which will contain 1 of either a {@link ErrorPacket},
   *         or an {@link AuthPacket}.
   */
  @Override
  public PacketStream<? extends Packet<? extends PacketBody>> handle(PacketStream<AuthPacket> packets,
      ConnectionState state) {
    // Ensure that the packet stream only contains one packet
    if (packets.length() != 1) {
      return new PacketStream<ErrorPacket>(ErrorPacket.class, packets.id,
          new ErrorPacket(new InvalidPacketCount(packets.length(), 1)));
    }

    // Create a new instance of the AuthCat service and get the body of the auth
    // packet
    AuthCat authCat = new AuthCat();
    AuthPacket p = packets.get(0);
    AuthPacket.Body body = p.getBody();
    AuthResult res;

    // Authenticate the user credentials with AuthCat
    try {
      res = authCat.tryLogin(body.username, body.password);
    } catch (InvalidResponse e) {
      e.printStackTrace();
      return new PacketStream<ErrorPacket>(ErrorPacket.class, packets.id,
          new ErrorPacket(new InvalidAuthCatResponse(e)));
    }

    // If successful, reply with an AuthPacket containing the full user data,
    // otherwise reply with an AuthFailed error.
    if (res.result) {
      User u;
      try {
        u = Utils.typeFromJson(res.user, User.class);
      } catch (Exception e) {
        e.printStackTrace();
        return new PacketStream<ErrorPacket>(ErrorPacket.class, packets.id,
            new ErrorPacket(new UnspecifiedException(e)));
      }

      AuthPacket response = new AuthPacket(u);
      state.user = u;
      return new PacketStream<AuthPacket>(AuthPacket.class, packets.id, response);
    } else {
      return new PacketStream<ErrorPacket>(ErrorPacket.class, packets.id, new ErrorPacket(new AuthFailed()));
    }
  }
}
