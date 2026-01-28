package net.nathcat.peoplecat_server.exceptions;

import net.nathcat.peoplecat.protocol.ProtocolError;

public class UnspecifiedException extends ProtocolError {
  private final Exception e;

  public UnspecifiedException(Exception e) {
    this.e = e;
  }

  @Override
  public String getName() {
    return "UnspecifiedException";
  }

  @Override
  public String getMessage() {
    return e.toString();
  }
}
