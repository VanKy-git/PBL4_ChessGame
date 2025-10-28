package com.chatapp.server.Model.DAO;

import com.chatapp.server.Model.Entity.chat_message;
import com.chatapp.server.Model.Entity.chat_room;
import com.chatapp.server.Model.Entity.user;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * chat_messageDAO - Xử lý truy vấn database cho chat_message
 * - Quản lý tin nhắn trong chat
 * - Không xử lý transaction (transaction được quản lý ở Service)
 */
public class chat_messageDAO {

    private final EntityManager em;

    public chat_messageDAO(EntityManager em) {
        this.em = em;
    }

    // ========================= INNER CLASS =========================

    /**
     * Class chứa thông tin message kèm user details
     */
    public static class MessageWithUser {
        public int messageId;
        public int chatroomId;
        public int userId;
        public String username;
        public String avatarUrl;
        public String message;
        public LocalDateTime sentAt;

        public MessageWithUser(int messageId, int chatroomId, int userId,
                               String username, String avatarUrl, String message, LocalDateTime sentAt) {
            this.messageId = messageId;
            this.chatroomId = chatroomId;
            this.userId = userId;
            this.username = username;
            this.avatarUrl = avatarUrl;
            this.message = message;
            this.sentAt = sentAt;
        }
    }

    // ========================= GỬI MESSAGE =========================

    /**
     * Gửi tin nhắn mới
     * @param chatroomId ID của chat room
     * @param userId ID của user gửi
     * @param message Nội dung tin nhắn
     * @return chat_message vừa tạo
     */
//    public chat_message sendMessage(int chatroomId, int userId, String message) {
//        chat_message msg = new chat_message();
//
//        chat_room room = em.find(chat_room.class, chatroomId);
//        user sender = em.find(user.class, userId);
//
//        msg.setChatRoom(room);
//        msg.setUser(sender);
//        msg.setMessage(message);
//        msg.setSend_at(LocalDateTime.now());
//
//        em.persist(msg);
//        return msg;
//    }

    // 🔧 Gửi tin nhắn mới
    public chat_message sendMessage(int chatroomId, int userId, String message) {
        chat_room room = em.find(chat_room.class, chatroomId);
        user sender = em.find(user.class, userId);

        if (room == null || sender == null)
            throw new IllegalArgumentException("Chat room hoặc user không tồn tại.");

        chat_message msg = new chat_message();
        msg.setChatRoom(room);
        msg.setUser(sender);
        msg.setMessage(message);
        msg.setSend_at(LocalDateTime.now());

        em.persist(msg);
        return msg;
    }
    // ========================= CRUD CƠ BẢN =========================

    /**
     * Lấy message theo ID
     * @param messageId ID của message
     * @return chat_message hoặc null
     */
    public chat_message getMessageById(int messageId) {
        return em.find(chat_message.class, messageId);
    }

    /**
     * Xóa message
     * @param messageId ID của message cần xóa
     * @return true nếu xóa thành công
     */
    public boolean deleteMessage(int messageId) {
        chat_message msg = em.find(chat_message.class, messageId);
        if (msg != null) {
            em.remove(msg);
            return true;
        }
        return false;
    }

    /**
     * Cập nhật nội dung message
     * @param messageId ID của message
     * @param newMessage Nội dung mới
     * @return true nếu cập nhật thành công
     */
    public boolean updateMessage(int messageId, String newMessage) {
        chat_message msg = em.find(chat_message.class, messageId);
        if (msg != null) {
            msg.setMessage(newMessage);
            em.merge(msg);
            return true;
        }
        return false;
    }

    // ========================= TRUY VẤN THEO ĐIỀU KIỆN =========================

