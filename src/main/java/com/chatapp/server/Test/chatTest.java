package com.chatapp.server.Test;

import com.chatapp.server.Controller.chat_messageController;
import com.chatapp.server.Controller.chat_participantController;
import com.chatapp.server.Controller.chat_roomController;
import com.chatapp.server.Controller.userController;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * ChatSystemIntegrationTest - Kiểm thử toàn bộ chức năng hệ thống chat.
 * Bao gồm:
 * - Đăng ký & đăng nhập người dùng
 * - Tạo phòng trò chuyện
 * - Thêm thành viên
 * - Gửi và nhận tin nhắn
 * - Thống kê, kiểm tra hoạt động
 */
public class chatTest {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");

        // Khởi tạo Controllers
        userController userCtrl = new userController(emf);
        chat_roomController roomCtrl = new chat_roomController(emf);
        chat_participantController participantCtrl = new chat_participantController(emf);
        chat_messageController messageCtrl = new chat_messageController(emf);

        System.out.println("========================================");
        System.out.println("     CHAT SYSTEM - INTEGRATION TEST");
        System.out.println("========================================\n");

        // ========== STEP 1: ĐĂNG KÝ NGƯỜI DÙNG ==========
        System.out.println("═══════════════════════════════════════");
        System.out.println("STEP 1: REGISTER USERS");
        System.out.println("═══════════════════════════════════════");

        String userARequest = "{ \"username\": \"nam20938\", \"password\": \"123456\" }";
        String userBRequest = "{ \"username\": \"minh20938\", \"password\": \"123456\" }";
        String userCRequest = "{ \"username\": \"hoang20938\", \"password\": \"123456\" }";

        // ---- Nam ----
        System.out.println("→ Registering user Nam...");
        String userAResponse = userCtrl.handleRequest("register", userARequest);
        System.out.println("  Response: " + userAResponse);
        int userAId = extractId(userAResponse, "user_id");

        // ---- Minh ----
        System.out.println("\n→ Registering user Minh...");
        String userBResponse = userCtrl.handleRequest("register", userBRequest);
        System.out.println("  Response: " + userBResponse);
        int userBId = extractId(userBResponse, "user_id");

        // ---- Hoang ----
        System.out.println("\n→ Registering user Hoang...");
        String userCResponse = userCtrl.handleRequest("register", userCRequest);
        System.out.println("  Response: " + userCResponse);
        int userCId = extractId(userCResponse, "user_id");

        // ========== STEP 2: ĐĂNG NHẬP ==========
        System.out.println("\n\n═══════════════════════════════════════");
        System.out.println("STEP 2: LOGIN USERS");
        System.out.println("═══════════════════════════════════════");

        System.out.println("→ Nam logging in...");
        System.out.println("  Response: " + userCtrl.handleRequest("login", userARequest));

        System.out.println("\n→ Minh logging in...");
        System.out.println("  Response: " + userCtrl.handleRequest("login", userBRequest));

        // ========== STEP 3: CẬP NHẬT TRẠNG THÁI ==========
        System.out.println("\n\n═══════════════════════════════════════");
        System.out.println("STEP 3: UPDATE ONLINE STATUS");
        System.out.println("═══════════════════════════════════════");

        String statusNam = "{ \"userId\": " + userAId + ", \"status\": \"Online\" }";
        String statusMinh = "{ \"userId\": " + userBId + ", \"status\": \"Online\" }";
        String statusHoang = "{ \"userId\": " + userCId + ", \"status\": \"Online\" }";

        userCtrl.handleRequest("updateStatus", statusNam);
        userCtrl.handleRequest("updateStatus", statusMinh);
        userCtrl.handleRequest("updateStatus", statusHoang);
        System.out.println("✓ All users are now online");

        // ========== STEP 4: TẠO PHÒNG CHAT ==========
        System.out.println("\n\n═══════════════════════════════════════");
        System.out.println("STEP 4: CREATE PRIVATE CHAT ROOM (Nam - Minh)");
        System.out.println("═══════════════════════════════════════");

        String createRoom = "{ \"user1Id\": " + userAId + ", \"user2Id\": " + userBId + " }";
        System.out.println("→ Creating private chat room...");
        String roomResponse = roomCtrl.handleRequest("createPrivateRoom", createRoom);
        System.out.println("  Response: " + roomResponse);

        int chatroomId = extractId(roomResponse, "chatroom_id");

        // ========== STEP 5: THÊM THÀNH VIÊN ==========
        System.out.println("\n\n═══════════════════════════════════════");
        System.out.println("STEP 5: ADD PARTICIPANTS");
        System.out.println("═══════════════════════════════════════");

        String addNam = "{ \"chatroomId\": " + chatroomId + ", \"userId\": " + userAId + " }";
        String addMinh = "{ \"chatroomId\": " + chatroomId + ", \"userId\": " + userBId + " }";

