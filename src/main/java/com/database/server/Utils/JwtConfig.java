// File: com/database/server/Utils/JwtConfig.java
package com.database.server.Utils;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;

public class JwtConfig {
    // Đây là chìa khóa bí mật DUY NHẤT dùng chung cho cả 2 server
    private static final String SECRET = "this_is_a_very_strong_secret_key_for_jwt_2025!";
    public static final Key JWT_SECRET_KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    
    // Tăng thời gian hết hạn lên 24 giờ (86400000 ms) để thuận tiện cho việc test
    public static final long JWT_EXPIRATION_MS = 86400000;
}