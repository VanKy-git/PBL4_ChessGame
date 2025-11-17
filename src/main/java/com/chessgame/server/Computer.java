package com.chessgame.server;

import java.io.IOException;
import java.util.*;

public class Computer {

    /* ===== THUỘC TÍNH ===== */

    private final String roomId;
    private final Player humanPlayer;
    private final DifficultyLevel difficulty;
    private final ChessValidator validator;
    private final NioWebSocketServer server;
    private final StockfishEngine stockfish;
    private String status;
    private boolean isPlayerWhite;

    /* ===== ĐƯỜNG DẪN STOCKFISH ===== */
    private static final String STOCKFISH_PATH = getStockfishPath();

//    private static String getStockfishPath() {
//        String os = System.getProperty("os.name").toLowerCase();
//        if (os.contains("win")) {
//            return "resources/stockfish/stockfish-windows.exe";
//        } else if (os.contains("mac")) {
//            return "resources/stockfish/stockfish-mac";
//        } else {
//            return "resources/stockfish/stockfish-linux";
//        }
//    }

    private static String getStockfishPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String projectRoot = System.getProperty("user.dir");

        String fileName;
        if (os.contains("win")) {
            fileName = "stockfish-windows.exe";
        } else if (os.contains("mac")) {
            fileName = "stockfish-mac";
        } else {
            fileName = "stockfish-linux";
        }

        // Thử tìm trong src/main/resources trước (Maven structure)
        String mavenPath = projectRoot + "/src/main/resources/stockfish/" + fileName;
        java.io.File mavenFile = new java.io.File(mavenPath);

        if (mavenFile.exists()) {
            return mavenPath;
        }

