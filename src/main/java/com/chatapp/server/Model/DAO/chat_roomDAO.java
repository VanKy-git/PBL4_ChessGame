package com.chatapp.server.Model.DAO;

import com.chatapp.server.Model.Entity.chat_room;
import com.chatapp.server.Model.Entity.chat_participant;
import com.chatapp.server.Model.Entity.chat_message;
import com.chatapp.server.Model.Entity.user;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * chat_roomDAO - Xử lý truy vấn database cho chat_room
 * - Quản lý phòng chat (1-1, group, room-based)
 * - Không xử lý transaction (transaction được quản lý ở Service)
 */
public class chat_roomDAO {

    private final EntityManager em;

    public chat_roomDAO(EntityManager em) {
        this.em = em;
    }

    // ========================= INNER CLASS: ROOM WITH DETAILS =========================

    /**
     * Class chứa thông tin chi tiết của chat room
     * Bao gồm cả danh sách participants và số lượng messages
     */
    public static class ChatRoomDetails {
        public int chatroomId;
        public String roomType;
        public Integer refId;
        public LocalDateTime createdAt;
        public int participantCount;
        public int messageCount;
        public List<ParticipantInfo> participants;

        public ChatRoomDetails(int chatroomId, String roomType, Integer refId,
                               LocalDateTime createdAt, int participantCount, int messageCount) {
            this.chatroomId = chatroomId;
            this.roomType = roomType;
            this.refId = refId;
            this.createdAt = createdAt;
            this.participantCount = participantCount;
            this.messageCount = messageCount;
        }
    }

    /**
     * Class chứa thông tin người tham gia
     */
    public static class ParticipantInfo {
        public int userId;
        public String username;
        public LocalDateTime joinedAt;

        public ParticipantInfo(int userId, String username, LocalDateTime joinedAt) {
            this.userId = userId;
            this.username = username;
            this.joinedAt = joinedAt;
        }
    }

    // ========================= TẠO PHÒNG CHAT =========================

    /**
     * Tạo phòng chat 1-1 giữa 2 user
     * @param user1Id ID user thứ nhất
     * @param user2Id ID user thứ hai
     * @return chat_room vừa tạo
     */
    public chat_room createPrivateRoom(int user1Id, int user2Id) {
        chat_room newRoom = new chat_room();
        newRoom.setRoom_type("private");
        newRoom.setRef_id(0);
        newRoom.setCreated_at(LocalDateTime.now());

        em.persist(newRoom);
        return newRoom;
    }

    /**
     * Tạo phòng chat dựa trên room game
     * @param roomId ID của room game
     * @return chat_room vừa tạo
     */
    public chat_room createRoomBasedChat(int roomId) {
        chat_room newRoom = new chat_room();
        newRoom.setRoom_type("room");
        newRoom.setRef_id(roomId);
        newRoom.setCreated_at(LocalDateTime.now());

        em.persist(newRoom);
        return newRoom;
    }

    // ========================= CRUD CƠ BẢN =========================

    /**
     * Lấy chat room theo ID
     * @param chatroomId ID của chat room
     * @return chat_room hoặc null nếu không tìm thấy
     */
    public chat_room getChatRoomById(int chatroomId) {
        return em.find(chat_room.class, chatroomId);
    }

    /**
     * Lấy tất cả chat rooms
     * @return Danh sách chat rooms
     */
    public List<chat_room> getAllChatRooms() {
        return em.createQuery("SELECT c FROM chat_room c ORDER BY c.created_at DESC", chat_room.class)
                .getResultList();
    }

    /**
     * Xóa chat room
     * @param chatroomId ID của chat room cần xóa
     * @return true nếu xóa thành công
     */
    public boolean deleteChatRoom(int chatroomId) {
        chat_room room = em.find(chat_room.class, chatroomId);
        if (room != null) {
            em.remove(room);
            return true;
        }
        return false;
    }

