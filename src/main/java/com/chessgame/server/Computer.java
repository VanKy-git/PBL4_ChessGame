package com.chessgame.server;

import java.util.*;

/**
 * Chess AI - PHIÊN BẢN SẠCH: ĐÃ LOẠI BỎ HOÀN TOÁI TIMEOUT
 * Chỉ dùng depth cố định theo DifficultyLevel
 * Chạy đủ depth, không bị ngắt giữa chừng
 */
public class Computer {

    /* ===== THUỘC TÍNH ===== */
    private final ChessValidator validator;
    private final String aiColor;
    private final DifficultyLevel difficulty;

    // Transposition Table + Zobrist
    private final Map<Long, TranspositionEntry> transpositionTable;
    private final long[][][] zobristTable;
    private final long[] zobristCastling;
    private final long[] zobristEnPassant;
    private final long zobristTurn;

    // Thống kê
    private final SearchStatistics stats = new SearchStatistics();

    /* ===== ENUM MỨC ĐỘ KHÓ ===== */
    public enum DifficultyLevel {
        EASY(4, "Dễ"),
        MEDIUM(5, "Trung bình"),
        HARD(6, "Khó"),
        EXPERT(8, "Chuyên gia"),      // Đã tăng lên 8 cho mạnh hơn
        MASTER(10, "Bậc thầy");

        private final int searchDepth;
        private final String displayName;

        DifficultyLevel(int depth, String name) {
            this.searchDepth = depth;
            this.displayName = name;
        }

        public int getSearchDepth() { return searchDepth; }
        public String getDisplayName() { return displayName; }
    }

    /* ===== CÁC CLASS NỘI BỘ ===== */
    private static class TranspositionEntry {
        long zobristHash;
        int depth;
        int score;
        int flag;
        String bestMove;

        static final int EXACT = 0;
        static final int LOWER_BOUND = 1;
        static final int UPPER_BOUND = 2;

        TranspositionEntry(long hash, int depth, int score, int flag, String move) {
            this.zobristHash = hash;
            this.depth = depth;
            this.score = score;
            this.flag = flag;
            this.bestMove = move;
        }
    }

    public static class SearchStatistics {
        public long nodesEvaluated = 0;
        public long branchesPruned = 0;
        public long cacheHits = 0;
        public long cacheMisses = 0;
        public double searchTimeMs = 0;
        public int maxDepthReached = 0;

        public void reset() {
            nodesEvaluated = branchesPruned = cacheHits = cacheMisses = 0;
            searchTimeMs = 0;
            maxDepthReached = 0;
        }

        @Override
        public String toString() {
            double hitRate = (cacheHits + cacheMisses) > 0 ? 100.0 * cacheHits / (cacheHits + cacheMisses) : 0;
            return String.format(
                    "Nodes: %,d | Pruned: %,d | Cache: %,d/%,d (%.1f%%) | Time: %.3fs | Depth: %d",
                    nodesEvaluated, branchesPruned, cacheHits, (cacheHits + cacheMisses),
                    hitRate, searchTimeMs / 1000.0, maxDepthReached
            );
        }
    }

    /* ===== CONSTRUCTOR ===== */
    public Computer(ChessValidator validator, String aiColor, DifficultyLevel difficulty) {
        this.validator = validator;
        this.aiColor = aiColor;
        this.difficulty = difficulty;

        this.transpositionTable = new HashMap<>();
        this.zobristTable = initializeZobristTable();
        this.zobristCastling = initializeZobristCastling();
        this.zobristEnPassant = initializeZobristEnPassant();
        this.zobristTurn = new Random(12345).nextLong();

        System.out.println("[Computer] Khởi tạo AI: " + aiColor + " | Level: " + difficulty.getDisplayName() +
                " | Depth cố định: " + difficulty.getSearchDepth());
    }

    /* ===== ZOBRIST HASHING (giữ nguyên) ===== */
    private long[][][] initializeZobristTable() {
        Random rand = new Random(12345);
        long[][][] table = new long[6][64][2];
        for (int p = 0; p < 6; p++)
            for (int sq = 0; sq < 64; sq++)
                for (int c = 0; c < 2; c++)
                    table[p][sq][c] = rand.nextLong();
        return table;
    }

    private long[] initializeZobristCastling() {
        Random r = new Random(54321);
        long[] t = new long[4];
        for (int i = 0; i < 4; i++) t[i] = r.nextLong();
        return t;
    }

    private long[] initializeZobristEnPassant() {
        Random r = new Random(98765);
        long[] t = new long[8];
        for (int i = 0; i < 8; i++) t[i] = r.nextLong();
        return t;
    }

