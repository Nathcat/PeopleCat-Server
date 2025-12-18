package net.nathcat.peoplecat.database.exceptions;

public class NoMessages extends Exception {
  @Override
  public String toString() {
    return "An attempt was made to modify a message queue's contents, when the message queue has no messages!";
  }
}
