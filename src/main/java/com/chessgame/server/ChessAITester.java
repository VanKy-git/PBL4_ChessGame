//package com.chessgame.server;
//
//import java.util.*;
//
///**
// * Chess AI Performance Tester - Đơn giản
// * Nhập nước đi → Test AI với tất cả các level
// */
//public class ChessAITester {
//
//    private ChessValidator validator;
//    private Scanner scanner;
//
//    public ChessAITester() {
//        this.scanner = new Scanner(System.in);
//    }
//
//    public void start() {
//        printWelcome();
//
//        // Khởi tạo bàn cờ
//        validator = new ChessValidator();
//
//        System.out.println("\n📋 BÀN CỜ BAN ĐẦU:");
//        printBoard();
//
//        // Cho phép người dùng nhập các nước đi để setup position
//        setupPosition();
//
//        // Test AI ở tất cả các level
//        testAllLevels();
//    }
//
//    private void printWelcome() {
//        System.out.println("╔═══════════════════════════════════════════════╗");
//        System.out.println("║     CHESS AI PERFORMANCE TESTER               ║");
//        System.out.println("║     Test AI với các mức độ khó khác nhau     ║");
//        System.out.println("╚═══════════════════════════════════════════════╝");
//    }
//
//    private void setupPosition() {
//        System.out.println("\n═════════════════════════════════════════════════");
//        System.out.println("SETUP POSITION");
//        System.out.println("═════════════════════════════════════════════════");
//        System.out.println("Nhập các nước đi để tạo position (cách nhau bởi dấu cách)");
//        System.out.println("Ví dụ: e2e4 e7e5 g1f3 b8c6");
//        System.out.println("Hoặc nhấn Enter để dùng position ban đầu");
//        System.out.print("\n➤ Nhập: ");
//
//        String input = scanner.nextLine().trim();
//
//        if (input.isEmpty()) {
//            System.out.println("✓ Sử dụng position ban đầu");
//            return;
//        }
//
//        String[] moves = input.split("\\s+");
//
//        for (String move : moves) {
//            if (move.length() < 4) {
//                System.out.println("✗ Nước đi không hợp lệ: " + move);
//                continue;
//            }
//
//            String from = move.substring(0, 2);
//            String to = move.substring(2, 4);
//            String currentTurn = validator.getCurrentTurn();
//
//            ChessValidator.MoveResult result = validator.validateMove(from, to, currentTurn);
//
//            if (result.isValid) {
//                System.out.println("  ✓ " + move);
//            } else {
//                System.out.println("  ✗ " + move + " - " + result.message);
//                return;
//            }
//        }
//
//        System.out.println("\n📋 POSITION SAU KHI SETUP:");
//        printBoard();
//    }
//
//    private void testAllLevels() {
//        System.out.println("\n═════════════════════════════════════════════════");
//        System.out.println("TESTING AI - TẤT CẢ CÁC MỨC ĐỘ");
//        System.out.println("═════════════════════════════════════════════════");
//        System.out.println("Lượt đi: " + validator.getCurrentTurn());
//        System.out.println("\nBắt đầu test...\n");
//
//        Computer.DifficultyLevel[] levels = Computer.DifficultyLevel.values();
//
//        // In header bảng
//        System.out.println("╔════════════════════════════════════════════════════════════════════════════════╗");
//        System.out.println("║                           TEST RESULTS                                         ║");
//        System.out.println("╠════════════════════════════════════════════════════════════════════════════════╣");
//        System.out.printf("║ %-12s │ Depth │  Move  │ Time(s) │   Nodes    │   NPS    │ Pruned   ║%n", "Level");
//        System.out.println("╠════════════════════════════════════════════════════════════════════════════════╣");
//
//        for (Computer.DifficultyLevel level : levels) {
//            testSingleLevel(level);
//        }
//
//        System.out.println("╚════════════════════════════════════════════════════════════════════════════════╝");
//        System.out.println("\n✓ Test hoàn tất!");
//    }
//
//    private void testSingleLevel(Computer.DifficultyLevel level) {
//        // Tạo bản sao validator để không ảnh hưởng đến test tiếp theo
//        ChessValidator testValidator = cloneValidator();
//        Computer computer = new Computer(testValidator, testValidator.getCurrentTurn(), level);
//
//        System.out.printf("║ Testing %-8s... ", level.getDisplayName());
//        System.out.flush();
//
//        long startTime = System.currentTimeMillis();
//        String bestMove = computer.getBestMove();
//        long elapsedTime = System.currentTimeMillis() - startTime;
//
//        Computer.SearchStatistics stats = computer.getStatistics();
//        long nps = elapsedTime > 0 ? (stats.nodesEvaluated * 1000 / elapsedTime) : 0;
//
//        // In kết quả
//        System.out.printf("\r║ %-12s │   %2d  │  %4s  │ %7.2f │ %,10d │ %,8d │ %,8d ║%n",
//                level.getDisplayName(),
//                stats.maxDepthReached,
//                bestMove != null ? bestMove : "N/A",
//                elapsedTime / 1000.0,
//                stats.nodesEvaluated,
//                nps,
//                stats.branchesPruned
//        );
//
//        // In chi tiết statistics
//        printDetailedStats(level, bestMove, stats, elapsedTime);
//    }
//
//    private void printDetailedStats(Computer.DifficultyLevel level, String move,
//                                    Computer.SearchStatistics stats, long elapsedTime) {
//        System.out.println("║                                                                                ║");
//        System.out.printf("║   └─ Cache: %,d hits / %,d total (%.1f%% hit rate)                     ║%n",
//                stats.cacheHits,
//                stats.cacheHits + stats.cacheMisses,
//                (stats.cacheHits + stats.cacheMisses) > 0 ?
//                        100.0 * stats.cacheHits / (stats.cacheHits + stats.cacheMisses) : 0
//        );
//
//        double pruningRate = stats.nodesEvaluated > 0 ?
//                100.0 * stats.branchesPruned / stats.nodesEvaluated : 0;
//        System.out.printf("║   └─ Pruning efficiency: %.1f%% of nodes cut                           ║%n",
//                pruningRate);
//        System.out.println("║                                                                                ║");
//        System.out.println("╠════════════════════════════════════════════════════════════════════════════════╣");
//    }
//
//    private void printBoard() {
//        char[][] board = validator.getBoard();
//        System.out.println("\n  ┌───┬───┬───┬───┬───┬───┬───┬───┐");
//
//        for (int r = 0; r < 8; r++) {
//            System.out.print((8 - r) + " │");
//            for (int c = 0; c < 8; c++) {
//                char piece = board[r][c];
//                String symbol = getPieceSymbol(piece);
//                System.out.print(" " + symbol + " │");
//            }
//            System.out.println();
//            if (r < 7) System.out.println("  ├───┼───┼───┼───┼───┼───┼───┼───┤");
//        }
//
//        System.out.println("  └───┴───┴───┴───┴───┴───┴───┴───┘");
//        System.out.println("    a   b   c   d   e   f   g   h");
//    }
//
//    private String getPieceSymbol(char piece) {
//        return switch (piece) {
//            case 'K' -> "♔";
//            case 'Q' -> "♕";
//            case 'R' -> "♖";
//            case 'B' -> "♗";
//            case 'N' -> "♘";
//            case 'P' -> "♙";
//            case 'k' -> "♚";
//            case 'q' -> "♛";
//            case 'r' -> "♜";
//            case 'b' -> "♝";
//            case 'n' -> "♞";
//            case 'p' -> "♟";
//            default -> " ";
//        };
//    }
//
//    private ChessValidator cloneValidator() {
//        ChessValidator clone = new ChessValidator();
//        String fen = validator.toFen();
//        clone.setFromFen(fen);
//        return clone;
//    }
//
//    public static void main(String[] args) {
//        ChessAITester tester = new ChessAITester();
//        tester.start();
//    }
//}