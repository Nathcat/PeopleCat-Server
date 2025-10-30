package com.nathcat.peoplecat_server.handlers;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.json.simple.JSONObject;

import com.nathcat.peoplecat_database.Database;
import com.nathcat.peoplecat_database.KeyManager;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class AddToChat implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        JSONObject request = packets[0].getData();

        if (!request.containsKey("id") || !request.containsKey("chatId")) {
            return new Packet[] {
                    Packet.createError("Invalid Format", "Request is missing some required fields!") };
        }

        request.put("id", request.get("id").getClass() == Long.class ? Math.toIntExact((long) request.get("id"))
                : request.get("id"));
        request.put("chatId",
                request.get("chatId").getClass() == Long.class ? Math.toIntExact((long) request.get("chatId"))
                        : request.get("chatId"));

        // Verify that this user and the target user are friends
        try {
            PreparedStatement stmt = server.db
                    .getPreparedStatement("SELECT * FROM Friends WHERE id = ? AND follower = ?");
            stmt.setInt(1, (int) handler.user.get("id"));
            stmt.setInt(2, (int) request.get("id"));
            stmt.execute();

            JSONObject[] results = Database.extractResultSet(stmt.getResultSet());
            if (results.length == 0) {
                return new Packet[] {
                        Packet.createError("Request Rejected", "You are not friends with the target user.") };
            }
        } catch (SQLException e) {
            return new Packet[] { Packet.createError("Friend Verification Failed",
                    "Failed to verify whether or not you and the target user are friends: " + e.getMessage()) };
        }

        // Verify that this user is a member of the chat, and has its key
        try {
            if (KeyManager.getChatKey((int) handler.user.get("id"), (int) request.get("chatId")) == null) {
                throw new IllegalStateException();
            }
        } catch (IOException | IllegalStateException e) {
            return new Packet[] { Packet.createError("Access Denied",
                    " You do not have access to this chat sufficient to perform this action.") };
        }

        // Add the chat membership and key to the other user's records
        try {
            KeyManager.addChatKey((int) request.get("id"), (int) request.get("chatId"),
                    (String) request.get("key"));
            PreparedStatement stmt = server.db
                    .getPreparedStatement("INSERT INTO ChatMemberships (`user`, `chatId`) VALUES (?, ?)");
            stmt.setInt(1, (int) request.get("id"));
            stmt.setInt(2, (int) request.get("chatId"));
            stmt.executeUpdate();

        } catch (SQLException e) {
            return new Packet[] { Packet.createError("DB Error",
                    "Failed to add the membership record to the database: " + e.getMessage()) };
        } catch (IOException | IllegalStateException e) {
            return new Packet[] { Packet.createError("Key Submission Error",
                    "Failed to add the key to the target user's key set: " + e.getMessage()) };
        }

        return new Packet[] { Packet.createPacket(
                Packet.TYPE_ADD_TO_CHAT,
                true,
                null) };
    }

}
