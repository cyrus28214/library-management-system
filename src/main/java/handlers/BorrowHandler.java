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
    @JsonProperty(required = true) int cardId,
    @JsonProperty(required = true) long borrowTime
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
        try {
            switch (requestMethod) {
                case "GET":
                    this.handleGetRequest(exchange);
                break;
            case "POST":
                this.handlePostRequest(exchange);
                break;
            case "DELETE":
                this.handleDeleteRequest(exchange);
                break;
            default:
                exchange.sendResponseHeaders(405, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.severe(e.getMessage());
            exchange.sendResponseHeaders(500, 0);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, e.getMessage()));
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = HttpUtil.extractParams(query);
        log.info("GET /borrow with params: " + params.toString());
        // check if params contains "cardId"
        if (!params.containsKey("cardId")) {
            exchange.sendResponseHeaders(400, 0);
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
            exchange.sendResponseHeaders(400, 0);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "cardId is not a valid integer"));
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        BorrowPostRequest request = HttpUtil.jsonRequest(exchange, BorrowPostRequest.class);
        log.info("POST /borrow with request body: " + request);
        Borrow borrow = new Borrow(request.bookId(), request.cardId());
        borrow.setBorrowTime(request.borrowTime());
        ApiResult result = this.lms.borrowBook(borrow);
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }

    private void handleDeleteRequest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = HttpUtil.extractParams(query);
        log.info("DELETE /borrow with params: " + params.toString());
        if (!params.containsKey("bookId") || !params.containsKey("cardId") || !params.containsKey("returnTime")) {
            exchange.sendResponseHeaders(400, 0);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "图书ID、借书证ID和归还时间是不能为空"));
            return;
        }
        ApiResult result = null;
        try {
            int bookId = Integer.parseInt(params.get("bookId"));
            int cardId = Integer.parseInt(params.get("cardId"));
            long returnTime = Long.parseLong(params.get("returnTime"));
            Borrow borrow = new Borrow(bookId, cardId);
            borrow.setReturnTime(returnTime);
            result = this.lms.returnBook(borrow);
        } catch (NumberFormatException e) {
            exchange.sendResponseHeaders(400, 0);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "图书ID、借书证ID和归还时间必须是整数"));
            return;
        }
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }
}