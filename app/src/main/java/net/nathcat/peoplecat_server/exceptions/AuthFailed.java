package net.nathcat.peoplecat_server.exceptions;

import net.nathcat.peoplecat.protocol.ProtocolError;

public class AuthFailed extends ProtocolError {
  @Override
  public String getName() {
    return "AuthFailed";
  }

  @Override
  public String getMessage() {
    return "Failed to authenticate user";
  }
}
