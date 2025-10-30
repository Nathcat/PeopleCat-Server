package com.nathcat.peoplecat_server.handlers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JSONObject;

import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class PushSubscribe implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        JSONObject request = packets[0].getData();
        if (!request.containsKey("endpoint") || !request.containsKey("auth") || !request.containsKey("key")) {
            return new Packet[] {
                    Packet.createError("Invalid Format", "You are missing some required fields!") };
        }

        int id;

        try {
            PreparedStatement stmt = server.db.getPreparedStatement(
                    "INSERT INTO PushSubscriptions (`user`, endpoint, `key`, auth) VALUES (?, ?, ?, ?)");
            stmt.setInt(1, (int) handler.user.get("id"));
            stmt.setString(2, (String) request.get("endpoint"));
            stmt.setString(3, (String) request.get("key"));
            stmt.setString(4, (String) request.get("auth"));
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            rs.next();
            id = rs.getInt(1);

        } catch (SQLException e) {
            return new Packet[] { Packet.createError("DB Error",
                    "Failed to add the subscription record to the database: " + e.getMessage()) };
        }

        JSONObject r = new JSONObject();
        r.put("id", id);

        return new Packet[] {
                Packet.createPacket(
                        Packet.TYPE_PUSH_SUBSCRIBE,
                        true,
                        r)
        };   
    }   
}
