package com.chatapp.server.Service;

import com.chatapp.server.Model.DAO.matchesDAO;
import com.chatapp.server.Model.Entity.matches;
import com.chatapp.server.Model.Entity.user;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

/**
 * matchesService - Xử lý nghiệp vụ liên quan đến trận đấu (Match)
 * - Không thao tác SQL trực tiếp.
 * - Mỗi request mở/đóng EntityManager riêng.
 * - Transaction được quản lý tại Service, không ở DAO.
 */
public class matchesService {

    private final EntityManagerFactory emf;

    public matchesService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // ========================= CRUD CƠ BẢN =========================

    /** Lấy danh sách tất cả các trận đấu */
    public List<matches> getAllMatches() {
        EntityManager em = emf.createEntityManager();
        matchesDAO dao = new matchesDAO(em);
        try {
            return dao.getAllMatches();
        } finally {
            em.close();
        }
    }

    /** Lấy thông tin 1 trận đấu theo ID */
    public matches getMatchById(int matchId) {
        EntityManager em = emf.createEntityManager();
        matchesDAO dao = new matchesDAO(em);
        try {
            return dao.getMatchById(matchId);
        } finally {
            em.close();
        }
    }

    /** Xóa 1 trận đấu */
    public boolean deleteMatch(int matchId) {
        EntityManager em = emf.createEntityManager();
        matchesDAO dao = new matchesDAO(em);
        try {
            em.getTransaction().begin();
            boolean result = dao.deleteMatch(matchId);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /** Thêm một trận đấu mới */
    public matches createMatch(user player1Id, user player2Id) {
        EntityManager em = emf.createEntityManager();
        matchesDAO dao = new matchesDAO(em);
        try {
            em.getTransaction().begin();
            matches m = dao.createMatch(player1Id, player2Id);
            em.getTransaction().commit();
            return m;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========================= MỞ RỘNG: MATCH + NGƯỜI CHƠI =========================

    /** Lấy thông tin chi tiết 1 trận đấu (bao gồm thông tin 2 người chơi) */
    public matchesDAO.match_player getMatchWithPlayers(int matchId) {
        EntityManager em = emf.createEntityManager();
        matchesDAO dao = new matchesDAO(em);
        try {
            return dao.getMatchWithPlayers(matchId);
        } finally {
            em.close();
        }
    }

    /** Lấy danh sách tất cả các trận + thông tin người chơi */
    public List<matchesDAO.match_player> getAllMatchesWithPlayers() {
        EntityManager em = emf.createEntityManager();
        matchesDAO dao = new matchesDAO(em);
        try {
            return dao.getAllMatchesWithPlayers();
        } finally {
            em.close();
        }
    }
}
