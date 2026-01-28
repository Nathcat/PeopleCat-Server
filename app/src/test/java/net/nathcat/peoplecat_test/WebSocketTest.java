package net.nathcat.peoplecat_test;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import net.nathcat.peoplecat_server.Server;

public class WebSocketTest {
  @Test
  public void connectionTest() throws URISyntaxException {
    Thread s = new Thread(() -> {
      Server server = new Server(1234);
      server.start();
    });

    s.setDaemon(true);
    s.start();

    Client c = new Client(new URI("ws://localhost:1234"));
    c.send("Hello from client!");
    c.close();
  }
}
