package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import service.LibraryManagementSystem;
import utils.HttpUtil;
import java.util.logging.Logger;
import java.util.Map;
import queries.ApiResult;

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
}