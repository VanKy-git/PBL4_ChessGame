package com.chatapp.server;

import java.util.*;

public class ChessValidator {

    private final char[][] board;  // Bàn cờ 8x8
    private String currentTurn = "white"; // Lượt hiện tại: "white" hoặc "black"

    public ChessValidator() {
        board = new char[8][8];
        resetBoard();
    }

    // Đặt bàn cờ về trạng thái ban đầu
    public void resetBoard() {
        String whitePieces = "RNBQKBNR";
        String blackPieces = "rnbqkbnr";

        // Hàng đen
        for (int i = 0; i < 8; i++) {
            board[0][i] = blackPieces.charAt(i);
            board[1][i] = 'p'; // tốt đen
            board[6][i] = 'P'; // tốt trắng
            board[7][i] = whitePieces.charAt(i);
        }

        // Các ô trống
        for (int i = 2; i < 6; i++) {
            Arrays.fill(board[i], '.');
        }

        currentTurn = "white";
    }

    // Kiểm tra và thực hiện nước đi
    public synchronized MoveResult validateMove(String from, String to, String color) {
        int fromRow = 8 - Character.getNumericValue(from.charAt(1));
        int fromCol = from.charAt(0) - 'a';
        int toRow = 8 - Character.getNumericValue(to.charAt(1));
        int toCol = to.charAt(0) - 'a';

        char piece = board[fromRow][fromCol];
        char target = board[toRow][toCol];

        // Kiểm tra lượt đi hợp lệ
        if (!isCorrectTurn(piece, color)) {
            return new MoveResult(false, "Không phải lượt của bạn");
        }

        if (piece == '.') {
            return new MoveResult(false, "Không có quân ở vị trí này");
        }

        // Kiểm tra logic di chuyển
        if (!isValidMove(piece, fromRow, fromCol, toRow, toCol)) {
            return new MoveResult(false, "Nước đi không hợp lệ");
        }

        // Nếu ăn quân
        boolean capture = (target != '.');

        // Thực hiện di chuyển
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = '.';

        // Kiểm tra thắng thua
        boolean whiteKingAlive = false, blackKingAlive = false;
        for (char[] row : board) {
            for (char c : row) {
                if (c == 'K') whiteKingAlive = true;
                if (c == 'k') blackKingAlive = true;
            }
        }

        String winner = null;
        if (!whiteKingAlive) winner = "black";
        if (!blackKingAlive) winner = "white";

        // Đổi lượt
        currentTurn = currentTurn.equals("white") ? "black" : "white";

        return new MoveResult(true, capture ? "Ăn quân!" : "Di chuyển hợp lệ", winner);
    }

    private boolean isCorrectTurn(char piece, String color) {
        if (color.equals("white")) {
            return Character.isUpperCase(piece);
        } else {
            return Character.isLowerCase(piece);
        }
    }

    // Kiểm tra nước đi hợp lệ theo loại quân
    private boolean isValidMove(char piece, int fromRow, int fromCol, int toRow, int toCol) {
        piece = Character.toLowerCase(piece);
        int dr = toRow - fromRow;
        int dc = toCol - fromCol;

        switch (piece) {
            case 'p': // tốt
                return validatePawn(fromRow, fromCol, toRow, toCol);
            case 'r': // xe
                return (dr == 0 || dc == 0) && isPathClear(fromRow, fromCol, toRow, toCol);
            case 'n': // mã
                return (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2);
            case 'b': // tượng
                return Math.abs(dr) == Math.abs(dc) && isPathClear(fromRow, fromCol, toRow, toCol);
            case 'q': // hậu
                return (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc)) && isPathClear(fromRow, fromCol, toRow, toCol);
            case 'k': // vua
                return Math.abs(dr) <= 1 && Math.abs(dc) <= 1;
            default:
                return false;
        }
    }

    private boolean validatePawn(int fromRow, int fromCol, int toRow, int toCol) {
        char pawn = board[fromRow][fromCol];
        int direction = Character.isUpperCase(pawn) ? -1 : 1;
        int startRow = Character.isUpperCase(pawn) ? 6 : 1;

        // Di chuyển thẳng
        if (fromCol == toCol) {
            if (board[toRow][toCol] == '.' && toRow - fromRow == direction) return true;
            if (fromRow == startRow && toRow - fromRow == 2 * direction && board[fromRow + direction][fromCol] == '.' && board[toRow][toCol] == '.') return true;
        }
        // Ăn chéo
        if (Math.abs(toCol - fromCol) == 1 && toRow - fromRow == direction) {
            if (board[toRow][toCol] != '.' &&
                    Character.isUpperCase(pawn) != Character.isUpperCase(board[toRow][toCol])) {
                return true;
            }
        }
        return false;
    }

    private boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
        int dr = Integer.compare(toRow, fromRow);
        int dc = Integer.compare(toCol, fromCol);

        int r = fromRow + dr, c = fromCol + dc;
        while (r != toRow || c != toCol) {
            if (board[r][c] != '.') return false;
            r += dr;
            c += dc;
        }
        return true;
    }

    public char[][] getBoard() {
        return board;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public static class MoveResult {
        public final boolean valid;
        public final String message;
        public final String winner;

        public MoveResult(boolean valid, String message) {
            this(valid, message, null);
        }

        public MoveResult(boolean valid, String message, String winner) {
            this.valid = valid;
            this.message = message;
            this.winner = winner;
        }
    }
}

