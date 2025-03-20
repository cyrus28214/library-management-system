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
        exchange.sendResponseHeaders(200, 0);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = HttpUtil.extractParams(query);
        log.info("GET /borrow with params: " + params.toString());
        // check if params contains "cardID"
        if (!params.containsKey("cardID")) {
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "cardID is required"));
            return;
        }
        String cardIdStr = params.get("cardID");
        try {
            int cardId = Integer.parseInt(cardIdStr);
            ApiResult result = this.lms.showBorrowHistory(cardId);
            HttpUtil.jsonResponse(exchange, result);
        } catch (NumberFormatException e) {
            HttpUtil.jsonResponse(exchange, new ApiResult(false, "cardID is not a valid integer"));
        }
    }
}