package com.nathcat.peoplecat_server.handlers;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.json.simple.JSONObject;

import com.nathcat.peoplecat_database.Database;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class GetChatMembers implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        JSONObject request = packets[0].getData();
        if (!request.containsKey("chatId")) return new Packet[] { Packet.createError("Missing field", "Must specify chatId field in request.") };     

        if (!server.db.isMemberOfChat((int) handler.user.get("id"), (int) request.get("chatId"))) return new Packet[] { Packet.createError("Not member of chat", "You are not a member of this chat.") };

        try {
            PreparedStatement stmt = server.db.getPreparedStatement("SELECT `user` AS 'id' FROM ChatMemberships WHERE `ChatID` = ? AND `user` != ?");
            stmt.setInt(1, (int) request.get("chatId"));
            stmt.setInt(2, (int) handler.user.get("id"));
            stmt.execute();
            JSONObject[] results = Database.extractResultSet(stmt.getResultSet());

            if (results.length == 0) return new Packet[] { Packet.createError("No members", "Chat has no members!") };

            Packet[] response = new Packet[results.length];
            for (int i = 0; i < results.length; i++) {
                response[i] = Packet.createPacket(Packet.TYPE_GET_CHAT_MEMBERS, i == (results.length - 1), results[i]);
            }

            return response;
        } 
        catch (SQLException e) {
            handler.log(e.getMessage());
            return new Packet[] { Packet.createError("DB Error", "Database error!") };
        }
    }

}
