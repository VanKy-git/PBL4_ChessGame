package com.chatapp.server.Utils;

import com.google.gson.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GsonUtils - Cấu hình Gson hoàn chỉnh:
 * ✅ Hỗ trợ LocalDateTime
 * ✅ Loại bỏ field Hibernate (proxy, handler)
 * ✅ Hỗ trợ @Expose annotation
 * ✅ Tùy chọn pretty JSON và serialize nulls
 */
public class GsonUtils {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final Gson gson = new GsonBuilder()
            // Chỉ serialize các trường có @Expose (nếu có)
            //.excludeFieldsWithoutExposeAnnotation()
            // Serialize các giá trị null
            .serializeNulls()
            // Hiển thị JSON đẹp
            .setPrettyPrinting()
            // Bỏ qua các field do Hibernate tạo ra
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getDeclaringClass().getName().contains("hibernate");
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            // Serialize LocalDateTime -> String
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                            new JsonPrimitive(src.format(FORMATTER)))
            // Deserialize String -> LocalDateTime
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                            LocalDateTime.parse(json.getAsString(), FORMATTER))
            .create();

//    /**
//     * Lấy Gson instance đã cấu hình sẵn
//     */
//    public static Gson getGson() {
//        return gson;
//    }
}
