package Model.DAO;

import Model.Entity.user;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class userDAO {
    private Connection conn;

    public userDAO(Connection conn) {
        this.conn = conn;
    }

    private user mapResultSetToUser(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("create_at");
        LocalDateTime createdAt = (timestamp != null) ? timestamp.toLocalDateTime() : LocalDateTime.now();

        return new user(
                rs.getInt("user_id"),
                rs.getString("user_name"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getInt("elo_rating"),
                rs.getInt("win_count"),
                rs.getInt("loss_count"),
                createdAt,
                rs.getString("avatar_url"),
                rs.getString("provider"),
                rs.getString("provider_id"),
                rs.getString("status")
        );
    }

    // ========== ĐĂNG NHẬP THÔNG THƯỜNG ==========
    public user login(String username, String password) throws SQLException {
        String query = "SELECT * FROM users WHERE user_name = ? AND password = ? AND status = 'Offline'";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    // ========== ĐĂNG KÝ THÔNG THƯỜNG ==========
    public user createUser(String username, String password) throws SQLException {
        if (password == null || password.isEmpty()) {
            password = "default_pass";
        }

        String query = "INSERT INTO users (user_name, password, elo_rating, win_count, loss_count, status) " +
                "VALUES (?, ?, 1200, 0, 0, 'Offline')";

        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);
                    return getUser(userId);
                }
            }
        }
        return null;
    }

    public boolean isUsernameExists(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE user_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    public boolean isEmailExists(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    // ========== GOOGLE OAUTH ==========
    public user getUserByGoogleId(String googleId) throws SQLException {
        String query = "SELECT * FROM users WHERE provider_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, googleId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapResultSetToUser(rs);
        }
        return null;
    }

    public user createUserWithGoogle(String email, String googleId, String displayName, String avatarUrl) throws SQLException {
        String query = "INSERT INTO users (user_name, email, provider, provider_id, avatar_url, password, elo_rating, win_count, loss_count, status) " +
                "VALUES (?, ?, 'google', ?, ?, 'oauth_default', 1200, 0, 0, 'Offline')";

        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            String username = generateUsernameFromEmail(email);

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, googleId);
            pstmt.setString(4, avatarUrl);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);
                    return getUser(userId);
                }
            }
        }
        return null;
    }

    public boolean linkGoogleAccount(int userId, String googleId) throws SQLException {
        String query = "UPDATE users SET provider = 'google', provider_id = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, googleId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean unlinkGoogleAccount(int userId) throws SQLException {
        String query = "UPDATE users SET provider = NULL, provider_id = NULL WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean hasPassword(int userId) throws SQLException {
        String query = "SELECT password FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String password = rs.getString("password");
                return password != null && !password.isEmpty();
            }
        }
        return false;
    }

    private String generateUsernameFromEmail(String email) throws SQLException {
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String username = baseUsername;
        int counter = 1;

        while (isUsernameExists(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }



// ========== CRUD CƠ BẢN ==========

    /**
     * READ - Lấy tất cả users
     */
    public List<user> getAllUsers() throws SQLException {
        List<user> list = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        }
        return list;
    }

    /**
     * READ - Lấy user theo ID
     */
    public user getUser(int id) throws SQLException {
        String query = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    /**
     * READ - Lấy user theo username
     */
    public user getUserByUsername(String username) throws SQLException {
        String query = "SELECT * FROM users WHERE user_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    /**
     * READ - Lấy user theo email
     */
    public user getUserByEmail(String email) throws SQLException {
        String query = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    /**
     * UPDATE - Cập nhật thông tin user đầy đủ
     */
    public boolean updateUser(user user) throws SQLException {
        String query = "UPDATE users SET user_name = ?, email = ?, password = ?, elo_rating = ?, win_count = ?, loss_count = ?, avatar_url = ?, provider = ?, provider_id = ?, status = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, user.getUser_name());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPassword());
            pstmt.setInt(4, user.getElo_rating());
            pstmt.setInt(5, user.getWin_count());
            pstmt.setInt(6, user.getLoss_count());
            pstmt.setString(7, user.getAvatar_url());
            pstmt.setString(8, user.getProvider());
            pstmt.setString(9, user.getProvider_id());
            pstmt.setString(10, user.getStatus());
            pstmt.setInt(11, user.getUser_id());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * UPDATE - Cập nhật username
     */
    public boolean updateUsername(int userId, String newUsername) throws SQLException {
        String query = "UPDATE users SET user_name = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newUsername);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * UPDATE - Cập nhật email
     */
    public boolean updateEmail(int userId, String newEmail) throws SQLException {
        String query = "UPDATE users SET email = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newEmail);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * UPDATE - Cập nhật password
     */
    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        String query = "UPDATE users SET password = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * UPDATE - Cập nhật avatar
     */
    public boolean updateAvatar(int userId, String avatarUrl) throws SQLException {
        String query = "UPDATE users SET avatar_url = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, avatarUrl);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * UPDATE - Cập nhật ELO rating
     */
    public boolean updateEloRating(int userId, int newElo) throws SQLException {
        String query = "UPDATE users SET elo_rating = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, newElo);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * UPDATE - Tăng số trận thắng
     */
    public boolean incrementWinCount(int userId) throws SQLException {
        String query = "UPDATE users SET win_count = win_count + 1 WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * UPDATE - Tăng số trận thua
     */
    public boolean incrementLossCount(int userId) throws SQLException {
        String query = "UPDATE users SET loss_count = loss_count + 1 WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * UPDATE - Cập nhật kết quả trận đấu (thắng)
     */
    public boolean updateMatchResultWin(int userId, int eloChange) throws SQLException {
        String query = "UPDATE users SET win_count = win_count + 1, elo_rating = elo_rating + ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, eloChange);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * UPDATE - Cập nhật kết quả trận đấu (thua)
     */
    public boolean updateMatchResultLoss(int userId, int eloChange) throws SQLException {
        String query = "UPDATE users SET loss_count = loss_count + 1, elo_rating = elo_rating + ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, eloChange);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * UPDATE - Thay đổi status
     */
    public boolean updateStatus(int userId, String status) throws SQLException {
        String query = "UPDATE users SET status = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    // UPDATE - Cập nhật provider
    public  boolean updateProvider(int userId, String provider) throws SQLException {
        String query = "UPDATE users SET provider = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, provider);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }
    /**
     * DELETE - Xóa user (hard delete)
     */
    public boolean deleteUser(int userId) throws SQLException {
        String query = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * DELETE - Xóa user (soft delete - đổi status thành 'deleted')
     */
//    public boolean softDeleteUser(int userId) throws SQLException {
//        return updateStatus(userId, "deleted");
//    }

    /**
     * Khôi phục user đã bị soft delete
     */
//    public boolean restoreUser(int userId) throws SQLException {
//        return updateStatus(userId, "active");
//    }

    // ========== QUERIES & FILTERING ==========

    /**
     * Lấy users theo ELO rating giảm dần
     */
    public List<user> getUsersOrderByElo() throws SQLException {
        List<user> users = new ArrayList<>();
        String query = "SELECT * FROM users ORDER BY elo_rating DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * Lấy users theo số trận thắng giảm dần
     */
    public List<user> getUsersOrderByWins() throws SQLException {
        List<user> users = new ArrayList<>();
        String query = "SELECT * FROM users ORDER BY win_count DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * Lấy users theo thời gian tạo giảm dần
     */
    public List<user> getUsersOrderByCreateTimeDesc() throws SQLException {
        List<user> users = new ArrayList<>();
        String query = "SELECT * FROM users ORDER BY create_at DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * Lấy top N users theo ELO
     */
    public List<user> getTopUsersByElo(int limit) throws SQLException {
        List<user> users = new ArrayList<>();
        String query = "SELECT * FROM users ORDER BY elo_rating DESC LIMIT ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * Tìm kiếm users theo tên (LIKE)
     */
    public List<user> searchUsersByName(String keyword) throws SQLException {
        List<user> users = new ArrayList<>();
        String query = "SELECT * FROM users WHERE user_name LIKE ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * Lấy users có ELO trong khoảng
     */
    public List<user> getUsersByEloRange(int minElo, int maxElo) throws SQLException {
        List<user> users = new ArrayList<>();
        String query = "SELECT * FROM users WHERE elo_rating BETWEEN ? AND ? ORDER BY elo_rating DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, minElo);
            pstmt.setInt(2, maxElo);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * Lấy users được tạo sau một thời điểm
     */
    public List<user> getUsersCreatedAfter(LocalDateTime dateTime) throws SQLException {
        List<user> users = new ArrayList<>();
        String query = "SELECT * FROM users WHERE create_at > ? ORDER BY create_at DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(dateTime));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * Đếm tổng số users
     */
    public int getTotalUserCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM users";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Lấy users theo status
     */
    public List<user> getUsersByStatus(String status) throws SQLException {
        List<user> users = new ArrayList<>();
        String query = "SELECT * FROM users WHERE status = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * Lấy users với phân trang
     */
    public List<user> getUsersWithPagination(int page, int pageSize) throws SQLException {
        List<user> users = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        String query = "SELECT * FROM users ORDER BY user_id LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, offset);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * Kiểm tra user có tồn tại không
     */
    public boolean userExists(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // ========== STATISTICS ==========

    /**
     * Lấy thống kê tổng quan
     */
    public UserStatistics getUserStatistics(int userId) throws SQLException {
        user u = getUser(userId);
        if (u == null) return null;

        int totalMatches = u.getWin_count() + u.getLoss_count();
        double winRate = totalMatches > 0 ? (double) u.getWin_count() / totalMatches * 100 : 0;

        return new UserStatistics(
                u.getUser_id(),
                u.getUser_name(),
                u.getElo_rating(),
                u.getWin_count(),
                u.getLoss_count(),
                totalMatches,
                winRate
        );
    }

    /**
     * Class lưu thống kê user
     */
    public static class UserStatistics {
        public int userId;
        public String username;
        public int eloRating;
        public int wins;
        public int losses;
        public int totalMatches;
        public double winRate;

        public UserStatistics(int userId, String username, int eloRating, int wins, int losses, int totalMatches, double winRate) {
            this.userId = userId;
            this.username = username;
            this.eloRating = eloRating;
            this.wins = wins;
            this.losses = losses;
            this.totalMatches = totalMatches;
            this.winRate = winRate;
        }
    }
}