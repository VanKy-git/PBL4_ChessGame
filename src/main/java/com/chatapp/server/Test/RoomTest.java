package com.chatapp.server.Test;

import com.chatapp.server.Controller.roomController;
import com.chatapp.server.Model.DAO.roomDAO.RoomWithPlayer;
import com.chatapp.server.Model.DAO.roomDAO.RoomStatistics;
import com.chatapp.server.Model.Entity.DBConnection;
import com.chatapp.server.Model.Entity.room;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;

public class RoomTest {

    public static void main(String[] args) {
        // Kiểm tra kết nối DB
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("✅ Kết nối database thành công: " + conn.getMetaData().getURL());
        } catch (SQLException e) {
            System.err.println("❌ Kết nối database thất bại: " + e.getMessage());
            return;
        }

        // Tạo controller
        roomController controller = new roomController();
        Gson gson = new Gson();

        try {
            // ====== TEST 1: Tạo phòng mới ======
//            System.out.println("\n🧩 Test 1: Tạo phòng mới");
//            JsonObject createRequest = new JsonObject();
//            createRequest.addProperty("hostId", 97); // ID user host
//            String createResponseJson = controller.handleRequest("createRoom", createRequest.toString());
//            JsonObject createResponse = gson.fromJson(createResponseJson, JsonObject.class);
//
 //           RoomWithPlayer room = null;
//            if (createResponse.get("success").getAsBoolean()) {
//                room = gson.fromJson(createResponse.get("data"), RoomWithPlayer.class);
//                System.out.println("✅ Tạo phòng thành công: Room#" + room.roomId);
//            } else {
//                System.out.println("❌ Tạo phòng thất bại: " + createResponse.get("error").getAsString());
//            }

            //if (room != null) {
                // ====== TEST 2: Tham gia phòng ======
                System.out.println("\n🧩 Test 2: Tham gia phòng");
                JsonObject joinRequest = new JsonObject();
                joinRequest.addProperty("roomId", 13);
                joinRequest.addProperty("guestId", 2); // ID user guest
                String joinResponseJson = controller.handleRequest("joinRoom", joinRequest.toString());
                JsonObject joinResponse = gson.fromJson(joinResponseJson, JsonObject.class);
                System.out.println(joinResponse.get("success").getAsBoolean() ?
                        "✅ User tham gia phòng thành công" :
                        "❌ Tham gia phòng thất bại: " + joinResponse.get("error").getAsString());

                // ====== TEST 3: Lấy phòng theo user ======
                System.out.println("\n🧩 Test 3: Lấy phòng theo userId");
                JsonObject getUserRoomsReq = new JsonObject();
                getUserRoomsReq.addProperty("userId", 1);
                String roomsByUserJson = controller.handleRequest("getRoomsByUserId", getUserRoomsReq.toString());
                JsonObject roomsByUserResp = gson.fromJson(roomsByUserJson, JsonObject.class);
                if (roomsByUserResp.get("success").getAsBoolean()) {
                    RoomWithPlayer[] rooms = gson.fromJson(roomsByUserResp.get("data"), RoomWithPlayer[].class);
                    System.out.println("User 1 đang ở các phòng: " + Arrays.toString(Arrays.stream(rooms)
                            .map(r -> r.roomId + "(" + r.roomStatus + ")").toArray()));
                }

                // ====== TEST 4: Quick Match ======
                System.out.println("\n🧩 Test 4: Quick Match cho user 3");
                JsonObject quickMatchReq = new JsonObject();
                quickMatchReq.addProperty("userId", 3);
                String quickMatchRespJson = controller.handleRequest("quickMatch", quickMatchReq.toString());
                JsonObject quickMatchResp = gson.fromJson(quickMatchRespJson, JsonObject.class);
                System.out.println(quickMatchResp.get("success").getAsBoolean() ?
                        "✅ Quick match thành công" :
                        "❌ Quick match thất bại: " + quickMatchResp.get("error").getAsString());

                // ====== TEST 5: Thống kê ======
                System.out.println("\n🧩 Test 5: Thống kê phòng");
                String statsJson = controller.handleRequest("getRoomStatistics", "{}");
                JsonObject statsResp = gson.fromJson(statsJson, JsonObject.class);
                if (statsResp.get("success").getAsBoolean()) {
                    RoomStatistics stats = gson.fromJson(statsResp.get("data"), RoomStatistics.class);
                    System.out.printf("Tổng số phòng: %d | Waiting: %d | Active: %d | Closed: %d\n",
                            stats.totalRooms, stats.waitingRooms, stats.activeRooms, stats.closedRooms);
                }

                // ====== TEST 6: Đóng phòng ======
                System.out.println("\n🧩 Test 6: Đóng phòng Room#13");
                JsonObject closeReq = new JsonObject();
                closeReq.addProperty("roomId", 13);
                String closeRespJson = controller.handleRequest("closeRoom", closeReq.toString());
                JsonObject closeResp = gson.fromJson(closeRespJson, JsonObject.class);
                System.out.println(closeResp.get("success").getAsBoolean() ?
                        "✅ Đóng phòng thành công" :
                        "❌ Đóng phòng thất bại: " + closeResp.get("error").getAsString());
            //}

        } catch (Exception e) {
            System.err.println("❌ Lỗi trong quá trình test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close();
            System.out.println("\n🧹 Đã đóng pool kết nối.");
        }
    }
}
