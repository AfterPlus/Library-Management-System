import java.util.*;
import java.util.logging.*;

// Logger
class LogManager
{
    static Logger logger = Logger.getLogger("LibraryLogger");
}

// Enum
enum BookStatus
{
    AVAILABLE,
    NOT_AVAILABLE
}

enum Suggestion
{
    FICTION,
    NON_FICTION,
    FANTASY,
    SCIENCE_FICTION,
    MYSTERY,
    THRILLER
}

// ======================= OBSERVER =======================
interface Observer
{
    void update(String message);
}

class PatronObserver implements Observer
{
    String name;

    PatronObserver(String name)
    {
        this.name = name;
    }

    public void update(String message)
    {
        LogManager.logger.info(name + " notified: " + message);
    }
}

// ======================= BOOK =======================
class Book
{
    private String Title;
    private String Author;
    private String ISBN;
    private String YearOfPublication;
    private BookStatus Status;

    List<Observer> observers = new ArrayList<>();

    Book(String Title, String Author, String ISBN, String YearOfPublication)
    {
        this.Title = Title;
        this.Author = Author;
        this.ISBN = ISBN;
        this.YearOfPublication = YearOfPublication;
        this.Status = BookStatus.AVAILABLE;
    }

    String getTitle() { return Title; }
    String getAuthor() { return Author; }
    String getISBN() { return ISBN; }
    BookStatus getStatus() { return Status; }

    void UpdateBookStatus(BookStatus Status)
    {
        this.Status = Status;
        notifyObservers("Status changed to " + Status);
    }

    void addObserver(Observer o)
    {
        observers.add(o);
    }

    void notifyObservers(String message)
    {
        for (Observer o : observers)
        {
            o.update(message);
        }
    }
}

// ======================= FACTORY =======================
class BookFactory
{
    static Book createBook(String title, String author, String isbn, String year)
    {
        return new Book(title, author, isbn, year);
    }
}

// ======================= STRATEGY =======================
interface SearchStrategy
{
    boolean match(Book book);
}

class SearchByTitle implements SearchStrategy
{
    String title;
    SearchByTitle(String title) { this.title = title; }

    public boolean match(Book book)
    {
        return book.getTitle().equalsIgnoreCase(title);
    }
}

class SearchByAuthor implements SearchStrategy
{
    String author;
    SearchByAuthor(String author) { this.author = author; }

    public boolean match(Book book)
    {
        return book.getAuthor().equalsIgnoreCase(author);
    }
}

class SearchByISBN implements SearchStrategy
{
    String isbn;
    SearchByISBN(String isbn) { this.isbn = isbn; }

    public boolean match(Book book)
    {
        return book.getISBN().equals(isbn);
    }
}

// ======================= PATRON =======================
class Patron
{
    String Name;
    List<Book> BorrowHistory = new ArrayList<>();
    List<Book> InHandBooks = new ArrayList<>();

    Patron(String Name)
    {
        this.Name = Name;
    }

    void AddBorrowedBook(Book book)
    {
        InHandBooks.add(book);
        BorrowHistory.add(book);
    }

    void ReturnBook(Book book)
    {
        InHandBooks.remove(book);
    }
}

// ======================= PATRON MANAGER =======================
class PatronManager
{
    Map<String, Patron> Patrons = new HashMap<>();

    void AddPatron(String Name)
    {
        Patrons.put(Name, new Patron(Name));
    }

    Patron FindPatron(String name)
    {
        return Patrons.get(name);
    }
}

// ======================= LIBRARY =======================
class Library
{

    PatronManager PatronManager = new PatronManager();

    Map<String, Book> BookMap = new HashMap<>();
    List<Book> Books = new ArrayList<>();

    void AddBook(String Title, String Author, String ISBN, String Year)
    {
        Book book = BookFactory.createBook(Title, Author, ISBN, Year);
        Books.add(book);
        BookMap.put(ISBN, book);
        LogManager.logger.info("Book added: " + Title);
    }

    Book FindBookByISBN(String isbn)
    {
        return BookMap.get(isbn);
    }

    // STRATEGY SEARCH
    List<Book> SearchBooks(SearchStrategy strategy)
    {
        List<Book> result = new ArrayList<>();
        for (Book book : Books)
        {
            if (strategy.match(book))
            {
                result.add(book);
            }
        }
        return result;
    }

    void BorrowBook(String patronName, String isbn)
    {
        Book book = FindBookByISBN(isbn);
        Patron patron = PatronManager.FindPatron(patronName);

        if (book == null || patron == null)
        {
            LogManager.logger.warning("Invalid borrow attempt");
            return;
        }

        if (book.getStatus() == BookStatus.NOT_AVAILABLE)
        {
            LogManager.logger.warning("Book already borrowed");
            return;
        }

        book.UpdateBookStatus(BookStatus.NOT_AVAILABLE);
        patron.AddBorrowedBook(book);

        book.addObserver(new PatronObserver(patronName));

        LogManager.logger.info(patronName + " borrowed " + book.getTitle());
    }

    void ReturnBook(String patronName, String isbn)
    {
        Book book = FindBookByISBN(isbn);
        Patron patron = PatronManager.FindPatron(patronName);

        if (book == null || patron == null)
        {
            LogManager.logger.warning("Invalid return");
            return;
        }

        if (!patron.InHandBooks.contains(book))
        {
            LogManager.logger.warning("Book not with patron");
            return;
        }

        book.UpdateBookStatus(BookStatus.AVAILABLE);
        patron.ReturnBook(book);

        LogManager.logger.info(book.getTitle() + " returned by " + patronName);
    }
}

// ======================= MAIN =======================
public class Main
{
    public static void main(String[] args)
    {

        Library library = new Library();

        library.AddBook("Game Dev", "Sanjay", "123", "2025");
        library.AddBook("AI Basics", "John", "456", "2023");

        library.PatronManager.AddPatron("Sanjay");

        library.BorrowBook("Sanjay", "123");

        // Strategy Search
        List<Book> books = library.SearchBooks(new SearchByTitle("Game Dev"));

        for (Book b : books)
        {
            System.out.println(b.getTitle());
        }

        library.ReturnBook("Sanjay", "123");
    }
}

