package com.chatapp.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameRoom {
    private String roomId;
    private List<Player> players;
    private String status; // "waiting", "playing", "finished"
    private String currentTurn; // "white" or "black"
    private String gameState; // Chess board state (could be FEN notation or custom format)
    private long createdAt;
    private ChessValidator validator;

    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.players = new CopyOnWriteArrayList<>(); // Thread-safe list
        this.status = "waiting";
        this.currentTurn = "white";
        this.gameState = getInitialChessState();
        this.createdAt = System.currentTimeMillis();
        this.validator = new ChessValidator();
    }

    private String getInitialChessState() {
        // Return initial chess board state
        // This could be FEN notation: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        // Or a custom JSON representation of the board
        return "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    }

    public void addPlayer(Player player) {
        if (players.size() < 2) {
            players.add(player);

            // Assign color based on position
            if (players.size() == 1) {
                player.setColor("white");
            } else if (players.size() == 2) {
                player.setColor("black");
            }
        }
    }

    public ChessValidator getValidator() {
        return validator;
    }

    public void removePlayer(Player player) {
        players.remove(player);
        if (isEmpty()) {
            this.status = "finished";
        }
    }

    public boolean isFull() {
        return players.size() >= 2;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public Player getPlayerByColor(String color) {
        return players.stream()
                .filter(player -> color.equals(player.getColor()))
                .findFirst()
                .orElse(null);
    }

    public Player getOpponent(Player player) {
        return players.stream()
                .filter(p -> !p.equals(player))
                .findFirst()
                .orElse(null);
    }

    // Getters and Setters
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players); // Return copy to prevent external modification
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public String getGameState() {
        return gameState;
    }

    public void setGameState(String gameState) {
        this.gameState = gameState;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "GameRoom{" +
                "roomId='" + roomId + '\'' +
                ", players=" + players.size() +
                ", status='" + status + '\'' +
                ", currentTurn='" + currentTurn + '\'' +
                '}';
    }
}