package service;
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
        Connection conn = connector.getConn();
        try {
            // lock the row
            String checkSql = "SELECT stock FROM book WHERE book_id = ? FOR UPDATE";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, bookId);
            
            ResultSet rs = checkStmt.executeQuery();
            if (!rs.next()) {
                rollback(conn);
                return new ApiResult(false, "Book not found");
            }
            
            int currentStock = rs.getInt("stock");
            int newStock = currentStock + deltaStock;
            
            // check if the new stock is valid
            if (newStock < 0) {
                rollback(conn);
                return new ApiResult(false, "库存不能为负");
            }
            
            // update the stock
            String updateSql = "UPDATE book SET stock = ? WHERE book_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setInt(1, newStock);
            updateStmt.setInt(2, bookId);
            
            updateStmt.executeUpdate();
            
            commit(conn);
            return new ApiResult(true, null);
            
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return new ApiResult(true, null);
        }

        Connection conn = connector.getConn();
        try {
            // check duplicate books
            StringBuilder checkSql = new StringBuilder(
                "SELECT COUNT(*) FROM book WHERE (category, title, press, publish_year, author) IN ("
            );
            for (int i = 0; i < books.size(); i++) {
                if (i > 0) checkSql.append(",");
                checkSql.append("(?, ?, ?, ?, ?)");
            }
            checkSql.append(")");

            PreparedStatement checkStmt = conn.prepareStatement(checkSql.toString());
            int paramIndex = 1;
            for (Book book : books) {
                checkStmt.setString(paramIndex++, book.getCategory());
                checkStmt.setString(paramIndex++, book.getTitle());
                checkStmt.setString(paramIndex++, book.getPress());
                checkStmt.setInt(paramIndex++, book.getPublishYear());
                checkStmt.setString(paramIndex++, book.getAuthor());
            }

            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                rollback(conn);
                return new ApiResult(false, "Duplicate books found");
            }

            // bulk insert books
            String insertSql = "INSERT INTO book (category, title, press, publish_year, author, price, stock) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);

            for (Book book : books) {
                insertStmt.setString(1, book.getCategory());
                insertStmt.setString(2, book.getTitle());
                insertStmt.setString(3, book.getPress());
                insertStmt.setInt(4, book.getPublishYear());
                insertStmt.setString(5, book.getAuthor());
                insertStmt.setDouble(6, book.getPrice());
                insertStmt.setInt(7, book.getStock());
                insertStmt.addBatch();
            }

            insertStmt.executeBatch();

            ResultSet generatedKeys = insertStmt.getGeneratedKeys();
            int i = 0;
            while (generatedKeys.next()) {
                books.get(i++).setBookId(generatedKeys.getInt(1));
            }

            commit(conn);
            return new ApiResult(true, null);

        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = connector.getConn();
        try {
            // check if the book exists
            String checkBookSql = "SELECT book_id FROM book WHERE book_id = ?";
            PreparedStatement checkBookStmt = conn.prepareStatement(checkBookSql);
            checkBookStmt.setInt(1, bookId);
            ResultSet bookRs = checkBookStmt.executeQuery();
            if (!bookRs.next()) {
                rollback(conn);
                return new ApiResult(false, "Book not found");
            }

            // check if the book has unreturned records
            String checkBorrowSql = "SELECT COUNT(*) FROM borrow WHERE book_id = ? AND return_time = 0";
            PreparedStatement checkBorrowStmt = conn.prepareStatement(checkBorrowSql);
            checkBorrowStmt.setInt(1, bookId);
            ResultSet borrowRs = checkBorrowStmt.executeQuery();
            if (borrowRs.next() && borrowRs.getInt(1) > 0) {
                rollback(conn);
                return new ApiResult(false, "Book has unreturned records");
            }

            // remove the book
            String deleteBookSql = "DELETE FROM book WHERE book_id = ?";
            PreparedStatement deleteBookStmt = conn.prepareStatement(deleteBookSql);
            deleteBookStmt.setInt(1, bookId);
            deleteBookStmt.executeUpdate();

            commit(conn);
            return new ApiResult(true, null);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        try {
            // 检查图书是否存在
            String checkExistSql = "SELECT stock FROM book WHERE book_id = ? FOR UPDATE";
            PreparedStatement checkExistStmt = conn.prepareStatement(checkExistSql);
            checkExistStmt.setInt(1, book.getBookId());
            ResultSet existRs = checkExistStmt.executeQuery();
            if (!existRs.next()) {
                rollback(conn);
                return new ApiResult(false, "Book not found");
            }

            // 检查修改后的信息是否与其他图书重复
            String checkDupSql = "SELECT count(*) FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ? AND book_id != ?";
            PreparedStatement checkDupStmt = conn.prepareStatement(checkDupSql);
            checkDupStmt.setString(1, book.getCategory());
            checkDupStmt.setString(2, book.getTitle());
            checkDupStmt.setString(3, book.getPress());
            checkDupStmt.setInt(4, book.getPublishYear());
            checkDupStmt.setString(5, book.getAuthor());
            checkDupStmt.setInt(6, book.getBookId());
            ResultSet dupRs = checkDupStmt.executeQuery();
            if (dupRs.next() && dupRs.getInt(1) > 0) {
                rollback(conn);
                return new ApiResult(false, "Book with same info already exists");
            }

            // 更新图书信息(不修改book_id和stock)
            String updateSql = "UPDATE book SET category = ?, title = ?, press = ?, publish_year = ?, author = ?, price = ? WHERE book_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, book.getCategory());
            updateStmt.setString(2, book.getTitle());
            updateStmt.setString(3, book.getPress());
            updateStmt.setInt(4, book.getPublishYear());
            updateStmt.setString(5, book.getAuthor());
            updateStmt.setDouble(6, book.getPrice());
            updateStmt.setInt(7, book.getBookId());
            updateStmt.executeUpdate();

            commit(conn);
            return new ApiResult(true, null);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
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
                    case STOCK:
                        sql.append("stock");
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
        Connection conn = connector.getConn();
        try {
            String checkBookSql = "SELECT stock FROM book WHERE book_id = ? FOR UPDATE";
            PreparedStatement checkBookStmt = conn.prepareStatement(checkBookSql);
            checkBookStmt.setInt(1, borrow.getBookId());
            ResultSet bookRs = checkBookStmt.executeQuery();
            if (!bookRs.next()) {
                rollback(conn);
                return new ApiResult(false, "Book not found");
            }
            int stock = bookRs.getInt("stock");
            if (stock <= 0) {
                rollback(conn);
                return new ApiResult(false, "Book out of stock");
            }

            String checkCardSql = "SELECT card_id FROM card WHERE card_id = ?";
            PreparedStatement checkCardStmt = conn.prepareStatement(checkCardSql);
            checkCardStmt.setInt(1, borrow.getCardId());
            ResultSet cardRs = checkCardStmt.executeQuery();
            if (!cardRs.next()) {
                rollback(conn);
                return new ApiResult(false, "Card not found");
            }

            // check if the book has been borrowed
            String checkBorrowSql = "SELECT return_time FROM borrow WHERE card_id = ? AND book_id = ? AND return_time = 0";
            PreparedStatement checkBorrowStmt = conn.prepareStatement(checkBorrowSql);
            checkBorrowStmt.setInt(1, borrow.getCardId());
            checkBorrowStmt.setInt(2, borrow.getBookId());
            ResultSet borrowRs = checkBorrowStmt.executeQuery();
            if (borrowRs.next()) {
                rollback(conn);
                return new ApiResult(false, "Book already borrowed");
            }

            // insert the borrow record
            String insertSql = "INSERT INTO borrow (card_id, book_id, borrow_time, return_time) VALUES (?, ?, ?, 0)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, borrow.getCardId());
            insertStmt.setInt(2, borrow.getBookId());
            insertStmt.setLong(3, borrow.getBorrowTime());
            insertStmt.executeUpdate();

            // update the stock
            String updateStockSql = "UPDATE book SET stock = stock - 1 WHERE book_id = ?";
            PreparedStatement updateStockStmt = conn.prepareStatement(updateStockSql);
            updateStockStmt.setInt(1, borrow.getBookId());
            updateStockStmt.executeUpdate();

            commit(conn);
            return new ApiResult(true, null);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            // check if the borrow record exists
            String checkBorrowSql = "SELECT borrow_time FROM borrow WHERE card_id = ? AND book_id = ? AND return_time = 0";
            PreparedStatement checkBorrowStmt = conn.prepareStatement(checkBorrowSql);
            checkBorrowStmt.setInt(1, borrow.getCardId());
            checkBorrowStmt.setInt(2, borrow.getBookId());
            ResultSet borrowRs = checkBorrowStmt.executeQuery();
            if (!borrowRs.next()) {
                rollback(conn);
                return new ApiResult(false, "Borrow record not found");
            }

            // check if the return time is valid
            long borrowTime = borrowRs.getLong("borrow_time");
            if (borrow.getReturnTime() <= borrowTime) {
                rollback(conn);
                return new ApiResult(false, "Invalid return time");
            }

            // update the borrow record
            String updateBorrowSql = "UPDATE borrow SET return_time = ? WHERE card_id = ? AND book_id = ? AND borrow_time = ?";
            PreparedStatement updateBorrowStmt = conn.prepareStatement(updateBorrowSql);
            updateBorrowStmt.setLong(1, borrow.getReturnTime());
            updateBorrowStmt.setInt(2, borrow.getCardId());
            updateBorrowStmt.setInt(3, borrow.getBookId());
            updateBorrowStmt.setLong(4, borrowTime);
            updateBorrowStmt.executeUpdate();

            // update the stock
            String updateStockSql = "UPDATE book SET stock = stock + 1 WHERE book_id = ?";
            PreparedStatement updateStockStmt = conn.prepareStatement(updateStockSql);
            updateStockStmt.setInt(1, borrow.getBookId());
            updateStockStmt.executeUpdate();

            commit(conn);
            return new ApiResult(true, null);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        try {
            String sql = "SELECT b.*, bk.category, bk.title, bk.press, bk.publish_year, " +
                        "bk.author, bk.price, bk.stock " +
                        "FROM borrow b " +
                        "JOIN book bk ON b.book_id = bk.book_id " +
                        "WHERE b.card_id = ? " +
                        "ORDER BY b.borrow_time DESC, b.book_id ASC";
            
            try (PreparedStatement stmt = connector.getConn().prepareStatement(sql)) {
                stmt.setInt(1, cardId);
                ResultSet rs = stmt.executeQuery();
                
                List<BorrowHistories.Item> items = new ArrayList<>();
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
                    
                    Borrow borrow = new Borrow();
                    borrow.setCardId(cardId);
                    borrow.setBookId(rs.getInt("book_id"));
                    
                    borrow.setBorrowTime(rs.getLong("borrow_time"));
                    
                    long returnTime = rs.getLong("return_time");
                    if (!rs.wasNull()) {
                        borrow.setReturnTime(returnTime);
                    }
                    
                    BorrowHistories.Item item = new BorrowHistories.Item(cardId, book, borrow);
                    items.add(item);
                }
                
                BorrowHistories histories = new BorrowHistories(items);
                return new ApiResult(true, histories);
            }
        } catch (Exception e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        try {
            // check if the card already exists
            String checkSql = "SELECT count(*) FROM card WHERE name = ? AND department = ? AND type = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, card.getName());
            checkStmt.setString(2, card.getDepartment());
            checkStmt.setString(3, card.getType().name());
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next() && checkRs.getInt(1) > 0) {
                rollback(conn);
                return new ApiResult(false, "Card already exists");
            }

            // insert the new card
            String insertSql = "INSERT INTO card (name, department, type) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, card.getName());
            insertStmt.setString(2, card.getDepartment());
            insertStmt.setString(3, card.getType().name());
            insertStmt.executeUpdate();

            ResultSet generatedKeys = insertStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                card.setCardId(generatedKeys.getInt(1));
            }

            commit(conn);
            return new ApiResult(true, null);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        try {
            // check if the card has unreturned books

            String checkBorrowSql = "SELECT count(*) FROM borrow WHERE card_id = ? AND return_time = 0";
            PreparedStatement checkBorrowStmt = conn.prepareStatement(checkBorrowSql);
            checkBorrowStmt.setInt(1, cardId);
            ResultSet borrowRs = checkBorrowStmt.executeQuery();
            
            if (borrowRs.next() && borrowRs.getInt(1) > 0) {
                rollback(conn);
                return new ApiResult(false, "Card has unreturned books");
            }

            // remove the card
            String deleteSql = "DELETE FROM card WHERE card_id = ?";
            PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
            deleteStmt.setInt(1, cardId);
            int affectedRows = deleteStmt.executeUpdate();
            
            if (affectedRows == 0) {
                rollback(conn);
                return new ApiResult(false, "Card not found");
            }

            commit(conn);
            return new ApiResult(true, null);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult showCards() {
        try {
            // query all cards and sort by card_id
            String sql = "SELECT card_id, name, department, type FROM card ORDER BY card_id ASC";
            PreparedStatement stmt = connector.getConn().prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            List<Card> cards = new ArrayList<>();
            while (rs.next()) {
                Card card = new Card();
                card.setCardId(rs.getInt("card_id"));
                card.setName(rs.getString("name"));
                card.setDepartment(rs.getString("department"));
                card.setType(Card.CardType.valueOf(rs.getString("type")));
                cards.add(card);
            }

            return new ApiResult(true, new CardList(cards));
        } catch (Exception e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    
    @Override
    public ApiResult modifyCardInfo(Card card) {
        Connection conn = connector.getConn();
        try {
            String checkSql = "SELECT count(*) FROM card WHERE card_id = ? FOR UPDATE";
            PreparedStatement checkExistStmt = conn.prepareStatement(checkSql);
            checkExistStmt.setInt(1, card.getCardId());
            ResultSet checkRs = checkExistStmt.executeQuery();
            if (!checkRs.next()) {
                rollback(conn);
                return new ApiResult(false, "Card not found");
            }

            // check if duplicated
            String checkDupSql = "SELECT count(*) FROM card WHERE name = ? AND department = ? AND type = ?";
            PreparedStatement checkDupStmt = conn.prepareStatement(checkDupSql);
            checkDupStmt.setString(1, card.getName());
            checkDupStmt.setString(2, card.getDepartment());
            checkDupStmt.setString(3, card.getType().name());
            ResultSet dupRs = checkDupStmt.executeQuery();
            if (dupRs.next() && dupRs.getInt(1) > 0) {
                rollback(conn);
                return new ApiResult(false, "Card with same info already exists");
            }

            // update card info
            String updateSql = "UPDATE card SET name = ?, department = ?, type = ? WHERE card_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, card.getName());
            updateStmt.setString(2, card.getDepartment());
            updateStmt.setString(3, card.getType().name());
            updateStmt.setInt(4, card.getCardId());
            updateStmt.executeUpdate();

            commit(conn);
            return new ApiResult(true, null);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
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
