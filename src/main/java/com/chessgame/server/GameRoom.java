package com.chessgame.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.Map; // Thêm import
import java.util.HashMap; // Thêm import
import java.util.concurrent.*;

public class GameRoom {
    private String roomId;
    private List<Player> players;
    private List<Player> spectators; // Danh sách người xem
    private String status; // "waiting", "playing", "finished"
    private String currentTurn; // "white" or "black"
    private long createdAt;
    private ChessValidator validator;
    private String isDrawOffered;
    private long initialTimeMs;
    private long whiteTimeMs;
    private long blackTimeMs;
    private StockfishEngine stockfishEngine;
    private final NioWebSocketServer serverRef; 
    private transient ScheduledExecutorService timerService;
    private transient ScheduledFuture<?> timerTask;
    private String rematchRequestedByColor = null;
    private final List<Map<String, Object>> moveHistory = new CopyOnWriteArrayList<>();
    private final List<String> fenHistory = new CopyOnWriteArrayList<>();


    public GameRoom(String roomId, long initialTimeMs, NioWebSocketServer serverRef) {
        this.roomId = roomId;
        this.players = new CopyOnWriteArrayList<>(); // Thread-safe list
        this.spectators = new CopyOnWriteArrayList<>(); // Thread-safe list for spectators
        this.status = "waiting";
        this.currentTurn = "white";
        this.createdAt = System.currentTimeMillis();
        this.validator = new ChessValidator();
        this.isDrawOffered = null;
        this.initialTimeMs = initialTimeMs;
        this.whiteTimeMs = initialTimeMs;
        this.blackTimeMs = initialTimeMs;
        this.serverRef = serverRef;
        this.fenHistory.add(this.validator.toFen()); // Add initial FEN
    }

