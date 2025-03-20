package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.logging.Logger;

public class JsonUtil {
    private static final Logger log = Logger.getLogger(JsonUtil.class.getName());
    // https://fasterxml.github.io/jackson-databind/javadoc/2.6/com/fasterxml/jackson/databind/ObjectMapper.html
    // 官方文档指出，ObjectMapper是thread-safe的，因此，不需要每次都new ObjectMapper()，使用单例模式
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.severe("Failed to convert object to JSON string. Object: " + obj);
            throw new RuntimeException("Failed to convert object to JSON string", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (Exception e) {
            log.severe("Failed to convert JSON string to object. JSON: " + json);
            throw new RuntimeException("Failed to convert JSON string to object", e);
        }
    }
}
