package com.chatapp.server.Model.DAO;

import com.chatapp.server.Model.Entity.moves;
import com.chatapp.server.Model.Entity.matches;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.Persistence;
import java.util.List;

/**
 * DAO quản lý các thao tác CSDL của bảng "moves".
 */
public class movesDAO {

    private static final EntityManager entityManager =
            Persistence.createEntityManagerFactory("PBL4_ChessPU").createEntityManager();

    public static boolean addMove(moves move) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            entityManager.persist(move);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            System.err.println("addMove error: " + e.getMessage());
            return false;
        }
    }

    // chỉnh sửa ghi lạ danh sách nước đi sau trận đã được lưu vào stack
    public static boolean addListMoves(List<moves> listMoves) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            entityManager.persist(listMoves);
            tx.commit();
            return true;
        }  catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            System.err.println("addListMoves error: " + e.getMessage());
            return false;
        }
    }

    public List<moves> getMovesByMatch(matches match) {
        try {
            TypedQuery<moves> query = entityManager.createQuery(
                    "SELECT m FROM moves m WHERE m.match = :match ORDER BY m.moveNumber ASC",
                    moves.class
            );
            query.setParameter("match", match);
            return query.getResultList();
        } catch (Exception e) {
            System.err.println("getMovesByMatch error: " + e.getMessage());
            return List.of();
        }
    }

    public List<moves> getAllMoves() {
        try {
            return entityManager.createQuery("SELECT m FROM moves m", moves.class).getResultList();
        } catch (Exception e) {
            System.err.println("getAllMoves error: " + e.getMessage());
            return List.of();
        }
    }

    public boolean deleteMove(int moveId) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            moves mv = entityManager.find(moves.class, moveId);
            if (mv != null) entityManager.remove(mv);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            System.err.println("deleteMove error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateMove(moves updated) {
        EntityTransaction tx = entityManager.getTransaction();
        try {
            tx.begin();
            entityManager.merge(updated);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            System.err.println("updateMove error: " + e.getMessage());
            return false;
        }
    }

    public moves getMoveById(int id) {
        try {
            return entityManager.find(moves.class, id);
        } catch (Exception e) {
            System.err.println("getMoveById error: " + e.getMessage());
            return null;
        }
    }
}
