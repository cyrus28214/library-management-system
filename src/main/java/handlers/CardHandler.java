package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import service.LibraryManagementSystem;

public class CardHandler implements HttpHandler {
    private final LibraryManagementSystem lms;
    
    public CardHandler(LibraryManagementSystem lms) {
        this.lms = lms;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        String response = "{\"message\": \"Hello, World!\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        
        switch (requestMethod) {
            case "GET":
                exchange.sendResponseHeaders(200, 0);
                // 使用 lms 处理请求
                // 例如：ApiResult result = lms.showCards();
                
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(response.getBytes());
                }
                break;
                
            case "POST":
                exchange.sendResponseHeaders(200, 0);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(response.getBytes());
                }
                break;
                
            default:
                exchange.sendResponseHeaders(405, -1);
        }
    }
}