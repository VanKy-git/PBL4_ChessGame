package Model.DAO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class matchesDAO {
    private Connection conn;

    public matchesDAO(Connection conn) {
        this.conn = conn;
    }

    // Class để chứa trận + thông tin người chơi
    public static class match_player {
        public int match_id;
        public int player1_id;
        public int player2_id;
        public String player1_name;
        public Integer player1_elo_rating;
        public Integer player2_elo_rating;
        public String player2_name;
        public String match_status;
        public LocalDateTime start_time;
        public LocalDateTime end_time;
        public String pgn_notation;
    }

    // Map ResultSet → match_player
    private match_player mapMatchPlayer(ResultSet rs) throws SQLException {
        match_player mp = new match_player();
        mp.match_id = rs.getInt("match_id");
        mp.match_status = rs.getString("match_status");
        Timestamp start = rs.getTimestamp("start_time");
        Timestamp end = rs.getTimestamp("end_time");
        mp.start_time = (start != null) ? start.toLocalDateTime() : null;
        mp.end_time = (end != null) ? end.toLocalDateTime() : null;
        mp.pgn_notation = rs.getString("pgn_notation");

        mp.player1_id = rs.getInt("player1_id");
        mp.player1_name = rs.getString("player1_name");
        mp.player1_elo_rating = (Integer) rs.getObject("player1_elo_rating");

        mp.player2_id = (Integer) rs.getObject("player2_id");
        mp.player2_name = rs.getString("player2_name");
        mp.player2_elo_rating = (Integer) rs.getObject("player2_elo_rating");

        return mp;
    }

    // ==== TEMPLATE QUERY CHÍNH ====
    private static final String BASE_SELECT = """
        SELECT m.match_id, m.match_status, m.start_time, m.end_time, m.pgn_notation,
               p1.user_id AS player1_id, p1.user_name AS player1_name, p1.elo_rating AS player1_elo_rating,
               p2.user_id AS player2_id, p2.user_name AS player2_name, p2.elo_rating AS player2_elo_rating
        FROM matches m
        LEFT JOIN users p1 ON m.player1_id = p1.user_id
        LEFT JOIN users p2 ON m.player2_id = p2.user_id
    """;

    // ==== CREATE ====
    public match_player createMatch(int player1_id, int player2_id) {
        String sql = "INSERT INTO matches (player1_id, player2_id, start_time, match_status) VALUES (?, ?, NOW(), 'playing') RETURNING match_id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, player1_id);
            stmt.setInt(2, player2_id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int match_id = rs.getInt("match_id");
                return getMatchById(match_id);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // ==== READ ====

    // Lấy 1 trận
    public match_player getMatchById(int match_id) {
        String sql = BASE_SELECT + " WHERE m.match_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, match_id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapMatchPlayer(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // Lấy tất cả trận
    public List<match_player> getAllMatches() {
        List<match_player> list = new ArrayList<>();
        String sql = BASE_SELECT + " ORDER BY m.start_time DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapMatchPlayer(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    // Lấy tất cả trận có liên quan tới 1 user
    public List<match_player> getMatchesByUserId(int userId) {
        List<match_player> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE m.player1_id = ? OR m.player2_id = ? ORDER BY m.start_time DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapMatchPlayer(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    // ==== UPDATE ====

    // Cập nhật trạng thái trận
    public boolean updateMatchStatus(int match_id, String status) {
        String sql = "UPDATE matches SET match_status = ? WHERE match_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, match_id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Cập nhật PGN notation
    public boolean updatePGN(int match_id, String pgn) {
        String sql = "UPDATE matches SET pgn_notation = ? WHERE match_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pgn);
            stmt.setInt(2, match_id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Kết thúc trận
    public boolean endMatch(int match_id, String finalPGN, String status) {
        String sql = "UPDATE matches SET end_time = NOW(), pgn_notation = ?, match_status = ? WHERE match_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, finalPGN);
            stmt.setString(2, status);
            stmt.setInt(3, match_id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ==== DELETE ====
    public boolean deleteMatch(int match_id) {
        String sql = "DELETE FROM matches WHERE match_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, match_id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
