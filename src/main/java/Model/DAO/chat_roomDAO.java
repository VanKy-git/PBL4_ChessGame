package Model.DAO;

import Model.Entity.chat_room;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Bao gồm các chức năng:
 *  - Tạo phòng chat mới
 *  - Lấy thông tin phòng chat
 *  - Lấy danh sách phòng chat của người dùng
 *  - Cập nhật loại phòng chat
 *  - Xóa phòng chat cùng dữ liệu liên quan (tin nhắn, người tham gia)
 *  - Kiểm tra xem giữa 2 người đã có phòng PRIVATE hay chưa
 */
public class chat_roomDAO {
    private Connection conn;

    public chat_roomDAO(Connection conn) {
        this.conn = conn;
    }

    private chat_room mapChatRoom(ResultSet rs) throws SQLException {
        return new chat_room(
                rs.getInt("chatroom_id"),
                rs.getString("room_type"),
                rs.getInt("ref_id"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    //Tạo mới một phòng chat trong bảng chat_room
    public int insertChatRoom(String roomType, Integer refId) {
        String sql = """
            INSERT INTO chat_room (room_type, ref_id, created_at)
            VALUES (?, ?, NOW())
            RETURNING chatroom_id
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomType);
            if (refId != null)
                stmt.setInt(2, refId);
            else
                stmt.setNull(2, Types.INTEGER);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("chatroom_id");

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thêm phòng chat", e);
        }
        return -1;
    }

    //Lấy thông tin chi tiết của một phòng chat
    public chat_room getChatRoomById(int chatroomId) {
        String sql = "SELECT * FROM chat_room WHERE chatroom_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chatroomId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapChatRoom(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy thông tin phòng chat", e);
        }
        return null;
    }

    /**
     * Lấy danh sách tất cả các phòng chat mà người dùng đang tham gia
     * Dựa vào bảng chat_participant để liên kết user → room
     */
    public List<chat_room> getChatRoomsByUser(int userId) {
        List<chat_room> list = new ArrayList<>();
        String sql = """
            SELECT cr.*
            FROM chat_room cr
            JOIN chat_participant cp ON cr.chatroom_id = cp.chatroom_id
            WHERE cp.user_id = ?
            ORDER BY cr.created_at DESC
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapChatRoom(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách phòng chat", e);
        }
        return list;
    }

    public boolean updateRoomType(int chatroomId, String newType) {
        String sql = "UPDATE chat_room SET room_type = ? WHERE chatroom_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newType);
            stmt.setInt(2, chatroomId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi cập nhật loại phòng chat", e);
        }
    }

    /**
     * Xóa phòng chat và toàn bộ dữ liệu liên quan:
     *  - Xóa tin nhắn trong bảng chat_message
     *  - Xóa người tham gia trong bảng chat_participant
     *  - Xóa bản ghi phòng chat trong bảng chat_room
     */
    public boolean deleteChatRoomCascade(int chatroomId) {
        String deleteMessages = "DELETE FROM chat_message WHERE chatroom_id = ?";
        String deleteParticipants = "DELETE FROM chat_participant WHERE chatroom_id = ?";
        String deleteRoom = "DELETE FROM chat_room WHERE chatroom_id = ?";

        try {
            conn.setAutoCommit(false); // Bắt đầu transaction

            try (PreparedStatement stmt1 = conn.prepareStatement(deleteMessages);
                 PreparedStatement stmt2 = conn.prepareStatement(deleteParticipants);
                 PreparedStatement stmt3 = conn.prepareStatement(deleteRoom)) {

                // Xóa tin nhắn trong phòng
                stmt1.setInt(1, chatroomId);
                stmt1.executeUpdate();

                // Xóa người tham gia trong phòng
                stmt2.setInt(1, chatroomId);
                stmt2.executeUpdate();

                // Cuối cùng xóa bản ghi phòng chat
                stmt3.setInt(1, chatroomId);
                stmt3.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);
            return true;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Lỗi khi xóa toàn bộ dữ liệu phòng chat", e);
        }
    }

    /**
     * Kiểm tra xem giữa 2 người dùng đã tồn tại phòng chat PRIVATE chưa.
     * Nếu đã có thì trả về chatroom_id của phòng đó.
     * @param user1Id ID người dùng 1
     * @param user2Id ID người dùng 2
     * @return chatroom_id nếu có, -1 nếu chưa tồn tại
     */
    public int findPrivateRoomBetweenUsers(int user1Id, int user2Id) {
        String sql = """
            SELECT cr.chatroom_id
            FROM chat_room cr
            JOIN chat_participant p1 ON cr.chatroom_id = p1.chatroom_id
            JOIN chat_participant p2 ON cr.chatroom_id = p2.chatroom_id
            WHERE cr.room_type = 'PRIVATE'
              AND p1.user_id = ?
              AND p2.user_id = ?
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user1Id);
            stmt.setInt(2, user2Id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("chatroom_id");
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi tìm phòng chat riêng giữa 2 người", e);
        }
        return -1;
    }
}
