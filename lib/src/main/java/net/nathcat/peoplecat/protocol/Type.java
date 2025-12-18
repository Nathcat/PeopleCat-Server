package net.nathcat.peoplecat.protocol;

public enum Type {
  Ping(0);

  public final int typeId;

  private Type(int typeId) {
    this.typeId = typeId;
  }
}
