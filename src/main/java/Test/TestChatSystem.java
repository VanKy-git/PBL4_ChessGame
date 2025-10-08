//package Test;
//
//import Model.DAO.chat_paticipantDAO;
//import Model.DAO.chat_roomDAO;
//import Model.DAO.chat_messageDAO;
//import Model.Entity.chat_message;
//import Model.Entity.DBConnection;
//
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.List;
//
///**
// * Mô phỏng việc user1 tạo phòng chat riêng với user2,
// * gửi tin nhắn và kiểm tra lịch sử chat.
// */
//public class TestChatSystem {
//    public static void main(String[] args) {
//        Connection conn = null;
//
//        try {
//            // Kết nối database
//            conn = DBConnection.getConnection();
//            System.out.println("✅ Kết nối CSDL thành công!");
//
//            // Khởi tạo DAO
//            chat_roomDAO roomDAO = new chat_roomDAO(conn);
//            chat_paticipantDAO participantDAO = new chat_paticipantDAO(conn);
//            chat_messageDAO messageDAO = new chat_messageDAO(conn);
//
//            int user1Id = 1;
//            int user2Id = 2;
//
//            // Kiểm tra xem giữa 2 người đã có phòng PRIVATE chưa
//            int chatroomId = roomDAO.findPrivateRoomBetweenUsers(user1Id, user2Id);
//            if (chatroomId == -1) {
//                // Nếu chưa có, tạo mới
//                chatroomId = roomDAO.insertChatRoom("private", null);
//                System.out.println(" Tạo phòng chat PRIVATE mới, ID = " + chatroomId);
//
//                // Thêm hai người vào phòng
//                participantDAO.insertParticipant(chatroomId, user1Id);
//                participantDAO.insertParticipant(chatroomId, user2Id);
//                System.out.println("👥 Đã thêm user1 và user2 vào phòng.");
//            } else {
//                System.out.println("⚡ Phòng PRIVATE giữa user1 và user2 đã tồn tại, ID = " + chatroomId);
//            }
//
//            // 4️⃣ Gửi tin nhắn từ user1
//            messageDAO.insertMessage(chatroomId, user1Id, "Chào bạn, sẵn sàng chơi chưa?");
//            messageDAO.insertMessage(chatroomId, user2Id, "Chào! Tôi sẵn sàng rồi.");
//
//            System.out.println("\n💬 Đã gửi 2 tin nhắn.");
//
//            // 5️⃣ Lấy danh sách tin nhắn trong phòng chat
//            List<chat_message> messages = messageDAO.getMessagesByRoom(chatroomId);
//            System.out.println("\n📜 Lịch sử tin nhắn trong phòng #" + chatroomId + ":");
//            for (chat_message msg : messages) {
//                System.out.printf("[%s] User %d: %s%n",
//                        msg.getSend_at(), msg.getUser_id(), msg.getMessage());
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//            System.out.println("❌ Lỗi khi thao tác với CSDL.");
//        } finally {
//            // 7️⃣ Đóng kết nối
//            try {
//                if (conn != null) conn.close();
//                System.out.println("\n🔒 Đã đóng kết nối CSDL.");
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}
