import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();

        try {
            // first check if the book already exists
            // we assume that two books are equal iff their category...author are equal
            String checkSql = "SELECT count(*) FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, book.getCategory());
            checkStmt.setString(2, book.getTitle());
            checkStmt.setString(3, book.getPress());
            checkStmt.setInt(4, book.getPublishYear());
            checkStmt.setString(5, book.getAuthor());
            ResultSet checkRs = checkStmt.executeQuery();
            if (checkRs.next() && checkRs.getInt(1) > 0) {
                return new ApiResult(false, "Book already exists");
            }

            // if the book does not exist, insert it
            String insertSql = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, book.getCategory());
            insertStmt.setString(2, book.getTitle());
            insertStmt.setString(3, book.getPress());
            insertStmt.setInt(4, book.getPublishYear());
            insertStmt.setString(5, book.getAuthor());
            insertStmt.setDouble(6, book.getPrice());
            insertStmt.setInt(7, book.getStock());
            
            insertStmt.executeUpdate();

            ResultSet insertRs = insertStmt.getGeneratedKeys();
            if (insertRs.next()) {
                book.setBookId(insertRs.getInt(1));
            }

            commit(conn);
            return new ApiResult(true, null);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult removeBook(int bookId) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        try {
            StringBuilder sql = new StringBuilder();
            List<Object> params = new ArrayList<>();
            
            sql.append("SELECT * FROM book WHERE 1=1 ");
            
            // conditions
            if (conditions.getCategory() != null) {
                sql.append("AND category = ? ");
                params.add(conditions.getCategory()); // exact matching
            }
            
            if (conditions.getTitle() != null) {
                sql.append("AND title LIKE ? ");
                params.add("%" + conditions.getTitle() + "%");  // fuzzy matching
            }
            
            if (conditions.getPress() != null) {
                sql.append("AND press LIKE ? ");
                params.add("%" + conditions.getPress() + "%");  // fuzzy matching
            }
            
            if (conditions.getAuthor() != null) {
                sql.append("AND author LIKE ? ");
                params.add("%" + conditions.getAuthor() + "%");  // fuzzy matching
            }
            
            if (conditions.getMinPublishYear() != null) {
                sql.append("AND publish_year >= ? ");
                params.add(conditions.getMinPublishYear());
            }
            if (conditions.getMaxPublishYear() != null) {
                sql.append("AND publish_year <= ? ");
                params.add(conditions.getMaxPublishYear());
            }
            
            if (conditions.getMinPrice() != null) {
                sql.append("AND price >= ? ");
                params.add(conditions.getMinPrice());
            }
            if (conditions.getMaxPrice() != null) {
                sql.append("AND price <= ? ");
                params.add(conditions.getMaxPrice());
            }

            sql.append("ORDER BY ");
            if (conditions.getSortBy() != null) {
                switch (conditions.getSortBy()) {
                    case BOOK_ID:
                        sql.append("book_id");
                        break;
                    case CATEGORY:
                        sql.append("category");
                        break;
                    case TITLE:
                        sql.append("title");
                        break;
                    case PRESS:
                        sql.append("press");
                        break;
                    case PUBLISH_YEAR:
                        sql.append("publish_year");
                        break;
                    case AUTHOR:
                        sql.append("author");
                        break;
                    case PRICE:
                        sql.append("price");
                        break;
                    default:
                        sql.append("book_id");
                }
            } else {
                sql.append("book_id");
            }
            
            sql.append(conditions.getSortOrder() == SortOrder.ASC ? " ASC" : " DESC");
            
            // if the primary sort field is not book_id, add book_id as the secondary sort
            if (conditions.getSortBy() != Book.SortColumn.BOOK_ID) {
                sql.append(", book_id ASC");
            }
            
            try (PreparedStatement stmt = connector.getConn().prepareStatement(sql.toString())) {
                // set parameters
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
                
                ResultSet rs = stmt.executeQuery();
                List<Book> books = new ArrayList<>();
                while (rs.next()) {
                    Book book = new Book();
                    book.setBookId(rs.getInt("book_id"));
                    book.setCategory(rs.getString("category"));
                    book.setTitle(rs.getString("title"));
                    book.setPress(rs.getString("press"));
                    book.setPublishYear(rs.getInt("publish_year"));
                    book.setAuthor(rs.getString("author"));
                    book.setPrice(rs.getDouble("price"));
                    book.setStock(rs.getInt("stock"));
                    books.add(book);
                }
                
                BookQueryResults results = new BookQueryResults(books);
                
                return new ApiResult(true, results);
            }
            
        } catch (Exception e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult registerCard(Card card) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult removeCard(int cardId) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult showCards() {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
