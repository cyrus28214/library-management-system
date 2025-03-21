package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import service.LibraryManagementSystem;
import utils.HttpUtil;
import java.util.logging.Logger;
import java.util.Map;
import queries.ApiResult;
import entities.Borrow;
import com.fasterxml.jackson.annotation.JsonProperty;
record BorrowPostRequest(
    @JsonProperty(required = true) int bookId,
    @JsonProperty(required = true) int cardId
) {};

public class BorrowHandler implements HttpHandler {
    private final Logger log = Logger.getLogger(BorrowHandler.class.getName());

    private final LibraryManagementSystem lms;

    public BorrowHandler(LibraryManagementSystem lms) {
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
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = HttpUtil.extractParams(query);
        log.info("GET /borrow with params: " + params.toString());
        // check if params contains "cardId"
        if (!params.containsKey("cardId")) {
            exchange.sendResponseHeaders(400, -1);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "cardId is required"));
            return;
        }
        String cardIdStr = params.get("cardId");
        try {
            int cardId = Integer.parseInt(cardIdStr);
            ApiResult result = this.lms.showBorrowHistory(cardId);
            exchange.sendResponseHeaders(200, 0);
            HttpUtil.jsonResponse(exchange, result);
        } catch (NumberFormatException e) {
            exchange.sendResponseHeaders(400, -1);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "cardId is not a valid integer"));
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        BorrowPostRequest request = HttpUtil.jsonRequest(exchange, BorrowPostRequest.class);
        log.info("POST /borrow with request body: " + request);
        Borrow borrow = new Borrow(request.bookId(), request.cardId());
        ApiResult result = this.lms.borrowBook(borrow);
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }
}