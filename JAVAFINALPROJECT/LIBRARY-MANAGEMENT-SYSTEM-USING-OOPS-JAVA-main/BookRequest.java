public class BookRequest {
    int requestId;
    int memberId;
    String memberName;
    int bookId;
    String bookTitle;
    int quantity;
    String status; // PENDING, APPROVED, REJECTED

    BookRequest(int requestId, int memberId, String memberName, int bookId, String bookTitle) {
        this.requestId = requestId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.quantity = 1;
        this.status = "PENDING";
    }
    
    // Constructor with status parameter (used when loading from database)
    BookRequest(int requestId, int memberId, String memberName, int bookId, String bookTitle, int quantity, String status) {
        this.requestId = requestId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.quantity = quantity > 0 ? quantity : 1;
        this.status = status != null ? status : "PENDING";
    }
}



