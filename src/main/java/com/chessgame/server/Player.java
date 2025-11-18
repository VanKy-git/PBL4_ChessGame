package com.chessgame.server;

import org.java_websocket.WebSocket;

import java.nio.channels.SocketChannel;

public class Player {
    private String playerId;
    private String playerName;
    private String color;
    private SocketChannel connection;
    private Long preferredTimeMs;

    public Player(String playerId, String playerName, SocketChannel connection) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.connection = connection;
        this.color = null; // Will be set when joining a room
    }

    // Getters and Setters
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public SocketChannel getConnection() {
        return connection;
    }

    public void setConnection(SocketChannel connection) {
        this.connection = connection;
    }

    public Long getPreferredTimeMs() {
        return preferredTimeMs;
    }

    public void setPreferredTimeMs(Long preferredTimeMs) {
        this.preferredTimeMs = preferredTimeMs;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Player player = (Player) obj;
        return playerId.equals(player.playerId);
    }

    @Override
    public int hashCode() {
        return playerId.hashCode();
    }

    @Override
    public String toString() {
        return "Player{" +
                "playerId='" + playerId + '\'' +
                ", playerName='" + playerName + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}