package utils;

import java.util.Map;
import java.net.URLDecoder;
import java.util.HashMap;  
import java.util.logging.Logger;
import com.sun.net.httpserver.HttpExchange;
import java.io.OutputStream;
import java.io.IOException;

public class HttpUtil {
    private static final Logger log = Logger.getLogger(HttpUtil.class.getName());

    public static Map<String, String> extractParams(String query) {
        Map<String, String> params = new HashMap<>();
        try {
            if (query != null && !query.isEmpty()) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=", 2);
                    if (keyValue.length == 2) {
                        String key = URLDecoder.decode(keyValue[0], "UTF-8");
                        String value = URLDecoder.decode(keyValue[1], "UTF-8");
                        params.put(key, value);
                    } else if (keyValue.length == 1) {
                        String key = URLDecoder.decode(keyValue[0], "UTF-8");
                        params.put(key, "");
                    }
                }
            }
        } catch (Exception e) {
            log.severe("Failed to extract params from query: " + query);
            throw new RuntimeException("Failed to extract params from query", e);
        }
        return params;
    }

    public static void jsonResponse(HttpExchange exchange, Object obj) {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        String response = JsonUtil.toJson(obj);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response.getBytes());
            outputStream.close(); 
        } catch (IOException e) {
            log.severe("Failed to write response: " + e.getMessage());
            throw new RuntimeException("Failed to write response", e);
        }
        exchange.close();
    }

    public static <T> T jsonRequest(HttpExchange exchange, Class<T> requestType) {
        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), "UTF-8");
            return JsonUtil.fromJson(requestBody, requestType);
        } catch (IOException e) {
            log.severe("Failed to read request body: " + e.getMessage());
            throw new RuntimeException("Failed to read request body", e);
        }
    }
}
