import java.util.ArrayList;
import java.util.Scanner;
import java.sql.*;

public class admin 
{
    Scanner sc = new Scanner(System.in);
    String Adminname;
    int AdminId;
    String password = "admin";
     
    //admin constructor
    admin(String name, int id)
    {
        this.Adminname = name;
        this.AdminId = id;
    }

    //display all books in library
    public void showBooks() 
    {
        ArrayList<Book> books = getBooks();
        if (books.isEmpty()) 
        {
            System.out.println("No books available.");
        } 
        else 
        {
            for (Book i : books) 
            {
                System.out.println(i.toString());
            }
        }
    }
    

    public void addbook(int bookidnum, String booktitle, String bookAuthor, double price)
    {
        addbook(bookidnum, booktitle, bookAuthor, price, 1);
    }

    //add a book by id with quantity
    public void addbook(int bookidnum, String booktitle, String bookAuthor, double price, int quantity)
    {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO books (book_id, title, author, price, quantity, issued, issued_to_member_id) VALUES (?, ?, ?, ?, ?, FALSE, NULL)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, bookidnum);
                pstmt.setString(2, booktitle);
                pstmt.setString(3, bookAuthor);
                pstmt.setDouble(4, price);
                pstmt.setInt(5, Math.max(0, quantity));
                pstmt.executeUpdate();
                System.out.println("BOOK ADDED!");
                return;
            }
        } catch (SQLException e) {
            // Handle duplicate cases gracefully: either same book_id or same (title, author)
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            boolean isDuplicate = "23000".equals(e.getSQLState()) || msg.contains("duplicate") || msg.contains("unique") || msg.contains("uq_books_title_author");
            if (!isDuplicate) {
                System.err.println("Error adding book: " + e.getMessage());
                return;
            }

            // Try to increase by ID first
            try (Connection conn2 = DatabaseConnection.getConnection()) {
                try (Statement useStmt = conn2.createStatement()) { useStmt.executeUpdate("USE library_db"); }

                String incById = "UPDATE books SET quantity = quantity + ?, price = ? WHERE book_id = ?";
                try (PreparedStatement ps = conn2.prepareStatement(incById)) {
                    ps.setInt(1, Math.max(0, quantity));
                    ps.setDouble(2, price);
                    ps.setInt(3, bookidnum);
                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        System.out.println("BOOK EXISTS. QUANTITY INCREASED for ID: " + bookidnum);
                        return;
                    }
                }

                // Fall back to title+author upsert (unique on title, author)
                String incByTitleAuthor = "UPDATE books SET quantity = quantity + ?, price = ? WHERE title = ? AND author = ?";
                try (PreparedStatement ps = conn2.prepareStatement(incByTitleAuthor)) {
                    ps.setInt(1, Math.max(0, quantity));
                    ps.setDouble(2, price);
                    ps.setString(3, booktitle);
                    ps.setString(4, bookAuthor);
                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        System.out.println("BOOK EXISTS (same title & author). QUANTITY INCREASED.");
                        return;
                    }
                }

                // If neither path updated, report error
                System.err.println("Duplicate detected but could not increase quantity. Check data.");
            } catch (SQLException ex) {
                System.err.println("Error handling duplicate book add: " + ex.getMessage());
            }
        }
    }

    // add a book without specifying ID (auto-increment or increment quantity if exists); returns book ID or -1
    public int addbook(String booktitle, String bookAuthor, double price, int quantity)
    {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement useStmt = conn.createStatement()) {
                useStmt.executeUpdate("USE library_db");
            } catch (SQLException ignore) {}

            // First try to update existing by title+author
            String updateSql = "UPDATE books SET quantity = quantity + ?, price = ? WHERE title = ? AND author = ?";
            int updated = 0;
            try (PreparedStatement up = conn.prepareStatement(updateSql)) {
                up.setInt(1, Math.max(0, quantity));
                up.setDouble(2, price);
                up.setString(3, booktitle);
                up.setString(4, bookAuthor);
                updated = up.executeUpdate();
            }

            if (updated > 0) {
                // Fetch existing ID
                String findSql = "SELECT book_id FROM books WHERE title = ? AND author = ?";
                try (PreparedStatement find = conn.prepareStatement(findSql)) {
                    find.setString(1, booktitle);
                    find.setString(2, bookAuthor);
                    try (ResultSet rs = find.executeQuery()) {
                        if (rs.next()) {
                            int id = rs.getInt(1);
                            conn.commit();
                            System.out.println("BOOK QUANTITY INCREASED. ID: " + id);
                            return id;
                        }
                    }
                }
                conn.commit();
                return -1;
            }

            // Not existing, insert new
            String insertSql = "INSERT INTO books (title, author, price, quantity, issued, issued_to_member_id) VALUES (?, ?, ?, ?, FALSE, NULL)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, booktitle);
                pstmt.setString(2, bookAuthor);
                pstmt.setDouble(3, price);
                pstmt.setInt(4, Math.max(0, quantity));
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int newId = keys.getInt(1);
                            conn.commit();
                            System.out.println("BOOK ADDED! ID: " + newId);
                            return newId;
                        }
                    }
                }
            }

            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error adding book (auto-id/upsert): " + e.getMessage());
        }
        return -1;
    }

    //remove a book by id
    public void removebook() 
    {
        System.out.println("ENTER BOOK ID TO REMOVE:");
        int bookidtoremove = sc.nextInt();
        removebookById(bookidtoremove);
    }
    
    public void removebookById(int bookId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM books WHERE book_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, bookId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                System.out.println("Book removed successfully!");
                } else {
                    System.out.println("BOOK NOT FOUND!");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error removing book: " + e.getMessage());
        }
    }

    // increment quantity for existing book by ID; returns true if updated
    public boolean increaseBookQuantityById(int bookId, int addQuantity) {
        if (addQuantity <= 0) return false;
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }
            String sql = "UPDATE books SET quantity = quantity + ? WHERE book_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, addQuantity);
                pstmt.setInt(2, bookId);
                int rows = pstmt.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error increasing book quantity: " + e.getMessage());
            return false;
        }
    }
    
    // Overloaded method for backward compatibility (with ID)
    public void AddMember(int Id, String name)
    {
        AddMember(name, Id);
    }
    
    // Main method - adds member with auto-increment ID
    public int AddMember(String name)
    {
        return AddMember(name, -1);
    }
    
    // Internal method - adds member (ID is optional, -1 means auto-increment)
    private int AddMember(String name, int id)
    {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Ensure we're using the correct database
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }
            
            int generatedId = -1;
            
            if (id > 0) {
                // Manual ID specified
                String defaultPassword = "member" + (System.currentTimeMillis() % 1000);
                String sql = "INSERT INTO members (member_id, member_name, password) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, id);
                    pstmt.setString(2, name);
                    pstmt.setString(3, defaultPassword);
                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        generatedId = id;
                        System.out.println("✓ Member added successfully! ID: " + id + ", Name: " + name);
                    }
                }
            } else {
                // Try auto-increment first
                try {
                // Generate default password: member + first 3 digits of timestamp
                String defaultPassword = "member" + (System.currentTimeMillis() % 1000);
                String sql = "INSERT INTO members (member_name, password) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, defaultPassword);
                    int rowsAffected = pstmt.executeUpdate();
                        if (rowsAffected > 0) {
                            // Get the generated ID
                            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                                if (generatedKeys.next()) {
                                    generatedId = generatedKeys.getInt(1);
                                    System.out.println("✓ Member added successfully! Auto-generated ID: " + generatedId + ", Name: " + name);
                                } else {
                                    System.err.println("Warning: No generated keys returned, but insert succeeded");
                                }
                            }
                        }
                    }
                } catch (SQLException autoIncError) {
                    // If auto-increment fails, try to get max ID and add 1
                    System.err.println("Auto-increment failed: " + autoIncError.getMessage());
                    System.out.println("Falling back to manual ID generation...");
                    
                    try {
                        // Get the maximum ID and add 1
                        String maxIdSql = "SELECT COALESCE(MAX(member_id), 0) + 1 AS next_id FROM members";
                        try (Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(maxIdSql)) {
                            if (rs.next()) {
                                generatedId = rs.getInt("next_id");
                                
                                // Generate default password
                                String defaultPassword = "member" + (System.currentTimeMillis() % 1000);
                                String sql = "INSERT INTO members (member_id, member_name, password) VALUES (?, ?, ?)";
                                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                                    pstmt.setInt(1, generatedId);
                                    pstmt.setString(2, name);
                                    pstmt.setString(3, defaultPassword);
                                    int rowsAffected = pstmt.executeUpdate();
                                    if (rowsAffected > 0) {
                                        System.out.println("✓ Member added successfully! Generated ID: " + generatedId + ", Name: " + name);
                                    } else {
                                        generatedId = -1;
                                    }
                                }
                            }
                        }
                    } catch (SQLException fallbackError) {
                        System.err.println("Fallback ID generation also failed: " + fallbackError.getMessage());
                        throw fallbackError;
                    }
                }
            }
            
            return generatedId;
        } catch (SQLException e) {
            System.err.println("Error adding member: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            return -1;
        }
    } 

    public void removemember(int Id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM members WHERE member_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, Id);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Member removed successfully!");
                } else {
    System.out.println("Member not found!");
}
            }
        } catch (SQLException e) {
            System.err.println("Error removing member: " + e.getMessage());
        }
    }
    
    public void showmembers()
    {
        ArrayList<member> members = getMembers();
        if(members.isEmpty())
        {
            System.out.println("THERE ARE NO MEMBERS IN THE LIBRARY!");
        }
        else
        {
            for(member i : members)
            {
                System.out.println("memberid:" + i.MemberId + " MEMBER name "+ i.Membername );
            }
        }   
    }

	// search by title
	public ArrayList<Book> searchBooksByTitle(String query)
	{
		ArrayList<Book> result = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return result;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }
            String sql = "SELECT * FROM books WHERE LOWER(title) LIKE ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "%" + query.toLowerCase() + "%");
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        result.add(createBookFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching books by title: " + e.getMessage());
		}
		return result;
	}

	// search by author
	public ArrayList<Book> searchBooksByAuthor(String query)
	{
		ArrayList<Book> result = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return result;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }
            String sql = "SELECT * FROM books WHERE LOWER(author) LIKE ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "%" + query.toLowerCase() + "%");
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        result.add(createBookFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching books by author: " + e.getMessage());
		}
		return result;
	}

	// issue book
	public boolean issueBookToMember(int bookId, int memberId)
	{
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }

            // Check availability
            String lockSql = "SELECT quantity FROM books WHERE book_id = ? FOR UPDATE";
            int qty = 0;
            try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                ps.setInt(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return false; }
                    qty = rs.getInt(1);
                }
            }
            if (qty <= 0) { conn.rollback(); return false; }

            // Check member exists
            String memberSql = "SELECT 1 FROM members WHERE member_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(memberSql)) {
                ps.setInt(1, memberId);
                try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) { conn.rollback(); return false; } }
            }

            // Create loan
            String loanSql = "INSERT INTO book_loans (book_id, member_id) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(loanSql)) {
                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                ps.executeUpdate();
            }

            // Decrement quantity
            String decSql = "UPDATE books SET quantity = quantity - 1 WHERE book_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(decSql)) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error issuing book: " + e.getMessage());
            return false;
        }
	}

	// return book
	public boolean returnBookFromMember(int bookId, int memberId)
	{
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }

            // Find one active loan for this book/member
            String findLoan = "SELECT loan_id FROM book_loans WHERE book_id = ? AND member_id = ? AND returned_at IS NULL LIMIT 1 FOR UPDATE";
            Integer loanId = null;
            try (PreparedStatement ps = conn.prepareStatement(findLoan)) {
                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) loanId = rs.getInt(1);
                }
            }
            if (loanId == null) { conn.rollback(); return false; }

            // Close loan
            String closeSql = "UPDATE book_loans SET returned_at = CURRENT_TIMESTAMP WHERE loan_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(closeSql)) {
                ps.setInt(1, loanId);
                ps.executeUpdate();
            }

            // Increment quantity
            String incSql = "UPDATE books SET quantity = quantity + 1 WHERE book_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(incSql)) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error returning book: " + e.getMessage());
            return false;
        }
	}

	public member getMemberById(int id)
	{
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM members WHERE member_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return createMemberFromResultSet(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting member by ID: " + e.getMessage());
		}
		return null;
	}

	public ArrayList<Book> getBooks()
	{
        ArrayList<Book> books = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }
            String sql = "SELECT * FROM books";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    books.add(createBookFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting books: " + e.getMessage());
        }
		return books;
	}

    public ArrayList<Book> getIssuedBooks()
    {
        // Deprecated with loans table; keeping method but return empty to avoid confusion
        return new ArrayList<>();
    }

    // Loan view record
    public static class LoanRecord {
        public final int bookId;
        public final String title;
        public final String author;
        public final int memberId;
        public final String memberName;
        public final int borrowedQty;
        public LoanRecord(int bookId, String title, String author, int memberId, String memberName, int borrowedQty) {
            this.bookId = bookId; this.title = title; this.author = author; this.memberId = memberId; this.memberName = memberName; this.borrowedQty = borrowedQty;
        }
    }

    public ArrayList<LoanRecord> getActiveLoans()
    {
        ArrayList<LoanRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) { stmt.executeUpdate("USE library_db"); }
            String sql = "SELECT b.book_id, b.title, b.author, m.member_id, m.member_name, COUNT(*) AS qty " +
                         "FROM book_loans l " +
                         "JOIN books b ON b.book_id = l.book_id " +
                         "JOIN members m ON m.member_id = l.member_id " +
                         "WHERE l.returned_at IS NULL " +
                         "GROUP BY b.book_id, b.title, b.author, m.member_id, m.member_name " +
                         "ORDER BY b.title, m.member_name";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new LoanRecord(
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5), rs.getInt(6)
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting active loans: " + e.getMessage());
        }
        return list;
    }

	public ArrayList<member> getMembers()
	{
        ArrayList<member> members = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Ensure we're using the correct database
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }
            
            String sql = "SELECT * FROM members ORDER BY member_id";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        member m = createMemberFromResultSet(rs);
                        members.add(m);
                    } catch (Exception e) {
                        System.err.println("Error creating member from row: " + e.getMessage());
                    }
                }
                System.out.println("✓ Loaded " + members.size() + " members from database");
            }
        } catch (SQLException e) {
            System.err.println("Error getting members: " + e.getMessage());
            e.printStackTrace();
        }
        return members;
	}

	public ArrayList<BookRequest> getRequests()
	{
        ArrayList<BookRequest> requests = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM book_requests";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(createRequestFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting requests: " + e.getMessage());
        }
		return requests;
	}

    public BookRequest submitRequest(int memberId, int bookId, String title)
	{
		member m = getMemberById(memberId);
		if (m == null) return null;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO book_requests (member_id, member_name, book_id, book_title, quantity, status) VALUES (?, ?, ?, ?, 1, 'PENDING')";
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, memberId);
                pstmt.setString(2, m.Membername);
                pstmt.setInt(3, bookId);
                pstmt.setString(4, title);
                pstmt.executeUpdate();
                
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int requestId = generatedKeys.getInt(1);
                        return new BookRequest(requestId, memberId, m.Membername, bookId, title);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error submitting request: " + e.getMessage());
        }
        return null;
	}

    public boolean approveRequest(int requestId)
	{
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Get request details
                String getRequestSql = "SELECT * FROM book_requests WHERE request_id = ? AND status = 'PENDING'";
                BookRequest req = null;
                try (PreparedStatement pstmt = conn.prepareStatement(getRequestSql)) {
                    pstmt.setInt(1, requestId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            req = createRequestFromResultSet(rs);
                        }
                    }
                }
                
                if (req == null) {
                    conn.rollback();
                    return false;
                }
                
                // Check availability for requested quantity
                String checkBookSql = "SELECT quantity FROM books WHERE book_id = ? FOR UPDATE";
                int availableQty = 0;
                try (PreparedStatement pstmt = conn.prepareStatement(checkBookSql)) {
                    pstmt.setInt(1, req.bookId);
                    try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) availableQty = rs.getInt(1); }
                }
                
                int requestedQty = (req.quantity <= 0 ? 1 : req.quantity);
                int toIssue = Math.min(availableQty, requestedQty);
                if (toIssue <= 0) {
                    // Nothing available to issue now; keep as PENDING and return false
                    conn.rollback();
                    return false;
                }
                
                // Check if member exists
                String checkMemberSql = "SELECT * FROM members WHERE member_id = ?";
                boolean memberExists = false;
                try (PreparedStatement pstmt = conn.prepareStatement(checkMemberSql)) {
                    pstmt.setInt(1, req.memberId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        memberExists = rs.next();
                    }
                }
                
                if (!memberExists) {
                    conn.rollback();
		return false;
                }
                
                // Update book: decrement by toIssue quantity
                String updateBookSql = "UPDATE books SET quantity = quantity - ? WHERE book_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateBookSql)) {
                    pstmt.setInt(1, toIssue);
                    pstmt.setInt(2, req.bookId);
                    pstmt.executeUpdate();
                }
                // Create that many loan rows
                String loanSql = "INSERT INTO book_loans (book_id, member_id) VALUES (?, ?)";
                try (PreparedStatement lp = conn.prepareStatement(loanSql)) {
                    for (int i = 0; i < toIssue; i++) {
                        lp.setInt(1, req.bookId);
                        lp.setInt(2, req.memberId);
                        lp.addBatch();
                    }
                    lp.executeBatch();
                }
                
                // Update request status to APPROVED
                String updateStatusSql = "UPDATE book_requests SET status = 'APPROVED' WHERE request_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateStatusSql)) {
                    pstmt.setInt(1, requestId);
                    pstmt.executeUpdate();
                }
                
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error approving request: " + e.getMessage());
            return false;
        }
	}

	public boolean rejectRequest(int requestId)
	{
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE book_requests SET status = 'REJECTED' WHERE request_id = ? AND status = 'PENDING'";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, requestId);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error rejecting request: " + e.getMessage());
            return false;
        }
    }

    // Clear all APPROVED or REJECTED requests for a member; returns number deleted
    public int clearResolvedRequestsForMember(int memberId)
    {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement useStmt = conn.createStatement()) { useStmt.executeUpdate("USE library_db"); }
            String sql = "DELETE FROM book_requests WHERE member_id = ? AND status IN ('APPROVED','REJECTED')";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, memberId);
                return pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error clearing resolved requests: " + e.getMessage());
            return 0;
        }
    }

    // Clear all APPROVED or REJECTED requests globally; returns number deleted
    public int clearAllResolvedRequests()
    {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement useStmt = conn.createStatement()) { useStmt.executeUpdate("USE library_db"); }
            String sql = "DELETE FROM book_requests WHERE status IN ('APPROVED','REJECTED')";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                return pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error clearing all resolved requests: " + e.getMessage());
            return 0;
        }
    }
    
    // Helper method to create Book from ResultSet
    private Book createBookFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("book_id");
        String title = rs.getString("title");
        String author = rs.getString("author");
        double price = rs.getDouble("price");
        int quantity = 1;
        try {
            quantity = rs.getInt("quantity");
        } catch (SQLException ignore) { }

        Book book = new Book(id, title, author, price, quantity);
        
        boolean issued = false;
        Integer issuedToMemberId = null;
        try {
            issued = rs.getBoolean("issued");
            issuedToMemberId = rs.getObject("issued_to_member_id") != null ? rs.getInt("issued_to_member_id") : null;
        } catch (SQLException ignore) { }
        
        if (issued && issuedToMemberId != null) {
            // Do not change quantity here; DB already holds correct quantity
            book.Issued = true;
            book.IssuedToMemberId = issuedToMemberId;
        }
        
        return book;
    }
    
    // Helper method to create member from ResultSet
    private member createMemberFromResultSet(ResultSet rs) throws SQLException {
        int memberId = rs.getInt("member_id");
        String memberName = rs.getString("member_name");
        String password = rs.getString("password");
        
        if (memberName == null || memberName.trim().isEmpty()) {
            throw new SQLException("Member name is null or empty for member_id: " + memberId);
        }
        
        member m = new member(memberId, memberName);
        // Store password in member object for later use
        if (password != null) {
            m.password = password;
        }
        
        // Load borrowed books for this member using active loans
        String sql = "SELECT b.* FROM book_loans l JOIN books b ON b.book_id = l.book_id WHERE l.member_id = ? AND l.returned_at IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement useStmt = conn.createStatement()) {
            useStmt.executeUpdate("USE library_db");
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, m.MemberId);
                try (ResultSet bookRs = pstmt.executeQuery()) {
                    while (bookRs.next()) {
                        Book b = createBookFromResultSet(bookRs);
                        m.recievebook(b);
                    }
                }
            }
        }
        
        return m;
    }
    
    // Helper method to create BookRequest from ResultSet
    private BookRequest createRequestFromResultSet(ResultSet rs) throws SQLException {
        int qty = 1;
        try { qty = rs.getInt("quantity"); } catch (SQLException ignore) { }
        return new BookRequest(
            rs.getInt("request_id"),
            rs.getInt("member_id"),
            rs.getString("member_name"),
            rs.getInt("book_id"),
            rs.getString("book_title"),
            qty,
            rs.getString("status")
        );
    }
    
    // Verify admin login credentials
    public boolean verifyAdminLogin(String username, String password) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }
            
            String sql = "SELECT password FROM admins WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String dbPassword = rs.getString("password");
                        return password.equals(dbPassword);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error verifying admin login: " + e.getMessage());
            // Fallback to hardcoded check if table doesn't exist yet
            if (username.equalsIgnoreCase("admin") && password.equals("admin")) {
				return true;
			}
		}
		return false;
	}
    
    // Verify member login credentials
    public boolean verifyMemberLogin(int memberId, String password) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }
            
            String sql = "SELECT password FROM members WHERE member_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, memberId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String dbPassword = rs.getString("password");
                        if (dbPassword == null) {
                            // If no password set, use default
                            dbPassword = "member";
                        }
                        return password.equals(dbPassword);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error verifying member login: " + e.getMessage());
        }
        return false;
    }
    
    // Change admin password
    public boolean changeAdminPassword(String username, String currentPassword, String newPassword) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }
            
            // First verify current password
            if (!verifyAdminLogin(username, currentPassword)) {
                System.err.println("Current password is incorrect");
                return false;
            }
            
            // Update password
            String sql = "UPDATE admins SET password = ? WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newPassword);
                pstmt.setString(2, username);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("✓ Admin password changed successfully for: " + username);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error changing admin password: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // Change member password (by admin - no current password required)
    public boolean changeMemberPasswordByAdmin(int memberId, String newPassword) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }
            
            // Check if member exists
            String checkSql = "SELECT COUNT(*) FROM members WHERE member_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, memberId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) == 0) {
                        System.err.println("Member not found: " + memberId);
                        return false;
                    }
                }
            }
            
            // Update password
            String sql = "UPDATE members SET password = ? WHERE member_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newPassword);
                pstmt.setInt(2, memberId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("✓ Member password changed successfully for member ID: " + memberId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error changing member password: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // Change member password (by member - requires current password)
    public boolean changeMemberPassword(int memberId, String currentPassword, String newPassword) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("USE library_db");
            }
            
            // First verify current password
            if (!verifyMemberLogin(memberId, currentPassword)) {
                System.err.println("Current password is incorrect");
                return false;
            }
            
            // Update password
            String sql = "UPDATE members SET password = ? WHERE member_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newPassword);
                pstmt.setInt(2, memberId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("✓ Member password changed successfully for member ID: " + memberId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error changing member password: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // Get current admin username (helper for UI)
    public String getCurrentAdminUsername() {
        // For now, return default admin - can be enhanced later
        return "admin";
    }
}