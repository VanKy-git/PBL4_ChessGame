package com.database.server.Service;

import com.database.server.DAO.friendsDAO;
import com.database.server.Entity.friends;
import com.database.server.Entity.user;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class friendsService {

    private final EntityManagerFactory emf;

    public friendsService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public boolean sendFriendRequest(int senderId, int receiverId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            friendsDAO dao = new friendsDAO(em);
            user sender = em.find(user.class, senderId);
            user receiver = em.find(user.class, receiverId);

            if (sender == null || receiver == null) {
                throw new IllegalArgumentException("Sender or receiver not found");
            }
            if (senderId == receiverId) {
                throw new IllegalArgumentException("Cannot send friend request to yourself");
            }

            List<friends> existing = dao.getFriendsOfUser(senderId);
            boolean alreadyExists = existing.stream().anyMatch(f ->
                    (f.getUser1().getUserId() == senderId && f.getUser2().getUserId() == receiverId) ||
                    (f.getUser1().getUserId() == receiverId && f.getUser2().getUserId() == senderId)
            );
            if (alreadyExists) {
                throw new RuntimeException("Friend request already exists or you are already friends.");
            }

            dao.addFriendRequest(senderId, receiverId);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<friends> getFriendsOfUser(int userId) {
        EntityManager em = emf.createEntityManager();
        try {
            friendsDAO dao = new friendsDAO(em);
            return dao.getFriendsOfUser(userId);
        } finally {
            em.close();
        }
    }

    public boolean acceptFriendRequest(int friendshipId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            friendsDAO dao = new friendsDAO(em);
            friends f = dao.getFriendById(friendshipId);
            if (f == null || !"pending".equalsIgnoreCase(f.getStatus())) {
                em.getTransaction().rollback();
                return false;
            }
            f.setStatus("accepted");
            dao.updateFriend(f);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean rejectFriendRequest(int friendshipId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            friendsDAO dao = new friendsDAO(em);
            friends f = dao.getFriendById(friendshipId);
            if (f == null || !"pending".equalsIgnoreCase(f.getStatus())) {
                em.getTransaction().rollback();
                return false;
            }
            dao.deleteFriendship(friendshipId); // Or update status to REJECTED
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean deleteFriendship(int friendshipId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            friendsDAO dao = new friendsDAO(em);
            boolean result = dao.deleteFriendship(friendshipId);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
