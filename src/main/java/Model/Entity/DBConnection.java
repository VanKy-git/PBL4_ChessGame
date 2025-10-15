package Model.Entity;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;


public class DBConnection {
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/pbl4db");
        config.setUsername("postgres");
        config.setPassword("300325");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);      // 30s
        config.setMaxLifetime(1800000);    // 30p

        dataSource = new HikariDataSource(config);
    }
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // Đóng pool khi tắt app
    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

}
