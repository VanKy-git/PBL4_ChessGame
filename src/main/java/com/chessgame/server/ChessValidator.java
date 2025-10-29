package com.chessgame.server;

import java.util.ArrayList; // Thêm import
import java.util.List;

/**
 * Lớp ChessValidator quản lý toàn bộ logic của một ván cờ vua.
 * Nó xử lý việc xác thực nước đi, kiểm tra các luật lệ đặc biệt (nhập thành,
 * bắt tốt qua đường, phong cấp), và theo dõi trạng thái ván cờ (chiếu, chiếu bí, hòa cờ).
 * Lớp này cũng quản lý thời gian của mỗi người chơi và có thể tạo chuỗi FEN
 * đại diện cho trạng thái hiện tại của bàn cờ.
 */
public class ChessValidator {

    // =================================================================================
    // Properties - Các thuộc tính trạng thái của ván cờ
    // =================================================================================

    /** Bàn cờ 8x8, char[row][col]. 'P' là Tốt trắng, 'p' là Tốt đen, '.' là ô trống. */
    private char[][] board;

    /** Lượt đi hiện tại: "white" hoặc "black". */
    private String currentTurn;

    /** Quyền nhập thành. Ví dụ: "KQkq", "Kq", "-". */
    private String castlingRights;

    /** Ô có thể bắt tốt qua đường. Ví dụ: "e3", "-". */
    private String enPassantTarget;

    /** Đếm số nước đi nửa vời (halfmove) cho luật 50 nước đi. Reset khi có nước đi tốt hoặc bắt quân. */
    private int halfmoveClock;

    /** Đếm số nước đi đầy đủ. Tăng lên 1 sau mỗi nước đi của quân Đen. */
    private int fullmoveNumber;


    // =================================================================================
    // Constructor and Initialization - Khởi tạo
    // =================================================================================

    /**
     * Khởi tạo một ván cờ mới với thời gian cho mỗi người chơi.
     */
    public ChessValidator() {
        this.board = new char[8][8];
        resetBoard();
    }

    /**
     * Thiết lập lại bàn cờ và tất cả các trạng thái về vị trí ban đầu.
     */
    public void resetBoard() {
        board = new char[][]{
                {'r', 'n', 'b', 'q', 'k', 'b', 'n', 'r'},
                {'p', 'p', 'p', 'p', 'p', 'p', 'p', 'p'},
                {'.', '.', '.', '.', '.', '.', '.', '.'},
                {'.', '.', '.', '.', '.', '.', '.', '.'},
                {'.', '.', '.', '.', '.', '.', '.', '.'},
                {'.', '.', '.', '.', '.', '.', '.', '.'},
                {'P', 'P', 'P', 'P', 'P', 'P', 'P', 'P'},
                {'R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R'}
        };
        currentTurn = "white";
        castlingRights = "KQkq";
        enPassantTarget = "-";
        halfmoveClock = 0;
        fullmoveNumber = 1;
    }

    // =================================================================================
    // Core Logic - Logic chính
    // =================================================================================

    /**
     * Overloaded method cho các nước đi không phải là phong cấp.
     */
    public synchronized MoveResult validateMove(String from, String to, String color) {
        return validateMove(from, to, color, null);
    }

