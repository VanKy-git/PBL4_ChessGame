package com.database.server.DAO;

import com.database.server.Entity.friends;
import com.database.server.Entity.user;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

public class friendsDAO {

    private final EntityManager em;

    public friendsDAO(EntityManager em) {
        this.em = em;
    }

    // Gửi lời mời kết bạn
    public boolean addFriendRequest(int user1Id, int user2Id) {
        user u1 = em.find(user.class, user1Id);
        user u2 = em.find(user.class, user2Id);
        if (u1 == null || u2 == null) return false;

        friends f = new friends();
        f.setUser1(u1);
        f.setUser2(u2);
        f.setStatus("PENDING");
        f.setCreatedAt(LocalDateTime.now());

        em.persist(f);
        return true;
    }

    // Lấy tất cả bạn bè hoặc lời mời của 1 người dùng
    public List<friends> getFriendsOfUser(int userId) {
        return em.createQuery("""
                SELECT f FROM friends f
                JOIN FETCH f.user1
                JOIN FETCH f.user2
                WHERE f.user1.user_id = :id OR f.user2.user_id = :id
            """, friends.class)
                .setParameter("id", userId)
                .getResultList();
    }

    public friends getFriendById(int friendshipId) {
        return em.find(friends.class, friendshipId);
    }

    public boolean updateFriend (friends f) {
        return em.merge(f) != null;
    }

    // Chấp nhận kết bạn
    public boolean acceptFriendRequest(int friendshipId) {
        friends f = em.find(friends.class, friendshipId);
        if (f == null) return false;
        f.setStatus("ACCEPTED");
        em.merge(f);
        return true;
    }

    // Từ chối / hủy
    public boolean rejectFriendRequest(int friendshipId) {
        friends f = em.find(friends.class, friendshipId);
        if (f == null) return false;
        f.setStatus("REJECTED");
        em.merge(f);
        return true;
    }

    // Xóa bạn bè
    public boolean deleteFriendship(int friendshipId) {
        friends f = em.find(friends.class, friendshipId);
        if (f == null) return false;
        em.remove(f);
        return true;
    }
}
