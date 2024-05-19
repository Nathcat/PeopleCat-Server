package com.nathcat.peoplecat_server;

import com.nathcat.peoplecat_database.Database;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;


public class Server {
    public static class Options {
        public int port;
        public int threadCount;

        public Options(int port, int threadCount) {
            this.port = port;
            this.threadCount = threadCount;
        }
    }

    public static final String version = "0.0.0-Development";

    public int port;
    public int threadCount;
    public Database db;
    public ArrayList<ClientHandler> handlers = new ArrayList<>();


    public Server(Options options) throws NoSuchFieldException, IllegalAccessException, SQLException, IOException, ParseException {
        // Set the options provided in the record
        for (Field field : Options.class.getFields()) {
            Server.class.getField(field.getName()).set(this, field.get(options));
        }

        db = new Database();
    }

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException, SQLException, IOException, ParseException {
        // Get the options from the cli arguments
        int port = 1234;
        int threadCount = 10;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-p" -> {
                    i++;
                    port = Integer.parseInt(args[i]);
                }

                case "-t" -> {
                    i++;
                    threadCount = Integer.parseInt(args[i]);
                }

                default -> throw new RuntimeException("Invalid option " + args[i]);
            }
        }

        // Create the server instance from the options
        Server server = new Server(new Options(port, threadCount));
        server.start();
    }

    public void start() throws IOException {
        log("""
                ----- PeopleCat Server -----
                Version\s""" + version + """
                Developed by Nathcat 2023
                """);

        log("Starting up...");

        // This is a small worker thread which cleans the handler array by removing inactive handlers.
        Thread cleanerThread = new Thread(() -> {
            while (true) {
                for (int i = 0; i < handlers.size(); i++) {
                    if (!handlers.get(i).active) {
                        handlers.remove(i);
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        cleanerThread.setDaemon(true);
        cleanerThread.start();

        ServerSocket ss = new ServerSocket(port);

        log("Ready.");

        while (true) {
            // Accept a connection and pass it to a new handler thread
            Socket client = ss.accept();

            // Check if the server is allowed to accept any more connections
            if (handlers.size() >= threadCount) {
                OutputStream os = client.getOutputStream();
                os.write(Packet.createError("Server full", "The server cannot currently accept any more connections.").getBytes());
                os.flush();
                client.close();
                continue;
            }

            ClientHandler handler = new ClientHandler(this, client);
            handlers.add(handler);
        }
    }

    public void removeThread(int threadID) {
        for (ClientHandler handler : handlers) {
            if (handler.threadId() == threadID) {
                handlers.remove(handler);
            }
        }
    }

    public static void log(Object message) {
        System.out.println("Server: " + message);
    }

    public static String stringifyStackTrace(StackTraceElement[] st) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement e : st) {
            sb.append("\t").append(e.toString()).append("\n");
        }

        return sb.toString();
    }
}
