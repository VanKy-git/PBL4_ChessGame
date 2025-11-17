package com.chessgame.server;

import java.io.*;

/**
 * CLASS STOCKFISH ENGINE - Giao tiếp với Stockfish qua UCI Protocol
 * Không sửa ChessValidator, chỉ giao tiếp với Stockfish engine
 */
public class StockfishEngine {

    private Process process;
    private BufferedReader reader;
    private OutputStreamWriter writer;
    private boolean isInitialized = false;

    /**
     * Khởi tạo Stockfish Engine
     * @param stockfishPath Đường dẫn đến file Stockfish executable
     */
    public StockfishEngine(String stockfishPath) throws IOException {
        try {
            // Khởi động process Stockfish
            ProcessBuilder pb = new ProcessBuilder(stockfishPath);
            process = pb.start();

            // Setup input/output streams
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new OutputStreamWriter(process.getOutputStream());

            // Khởi tạo UCI
            sendCommand("uci");

            // Đợi phản hồi "uciok"
            String line;
            while ((line = readLine()) != null) {
                if (line.equals("uciok")) {
                    isInitialized = true;
                    break;
                }
            }

            if (!isInitialized) {
                throw new IOException("Không thể khởi tạo Stockfish UCI");
            }

            // Setup các tùy chọn mặc định
            sendCommand("setoption name UCI_AnalyseMode value false");
            sendCommand("isready");
            waitForReady();

            System.out.println("[Stockfish] Engine khởi tạo thành công");

        } catch (IOException e) {
            System.err.println("[Stockfish] Lỗi khởi tạo: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Thiết lập vị trí bàn cờ từ FEN
     */
    public void setPosition(String fen) throws IOException {
        sendCommand("position fen " + fen);
    }

    /**
     * Tính nước đi tốt nhất với thời gian giới hạn
     * @param timeMs Thời gian suy nghĩ tối đa (milliseconds)
     * @return Nước đi dạng UCI (ví dụ: "e2e4", "e7e8q")
     */
    public String getBestMove(int timeMs) throws IOException {
        sendCommand("go movetime " + timeMs);

        String line;
        String bestMove = null;

        while ((line = readLine()) != null) {
            if (line.startsWith("bestmove")) {
                String[] parts = line.split(" ");
                if (parts.length >= 2) {
                    bestMove = parts[1];
                }
                break;
            }
        }

        return bestMove;
    }

    /**
     * Thiết lập mức độ Elo (độ mạnh của engine)
     * @param elo Elo rating (1350-2850)
     */
    public void setElo(int elo) throws IOException {
        // Giới hạn Elo trong khoảng hợp lệ
        elo = Math.max(1350, Math.min(2850, elo));

        sendCommand("setoption name UCI_LimitStrength value true");
        sendCommand("setoption name UCI_Elo value " + elo);
        sendCommand("isready");
        waitForReady();

        System.out.println("[Stockfish] Đã đặt Elo: " + elo);
    }

    /**
     * Thiết lập số luồng CPU
     */
    public void setThreads(int threads) throws IOException {
        sendCommand("setoption name Threads value " + threads);
    }

    /**
     * Thiết lập kích thước Hash Table (MB)
     */
    public void setHashSize(int sizeMB) throws IOException {
        sendCommand("setoption name Hash value " + sizeMB);
    }

    /**
     * Reset engine về vị trí ban đầu
     */
    public void resetPosition() throws IOException {
        sendCommand("ucinewgame");
        sendCommand("isready");
        waitForReady();
    }

    /**
     * Gửi lệnh đến Stockfish
     */
    private void sendCommand(String command) throws IOException {
        writer.write(command + "\n");
        writer.flush();
    }

    /**
     * Đọc một dòng output từ Stockfish
     */
    private String readLine() throws IOException {
        return reader.readLine();
    }

    /**
     * Đợi cho đến khi Stockfish sẵn sàng
     */
    private void waitForReady() throws IOException {
        String line;
        while ((line = readLine()) != null) {
            if (line.equals("readyok")) {
                break;
            }
        }
    }

    /**
     * Đóng engine
     */
    public void close() {
        try {
            if (writer != null) {
                sendCommand("quit");
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (process != null) {
                process.destroy();
                process.waitFor();
            }
            System.out.println("[Stockfish] Engine đã đóng");
        } catch (Exception e) {
            System.err.println("[Stockfish] Lỗi khi đóng: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra engine có đang chạy không
     */
    public boolean isRunning() {
        return process != null && process.isAlive() && isInitialized;
    }
}