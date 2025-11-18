package com.database.server.DAO;

import com.database.server.Entity.moves;
import com.database.server.Entity.matches;
import com.database.server.Entity.user;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

public class movesDAO {

    private final EntityManager em;

    public movesDAO(EntityManager em) {
        this.em = em;
    }

    // ========================= CRUD CƠ BẢN =========================

    /**
     * Tạo một nước đi mới trong trận đấu
     * @param match Trận đấu
     * @param player Người chơi thực hiện nước đi
     * @param moveNumber Số thứ tự nước đi
     * @param fromSquare Ô xuất phát (vd: "e2")
     * @param toSquare Ô đích (vd: "e4")
     * @param piece Quân cờ di chuyển (vd: "Pawn", "Knight")
     * @return Đối tượng moves vừa tạo
     */
    public moves createMove(matches match, user player, int moveNumber,
                            String fromSquare, String toSquare, String piece) {
        moves newMove = new moves(match, player, moveNumber, fromSquare, toSquare, piece);
        em.persist(newMove);
        return newMove;
    }

    public moves getMoveById(int moveId) {
        return em.find(moves.class, moveId);
    }

    public List<moves> getAllMoves() {
        return em.createQuery("SELECT m FROM moves m ORDER BY m.moveTime DESC", moves.class)
                .getResultList();
    }

    public boolean updateMove(moves move) {
        em.merge(move);
        return true;
    }

    public boolean deleteMove(int moveId) {
        moves move = em.find(moves.class, moveId);
        if (move != null) {
            em.remove(move);
            return true;
        }
        return false;
    }

    public boolean deactivateMove(int moveId) {
        moves move = em.find(moves.class, moveId);
        if (move != null) {
            move.setActive(false);
            em.merge(move);
            return true;
        }
        return false;
    }

    // ========================= TRUY VẤN THEO MATCH =========================

    public List<moves> getMovesByMatch(int matchId) {
        return em.createQuery(
                "SELECT m FROM moves m WHERE m.match.matchId = :matchId ORDER BY m.moveNumber ASC",
                moves.class
        ).setParameter("matchId", matchId).getResultList();
    }

    public List<moves> getActiveMovesByMatch(int matchId) {
        return em.createQuery(
                "SELECT m FROM moves m WHERE m.match.matchId = :matchId AND m.active = true ORDER BY m.moveNumber ASC",
                moves.class
        ).setParameter("matchId", matchId).getResultList();
    }

    public moves getLastMoveOfMatch(int matchId) {
        try {
            return em.createQuery(
                            "SELECT m FROM moves m WHERE m.match.matchId = :matchId ORDER BY m.moveNumber DESC",
                            moves.class
                    ).setParameter("matchId", matchId)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public int getMoveCountByMatch(int matchId) {
        return em.createQuery(
                "SELECT COUNT(m) FROM moves m WHERE m.match.matchId = :matchId AND m.active = true",
                Long.class
        ).setParameter("matchId", matchId).getSingleResult().intValue();
    }

    // ========================= TRUY VẤN THEO PLAYER =========================

    public List<moves> getMovesByPlayer(int playerId) {
        return em.createQuery(
                "SELECT m FROM moves m WHERE m.player.userId = :playerId ORDER BY m.moveTime DESC",
                moves.class
        ).setParameter("playerId", playerId).getResultList();
    }

    public List<moves> getMovesByPlayerInMatch(int playerId, int matchId) {
        return em.createQuery(
                        "SELECT m FROM moves m WHERE m.player.userId = :playerId AND m.match.matchId = :matchId ORDER BY m.moveNumber ASC",
                        moves.class
                ).setParameter("playerId", playerId)
                .setParameter("matchId", matchId)
                .getResultList();
    }

    public int getMoveCountByPlayer(int playerId) {
        return em.createQuery(
                "SELECT COUNT(m) FROM moves m WHERE m.player.userId = :playerId AND m.active = true",
                Long.class
        ).setParameter("playerId", playerId).getSingleResult().intValue();
    }

    // ========================= TRUY VẤN NÂNG CAO =========================

    public List<moves> getMovesByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return em.createQuery(
                        "SELECT m FROM moves m WHERE m.moveTime BETWEEN :start AND :end ORDER BY m.moveTime DESC",
                        moves.class
                ).setParameter("start", startTime)
                .setParameter("end", endTime)
                .getResultList();
    }

    public List<moves> getMovesByPiece(String piece) {
        return em.createQuery(
                "SELECT m FROM moves m WHERE m.piece = :piece ORDER BY m.moveTime DESC",
                moves.class
        ).setParameter("piece", piece).getResultList();
    }

    public boolean deleteAllMovesByMatch(int matchId) {
        int deleted = em.createQuery(
                "DELETE FROM moves m WHERE m.match.matchId = :matchId"
        ).setParameter("matchId", matchId).executeUpdate();
        return deleted > 0;
    }

    // ========================= THỐNG KÊ =========================

    public MoveStatistics getMoveStatistics(int matchId) {
        List<moves> allMoves = getMovesByMatch(matchId);
        if (allMoves.isEmpty()) return null;

        int totalMoves = allMoves.size();
        int activeMoves = (int) allMoves.stream().filter(moves::isActive).count();

        moves firstMove = allMoves.get(0);
        moves lastMove = allMoves.get(allMoves.size() - 1);

        return new MoveStatistics(
                matchId,
                totalMoves,
                activeMoves,
                firstMove.getMoveTime(),
                lastMove.getMoveTime()
        );
    }

    public static class MoveStatistics {
        public int matchId;
        public int totalMoves;
        public int activeMoves;
        public LocalDateTime firstMoveTime;
        public LocalDateTime lastMoveTime;

        public MoveStatistics(int matchId, int totalMoves, int activeMoves,
                              LocalDateTime firstMoveTime, LocalDateTime lastMoveTime) {
            this.matchId = matchId;
            this.totalMoves = totalMoves;
            this.activeMoves = activeMoves;
            this.firstMoveTime = firstMoveTime;
            this.lastMoveTime = lastMoveTime;
        }
    }
}