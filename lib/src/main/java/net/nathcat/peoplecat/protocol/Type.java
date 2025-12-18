package net.nathcat.peoplecat.protocol;

public enum Type {
  Ping(0),
  Close(1),
  Error(2),
  Auth(3);

  public final int typeId;

  private Type(int typeId) {
    this.typeId = typeId;
  }
}
