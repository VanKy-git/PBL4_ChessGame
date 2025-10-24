package com.chatapp.server.Test;

import com.chatapp.server.Controller.roomController;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * File test cho roomController – kiểm tra toàn bộ flow CRUD + business logic
 * Giữ đúng phong cách log từng bước (STEP X)
 */
public class RoomTest {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("PBL4_ChessPU");
        roomController controller = new roomController(emf);

        try {
            // ================== STEP 1: TẠO PHÒNG MỚI ==================
            System.out.println("═══════════════════════════════════════");
            System.out.println("STEP 1: CREATE ROOM");
            System.out.println("═══════════════════════════════════════");
            String createRoomJson = "{ \"hostId\": 1 }";
            String createRoomResponse = controller.handleRequest("createRoom", createRoomJson);
            System.out.println("Response: " + createRoomResponse);

            // ================== STEP 2: LẤY DANH SÁCH PHÒNG ==================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("STEP 2: GET ALL ROOMS");
            System.out.println("═══════════════════════════════════════");
            String allRoomsResponse = controller.handleRequest("getAllRooms", "{}");
            System.out.println("Response: " + allRoomsResponse);

            // ================== STEP 3: THAM GIA PHÒNG ==================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("STEP 3: JOIN ROOM");
            System.out.println("═══════════════════════════════════════");
            String joinRoomJson = "{ \"roomId\": 1, \"guestId\": 2 }";
            String joinRoomResponse = controller.handleRequest("joinRoom", joinRoomJson);
            System.out.println("Response: " + joinRoomResponse);

            // ================== STEP 4: CẬP NHẬT TRẠNG THÁI PHÒNG ==================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("STEP 4: UPDATE ROOM STATUS");
            System.out.println("═══════════════════════════════════════");
            String updateStatusJson = "{ \"roomId\": 1, \"status\": \"Active\" }";
            String updateStatusResponse = controller.handleRequest("updateRoomStatus", updateStatusJson);
            System.out.println("Response: " + updateStatusResponse);

            // ================== STEP 5: LẤY PHÒNG THEO ID ==================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("STEP 5: GET ROOM BY ID");
            System.out.println("═══════════════════════════════════════");
            String getRoomJson = "{ \"roomId\": 1 }";
            String getRoomResponse = controller.handleRequest("getRoomById", getRoomJson);
            System.out.println("Response: " + getRoomResponse);

            // ================== STEP 6: RỜI KHỎI PHÒNG ==================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("STEP 6: LEAVE ROOM");
            System.out.println("═══════════════════════════════════════");
            String leaveRoomJson = "{ \"roomId\": 1, \"userId\": 2 }";
            String leaveRoomResponse = controller.handleRequest("leaveRoom", leaveRoomJson);
            System.out.println("Response: " + leaveRoomResponse);

            // ================== STEP 7: ĐÓNG PHÒNG ==================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("STEP 7: CLOSE ROOM");
            System.out.println("═══════════════════════════════════════");
            String closeRoomJson = "{ \"roomId\": 1 }";
            String closeRoomResponse = controller.handleRequest("closeRoom", closeRoomJson);
            System.out.println("Response: " + closeRoomResponse);

            // ================== STEP 8: MỞ LẠI PHÒNG ==================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("STEP 8: REOPEN ROOM");
            System.out.println("═══════════════════════════════════════");
            String reopenRoomJson = "{ \"roomId\": 1 }";
            String reopenRoomResponse = controller.handleRequest("reopenRoom", reopenRoomJson);
            System.out.println("Response: " + reopenRoomResponse);

            // ================== STEP 9: LẤY DANH SÁCH PHÒNG ACTIVE ==================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("STEP 9: GET ACTIVE ROOMS");
            System.out.println("═══════════════════════════════════════");
            String activeRoomsResponse = controller.handleRequest("getActiveRooms", "{}");
            System.out.println("Response: " + activeRoomsResponse);

            // ================== STEP 10: LẤY THỐNG KÊ PHÒNG ==================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("STEP 10: GET ROOM STATISTICS");
            System.out.println("═══════════════════════════════════════");
            String statsResponse = controller.handleRequest("getRoomStatistics", "{}");
            System.out.println("Response: " + statsResponse);

            // ================== STEP 11: XÓA PHÒNG ==================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("STEP 11: DELETE ROOM");
            System.out.println("═══════════════════════════════════════");
            String deleteRoomJson = "{ \"roomId\": 1 }";
            String deleteRoomResponse = controller.handleRequest("deleteRoom", deleteRoomJson);
            System.out.println("Response: " + deleteRoomResponse);

            System.out.println("\n✅ TEST HOÀN TẤT KHÔNG LỖI!");
        } catch (Exception e) {
            System.err.println("❌ Lỗi trong quá trình test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            emf.close();
        }
    }
}