    /**
     * Cập nhật ref_id của chat room
     * @param chatroomId ID của chat room
     * @param refId ID tham chiếu mới
     * @return true nếu cập nhật thành công
     */
    public boolean updateRefId(int chatroomId, int refId) {
        chat_room room = em.find(chat_room.class, chatroomId);
        if (room != null) {
            room.setRef_id(refId);
            em.merge(room);
            return true;
        }
        return false;
    }

    // ========================= TRUY VẤN THEO ĐIỀU KIỆN =========================

    /**
     * Lấy các chat rooms theo loại (private, group, room)
     * @param roomType Loại phòng chat
     * @return Danh sách chat rooms
     */
    public List<chat_room> getChatRoomsByType(String roomType) {
        try {
            return em.createQuery(
                            "SELECT c FROM chat_room c WHERE c.room_type = :type ORDER BY c.created_at DESC",
                            chat_room.class
                    ).setParameter("type", roomType)
                    .getResultList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Lấy chat room dựa trên ref_id (dùng cho room-based chat)
     * @param refId ID tham chiếu (ví dụ: room_id)
     * @return chat_room hoặc null
     */
    public chat_room getChatRoomByRefId(int refId) {
        try {
            return em.createQuery(
                            "SELECT c FROM chat_room c WHERE c.ref_id = :refId",
                            chat_room.class
                    ).setParameter("refId", refId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Lấy các chat rooms mà user tham gia
     * @param userId ID của user
     * @return Danh sách chat rooms
     */
    public List<chat_room> getChatRoomsByUserId(int userId) {
        try {
            return em.createQuery(
                            "SELECT DISTINCT c FROM chat_room c " +
                                    "JOIN c.participants p " +
                                    "WHERE p.user.user_id = :userId " +
                                    "ORDER BY c.created_at DESC",
                            chat_room.class
                    ).setParameter("userId", userId)
                    .getResultList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Tìm phòng chat private giữa 2 user
     * @param user1Id ID user thứ nhất
     * @param user2Id ID user thứ hai
     * @return chat_room nếu tìm thấy, null nếu không
     */
    public chat_room findPrivateRoomBetweenUsers(int user1Id, int user2Id) {
        try {
            return em.createQuery(
                            "SELECT c FROM chat_room c " +
                                    "WHERE c.room_type = 'private' " +
                                    "AND EXISTS (SELECT p1 FROM chat_participant p1 WHERE p1.chatRoom = c AND p1.user.user_id = :user1) " +
                                    "AND EXISTS (SELECT p2 FROM chat_participant p2 WHERE p2.chatRoom = c AND p2.user.user_id = :user2)",
                            chat_room.class
                    ).setParameter("user1", user1Id)
                    .setParameter("user2", user2Id)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Đếm tổng số chat rooms trong hệ thống
     * @return Tổng số chat rooms
     */
    public int getTotalChatRoomCount() {
        return em.createQuery("SELECT COUNT(c) FROM chat_room c", Long.class)
                .getSingleResult().intValue();
    }

    /**
     * Đếm số chat rooms theo loại
     * @param roomType Loại phòng chat
     * @return Số lượng
     */
    public int getChatRoomCountByType(String roomType) {
        return em.createQuery(
                        "SELECT COUNT(c) FROM chat_room c WHERE c.room_type = :type", Long.class
                ).setParameter("type", roomType)
                .getSingleResult().intValue();
    }

    /**
     * Đếm số messages trong một chat room
     * @param chatroomId ID của chat room
     * @return Số lượng messages
     */
    public int getMessageCount(int chatroomId) {
        return em.createQuery(
                        "SELECT COUNT(m) FROM chat_message m WHERE m.chatRoom.chatroom_id = :roomId", Long.class
                ).setParameter("roomId", chatroomId)
                .getSingleResult().intValue();
    }

    /**
     * Đếm số participants trong một chat room
     * @param chatroomId ID của chat room
     * @return Số lượng participants
     */
    public int getParticipantCount(int chatroomId) {
        return em.createQuery(
                        "SELECT COUNT(p) FROM chat_participant p WHERE p.chatRoom.chatroom_id = :roomId", Long.class
                ).setParameter("roomId", chatroomId)
                .getSingleResult().intValue();
    }

    /**
     * Lấy thông tin chi tiết của chat room
     * @param chatroomId ID của chat room
     * @return ChatRoomDetails hoặc null
     */
    public ChatRoomDetails getChatRoomDetails(int chatroomId) {
        chat_room room = getChatRoomById(chatroomId);
        if (room == null) return null;

        int participantCount = getParticipantCount(chatroomId);
        int messageCount = getMessageCount(chatroomId);

        ChatRoomDetails details = new ChatRoomDetails(
                room.getChatroom_id(),
                room.getRoom_type(),
                room.getRef_id() > 0 ? room.getRef_id() : null,
                room.getCreated_at(),
                participantCount,
                messageCount
        );

        // Lấy danh sách participants
        details.participants = getParticipantsByRoomId(chatroomId);

        return details;
    }

    /**
     * Lấy danh sách participants của một chat room
     * @param chatroomId ID của chat room
     * @return Danh sách ParticipantInfo
     */
    private List<ParticipantInfo> getParticipantsByRoomId(int chatroomId) {
        try {
            List<Object[]> results = em.createQuery(
                            "SELECT p.user.user_id, p.user.userName, p.joined_at " +
                                    "FROM chat_participant p " +
                                    "WHERE p.chatRoom.chatroom_id = :roomId " +
                                    "ORDER BY p.joined_at ASC",
                            Object[].class
                    ).setParameter("roomId", chatroomId)
                    .getResultList();

            return results.stream()
                    .map(row -> new ParticipantInfo(
                            (Integer) row[0],
                            (String) row[1],
                            (LocalDateTime) row[2]
                    ))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    // ========================= THỐNG KÊ TỔNG QUAN =========================

    /**
     * Class chứa thống kê tổng quan về chat rooms
     */
    public static class ChatRoomStatistics {
        public int totalRooms;
        public int privateRooms;
        public int groupRooms;
        public int roomBasedChats;

        public ChatRoomStatistics(int totalRooms, int privateRooms, int groupRooms, int roomBasedChats) {
            this.totalRooms = totalRooms;
            this.privateRooms = privateRooms;
            this.groupRooms = groupRooms;
            this.roomBasedChats = roomBasedChats;
        }
    }

    /**
     * Lấy thống kê tổng quan về chat rooms
     * @return ChatRoomStatistics
     */
    public ChatRoomStatistics getChatRoomStatistics() {
        int total = getTotalChatRoomCount();
        int privateCount = getChatRoomCountByType("private");
        int groupCount = getChatRoomCountByType("group");
        int roomCount = getChatRoomCountByType("room");

        return new ChatRoomStatistics(total, privateCount, groupCount, roomCount);
    }

    // ========================= KIỂM TRA =========================

    /**
     * Kiểm tra user có phải là thành viên của chat room không
     * @param chatroomId ID của chat room
     * @param userId ID của user
     * @return true nếu user là thành viên
     */
    public boolean isUserInChatRoom(int chatroomId, int userId) {
        Long count = em.createQuery(
                        "SELECT COUNT(p) FROM chat_participant p " +
                                "WHERE p.chatRoom.chatroom_id = :roomId AND p.user.user_id = :userId",
                        Long.class
                ).setParameter("roomId", chatroomId)
                .setParameter("userId", userId)
                .getSingleResult();

        return count > 0;
    }

    /**
     * Kiểm tra chat room có tồn tại không
     * @param chatroomId ID của chat room
     * @return true nếu tồn tại
     */
    public boolean chatRoomExists(int chatroomId) {
        return getChatRoomById(chatroomId) != null;
    }
}