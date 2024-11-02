package com.nathcat.peoplecat_server;

import com.mysql.cj.xdevapi.Client;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.pem.util.PemUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.DefaultWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Acts as an alternative to the Server class. This class uses websockets to handle connections to clients, where the
 * Server class uses native TCP connections.
 * @author Nathan Baines
 */
public class WebSocketHandler extends WebSocketServer {
    public Server server;
    private final HashMap<WebSocket, ClientHandler> sockHandlerMap;

    public static void main(String[] args) throws SQLException, IOException, ParseException, NoSuchFieldException, IllegalAccessException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        WebSocketHandler webSocketHandler = new WebSocketHandler(new Server(Server.getOptions(args)));

        if (webSocketHandler.server.useSSL) {
            // Load SSL config file
            JSONObject sslConfig;
            try (FileInputStream fis = new FileInputStream("Assets/SSL_Config.json")) {
                sslConfig = (JSONObject) new JSONParser().parse(new String(fis.readAllBytes()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            assert sslConfig != null;

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream("Assets/SSL/nathcat.net.keystore"), "changeit".toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, "changeit".toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

            // Start the server with the given SSL parameters
            webSocketHandler.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
        }
        else {
            webSocketHandler.setWebSocketFactory(new DefaultWebSocketServerFactory());
            webSocketHandler.server.log("\033[33;3mRunning in no-SSL mode!\033[0m");
        }

        webSocketHandler.start();
    }

    public WebSocketHandler(Server s) {
        super(new InetSocketAddress(s.port));
        server = s;
        sockHandlerMap = new HashMap<>();
    }


    @Override
    public void onOpen(org.java_websocket.WebSocket webSocket, ClientHandshake clientHandshake) {
        server.log("Connection received");

        // If the server is full close the connection
        if (server.handlers.size() >= server.threadCount) {
            server.log("Server is full, rejecting connection");
            webSocket.send("Server is full!");
            webSocket.close();
        }
        else {  // ... otherwise link the connection to a new client handler
            server.log("Trying to accept connection");
            try {
                ClientHandler h = new ClientHandler(server, webSocket, new WebSocketOutputStream(webSocket), new WebSocketInputStream(webSocket));
                sockHandlerMap.put(webSocket, h);
                h.active = true;
                server.handlers.add(h);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            server.log("Accepted connection into handler");
        }
    }

    @Override
    public void onClose(org.java_websocket.WebSocket webSocket, int i, String s, boolean b) {
        // Close the relevant client handler
        ClientHandler h = sockHandlerMap.get(webSocket);
        h.close();
        sockHandlerMap.remove(webSocket);
    }

    @Override
    public void onMessage(org.java_websocket.WebSocket webSocket, String s) {
        try {
            handlePacket(
                    webSocket,
                    Packet.fromData((JSONObject) new JSONParser().parse(s))
            );
        } catch (Exception e) {
            ClientHandler h = sockHandlerMap.get(webSocket);
            h.writePacket(Packet.createError(e.getClass().getName(), e.getMessage()));
            h.log("Written error message: \033[91;3m" + e.getClass().getName() + ": " + e.getMessage() + "\n" + Server.stringifyStackTrace(e.getStackTrace()) + "\033[0m");
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        ClientHandler h = sockHandlerMap.get(conn);
        try {
            handlePacket(
                    conn,
                    new Packet(new ByteArrayInputStream(message.array()))
            );

        } catch (Exception e) {
            h.writePacket(Packet.createError(e.getClass().getName(), e.getMessage()));
            h.log("Written error message: \033[91;3m" + e.getClass().getName() + ": " + e.getMessage() + "\n" + Server.stringifyStackTrace(e.getStackTrace()) + "\033[0m");
        }
    }

    @Override
    public void onError(org.java_websocket.WebSocket webSocket, Exception e) {
        try {
            // Close the relevant client handler and log the error
            ClientHandler h = sockHandlerMap.get(webSocket);
            h.log("WebSocket error: " + e.getClass().getName() + "\n" + Server.stringifyStackTrace(e.getStackTrace()));
            h.close();
            sockHandlerMap.remove(webSocket);
        }
        catch (Exception e2) {
            Server.log("\033[91;3mFailed to stop handler as none was found for the given websocket");
            Server.log("An error occurred during setup:");
            Server.log(e.getClass().getName() + "\n" + Server.stringifyStackTrace(e.getStackTrace()));
        }
    }

    @Override
    public void onStart() {
        Server.log("""
----- PeopleCat Server -----
Version\s""" + Server.version + "\n" + """ 
Developed by Nathcat 2024""");

        Server.log("Running in websocket mode!");

        server.startCleaner(false);
    }

    public void handlePacket(WebSocket conn, Packet p) {
        ClientHandler h = sockHandlerMap.get(conn);
        h.log("Got packet from socket: " + p);
        try {
            WebSocketInputStream is = (WebSocketInputStream) h.inStream;
            is.pushPacket(p);

            ArrayList<Packet> packetList = new ArrayList<>();
            if (p.isFinal) {
                while (is.available() > 0) {
                    packetList.add(is.getNextPacket());
                }

                Packet[] response = h.packetHandler.handle(h, packetList.toArray(new Packet[0]));
                ((WebSocketOutputStream) h.outStream).write(response);
                h.log("Written response: " + Arrays.toString(response));
            }
        }
        catch (Exception e) {
            h.writePacket(Packet.createError(e.getClass().getName(), e.getMessage()));
            h.log("Written error message: \033[91;3m" + e.getClass().getName() + ": " + e.getMessage() + "\n" + Server.stringifyStackTrace(e.getStackTrace()) + "\033[0m");
        }
    }
}
