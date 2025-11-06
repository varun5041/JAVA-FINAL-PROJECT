import java.util.ArrayList;

public class member 
{
    int MemberId;
    String Membername;
    ArrayList<Book> borrowedbooks = new ArrayList<>();
    String password = "member"; // naive default password; in real app, hash & store
    
    member(int MemberId,String Membername)
    {
        this.MemberId=MemberId;
        this.Membername=Membername;
    }

    public void recievebook(Book book)
    {
        if (book == null) return;
        borrowedbooks.add(book);
    }

    public void givebook(Book book){
       if (book == null) return;
       borrowedbooks.remove(book);
    }
}
