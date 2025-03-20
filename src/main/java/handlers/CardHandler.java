package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.logging.Logger;
import service.LibraryManagementSystem;
import queries.ApiResult;
import utils.HttpUtil;
import utils.JsonUtil;
import entities.Card;

record PostCardRequest(String name, String department, String type) {}

public class CardHandler implements HttpHandler {
    private final Logger log = Logger.getLogger(CardHandler.class.getName());
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
                this.handlePostRequest(exchange);
                break;
            default:
                exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        ApiResult result = this.lms.showCards();
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        String requestBody = exchange.getRequestBody().toString();
        PostCardRequest request = JsonUtil.fromJson(requestBody, PostCardRequest.class);
        log.info("POST /card with body: " + requestBody);
        String name = request.name();
        String department = request.department();
        String type = request.type();
        Card card = new Card(0, name, department, Card.CardType.values(type));
        ApiResult result = this.lms.registerCard(card);
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }
}