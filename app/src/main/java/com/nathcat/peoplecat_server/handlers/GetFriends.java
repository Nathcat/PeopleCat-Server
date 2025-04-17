package com.nathcat.peoplecat_server.handlers;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.json.simple.JSONObject;

import com.nathcat.peoplecat_database.Database;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class GetFriends implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        JSONObject[] results;
        try {
            PreparedStatement stmt = server.db.getPreparedStatement(
                    "SELECT u.username, u.fullName, u.pfpPath FROM Friends LEFT JOIN SSO.Users as u ON Friends.id = u.id WHERE Friends.id = ?");
            stmt.setInt(1, (int) handler.user.get("id"));
            stmt.execute();

            results = Database.extractResultSet(stmt.getResultSet());
            stmt.close();

        } catch (SQLException e) {
            handler.log("\033[91;3mSQL error! " + e.getMessage() + "\033[0m");
            return new Packet[] { Packet.createError("Database error", e.getMessage()) };
        }

        Packet[] response = new Packet[results.length];
        for (int i = 0; i < results.length; i++) {
            response[i] = Packet.createPacket(
                    Packet.TYPE_GET_FRIENDS,
                    false,
                    results[i]);
        }

        response[response.length - 1].isFinal = true;
        return response;
    }

}
