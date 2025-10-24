package com.chatapp.server.Model.Service;

import com.chatapp.server.Model.DAO.chat_messageDAO;
import com.chatapp.server.Model.DAO.chat_messageDAO.MessageWithUser;
import com.chatapp.server.Model.Entity.chat_message;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * chat_messageService - Xử lý nghiệp vụ cho chat_message
 * - Không thao tác SQL trực tiếp
 * - Mỗi request mở/đóng EntityManager riêng
 * - Transaction được quản lý tại Service, không ở DAO
 */
public class chat_messageService {

    private final EntityManagerFactory emf;

    public chat_messageService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // ========================= GỬI MESSAGE =========================

    /**
     * Gửi tin nhắn mới
     */
//    public chat_message sendMessage(int chatroomId, int userId, String message) {
//        EntityManager em = emf.createEntityManager();
//        chat_messageDAO dao = new chat_messageDAO(em);
//
//        try {
//            em.getTransaction().begin();
//            chat_message msg = dao.sendMessage(chatroomId, userId, message);
//            em.getTransaction().commit();
//            return msg;
//        } catch (Exception e) {
//            if (em.getTransaction().isActive()) em.getTransaction().rollback();
//            throw e;
//        } finally {
//            em.close();
//        }
//    }
    // ✅ Gửi tin nhắn (trả DTO tránh lỗi LazyInitializationException)
    public chat_messageDAO.MessageWithUser sendMessage(int chatroomId, int userId, String message) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);

        try {
            em.getTransaction().begin();
            chat_message msg = dao.sendMessage(chatroomId, userId, message);
            em.getTransaction().commit();

            // ✅ Chuyển sang DTO
            return new chat_messageDAO.MessageWithUser(
                    msg.getMessage_id(),
                    msg.getChatRoom().getChatroom_id(),
                    msg.getUser().getUserId(),
                    msg.getUser().getUserName(),
                    msg.getUser().getAvatarUrl(),
                    msg.getMessage(),
                    msg.getSend_at()
            );
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========================= CRUD CƠ BẢN =========================

    /**
     * Lấy message theo ID
     */
    public chat_message getMessageById(int messageId) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);
        try {
            return dao.getMessageById(messageId);
        } finally {
            em.close();
        }
    }

    /**
     * Xóa message
     */
    public boolean deleteMessage(int messageId) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);

        try {
            em.getTransaction().begin();
            boolean result = dao.deleteMessage(messageId);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Cập nhật nội dung message
     */
    public boolean updateMessage(int messageId, String newMessage) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);

        try {
            em.getTransaction().begin();
            boolean result = dao.updateMessage(messageId, newMessage);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========================= TRUY VẤN =========================

    /**
     * Lấy tất cả messages trong chat room
     */
//    public List<chat_message> getMessagesByRoom(int chatroomId) {
//        EntityManager em = emf.createEntityManager();
//        chat_messageDAO dao = new chat_messageDAO(em);
//        try {
//            return dao.getMessagesByRoom(chatroomId);
//        } finally {
//            em.close();
//        }
//    }
    // ✅ Lấy danh sách tin nhắn trong phòng (cũng trả DTO)
    public List<chat_messageDAO.MessageWithUser> getMessagesByRoom(int chatroomId) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);

        try {
            List<chat_message> messages = dao.getMessagesByRoom(chatroomId);
            List<chat_messageDAO.MessageWithUser> result = new ArrayList<>();

            for (chat_message msg : messages) {
                result.add(new chat_messageDAO.MessageWithUser(
                        msg.getMessage_id(),
                        msg.getChatRoom().getChatroom_id(),
                        msg.getUser().getUserId(),
                        msg.getUser().getUserName(),
                        msg.getUser().getAvatarUrl(),
                        msg.getMessage(),
                        msg.getSend_at()
                ));
            }
            return result;
        } finally {
            em.close();
        }
    }

    /**
     * Lấy messages của user
     */
    public List<chat_message> getMessagesByUser(int userId) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);
        try {
            return dao.getMessagesByUser(userId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy messages trong khoảng thời gian
     */
    public List<chat_message> getMessagesByTimeRange(int chatroomId, LocalDateTime startTime, LocalDateTime endTime) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);
        try {
            return dao.getMessagesByTimeRange(chatroomId, startTime, endTime);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy messages gần đây
     */
    public List<chat_message> getRecentMessages(int chatroomId, int limit) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);
        try {
            return dao.getRecentMessages(chatroomId, limit);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy messages kèm thông tin user
     */
    public List<MessageWithUser> getMessagesWithUserInfo(int chatroomId) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);
        try {
            return dao.getMessagesWithUserInfo(chatroomId);
        } finally {
            em.close();
        }
    }

    /**
     * Tìm kiếm messages
     */
    public List<chat_message> searchMessages(int chatroomId, String keyword) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);
        try {
            return dao.searchMessages(chatroomId, keyword);
        } finally {
            em.close();
        }
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Đếm số messages trong chat room
     */
    public int countMessagesByRoom(int chatroomId) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);
        try {
            return dao.countMessagesByRoom(chatroomId);
        } finally {
            em.close();
        }
    }

    /**
     * Đếm số messages của user
     */
    public int countMessagesByUser(int userId) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);
        try {
            return dao.countMessagesByUser(userId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy message cuối cùng
     */
    public chat_message getLastMessage(int chatroomId) {
        EntityManager em = emf.createEntityManager();
        chat_messageDAO dao = new chat_messageDAO(em);
        try {
            return dao.getLastMessage(chatroomId);
        } finally {
            em.close();
        }
    }
}