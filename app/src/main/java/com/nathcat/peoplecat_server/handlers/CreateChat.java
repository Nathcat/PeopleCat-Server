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

public class CreateChat implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        JSONObject request = packets[0].getData();
        if (!request.containsKey("name")) {
            return new Packet[] { Packet.createError("Invalid Format", "You must specify the name field!") };
        } else if (request.get("name").equals("")) {
            return new Packet[] { Packet.createError("Invalid Format", "The name field cannot be empty!") };
        }

        String name = (String) request.get("name");
        String icon = (String) request.get("icon");
        String key = (String) request.get("key");
        JSONObject chat;

        try {
            PreparedStatement stmt = server.db.getPreparedStatement("INSERT INTO Chats (Name"
                    + (key == null ? "" : ", isPrivate") + (icon == null ? "" : ", Icon") + ") VALUES (?"
                    + (key == null ? "" : ", 1") + (icon == null ? "" : ", ?") + ")");
            stmt.setString(1, name);
            if (icon != null)
                stmt.setString(2, icon);
            stmt.executeUpdate();

            stmt = server.db.getPreparedStatement(
                    "SELECT ChatID AS `chatId`, Name AS `name`, Icon AS `icon`, isPrivate FROM Chats WHERE ChatID = LAST_INSERT_ID()");
            stmt.execute();
            chat = Database.extractResultSet(stmt.getResultSet())[0];

            stmt = server.db
                    .getPreparedStatement("INSERT INTO ChatMemberships (`user`, `chatid`) VALUES (?, ?)");
            stmt.setInt(1, (int) handler.user.get("id"));
            stmt.setInt(2, (int) chat.get("chatId"));
            stmt.executeUpdate();

            if (key != null)
                KeyManager.addChatKey((int) handler.user.get("id"), (int) chat.get("chatId"), key);
        } catch (SQLException e) {
            handler.log("\033[91m;3mSQL Error! " + e.getClass().getName() + " " + e.getMessage() + "\n"
                    + Server.stringifyStackTrace(e.getStackTrace()) + "\033[0m");
            return new Packet[] { Packet.createError("Database Error", e.getMessage()) };
        } catch (IOException | IllegalStateException e) {
            handler.log("\033[91m;3mKey Submission Error! " + e.getClass().getName() + " " + e.getMessage()
                    + "\n" + Server.stringifyStackTrace(e.getStackTrace()) + "\033[0m");
            return new Packet[] { Packet.createError("Key Submission Error",
                    "Failed to submit private key: " + e.getMessage()) };
        }

        return new Packet[] {
                Packet.createPacket(
                        Packet.TYPE_CREATE_CHAT,
                        true,
                        chat)
        };
    }

}
