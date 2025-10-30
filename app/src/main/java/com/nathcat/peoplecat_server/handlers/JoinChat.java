package com.nathcat.peoplecat_server.handlers;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.json.simple.JSONObject;

import com.nathcat.peoplecat_database.Database;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class JoinChat implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        JSONObject request = packets[0].getData();
        JSONObject chat;
        try {
            JSONObject[] results;
            PreparedStatement stmt = server.db.getPreparedStatement("SELECT * FROM Chats WHERE ChatID = ?");
            stmt.setInt(1, (int) ((long) request.get("chatId")));
            stmt.execute();
            results = Database.extractResultSet(stmt.getResultSet());

            if (results.length != 1) {
                return new Packet[] { Packet.createError("Database error",
                        "Could not find the specified chat or multiple chats exist with this ID.") };
            }

            chat = results[0];
        } catch (Exception e) {
            return new Packet[] { Packet.createError("Server error", e.getMessage()) };
        }

        if ((boolean) chat.get("isPrivate")) {
            return new Packet[] { Packet.createError("Access Denied", "You do not have access to this chat!") };
        }

        int chatID = Math.toIntExact((long) request.get("chatId"));

        try {
            PreparedStatement stmt = server.db
                    .getPreparedStatement("INSERT INTO ChatMemberships (`user`, `chatid`) VALUES (?, ?)");
            stmt.setInt(1, (int) handler.user.get("id"));
            stmt.setInt(2, chatID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getClass().getName()
                    .contentEquals(SQLIntegrityConstraintViolationException.class.getName())) {
                return new Packet[] {
                        Packet.createError("Already member", "You are already a member of this chat.") };
            } else {
                handler.log("\033[91m;3mSQL Error! " + e.getClass().getName() + " " + e.getMessage() + "\n"
                        + Server.stringifyStackTrace(e.getStackTrace()));
                return new Packet[] { Packet.createError("SQL Error", e.getMessage()) };
            }
        }

        JSONObject chatJSON = new JSONObject();
        chatJSON.put("chatId", chat.get("ChatID"));
        chatJSON.put("name", chat.get("Name"));
        chatJSON.put("keyId", chat.get("KeyID"));
        chatJSON.put("icon", chat.get("Icon"));

        return new Packet[] { Packet.createPacket(Packet.TYPE_JOIN_CHAT, true, chatJSON) };
    }

}
