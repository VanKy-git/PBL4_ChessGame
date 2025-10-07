package Test;

import Model.DAO.roomDAO;
import Model.DAO.roomDAO.roomWithPlayer;
import Model.Entity.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class RoomTest {
    public static void main(String[] args) {
        // 1. Kết nối database
        Connection conn = null;
        try {
            conn = DBConnection.getConnection(); // giả sử bạn có method getConnection()
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Không kết nối được database!");
            return;
        }

        // 2. Tạo DAO
        roomDAO dao = new roomDAO(conn);

        // 3. Chọn room_id để test
        int testRoomId = 1;

        // 4. Gọi getRoom
        roomWithPlayer room = dao.getRoom(testRoomId);

        // 5. Hiển thị kết quả
        if (room != null) {
            System.out.println("=== Thông tin phòng ===");
            System.out.println("Room ID: " + room.room_id);
            System.out.println("Room Status: " + room.room_status);
            System.out.println("Created At: " + room.create_at);

            System.out.println("--- Host ---");
            System.out.println("Host ID: " + room.host_id);
            System.out.println("Host Name: " + room.host_name);
            System.out.println("Host ELO: " + room.host_elo_rating);

            System.out.println("--- Guest ---");
            System.out.println("Guest ID: " + room.guest_id);
            System.out.println("Guest Name: " + room.guest_name);
            System.out.println("Guest ELO: " + room.guest_elo_rating);

        } else {
            System.out.println("Không tìm thấy room với ID = " + testRoomId);
        }

        // 6. Đóng kết nối
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
