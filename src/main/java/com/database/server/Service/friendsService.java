package com.database.server.Service;

import com.database.server.DAO.friendsDAO;
import com.database.server.Entity.friends;
import com.database.server.Entity.user;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

//    public List<friends> getFriendsOfUser(int userId) {
//        EntityManager em = emf.createEntityManager();
//        try {
//            friendsDAO dao = new friendsDAO(em);
//            return dao.getFriendsOfUser(userId);
//        } finally {
//            em.close();
//        }
//    }


    public List<Map<String, Object>> getFriendsOfUser(int userId) {
        EntityManager em = emf.createEntityManager();
        try {
            friendsDAO dao = new friendsDAO(em);

            // Lấy danh sách thô từ DB
            List<friends> rawList = dao.getFriendsOfUser(userId);
            List<Map<String, Object>> resultList = new ArrayList<>();

            for (friends f : rawList) {
                user friendUser;

                // Logic xác định ai là bạn
                if (f.getUser1().getUserId() == userId) {
                    friendUser = f.getUser2();
                } else {
                    friendUser = f.getUser1();
                }

                Map<String, Object> friendMap = new HashMap<>();
                friendMap.put("friendship_id", f.getFriendshipId());
                friendMap.put("friend_id", friendUser.getUserId());
                friendMap.put("friend_name", friendUser.getUserName());
                friendMap.put("avatar_url", friendUser.getAvatarUrl());
                friendMap.put("friend_status", friendUser.getStatus());
                friendMap.put("status", f.getStatus());

                // 🔴 QUAN TRỌNG NHẤT: BẮT BUỘC PHẢI CÓ DÒNG NÀY
                // User1 luôn là người gửi (Sender)
                friendMap.put("sender_id", f.getUser1().getUserId());

                resultList.add(friendMap);
            }

            return resultList;

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
