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

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
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

    public static void main(String[] args) throws SQLException, IOException, ParseException, NoSuchFieldException, IllegalAccessException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException {
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
        /*
        assert sslConfig.containsKey("certchain-path");
        assert sslConfig.containsKey("privatekey-path");
        assert sslConfig.containsKey("ca-path");

        String certchain_path = (String) sslConfig.get("certchain-path");
        String privatekey_path = (String) sslConfig.get("privatekey-path");
        String ca_path = (String) sslConfig.get("ca-path");

        // Get SSL certificates and keys
        X509ExtendedKeyManager keyManager = PemUtils.loadIdentityMaterial(certchain_path, privatekey_path);
        X509ExtendedTrustManager trustManager = PemUtils.loadTrustMaterial(ca_path);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial(keyManager)
                .withTrustMaterial(trustManager)
                .build();

        SSLContext sslContext = sslFactory.getSslContext();*/

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream("Assets/SSL/nathcat.net.keystore"), "changeit".toCharArray());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "changeit".toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        // Start the server with the given SSL parameters
        webSocketHandler.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
        webSocketHandler.start();
    }

    public WebSocketHandler(Server s) {
        super(new InetSocketAddress(s.port));
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
Version\s""" + Server.version + "\n" + """ 
Developed by Nathcat 2024""");

        Server.log("Running in websocket mode!");

        server.startCleaner();
    }
}
