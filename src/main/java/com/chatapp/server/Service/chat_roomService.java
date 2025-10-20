package com.chatapp.server.Service;

import com.chatapp.server.Model.DAO.chat_roomDAO;
import com.chatapp.server.Model.DAO.chat_roomDAO.ChatRoomDetails;
import com.chatapp.server.Model.DAO.chat_roomDAO.ChatRoomStatistics;
import com.chatapp.server.Model.Entity.chat_room;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

/**
 * chat_roomService - Xử lý nghiệp vụ cho đối tượng chat_room
 * - Không thao tác SQL trực tiếp
 * - Mỗi request mở/đóng EntityManager riêng
 * - Transaction được quản lý tại Service, không ở DAO
 */
public class chat_roomService {

    private final EntityManagerFactory emf;

    public chat_roomService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // ========================= TẠO PHÒNG CHAT =========================

    /**
     * Tạo hoặc lấy phòng chat 1-1 giữa 2 user
     * Kiểm tra xem đã có phòng chat giữa 2 user chưa
     */
    public chat_room createOrGetPrivateRoom(int user1Id, int user2Id) {
        EntityManager em = emf.createEntityManager();
        chat_roomDAO dao = new chat_roomDAO(em);

        try {
            // Kiểm tra đã có phòng chat chưa
            chat_room existing = dao.findPrivateRoomBetweenUsers(user1Id, user2Id);
            if (existing != null) {
                return existing;
            }

            // Tạo phòng mới
            em.getTransaction().begin();
            chat_room newRoom = dao.createPrivateRoom(user1Id, user2Id);
            em.getTransaction().commit();
            return newRoom;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Tạo phòng chat dựa trên room game
     */
    public chat_room createRoomBasedChat(int roomId) {
        EntityManager em = emf.createEntityManager();
        chat_roomDAO dao = new chat_roomDAO(em);

        try {
            em.getTransaction().begin();
            chat_room newRoom = dao.createRoomBasedChat(roomId);
            em.getTransaction().commit();
            return newRoom;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========================= CRUD CƠ BẢN =========================

    /**
     * Lấy chat room theo ID
     */
    public chat_room getChatRoomById(int chatroomId) {
        EntityManager em = emf.createEntityManager();
        chat_roomDAO dao = new chat_roomDAO(em);
        try {
            return dao.getChatRoomById(chatroomId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy tất cả chat rooms
     */
    public List<chat_room> getAllChatRooms() {
        EntityManager em = emf.createEntityManager();
        chat_roomDAO dao = new chat_roomDAO(em);
        try {
            return dao.getAllChatRooms();
        } finally {
            em.close();
        }
    }

    /**
     * Xóa chat room
     */
    public boolean deleteChatRoom(int chatroomId) {
        EntityManager em = emf.createEntityManager();
        chat_roomDAO dao = new chat_roomDAO(em);
        try {
            em.getTransaction().begin();
            boolean result = dao.deleteChatRoom(chatroomId);
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
     * Lấy các chat rooms của user
     */
    public List<chat_room> getChatRoomsByUserId(int userId) {
        EntityManager em = emf.createEntityManager();
        chat_roomDAO dao = new chat_roomDAO(em);
        try {
            return dao.getChatRoomsByUserId(userId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy chat room theo ref_id (dùng cho room-based chat)
     */
    public chat_room getChatRoomByRefId(int refId) {
        EntityManager em = emf.createEntityManager();
        chat_roomDAO dao = new chat_roomDAO(em);
        try {
            return dao.getChatRoomByRefId(refId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy thông tin chi tiết của chat room
     */
    public ChatRoomDetails getChatRoomDetails(int chatroomId) {
        EntityManager em = emf.createEntityManager();
        chat_roomDAO dao = new chat_roomDAO(em);
        try {
            return dao.getChatRoomDetails(chatroomId);
        } finally {
            em.close();
        }
    }

    // ========================= KIỂM TRA =========================

    /**
     * Kiểm tra user có trong chat room không
     */
    public boolean isUserInChatRoom(int chatroomId, int userId) {
        EntityManager em = emf.createEntityManager();
        chat_roomDAO dao = new chat_roomDAO(em);
        try {
            return dao.isUserInChatRoom(chatroomId, userId);
        } finally {
            em.close();
        }
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Lấy thống kê tổng quan
     */
    public ChatRoomStatistics getChatRoomStatistics() {
        EntityManager em = emf.createEntityManager();
        chat_roomDAO dao = new chat_roomDAO(em);
        try {
            return dao.getChatRoomStatistics();
        } finally {
            em.close();
        }
    }
}