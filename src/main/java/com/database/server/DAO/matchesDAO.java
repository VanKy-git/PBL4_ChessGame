package com.database.server.DAO;

import com.database.server.Entity.matches;
import com.database.server.Entity.user;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DAO quản lý các thao tác với bảng matches (trận đấu)
 * Bao gồm: CRUD, thống kê, và truy vấn mở rộng kèm thông tin người chơi.
 */
public class matchesDAO {

    private final EntityManager em;

    public matchesDAO(EntityManager em) {
        this.em = em;
    }

    // ========================= TẠO & CẬP NHẬT TRẬN ĐẤU =========================

    /** Tạo mới một trận giữa 2 người chơi */
    public matches createMatch(user player1, user player2) {
        matches newMatch = new matches(player1, player2);
        newMatch.setStartTime(LocalDateTime.now());
        newMatch.setMatchStatus("Playing");
        newMatch.setPgnNotation("None");

        em.persist(newMatch);
        return newMatch;
    }

    /** Cập nhật trạng thái hoặc thông tin trận đấu */
    public boolean updateMatch(matches m) {
        if (m == null) return false;
        em.merge(m);
        return true;
    }

    /** Kết thúc trận đấu */
    public boolean endMatch(int matchId, String status, String pgnNotation) {
        matches m = em.find(matches.class, matchId);
        if (m != null) {
            m.setEndTime(LocalDateTime.now());
            m.setMatchStatus(status);
            m.setPgnNotation(pgnNotation);
            em.merge(m);
            return true;
        }
        return false;
    }

    // ========================= TRUY VẤN TRẬN ĐẤU =========================

    /** Lấy tất cả trận đấu */
    public List<matches> getAllMatches() {
        return em.createQuery("SELECT m FROM matches m", matches.class)
                .getResultList();
    }

    /** Lấy trận đấu theo ID */
    public matches getMatchById(int id) {
        return em.find(matches.class, id);
    }

    /** Lấy danh sách trận của 1 người chơi */
    public List<matches> getMatchesByUser(int userId) {
        return em.createQuery(
                        "SELECT m FROM matches m WHERE m.player1.user_id = :uid OR m.player2.user_id = :uid ORDER BY m.startTime DESC",
                        matches.class
                ).setParameter("uid", userId)
                .getResultList();
    }

