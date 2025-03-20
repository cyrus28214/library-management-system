package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import service.LibraryManagementSystem;
import utils.JsonUtil;
import queries.ApiResult;

public class CardHandler implements HttpHandler {
    private final LibraryManagementSystem lms;
    
    public CardHandler(LibraryManagementSystem lms) {
        this.lms = lms;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        
        switch (requestMethod) {
            case "GET":
                this.handleGetRequest(exchange);
                break;
                
            case "POST":
                // TODO
                break;
                
            default:
                exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, 0);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        ApiResult result = this.lms.showCards();
        // use jackson to serialize the result
        String response = JsonUtil.toJson(result);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response.getBytes());
        }
        exchange.close();
    }
}