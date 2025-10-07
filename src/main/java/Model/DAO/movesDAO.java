package Model.DAO;

import Model.Entity.moves;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp DAO (Data Access Object) cho bảng moves
 * Quản lý toàn bộ các thao tác thêm / đọc / cập nhật / xóa nước đi trong trận đấu.
 */
public class movesDAO {
    private final Connection conn;

    public movesDAO(Connection conn) {
        this.conn = conn;
    }

    // ------------------- THÊM NƯỚC ĐI (CREATE) -------------------
    /**
     * Thêm một nước đi mới vào cơ sở dữ liệu.
     * - Tự động xác định số thứ tự nước đi (move_number).
     * - Xóa các nước đã bị hoàn tác (is_active = FALSE) trước đó.
     * - Ghi nhận thời gian thực hiện nước đi (move_time = NOW()).
     */
    public boolean insertMove(int matchId, int playerId, String fromSquare, String toSquare, String piece) {
        try {
            int moveNumber = getNextMoveNumber(matchId);
            deleteInactiveMoves(matchId); // Xóa các nước bị hoàn tác trước đó

            String sql = """
                INSERT INTO moves (match_id, player_id, move_number, from_square, to_square, piece, is_active, move_time)
                VALUES (?, ?, ?, ?, ?, ?, TRUE, ?)
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, matchId);
                stmt.setInt(2, playerId);
                stmt.setInt(3, moveNumber);
                stmt.setString(4, fromSquare);
                stmt.setString(5, toSquare);
                stmt.setString(6, piece);
                stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now())); // Thời gian hiện tại
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thêm nước đi", e);
        }
    }

    // ------------------- ĐỌC DỮ LIỆU (READ) -------------------
    /**
     * Lấy toàn bộ danh sách nước đi của một trận đấu theo thứ tự.
     */
    public List<moves> getMovesByMatch(int matchId) {
        List<moves> list = new ArrayList<>();
        String sql = "SELECT * FROM moves WHERE match_id = ? ORDER BY move_number ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, matchId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapMove(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách nước đi", e);
        }
        return list;
    }

    /**
     * Lấy danh sách các nước đi đang còn hiệu lực (is_active = TRUE).
     * Dùng sau khi người chơi hoàn tác (undo) để hiển thị bàn cờ hiện tại.
     */
    public List<moves> getActiveMoves(int matchId) {
        List<moves> list = new ArrayList<>();
        String sql = """
            SELECT * FROM moves
            WHERE match_id = ? AND is_active = TRUE
            ORDER BY move_number ASC
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, matchId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapMove(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách nước đang hoạt động", e);
        }
        return list;
    }

    // ------------------- CẬP NHẬT (UNDO / REDO) -------------------
    /**
     * Hoàn tác (Undo) nước đi cuối cùng còn hiệu lực trong trận đấu.
     * => Đánh dấu is_active = FALSE cho nước đi cuối cùng.
     */
    public boolean undoMove(int matchId) {
        String sql = """
            UPDATE moves
            SET is_active = FALSE
            WHERE move_id = (
                SELECT move_id FROM moves
                WHERE match_id = ? AND is_active = TRUE
                ORDER BY move_number DESC
                LIMIT 1
            )
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, matchId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi hoàn tác nước đi (Undo)", e);
        }
    }

    /**
     * Làm lại (Redo) nước đi bị hoàn tác gần nhất.
     * => Đánh dấu is_active = TRUE cho nước bị undo gần nhất.
     */
    public boolean redoMove(int matchId) {
        String sql = """
            UPDATE moves
            SET is_active = TRUE
            WHERE move_id = (
                SELECT move_id FROM moves
                WHERE match_id = ? AND is_active = FALSE
                ORDER BY move_number ASC
                LIMIT 1
            )
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, matchId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi làm lại nước đi (Redo)", e);
        }
    }

    // ------------------- XÓA DỮ LIỆU (DELETE) -------------------
    /**
     * Xóa một nước đi cụ thể theo ID.
     */
    public boolean deleteMove(int moveId) {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM moves WHERE move_id = ?")) {
            stmt.setInt(1, moveId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa nước đi", e);
        }
    }

    /**
     * Xóa toàn bộ nước đi của một trận đấu.
     */
    public boolean deleteAllMovesByMatch(int matchId) {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM moves WHERE match_id = ?")) {
            stmt.setInt(1, matchId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa toàn bộ nước đi của trận", e);
        }
    }

    // ------------------- HÀM PHỤ TRỢ (HELPER) -------------------
    /**
     * Lấy số thứ tự nước đi kế tiếp (move_number = MAX + 1).
     */
    private int getNextMoveNumber(int matchId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(move_number), 0) + 1 AS next_num FROM moves WHERE match_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, matchId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("next_num");
        }
        return 1;
    }

    /**
     * Xóa các nước đi không còn hiệu lực (is_active = FALSE).
     * Gọi khi người chơi tạo nước đi mới sau khi đã Undo.
     */
    private void deleteInactiveMoves(int matchId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM moves WHERE match_id = ? AND is_active = FALSE")) {
            stmt.setInt(1, matchId);
            stmt.executeUpdate();
        }
    }

    /**
     * Ánh xạ dữ liệu từ ResultSet -> đối tượng moves.
     */
    private moves mapMove(ResultSet rs) throws SQLException {
        moves m = new moves();
        m.setMove_id(rs.getInt("move_id"));
        m.setMatch_id(rs.getInt("match_id"));
        m.setPlayer_id(rs.getInt("player_id"));
        m.setMove_number(rs.getInt("move_number"));
        m.setFrom_square(rs.getString("from_square"));
        m.setTo_square(rs.getString("to_square"));
        m.setPiece(rs.getString("piece"));
        m.setActive(rs.getBoolean("is_active"));

        Timestamp ts = rs.getTimestamp("move_time");
        if (ts != null) m.setMove_time(ts.toLocalDateTime());

        return m;
    }
}
