package net.nathcat.peoplecat.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;

import net.nathcat.peoplecat.database.exceptions.NoMessages;
import net.nathcat.peoplecat.database.types.Message;

/**
 * Encapsulates a JSON data structure which contains all the messages for a
 * given chat. Upon loading, this class will determine the messages which have
 * passed the expiry time, and will delete them.
 */
public class MessageQueue {
  /**
   * The ID of the chat this queue is linked to.
   */
  protected int chat;
  /**
   * The list of messages in this chat
   */
  protected Message[] messages;

  protected MessageQueue() {
  }

  public int getChatId() {
    return chat;
  }

  public Message[] getMessages() {
    return messages;
  }

  private List<Message> getMessageList() {
    return Arrays.asList(messages);
  }

  private void setMessages(List<Message> m) {
    messages = m.toArray(new Message[0]);
  }

  /**
   * Save this message queue to its' file
   *
   * @throws IOException
   */
  public void save() throws IOException {
    Gson gson = new Gson();
    FileOutputStream fos = new FileOutputStream(getFile(chat));
    fos.write(gson.toJson(this).getBytes());
    fos.close();
  }

  /**
   * Add a new message to this message queue
   *
   * @param m The message to add
   * @throws IOException
   */
  public void addMessage(Message m) throws IOException {
    if (messages == null) {
      messages = new Message[0];
    }

    List<Message> mA = getMessageList();
    mA.add(m);
    setMessages(mA);

    save();
  }

  /**
   * Remove a message from this queue
   *
   * @param m The message to remove
   * @throws IOException
   * @throws NoMessages
   */
  public void removeMessage(Message m) throws IOException, NoMessages {
    if (messages == null)
      throw new NoMessages();

    List<Message> mA = getMessageList();
    mA.remove(m);
    setMessages(mA);

    save();
  }

  /**
   * Check the queue for expired messages and delete them
   */
  private void checkForExpiredMessages() throws IOException {
    try {
      for (Message m : messages) {
        if (m.expired())
          removeMessage(m);
      }
    } catch (NoMessages ignored) {
    }
  }

  /**
   * Get the message queue file for a chat ID
   *
   * @param chatId The chat ID to get the queue file for.
   * @return The file cotaining the message queue for the given chat.
   */
  private static File getFile(int chatId) {
    return new File(DBConfig.instance.chatContentsDirectory, chatId + ".msgqueue");
  }

  /**
   * Load a message queue from a chat ID.
   *
   * @param chatId The chat ID of the message queue to load.
   */
  public static MessageQueue load(int chatId) throws IOException, FileNotFoundException {
    Gson gson = new Gson();

    FileInputStream fis = new FileInputStream(getFile(chatId));
    MessageQueue q = gson.fromJson(new InputStreamReader(fis), MessageQueue.class);
    q.checkForExpiredMessages();

    return q;
  }

  /**
   * Create a new message queue with the given chat ID
   *
   * @param chatId The chat ID
   * @return The new message queue
   * @throws IOException
   */
  public static MessageQueue create(int chatId) throws IOException {
    MessageQueue q = new MessageQueue();
    q.chat = chatId;

    q.save();
    return q;
  }
}
