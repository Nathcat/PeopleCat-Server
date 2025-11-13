package com.nathcat.peoplecat_server;

import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.nathcat.peoplecat_server.handlers.IPacketHandler;
import com.nathcat.peoplecat_server.handlers.PacketRouter;

public class ConnectionHandler extends Thread {
    /**
     * The connected TCP client
     */
    private Socket client;
    /**
     * The connected WebSocket client
     */
    private WebSocket webClient;
    /**
     * The output byte stream to the client
     */
    public OutputStream outStream;
    /**
     * The input byte stream from the client
     */
    public InputStream inStream;
    /**
     * Facilitates routing of packets to the appropriate handler
     */
    public final PacketRouter packetRouter = new PacketRouter();
    /**
     * Determines whether or not the current client is authenticated
     */
    public boolean authenticated = false;
    /**
     * The authenticated user data
     */
    public JSONObject user;
    /**
     * Determines whether or not the connection is over a websocket or TCP
     */
    public boolean isWebsocket = false;
    /**
     * Determines whether or not this handler is actively managing a connection
     */
    public boolean active = false;

    public ConnectionHandler(Socket client, IPacketHandler packetHandler) throws IOException {
        this.client = client;
        outStream = this.client.getOutputStream();
        inStream = this.client.getInputStream();

        setDaemon(true);
        start();
    }

    public ConnectionHandler(WebSocket client, WebSocketOutputStream os, WebSocketInputStream is) throws IOException {
        webClient = client;
        this.outStream = os;
        this.inStream = is;
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
     * @deprecated no longer needed with the new websocket library
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
            if (client != null) client.close();
            else webClient.close();

            active = false;
        } catch (IOException ignored) {}
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == ConnectionHandler.class && ((ConnectionHandler) obj).threadId() == this.threadId();
    }
    
    /*public long threadId() {
        return this.getId();
    }*/
}
