package com.nathcat.peoplecat_server;

import org.java_websocket.WebSocket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

public class WebSocketInputStream extends InputStream {
    private Queue<Packet> queue = new LinkedList<>();
    private ByteArrayInputStream currentPacketStream;
    public final WebSocket socket;

    public WebSocketInputStream(WebSocket socket) { this.socket = socket; }

    @Override
    public int read() throws IOException {
        return 0;
    }

    public void pushPacket(Packet p) {
        queue.add(p);
    }

    public Packet getNextPacket() {
        return queue.remove();
    }

    private void updateCurrentPacket() {
        if (currentPacketStream == null || currentPacketStream.available() == 0) {
            if (!queue.isEmpty()) {
                currentPacketStream = new ByteArrayInputStream(queue.remove().getBytes());
            }
            else {
                currentPacketStream = null;
            }
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        while (currentPacketStream == null || currentPacketStream.available() == 0) {
            updateCurrentPacket();
        }

        return currentPacketStream.read(b);
    }

    @Override
    public byte[] readNBytes(int n) throws IOException {
        updateCurrentPacket();
        return currentPacketStream.readNBytes(n);
    }

    @Override
    public int available() {
        return queue.size();
    }
}
