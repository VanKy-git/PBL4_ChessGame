package Model.DAO;

import Model.Entity.user;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class userDAO {
    private Connection conn;

    public userDAO(Connection conn) {
        this.conn = conn;
    }

    private user mapResultSetToUser(ResultSet rs) throws SQLException {
        return new user(
                rs.getInt("user_id"),
                rs.getString("user_name"),
                rs.getString("password"),
                rs.getInt("elo_rating"),
                rs.getInt("win_count"),
                rs.getInt("loss_count"),
                rs.getTimestamp("create_at").toLocalDateTime()
        );
    }

    public user login (String username, String password) throws SQLException {
        String query = "SELECT * FROM users WHERE user_name = ? AND password = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<user> getAllUsers() throws SQLException {
        List<user> list = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
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
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
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

}
