package Model.DAO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class friendsDAO {
    private Connection conn;

    // Hàm khởi tạo nhận vào đối tượng kết nối Database
    public friendsDAO(Connection conn) {
        this.conn = conn;
    }

    // ------------------------- LỚP PHỤ -------------------------
    /**
     * Lớp phụ friend_info: dùng để lưu trữ thông tin chi tiết của mối quan hệ bạn bè
     * bao gồm cả thông tin người dùng 1, người dùng 2 (tên, elo)
     */
    public static class friend_info {
        public int friendship_id;          // ID của mối quan hệ bạn bè
        public int user1_id;               // ID người gửi lời mời
        public int user2_id;               // ID người nhận lời mời
        public String user1_name;          // Tên người 1
        public Integer user1_elo_rating;   // Elo người 1
        public String user2_name;          // Tên người 2
        public Integer user2_elo_rating;   // Elo người 2
        public String status;              // Trạng thái: PENDING / ACCEPTED / REJECTED
        public LocalDateTime created_at;   // Thời điểm tạo mối quan hệ
    }

    // ------------------------- MAP -------------------------
    /**
     * Hàm mapFriend() ánh xạ (map) dữ liệu từ ResultSet → friend_info
     * Giúp chuyển đổi từng dòng dữ liệu SQL thành đối tượng Java
     */
    private friend_info mapFriend(ResultSet rs) throws SQLException {
        friend_info f = new friend_info();
        f.friendship_id = rs.getInt("friendship_id");
        f.user1_id = rs.getInt("user1_id");
        f.user2_id = rs.getInt("user2_id");
        f.status = rs.getString("status");

        // Chuyển đổi thời gian SQL → LocalDateTime
        Timestamp created = rs.getTimestamp("created_at");
        f.created_at = (created != null) ? created.toLocalDateTime() : null;

        // Thông tin người dùng 1
        f.user1_name = rs.getString("user1_name");
        f.user1_elo_rating = (Integer) rs.getObject("user1_elo_rating");

        // Thông tin người dùng 2
        f.user2_name = rs.getString("user2_name");
        f.user2_elo_rating = (Integer) rs.getObject("user2_elo_rating");

        return f;
    }

    // ------------------------- CREATE -------------------------
    /**
     * Gửi lời mời kết bạn (mặc định trạng thái là "PENDING")
     */
    public boolean addFriendRequest(int user1Id, int user2Id) {
        String sql = """
            INSERT INTO friends (user_id1, user_id2, status, created_at)
            VALUES (?, ?, 'PENDING', ?)
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user1Id);
            stmt.setInt(2, user2Id);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi thêm yêu cầu kết bạn", e);
        }
    }

    // ------------------------- READ -------------------------
    /**
     * Lấy danh sách tất cả bạn bè hoặc lời mời kết bạn của một người dùng
     * (Bao gồm tên và Elo của cả 2 người)
     */
    public List<friend_info> getFriendsOfUser(int userId) {
        String sql = """
            SELECT f.*, 
                   u1.user_name AS user1_name, u1.elo_rating AS user1_elo_rating,
                   u2.user_name AS user2_name, u2.elo_rating AS user2_elo_rating
            FROM friends f
            JOIN users u1 ON f.user_id1 = u1.user_id
            JOIN users u2 ON f.user_id2 = u2.user_id
            WHERE f.user_id1 = ? OR f.user_id2 = ?
        """;

        List<friend_info> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapFriend(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách bạn bè", e);
        }
        return list;
    }

    /**
     * Lấy thông tin chi tiết của 1 mối quan hệ bạn bè cụ thể
     */
    public friend_info getFriendshipById(int friendshipId) {
        String sql = """
            SELECT f.*, 
                   u1.user_name AS user1_name, u1.elo_rating AS user1_elo_rating,
                   u2.user_name AS user2_name, u2.elo_rating AS user2_elo_rating
            FROM friends f
            JOIN users u1 ON f.user_id1 = u1.user_id
            JOIN users u2 ON f.user_id2 = u2.user_id
            WHERE f.friendship_id = ?
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, friendshipId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapFriend(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi truy xuất mối quan hệ bạn bè", e);
        }
        return null;
    }

    // ------------------------- UPDATE -------------------------
    /**
     * Chấp nhận lời mời kết bạn → đổi trạng thái sang "ACCEPTED"
     */
    public boolean acceptFriendRequest(int friendshipId) {
        String sql = "UPDATE friends SET status = 'ACCEPTED' WHERE friendship_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, friendshipId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi chấp nhận kết bạn", e);
        }
    }

     // Từ chối hoặc hủy lời mời kết bạn → đổi trạng thái sang "REJECTED"

    public boolean rejectFriendRequest(int friendshipId) {
        String sql = "UPDATE friends SET status = 'REJECTED' WHERE friendship_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, friendshipId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi từ chối/hủy kết bạn", e);
        }
    }

    // ------------------------- DELETE -------------------------
    public boolean deleteFriendship(int friendshipId) {
        String sql = "DELETE FROM friends WHERE friendship_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, friendshipId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi xóa bạn bè", e);
        }
    }
}
