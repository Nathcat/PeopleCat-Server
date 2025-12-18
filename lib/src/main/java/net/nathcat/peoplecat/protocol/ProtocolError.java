package net.nathcat.peoplecat.protocol;

public abstract class ProtocolError extends Exception {
  abstract public String getName();

  abstract public String getMessage();

  @Override
  public String toString() {
    return "PeopleCat protocol error: " + getName() + " -> " + getMessage();
  }
}
