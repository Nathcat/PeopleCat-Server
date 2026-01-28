package net.nathcat.peoplecat_test;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

public class Client extends WebSocketClient {
  public Client(URI server, Draft draft) {
    super(server, draft);
  }

  public Client(URI server) {
    super(server);
  }

  @Override
  public void onOpen(ServerHandshake handshake) {
    System.out.println("Connection opened to server!");
  }

  @Override
  public void onClose(int code, String reason, boolean remote) {
    System.out.println("Connection to server was closed!");
  }

  @Override
  public void onMessage(String message) {
    System.out.println("Received string message from server! " + message);
  }

  @Override
  public void onError(Exception ex) {
    System.out.println("An error occurred!");
    ex.printStackTrace();
  }
}
