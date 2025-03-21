package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.logging.Logger;
import service.LibraryManagementSystem;
import queries.ApiResult;
import utils.HttpUtil;
import queries.BookQueryConditions;
import entities.Book;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

record BookPostRequest(
    @JsonProperty(required = true) String category,
    @JsonProperty(required = true) String title,
    @JsonProperty(required = true) String press,
    @JsonProperty(required = true) int publishYear,
    @JsonProperty(required = true) String author,
    @JsonProperty(required = true) double price,
    @JsonProperty(required = true) int stock
) {}

record BookPutRequest(
    @JsonProperty(required = true) int bookId,
    @JsonProperty(required = true) String category,
    @JsonProperty(required = true) String title,
    @JsonProperty(required = true) String press,
    @JsonProperty(required = true) int publishYear,
    @JsonProperty(required = true) String author,
    @JsonProperty(required = true) double price,
    @JsonProperty(required = false, defaultValue = "0") int deltaStock
) {}

public class BookHandler implements HttpHandler {
    private final Logger log = Logger.getLogger(CardHandler.class.getName());
    private final LibraryManagementSystem lms;
    
    public BookHandler(LibraryManagementSystem lms) {
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
                case "PUT":
                    this.handlePutRequest(exchange);
                    break;
                default:
                    exchange.sendResponseHeaders(405, -1);
            }
        } catch (Exception e) {
            exchange.sendResponseHeaders(500, 0);
            log.severe(String.format("%s /book error: %s", requestMethod, e.getMessage()));
            HttpUtil.jsonResponse(exchange, new ApiResult(false, e.getMessage()));
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        ApiResult result = this.lms.queryBook(new BookQueryConditions());
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        BookPostRequest request = HttpUtil.jsonRequest(exchange, BookPostRequest.class);
        log.info("POST /book with body: " + request);
        Book newBook = new Book(request.category(), request.title(), request.press(), request.publishYear(), request.author(), request.price(), request.stock());
        ApiResult result = this.lms.storeBook(newBook);
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }

    private void handleDeleteRequest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = HttpUtil.extractParams(query);
        log.info("DELETE /book with params: " + params.toString());
        if (!params.containsKey("bookId")) {
            exchange.sendResponseHeaders(400, -1);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "bookId is required"));
        }
        String bookIdStr = params.get("bookId");
        int bookId = Integer.parseInt(bookIdStr);
        ApiResult result = this.lms.removeBook(bookId);
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }

    private void handlePutRequest(HttpExchange exchange) throws IOException {
        BookPutRequest request = HttpUtil.jsonRequest(exchange, BookPutRequest.class);
        log.info("PUT /book with body: " + request);
        Book mBook = new Book(
            request.category(),
            request.title(),
            request.press(),
            request.publishYear(),
            request.author(),
            request.price(),
            0 // should not be modified
        );
        mBook.setBookId(request.bookId());
        ApiResult modifyInfoResult = this.lms.modifyBookInfo(mBook);
        log.info("modifyInfoResult: " + modifyInfoResult.message);
        ApiResult incStockResult = this.lms.incBookStock(request.bookId(), request.deltaStock());
        log.info("incStockResult: " + incStockResult.message);
        
        // 组合两个ApiResult的结果
        boolean combinedOk = modifyInfoResult.ok && incStockResult.ok;
        String message;
        
        if (!modifyInfoResult.ok && !incStockResult.ok) {
            message = "修改图书信息和库存都失败: " + modifyInfoResult.message + "; " + incStockResult.message;
        } else if (!modifyInfoResult.ok) {
            message = "修改图书信息失败: " + modifyInfoResult.message;
        } else if (!incStockResult.ok) {
            message = "修改库存失败: " + incStockResult.message;
        } else {
            message = "图书信息和库存修改成功";
        }
        
        ApiResult result = new ApiResult(combinedOk, message);
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }
}