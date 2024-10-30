package com.nathcat.peoplecat_server;

import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;

/**
 * Allows output of a packet through a websocket
 * @author Nathan Baines
 */
public class WebSocketOutputStream extends OutputStream {
    private final WebSocket socket;

    public WebSocketOutputStream(WebSocket socket) {
        this.socket = socket;
    }

    /**
     * Transform a packet into a websocket fragment message buffer.
     * @param packet The packet to transform
     * @return A byte array containing the fragment data to be sent through the websocket.
     */
    private static byte[] createMessageBuffer(Packet packet) {
        JSONObject packetData = packet.getData();
        if (packetData == null) packetData = new JSONObject();
        packetData.put("type", packet.type);
        packetData.put("isFinal", packet.isFinal ? 1 : 0);

        SecureRandom rng = new SecureRandom();
        return packetData.toJSONString().getBytes();
    }

    @Override
    public void write(int b) throws IOException {
        socket.send(new byte[b]);
    }

    @Override
    public void write(byte[] b) throws IOException {
        Packet p = new Packet(new ByteArrayInputStream(b));
        JSONObject d = p.getData_WebSocket();

        socket.send(d.toJSONString());
    }

    /**
     * Write all given packets to the websocket
     * @param packets The array of packets to write, must end with a final packet, where <code>Packet.isFinal = true</code>
     * @throws IOException Thrown should the writing operation fail
     */
    public void write(Packet[] packets) throws IOException {
        for (Packet p : packets) {
            socket.send(p.getData_WebSocket().toJSONString());
        }
    }

    @Override
    public void flush() { /* Do nothing, this stream does not buffer any data */ }
}
