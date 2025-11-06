class Book
{	 
	//book attributes
	int BookId= 1;
	String Title;
	String Author;
	double Price;
	int Quantity = 1;
	boolean Issued = false;
	Integer IssuedToMemberId = null; // null when available

	//constructor
	Book(int BookId,String Title,String Author,double Price)
	{
		this.BookId=BookId;
		this.Title=Title;
		this.Author=Author;
		this.Price=Price;
	}

	//constructor with quantity
	Book(int BookId,String Title,String Author,double Price,int Quantity)
	{
		this(BookId, Title, Author, Price);
		this.Quantity = Quantity;
	}

	boolean isAvailable()
	{
		return Quantity > 0;
	}

	void markIssuedTo(int memberId)
	{
		this.Issued = true;
		this.IssuedToMemberId = memberId;
	}

	void markReturned()
	{
		this.Issued = false;
		this.IssuedToMemberId = null;
	}

	@Override
	public String toString()
	{
		String status = (Quantity > 0 ? ("Available (" + Quantity + ")") : "Out of stock");
		return "Book ID: " + BookId + ", Title: " + Title + ", Author: " + Author + ", Price: " + Price + ", Qty: " + Quantity + ", Status: " + status;
	}
}