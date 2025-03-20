import utils.ConnectConfig;
import utils.DatabaseConnector;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import handlers.CardHandler;
import handlers.BorrowHandler;
import handlers.CorsFilter;
import service.LibraryManagementSystem;
import service.LibraryManagementSystemImpl;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import utils.LogFormatter;
import java.util.logging.LogManager;
public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            // configure logger
            LogManager.getLogManager().reset();
            Logger rootLogger = LogManager.getLogManager().getLogger("");
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new LogFormatter());
            rootLogger.addHandler(consoleHandler);

            // parse connection config from "resources/application.yaml"
            ConnectConfig conf = new ConnectConfig();
            log.info("Success to parse connect config. " + conf.toString());
            // connect to database
            DatabaseConnector connector = new DatabaseConnector(conf);
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }

            LibraryManagementSystem lms = new LibraryManagementSystemImpl(connector);

            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/card", new CorsFilter(new CardHandler(lms)));
            server.createContext("/borrow", new CorsFilter(new BorrowHandler(lms)));
            server.start();
            log.info("Server is listening on port 8000");

            // release database connection handler
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (connector.release()) {
                    log.info("Success to release connection.");
                } else {
                    log.warning("Failed to release connection.");
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
