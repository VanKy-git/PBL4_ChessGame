package com.database.server.Test;

import com.database.server.Entity.DBConnection;

import java.sql.*;


public class DBTest {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("✅ Kết nối thành công: " + conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
