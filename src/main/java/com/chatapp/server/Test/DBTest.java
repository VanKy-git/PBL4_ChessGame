package com.chatapp.server.Test;

import Model.Entity.DBConnection;

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
