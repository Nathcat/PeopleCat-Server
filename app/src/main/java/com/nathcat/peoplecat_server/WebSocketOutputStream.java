package com.nathcat.peoplecat_server;

import java.io.IOException;
import java.io.OutputStream;

import org.java_websocket.WebSocket;

/**
 * Allows output of a packet through a websocket
 * @author Nathan Baines
 */
public class WebSocketOutputStream extends OutputStream {
    private final WebSocket socket;

    public WebSocketOutputStream(WebSocket socket) {
        this.socket = socket;
    }

    @Override
    public void write(int b) throws IOException {
        socket.send(new byte[b]);
    }

    @Override
    public void write(byte[] b) throws IOException {
        socket.send(b);
    }

    /**
     * Write all given packets to the websocket
     * @param packets The array of packets to write, must end with a final packet, where <code>Packet.isFinal = true</code>
     * @throws IOException Thrown should the writing operation fail
     */
    public void write(Packet[] packets) throws IOException {
        for (Packet p : packets) {
            socket.send(p.getBytes());
        }
    }

    @Override
    public void flush() { /* Do nothing, this stream does not buffer any data */ }
}
