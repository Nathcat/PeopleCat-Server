package com.nathcat.peoplecat_server;

import java.lang.reflect.Field;


public class Server {
    public record Options(int port, int threadCount) {}

    public static final String version = "0.0.0-Development";

    public int port;
    public int threadCount;


    public Server(Options options) throws NoSuchFieldException, IllegalAccessException {
        // Set the options provided in the record
        for (Field field : Options.class.getFields()) {
            Server.class.getField(field.getName()).set(this, field.get(options));
        }
    }

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
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

    public void start() {
        log("""
                ----- PeopleCat Server -----
                Version\s""" + version + """
                Developed by Nathcat 2023
                """);

        log("Starting up...");
    }

    public static void log(Object message) {
        System.out.println("Server: " + message);
    }
}
