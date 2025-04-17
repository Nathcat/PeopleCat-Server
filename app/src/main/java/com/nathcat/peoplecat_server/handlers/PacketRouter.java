package com.nathcat.peoplecat_server.handlers;

import java.util.HashMap;

import com.nathcat.peoplecat_server.Packet;

/**
 * Routes packets to their designated handler/s.
 * @author Nathan Baines
 */
public class PacketRouter {
    private HashMap<Integer, IPacketHandler> handlers = new HashMap<>();

    /**
     * Register a packet handler for a packet type
     * @param packetType The packet type
     * @param handler The handler
     */
    public void register(int packetType, IPacketHandler handler) {
        handlers.put(packetType, handler);
    }

    /**
     * Remove the handler associated with a packet type
     * @param packetType The packet type to clear
     */
    public void remove(int packetType) {
        handlers.remove(packetType);
    }

    /**
     * Pass the provided packet sequence to all the registered handlers
     * @param packets The packet sequence to pass
     * @return The packet sequence returned by the handler.
     */
    public Packet[] handlePacketSequence(com.nathcat.peoplecat_server.Server server, com.nathcat.peoplecat_server.ConnectionHandler conn, Packet[] packets) {
        if (handlers.containsKey(packets[0].type)) {
            return handlers.get(packets[0].type).handle(server, conn, packets);
        }

        return null;
    }
}
