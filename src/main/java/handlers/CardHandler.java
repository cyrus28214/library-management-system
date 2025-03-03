package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

public class CardHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // CORS
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        String requestMethod = exchange.getRequestMethod();
        if (requestMethod.equals("GET")) {
            // TODO: handle GET request
        } else if (requestMethod.equals("POST")) {
            // TODO: handle POST request
        } else if (requestMethod.equals("OPTIONS")) {
            // TODO: handle OPTIONS request
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }
}