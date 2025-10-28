package com.chatapp.server.Model.Service;

import com.chatapp.server.Model.DAO.friendsDAO;
import com.chatapp.server.Model.Entity.friends;
import com.chatapp.server.Model.Entity.user;

import java.time.LocalDateTime;
import java.util.List;

/**
 * friendsService - Xử lý nghiệp vụ bạn bè (friendship).
 *
 * 💡 Tầng này KHÔNG làm việc trực tiếp với EntityManager.
 * 💡 Không commit / rollback transaction.
 * 💡 Chỉ chứa logic xử lý nghiệp vụ (kiểm tra, xác thực, trạng thái...).
 */
public class friendsService {

    public final friendsDAO dao;

    public friendsService(friendsDAO dao) {
        this.dao = dao;
    }

    /**
     * Gửi lời mời kết bạn
     * @param sender người gửi
     * @param receiver người nhận
     */
    public boolean sendFriendRequest(user sender, user receiver) {
        // Không thể tự gửi lời mời cho chính mình
        if (sender.getUserId() == receiver.getUserId()) {
            throw new IllegalArgumentException("Không thể tự kết bạn với chính mình!");
        }

        // Kiểm tra đã có quan hệ bạn bè hoặc yêu cầu chờ trước đó chưa
        List<friends> existing = dao.getFriendsOfUser(sender.getUserId());
        boolean alreadyExists = existing.stream().anyMatch(f ->
                (f.getUser1().equals(sender) && f.getUser2().equals(receiver)) ||
                        (f.getUser1().equals(receiver) && f.getUser2().equals(sender))
        );
        if (alreadyExists) {
            throw new RuntimeException("Yêu cầu kết bạn đã tồn tại hoặc hai người đã là bạn bè!");
        }

        // Tạo lời mời mới
        friends f = new friends();
        f.setUser1(sender);
        f.setUser2(receiver);
        f.setStatus("PENDING");
        f.setCreatedAt(LocalDateTime.now());

        dao.addFriendRequest(sender.getUserId(), receiver.getUserId());
        return true;
    }

    /**
     * Lấy danh sách bạn bè hoặc yêu cầu của một người
     */
    public List<friends> getFriendsOfUser(int userId) {
        return dao.getFriendsOfUser(userId);
    }

    /**
     * Chấp nhận lời mời kết bạn
     */
    public boolean acceptFriendRequest(int friendshipId) {
        friends f = dao.getFriendById(friendshipId);
        if (f == null || !"PENDING".equals(f.getStatus())) return false;

        f.setStatus("ACCEPTED");
        dao.updateFriend(f);
        return true;
    }

    /**
     * Từ chối lời mời kết bạn
     */
    public boolean rejectFriendRequest(int friendshipId) {
        friends f = dao.getFriendById(friendshipId);
        if (f == null || !"PENDING".equals(f.getStatus())) return false;

        f.setStatus("REJECTED");
        dao.updateFriend(f);
        return true;
    }

    /**
     * Xóa bạn bè
     */
    public boolean deleteFriendship(int friendshipId) {
        friends f = dao.getFriendById(friendshipId);
        if (f == null) return false;

        dao.deleteFriendship(friendshipId);
        return true;
    }
}
