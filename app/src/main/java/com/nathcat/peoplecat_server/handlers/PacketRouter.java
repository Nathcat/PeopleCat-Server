package com.nathcat.peoplecat_server.handlers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.nathcat.peoplecat_server.Packet;

/**
 * Routes packets to their designated handler/s.
 * @author Nathan Baines
 */
public class PacketRouter {
    private HashMap<Integer, List<IPacketHandler>> handlers = new HashMap<>();

    /**
     * Register a packet handler for a packet type
     * @param packetType The packet type
     * @param handler The handler
     */
    public void register(int packetType, IPacketHandler handler) {
        if (handlers.containsKey(packetType)) {
            handlers.get(packetType).add(handler);
        }
        else {
            handlers.put(packetType, Arrays.stream(new IPacketHandler[] { handler }).toList());
        }
    }

    /**
     * Remove a specific handler from the list
     * @param packetType The packet type which the handler is registered to
     * @param handler The handler to remove
     */
    public void remove(int packetType, IPacketHandler handler) {
        if (handlers.containsKey(packetType)) {
            List<IPacketHandler> l = handlers.get(packetType);

            for (int i = 0; i < l.size(); i++) {
                if (l.get(i) == handler) {{
                    l.remove(i);
                    return;
                }}
            }
        }
    }

    /**
     * Remove a handler by its index
     * @param packetType The packet type which the handler is registered to
     * @param index The index of the handler to remove
     */
    public void remove(int packetType, int index) {
        if (handlers.containsKey(packetType)) handlers.get(packetType).remove(index);
    }

    /**
     * Remove all the handlers associated with a packet type
     * @param packetType The packet type to clear
     */
    public void remove(int packetType) {
        handlers.remove(packetType);
    }

    /**
     * Pass the provided packet sequence to all the registered handlers
     * @param packets The packet sequence to pass
     */
    public void routePackets(Packet[] packets) {
        List<IPacketHandler> l = handlers.get(packets[0].type);
        if (l != null) {
            for (IPacketHandler h : l) {
                h.handle(packets);
            }
        }
    }
}
