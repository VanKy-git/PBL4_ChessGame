package com.chessgame.server;

import java.io.IOException;

/**
 * TEST STOCKFISH ENGINE
 * Chạy file này để test xem Stockfish có hoạt động không
 */
public class TestStockfish {

    public static void main(String[] args) {
        System.out.println("=== TEST STOCKFISH ENGINE ===\n");

        // Đường dẫn Stockfish (thay đổi theo HĐH của bạn)
        String stockfishPath = getStockfishPath();
        System.out.println("Stockfish path: " + stockfishPath);

        try {
            // 1. Khởi tạo Stockfish
            System.out.println("\n[1] Khởi tạo Stockfish...");
            StockfishEngine engine = new StockfishEngine(stockfishPath);
            System.out.println("✅ Stockfish khởi tạo thành công!");

            // 2. Test với vị trí ban đầu
            System.out.println("\n[2] Test vị trí ban đầu...");
            String startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
            engine.setPosition(startFen);
            System.out.println("✅ Đã set vị trí ban đầu");

            // 3. Tính nước đi tốt nhất (1 giây)
            System.out.println("\n[3] Tính nước đi tốt nhất (1000ms)...");
            long startTime = System.currentTimeMillis();
            String bestMove = engine.getBestMove(1000);
            long endTime = System.currentTimeMillis();

            System.out.println("✅ Nước đi tốt nhất: " + bestMove);
            System.out.println("   Thời gian: " + (endTime - startTime) + "ms");

            // 4. Test với các mức độ Elo khác nhau
            System.out.println("\n[4] Test các mức độ Elo...");

            testEloLevel(engine, startFen, 1350, "EASY");
            testEloLevel(engine, startFen, 1800, "MEDIUM");
            testEloLevel(engine, startFen, 2200, "HARD");
            testEloLevel(engine, startFen, 2850, "EXPERT");

            // 5. Test với vị trí khác
            System.out.println("\n[5] Test vị trí giữa ván...");
            String midGameFen = "rnbqkb1r/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 4 3";
            engine.setPosition(midGameFen);
            bestMove = engine.getBestMove(1000);
            System.out.println("✅ Nước đi từ vị trí giữa ván: " + bestMove);

            // 6. Đóng engine
            System.out.println("\n[6] Đóng Stockfish...");
            engine.close();
            System.out.println("✅ Đã đóng Stockfish");

            System.out.println("\n=== TEST HOÀN TẤT THÀNH CÔNG! ===");

        } catch (IOException e) {
            System.err.println("\n❌ LỖI: " + e.getMessage());
            e.printStackTrace();
            System.err.println("\nKiểm tra lại:");
            System.err.println("1. File Stockfish có tồn tại tại: " + stockfishPath);
            System.err.println("2. File có quyền thực thi (chmod +x trên Linux/Mac)");
            System.err.println("3. Đường dẫn có đúng không");
        } catch (Exception e) {
            System.err.println("\n❌ LỖI KHÔNG XÁC ĐỊNH: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test một mức độ Elo
     */
    private static void testEloLevel(StockfishEngine engine, String fen, int elo, String levelName) {
        try {
            engine.setElo(elo);
            engine.setPosition(fen);

            long startTime = System.currentTimeMillis();
            String bestMove = engine.getBestMove(500); // 500ms
            long endTime = System.currentTimeMillis();

            System.out.println(String.format(
                    "   %s (Elo %d): %s (%dms)",
                    levelName, elo, bestMove, (endTime - startTime)
            ));

        } catch (IOException e) {
            System.err.println("   ❌ Lỗi test " + levelName + ": " + e.getMessage());
        }
    }

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
}