    public synchronized void startTimer() {
        stopTimer(); 
        if (!"playing".equals(status)) return; 

        if (timerService == null || timerService.isShutdown()) {
            timerService = Executors.newSingleThreadScheduledExecutor();
        }

        System.out.println("Starting timer for room " + roomId); 

        timerTask = timerService.scheduleAtFixedRate(() -> {
            try {
                if (!"playing".equals(status) || serverRef == null) {
                    stopTimer();
                    return;
                }

                long timeDecrement = 1000;
                long newTime;
                String playerWithTurn = getCurrentTurn(); 

                if ("white".equals(playerWithTurn)) {
                    newTime = getWhiteTimeMs() - timeDecrement;
                    setWhiteTimeMs(newTime);
                } else {
                    newTime = getBlackTimeMs() - timeDecrement;
                    setBlackTimeMs(newTime);
                }

                if (newTime <= 0) {
                    System.out.println("Time out detected in GameRoom task for " + playerWithTurn);
                    stopTimer(); 

                    String winnerColor = "white".equals(playerWithTurn) ? "black" : "white";
                    serverRef.handleTimeout(this, winnerColor);
                }
            } catch (Exception e) {
                System.err.println("Lỗi trong timer task (GameRoom) phòng " + roomId + ": " + e.getMessage());
                e.printStackTrace();
                stopTimer(); 
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    public synchronized void stopTimer() {
        if (timerTask != null && !timerTask.isDone()) {
            timerTask.cancel(false);
            System.out.println("Timer stopped for room " + roomId);
        }
        timerTask = null;
    }

    public void shutdownTimerService() {
        stopTimer(); 
        if (timerService != null && !timerService.isShutdown()) {
            timerService.shutdown();
            System.out.println("Timer service shut down for room " + roomId);
        }
        if (stockfishEngine != null) {
            stockfishEngine.close();
            System.out.println("Đã tắt Stockfish cho phòng " + roomId);
        }
    }

    public void addPlayer(Player player) {
        if (players.size() < 2) {
            players.add(player);
        }
    }

    public void addSpectator(Player player) {
        spectators.add(player);
    }

    public void removeSpectator(Player player) {
        spectators.remove(player);
    }

    public List<Player> getSpectators() {
        return new ArrayList<>(spectators);
    }

    public void resetForRematch() {
        stopTimer(); 
        validator.resetBoard();
        this.currentTurn = "white";
        this.status = "playing";
        this.isDrawOffered = null;
        this.whiteTimeMs = initialTimeMs;
        this.blackTimeMs = initialTimeMs;
        this.moveHistory.clear();
        this.fenHistory.clear();
        this.fenHistory.add(this.validator.toFen());
    }

    public synchronized void swapPlayerColors() {
        if (players.size() == 2) {
            Player p1 = players.get(0);
            Player p2 = players.get(1);
            String p1OldColor = p1.getColor();
            p1.setColor(p2.getColor());
            p2.setColor(p1OldColor);
        }
    }

    public synchronized ChessValidator getValidator() {
        return validator;
    }

    public void removePlayer(Player player) {
        players.remove(player);
        if (isEmpty()) {
            this.status = "finished";
        }
    }
    public synchronized String getRematchRequestedByColor() {
        return rematchRequestedByColor;
    }

    public synchronized void setRematchRequestedByColor(String color) {
        this.rematchRequestedByColor = color;
    }

    public synchronized boolean isFull() {
        return players.size() >= 2;
    }

    public synchronized boolean isEmpty() {
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

    public void addMoveToHistory(Map<String, Object> moveData) {
        this.moveHistory.add(moveData);
        this.fenHistory.add((String) moveData.get("fen"));
    }

    public List<Map<String, Object>> getMoveHistory() {
        return this.moveHistory;
    }

    public boolean takeBackMove() {
        // Can only take back if it's a game against AI and there are at least 2 moves (player + AI)
        if (stockfishEngine == null || fenHistory.size() < 3) {
            return false;
        }

        // Remove last 2 FENs (player's move and AI's move)
        fenHistory.remove(fenHistory.size() - 1);
        fenHistory.remove(fenHistory.size() - 1);
        
        // Remove last 2 moves from move history
        moveHistory.remove(moveHistory.size() - 1);
        moveHistory.remove(moveHistory.size() - 1);

        // Restore the board to the state before the player's last move
        String previousFen = fenHistory.get(fenHistory.size() - 1);
        validator.setFromFen(previousFen);
        
        // Update current turn from the restored FEN
        this.currentTurn = validator.getCurrentTurn();

        return true;
    }

    // Getters and Setters


    public StockfishEngine getStockfishEngine() {
        return stockfishEngine;
    }

    public void setStockfishEngine(StockfishEngine stockfishEngine) {
        this.stockfishEngine = stockfishEngine;
    }

    public synchronized long getInitialTimeMs() { return initialTimeMs; }

    public synchronized String getIsDrawOffered() {
        return isDrawOffered;
    }

    public synchronized void setIsDrawOffered(String isDrawOffered) {
        this.isDrawOffered = isDrawOffered;
    }

    public synchronized String getRoomId() {
        return roomId;
    }

    public synchronized void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public synchronized List<Player> getPlayers() {
        return new ArrayList<>(players); // Return copy to prevent external modification
    }

    public synchronized String getStatus() {
        return status;
    }

    public synchronized void setStatus(String status) {
        this.status = status;
    }

    public synchronized String getCurrentTurn() {
        return currentTurn;
    }

    public synchronized void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public synchronized long getCreatedAt() {
        return createdAt;
    }

    public synchronized void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public synchronized long getWhiteTimeMs() { return whiteTimeMs; }
    public synchronized void setWhiteTimeMs(long whiteTimeMs) { this.whiteTimeMs = whiteTimeMs; }
    public synchronized long getBlackTimeMs() { return blackTimeMs; }
    public synchronized void setBlackTimeMs(long blackTimeMs) { this.blackTimeMs = blackTimeMs; }
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
