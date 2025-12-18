package net.nathcat.peoplecat.database.types;

import java.util.Date;

import net.nathcat.peoplecat.database.DBConfig;

public class Message implements DBType {
  public int chatId;
  public int senderId;
  public long timeSent;
  public String content;

  public boolean expired() {
    return (new Date().getTime() - timeSent) >= DBConfig.instance.messageExpiryTime;
  }
}
