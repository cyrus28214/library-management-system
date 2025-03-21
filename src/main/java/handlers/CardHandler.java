package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.logging.Logger;
import service.LibraryManagementSystem;
import queries.ApiResult;
import utils.HttpUtil;
import entities.Card;

record PostRequest(String name, String department, String type) {}
record PutRequest(int cardId, String name, String department, String type) {}

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
            case "PUT":
                this.handlePutRequest(exchange);
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
        PostRequest request = HttpUtil.jsonRequest(exchange, PostRequest.class);
        log.info("POST /card with body: " + request);
        Card.CardType type = Card.CardType.values(request.type());
        if (type == null) {
            exchange.sendResponseHeaders(400, -1);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "Invalid card type"));
            return;
        }
        Card card = new Card(0, 
            request.name(), 
            request.department(), 
            type
        );
        ApiResult result = this.lms.registerCard(card);
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }

    private void handlePutRequest(HttpExchange exchange) throws IOException {
        PutRequest request = HttpUtil.jsonRequest(exchange, PutRequest.class);
        log.info("PUT /card with body: " + request);
        Card.CardType type = Card.CardType.values(request.type());
        if (type == null) {
            exchange.sendResponseHeaders(400, -1);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "Invalid card type"));
            return;
        }
        Card card = new Card(request.cardId(), request.name(), request.department(), type);
        ApiResult result = this.lms.modifyCardInfo(card);
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }
}