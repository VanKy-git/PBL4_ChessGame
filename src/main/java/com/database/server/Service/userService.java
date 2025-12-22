package com.database.server.Service;

import com.database.server.DAO.userDAO;
import com.database.server.Entity.user;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.mindrot.jbcrypt.BCrypt;

import java.security.Key;
import java.util.*;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.database.server.Utils.JwtConfig;

/**
 * userService - Xử lý nghiệp vụ cho đối tượng {@link user}.
 * - Không thao tác SQL trực tiếp.
 * - Mỗi request mở/đóng EntityManager riêng.
 * - Transaction được quản lý tại Service, không ở DAO.
 */
public class userService {

    private final EntityManagerFactory emf;
    private final GoogleIdTokenVerifier verifier;

    // ⚠️⚠️⚠️ THAY ĐỔI CLIENT_ID NÀY BẰNG CLIENT ID CỦA BẠN ⚠️⚠️⚠️
    private static final String GOOGLE_CLIENT_ID = "660085540047-jc210st32m11fil9rp7n5lck025jfc67.apps.googleusercontent.com";

    public userService(EntityManagerFactory emf) {
        this.emf = emf;

        // Khởi tạo Google ID Token Verifier để xác thực token THẬT với Google
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                .build();
    }

    // ========== ĐĂNG NHẬP / ĐĂNG KÝ LOCAL ==========

    public Map<String, Object> login(String username, String password) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            // 1. Lấy user từ DB (sử dụng hàm DAO mới)
            user dbUser = dao.findByUsername(username);

            // 2. Kiểm tra User có tồn tại không
            if (dbUser == null) {
                throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu");
            }

            // 3. BỎ QUA KIỂM TRA STATUS ĐỂ CHO PHÉP LOGIN ĐÈ
            // if (!"Offline".equals(dbUser.getStatus())) {
            //     throw new RuntimeException("Tài khoản đang online ở nơi khác");
            // }

            // 4. KIỂM TRA MẬT KHẨU (Quan trọng nhất)
            // Giả sử dbUser.getPassword() trả về chuỗi HASH từ BCrypt
            boolean passwordMatch = BCrypt.checkpw(password, dbUser.getPassword());

            if (!passwordMatch) {
                // Nếu mật khẩu không khớp -> ném lỗi
                throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu");
            }

            // 5. Mật khẩu ĐÚNG -> TẠO TOKEN
            long nowMillis = System.currentTimeMillis();
            Date now = new Date(nowMillis);
            Date exp = new Date(nowMillis + JwtConfig.JWT_EXPIRATION_MS);

            String token = Jwts.builder()
                    .setSubject(String.valueOf(dbUser.getUserId())) // Lưu ID user
                    .claim("username", dbUser.getUserName()) // Lưu username
                    .setIssuedAt(now)
                    .setExpiration(exp)
                    .signWith(JwtConfig.JWT_SECRET_KEY, SignatureAlgorithm.HS256)
                    .compact();
            System.out.println(token + " " + JwtConfig.JWT_SECRET_KEY);
            // 6. Cập nhật trạng thái user thành "Online"
            dao.updateStatus(dbUser.getUserId(), "Online");

            // 7. Trả về kết quả
            Map<String, Object> loginResult = new HashMap<>();
            loginResult.put("token", token);
            loginResult.put("userId", dbUser.getUserId());
            loginResult.put("username", dbUser.getUserName());

