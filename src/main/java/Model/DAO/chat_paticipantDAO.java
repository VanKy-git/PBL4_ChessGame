package Model.DAO;

import Model.Entity.chat_paticipant;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng chat_participant
 * Quản lý danh sách người dùng tham gia từng phòng chat
 */
public class chat_paticipantDAO {
    private Connection conn;

    public chat_paticipantDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Thêm người dùng vào phòng chat
     */
    public boolean insertParticipant(int chatroomId, int userId) {
        String sql = "INSERT INTO chat_participant (chatroom_id, user_id, joined_at) VALUES (?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chatroomId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thêm người dùng vào phòng chat", e);
        }
    }

    /**
     * Lấy danh sách tất cả người tham gia trong một phòng chat
     */
    public List<chat_paticipant> getParticipantsByRoom(int chatroomId) {
        List<chat_paticipant> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_participant WHERE chatroom_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chatroomId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapPaticipant(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách người tham gia", e);
        }
        return list;
    }

    /**
     * Xóa người dùng khỏi phòng chat
     */
    public boolean removeParticipant(int chatroomId, int userId) {
        String sql = "DELETE FROM chat_participant WHERE chatroom_id = ? AND user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chatroomId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa người dùng khỏi phòng chat", e);
        }
    }

    /**
     * Xóa tất cả người tham gia trong phòng (khi xóa phòng)
     */
    public boolean removeAllByRoom(int chatroomId) {
        String sql = "DELETE FROM chat_participant WHERE chatroom_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chatroomId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa người tham gia theo phòng", e);
        }
    }

    // Hàm ánh xạ ResultSet → đối tượng chat_participant
    private chat_paticipant mapPaticipant(ResultSet rs) throws SQLException {
        return new chat_paticipant(
                //rs.getInt("chatparticipant_id"),
                rs.getInt("chatroom_id"),
                rs.getInt("user_id"),
                rs.getTimestamp("joined_at").toLocalDateTime()
        );
    }
}
