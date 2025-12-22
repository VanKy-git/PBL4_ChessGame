package com.database.server.Service;

import com.database.server.DAO.matchesDAO;
import com.database.server.Entity.matches;
import com.database.server.Entity.user;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;

/**
 * matchesService - Xử lý nghiệp vụ liên quan đến trận đấu (Match).
 * ✅ Tầng này quản lý EntityManager và Transaction.
 */
public class matchesService {

    private final EntityManagerFactory emf;

    public matchesService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // ========================= TRUY VẤN VÀ ĐỌC (READ) =========================

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
    
    /** * Lấy lịch sử trận đấu chi tiết của một người chơi.
     * ✅ FIX cho lỗi Type Mismatch ở Controller.
     * @return List<matchesDAO.match_player> chứa thông tin trận đấu, tên người chơi, và Elo.
     */
    public List<matches> getMatchesWithPlayersByUser(int userId) {
        EntityManager em = emf.createEntityManager();
        matchesDAO dao = new matchesDAO(em);
        try {
            // Gọi hàm DAO đã được bổ sung để lấy dữ liệu mở rộng
            // Hàm này trả về List<match_player> (kiểu dữ liệu đúng)
            return dao.getMatchesByUser(userId); 
        } finally {
            em.close();
        }
    }

    /** Tìm trận đấu đang diễn ra giữa 2 người chơi */
    public matches getOngoingMatchBetween(int player1Id, int player2Id) {
        EntityManager em = emf.createEntityManager();
        matchesDAO dao = new matchesDAO(em);
        try {
            return dao.getOngoingMatchBetween(player1Id, player2Id);
        } finally {
            em.close();
        }
    }

    // ========================= THAO TÁC GHI (WRITE/TRANSACTION) =========================

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
            // Rollback nếu có lỗi xảy ra
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /** Thêm một trận đấu mới */
    public matches createMatch(user player1, user player2) {
        EntityManager em = emf.createEntityManager();
        matchesDAO dao = new matchesDAO(em);
        try {
            em.getTransaction().begin();
            matches m = dao.createMatch(player1, player2); 
            em.getTransaction().commit();
            return m;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    /** * Kết thúc và cập nhật trạng thái của trận đấu.
     * Sử dụng cho logic game server khi có kết quả.
     */
    public boolean endMatch(int matchId, String status, String pgnNotation) {
         EntityManager em = emf.createEntityManager();
         matchesDAO dao = new matchesDAO(em);
         try {
             em.getTransaction().begin();
             boolean result = dao.endMatch(matchId, status, pgnNotation);
             em.getTransaction().commit();
             return result;
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