        // Nếu không có, tìm trong resources (project root)
        return projectRoot + "/resources/stockfish/" + fileName;
    }

    /* ===== ENUM MỨC ĐỘ KHÓ ===== */

    public enum DifficultyLevel {
        EASY(1350, 500, "Dễ"),           // Elo 1350, 0.5s
        MEDIUM(1800, 1000, "Trung bình"), // Elo 1800, 1s
        HARD(2200, 2000, "Khó"),          // Elo 2200, 2s
        EXPERT(2850, 3000, "Chuyên gia"); // Elo 2850, 3s

        private final int eloRating;
        private final int thinkTimeMs;
        private final String displayName;

        DifficultyLevel(int elo, int timeMs, String name) {
            this.eloRating = elo;
            this.thinkTimeMs = timeMs;
            this.displayName = name;
        }

        public int getEloRating() { return eloRating; }
        public int getThinkTimeMs() { return thinkTimeMs; }
        public String getDisplayName() { return displayName; }
    }

    /* ===== CONSTRUCTOR ===== */

    public Computer(String roomId, Player humanPlayer, DifficultyLevel difficulty,
                    boolean isPlayerWhite, NioWebSocketServer server) {
        this.roomId = roomId;
        this.humanPlayer = humanPlayer;
        this.difficulty = difficulty;
        this.isPlayerWhite = isPlayerWhite;
        this.server = server;
        this.validator = new ChessValidator();
        this.status = "waiting";

        humanPlayer.setColor(isPlayerWhite ? "white" : "black");

        // Khởi tạo Stockfish Engine
        try {
            this.stockfish = new StockfishEngine(STOCKFISH_PATH);
            this.stockfish.setElo(difficulty.getEloRating());
            this.stockfish.setThreads(2);
            this.stockfish.setHashSize(128);

            System.out.println(String.format(
                    "[Computer Room %s] Tạo phòng: %s (%s) vs Stockfish (Elo %d - %s)",
                    roomId, humanPlayer.getPlayerName(),
                    humanPlayer.getColor(), difficulty.getEloRating(), difficulty.getDisplayName()
            ));
        } catch (IOException e) {
            System.err.println("[Computer Room] Lỗi khởi tạo Stockfish: " + e.getMessage());
            throw new RuntimeException("Không thể khởi động Stockfish Engine", e);
        }
    }

    /* ===== BẮT ĐẦU GAME ===== */

    public void startGame() {
        this.status = "playing";
        this.validator.resetBoard();

        try {
            this.stockfish.resetPosition();
        } catch (IOException e) {
            System.err.println("[Computer Room] Lỗi reset Stockfish: " + e.getMessage());
        }

        System.out.println(String.format(
                "[Computer Room %s] Game bắt đầu! %s vs Stockfish (%s)",
                roomId, humanPlayer.getPlayerName(), difficulty.getDisplayName()
        ));

        Map<String, Object> gameStartData = new HashMap<>();
        gameStartData.put("gameState", validator.toFen());
        gameStartData.put("currentTurn", "white");
        gameStartData.put("roomId", roomId);
        gameStartData.put("playerColor", humanPlayer.getColor());
        gameStartData.put("aiLevel", difficulty.getDisplayName());
        gameStartData.put("playerWhite", Map.of(
                "id", humanPlayer.getPlayerId(),
                "name", humanPlayer.getPlayerName()
        ));
        gameStartData.put("playerBlack", Map.of(
                "id", "ai",
                "name", "Stockfish (" + difficulty.getDisplayName() + ")"
        ));

        server.sendMessage(humanPlayer.getConnection(), Map.of(
                "type", "game_start",
                "data", gameStartData
        ));

        // Nếu AI đi trước (người chơi cầm đen)
        if (!isPlayerWhite) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    makeAIMove();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /* ===== NHẬN NƯỚC ĐI TỪ NGƯỜI CHƠI ===== */

    /**
     * Nhận nước đi từ người chơi (algebraic notation: e2, e4,...)
     * Check hợp lệ bằng ChessValidator API
     * Nếu hợp lệ, gọi AI đi tiếp
     */
    public void receivePlayerMove(String from, String to, Character promotion) {
        System.out.println(String.format(
                "[Computer Room %s] Nhận nước đi: %s -> %s",
                roomId, from, to
        ));

        if (!"playing".equals(status)) {
            sendError("Game không ở trạng thái đang chơi!");
            return;
        }

        if (!validator.getCurrentTurn().equals(humanPlayer.getColor())) {
            sendError("Không phải lượt của bạn!");
            return;
        }

        // Validate và thực hiện nước đi bằng ChessValidator API
        ChessValidator.MoveResult moveResult = validator.validateMove(
                from, to, humanPlayer.getColor(), promotion
        );

        if (!moveResult.isValid) {
            server.sendMessage(humanPlayer.getConnection(), Map.of(
                    "type", "move_result",
                    "result", false,
                    "message", moveResult.message
            ));
            return;
        }

        // Nước đi hợp lệ
        String aiColor = isPlayerWhite ? "black" : "white";
        String fen = validator.toFen();
        boolean isAIInCheck = validator.isKingInCheck(aiColor, validator.getBoard());

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("result", true);
        resultData.put("fen", fen);
        resultData.put("lastMove", Map.of("from", from, "to", to));
        resultData.put("isCheck", isAIInCheck);

        server.sendMessage(humanPlayer.getConnection(), Map.of(
                "type", "move_result",
                "data", resultData
        ));

        // Kiểm tra game over
        if (moveResult.winner != null) {
            handleGameOver(moveResult.winner, "checkmate");
            return;
        }

        // AI đi tiếp (delay 500ms)
        new Thread(() -> {
            try {
                Thread.sleep(500);
                makeAIMove();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /* ===== AI TÍNH TOÁN VÀ ĐI (STOCKFISH) ===== */

    /**
     * AI tính toán và trả về nước đi cho server
     * Sử dụng Stockfish để tính nước đi tốt nhất
     * Trả về nước đi dạng algebraic notation (e2, e4,...)
     */
    private void makeAIMove() {
        long startTime = System.currentTimeMillis();

        String aiColor = isPlayerWhite ? "black" : "white";
        System.out.println(String.format(
                "[Computer Room %s] Stockfish (%s) đang suy nghĩ... (Elo: %d)",
                roomId, aiColor, difficulty.getEloRating()
        ));

        try {
            // Cập nhật vị trí hiện tại cho Stockfish
            String currentFen = validator.toFen();
            stockfish.setPosition(currentFen);

            // Lấy nước đi tốt nhất từ Stockfish (UCI format: e2e4, e7e8q)
            String bestMoveUCI = stockfish.getBestMove(difficulty.getThinkTimeMs());

            if (bestMoveUCI == null || bestMoveUCI.isEmpty()) {
                System.err.println("[AI ERROR] Stockfish không trả về nước đi");
                handleGameOver(humanPlayer.getColor(), "ai_error");
                return;
            }

            long endTime = System.currentTimeMillis();
            double thinkingTime = (endTime - startTime) / 1000.0;

            // Chuyển đổi UCI move (e2e4) sang algebraic notation (from: e2, to: e4)
            String from = bestMoveUCI.substring(0, 2); // e2
            String to = bestMoveUCI.substring(2, 4);   // e4

            Character promotion = null;
            if (bestMoveUCI.length() == 5) {
                promotion = bestMoveUCI.charAt(4); // q, r, b, n
            }

            // Thực hiện nước đi bằng ChessValidator API
            ChessValidator.MoveResult moveResult = validator.validateMove(
                    from, to, aiColor, promotion
            );

            if (!moveResult.isValid) {
                System.err.println("[AI ERROR] Nước đi Stockfish không hợp lệ: " + bestMoveUCI);
                handleGameOver(humanPlayer.getColor(), "ai_error");
                return;
            }

            System.out.println(String.format(
                    "[Computer Room %s] Stockfish đã đi: %s->%s (%.2fs)",
                    roomId, from, to, thinkingTime
            ));

            String fen = validator.toFen();
            boolean isPlayerInCheck = validator.isKingInCheck(
                    humanPlayer.getColor(), validator.getBoard()
            );

            Map<String, Object> aiMoveData = new HashMap<>();
            aiMoveData.put("result", true);
            aiMoveData.put("fen", fen);
            aiMoveData.put("lastMove", Map.of("from", from, "to", to));
            aiMoveData.put("isCheck", isPlayerInCheck);
            aiMoveData.put("aiThinkingTime", thinkingTime);

            server.sendMessage(humanPlayer.getConnection(), Map.of(
                    "type", "ai_move",
                    "data", aiMoveData
            ));

            if (moveResult.winner != null) {
                handleGameOver(moveResult.winner, "checkmate");
            }

        } catch (IOException e) {
            System.err.println("[AI ERROR] Lỗi giao tiếp với Stockfish: " + e.getMessage());
            handleGameOver(humanPlayer.getColor(), "ai_error");
        }
    }

    /* ===== XỬ LÝ KẾT THÚC GAME ===== */

    private void handleGameOver(String winner, String reason) {
        status = "finished";

        System.out.println(String.format(
                "[Computer Room %s] Game kết thúc! Winner: %s, Reason: %s",
                roomId, winner, reason
        ));

        Map<String, Object> endData = new HashMap<>();
        endData.put("winner", winner);
        endData.put("reason", reason);

        server.sendMessage(humanPlayer.getConnection(), Map.of(
                "type", "end_game",
                "data", endData
        ));

        // Đóng Stockfish engine
        cleanup();
    }

    private void sendError(String message) {
        server.sendMessage(humanPlayer.getConnection(), Map.of(
                "type", "error",
                "message", message
        ));
    }

    /* ===== CLEANUP ===== */

    public void cleanup() {
        if (stockfish != null && stockfish.isRunning()) {
            stockfish.close();
            System.out.println("[Computer Room " + roomId + "] Đã đóng Stockfish engine");
        }
    }

    /* ===== GETTER ===== */

    public String getRoomId() { return roomId; }
    public String getStatus() { return status; }
    public Player getHumanPlayer() { return humanPlayer; }

    /**
     * Người chơi đầu hàng
     */
    public void resign() {
        String aiColor = isPlayerWhite ? "black" : "white";
        handleGameOver(aiColor, "resignation");
    }

    /**
     * Lấy các nước đi hợp lệ cho một ô (sử dụng ChessValidator API)
     */
    public List<String> getValidMovesForSquare(String square) {
        return validator.getValidMovesForSquare(square);
    }
}