package com.database.server.Utils;

import com.google.gson.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GsonUtils {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final Gson gson = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    // Bỏ qua các field Hibernate proxy, handler, và vòng lặp thường gặp
                    String name = f.getName();
                    return f.getDeclaringClass().getName().contains("hibernate")
                            || name.equals("chatRoom")
                            || name.equals("messages")
                            || name.equals("participants");
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            // ✅ Serialize LocalDateTime an toàn
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> {
                        if (src == null) return JsonNull.INSTANCE;
                        try {
                            return new JsonPrimitive(FORMATTER.format(src));
                        } catch (Exception e) {
                            return new JsonPrimitive(src.toString());
                        }
                    })
            // ✅ Deserialize ngược lại
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> {
                        try {
                            return LocalDateTime.parse(json.getAsString(), FORMATTER);
                        } catch (Exception e) {
                            return LocalDateTime.parse(json.getAsString());
                        }
                    })
            .create();
}
