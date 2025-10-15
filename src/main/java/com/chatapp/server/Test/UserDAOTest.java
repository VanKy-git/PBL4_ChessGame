package com.chatapp.server.Test;

import Model.DAO.userDAO;
import Model.Entity.user;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest {

    private static Connection conn;
    private static userDAO dao;
    private static int createdUserId;

    @BeforeAll
    static void setup() throws Exception {
        // ⚙️ Kết nối tới PostgreSQL (hoặc đổi sang MySQL nếu bạn dùng khác)
        String url = "jdbc:postgresql://localhost:5432/DataBase_PBL4";  // chỉnh lại nếu DB khác
        String username = "postgres";                          // tên user DB
        String password = "300325";                               // mật khẩu DB

        conn = DriverManager.getConnection(url, username, password);
        dao = new userDAO(conn);
        System.out.println("✅ Database connected successfully.");
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (conn != null) conn.close();
    }

    // ───────────────────────────────────────────────
    // 1️⃣ Đăng ký / Đăng nhập
//    // ───────────────────────────────────────────────
//    @Test
//    @Order(1)
//    void testCreateUser() throws Exception {
//        user u = dao.createUser("test_user_junit", "pass123");
//        assertNotNull(u);
//        createdUserId = u.getUser_id();
//        System.out.println("🆕 Created user ID: " + createdUserId);
//    }
//
//    @Test
//    @Order(2)
//    void testLoginSuccess() throws Exception {
//        user u = dao.login("test_user_junit", "pass123");
//        assertNotNull(u);
//        assertEquals("test_user_junit", u.getUser_name());
//    }
//
//    @Test
//    @Order(3)
//    void testLoginFailWrongPassword() throws Exception {
//        user u = dao.login("test_user_junit", "wrong_pass");
//        assertNull(u);
//    }
//
//    // ───────────────────────────────────────────────
//    // 2️⃣ GOOGLE OAUTH
//    // ───────────────────────────────────────────────
//    @Test
//    @Order(4)
//    void testCreateUserWithGoogle() throws Exception {
//        user u = dao.createUserWithGoogle("test@gmail.com", "google_123", "GoogleUser", "/avatar.png");
//        assertNotNull(u);
//        assertEquals("google", u.getProvider());
//    }
//
//    // ───────────────────────────────────────────────
//    // 3️⃣ CẬP NHẬT DỮ LIỆU
//    // ───────────────────────────────────────────────
//    @Test
//    @Order(5)
//    void testUpdateEmailAndPassword() throws Exception {
//        boolean ok1 = dao.updateEmail(createdUserId, "new_email@example.com");
//        boolean ok2 = dao.updatePassword(createdUserId, "newpass456");
//        assertTrue(ok1 && ok2);
//
//        user updated = dao.getUser(createdUserId);
//        assertEquals("new_email@example.com", updated.getEmail());
//    }
//
//    @Test
//    @Order(6)
//    void testUpdateEloAndAvatar() throws Exception {
//        boolean ok1 = dao.updateEloRating(createdUserId, 1300);
//        boolean ok2 = dao.updateAvatar(createdUserId, "/img/new.png");
//        assertTrue(ok1 && ok2);
//
//        user updated = dao.getUser(createdUserId);
//        assertEquals(1300, updated.getElo_rating());
//    }
//
//    // ───────────────────────────────────────────────
//    // 4️⃣ STATUS
//    // ───────────────────────────────────────────────
//    @Test
//    @Order(7)
//    void testUpdateStatus() throws Exception {
//        boolean ok = dao.updateStatus(createdUserId, "Online");
//        assertTrue(ok);
//        user updated = dao.getUser(createdUserId);
//        assertEquals("Online", updated.getStatus());
//    }
//
//    // ───────────────────────────────────────────────
//    // 5️⃣ TRẬN ĐẤU (WIN/LOSS)
//    // ───────────────────────────────────────────────
//    @Test
//    @Order(8)
//    void testWinLossIncrement() throws Exception {
//        boolean winOK = dao.incrementWinCount(createdUserId);
//        boolean lossOK = dao.incrementLossCount(createdUserId);
//        assertTrue(winOK && lossOK);
//
//        user updated = dao.getUser(createdUserId);
//        assertTrue(updated.getWin_count() >= 1);
//        assertTrue(updated.getLoss_count() >= 1);
//    }
//
//    @Test
//    @Order(9)
//    void testUpdateMatchResult() throws Exception {
//        boolean winOK = dao.updateMatchResultWin(createdUserId, 10);
//        boolean lossOK = dao.updateMatchResultLoss(createdUserId, -5);
//        assertTrue(winOK && lossOK);
//    }
//
//    // ───────────────────────────────────────────────
//    // 6️⃣ THỐNG KÊ VÀ DANH SÁCH
//    // ───────────────────────────────────────────────
//    @Test
//    @Order(10)
//    void testLeaderboardAndStatistics() throws Exception {
//        List<user> leaderboard = dao.getUsersOrderByElo();
//        assertNotNull(leaderboard);
//        assertFalse(leaderboard.isEmpty());
//
//        userDAO.UserStatistics stats = dao.getUserStatistics(createdUserId);
//        assertNotNull(stats);
//        System.out.println("📊 Winrate: " + stats.winRate + "%");
//    }
//
//    // ───────────────────────────────────────────────
//    // 7️⃣ XÓA NGƯỜI DÙNG
//    // ───────────────────────────────────────────────
  @Test
  @Order(11)
    void testDeleteUser() throws Exception {
        boolean deleted = dao.deleteUser(89);
        assertTrue(deleted);

    }
}
