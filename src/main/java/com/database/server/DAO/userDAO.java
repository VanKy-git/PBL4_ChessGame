package com.database.server.DAO;

import com.database.server.Entity.user;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

public class userDAO {

    private final EntityManager em;

    public userDAO(EntityManager em) {
        this.em = em;
    }

    // ========================= ĐĂNG NHẬP / ĐĂNG KÝ =========================

    public user findByUsername(String username) {
        try {
            TypedQuery<user> query = em.createQuery(
                    "SELECT u FROM user u WHERE u.userName = :username",
                    user.class
            );
            query.setParameter("username", username);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public user createUser(String username, String hashedPassword) {
        user newUser = new user();
        newUser.setUserName(username);
        newUser.setPassword(hashedPassword);
        newUser.setEloRating(1200);
        newUser.setWinCount(0);
        newUser.setLossCount(0);
        newUser.setStatus("Offline");
        newUser.setCreatedAt(LocalDateTime.now());
        em.persist(newUser);
        return newUser;
    }

    public boolean isUsernameExists(String username) {
        Long count = em.createQuery(
                        "SELECT COUNT(u) FROM user u WHERE u.userName = :username", Long.class
                ).setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }

    public boolean isEmailExists(String email) {
        Long count = em.createQuery(
                        "SELECT COUNT(u) FROM user u WHERE u.email = :email", Long.class
                ).setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    // ========================= GOOGLE OAUTH =========================

    public user getUserByGoogleId(String googleId) {
        try {
            TypedQuery<user> query = em.createQuery(
                    "SELECT u FROM user u WHERE u.providerId = :providerId AND u.provider = 'google'",
                    user.class
            );
            query.setParameter("providerId", googleId);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public user getUserByEmail(String email) {
        try {
            TypedQuery<user> query = em.createQuery(
                    "SELECT u FROM user u WHERE u.email = :email", user.class
            );
            query.setParameter("email", email);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public user createUserWithGoogle(String email, String googleId, String displayName, String avatarUrl) {
        String username = generateUsernameFromEmail(email);
        user newUser = new user();
        newUser.setUserName(username);
        newUser.setEmail(email);
        newUser.setProvider("google");
        newUser.setProviderId(googleId);
        newUser.setAvatarUrl(avatarUrl);
        newUser.setPassword("oauth_default");
        newUser.setEloRating(1200);
        newUser.setWinCount(0);
        newUser.setLossCount(0);
        newUser.setStatus("Offline");
        newUser.setCreatedAt(LocalDateTime.now());
        em.persist(newUser);
        return newUser;
    }

    public boolean linkGoogleAccount(int userId, String googleId, String email, String avatarUrl) {
        user u = em.find(user.class, userId);
        if (u == null) return false;
        u.setProvider("google");
        u.setProviderId(googleId);
        if (u.getEmail() == null || u.getEmail().isEmpty()) {
            u.setEmail(email);
        }
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            u.setAvatarUrl(avatarUrl);
        }
        em.merge(u);
        return true;
    }

    public boolean unlinkGoogleAccount(int userId) {
        user u = em.find(user.class, userId);
        if (u == null) return false;
        u.setProvider("local");
        u.setProviderId(null);
        em.merge(u);
        return true;
    }

    private String generateUsernameFromEmail(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String username = base;
        int counter = 1;
        while (isUsernameExists(username)) {
            username = base + counter++;
        }
        return username;
    }

    public boolean hasPassword(int userId) {
        user u = em.find(user.class, userId);
        return u != null && u.getPassword() != null && !u.getPassword().isEmpty() && !"oauth_default".equals(u.getPassword());
    }

    // ========================= CRUD CƠ BẢN =========================

    public List<user> getAllUsers() {
        return em.createQuery("SELECT u FROM user u", user.class).getResultList();
    }

    public user getUserById(int id) {
        return em.find(user.class, id);
    }

    public boolean updateUser(user u) {
        em.merge(u);
        return true;
    }

    public boolean updateStatus(int userId, String status) {
        user u = em.find(user.class, userId);
        if (u != null) {
            u.setStatus(status);
            em.merge(u);
            return true;
        }
        return false;
    }

    public boolean updateEloRating(int userId, int newElo) {
        user u = em.find(user.class, userId);
        if (u != null) {
            u.setEloRating(newElo);
            em.merge(u);
            return true;
        }
        return false;
    }

    public boolean deleteUser(int userId) {
        user u = em.find(user.class, userId);
        if (u != null) {
            em.remove(u);
            return true;
        }
        return false;
    }

    // ========================= TRUY VẤN & THỐNG KÊ =========================

    public List<user> getUsersOrderByElo() {
        return em.createQuery("SELECT u FROM user u ORDER BY u.eloRating DESC", user.class).getResultList();
    }

    public List<user> getTopUsersByElo(int limit) {
        return em.createQuery("SELECT u FROM user u ORDER BY u.eloRating DESC", user.class)
                .setMaxResults(limit)
                .getResultList();
    }

    public int getTotalUserCount() {
        return em.createQuery("SELECT COUNT(u) FROM user u", Long.class)
                .getSingleResult().intValue();
    }

    public List<user> searchUsers(String keyword) {
        return em.createQuery("SELECT u FROM user u WHERE LOWER(u.userName) LIKE LOWER(:keyword)", user.class)
        .setParameter("keyword", "%" + keyword + "%")
        .setMaxResults(20)
        .getResultList();
    }
}
