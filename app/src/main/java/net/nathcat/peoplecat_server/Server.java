package net.nathcat.peoplecat_server;

import java.net.InetSocketAddress;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import net.nathcat.peoplecat.protocol.ConnectionState;
import net.nathcat.peoplecat.protocol.Packet;
import net.nathcat.peoplecat.protocol.PacketBody;
import net.nathcat.peoplecat.protocol.PacketHandler;
import net.nathcat.peoplecat.protocol.Type;

public class Server extends WebSocketServer {
  private final HashMap<Type, PacketHandler<? extends Packet<? extends PacketBody>>> handlerMap = new HashMap<>();
  private final HashMap<WebSocket, ConnectionState> stateMap = new HashMap<>();

  public Server(int port) {
    super(new InetSocketAddress(port));
  }

  @Override
  public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
    System.out.println("New connection opened!");
    stateMap.put(webSocket, new ConnectionState());
  }

  @Override
  public void onClose(WebSocket webSocket, int i, String s, boolean b) {
    System.out.println("Connection closed!");
    stateMap.remove(webSocket);
  }

  @Override
  public void onMessage(WebSocket webSocket, String s) {
    System.out.println("String message received: " + s);
  }

  @Override
  public void onError(WebSocket webSocket, Exception e) {
    System.out.println("Error !");
    e.printStackTrace();

    webSocket.close();
    stateMap.remove(webSocket);
  }

  @Override
  public void onStart() {
    System.out.println("Server started!");
  }
}
