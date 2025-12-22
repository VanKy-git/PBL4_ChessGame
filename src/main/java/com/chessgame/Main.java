package com.chessgame;

import com.chessgame.server.NioWebSocketServer;
import com.database.server.MainApiServer;

public class Main {
    public static void main(String[] args) {
        System.out.println("Attempting to start all servers...");

        // Start the HTTP API server for login, registration, etc.
        // This runs on port 8910 by default.
        try {
            MainApiServer.start();
            System.out.println("HTTP API server has been initiated.");
        } catch (Exception e) {
            System.err.println("Failed to start HTTP API server.");
            e.printStackTrace();
        }

        // Start the WebSocket server for real-time game communication.
        // This runs on port 8080 by default.
        try {
            // NioWebSocketServer implements Runnable, so we start it in a new thread
            NioWebSocketServer server = new NioWebSocketServer(8080);
            new Thread(server).start();
            System.out.println("WebSocket server has been initiated.");
        } catch (Exception e) {
            System.err.println("Failed to start WebSocket server.");
            e.printStackTrace();
        }

        System.out.println("Server startup process complete.");
    }
}
