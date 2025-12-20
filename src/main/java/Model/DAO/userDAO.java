package Model.DAO;

import Model.Entity.DBConnection;
import Model.Entity.user;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class userDAO {

    // Không cần constructor với conn nữa, sẽ lấy trực tiếp từ DBConnection
    public userDAO() {}

    private user mapResultSetToUser(ResultSet rs) throws SQLException {
        // Cần tạo một constructor phù hợp trong class 'user'
        // Giả sử class 'user' có constructor đủ các tham số này
        return new user(
                rs.getInt("user_id"),
                rs.getString("user_name"),
                rs.getString("password"),
                rs.getInt("elo_rating"),
                rs.getInt("win_count"),
                rs.getInt("loss_count"),
                rs.getTimestamp("create_at") != null ? rs.getTimestamp("create_at").toLocalDateTime() : null,
                rs.getString("avatar_url"),
                rs.getString("email"),
                rs.getString("status"),
                rs.getString("auth_provider"),
                rs.getString("full_name")
        );
    }

    public user login(String username, String password) throws SQLException {
        String query = "SELECT * FROM users WHERE user_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPasswordFromDB = rs.getString("password");
                // Kiểm tra mật khẩu bằng BCrypt
                if (BCrypt.checkpw(password, hashedPasswordFromDB)) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Ném lại exception để tầng trên xử lý
        }
        return null; // Trả về null nếu sai username hoặc password
    }

    public boolean register(String username, String password, String email, String fullName) throws SQLException {
        String checkUserQuery = "SELECT 1 FROM users WHERE user_name = ? OR email = ?";
        String insertUserQuery = "INSERT INTO users (user_name, password, email, full_name, elo_rating, win_count, loss_count, create_at, status, auth_provider) VALUES (?, ?, ?, ?, 1200, 0, 0, NOW(), 'Offline', 'local')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkUserQuery)) {

            // 1. Kiểm tra username hoặc email đã tồn tại chưa
            checkStmt.setString(1, username);
            checkStmt.setString(2, email);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // User hoặc email đã tồn tại
                    return false;
                }
            }

            // 2. Nếu chưa tồn tại, tiến hành mã hóa và thêm mới
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            try (PreparedStatement insertStmt = conn.prepareStatement(insertUserQuery)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashedPassword);
                insertStmt.setString(3, email);
                insertStmt.setString(4, fullName);

                int affectedRows = insertStmt.executeUpdate();
                return affectedRows > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }


    public List<user> getAllUsers() throws SQLException {
        List<user> list = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return  list;
    }

    public user getUser(int id) throws SQLException {
        String query = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Các hàm còn lại giữ nguyên, chỉ cần sửa cách lấy connection
    public List<user> getUsersOrderByElo() throws SQLException {
        List<user> users = new ArrayList<>();
        String query = "SELECT * FROM users ORDER BY elo_rating DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    public List<user> getUsersOrderByWins() throws SQLException {
        List<user> users = new ArrayList<>();
        String query = "SELECT * FROM users ORDER BY win_count DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    public List<user> getUsersOrderByCreateTimeDesc() throws SQLException {
        List<user> users = new ArrayList<>();
        String query = "SELECT * FROM users ORDER BY create_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }
}