    /**
     * Kiểm tra và thực hiện một nước đi. Đây là hàm trung tâm của logic cờ vua.
     *
     * @param from  Tọa độ ô đi, ví dụ: "e2".
     * @param to    Tọa độ ô đến, ví dụ: "e4".
     * @param color Màu quân của người chơi thực hiện nước đi ("white" hoặc "black").
     * @param promotionPiece Ký tự quân cờ muốn phong cấp (ví dụ: 'Q', 'R'), hoặc null.
     * @return Một đối tượng MoveResult chứa kết quả của nước đi.
     */
    public synchronized MoveResult validateMove(String from, String to, String color, Character promotionPiece) {
        // 1. Kiểm tra lượt đi
        if (!color.equals(currentTurn)) {
            return new MoveResult(false, "Không phải lượt của bạn");
        }

        // 2. Chuyển đổi và kiểm tra tọa độ
        int fromRow = 8 - Character.getNumericValue(from.charAt(1));
        int fromCol = from.charAt(0) - 'a';
        int toRow = 8 - Character.getNumericValue(to.charAt(1));
        int toCol = to.charAt(0) - 'a';
        if (fromRow < 0 || fromRow > 7 || fromCol < 0 || fromCol > 7 ||
                toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
            return new MoveResult(false, "Tọa độ không hợp lệ");
        }

        char piece = board[fromRow][fromCol];
        char target = board[toRow][toCol];

        // 3. Kiểm tra các điều kiện cơ bản
        if (piece == '.') return new MoveResult(false, "Không có quân ở vị trí này");
        if (!isCorrectTurn(piece, color)) return new MoveResult(false, "Đây không phải quân cờ của bạn");
        if (target != '.' && Character.isUpperCase(piece) == Character.isUpperCase(target)) {
            return new MoveResult(false, "Không thể ăn quân cùng màu");
        }

        // 4. Kiểm tra logic di chuyển cơ bản của quân cờ
        if (!isValidMove(piece, fromRow, fromCol, toRow, toCol, board, enPassantTarget)) {
            return new MoveResult(false, "Nước đi không hợp lệ cho quân cờ này");
        }

        // 5. Tạo bàn cờ tạm và thực hiện nước đi thử
        char[][] tempBoard = cloneBoard(board);
        tempBoard[toRow][toCol] = piece;
        tempBoard[fromRow][fromCol] = '.';

        // Xử lý bắt tốt qua đường trên bàn cờ tạm
        boolean isPawnMove = Character.toLowerCase(piece) == 'p';
        if (isPawnMove && fromCol != toCol && target == '.') {
            String targetSquare = (char)('a' + toCol) + "" + (8 - toRow);
            if (targetSquare.equals(enPassantTarget)) {
                int capturedPawnRow = color.equals("white") ? toRow + 1 : toRow - 1;
                tempBoard[capturedPawnRow][toCol] = '.';
            }
        }

        if (isKingInCheck(color, tempBoard)) {
            return new MoveResult(false, "Nước đi không hợp lệ vì Vua sẽ bị chiếu");
        }

        // 6. Nước đi hợp lệ -> Bắt đầu thực hiện và cập nhật trạng thái
        boolean isCapture = (target != '.');
        String oldEnPassantTarget = enPassantTarget;

        // Reset enPassantTarget trước, nó sẽ được đặt lại nếu đây là nước đi tốt 2 ô
        enPassantTarget = "-";

        // Cập nhật quyền nhập thành khi Xe bị ăn
        if (target == 'R') {
            if (toRow == 7 && toCol == 0) castlingRights = castlingRights.replace("Q", "");
            if (toRow == 7 && toCol == 7) castlingRights = castlingRights.replace("K", "");
        }
        if (target == 'r') {
            if (toRow == 0 && toCol == 0) castlingRights = castlingRights.replace("q", "");
            if (toRow == 0 && toCol == 7) castlingRights = castlingRights.replace("k", "");
        }

        // Thực hiện di chuyển chính
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = '.';

        // 7. XỬ LÝ CÁC LUẬT ĐẶC BIỆT
        // 7.1. Phong cấp (Promotion)
        if (isPawnMove) {
            if ((piece == 'P' && toRow == 0) || (piece == 'p' && toRow == 7)) {
                if (promotionPiece == null) {
                    // Rollback
                    board[fromRow][fromCol] = piece;
                    board[toRow][toCol] = target;
                    return new MoveResult(false, "Cần chọn quân cờ để phong cấp");
                }
                char promoted = color.equals("white") ? Character.toUpperCase(promotionPiece) : Character.toLowerCase(promotionPiece);
                if ("QRNBqrnb".indexOf(promoted) == -1) {
                    // Rollback
                    board[fromRow][fromCol] = piece;
                    board[toRow][toCol] = target;
                    return new MoveResult(false, "Quân cờ phong cấp không hợp lệ");
                }
                board[toRow][toCol] = promoted;
            }
        }

        // 7.2. Bắt tốt qua đường (En Passant)
        if (isPawnMove && fromCol != toCol && !isCapture) {
            String targetSquare = (char)('a' + toCol) + "" + (8 - toRow);
            if (targetSquare.equals(oldEnPassantTarget)) {
                int capturedPawnRow = color.equals("white") ? toRow + 1 : toRow - 1;
                board[capturedPawnRow][toCol] = '.';
                isCapture = true;
            }
        }

        // 7.3. Nhập thành (Castling)
        if (Character.toLowerCase(piece) == 'k' && Math.abs(fromCol - toCol) == 2) {
            if (toCol > fromCol) {
                board[fromRow][fromCol + 1] = board[fromRow][fromCol + 3];
                board[fromRow][fromCol + 3] = '.';
            } else {
                board[fromRow][fromCol - 1] = board[fromRow][fromCol - 4];
                board[fromRow][fromCol - 4] = '.';
            }
        }

        // 8. CẬP NHẬT TRẠNG THÁI GAME
        // 8.1. Cập nhật enPassantTarget cho nước đi tiếp theo
        if (isPawnMove && Math.abs(fromRow - toRow) == 2) {
            int enPassantRow = (fromRow + toRow) / 2;
            enPassantTarget = "" + (char)('a' + fromCol) + (8 - enPassantRow);
        }

        // 8.2. Cập nhật quyền nhập thành
        if (piece == 'K') castlingRights = castlingRights.replace("K", "").replace("Q", "");
        if (piece == 'k') castlingRights = castlingRights.replace("k", "").replace("q", "");
        if (piece == 'R' && fromCol == 0 && fromRow == 7) castlingRights = castlingRights.replace("Q", "");
        if (piece == 'R' && fromCol == 7 && fromRow == 7) castlingRights = castlingRights.replace("K", "");
        if (piece == 'r' && fromCol == 0 && fromRow == 0) castlingRights = castlingRights.replace("q", "");
        if (piece == 'r' && fromCol == 7 && fromRow == 0) castlingRights = castlingRights.replace("k", "");

        if (castlingRights.isEmpty()) castlingRights = "-";

        // 8.3. Cập nhật halfmove clock
        if (isPawnMove || isCapture) halfmoveClock = 0;
        else halfmoveClock++;

        // 8.4. Đổi lượt và tăng số nước đi
        if (currentTurn.equals("black")) fullmoveNumber++;
        currentTurn = currentTurn.equals("white") ? "black" : "white";

        // 9. KIỂM TRA KẾT QUẢ VÁN ĐẤU
        boolean isCheck = isKingInCheck(currentTurn, board);
        boolean hasLegalMoves = hasAnyLegalMove(currentTurn);

        String winner = null;
        String message = "Di chuyển hợp lệ";

        if (halfmoveClock >= 100) {
            winner = "draw";
            message = "Hòa cờ do luật 50 nước đi!";
        }
        else if (!hasLegalMoves) {
            if (isCheck) {
                winner = color;
                message = "Chiếu bí! " + color + " thắng!";
            } else {
                winner = "draw";
                message = "Hòa cờ do hết nước đi!";
            }
        } else if (isCheck) {
            message = "Chiếu!";
        } else if (isCapture) {
            message = "Ăn quân!";
        }

        return new MoveResult(true, message, winner, from, to);
    }

