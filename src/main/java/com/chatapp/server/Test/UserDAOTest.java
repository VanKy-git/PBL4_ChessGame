package com.chatapp.server.Test;

import com.chatapp.server.Model.Entity.DBConnection;
import com.chatapp.server.Model.Entity.user;
import com.chatapp.server.Controller.userController;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserDAOTest {

    public static void main(String[] args) {
        // Kiểm tra kết nối DB
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("✅ Kết nối database thành công: " + conn.getMetaData().getURL());
        } catch (SQLException e) {
            System.err.println("❌ Kết nối database thất bại: " + e.getMessage());
            return;
        }

        // Tạo controller
        userController controller = new userController();
        Gson gson = new Gson();

        try {
            // ====== TEST 1: Đăng ký ======
            System.out.println("\n🧩 Test 1: Đăng ký user mới");
            JsonObject registerRequest = new JsonObject();
            registerRequest.addProperty("username", "nhuantest");
            registerRequest.addProperty("password", "12345");

            String registerResponseJson = controller.handleRequest("register", registerRequest.toString());
            JsonObject registerResponse = gson.fromJson(registerResponseJson, JsonObject.class);

            if (registerResponse.get("success").getAsBoolean()) {
                user newUser = gson.fromJson(registerResponse.get("data"), user.class);
                System.out.println("✅ Tạo user thành công: " + newUser.getUserName() + " (ID: " + newUser.getUserId() + ")");
            } else {
                System.out.println("❌ Đăng ký thất bại: " + registerResponse.get("error").getAsString());
            }

            // ====== TEST 2: Đăng nhập ======
            System.out.println("\n🧩 Test 2: Đăng nhập user");
            JsonObject loginRequest = new JsonObject();
            loginRequest.addProperty("username", "nhuantest");
            loginRequest.addProperty("password", "12345");

            String loginResponseJson = controller.handleRequest("login", loginRequest.toString());
            JsonObject loginResponse = gson.fromJson(loginResponseJson, JsonObject.class);

            user loginUser = null;
            if (loginResponse.get("success").getAsBoolean()) {
                loginUser = gson.fromJson(loginResponse.get("data"), user.class);
                System.out.println("✅ Đăng nhập thành công: " + loginUser.getUserName());
            } else {
                System.out.println("❌ Đăng nhập thất bại: " + loginResponse.get("error").getAsString());
            }

            if (loginUser != null) {
                // ====== TEST 3: Cập nhật trạng thái ======
                System.out.println("\n🧩 Test 3: Cập nhật trạng thái user");
                JsonObject statusRequest = new JsonObject();
                statusRequest.addProperty("userId", loginUser.getUserId());
                statusRequest.addProperty("status", "Online");

                String statusResponseJson = controller.handleRequest("updateStatus", statusRequest.toString());
                JsonObject statusResponse = gson.fromJson(statusResponseJson, JsonObject.class);
                System.out.println(statusResponse.get("success").getAsBoolean() ?
                        "✅ Cập nhật trạng thái thành công" :
                        "❌ Cập nhật thất bại");
            }

            // ====== TEST 4: Lấy danh sách người dùng ======
            System.out.println("\n🧩 Test 4: Danh sách người chơi");
            String allUsersJson = controller.handleRequest("getAllUsers", "{}");
            JsonObject allUsersResponse = gson.fromJson(allUsersJson, JsonObject.class);

            if (allUsersResponse.get("success").getAsBoolean()) {
                user[] users = gson.fromJson(allUsersResponse.get("data"), user[].class);
                for (user u : users) {
                    System.out.printf("👤 %-15s | Trạng thái: %s | Elo: %d\n",
                            u.getUserName(), u.getStatus(), u.getEloRating());
                }
            } else {
                System.out.println("❌ Lấy danh sách thất bại: " + allUsersResponse.get("error").getAsString());
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi trong quá trình test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.close(); // đóng pool sau khi test
            System.out.println("\n🧹 Đã đóng pool kết nối.");
        }
    }
}
