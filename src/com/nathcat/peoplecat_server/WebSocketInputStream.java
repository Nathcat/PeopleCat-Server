package com.nathcat.peoplecat_server;

import org.java_websocket.WebSocket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractQueue;

public class WebSocketInputStream extends InputStream {
    private AbstractQueue<Packet> queue;
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
        updateCurrentPacket();
        return currentPacketStream.read(b);
    }

    @Override
    public byte[] readNBytes(int n) throws IOException {
        updateCurrentPacket();
        return currentPacketStream.readNBytes(n);
    }
}
