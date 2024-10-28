package com.nathcat.peoplecat_server;

import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.pem.util.PemUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509ExtendedKeyManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Acts as an alternative to the Server class. This class uses websockets to handle connections to clients, where the
 * Server class uses native TCP connections.
 * @author Nathan Baines
 */
public class WebSocketHandler extends WebSocketServer {
    public Server server;
    private final HashMap<WebSocket, ClientHandler> sockHandlerMap;

    public static void main(String[] args) throws SQLException, IOException, ParseException, NoSuchFieldException, IllegalAccessException {
        WebSocketHandler webSocketHandler = new WebSocketHandler(new Server(Server.getOptions(args)));

        // Load SSL config file
        JSONObject sslConfig;
        try (FileInputStream fis = new FileInputStream("Assets/SSL_Config.json")) {
            sslConfig = (JSONObject) new JSONParser().parse(new String(fis.readAllBytes()));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        assert sslConfig != null;
        assert sslConfig.containsKey("certchain-path");
        assert sslConfig.containsKey("privatekey-path");

        String certchain_path = (String) sslConfig.get("certchain-path");
        String privatekey_path = (String) sslConfig.get("privatekey-path");
        String privatekey_password = sslConfig.containsKey("privatekey-password") ? (String) sslConfig.get("privatekey-password") : "";

        // Get SSL certificates and keys
        X509ExtendedKeyManager keyManager = PemUtils.loadIdentityMaterial(certchain_path, privatekey_path, privatekey_password.toCharArray());

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(keyManager)
                .build();

        SSLContext sslContext = sslFactory.getSslContext();

        // Start the server with the given SSL parameters
        webSocketHandler.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
        webSocketHandler.start();
    }

    public WebSocketHandler(Server s) {
        server = s;
        sockHandlerMap = new HashMap<>();
    }


    @Override
    public void onOpen(org.java_websocket.WebSocket webSocket, ClientHandshake clientHandshake) {
        // If the server is full close the connection
        if (server.handlers.size() >= server.threadCount) {
            webSocket.send("Server is full!");
            webSocket.close();
        }
        else {  // ... otherwise link the connection to a new client handler
            try {
                ClientHandler h = new ClientHandler(server, webSocket, new WebSocketOutputStream(webSocket), new WebSocketInputStream(webSocket));
                sockHandlerMap.put(webSocket, h);
                server.handlers.add(h);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
        // Try to pass the received packet to the relevant client handler,
        // failing this, send an error packet with the error information
        ClientHandler h = sockHandlerMap.get(webSocket);
        try {
            ((WebSocketInputStream) h.inStream)
                    .pushPacket(
                            Packet.fromData(
                                    (JSONObject) new JSONParser().parse(s)
                            )
                    );
        }
        catch (Exception e) {
            h.writePacket(Packet.createError(e.getClass().getName(), e.getMessage()));
        }
    }

    @Override
    public void onError(org.java_websocket.WebSocket webSocket, Exception e) {
        // Close the relevant client handler and log the error
        ClientHandler h = sockHandlerMap.get(webSocket);
        h.log("WebSocket error: " + e.getClass().getName() + "\n" + Server.stringifyStackTrace(e.getStackTrace()));
        h.close();
        sockHandlerMap.remove(webSocket);
    }

    @Override
    public void onStart() {
        Server.log("""
----- PeopleCat Server -----
Version""" + Server.version + """
Developed by Nathcat 2024""");

        Server.log("Running in websocket mode!");

        server.startCleaner();
    }
}