            return loginResult;

        } finally {
            em.close();
        }
    }

    public user register(String username, String password) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);

        try {
            em.getTransaction().begin();

            if (dao.isUsernameExists(username)) {
                throw new RuntimeException("Username already exists!");
            }
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            user newUser = dao.createUser(username, hashedPassword);

            em.getTransaction().commit();
            return newUser;

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========== GOOGLE OAUTH ==========

    /**
     * Verify ID token with Google and return payload (or null nếu invalid)
     */
    public GoogleIdToken.Payload verifyIdToken(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                System.err.println("Invalid ID token");
                return null;
            }
            return idToken.getPayload();
        } catch (Exception e) {
            System.err.println("verifyIdToken error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Register new user using Google ID token.
     * Nếu email/googleId đã tồn tại -> ném RuntimeException (hoặc trả null)
     */
    public user registerWithGoogle(String idTokenString) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);

        try {
            GoogleIdToken.Payload payload = verifyIdToken(idTokenString);
            if (payload == null) {
                throw new RuntimeException("Invalid Google ID token");
            }

            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            em.getTransaction().begin();

            // Nếu đã có user với googleId -> báo đã tồn tại
            user byGoogle = dao.getUserByGoogleId(googleId);
            if (byGoogle != null) {
                em.getTransaction().rollback();
                throw new RuntimeException("Tài khoản Google đã tồn tại. Vui lòng đăng nhập.");
            }

            // Nếu email đã tồn tại và provider != google -> báo lỗi
            if (email != null && dao.isEmailExists(email)) {
                user byEmail = dao.getUserByEmail(email);
                if (byEmail != null && !"google".equals(byEmail.getProvider())) {
                    em.getTransaction().rollback();
                    throw new RuntimeException("Email đã được đăng ký bằng phương thức khác. Vui lòng đăng nhập và liên kết.");
                }
                // if byEmail.provider == google and providerId null, we may link instead
            }

            // Tạo user mới bằng data từ Google
            user newUser = dao.createUserWithGoogle(email, googleId, name, picture);
            newUser.setStatus("Offline");
            em.getTransaction().commit();
            return newUser;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Error registering with Google: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    /**
     * Login by Google token — chỉ chấp nhận khi user đã tồn tại và liên kết Google
     * Trả về user nếu thành công, ngược lại trả null (hoặc ném exception)
     */
    public user loginWithGoogle(String idTokenString) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            GoogleIdToken.Payload payload = verifyIdToken(idTokenString);
            if (payload == null) {
                throw new RuntimeException("Invalid Google ID token");
            }

            String googleId = payload.getSubject();
            String email = payload.getEmail();

            // Ưu tiên tìm theo googleId
            user existing = dao.getUserByGoogleId(googleId);

            // Nếu chưa tìm theo googleId, có thể tìm theo email + provider == google
            if (existing == null && email != null) {
                user byEmail = dao.getUserByEmail(email);
                if (byEmail != null && "google".equals(byEmail.getProvider())) {
                    existing = byEmail;
                }
            }

            if (existing == null) {
                // Không tồn tại => trả null (frontend sẽ yêu cầu register)
                return null;
            }

            // Cập nhật avatar nếu thay đổi, set status online nếu bạn muốn
            boolean changed = false;
            String picture = (String) payload.get("picture");
            if (picture != null && !picture.equals(existing.getAvatarUrl())) {
                em.getTransaction().begin();
                existing.setAvatarUrl(picture);
                dao.updateUser(existing);
                em.getTransaction().commit();
                changed = true;
            }

            return existing;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error logging in with Google: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }


//    /**
//     * Xác thực Google ID Token THẬT và đăng nhập/đăng ký user
//     * Token được verify với Google server để đảm bảo tính xác thực
//     *
//     * @param idTokenString Token nhận từ Google Sign-In button
//     * @return User object hoặc null nếu thất bại
//     */
//    public user authenticateWithGoogle(String idTokenString) {
//        EntityManager em = emf.createEntityManager();
//        userDAO dao = new userDAO(em);
//
//        try {
//            // BƯỚC 1: XÁC THỰC TOKEN VỚI GOOGLE SERVER (Quan trọng!)
//            GoogleIdToken idToken = verifier.verify(idTokenString);
//            if (idToken == null) {
//                System.err.println("❌ Invalid Google ID token - Token không hợp lệ hoặc đã hết hạn");
//                return null;
//            }
//
//            // BƯỚC 2: LẤY THÔNG TIN USER TỪ TOKEN
//            GoogleIdToken.Payload payload = idToken.getPayload();
//            String googleId = payload.getSubject(); // Google User ID (unique)
//            String email = payload.getEmail(); // Email của user
//            String name = (String) payload.get("name"); // Tên đầy đủ
//            String pictureUrl = (String) payload.get("picture"); // Avatar URL
//
//            System.out.println("✅ Google authentication successful:");
//            System.out.println("   Google ID: " + googleId);
//            System.out.println("   Email: " + email);
//            System.out.println("   Name: " + name);
//
//            em.getTransaction().begin();
//
//            // BƯỚC 3: KIỂM TRA USER ĐÃ TỒN TẠI VỚI GOOGLE ID NÀY CHƯA
//            user existingUser = dao.getUserByGoogleId(googleId);
//
//            if (existingUser != null) {
//                // User đã tồn tại -> Đăng nhập
//                System.out.println("   Existing user found: " + existingUser.getUserName());
//                existingUser.setStatus("Online");
//
//                // Cập nhật avatar nếu có thay đổi
//                if (pictureUrl != null && !pictureUrl.equals(existingUser.getAvatarUrl())) {
//                    existingUser.setAvatarUrl(pictureUrl);
//                }
//
//                dao.updateUser(existingUser);
//                em.getTransaction().commit();
//                return existingUser;
//            }
//
//            // BƯỚC 4: KIỂM TRA EMAIL ĐÃ ĐƯỢC SỬ DỤNG CHƯA
//            if (dao.isEmailExists(email)) {
//                user userByEmail = dao.getUserByEmail(email);
//                if (userByEmail != null && !"google".equals(userByEmail.getProvider())) {
//                    // Email đã được dùng bởi local account
//                    em.getTransaction().rollback();
//                    throw new RuntimeException("Email đã được đăng ký. Vui lòng đăng nhập và liên kết tài khoản Google.");
//                }
//            }
//
//            // BƯỚC 5: TẠO USER MỚI VỚI THÔNG TIN TỪ GOOGLE
//            System.out.println("   Creating new user with Google account");
//            user newUser = dao.createUserWithGoogle(email, googleId, name, pictureUrl);
//            newUser.setStatus("Online");
//
//            em.getTransaction().commit();
//            System.out.println("✅ New Google user created: " + newUser.getUserName());
//            return newUser;
//
//        } catch (Exception e) {
//            if (em.getTransaction().isActive()) em.getTransaction().rollback();
//            System.err.println("❌ Google authentication error: " + e.getMessage());
//            e.printStackTrace();
//            throw new RuntimeException(e.getMessage());
//        } finally {
//            em.close();
//        }
//    }


    /**
     * Liên kết tài khoản Google THẬT vào user hiện có
     *
     * @param userId ID của user hiện tại (đã đăng nhập)
     * @param idTokenString Google ID Token từ Google Sign-In
     * @return true nếu liên kết thành công
     */
    public boolean linkGoogleToExistingAccount(int userId, String idTokenString) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);

        try {
            // XÁC THỰC TOKEN VỚI GOOGLE
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                System.err.println("❌ Invalid token when linking");
                return false;
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String avatarUrl = (String) payload.get("picture");

            System.out.println("🔗 Linking Google account:");
            System.out.println("   User ID: " + userId);
            System.out.println("   Google ID: " + googleId);
            System.out.println("   Email: " + email);

            em.getTransaction().begin();

            // Kiểm tra Google ID đã được user khác dùng chưa
            user existingGoogleUser = dao.getUserByGoogleId(googleId);
            if (existingGoogleUser != null && existingGoogleUser.getUserId() != userId) {
                em.getTransaction().rollback();
                throw new RuntimeException("Tài khoản Google này đã được liên kết với user khác.");
            }

            boolean success = dao.linkGoogleAccount(userId, googleId, email, avatarUrl);

            if (success) {
                em.getTransaction().commit();
                System.out.println("✅ Google account linked successfully");
                return true;
            } else {
                em.getTransaction().rollback();
                return false;
            }

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.err.println("❌ Error linking Google: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * Hủy liên kết tài khoản Google
     * Chỉ được phép nếu user đã có password
     *
     * @param userId ID của user
     * @return true nếu hủy liên kết thành công
     */
    public boolean unlinkGoogle(int userId) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);

        try {
            em.getTransaction().begin();

            // Kiểm tra user có password không (phải có password mới được unlink)
            if (!dao.hasPassword(userId)) {
                em.getTransaction().rollback();
                throw new RuntimeException("Không thể hủy liên kết Google. Vui lòng đặt mật khẩu trước.");
            }

            boolean success = dao.unlinkGoogleAccount(userId);

            if (success) {
                em.getTransaction().commit();
                System.out.println("✅ Google account unlinked for user: " + userId);
                return true;
            } else {
                em.getTransaction().rollback();
                return false;
            }

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.err.println("❌ Error unlinking: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        } finally {
            em.close();
        }
    }

    // ========== CRUD CƠ BẢN ==========

    public List<user> getAllUsers() {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            return dao.getAllUsers();
        } finally {
            em.close();
        }
    }

    public user getUserById(int id) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            return dao.getUserById(id);
        } finally {
            em.close();
        }
    }

    public boolean deleteUser(int id) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            em.getTransaction().begin();
            boolean result = dao.deleteUser(id);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========== CẬP NHẬT TRẠNG THÁI ==========

    public boolean updateStatus(int userId, String status) {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            em.getTransaction().begin();
            boolean updated = dao.updateStatus(userId, status);
            em.getTransaction().commit();
            return updated;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // ========== BẢNG XẾP HẠNG ==========
    public List<user> getLeaderboard() {
        EntityManager em = emf.createEntityManager();
        userDAO dao = new userDAO(em);
        try {
            return dao.getTopUsersByElo(20); // Lấy top 20 người chơi
        } finally {
            em.close();
        }
    }
}
