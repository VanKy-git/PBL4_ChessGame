package Model.DAO;

import Model.Entity.room;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.time.LocalDateTime;

public class roomDAO {
    private Connection conn;

    public roomDAO(Connection conn) {
        this.conn = conn;
    }

    // Tạo object room từ ResultSet (cho entity room)
    private room newroom(ResultSet rs) throws SQLException {
        return new room(
                rs.getInt("room_id"),
                rs.getInt("host_id"),
                rs.getInt("guest_id"),
                rs.getString("room_status"),
                rs.getTimestamp("create_at").toLocalDateTime() // Timestamp -> LocalDateTime
        );
    }

    // Class để chứa phòng + thông tin host/guest
    public static class roomWithPlayer {
        public int room_id;
        public int host_id;
        public int guest_id;
        public String host_name;
        public Integer host_elo_rating;
        public Integer guest_elo_rating;
        public String guest_name;
        public String room_status;
        public LocalDateTime create_at; // đổi sang LocalDateTime
    }

    // Map ResultSet -> roomWithPlayer (tái sử dụng)
    private roomWithPlayer mapRoomWithPlayer(ResultSet rs) throws SQLException {
        roomWithPlayer rw = new roomWithPlayer();
        rw.room_id = rs.getInt("room_id");
        rw.room_status = rs.getString("room_status");
        rw.create_at = rs.getTimestamp("create_at").toLocalDateTime();

        rw.host_id = rs.getInt("host_id");
        rw.host_name = rs.getString("host_name");
        rw.host_elo_rating = (Integer) rs.getObject("host_elo_rating");

        rw.guest_id = (Integer) rs.getObject("guest_id");
        rw.guest_name = rs.getString("guest_name");
        rw.guest_elo_rating = (Integer) rs.getObject("guest_elo_rating");

        return rw;
    }

    // Template query cơ bản (dùng lại cho nhiều hàm)
    private static final String BASE_SELECT = """
        SELECT r.room_id, r.room_status, r.create_at,
               h.user_id AS host_id, h.user_name AS host_name, h.elo_rating AS host_elo_rating,
               g.user_id AS guest_id, g.user_name AS guest_name, g.elo_rating AS guest_elo_rating
        FROM room r
        LEFT JOIN users h ON r.host_id = h.user_id
        LEFT JOIN users g ON r.guest_id = g.user_id
    """;

    // Hàm lấy 1 phòng theo id
    public roomWithPlayer getRoom(int room_id) {
        String query = BASE_SELECT + " WHERE r.room_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, room_id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRoomWithPlayer(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // SELECT tất cả phòng
    public List<roomWithPlayer> getAllRooms() {
        List<roomWithPlayer> rooms = new ArrayList<>();
        String query = BASE_SELECT + " ORDER BY r.create_at DESC";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rooms.add(mapRoomWithPlayer(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rooms;
    }

    // SELECT tất cả phòng của 1 user (host hoặc guest)
    public List<roomWithPlayer> getAllRoomsByUserId(int userId) {
        List<roomWithPlayer> rooms = new ArrayList<>();
        String query = BASE_SELECT + " WHERE r.host_id = ? OR r.guest_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rooms.add(mapRoomWithPlayer(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rooms;
    }

    // SELECT tất cả phòng theo trạng thái
    public List<roomWithPlayer> getAllRoomsByStatus(String status) {
        List<roomWithPlayer> rooms = new ArrayList<>();
        String query = BASE_SELECT + " WHERE r.room_status = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rooms.add(mapRoomWithPlayer(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rooms;
    }

    // SELECT tất cả phòng tạo sau 1 thời điểm
    public List<roomWithPlayer> getAllRoomsByDate(LocalDateTime after) {
        List<roomWithPlayer> rooms = new ArrayList<>();
        String query = BASE_SELECT + " WHERE r.create_at > ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setTimestamp(1, Timestamp.valueOf(after)); // LocalDateTime -> Timestamp
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rooms.add(mapRoomWithPlayer(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rooms;
    }

    // CREATE room
    public roomWithPlayer room_create(int host_id) {
        int roomId = -1;
        String sql = "INSERT INTO room (host_id, room_status, create_at) VALUES (?, 'waiting', NOW()) RETURNING room_id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, host_id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                roomId = rs.getInt("room_id");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return (roomId != -1) ? getRoom(roomId) : null;
    }

    // UPDATE join guest vào phòng
    public boolean joinRoom(int room_id, int guest_id) {
        String sql = "UPDATE room SET guest_id = ?, room_status = 'playing' WHERE room_id = ? AND guest_id IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, guest_id);
            stmt.setInt(2, room_id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // UPDATE Xóa guest khỏi phòng
    public boolean removeGuest(int roomId) {
        String sql = "UPDATE room SET guest_id = NULL WHERE room_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // UPDATE trạng thái phòng
    public boolean updateRoomStatus(int room_id, String status) {
        String sql = "UPDATE room SET room_status = ? WHERE room_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, room_id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // DELETE room
    public boolean deleteRoom(int room_id) {
        String sql = "DELETE FROM room WHERE room_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, room_id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
