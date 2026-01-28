package net.nathcat.peoplecat_server.exceptions;

import net.nathcat.authcat.Exceptions.InvalidResponse;
import net.nathcat.peoplecat.protocol.ProtocolError;

/**
 * Thrown as a result of an invalid response from AuthCat
 *
 */
public class InvalidAuthCatResponse extends ProtocolError {
  private final InvalidResponse e;

  public InvalidAuthCatResponse(InvalidResponse e) {
    this.e = e;
  }

  @Override
  public String getName() {
    return "InvalidAuthCatResponse";
  }

  @Override
  public String getMessage() {
    return "Invalid response from AuthCat! " + e;
  }
}
