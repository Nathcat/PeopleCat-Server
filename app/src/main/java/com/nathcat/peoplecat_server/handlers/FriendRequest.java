package com.nathcat.peoplecat_server.handlers;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.json.simple.JSONObject;

import com.nathcat.peoplecat_database.Database;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class FriendRequest implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        JSONObject data = packets[0].getData();
        String action = (String) data.get("action");
        Packet[] response;

        if (action.contentEquals("SEND")) {
            if (!data.containsKey("recipient")) {
                return new Packet[] { Packet.createError("Invalid request",
                        "This action type must contain the recipient field.") };
            }

            try {
                PreparedStatement stmt = server.db
                        .getPreparedStatement("INSERT INTO FriendRequests (sender, recipient) values (?, ?)");
                stmt.setInt(1, (int) handler.user.get("id"));
                stmt.setInt(2, (int) data.get("recipient"));
                stmt.executeUpdate();
                stmt.close();

                stmt = server.db.getPreparedStatement(
                        "SELECT id FROM FriendRequests WHERE sender = ? AND recipient = ?");
                stmt.setInt(1, (int) handler.user.get("id"));
                stmt.setInt(2, (int) data.get("recipient"));
                stmt.execute();

                JSONObject[] r = Database.extractResultSet(stmt.getResultSet());
                stmt.close();

                response = new Packet[] { Packet.createPacket(Packet.TYPE_FRIEND_REQUEST, true, r[0]) };
            } catch (SQLException e) {
                handler.log("\033[91;3mSQL error! " + e.getMessage() + "\033[0m");
                return new Packet[] { Packet.createError("Database error", e.getMessage()) };
            }
        } else if (action.contentEquals("ACCEPT")) {
            if (!data.containsKey("id")) {
                return new Packet[] {
                        Packet.createError("Invalid request", "This action type must contain the id field.") };
            }

            try {
                PreparedStatement stmt = server.db
                        .getPreparedStatement("SELECT sender FROM FriendRequests WHERE id = ?");
                stmt.setInt(1, (int) data.get("id"));
                stmt.execute();
                int sender;
                try {
                    sender = (int) Database.extractResultSet(stmt.getResultSet())[0].get("sender");
                } catch (Exception e) {
                    // Problem is likely that the request does not exist
                    stmt.close();
                    return new Packet[] { Packet.createError("Friend request does not exist", e.getMessage()) };
                }

                stmt.close();

                stmt = server.db
                        .getPreparedStatement("INSERT INTO Friends (id, follower) values (?, ?), (?, ?)");
                stmt.setInt(1, (int) handler.user.get("id"));
                stmt.setInt(2, sender);
                stmt.setInt(3, sender);
                stmt.setInt(4, (int) handler.user.get("id"));
                stmt.executeUpdate();
                stmt.close();

                stmt = server.db.getPreparedStatement("DELETE FROM FriendRequests WHERE id = ?");
                stmt.setInt(1, (int) data.get("id"));
                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                handler.log("\033[91;3mSQL error! " + e.getMessage() + "\033[0m");
                return new Packet[] { Packet.createError("Database error", e.getMessage()) };
            }

            response = new Packet[0];
        } else if (action.contentEquals("DECLINE")) {
            if (!data.containsKey("id")) {
                return new Packet[] {
                        Packet.createError("Invalid request", "This action type must contain the id field.") };
            }

            try {
                PreparedStatement stmt = server.db
                        .getPreparedStatement("DELETE FROM FriendRequests WHERE id = ?");
                stmt.setInt(1, (int) data.get("id"));
                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                handler.log("\033[91;3mSQL error! " + e.getMessage());
                return new Packet[] { Packet.createError("Database error", e.getMessage()) };
            }

            response = new Packet[0];
        } else if (action.contentEquals("GET")) {
            try {
                PreparedStatement stmt = server.db
                        .getPreparedStatement("SELECT id, sender FROM FriendRequests WHERE recipient = ?");
                stmt.setInt(1, (int) handler.user.get("id"));
                stmt.execute();
                JSONObject[] r = Database.extractResultSet(stmt.getResultSet());

                response = new Packet[r.length];
                for (int i = 0; i < r.length; i++) {
                    response[i] = Packet.createPacket(
                            Packet.TYPE_FRIEND_REQUEST,
                            false,
                            r[i]);
                }

                response[response.length - 1].isFinal = true;
            } catch (SQLException e) {
                handler.log("\033[91;3mSQL error! " + e.getMessage() + "\033[0m");
                return new Packet[] { Packet.createError("Database error", e.getMessage()) };
            }
        } else {
            response = new Packet[] { Packet.createError("Unrecognised friend request action",
                    "The action \"" + action + "\" is not recognised.") };
        }

        return response;
    }

}
