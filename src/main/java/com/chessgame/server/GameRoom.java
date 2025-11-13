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
    private String status; // "waiting", "playing", "finished"
    private String currentTurn; // "white" or "black"
    private long createdAt;
    private ChessValidator validator;
    private String isDrawOffered;
    private long initialTimeMs;
    private long whiteTimeMs;
    private long blackTimeMs;
    // ✅ THÊM CÁC THUỘC TÍNH TIMER
    private final NioWebSocketServer serverRef; // Tham chiếu đến ChessServer để gửi tin nhắn
    private transient ScheduledExecutorService timerService; // transient vì không cần serialize
    private transient ScheduledFuture<?> timerTask;
    private String rematchRequestedByColor = null;

    public GameRoom(String roomId, long initialTimeMs, NioWebSocketServer serverRef) {
        this.roomId = roomId;
        this.players = new CopyOnWriteArrayList<>(); // Thread-safe list
        this.status = "waiting";
        this.currentTurn = "white";
        this.createdAt = System.currentTimeMillis();
        this.validator = new ChessValidator();
        this.isDrawOffered = null;
        this.initialTimeMs = initialTimeMs;
        this.whiteTimeMs = initialTimeMs;
        this.blackTimeMs = initialTimeMs;
        this.serverRef = serverRef;
    }

    //HÀM BẮT ĐẦU TIMER (trong GameRoom)
    public synchronized void startTimer() {
        stopTimer(); // Dừng timer cũ (nếu có) trước khi bắt đầu mới
        if (!"playing".equals(status)) return; // Chỉ chạy khi đang chơi

        // Khởi tạo service nếu chưa có
        if (timerService == null || timerService.isShutdown()) {
            timerService = Executors.newSingleThreadScheduledExecutor();
        }

        System.out.println("Starting timer for room " + roomId); // DEBUG

        timerTask = timerService.scheduleAtFixedRate(() -> {
            try {
                // Kiểm tra lại trạng thái phòng trong task
                if (!"playing".equals(status) || serverRef == null) {
                    stopTimer();
                    return;
                }

                long timeDecrement = 1000;
                long newTime;
                String playerWithTurn = getCurrentTurn(); // Dùng getter

                if ("white".equals(playerWithTurn)) {
                    newTime = getWhiteTimeMs() - timeDecrement;
                    setWhiteTimeMs(newTime);
                } else {
                    newTime = getBlackTimeMs() - timeDecrement;
                    setBlackTimeMs(newTime);
                }

                // Kiểm tra hết giờ
                if (newTime <= 0) {
                    System.out.println("Time out detected in GameRoom task for " + playerWithTurn);
                    stopTimer(); // Dừng timer ngay

                    String winnerColor = "white".equals(playerWithTurn) ? "black" : "white";

                    // Gọi hàm xử lý hết giờ trên ChessServer
//                    serverRef.handleTimeout(this, winnerColor);
                }
            } catch (Exception e) {
                System.err.println("Lỗi trong timer task (GameRoom) phòng " + roomId + ": " + e.getMessage());
                e.printStackTrace();
                stopTimer(); // Dừng timer nếu có lỗi nghiêm trọng
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    // HÀM DỪNG TIMER (trong GameRoom)
    public synchronized void stopTimer() {
        if (timerTask != null && !timerTask.isDone()) {
            timerTask.cancel(false); // false để task hiện tại chạy xong nếu đang chạy
            System.out.println("Timer stopped for room " + roomId); // DEBUG
        }
        timerTask = null; // Reset task
        // Không shutdown service ngay nếu muốn dùng lại
        // if (timerService != null && !timerService.isShutdown()) {
        //     timerService.shutdown();
        // }
    }

    // HÀM DỌN DẸP HOÀN TOÀN TIMER SERVICE KHI PHÒNG BỊ HỦY
    public void shutdownTimerService() {
        stopTimer(); // Đảm bảo task đã dừng
        if (timerService != null && !timerService.isShutdown()) {
            timerService.shutdown();
            System.out.println("Timer service shut down for room " + roomId); // DEBUG
        }
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
    public void resetForRematch() {
        stopTimer(); // Dừng timer cũ trước khi reset
        validator.resetBoard();
        this.currentTurn = "white";
        this.status = "playing";
        this.isDrawOffered = null;
        this.whiteTimeMs = initialTimeMs;
        this.blackTimeMs = initialTimeMs;
        // Timer sẽ được start lại bởi ChessServer sau khi gửi game_start
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

    // Getters and Setters

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