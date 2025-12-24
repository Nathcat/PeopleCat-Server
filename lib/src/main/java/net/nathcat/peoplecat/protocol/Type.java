package net.nathcat.peoplecat.protocol;

public enum Type {
  Ping(1),
  Close(4),
  Error(0),
  Auth(2),
  AddToChat(21),
  Success(24);

  public final int typeId;

  private Type(int typeId) {
    this.typeId = typeId;
  }
}
