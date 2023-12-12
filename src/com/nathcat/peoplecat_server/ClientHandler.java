package com.nathcat.peoplecat_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Handles a connection to a client application.
 *
 * @author Nathan Baines
 */
public class ClientHandler extends Thread {
    private final Server server;
    private final Socket client;
    private OutputStream outStream;
    private InputStream inStream;

    public ClientHandler(Server server, Socket client) throws IOException {
        this.server = server;
        this.client = client;
        outStream = client.getOutputStream();
        inStream = client.getInputStream();
    }

    @Override
    public void run() {

    }
}
