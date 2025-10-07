package Model.DAO;

import Model.Entity.chat_message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng chat_message — xử lý toàn bộ nghiệp vụ liên quan đến tin nhắn.
 * Bao gồm: thêm, lấy theo phòng, lấy theo người dùng, và xóa.
 */
public class chat_messageDAO {
    private Connection conn;

    public chat_messageDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Chuyển 1 dòng ResultSet thành đối tượng chat_message
     */
    private chat_message mapMessage(ResultSet rs) throws SQLException {
        return new chat_message(
                rs.getInt("message_id"),
                rs.getInt("chatroom_id"),
                rs.getInt("user_id"),
                rs.getString("message"),
                rs.getTimestamp("sent_at").toLocalDateTime()
        );
    }

    /**
     * Gửi tin nhắn mới vào phòng chat
     * @param chatroomId ID phòng chat
     * @param userId ID người gửi
     * @param message nội dung tin nhắn
     * @return ID tin nhắn vừa thêm
     */
    public int insertMessage(int chatroomId, int userId, String message) {
        String sql = "INSERT INTO chat_message (chatroom_id, user_id, message, sent_at) VALUES (?, ?, ?, NOW()) RETURNING message_id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chatroomId);
            stmt.setInt(2, userId);
            stmt.setString(3, message);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("message_id");
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi gửi tin nhắn", e);
        }
        return -1;
    }

    /**
     * Lấy toàn bộ tin nhắn trong 1 phòng chat (sắp theo thời gian tăng dần)
     */
    public List<chat_message> getMessagesByRoom(int chatroomId) {
        List<chat_message> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_message WHERE chatroom_id = ? ORDER BY sent_at ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chatroomId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapMessage(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách tin nhắn trong phòng", e);
        }
        return list;
    }

    /**
     * Lấy danh sách tin nhắn do 1 người gửi trong tất cả phòng
     */
    public List<chat_message> getMessagesByUser(int userId) {
        List<chat_message> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_message WHERE user_id = ? ORDER BY sent_at DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapMessage(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách tin nhắn của người dùng", e);
        }
        return list;
    }

    /**
     * Xóa tin nhắn theo ID
     */
    public boolean deleteMessage(int messageId) {
        String sql = "DELETE FROM chat_message WHERE message_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa tin nhắn", e);
        }
    }

    /**
     * Xóa toàn bộ tin nhắn trong 1 phòng chat (khi phòng bị xóa)
     */
    public boolean deleteMessagesByRoom(int chatroomId) {
        String sql = "DELETE FROM chat_message WHERE chatroom_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chatroomId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa tin nhắn trong phòng", e);
        }
    }
}
