package com.nathcat.peoplecat_server.handlers;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.json.simple.JSONObject;

import com.nathcat.messagecat_database_entities.Message;
import com.nathcat.peoplecat_database.Database;
import com.nathcat.peoplecat_database.KeyManager;
import com.nathcat.peoplecat_database.MessageBox;
import com.nathcat.peoplecat_server.ClientHandler;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class SendMessage implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        JSONObject request = packets[0].getData();
        Message msg = new Message((int) handler.user.get("id"), Math.toIntExact((long) request.get("chatId")),
                (long) request.get("timeSent"), request.get("content"));

        try {
            Message[] messages = MessageBox.openMessageBox(msg.ChatID);
            Message[] newMessages = new Message[messages.length + 1];
            System.arraycopy(messages, 0, newMessages, 0, messages.length);
            newMessages[messages.length] = msg;
            MessageBox.updateMessageBox(msg.ChatID, newMessages);
        } catch (IOException e) {
            return new Packet[] { Packet.createError("Server error",
                    "An error occurred when writing the message store to the disk.") };
        }

        int chatID = Math.toIntExact((long) request.get("chatId"));
        JSONObject chat;

        try {
            PreparedStatement stmt = server.db.getPreparedStatement(
                    "SELECT ChatID AS `chatId`, Name AS `name`, Icon AS `icon`, isPrivate FROM Chats WHERE ChatID = ?");
            stmt.setInt(1, chatID);
            stmt.execute();

            JSONObject[] results = Database.extractResultSet(stmt.getResultSet());
            try {
                results[0].put("key", KeyManager.getChatKey((int) handler.user.get("id"),
                        chatID));
            } catch (IOException | IllegalStateException e) {
                results[0].put("key", null);
            }

            chat = results[0];
        } catch (SQLException | IndexOutOfBoundsException e) {
            return new Packet[] { Packet.createError("Server error",
                    "An error occurred when getting chat information.") };
        }

        // Notify other users about this message
        JSONObject notification = new JSONObject();
        notification.put("chat", chat);
        JSONObject msgJSON = MessageBox.messageToJSON(msg);
        notification.put("message", msgJSON);
        Packet notifyPacket = Packet.createPacket(Packet.TYPE_NOTIFICATION_MESSAGE, true, notification);

        JSONObject[] members;
        try {
            PreparedStatement stmt = server.db.getPreparedStatement(
                    "SELECT `user`, pfpPath, fullName, Chats.Name AS 'chatName' FROM ChatMemberships JOIN SSO.Users ON `user` = SSO.Users.id JOIN Chats ON ChatMemberships.`chatid` = Chats.ChatID WHERE ChatMemberships.`chatid` = ?");
            stmt.setInt(1, chatID);
            stmt.execute();

            members = Database.extractResultSet(stmt.getResultSet());
        } catch (SQLException e) {
            handler.log("\033[91m;3mSQL Error! " + e.getClass().getName() + " " + e.getMessage() + "\n"
                    + Server.stringifyStackTrace(e.getStackTrace()));
            return new Packet[] { Packet.createError("Database Error", e.getMessage()) };
        }

        for (JSONObject member : members) {
            int userID = (int) member.get("user");

            // Removing this condition will allow multiple clients connected under the same
            // user
            // to receive messages from each other.

            // if (userID == (int) handler.user.get("id")) {
            // continue;
            // }

            List<ClientHandler> handlerList = server.userToHandler.get(userID);
            if (handlerList != null)
                handlerList.forEach((ClientHandler h) -> h.writePacket(notifyPacket));

            JSONObject content = new JSONObject();
            content.put("content", msgJSON.get("content"));
            content.put("senderName", member.get("fullName"));
            content.put("senderPfp", member.get("pfpPath"));
            content.put("chatName", member.get("chatName"));
            server.sendPushNotification(userID, content);
        }

        return new Packet[] { Packet.createPing() };
    }

}
