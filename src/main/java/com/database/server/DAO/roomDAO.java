package com.database.server.DAO;

import com.database.server.Entity.room;
import com.database.server.Entity.user;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class roomDAO {

    private final EntityManager em;

    public roomDAO(EntityManager em) {
        this.em = em;
    }

    // ========================= INNER CLASS: ROOM WITH PLAYER INFO =========================

    public static class RoomWithPlayer {
        public int roomId;
        public int hostId;
        public String hostName;
        public Integer hostEloRating;
        public Integer guestId;
        public String guestName;
        public Integer guestEloRating;
        public String roomStatus;
        public LocalDateTime createAt;

        public RoomWithPlayer(int roomId, int hostId, String hostName,
                              Integer guestId, Integer hostEloRating,
                              String guestName, Integer guestEloRating,
                              String roomStatus, LocalDateTime createAt) {
            this.roomId = roomId;
            this.hostId = hostId;
            this.hostName = hostName;
            this.hostEloRating = hostEloRating;
            this.guestId = guestId;
            this.guestName = guestName;
            this.guestEloRating = guestEloRating;
            this.roomStatus = roomStatus;
            this.createAt = createAt;
        }
    }

    // ========================= HELPER METHODS =========================

    private RoomWithPlayer mapToRoomWithPlayer(room r) {
        if (r == null) return null;

        user host = em.find(user.class, r.getHost_id());
        // SỬA: Kiểm tra null trước khi so sánh
        user guest = (r.getGuest_id() != null && r.getGuest_id() > 0)
                ? em.find(user.class, r.getGuest_id())
                : null;

        return new RoomWithPlayer(
                r.getRoom_id(),
                r.getHost_id(),
                host != null ? host.getUserName() : null,
                host != null ? host.getEloRating() : null,
                r.getGuest_id(),
                guest != null ? guest.getUserName() : null,
                guest != null ? guest.getEloRating() : null,
                r.getRoom_status(),
                r.getCreate_at()
        );
    }

    // ========================= CRUD CƠ BẢN =========================

    public RoomWithPlayer getRoomById(int roomId) {
        room r = em.find(room.class, roomId);
        return mapToRoomWithPlayer(r);
    }

    public room getRoomEntity(int roomId) {
        return em.find(room.class, roomId);
    }

    public List<RoomWithPlayer> getAllRooms() {
        List<room> rooms = em.createQuery(
                "SELECT r FROM room r ORDER BY r.create_at DESC", room.class
        ).getResultList();

        List<RoomWithPlayer> result = new ArrayList<>();
        for (room r : rooms) {
            result.add(mapToRoomWithPlayer(r));
        }
        return result;
    }

    public boolean updateRoom(room r) {
        em.merge(r);
        return true;
    }

    public boolean deleteRoom(int roomId) {
        room r = em.find(room.class, roomId);
        if (r != null) {
            em.remove(r);
            return true;
        }
        return false;
    }

    // ========================= TẠO VÀ THAM GIA PHÒNG =========================

    public RoomWithPlayer createRoom(int hostId) {
        user host = em.find(user.class, hostId);
        if (host == null) return null;

        room newRoom = new room();
        newRoom.setHost_id(hostId);
        newRoom.setGuest_id(null); // guest_id có thể null
        newRoom.setRoom_status("Waiting");
        newRoom.setCreate_at(LocalDateTime.now());

        em.persist(newRoom);
        return mapToRoomWithPlayer(newRoom);
    }

    public boolean joinRoom(int roomId, int guestId) {
        room r = em.find(room.class, roomId);
        // SỬA: Kiểm tra null trước khi so sánh
        if (r == null || (r.getGuest_id() != null && r.getGuest_id() > 0))
            return false;

        user guest = em.find(user.class, guestId);
        if (guest == null) return false;

        r.setGuest_id(guestId);
        r.setRoom_status("Active");
        em.merge(r);
        return true;
    }

    public boolean leaveRoom(int roomId, int userId) {
        room r = em.find(room.class, roomId);
        if (r == null) return false;

        // SỬA: Kiểm tra null trước khi so sánh
        if (r.getGuest_id() != null && r.getGuest_id() == userId) {
            r.setGuest_id(null); // SỬA: Set về null thay vì 0
            r.setRoom_status("Waiting"); // SỬA: "Waiting" với chữ W hoa
            em.merge(r);
            return true;
        }

        // Nếu là host thì xóa phòng
        if (r.getHost_id() == userId) {
            em.remove(r);
            return true;
        }

        return false;
    }

    // ========================= CẬP NHẬT TRẠNG THÁI =========================

    public boolean updateRoomStatus(int roomId, String status) {
        room r = em.find(room.class, roomId);
        if (r != null) {
            r.setRoom_status(status);
            em.merge(r);
            return true;
        }
        return false;
    }

    // ========================= TRUY VẤN THEO ĐIỀU KIỆN =========================

    public List<RoomWithPlayer> getRoomsByUserId(int userId) {
        try {
            List<room> rooms = em.createQuery(
                            "SELECT r FROM room r WHERE r.host_id = :userId OR r.guest_id = :userId",
                            room.class
                    ).setParameter("userId", userId)
                    .getResultList();

            List<RoomWithPlayer> result = new ArrayList<>();
            for (room r : rooms) {
                result.add(mapToRoomWithPlayer(r));
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<RoomWithPlayer> getRoomsByStatus(String status) {
        try {
            List<room> rooms = em.createQuery(
                            "SELECT r FROM room r WHERE r.room_status = :status ORDER BY r.create_at DESC",
                            room.class
                    ).setParameter("status", status)
                    .getResultList();

            List<RoomWithPlayer> result = new ArrayList<>();
            for (room r : rooms) {
                result.add(mapToRoomWithPlayer(r));
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<RoomWithPlayer> getRoomsCreatedAfter(LocalDateTime afterTime) {
        try {
            List<room> rooms = em.createQuery(
                            "SELECT r FROM room r WHERE r.create_at > :time ORDER BY r.create_at DESC",
                            room.class
                    ).setParameter("time", afterTime)
                    .getResultList();

            List<RoomWithPlayer> result = new ArrayList<>();
            for (room r : rooms) {
                result.add(mapToRoomWithPlayer(r));
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<RoomWithPlayer> getAvailableRooms() {
        return getRoomsByStatus("Waiting");
    }

    // ========================= THỐNG KÊ =========================

    public int getTotalRoomCount() {
        return em.createQuery("SELECT COUNT(r) FROM room r", Long.class)
                .getSingleResult().intValue();
    }

    public int getRoomCountByStatus(String status) {
        return em.createQuery(
                        "SELECT COUNT(r) FROM room r WHERE r.room_status = :status", Long.class
                ).setParameter("status", status)
                .getSingleResult().intValue();
    }

    public RoomStatistics getRoomStatistics() {
        int total = getTotalRoomCount();
        int waiting = getRoomCountByStatus("Waiting");
        int active = getRoomCountByStatus("Active");
        int closed = getRoomCountByStatus("Closed");

        return new RoomStatistics(total, waiting, active, closed);
    }

    public static class RoomStatistics {
        public int totalRooms;
        public int waitingRooms;
        public int activeRooms;
        public int closedRooms;

        public RoomStatistics(int totalRooms, int waitingRooms, int activeRooms, int closedRooms) {
            this.totalRooms = totalRooms;
            this.waitingRooms = waitingRooms;
            this.activeRooms = activeRooms;
            this.closedRooms = closedRooms;
        }
    }
}