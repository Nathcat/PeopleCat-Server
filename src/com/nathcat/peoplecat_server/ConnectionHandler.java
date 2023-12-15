package com.nathcat.peoplecat_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ConnectionHandler extends Thread {
    private final Socket client;
    private OutputStream outStream;
    private InputStream inStream;
    public IPacketHandler packetHandler;

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
     * Read the next packet from the stream. If one is not currently available, then wait for the next one.
     * @return The packet just read from the stream
     */
    public Packet getPacket() {
        try {
            return new Packet(inStream);

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
            outStream.write(p.getBytes());
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
}