    /** Tìm trận đang chơi giữa 2 người cụ thể */
    public matches getOngoingMatchBetween(int player1Id, int player2Id) {
        try {
            TypedQuery<matches> query = em.createQuery(
                    "SELECT m FROM matches m WHERE " +
                            "((m.player1.user_id = :p1 AND m.player2.user_id = :p2) OR " +
                            " (m.player1.user_id = :p2 AND m.player2.user_id = :p1)) " +
                            "AND m.matchStatus = 'Playing'",
                    matches.class
            );
            query.setParameter("p1", player1Id);
            query.setParameter("p2", player2Id);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /** Lấy danh sách các trận đang diễn ra */
    public List<matches> getOngoingMatches() {
        return em.createQuery(
                "SELECT m FROM matches m WHERE m.matchStatus = 'Playing'",
                matches.class
        ).getResultList();
    }

    /** Lấy các trận đã hoàn thành */
    public List<matches> getFinishedMatches() {
        return em.createQuery(
                "SELECT m FROM matches m WHERE m.matchStatus = 'Finished' ORDER BY m.endTime DESC",
                matches.class
        ).getResultList();
    }

    // ========================= XÓA TRẬN ĐẤU =========================

    public boolean deleteMatch(int matchId) {
        matches m = em.find(matches.class, matchId);
        if (m != null) {
            em.remove(m);
            return true;
        }
        return false;
    }

    // ========================= THỐNG KÊ TRẬN ĐẤU =========================

    /** Đếm tổng số trận đã lưu */
    public int getTotalMatchCount() {
        return em.createQuery("SELECT COUNT(m) FROM matches m", Long.class)
                .getSingleResult().intValue();
    }

    /** Đếm tổng số trận đang diễn ra */
    public int getOngoingMatchCount() {
        return em.createQuery(
                "SELECT COUNT(m) FROM matches m WHERE m.matchStatus = 'Playing'",
                Long.class
        ).getSingleResult().intValue();
    }

    /** Thống kê tổng quan các trận của 1 người chơi */
    public MatchStatistics getMatchStatisticsForUser(int userId) {
        Long total = em.createQuery(
                        "SELECT COUNT(m) FROM matches m WHERE m.player1.user_id = :uid OR m.player2.user_id = :uid",
                        Long.class
                ).setParameter("uid", userId)
                .getSingleResult();

        Long finished = em.createQuery(
                        "SELECT COUNT(m) FROM matches m WHERE (m.player1.user_id = :uid OR m.player2.user_id = :uid) " +
                                "AND m.matchStatus = 'Finished'",
                        Long.class
                ).setParameter("uid", userId)
                .getSingleResult();

        Long ongoing = em.createQuery(
                        "SELECT COUNT(m) FROM matches m WHERE (m.player1.user_id = :uid OR m.player2.user_id = :uid) " +
                                "AND m.matchStatus = 'Playing'",
                        Long.class
                ).setParameter("uid", userId)
                .getSingleResult();

        return new MatchStatistics(userId, total.intValue(), finished.intValue(), ongoing.intValue());
    }

    // ========================= TRẬN + NGƯỜI CHƠI =========================

    /** Lấy thông tin chi tiết 1 trận đấu, bao gồm thông tin 2 người chơi */
    public match_player getMatchWithPlayers(int matchId) {
        String jpql = """
            SELECT new com.database.server.DAO.matchesDAO$match_player(
                m.matchId,
                m.player1.user_id, m.player1.userName, m.player1.eloRating,
                m.player2.user_id, m.player2.userName, m.player2.eloRating,
                m.matchStatus, m.startTime, m.endTime, m.pgnNotation
            )
            FROM matches m
            WHERE m.matchId = :id
        """;
        try {
            return em.createQuery(jpql, match_player.class)
                    .setParameter("id", matchId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /** Lấy danh sách tất cả các trận đấu kèm thông tin người chơi */
    public List<match_player> getAllMatchesWithPlayers() {
        String jpql = """
            SELECT new com.database.server.DAO.matchesDAO$match_player(
                m.matchId,
                m.player1.user_id, m.player1.userName, m.player1.eloRating,
                m.player2.user_id, m.player2.userName, m.player2.eloRating,
                m.matchStatus, m.startTime, m.endTime, m.pgnNotation
            )
            FROM matches m
            ORDER BY m.startTime DESC
        """;
        return em.createQuery(jpql, match_player.class).getResultList();
    }

    // ========================= LỚP PHỤ TRỢ =========================

    /** Lớp chứa thông tin mở rộng (match + player1 + player2) */
    public static class match_player {
        public int match_id;
        public int player1_id;
        public String player1_name;
        public Integer player1_elo_rating;
        public int player2_id;
        public String player2_name;
        public Integer player2_elo_rating;
        public String match_status;
        public LocalDateTime start_time;
        public LocalDateTime end_time;
        public String pgn_notation;

        public match_player(
                int match_id,
                int player1_id, String player1_name, Integer player1_elo_rating,
                int player2_id, String player2_name, Integer player2_elo_rating,
                String match_status, LocalDateTime start_time, LocalDateTime end_time,
                String pgn_notation) {
            this.match_id = match_id;
            this.player1_id = player1_id;
            this.player1_name = player1_name;
            this.player1_elo_rating = player1_elo_rating;
            this.player2_id = player2_id;
            this.player2_name = player2_name;
            this.player2_elo_rating = player2_elo_rating;
            this.match_status = match_status;
            this.start_time = start_time;
            this.end_time = end_time;
            this.pgn_notation = pgn_notation;
        }
    }

    /** Lớp thống kê cơ bản cho người chơi */
    public static class MatchStatistics {
        public int userId;
        public int totalMatches;
        public int finishedMatches;
        public int ongoingMatches;

        public MatchStatistics(int userId, int totalMatches, int finishedMatches, int ongoingMatches) {
            this.userId = userId;
            this.totalMatches = totalMatches;
            this.finishedMatches = finishedMatches;
            this.ongoingMatches = ongoingMatches;
        }
    }
}