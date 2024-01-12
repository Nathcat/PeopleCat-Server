package com.nathcat.peoplecat_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ConnectionHandler extends Thread {
    private final Socket client;
    public OutputStream outStream;
    public InputStream inStream;
    public IPacketHandler packetHandler;
    public boolean authenticated = false;
    public boolean isWebsocket = false;

    public ConnectionHandler(Socket client, IPacketHandler packetHandler) throws IOException {
        this.client = client;
        outStream = this.client.getOutputStream();
        inStream = this.client.getInputStream();
        this.packetHandler = packetHandler;

        setDaemon(true);
        start();
    }

    public void log(String message) {
        System.out.println("Handler " + threadId() + ": " + message);
    }

    @Override
    public String toString() {
        return "Handler " + threadId();
    }

    /**
     * Perform basic setup of connection between the server and the client. This is effectively the handshake process.
     */
    public void setup() {
        try {
            isWebsocket = WebSocketAdapter.detectWebSocket(client);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read the next packet from the stream. If one is not currently available, then wait for the next one.
     * @return The packet just read from the stream
     */
    public Packet getPacket() {
        try {
            if (!isWebsocket) {
                return new Packet(inStream);
            }
            else {
                return WebSocketAdapter.readPacket(inStream);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write a packet to the stream.
     * @param p The packet to write.
     */
    public void writePacket(Packet p) {
        try {
            outStream.write(isWebsocket ? WebSocketAdapter.createMessageBuffer(p) : p.getBytes());
            outStream.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Close the socket
     */
    public void close() {
        try {
            client.close();
        } catch (IOException ignored) {}
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == ConnectionHandler.class && ((ConnectionHandler) obj).threadId() == this.threadId();
    }
}
