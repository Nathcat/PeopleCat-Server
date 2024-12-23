package com.nathcat.peoplecat_database;

import com.nathcat.messagecat_database_entities.Message;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

/**
 * <p>Provides a way to store messages in chats. This an improvement upon the legacy system from MessageCat.</p>
 * <p>
 *     The original MessageQueue class from MessageCat, which has been used in PeopleCat up to server version 4.3.0,
 *     ensured that the last 10 messages sent into the chat are stored. A suggested improvement during the prototyping
 *     phase of a new client being written by <a href="https://github.com/brooke-ec">Brooke Reavell</a> was that
 *     the server should delete messages after a certain amount of time has elapsed since they were sent. This is
 *     the behaviour implemented by this class.
 * </p>
 * <p>
 *     This class will also change the way that messages are stored on the server. Previously, in line with the protocols
 *     in place on MessageCat, messages were stored as a binary file containing the Java <code>MessageQueue</code>,
 *     since this class is finalised and will not be changed again, this doesn't matter so much, but if changes to this
 *     class were to be made, it would mean that all message data would have to be erased, or the server would not be
 *     able to handle reading in the messages. So this class will store messages as a JSON array, a medium which will
 *     not be affected by changes in the class's source code. The actual JSON of the messages will be determined by the
 *     <code>Message</code> class, linked below.
 * </p>
 * <p>
 *     This class will ensure that messages are stored for 24 hours. Note that messages may actually exist on the server
 *     longer than this! The class will only process and purge any old messages when it is required to access the messages.
 *     So if no client attempts to access the messages for a chat for longer than 24 hours, those old messages will still
 *     be physically stored on the server, but the next time a client attempts to access them, they will be purged.
 * </p>
 * @author <a href="https://github.com/Nathcat">Nathan Baines</a>
 * @see <a href="https://nathcat.github.io/MessageCatServer/JavaDoc/com/nathcat/messagecat_database/MessageQueue.html">com.nathcat.messagecat_database.MessageQueue</a>
 * @see <a href="https://nathcat.github.io/MessageCatServer/JavaDoc/com/nathcat/messagecat_database_entities/Message.html">com.nathcat.messagecat_database_entities.Message</a>
 */
public class MessageBox {
    /**
     * Get the file path of the message box for a certain chat
     * @param id The ID of the chat which owns the message box
     * @return The <i>relative</i> file path of the chat's message box
     */
    public static String getMessageBoxPath(int id) {
        return "Assets/Data/MessageBoxes/" + id + ".box";
    }

    /**
     * Open a plain text message box for a chat by its <code>chatId</code>
     * @param id The <code>chatId</code> of the desired chat.
     * @return An array of messages contained by the message box
     */
    public static Message[] openMessageBox(int id) throws IOException, FileNotFoundException {
        String content;
        FileInputStream fis = new FileInputStream(getMessageBoxPath(id));
        content = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
        fis.close();

        // We expect a JSONArray here
        JSONArray messages;
        try {
            messages = (JSONArray) new JSONParser().parse(content);
        }
        catch (ParseException e) {
            throw new RuntimeException("Message box for chat " + id + " has invalid JSON syntax.");
        }

        // Purge the existing messages for messages which should be deleted.
        ArrayList<Message> purgedMessages = new ArrayList<>();
        long timeValidFrom = new Date().getTime() - 86_400_000;  // The current time subtract 24 hours
        messages.forEach((Object p) -> {
            if (p.getClass() != JSONObject.class) {
                throw new RuntimeException("An element of message box array is not a JSONObject!");
            }

            JSONObject obj = (JSONObject) p;
            Message m = new Message(
                    Math.toIntExact((long) obj.get("senderId")),
                    Math.toIntExact((long) obj.get("chatId")),
                    (long) obj.get("timeSent"),
                    obj.get("content")
            );

            if (m.TimeSent >= timeValidFrom) {
                purgedMessages.add(m);
            }
        });

        Message[] result = purgedMessages.toArray(new Message[0]);
        updateMessageBox(id, result);
        return result;
    }

    /**
     * Write the supplied array of messages into the specified message box, overwriting any existing messages
     * @param id The ID of the chat which owns the message box
     * @param messages The array o messages to write into the message box
     */
    public static void updateMessageBox(int id, Message[] messages) throws IOException {
        JSONArray box = new JSONArray();

        for (Message m : messages) {
            box.add(messageToJSON(m));
        }

        FileOutputStream fos = new FileOutputStream(getMessageBoxPath(id));
        fos.write(
          box.toJSONString().getBytes(StandardCharsets.UTF_8)
        );
        fos.close();
    }

    public static JSONObject messageToJSON(Message m) {
        JSONObject obj = new JSONObject();
        obj.put("senderId", m.SenderID);
        obj.put("chatId", m.ChatID);
        obj.put("timeSent", m.TimeSent);
        obj.put("content", m.Content);

        return obj;
    }
}
