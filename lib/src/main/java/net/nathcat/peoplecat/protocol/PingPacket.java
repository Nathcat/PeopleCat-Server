package net.nathcat.peoplecat.protocol;

public class PingPacket extends Packet {
  public PingPacket() {
    super(Type.Ping);
  }
}
