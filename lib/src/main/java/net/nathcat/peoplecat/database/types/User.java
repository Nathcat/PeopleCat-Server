package net.nathcat.peoplecat.database.types;

public class User implements DBType {
  public int id;
  public String username;
  public String fullName;
  public String email;
  public String password;
  public String pfpPath;
  public boolean verified;
}
