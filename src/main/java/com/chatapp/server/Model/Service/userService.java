package com.chatapp.server.Model.Service;

import com.chatapp.server.Model.DAO.userDAO;
import com.chatapp.server.Model.Entity.user;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;

/**
 * userService - Xử lý nghiệp vụ cho đối tượng {@link user}.
 * - Không thao tác SQL trực tiếp.
 * - Mỗi request mở/đóng EntityManager riêng.
 * - Transaction được quản lý tại Service, không ở DAO.
 */
public class userService {

    private final EntityManagerFactory emf;

    public userService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // ========== ĐĂNG NHẬP / ĐĂNG KÝ ==========

    public user login(String username, String password) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            return dao.login(username, password);
        } finally {
            em.close();
        }
    }

    public user register(String username, String password) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);

        try {
            em.getTransaction().begin();

            if (dao.isUsernameExists(username)) {
                throw new RuntimeException("Username already exists!");
            }

            user newUser = dao.createUser(username, password);

            em.getTransaction().commit();
            return newUser;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========== CRUD CƠ BẢN ==========

    public List<user> getAllUsers() {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            return dao.getAllUsers();
        } finally {
            em.close();
        }
    }

    public user getUserById(int id) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            return dao.getUserById(id);
        } finally {
            em.close();
        }
    }

    public boolean deleteUser(int id) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            em.getTransaction().begin();
            boolean result = dao.deleteUser(id);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========== CẬP NHẬT TRẠNG THÁI ==========

    public boolean updateStatus(int userId, String status) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            em.getTransaction().begin();
            boolean updated = dao.updateStatus(userId, status);
            em.getTransaction().commit();
            return updated;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========== GOOGLE AUTH ==========

    public user getUserByGoogleId(String googleId) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            return dao.getUserByGoogleId(googleId);
        } finally {
            em.close();
        }
    }

    public user registerGoogleUser(String email, String googleId, String name, String avatarUrl) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            em.getTransaction().begin();
            user newUser = dao.createUserWithGoogle(email, googleId, name, avatarUrl);
            em.getTransaction().commit();
            return newUser;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