        System.out.println("→ Adding Nam...");
        System.out.println("  Response: " + participantCtrl.handleRequest("addParticipant", addNam));

        System.out.println("\n→ Adding Minh...");
        System.out.println("  Response: " + participantCtrl.handleRequest("addParticipant", addMinh));

        // ========== STEP 6: KIỂM TRA DANH SÁCH ==========
        System.out.println("\n\n═══════════════════════════════════════");
        System.out.println("STEP 6: GET PARTICIPANTS INFO");
        System.out.println("═══════════════════════════════════════");

        String getParticipants = "{ \"chatroomId\": " + chatroomId + " }";
        System.out.println("→ Getting participant list...");
        System.out.println("  Response: " + participantCtrl.handleRequest("getParticipantsByRoom", getParticipants));

        System.out.println("\n→ Getting detailed participant info...");
        System.out.println("  Response: " + participantCtrl.handleRequest("getParticipantsWithUserInfo", getParticipants));

        // ========== STEP 7: GỬI TIN NHẮN ==========
        System.out.println("\n\n═══════════════════════════════════════");
        System.out.println("STEP 7: SEND MESSAGES");
        System.out.println("═══════════════════════════════════════");

        String msg1 = "{ \"chatroomId\": " + chatroomId + ", \"userId\": " + userAId + ", \"message\": \"Hey Minh, rảnh đánh cờ không?\" }";
        String msg2 = "{ \"chatroomId\": " + chatroomId + ", \"userId\": " + userBId + ", \"message\": \"Có chứ Nam, vô phòng nào?\" }";

        System.out.println("→ Nam sends a message...");
        String msgResp1 = messageCtrl.handleRequest("sendMessage", msg1);
        System.out.println("  Response: " + msgResp1);
        int msgId1 = extractId(msgResp1, "message_id");

        System.out.println("\n→ Minh replies...");
        String msgResp2 = messageCtrl.handleRequest("sendMessage", msg2);
        System.out.println("  Response: " + msgResp2);
        int msgId2 = extractId(msgResp2, "message_id");

        // ========== STEP 8: LẤY TIN NHẮN ==========
        System.out.println("\n\n═══════════════════════════════════════");
        System.out.println("STEP 8: FETCH MESSAGES");
        System.out.println("═══════════════════════════════════════");

        String getMessages = "{ \"chatroomId\": " + chatroomId + " }";
        System.out.println("→ Fetching all messages...");
        System.out.println("  Response: " + messageCtrl.handleRequest("getMessagesByRoom", getMessages));

//        // ========== STEP 9: CẬP NHẬT TIN NHẮN ==========
//        System.out.println("\n\n═══════════════════════════════════════");
//        System.out.println("STEP 9: UPDATE MESSAGE");
//        System.out.println("═══════════════════════════════════════");
//
//        String updateMsg = "{ \"messageId\": " + msgId1 + ", \"newMessage\": \"Minh, tối nay đấu 5 ván nhé!\" }";
//        System.out.println("→ Updating the first message...");
//        System.out.println("  Response: " + messageCtrl.handleRequest("updateMessage", updateMsg));

        // ========== STEP 10: THỐNG KÊ ==========
        System.out.println("\n\n═══════════════════════════════════════");
        System.out.println("STEP 10: STATISTICS");
        System.out.println("═══════════════════════════════════════");

        String countMsg = "{ \"chatroomId\": " + chatroomId + " }";
        System.out.println("→ Counting messages...");
        System.out.println("  Response: " + messageCtrl.handleRequest("countMessagesByRoom", countMsg));

        System.out.println("\n→ Counting participants...");
        System.out.println("  Response: " + participantCtrl.handleRequest("countParticipantsByRoom", countMsg));

        // ========== STEP 11: RỜI PHÒNG ==========
        System.out.println("\n\n═══════════════════════════════════════");
        System.out.println("STEP 11: LEAVE ROOM");
        System.out.println("═══════════════════════════════════════");

        String removeMinh = "{ \"chatroomId\": " + chatroomId + ", \"userId\": " + userBId + " }";
        System.out.println("→ Minh leaves the room...");
        System.out.println("  Response: " + participantCtrl.handleRequest("removeParticipant", removeMinh));

        System.out.println("\n\n========================================");
        System.out.println("     ✓ INTEGRATION TEST COMPLETED");
        System.out.println("========================================");
    }

    /**
     * Hỗ trợ lấy ID từ JSON dù nằm trong "data" hoặc top-level
     */
    private static int extractId(String json, String idKey) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("data") && obj.get("data").isJsonObject()) {
                JsonObject data = obj.getAsJsonObject("data");
                if (data.has(idKey)) {
                    return data.get(idKey).getAsInt();
                }
            }
            if (obj.has(idKey)) {
                return obj.get(idKey).getAsInt();
            }
        } catch (Exception e) {
            System.err.println("⚠ Could not extract " + idKey + " from JSON: " + json);
        }
        return -1;
    }
}
