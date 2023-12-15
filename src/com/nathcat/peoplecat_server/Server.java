package com.nathcat.peoplecat_server;

import com.nathcat.peoplecat_database.Database;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;


public class Server {
    public record Options(int port, int threadCount) {}

    public static final String version = "0.0.0-Development";

    public int port;
    public int threadCount;
    public Database db;
    public ArrayList<ClientHandler> handlers = new ArrayList<>();


    public Server(Options options) throws NoSuchFieldException, IllegalAccessException, SQLException, FileNotFoundException, ParseException {
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

        ServerSocket ss = new ServerSocket(1234);

        log("Ready.");

        while (true) {
            Socket client = ss.accept();

            ClientHandler handler = new ClientHandler(this, client);
            handler.start();
            handlers.add(handler);
        }
    }

    public static void log(Object message) {
        System.out.println("Server: " + message);
    }
}
