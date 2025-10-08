//package Test;
//
//import Model.DAO.userDAO;
//import Model.Entity.DBConnection;
//import Model.Entity.user;
//
//import java.sql.Connection;
//
//public class UserTest {
//    public static void main(String[] args) {
//        try (Connection conn = DBConnection.getConnection()) {
//            userDAO dao = new userDAO(conn);
//
//            // Thử đăng nhập
//            String testUsername = "nvn";
//            String testPassword = "3003";
//
//            user u = dao.login(testUsername, testPassword);
//
//            if (u != null) {
//                System.out.println("✅ Đăng nhập thành công!");
//                System.out.println("UserID: " + u.getUser_id());
//                System.out.println("Username: " + u.getUser_name());
//                System.out.println("Elo: " + u.getElo_rating());
//                System.out.println("CreatedAt: " + u.getCreate_at());
//            } else {
//                System.out.println("❌ Sai username hoặc password");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
