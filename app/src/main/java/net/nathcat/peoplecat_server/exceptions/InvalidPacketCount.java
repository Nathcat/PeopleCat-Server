package net.nathcat.peoplecat_server.exceptions;

import net.nathcat.peoplecat.protocol.ProtocolError;

public class InvalidPacketCount extends ProtocolError {
  private final int count;
  private final int expected;

  public InvalidPacketCount(int count, int expected) {
    this.count = count;
    this.expected = expected;
  }

  @Override
  public String getName() {
    return "InvalidPacketCount";
  }

  @Override
  public String getMessage() {
    return "An invalid number of packets was supplied! " + expected + " packets were expected, but " + count
        + " were given.";
  }
}