    private long computeZobristHash(char[][] board, String turn, String castling, String enPassant) {
        long hash = 0L;

        // Pieces
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                char p = board[r][c];
                if (p != '.') {
                    int type = getPieceType(p);
                    int sq = r * 8 + c;
                    int color = Character.isUpperCase(p) ? 0 : 1;
                    hash ^= zobristTable[type][sq][color];
                }
            }
        }

        // Castling rights
        if (castling.contains("K")) hash ^= zobristCastling[0];
        if (castling.contains("Q")) hash ^= zobristCastling[1];
        if (castling.contains("k")) hash ^= zobristCastling[2];
        if (castling.contains("q")) hash ^= zobristCastling[3];

        // En passant
        if (!"-".equals(enPassant)) {
            int file = enPassant.charAt(0) - 'a';
            hash ^= zobristEnPassant[file];
        }

        // Turn
        if ("white".equals(turn)) hash ^= zobristTurn;

        return hash;
    }

    private int getPieceType(char p) {
        return switch (Character.toLowerCase(p)) {
            case 'p' -> 0;
            case 'n' -> 1;
            case 'b' -> 2;
            case 'r' -> 3;
            case 'q' -> 4;
            case 'k' -> 5;
            default -> -1;
        };
    }

    /* ===== TÌM NƯỚC ĐI TỐT NHẤT - KHÔNG CÒN TIMEOUT ===== */
    public String getBestMove() {
        long startTime = System.currentTimeMillis();
        stats.reset();
        transpositionTable.clear();

        int targetDepth = difficulty.getSearchDepth();
        String bestMove = null;

        System.out.println("[Computer] Bắt đầu tìm kiếm - Depth cố định: " + targetDepth);

        // Iterative Deepening (vẫn giữ để có PV move tốt hơn)
        for (int depth = 1; depth <= targetDepth; depth++) {
            String move = searchRoot(depth);
            if (move != null) {
                bestMove = move;
                stats.maxDepthReached = depth;
            }
            System.out.println("[Computer] Depth " + depth + " hoàn thành → " + bestMove + " | " + stats);
        }

        stats.searchTimeMs = System.currentTimeMillis() - startTime;
        System.out.println("[Computer] HOÀN TẤT - Trả về nước đi depth " + stats.maxDepthReached + ": " + bestMove);
        System.out.println("[Computer] Thống kê cuối: " + stats);

        // Fallback cực hiếm
        if (bestMove == null) {
            List<String> moves = getAllLegalMoves(aiColor, validator.getBoard());
            bestMove = moves.isEmpty() ? "resign" : moves.get(0);
        }

        return bestMove;
    }

    private String searchRoot(int depth) {
        char[][] board = validator.getBoard();
        List<String> moves = getAllLegalMoves(aiColor, board);
        if (moves.isEmpty()) return null;

        orderMoves(moves, board);

        String bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (String move : moves) {
            char[][] newBoard = makeMove(board, move);
            int score = -minimax(newBoard, depth - 1, -beta, -alpha, getOpponentColor(aiColor));
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, score);
        }
        return bestMove;
    }

    /* ===== MINIMAX + ALPHA-BETA (đã bỏ hết timeout check) ===== */
    private int minimax(char[][] board, int depth, int alpha, int beta, String currentPlayer) {
        stats.nodesEvaluated++;

        // Transposition Table lookup
        String castling = computeCastlingRights(board);
        long hash = computeZobristHash(board, currentPlayer, castling, "-");
        TranspositionEntry entry = transpositionTable.get(hash);

        if (entry != null && entry.depth >= depth) {
            stats.cacheHits++;
            if (entry.flag == TranspositionEntry.EXACT) return entry.score;
            if (entry.flag == TranspositionEntry.LOWER_BOUND) alpha = Math.max(alpha, entry.score);
            if (entry.flag == TranspositionEntry.UPPER_BOUND) beta = Math.min(beta, entry.score);
            if (alpha >= beta) return entry.score;
        } else {
            stats.cacheMisses++;
        }

        if (depth == 0 || isGameOver(board)) {
            return evaluateBoard(board);
        }

        List<String> moves = getAllLegalMoves(currentPlayer, board);
        if (moves.isEmpty()) {
            return validator.isKingInCheck(currentPlayer, board)
                    ? -100000 + depth
                    : 0;
        }

        orderMoves(moves, board);

        int bestScore = Integer.MIN_VALUE;
        String bestMove = null;
        int originalAlpha = alpha;

        for (String move : moves) {
            char[][] newBoard = makeMove(board, move);
            int score = -minimax(newBoard, depth - 1, -beta, -alpha, getOpponentColor(currentPlayer));

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, score);
            if (alpha >= beta) {
                stats.branchesPruned++;
                break;
            }
        }

        // Lưu vào TT
        int flag = (bestScore <= originalAlpha) ? TranspositionEntry.UPPER_BOUND :
                (bestScore >= beta) ? TranspositionEntry.LOWER_BOUND :
                        TranspositionEntry.EXACT;

        transpositionTable.put(hash, new TranspositionEntry(hash, depth, bestScore, flag, bestMove));
        return bestScore;
    }

    /* ===== CÁC HÀM CÒN LẠI (giữ nguyên, chỉ rút gọn comment) ===== */
    private void orderMoves(List<String> moves, char[][] board) {
        moves.sort((m1, m2) -> Integer.compare(scoreMoveForOrdering(m2, board), scoreMoveForOrdering(m1, board)));
    }

    private int scoreMoveForOrdering(String move, char[][] board) {
        int score = 0;
        String from = move.substring(0, 2), to = move.substring(2, 4);
        int fromRow = 8 - Character.getNumericValue(from.charAt(1));
        int fromCol = from.charAt(0) - 'a';
        int toRow = 8 - Character.getNumericValue(to.charAt(1));
        int toCol = to.charAt(0) - 'a';

        char moving = board[fromRow][fromCol];
        char captured = board[toRow][toCol];

        if (captured != '.') {
            score += 10 * getPieceValue(captured) - getPieceValue(moving);
        }

        // PV move từ TT
        long hash = computeZobristHash(board, validator.getCurrentTurn(), computeCastlingRights(board), "-");
        TranspositionEntry e = transpositionTable.get(hash);
        if (e != null && move.equals(e.bestMove)) score += 10000;

        if ((toRow == 3 || toRow == 4) && (toCol >= 2 && toCol <= 5)) score += 20;

        return score;
    }

    private int evaluateBoard(char[][] board) {
        return evaluateMaterial(board) * 10 + evaluatePosition(board) * 2;
    }

    private int evaluateMaterial(char[][] board) {
        int score = 0;
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            char p = board[r][c];
            if (p != '.') {
                int val = getPieceValue(p);
                score += isPieceColor(p, aiColor) ? val : -val;
            }
        }
        return score;
    }

    private int evaluatePosition(char[][] board) {
        int score = 0;
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            char p = board[r][c];
            if (p != '.') {
                int val = getPositionalValue(p, r, c);
                score += isPieceColor(p, aiColor) ? val : -val;
            }
        }
        return score;
    }

    private int getPositionalValue(char piece, int row, int col) {
        int center = ((row == 3 || row == 4) && (col >= 2 && col <= 5)) ? 10 : 0;
        if (Character.toLowerCase(piece) == 'p') {
            return isPieceColor(piece, "white") ? (7 - row) * 5 + center : row * 5 + center;
        }
        return center;
    }

    private int getPieceValue(char p) {
        return switch (Character.toLowerCase(p)) {
            case 'p' -> 100;
            case 'n' -> 320;
            case 'b' -> 330;
            case 'r' -> 500;
            case 'q' -> 900;
            case 'k' -> 20000;
            default -> 0;
        };
    }

    private List<String> getAllLegalMoves(String color, char[][] board) {
        List<String> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                char p = board[r][c];
                if (p != '.' && isPieceColor(p, color)) {
                    String from = String.format("%c%d", (char)('a' + c), 8 - r);
                    List<String> targets = validator.getValidMovesForSquare(from);
                    for (String to : targets) moves.add(from + to);
                }
            }
        }
        return moves;
    }

    private String computeCastlingRights(char[][] board) {
        StringBuilder sb = new StringBuilder();
        if (board[7][4] == 'K' && board[7][7] == 'R') sb.append('K');
        if (board[7][4] == 'K' && board[7][0] == 'R') sb.append('Q');
        if (board[0][4] == 'k' && board[0][7] == 'r') sb.append('k');
        if (board[0][4] == 'k' && board[0][0] == 'r') sb.append('q');
        return sb.length() > 0 ? sb.toString() : "-";
    }

    private char[][] makeMove(char[][] board, String move) {
        char[][] copy = new char[8][8];
        for (int i = 0; i < 8; i++) copy[i] = Arrays.copyOf(board[i], 8);

        int fr = 8 - Character.getNumericValue(move.charAt(1));
        int fc = move.charAt(0) - 'a';
        int tr = 8 - Character.getNumericValue(move.charAt(3));
        int tc = move.charAt(2) - 'a';

        copy[tr][tc] = copy[fr][fc];
        copy[fr][fc] = '.';
        return copy;
    }

    private boolean isPieceColor(char piece, String color) {
        if (piece == '.') return false;
        boolean whitePiece = Character.isUpperCase(piece);
        return (color.equals("white") && whitePiece) || (color.equals("black") && !whitePiece);
    }

    private String getOpponentColor(String color) {
        return color.equals("white") ? "black" : "white";
    }

    private boolean isGameOver(char[][] board) {
        boolean hasWhiteKing = false, hasBlackKing = false;
        for (char[] row : board) for (char p : row) {
            if (p == 'K') hasWhiteKing = true;
            if (p == 'k') hasBlackKing = true;
        }
        return !hasWhiteKing || !hasBlackKing;
    }

    /* ===== GETTERS ===== */
    public SearchStatistics getStatistics() { return stats; }
    public DifficultyLevel getDifficulty() { return difficulty; }
}