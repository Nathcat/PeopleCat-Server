package com.nathcat.peoplecat_server.handlers;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.json.simple.JSONObject;

import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class PushUnsubscribe implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        JSONObject request = packets[0].getData();
        if (!request.containsKey("id")) {
            return new Packet[] { Packet.createError("Invalid Format", "You must specify the id field!") };
        }

        try {
            PreparedStatement stmt = server.db
                    .getPreparedStatement("DELETE FROM PushSubscriptions WHERE id = ? AND `user` = ?");
            stmt.setInt(1, Math.toIntExact((long) request.get("id")));
            stmt.setInt(2, (int) handler.user.get("id"));
            stmt.executeUpdate();
        } catch (SQLException e) {
            return new Packet[] { Packet.createError("DB Error",
                    "Failed to remove the subscription record to the database: " + e.getMessage()) };
        }

        return new Packet[] { Packet.createPacket(Packet.TYPE_PUSH_UNSUBSCRIBE, true, null) };
    }

}