    public List<String> getValidMovesForSquare(String algSquare) {
        List<String> validMoves = new ArrayList<>();
        if (algSquare == null || algSquare.length() != 2) {
            return validMoves; // Trả về rỗng nếu ô không hợp lệ
        }

        // Chuyển đổi ký hiệu đại số sang tọa độ
        int fromRow = 8 - Character.getNumericValue(algSquare.charAt(1));
        int fromCol = algSquare.charAt(0) - 'a';

        // Kiểm tra tọa độ hợp lệ
        if (fromRow < 0 || fromRow > 7 || fromCol < 0 || fromCol > 7) {
            return validMoves;
        }

        char piece = board[fromRow][fromCol];
        String pieceColor = isCorrectTurn(piece, "white") ? "white" : (isCorrectTurn(piece, "black") ? "black" : null);

        // Chỉ tính nếu ô đó có quân cờ và đúng lượt đi hiện tại
        if (piece == '.' || pieceColor == null || !pieceColor.equals(this.currentTurn)) {
            return validMoves;
        }

        // Thử đi đến tất cả các ô khác
        for (int tr = 0; tr < 8; tr++) {
            for (int tc = 0; tc < 8; tc++) {
                // 1. Kiểm tra sơ bộ (dùng hàm geometry hoặc isValidMove đầy đủ)
                // Dùng isValidMove đầy đủ sẽ chính xác hơn, bao gồm cả nhập thành
                if (isValidMove(piece, fromRow, fromCol, tr, tc, board, enPassantTarget)) {
                    // 2. Tạo bàn cờ tạm và đi thử
                    char[][] tempBoard = cloneBoard(board);
                    char originalTargetPiece = tempBoard[tr][tc]; // Lưu quân bị ăn
                    tempBoard[tr][tc] = piece;
                    tempBoard[fromRow][fromCol] = '.';

                    // Mô phỏng bắt tốt qua đường trên tempBoard (nếu cần, logic này đã có trong validateMove nên có thể bỏ qua?)
                    // Logic mô phỏng nhập thành trên tempBoard (nếu cần)

                    // 3. Kiểm tra Vua có an toàn không sau nước đi thử
                    // Sử dụng hàm isKingInCheck hoặc isKingInCheckSimple
                    if (!isKingInCheck(pieceColor, tempBoard)) {
                        // Nếu Vua an toàn, thêm ô đích vào danh sách
                        validMoves.add((char)('a' + tc) + "" + (8 - tr));
                    }
                    // Không cần rollback tempBoard
                }
            }
        }

        return validMoves;
    }

