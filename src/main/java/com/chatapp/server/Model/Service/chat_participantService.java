package com.chatapp.server.Model.Service;

import com.chatapp.server.Model.DAO.chat_participantDAO;
import com.chatapp.server.Model.DAO.chat_participantDAO.ParticipantWithUser;
import com.chatapp.server.Model.Entity.chat_participant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

/**
 * chat_participantService - Xử lý nghiệp vụ cho chat_participant
 * - Không thao tác SQL trực tiếp
 * - Mỗi request mở/đóng EntityManager riêng
 * - Transaction được quản lý tại Service
 */
public class chat_participantService {

    private final EntityManagerFactory emf;

    public chat_participantService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // ========================= THÊM / XÓA PARTICIPANT =========================

    /**
     * Thêm user vào chat room
     */
    public chat_participant addParticipant(int chatroomId, int userId) {
        EntityManager em = emf.createEntityManager();
        chat_participantDAO dao = new chat_participantDAO(em);

        try {
            em.getTransaction().begin();
            chat_participant participant = dao.addParticipant(chatroomId, userId);
            em.getTransaction().commit();
            return participant;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Xóa user khỏi chat room
     */
    public boolean removeParticipant(int chatroomId, int userId) {
        EntityManager em = emf.createEntityManager();
        chat_participantDAO dao = new chat_participantDAO(em);

        try {
            em.getTransaction().begin();
            boolean result = dao.removeParticipant(chatroomId, userId);
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
     * Lấy participant theo composite key
     */
    public chat_participant getParticipant(int chatroomId, int userId) {
        EntityManager em = emf.createEntityManager();
        chat_participantDAO dao = new chat_participantDAO(em);
        try {
            return dao.getParticipant(chatroomId, userId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy tất cả participants trong một chat room
     */
    public List<chat_participant> getParticipantsByRoom(int chatroomId) {
        EntityManager em = emf.createEntityManager();
        chat_participantDAO dao = new chat_participantDAO(em);
        try {
            return dao.getParticipantsByRoom(chatroomId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy tất cả chat rooms mà user tham gia
     */
    public List<chat_participant> getParticipantsByUser(int userId) {
        EntityManager em = emf.createEntityManager();
        chat_participantDAO dao = new chat_participantDAO(em);
        try {
            return dao.getParticipantsByUser(userId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy participants với thông tin user đầy đủ
     */
    public List<ParticipantWithUser> getParticipantsWithUserInfo(int chatroomId) {
        EntityManager em = emf.createEntityManager();
        chat_participantDAO dao = new chat_participantDAO(em);
        try {
            return dao.getParticipantsWithUserInfo(chatroomId);
        } finally {
            em.close();
        }
    }

    // ========================= KIỂM TRA =========================

    /**
     * Kiểm tra user có phải là participant không
     */
    public boolean isParticipant(int chatroomId, int userId) {
        EntityManager em = emf.createEntityManager();
        chat_participantDAO dao = new chat_participantDAO(em);
        try {
            return dao.isParticipant(chatroomId, userId);
        } finally {
            em.close();
        }
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Đếm số participants trong chat room
     */
    public int countParticipantsByRoom(int chatroomId) {
        EntityManager em = emf.createEntityManager();
        chat_participantDAO dao = new chat_participantDAO(em);
        try {
            return dao.countParticipantsByRoom(chatroomId);
        } finally {
            em.close();
        }
    }

    /**
     * Đếm số chat rooms mà user tham gia
     */
    public int countRoomsByUser(int userId) {
        EntityManager em = emf.createEntityManager();
        chat_participantDAO dao = new chat_participantDAO(em);
        try {
            return dao.countRoomsByUser(userId);
        } finally {
            em.close();
        }
    }
}