package com.chatapp.server.Model.DAO;

import com.chatapp.server.Model.Entity.chat_participant;
import com.chatapp.server.Model.Entity.chat_room;
import com.chatapp.server.Model.Entity.user;
import com.chatapp.server.Model.Entity.ChatParticipantKey;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * chat_participantDAO - Xử lý truy vấn database cho chat_participant
 * - Quản lý người tham gia phòng chat
 * - Không xử lý transaction (transaction được quản lý ở Service)
 */
public class chat_participantDAO {

    private final EntityManager em;

    public chat_participantDAO(EntityManager em) {
        this.em = em;
    }

    // ========================= INNER CLASS =========================

    /**
     * Class chứa thông tin participant kèm user details
     */
    public static class ParticipantWithUser {
        public int chatroomId;
        public int userId;
        public String username;
        public String email;
        public Integer eloRating;
        public LocalDateTime joinedAt;

        public ParticipantWithUser(int chatroomId, int userId, String username,
                                   String email, Integer eloRating, LocalDateTime joinedAt) {
            this.chatroomId = chatroomId;
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.eloRating = eloRating;
            this.joinedAt = joinedAt;
        }
    }

    // ========================= THÊM PARTICIPANT =========================

    /**
     * Thêm user vào chat room
     * @param chatroomId ID của chat room
     * @param userId ID của user
     * @return chat_participant vừa tạo
     */
    public chat_participant addParticipant(int chatroomId, int userId) {
        chat_participant participant = new chat_participant();
        participant.setChatroom_id(chatroomId);
        participant.setUser_id(userId);
        participant.setJoined_at(LocalDateTime.now());

        em.persist(participant);
        return participant;
    }

    // ========================= XÓA PARTICIPANT =========================

    /**
     * Xóa user khỏi chat room
     * @param chatroomId ID của chat room
     * @param userId ID của user
     * @return true nếu xóa thành công
     */
    public boolean removeParticipant(int chatroomId, int userId) {
        ChatParticipantKey key = new ChatParticipantKey(chatroomId, userId);
        chat_participant participant = em.find(chat_participant.class, key);

        if (participant != null) {
            em.remove(participant);
            return true;
        }
        return false;
    }

    // ========================= TRUY VẤN CƠ BẢN =========================

    /**
     * Lấy participant theo composite key
     * @param chatroomId ID của chat room
     * @param userId ID của user
     * @return chat_participant hoặc null
     */
    public chat_participant getParticipant(int chatroomId, int userId) {
        ChatParticipantKey key = new ChatParticipantKey(chatroomId, userId);
        return em.find(chat_participant.class, key);
    }

    /**
     * Lấy tất cả participants trong một chat room
     * @param chatroomId ID của chat room
     * @return Danh sách chat_participant
     */
    public List<chat_participant> getParticipantsByRoom(int chatroomId) {
        try {
            return em.createQuery(
                            "SELECT p FROM chat_participant p WHERE p.chatroom_id = :roomId ORDER BY p.joined_at ASC",
                            chat_participant.class
                    ).setParameter("roomId", chatroomId)
                    .getResultList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Lấy tất cả chat rooms mà user tham gia
     * @param userId ID của user
     * @return Danh sách chat_participant
     */
    public List<chat_participant> getParticipantsByUser(int userId) {
        try {
            return em.createQuery(
                            "SELECT p FROM chat_participant p WHERE p.user_id = :userId ORDER BY p.joined_at DESC",
                            chat_participant.class
                    ).setParameter("userId", userId)
                    .getResultList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Lấy participants với thông tin user đầy đủ
     * @param chatroomId ID của chat room
     * @return Danh sách ParticipantWithUser
     */
    public List<ParticipantWithUser> getParticipantsWithUserInfo(int chatroomId) {
        try {
            List<Object[]> results = em.createQuery(
                            "SELECT p.chatroom_id, p.user_id, u.userName, u.email, u.eloRating, p.joined_at " +
                                    "FROM chat_participant p " +
                                    "JOIN p.user u " +
                                    "WHERE p.chatroom_id = :roomId " +
                                    "ORDER BY p.joined_at ASC",
                            Object[].class
                    ).setParameter("roomId", chatroomId)
                    .getResultList();

            return results.stream()
                    .map(row -> new ParticipantWithUser(
                            (Integer) row[0],
                            (Integer) row[1],
                            (String) row[2],
                            (String) row[3],
                            (Integer) row[4],
                            (LocalDateTime) row[5]
                    ))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    // ========================= KIỂM TRA =========================

    /**
     * Kiểm tra user có phải là participant của chat room không
     * @param chatroomId ID của chat room
     * @param userId ID của user
     * @return true nếu user là participant
     */
    public boolean isParticipant(int chatroomId, int userId) {
        ChatParticipantKey key = new ChatParticipantKey(chatroomId, userId);
        return em.find(chat_participant.class, key) != null;
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Đếm số participants trong một chat room
     * @param chatroomId ID của chat room
     * @return Số lượng participants
     */
    public int countParticipantsByRoom(int chatroomId) {
        return em.createQuery(
                        "SELECT COUNT(p) FROM chat_participant p WHERE p.chatroom_id = :roomId",
                        Long.class
                ).setParameter("roomId", chatroomId)
                .getSingleResult().intValue();
    }

    /**
     * Đếm số chat rooms mà user tham gia
     * @param userId ID của user
     * @return Số lượng chat rooms
     */
    public int countRoomsByUser(int userId) {
        return em.createQuery(
                        "SELECT COUNT(p) FROM chat_participant p WHERE p.user_id = :userId",
                        Long.class
                ).setParameter("userId", userId)
                .getSingleResult().intValue();
    }
}