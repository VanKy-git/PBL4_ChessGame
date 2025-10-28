package com.chatapp.server.Model.Service;

import com.chatapp.server.Model.DAO.movesDAO;
import com.chatapp.server.Model.Entity.moves;
import com.chatapp.server.Model.Entity.matches;
import com.chatapp.server.Model.Entity.user;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;

/**
 * movesService - Xử lý nghiệp vụ cho đối tượng {@link moves}.
 * - Không thao tác SQL trực tiếp.
 * - Mỗi request mở/đóng EntityManager riêng.
 * - Transaction được quản lý tại Service, không ở DAO.
 */
public class movesService {

    private final EntityManagerFactory emf;

    public movesService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // ========================= CRUD CƠ BẢN =========================

    /**
     * Tạo nước đi mới trong trận đấu
     */
    public moves createMove(matches match, user player, int moveNumber,
                            String fromSquare, String toSquare, String piece) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            em.getTransaction().begin();
            moves newMove = dao.createMove(match, player, moveNumber, fromSquare, toSquare, piece);
            em.getTransaction().commit();
            return newMove;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Lấy thông tin nước đi theo ID
     */
    public moves getMoveById(int moveId) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            return dao.getMoveById(moveId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy tất cả nước đi
     */
    public List<moves> getAllMoves() {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            return dao.getAllMoves();
        } finally {
            em.close();
        }
    }

    /**
     * Cập nhật thông tin nước đi
     */
    public boolean updateMove(moves move) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            em.getTransaction().begin();
            boolean result = dao.updateMove(move);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Xóa nước đi
     */
    public boolean deleteMove(int moveId) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            em.getTransaction().begin();
            boolean result = dao.deleteMove(moveId);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Vô hiệu hóa nước đi
     */
    public boolean deactivateMove(int moveId) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            em.getTransaction().begin();
            boolean result = dao.deactivateMove(moveId);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========================= TRUY VẤN THEO MATCH =========================

    /**
     * Lấy tất cả nước đi của trận đấu
     */
    public List<moves> getMovesByMatch(int matchId) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            return dao.getMovesByMatch(matchId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy các nước đi đang hoạt động của trận đấu
     */
    public List<moves> getActiveMovesByMatch(int matchId) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            return dao.getActiveMovesByMatch(matchId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy nước đi mới nhất của trận đấu
     */
    public moves getLastMoveOfMatch(int matchId) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            return dao.getLastMoveOfMatch(matchId);
        } finally {
            em.close();
        }
    }

    /**
     * Đếm số nước đi trong trận đấu
     */
    public int getMoveCountByMatch(int matchId) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            return dao.getMoveCountByMatch(matchId);
        } finally {
            em.close();
        }
    }

    // ========================= TRUY VẤN THEO PLAYER =========================

    /**
     * Lấy tất cả nước đi của người chơi
     */
    public List<moves> getMovesByPlayer(int playerId) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            return dao.getMovesByPlayer(playerId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy nước đi của người chơi trong trận đấu cụ thể
     */
    public List<moves> getMovesByPlayerInMatch(int playerId, int matchId) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            return dao.getMovesByPlayerInMatch(playerId, matchId);
        } finally {
            em.close();
        }
    }

    /**
     * Đếm tổng số nước đi của người chơi
     */
    public int getMoveCountByPlayer(int playerId) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            return dao.getMoveCountByPlayer(playerId);
        } finally {
            em.close();
        }
    }

    // ========================= TRUY VẤN NÂNG CAO =========================

    /**
     * Lấy nước đi trong khoảng thời gian
     */
    public List<moves> getMovesByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            return dao.getMovesByTimeRange(startTime, endTime);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy nước đi theo loại quân cờ
     */
    public List<moves> getMovesByPiece(String piece) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            return dao.getMovesByPiece(piece);
        } finally {
            em.close();
        }
    }

    /**
     * Xóa tất cả nước đi của trận đấu
     */
    public boolean deleteAllMovesByMatch(int matchId) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            em.getTransaction().begin();
            boolean result = dao.deleteAllMovesByMatch(matchId);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Lấy thống kê nước đi của trận đấu
     */
    public movesDAO.MoveStatistics getMoveStatistics(int matchId) {
        EntityManager em = emf.createEntityManager();
        movesDAO dao = new movesDAO(em);
        try {
            return dao.getMoveStatistics(matchId);
        } finally {
            em.close();
        }
    }
}