    /**
     * Kiểm tra xem một màu quân có còn nước đi hợp lệ nào không.
     * @param color Màu quân cần kiểm tra ("white" hoặc "black").
     * @return true nếu còn ít nhất một nước đi hợp lệ.
     */
    private boolean hasAnyLegalMove(String color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                char piece = board[r][c];
                if (piece != '.' && isCorrectTurn(piece, color)) {
                    for (int tr = 0; tr < 8; tr++) {
                        for (int tc = 0; tc < 8; tc++) {
                            if (isValidMove(piece, r, c, tr, tc, board, enPassantTarget)) {
                                char[][] tempBoard = cloneBoard(board);
                                tempBoard[tr][tc] = piece;
                                tempBoard[r][c] = '.';

                                // Xử lý bắt tốt qua đường
                                if (Character.toLowerCase(piece) == 'p' && c != tc && board[tr][tc] == '.') {
                                    String targetSquare = (char)('a' + tc) + "" + (8 - tr);
                                    if (targetSquare.equals(enPassantTarget)) {
                                        int capturedPawnRow = color.equals("white") ? tr + 1 : tr - 1;
                                        tempBoard[capturedPawnRow][tc] = '.';
                                    }
                                }

                                if (!isKingInCheck(color, tempBoard)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Kiểm tra xem Vua của một màu có đang bị chiếu hay không trên một bàn cờ cụ thể.
     * @param kingColor Màu của Vua cần kiểm tra ("white" hoặc "black").
     * @param currentBoard Bàn cờ để kiểm tra.
     * @return true nếu Vua đang bị chiếu.
     */
    public boolean isKingInCheck(String kingColor, char[][] currentBoard) {
        int kingRow = -1, kingCol = -1;
        char kingPiece = kingColor.equals("white") ? 'K' : 'k';
        String opponentColor = kingColor.equals("white") ? "black" : "white";

        // Tìm vị trí Vua
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (currentBoard[r][c] == kingPiece) {
                    kingRow = r;
                    kingCol = c;
                    break;
                }
            }
        }
        if (kingRow == -1) return false;

        // Kiểm tra xem có quân cờ nào của đối phương có thể tấn công ô Vua không
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                char piece = currentBoard[r][c];
                if (piece != '.' && isCorrectTurn(piece, opponentColor)) {
                    if (isValidMove(piece, r, c, kingRow, kingCol, currentBoard, "-")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // =================================================================================
    // Move Validation Helpers - Các hàm hỗ trợ xác thực nước đi
    // =================================================================================

    private boolean isCorrectTurn(char piece, String color) {
        if (piece == '.') return false;
        return color.equals("white") ? Character.isUpperCase(piece) : Character.isLowerCase(piece);
    }

    private boolean isValidMove(char piece, int fromRow, int fromCol, int toRow, int toCol,
                                char[][] boardState, String enPassant) {

        char targetPiece = boardState[toRow][toCol];
        if (targetPiece != '.' && Character.isUpperCase(piece) == Character.isUpperCase(targetPiece)) {
            return false; // Không được đi vào ô có quân cùng màu
        }

        if (fromRow == toRow && fromCol == toCol) return false;

        char pieceType = Character.toLowerCase(piece);
        switch (pieceType) {
            case 'p': return validatePawn(piece, fromRow, fromCol, toRow, toCol, boardState, enPassant);
            case 'r': return validateRook(fromRow, fromCol, toRow, toCol, boardState);
            case 'n': return validateKnight(fromRow, fromCol, toRow, toCol);
            case 'b': return validateBishop(fromRow, fromCol, toRow, toCol, boardState);
            case 'q': return validateQueen(fromRow, fromCol, toRow, toCol, boardState);
            case 'k': return validateKing(piece, fromRow, fromCol, toRow, toCol, boardState);
            default: return false;
        }
    }

    private boolean validatePawn(char pawn, int fromRow, int fromCol, int toRow, int toCol,
                                 char[][] boardState, String enPassant) {
        int direction = Character.isUpperCase(pawn) ? -1 : 1;
        int startRow = Character.isUpperCase(pawn) ? 6 : 1;

        // Di chuyển thẳng 1 ô
        if (fromCol == toCol && boardState[toRow][toCol] == '.' && toRow - fromRow == direction)
            return true;

        // Di chuyển thẳng 2 ô từ vị trí ban đầu
        if (fromRow == startRow && fromCol == toCol &&
                boardState[toRow][toCol] == '.' &&
                boardState[fromRow + direction][fromCol] == '.' &&
                toRow - fromRow == 2 * direction)
            return true;

        // Ăn chéo
        if (Math.abs(fromCol - toCol) == 1 && toRow - fromRow == direction) {
            // Ăn quân bình thường
            if (boardState[toRow][toCol] != '.') return true;

            // Bắt tốt qua đường
            String targetSquare = (char)('a' + toCol) + "" + (8 - toRow);
            if (targetSquare.equals(enPassant)) return true;
        }
        return false;
    }

    private boolean validateKing(char piece, int fromRow, int fromCol, int toRow, int toCol,
                                 char[][] boardState) {
        String color = Character.isUpperCase(piece) ? "white" : "black";

        // Di chuyển thông thường 1 ô
        if (Math.abs(toRow - fromRow) <= 1 && Math.abs(toCol - fromCol) <= 1) {
            // ✅ THÊM KIỂM TRA: Ô ĐẾN có bị đối phương tấn công không?
            if (isSquareUnderAttack(toRow, toCol, color, boardState)) {
                return false; // Không được đi vào ô bị chiếu
            }
            return true; // Ô đến an toàn
        }

        // Nhập thành (chỉ kiểm tra khi di chuyển 2 ô ngang)
        if (fromRow == toRow && Math.abs(fromCol - toCol) == 2) {
            // Logic kiểm tra nhập thành (đã có vẻ đúng)
            if (isKingInCheck(color, boardState)) {
                return false;
            }
            return isCastlingValid(fromRow, fromCol, toRow, toCol, boardState, color);
        }
        return false;
    }

    private boolean isCastlingValid(int fromRow, int fromCol, int toRow, int toCol,
                                    char[][] boardState, String color) {
        String kingSide = color.equals("white") ? "K" : "k";
        String queenSide = color.equals("white") ? "Q" : "q";

        // Nhập thành cánh Vua (Kingside)
        if (toCol > fromCol) {
            if (castlingRights.contains(kingSide) &&
                    boardState[fromRow][fromCol + 1] == '.' &&
                    boardState[fromRow][fromCol + 2] == '.' &&
                    !isSquareUnderAttack(fromRow, fromCol + 1, color, boardState) &&
                    !isSquareUnderAttack(fromRow, fromCol + 2, color, boardState)) {
                return true;
            }
        }
        // Nhập thành cánh Hậu (Queenside)
        else {
            if (castlingRights.contains(queenSide) &&
                    boardState[fromRow][fromCol - 1] == '.' &&
                    boardState[fromRow][fromCol - 2] == '.' &&
                    boardState[fromRow][fromCol - 3] == '.' &&
                    !isSquareUnderAttack(fromRow, fromCol - 1, color, boardState) &&
                    !isSquareUnderAttack(fromRow, fromCol - 2, color, boardState)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSquareUnderAttack(int row, int col, String playerColor, char[][] boardState) {
        String opponentColor = playerColor.equals("white") ? "black" : "white";

        // Duyệt qua tất cả các ô trên bàn cờ được truyền vào (boardState)
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                char piece = boardState[r][c]; // Lấy quân cờ từ boardState

                // Nếu đây là quân của đối phương
                if (piece != '.' && isCorrectTurn(piece, opponentColor)) {

                    // Kiểm tra xem quân đối phương này có thể đi đến ô (row, col) không?
                    // Sử dụng chính boardState để kiểm tra, và "-" cho enPassant vì chỉ kiểm tra tấn công.
                    if (isValidMove(piece, r, c, row, col, boardState, "-")) {
                        return true; // Nếu có thể, ô đó đang bị tấn công
                    }
                }
            }
        }

        // Nếu không có quân đối phương nào tấn công được ô đó
        return false;
    }

    private boolean validateRook(int fromRow, int fromCol, int toRow, int toCol, char[][] boardState) {
        return (fromRow == toRow || fromCol == toCol) && isPathClear(fromRow, fromCol, toRow, toCol, boardState);
    }

    private boolean validateKnight(int fromRow, int fromCol, int toRow, int toCol) {
        int dr = Math.abs(toRow - fromRow);
        int dc = Math.abs(toCol - fromCol);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }

    private boolean validateBishop(int fromRow, int fromCol, int toRow, int toCol, char[][] boardState) {
        return Math.abs(toRow - fromRow) == Math.abs(toCol - fromCol) &&
                isPathClear(fromRow, fromCol, toRow, toCol, boardState);
    }

    private boolean validateQueen(int fromRow, int fromCol, int toRow, int toCol, char[][] boardState) {
        return (fromRow == toRow || fromCol == toCol ||
                Math.abs(toRow - fromRow) == Math.abs(toCol - fromCol))
                && isPathClear(fromRow, fromCol, toRow, toCol, boardState);
    }

    private boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol, char[][] boardState) {
        int dr = Integer.compare(toRow, fromRow);
        int dc = Integer.compare(toCol, fromCol);

        int r = fromRow + dr;
        int c = fromCol + dc;

        while (r != toRow || c != toCol) {
            if (boardState[r][c] != '.') {
                return false;
            }
            r += dr;
            c += dc;
        }
        return true;
    }

    private char[][] cloneBoard(char[][] original) {
        char[][] clone = new char[8][8];
        for(int i = 0; i < 8; i++) {
            clone[i] = original[i].clone();
        }
        return clone;
    }

    // =================================================================================
    // FEN Generation - Tạo chuỗi FEN
    // =================================================================================

    public String toFen() {
        StringBuilder fen = new StringBuilder();

        // 1. Vị trí các quân cờ
        for (int i = 0; i < 8; i++) {
            int emptyCounter = 0;
            for (int j = 0; j < 8; j++) {
                char piece = board[i][j];
                if (piece == '.') {
                    emptyCounter++;
                } else {
                    if (emptyCounter > 0) {
                        fen.append(emptyCounter);
                        emptyCounter = 0;
                    }
                    fen.append(piece);
                }
            }
            if (emptyCounter > 0) {
                fen.append(emptyCounter);
            }
            if (i < 7) {
                fen.append('/');
            }
        }

        // 2. Lượt đi
        fen.append(" ").append(currentTurn.equals("white") ? 'w' : 'b');

        // 3. Quyền nhập thành
        fen.append(" ").append(castlingRights.isEmpty() || castlingRights.equals("-") ? "-" : castlingRights);

        // 4. Bắt tốt qua đường
        fen.append(" ").append(enPassantTarget);

        // 5. Đồng hồ bán nước đi
        fen.append(" ").append(halfmoveClock);

        // 6. Số nước đi đầy đủ
        fen.append(" ").append(fullmoveNumber);

        return fen.toString();
    }

    // =================================================================================
    // Getters - Các hàm lấy thông tin
    // =================================================================================

    public char[][] getBoard() {
        return board;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }


    public String getCastlingRights() {
        return castlingRights;
    }

    public String getEnPassantTarget() {
        return enPassantTarget;
    }

    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    public int getFullmoveNumber() {
        return fullmoveNumber;
    }

    // =================================================================================
    // State Management - Quản lý trạng thái
    // =================================================================================

    /**
     * Thiết lập trạng thái bàn cờ từ chuỗi FEN.
     * @param fen Chuỗi FEN đại diện cho trạng thái bàn cờ.
     * @return true nếu thiết lập thành công, false nếu FEN không hợp lệ.
     */
    public boolean setFromFen(String fen) {
        try {
            String[] parts = fen.split(" ");
            if (parts.length != 6) return false;

            // 1. Thiết lập bàn cờ
            String[] rows = parts[0].split("/");
            if (rows.length != 8) return false;

            char[][] newBoard = new char[8][8];
            for (int i = 0; i < 8; i++) {
                int col = 0;
                for (char c : rows[i].toCharArray()) {
                    if (Character.isDigit(c)) {
                        int empty = Character.getNumericValue(c);
                        for (int j = 0; j < empty; j++) {
                            newBoard[i][col++] = '.';
                        }
                    } else {
                        newBoard[i][col++] = c;
                    }
                }
                if (col != 8) return false;
            }

            // 2. Lượt đi
            String turn = parts[1].equals("w") ? "white" : "black";

            // 3. Quyền nhập thành
            String castling = parts[2];

            // 4. Bắt tốt qua đường
            String enPassant = parts[3];

            // 5. Halfmove clock
            int halfmove = Integer.parseInt(parts[4]);

            // 6. Fullmove number
            int fullmove = Integer.parseInt(parts[5]);

            // Nếu tất cả đều hợp lệ, cập nhật trạng thái
            this.board = newBoard;
            this.currentTurn = turn;
            this.castlingRights = castling;
            this.enPassantTarget = enPassant;
            this.halfmoveClock = halfmove;
            this.fullmoveNumber = fullmove;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kiểm tra xem có đủ quân cờ để chiếu bí không.
     * Sử dụng cho luật hòa cờ do không đủ quân.
     */
    public boolean hasInsufficientMaterial() {
        int whiteKnights = 0, whiteBishops = 0, whitePawns = 0, whiteRooks = 0, whiteQueens = 0;
        int blackKnights = 0, blackBishops = 0, blackPawns = 0, blackRooks = 0, blackQueens = 0;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                char piece = board[r][c];
                switch (piece) {
                    case 'N': whiteKnights++; break;
                    case 'B': whiteBishops++; break;
                    case 'P': whitePawns++; break;
                    case 'R': whiteRooks++; break;
                    case 'Q': whiteQueens++; break;
                    case 'n': blackKnights++; break;
                    case 'b': blackBishops++; break;
                    case 'p': blackPawns++; break;
                    case 'r': blackRooks++; break;
                    case 'q': blackQueens++; break;
                }
            }
        }

        // Nếu có tốt, xe hoặc hậu thì có đủ quân
        if (whitePawns > 0 || blackPawns > 0 ||
                whiteRooks > 0 || blackRooks > 0 ||
                whiteQueens > 0 || blackQueens > 0) {
            return false;
        }

        // Chỉ còn 2 vua
        if (whiteKnights == 0 && whiteBishops == 0 &&
                blackKnights == 0 && blackBishops == 0) {
            return true;
        }

        // Vua + Mã vs Vua hoặc Vua + Tượng vs Vua
        if ((whiteKnights == 1 && whiteBishops == 0 && blackKnights == 0 && blackBishops == 0) ||
                (whiteBishops == 1 && whiteKnights == 0 && blackKnights == 0 && blackBishops == 0) ||
                (blackKnights == 1 && blackBishops == 0 && whiteKnights == 0 && whiteBishops == 0) ||
                (blackBishops == 1 && blackKnights == 0 && whiteKnights == 0 && whiteBishops == 0)) {
            return true;
        }

        // Vua + Tượng vs Vua + Tượng cùng màu ô
        if (whiteKnights == 0 && blackKnights == 0 &&
                whiteBishops == 1 && blackBishops == 1) {
            // Tìm vị trí 2 tượng
            int whiteBishopSquare = -1, blackBishopSquare = -1;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (board[r][c] == 'B') whiteBishopSquare = (r + c) % 2;
                    if (board[r][c] == 'b') blackBishopSquare = (r + c) % 2;
                }
            }
            if (whiteBishopSquare == blackBishopSquare) {
                return true;
            }
        }

        return false;
    }

    // =================================================================================
    // Inner Class - Lớp nội tuyến
    // =================================================================================

    public static class MoveResult {
        public final boolean isValid;
        public final String message;
        public final String winner;

        public final String from;
        public final String to;

        public MoveResult(boolean isValid, String message) {
            this(isValid, message, null, null, null);
        }

        public MoveResult(boolean isValid, String message, String winner) {
            this.isValid = isValid;
            this.message = message;
            this.winner = winner;
            this.from = null;
            this.to = null;
        }

        public MoveResult(boolean isValid, String message, String winner, String from, String to) {
            this.isValid = isValid;
            this.message = message;
            this.winner = winner;
            this.from = from;
            this.to = to;
        }
    }
}