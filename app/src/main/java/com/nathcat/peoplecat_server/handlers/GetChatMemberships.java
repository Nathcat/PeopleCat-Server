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

public class GetChatMemberships implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        Packet[] stream;
        try {
            PreparedStatement stmt = server.db.getPreparedStatement(
                    "SELECT Chats.ChatID AS `chatId`, Name AS `name`, Icon AS `icon`, isPrivate FROM ChatMemberships INNER JOIN Chats ON ChatMemberships.`chatid` = Chats.ChatID WHERE `user` = ?");
            stmt.setInt(1, (int) handler.user.get("id"));
            stmt.execute();
            JSONObject[] results = Database.extractResultSet(stmt.getResultSet());

            if (results.length == 0) {
                return new Packet[] {
                        Packet.createError("No Chat Memberships", "This user is not a member of any chats.") };
            }

            for (int i = 0; i < results.length; i++) {
                try {
                    results[i].put("key", KeyManager.getChatKey((int) handler.user.get("id"),
                            (int) results[i].get("chatId")));
                } catch (IOException | IllegalStateException e) {
                    results[i].put("key", null);
                }
            }

            stream = new Packet[results.length];
            for (int i = 0; i < results.length; i++) {
                stream[i] = Packet.createPacket(
                        Packet.TYPE_GET_CHAT_MEMBERSHIPS,
                        false,
                        results[i]);
            }
        } catch (SQLException e) {
            handler.log("\033[91m;3mSQL Error! " + e.getClass().getName() + " " + e.getMessage() + "\n"
                    + Server.stringifyStackTrace(e.getStackTrace()));
            return new Packet[] { Packet.createError("Database Error", e.getMessage()) };
        }

        stream[stream.length - 1].isFinal = true;
        return stream;
    }

}