    /**
     * Lấy tất cả messages trong một chat room
     * @param chatroomId ID của chat room
     * @return Danh sách messages, sắp xếp theo thời gian gửi
     */
    public List<chat_message> getMessagesByRoom(int chatroomId) {
        try {
            return em.createQuery(
                            "SELECT m FROM chat_message m WHERE m.chatRoom.chatroom_id = :roomId ORDER BY m.send_at ASC",
                            chat_message.class
                    ).setParameter("roomId", chatroomId)
                    .getResultList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Lấy messages của một user
     * @param userId ID của user
     * @return Danh sách messages của user
     */
    public List<chat_message> getMessagesByUser(int userId) {
        try {
            return em.createQuery(
                            "SELECT m FROM chat_message m WHERE m.user.user_id = :userId ORDER BY m.send_at DESC",
                            chat_message.class
                    ).setParameter("userId", userId)
                    .getResultList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Lấy messages trong khoảng thời gian
     * @param chatroomId ID của chat room
     * @param startTime Thời gian bắt đầu
     * @param endTime Thời gian kết thúc
     * @return Danh sách messages
     */
    public List<chat_message> getMessagesByTimeRange(int chatroomId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return em.createQuery(
                            "SELECT m FROM chat_message m WHERE m.chatRoom.chatroom_id = :roomId " +
                                    "AND m.send_at BETWEEN :start AND :end ORDER BY m.send_at ASC",
                            chat_message.class
                    ).setParameter("roomId", chatroomId)
                    .setParameter("start", startTime)
                    .setParameter("end", endTime)
                    .getResultList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Lấy messages gần đây nhất (limit)
     * @param chatroomId ID của chat room
     * @param limit Số lượng messages
     * @return Danh sách messages gần nhất
     */
    public List<chat_message> getRecentMessages(int chatroomId, int limit) {
        try {
            return em.createQuery(
                            "SELECT m FROM chat_message m WHERE m.chatRoom.chatroom_id = :roomId " +
                                    "ORDER BY m.send_at DESC",
                            chat_message.class
                    ).setParameter("roomId", chatroomId)
                    .setMaxResults(limit)
                    .getResultList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Lấy messages kèm thông tin user
     * @param chatroomId ID của chat room
     * @return Danh sách MessageWithUser
     */
    public List<MessageWithUser> getMessagesWithUserInfo(int chatroomId) {
        try {
            List<Object[]> results = em.createQuery(
                            "SELECT m.message_id, m.chatRoom.chatroom_id, m.user.user_id, " +
                                    "m.user.userName, m.user.avatarUrl, m.message, m.send_at " +
                                    "FROM chat_message m " +
                                    "WHERE m.chatRoom.chatroom_id = :roomId " +
                                    "ORDER BY m.send_at ASC",
                            Object[].class
                    ).setParameter("roomId", chatroomId)
                    .getResultList();

            return results.stream()
                    .map(row -> new MessageWithUser(
                            (Integer) row[0],
                            (Integer) row[1],
                            (Integer) row[2],
                            (String) row[3],
                            (String) row[4],
                            (String) row[5],
                            (LocalDateTime) row[6]
                    ))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Tìm kiếm messages theo keyword
     * @param chatroomId ID của chat room
     * @param keyword Từ khóa tìm kiếm
     * @return Danh sách messages chứa keyword
     */
    public List<chat_message> searchMessages(int chatroomId, String keyword) {
        try {
            return em.createQuery(
                            "SELECT m FROM chat_message m WHERE m.chatRoom.chatroom_id = :roomId " +
                                    "AND LOWER(m.message) LIKE LOWER(:keyword) ORDER BY m.send_at DESC",
                            chat_message.class
                    ).setParameter("roomId", chatroomId)
                    .setParameter("keyword", "%" + keyword + "%")
                    .getResultList();
        } catch (Exception e) {
            return List.of();
        }
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Đếm số messages trong chat room
     * @param chatroomId ID của chat room
     * @return Số lượng messages
     */
    public int countMessagesByRoom(int chatroomId) {
        return em.createQuery(
                        "SELECT COUNT(m) FROM chat_message m WHERE m.chatRoom.chatroom_id = :roomId",
                        Long.class
                ).setParameter("roomId", chatroomId)
                .getSingleResult().intValue();
    }

    /**
     * Đếm số messages của user
     * @param userId ID của user
     * @return Số lượng messages
     */
    public int countMessagesByUser(int userId) {
        return em.createQuery(
                        "SELECT COUNT(m) FROM chat_message m WHERE m.user.user_id = :userId",
                        Long.class
                ).setParameter("userId", userId)
                .getSingleResult().intValue();
    }

    /**
     * Lấy message cuối cùng trong chat room
     * @param chatroomId ID của chat room
     * @return Message cuối cùng hoặc null
     */
    public chat_message getLastMessage(int chatroomId) {
        try {
            return em.createQuery(
                            "SELECT m FROM chat_message m WHERE m.chatRoom.chatroom_id = :roomId " +
                                    "ORDER BY m.send_at DESC",
                            chat_message.class
                    ).setParameter("roomId", chatroomId)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}