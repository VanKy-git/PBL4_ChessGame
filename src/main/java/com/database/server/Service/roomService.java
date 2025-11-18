package com.database.server.Service;

import com.database.server.DAO.roomDAO;
import com.database.server.DAO.roomDAO.RoomWithPlayer;
import com.database.server.DAO.roomDAO.RoomStatistics;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RoomService - Xử lý nghiệp vụ cho đối tượng Room (Phòng chơi)
 * - Không thao tác SQL trực tiếp
 * - Mỗi request mở/đóng EntityManager riêng
 * - Transaction được quản lý tại Service, không ở DAO
 */
public class roomService {

    private final EntityManagerFactory emf;

    public roomService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // ========================= CRUD CƠ BẢN =========================

    /**
     * Lấy thông tin phòng theo ID
     * @param roomId ID của phòng
     * @return Thông tin phòng kèm thông tin người chơi, null nếu không tìm thấy
     */
    public RoomWithPlayer getRoomById(int roomId) {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            return dao.getRoomById(roomId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy danh sách tất cả các phòng
     * @return Danh sách các phòng, sắp xếp theo thời gian tạo (mới nhất trước)
     */
    public List<RoomWithPlayer> getAllRooms() {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            return dao.getAllRooms();
        } finally {
            em.close();
        }
    }

    /**
     * Xóa một phòng
     * @param roomId ID của phòng cần xóa
     * @return true nếu xóa thành công, false nếu thất bại
     */
    public boolean deleteRoom(int roomId) {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            em.getTransaction().begin();
            boolean result = dao.deleteRoom(roomId);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========================= TẠO VÀ THAM GIA PHÒNG =========================

    /**
     * Tạo phòng mới
     * @param hostId ID của người tạo phòng (host)
     * @return Thông tin phòng vừa tạo, null nếu thất bại
     */
    public RoomWithPlayer createRoom(int hostId) {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            em.getTransaction().begin();
            RoomWithPlayer room = dao.createRoom(hostId);
            em.getTransaction().commit();
            return room;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Tham gia vào một phòng đang chờ
     * @param roomId ID của phòng cần tham gia
     * @param guestId ID của người tham gia (guest)
     * @return true nếu tham gia thành công, false nếu phòng đã đầy hoặc không tồn tại
     */
    public boolean joinRoom(int roomId, int guestId) {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            em.getTransaction().begin();
            boolean result = dao.joinRoom(roomId, guestId);
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
     * Rời khỏi phòng
     * - Nếu là guest: xóa guest, chuyển phòng về trạng thái "Waiting"
     * - Nếu là host: xóa toàn bộ phòng
     * @param roomId ID của phòng
     * @param userId ID của người rời phòng
     * @return true nếu rời phòng thành công, false nếu thất bại
     */
    public boolean leaveRoom(int roomId, int userId) {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            em.getTransaction().begin();
            boolean result = dao.leaveRoom(roomId, userId);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========================= CẬP NHẬT TRẠNG THÁI =========================

    /**
     * Cập nhật trạng thái phòng
     * @param roomId ID của phòng
     * @param status Trạng thái mới ("Waiting", "Active", "Closed")
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    public boolean updateRoomStatus(int roomId, String status) {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            em.getTransaction().begin();
            boolean result = dao.updateRoomStatus(roomId, status);
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
     * Đóng phòng (chuyển sang trạng thái "Closed")
     * @param roomId ID của phòng cần đóng
     * @return true nếu đóng thành công, false nếu thất bại
     */
    public boolean closeRoom(int roomId) {
        return updateRoomStatus(roomId, "Closed");
    }

    /**
     * Mở lại phòng (chuyển sang trạng thái "Waiting")
     * @param roomId ID của phòng cần mở lại
     * @return true nếu mở lại thành công, false nếu thất bại
     */
    public boolean reopenRoom(int roomId) {
        return updateRoomStatus(roomId, "Waiting");
    }

    // ========================= TRUY VẤN THEO ĐIỀU KIỆN =========================

    /**
     * Lấy tất cả các phòng mà user đã tham gia (là host hoặc guest)
     * @param userId ID của user
     * @return Danh sách các phòng của user
     */
    public List<RoomWithPlayer> getRoomsByUserId(int userId) {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            return dao.getRoomsByUserId(userId);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy các phòng theo trạng thái
     * @param status Trạng thái cần lọc ("Waiting", "Active", "Closed")
     * @return Danh sách các phòng có trạng thái tương ứng
     */
    public List<RoomWithPlayer> getRoomsByStatus(String status) {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            return dao.getRoomsByStatus(status);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy danh sách các phòng đang chờ người chơi (status = "Waiting")
     * @return Danh sách các phòng có thể tham gia
     */
    public List<RoomWithPlayer> getAvailableRooms() {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            return dao.getAvailableRooms();
        } finally {
            em.close();
        }
    }

    /**
     * Lấy danh sách các phòng đang chơi (status = "Active")
     * @return Danh sách các phòng đang active
     */
    public List<RoomWithPlayer> getActiveRooms() {
        return getRoomsByStatus("Active");
    }

    /**
     * Lấy các phòng được tạo sau một thời điểm nhất định
     * @param afterTime Thời điểm mốc
     * @return Danh sách các phòng được tạo sau thời điểm này
     */
    public List<RoomWithPlayer> getRoomsCreatedAfter(LocalDateTime afterTime) {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            return dao.getRoomsCreatedAfter(afterTime);
        } finally {
            em.close();
        }
    }

    // ========================= BUSINESS LOGIC =========================

    /**
     * Tạo phòng mới và tự động thiết lập các thông tin cơ bản
     * @param hostId ID của người tạo phòng
     * @return Thông tin phòng vừa tạo, null nếu thất bại
     */
    public RoomWithPlayer createAndJoinRoom(int hostId) {
        RoomWithPlayer room = createRoom(hostId);
        if (room != null) {
            System.out.println("Phòng được tạo bởi user " + hostId + ": Room#" + room.roomId);
        }
        return room;
    }

    /**
     * Tìm trận nhanh (Quick Match)
     * - Nếu có phòng đang chờ: tự động join vào phòng đầu tiên
     * - Nếu không có phòng nào: tạo phòng mới và chờ đối thủ
     * @param userId ID của người chơi
     * @return true nếu tìm/tạo phòng thành công, false nếu thất bại
     */
    public boolean quickMatch(int userId) {
        // Tìm phòng đang chờ để join
        List<RoomWithPlayer> availableRooms = getAvailableRooms();

        if (availableRooms != null && !availableRooms.isEmpty()) {
            // Join vào phòng đầu tiên
            RoomWithPlayer room = availableRooms.get(0);
            return joinRoom(room.roomId, userId);
        } else {
            // Không có phòng nào, tạo phòng mới
            RoomWithPlayer newRoom = createRoom(userId);
            return newRoom != null;
        }
    }

    /**
     * Kiểm tra xem user có đang ở trong phòng nào đang hoạt động không
     * @param userId ID của user
     * @return true nếu user đang ở trong phòng Active hoặc Waiting
     */
    public boolean isUserInRoom(int userId) {
        List<RoomWithPlayer> userRooms = getRoomsByUserId(userId);
        if (userRooms == null || userRooms.isEmpty()) return false;

        // Kiểm tra có phòng Active hoặc Waiting nào không
        return userRooms.stream()
                .anyMatch(r -> "Active".equals(r.roomStatus) || "Waiting".equals(r.roomStatus));
    }

    /**
     * Lấy phòng hiện tại của user (phòng đang Active hoặc Waiting)
     * @param userId ID của user
     * @return Thông tin phòng hiện tại, null nếu user không ở trong phòng nào
     */
    public RoomWithPlayer getCurrentRoom(int userId) {
        List<RoomWithPlayer> userRooms = getRoomsByUserId(userId);
        if (userRooms == null) return null;

        // Trả về phòng Active hoặc Waiting đầu tiên
        return userRooms.stream()
                .filter(r -> "Active".equals(r.roomStatus) || "Waiting".equals(r.roomStatus))
                .findFirst()
                .orElse(null);
    }

    // ========================= THỐNG KÊ =========================

    /**
     * Lấy tổng số phòng trong hệ thống
     * @return Tổng số phòng
     */
    public int getTotalRoomCount() {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            return dao.getTotalRoomCount();
        } finally {
            em.close();
        }
    }

    /**
     * Lấy số lượng phòng theo trạng thái
     * @param status Trạng thái cần đếm
     * @return Số lượng phòng có trạng thái đó
     */
    public int getRoomCountByStatus(String status) {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            return dao.getRoomCountByStatus(status);
        } finally {
            em.close();
        }
    }

    /**
     * Lấy thống kê tổng quan về các phòng
     * @return Object chứa thống kê chi tiết (tổng số, số phòng chờ, đang chơi, đã đóng)
     */
    public RoomStatistics getRoomStatistics() {
        EntityManager em = emf.createEntityManager();
        roomDAO dao = new roomDAO(em);
        try {
            return dao.getRoomStatistics();
        } finally {
            em.close();
        }
    }
}