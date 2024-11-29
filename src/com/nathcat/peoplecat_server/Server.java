package com.nathcat.peoplecat_server;

import com.nathcat.peoplecat_database.Database;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;


public class Server {
    public static class Options {
        public int port;
        public int threadCount;
        public boolean useSSL;

        public Options(int port, int threadCount, boolean useSSL) {
            this.port = port;
            this.threadCount = threadCount;
            this.useSSL = useSSL;
        }
    }

    public class HandlerCleanerThread extends Thread {
        private final boolean livingHandlersMode;

        /**
         * Create a new Handler cleaning thread
         * @param livingHandlersMode Overrides the conditions which check if a handler's thread is actually running.
         *                           Will be used mainly with the <code>WebSocketHandler</code> since this does not allow
         *                           <code>ClientHandlers</code> to run in their own thread.
         */
        public HandlerCleanerThread(boolean livingHandlersMode) {
            this.setDaemon(true);
            this.livingHandlersMode = livingHandlersMode;
        }

        /**
         * Default constructor, sets <code>livingHandlerMode</code> to <code>true</code>.
         */
        public HandlerCleanerThread() {
            this.setDaemon(true);
            this.livingHandlersMode = true;
        }

        @Override
        public void run() {
            while (true) {
                for (int i = 0; i < handlers.size(); i++) {
                    if (!handlers.get(i).active || ((!handlers.get(i).isAlive() || handlers.get(i).isInterrupted()) && livingHandlersMode)) {
                        handlers.remove(i);
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static final String version = "4.2.0";

    public int port;
    public int threadCount;
    public boolean useSSL;
    public Database db;
    public ArrayList<ClientHandler> handlers = new ArrayList<>();
    /**
     * Maps a user's ID to their connected handler
     */
    public HashMap<Integer, ClientHandler> userToHandler = new HashMap<>();
    private Thread handlerCleaner;


    public Server(Options options) throws NoSuchFieldException, IllegalAccessException, SQLException, IOException, ParseException {
        // Set the options provided in the record
        for (Field field : Options.class.getFields()) {
            Server.class.getField(field.getName()).set(this, field.get(options));
        }

        db = new Database();
    }

    public static Options getOptions(String[] args) {
        int port = 1234;
        int threadCount = 10;
        boolean useSSL = true;

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

                case "--no-ssl" -> {
                    i++;
                    useSSL = false;
                }

                default -> throw new RuntimeException("Invalid option " + args[i]);
            }
        }

        return new Options(port, threadCount, useSSL);
    }

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException, SQLException, IOException, ParseException {
        // Create the server instance from the options
        Server server = new Server(getOptions(args));
        server.start();
    }

    /**
     * Starts the server
     * @throws IOException thrown by failing I/O operations
     */
    public void start() throws IOException {
        log("""
                ----- PeopleCat Server -----
                Version\s""" + version + """
                \nDeveloped by Nathcat 2024
                """);

        log("Starting up...");

        // This is a small worker thread which cleans the handler array by removing inactive handlers.
        Thread cleanerThread = new HandlerCleanerThread();
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

    public void startCleaner(boolean livingHandlerMode) {
        handlerCleaner = new HandlerCleanerThread(livingHandlerMode);
        handlerCleaner.start();
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
