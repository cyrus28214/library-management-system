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
import java.util.List;
import java.util.ArrayList;
import queries.SortOrder;

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

record BookInfoRequest(
    @JsonProperty(required = true) int bookId,
    @JsonProperty(required = true) String category,
    @JsonProperty(required = true) String title,
    @JsonProperty(required = true) String press,
    @JsonProperty(required = true) int publishYear,
    @JsonProperty(required = true) String author,
    @JsonProperty(required = true) double price
) {}

record BookStockRequest(
    @JsonProperty(required = true) int bookId,
    @JsonProperty(required = true) int deltaStock
) {}

record BookBatchPostRequest(
    @JsonProperty(required = true) List<BookPostRequest> books
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
        String path = exchange.getRequestURI().getPath();
        
        try {
            switch (requestMethod) {
                case "GET":
                    this.handleGetRequest(exchange);
                    break;  
                case "POST":
                    if (path.endsWith("/batch")) {
                        this.handleBatchPostRequest(exchange);
                    } else {
                        this.handlePostRequest(exchange);
                    }
                    break;
                case "DELETE":
                    this.handleDeleteRequest(exchange);
                    break;
                case "PUT":
                    if (path.endsWith("/info")) {
                        this.handlePutInfoRequest(exchange);
                    } else if (path.endsWith("/stock")) {
                        this.handlePutStockRequest(exchange);
                    } else {
                        exchange.sendResponseHeaders(404, 0);
                        HttpUtil.jsonResponse(exchange, new ApiResult(false, "invalid paths"));
                    }
                    break;
                default:
                    exchange.sendResponseHeaders(405, -1);
            }
        } catch (Exception e) {
            exchange.sendResponseHeaders(500, 0);
            e.printStackTrace();
            log.severe(e.getMessage());
            HttpUtil.jsonResponse(exchange, new ApiResult(false, e.getMessage()));
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = HttpUtil.extractParams(query);
        log.info("GET /book with params: " + params.toString());
        BookQueryConditions conditions = new BookQueryConditions();
        try {
            if (params.containsKey("category")) {
                conditions.setCategory(params.get("category"));
            }
            if (params.containsKey("title")) {
                conditions.setTitle(params.get("title"));
            }
            if (params.containsKey("press")) {
                conditions.setPress(params.get("press"));
            }
            if (params.containsKey("minPublishYear")) {
                int minPublishYear = Integer.parseInt(params.get("minPublishYear"));
                conditions.setMinPublishYear(minPublishYear);
            }
            if (params.containsKey("maxPublishYear")) {
                int maxPublishYear = Integer.parseInt(params.get("maxPublishYear"));
                conditions.setMaxPublishYear(maxPublishYear);
            }
            if (params.containsKey("author")) {
                conditions.setAuthor(params.get("author"));
            }
            if (params.containsKey("minPrice")) {
                double minPrice = Double.parseDouble(params.get("minPrice"));
                conditions.setMinPrice(minPrice);
            }
            if (params.containsKey("maxPrice")) {
                double maxPrice = Double.parseDouble(params.get("maxPrice"));
                conditions.setMaxPrice(maxPrice);
            }
        } catch (NumberFormatException e) {
            exchange.sendResponseHeaders(400, 0);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "数字格式不正确"));
        }
        try {
            if (params.containsKey("sortBy")) {
                conditions.setSortBy(Book.SortColumn.valueOf(params.get("sortBy")));
            }
            if (params.containsKey("sortOrder")) {
                conditions.setSortOrder(SortOrder.valueOf(params.get("sortOrder")));
            }
        } catch (IllegalArgumentException e) {
            exchange.sendResponseHeaders(400, 0);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "排序格式不正确"));
        }
        ApiResult result = this.lms.queryBook(conditions);
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        BookPostRequest request = HttpUtil.jsonRequest(exchange, BookPostRequest.class);
        log.info("POST /book with body: " + request);
        // stock must be greater than 0
        if (request.stock() <= 0) {
            exchange.sendResponseHeaders(400, 0);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "库存不能为负"));
            return;
        }
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
            exchange.sendResponseHeaders(400, 0);
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "bookId is required"));
        }
        String bookIdStr = params.get("bookId");
        int bookId = Integer.parseInt(bookIdStr);
        ApiResult result = this.lms.removeBook(bookId);
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }

    private void handlePutInfoRequest(HttpExchange exchange) throws IOException {
        BookInfoRequest request = HttpUtil.jsonRequest(exchange, BookInfoRequest.class);
        log.info("PUT /book/info with body: " + request);
        
        Book mBook = new Book(
            request.category(),
            request.title(),
            request.press(),
            request.publishYear(),
            request.author(),
            request.price(),
            0
        );
        mBook.setBookId(request.bookId());
        
        ApiResult result = this.lms.modifyBookInfo(mBook);
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }

    private void handlePutStockRequest(HttpExchange exchange) throws IOException {
        BookStockRequest request = HttpUtil.jsonRequest(exchange, BookStockRequest.class);
        log.info("PUT /book/stock with body: " + request);
        
        ApiResult result = this.lms.incBookStock(request.bookId(), request.deltaStock());
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }

    private void handleBatchPostRequest(HttpExchange exchange) throws IOException {
        BookBatchPostRequest request = HttpUtil.jsonRequest(exchange, BookBatchPostRequest.class);
        log.info("POST /book/batch with " + request.books().size() + " books");
        
        List<Book> books = new ArrayList<>();
        for (BookPostRequest bookRequest : request.books()) {
            Book newBook = new Book(
                bookRequest.category(), 
                bookRequest.title(), 
                bookRequest.press(), 
                bookRequest.publishYear(), 
                bookRequest.author(), 
                bookRequest.price(), 
                bookRequest.stock()
            );
            books.add(newBook);
        }
        
        ApiResult result = this.lms.storeBook(books);
        exchange.sendResponseHeaders(200, 0);
        HttpUtil.jsonResponse(exchange, result);
    }
}