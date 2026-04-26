import java.util.*;
import java.util.logging.*;

// ======================= LOGGER =======================
class LogManager
{
    static Logger logger = Logger.getLogger("LibraryLogger");
}

// ======================= ENUMS =======================
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
    private Suggestion genre;

    List<Observer> observers = new ArrayList<>();
    Queue<Patron> reservationQueue = new LinkedList<>();

    Book(String Title, String Author, String ISBN, String YearOfPublication, Suggestion genre)
    {
        this.Title = Title;
        this.Author = Author;
        this.ISBN = ISBN;
        this.YearOfPublication = YearOfPublication;
        this.genre = genre;
        this.Status = BookStatus.AVAILABLE;
    }

    String getTitle() { return Title; }
    String getAuthor() { return Author; }
    String getISBN() { return ISBN; }
    BookStatus getStatus() { return Status; }
    Suggestion getGenre() { return genre; }

    void UpdateBookStatus(BookStatus Status)
    {
        this.Status = Status;

        // Reservation handling
        if (Status == BookStatus.AVAILABLE && !reservationQueue.isEmpty())
        {
            Patron next = reservationQueue.poll();
            notifyObservers("Book available for " + next.Name);
        }

        notifyObservers("Status changed to " + Status);
    }

    void reserveBook(Patron patron)
    {
        reservationQueue.add(patron);
        addObserver(new PatronObserver(patron.Name));
        LogManager.logger.info(patron.Name + " reserved " + Title);
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
    static Book createBook(String title, String author, String isbn, String year, Suggestion genre)
    {
        return new Book(title, author, isbn, year, genre);
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

// ======================= STRATEGY =======================
interface RecommendationStrategy
{
    List<Book> recommend(Patron patron, List<Book> allBooks);
}

class GenreBasedRecommendation implements RecommendationStrategy
{
    public List<Book> recommend(Patron patron, List<Book> allBooks)
    {
        Map<Suggestion, Integer> freq = new HashMap<>();

        for (Book b : patron.BorrowHistory)
        {
            freq.put(b.getGenre(), freq.getOrDefault(b.getGenre(), 0) + 1);
        }

        Suggestion fav = null;
        int max = 0;

        for (Map.Entry<Suggestion, Integer> entry : freq.entrySet())
        {
            if (entry.getValue() > max)
            {
                max = entry.getValue();
                fav = entry.getKey();
            }
        }

        List<Book> result = new ArrayList<>();

        for (Book b : allBooks)
        {
            if (b.getGenre() == fav && b.getStatus() == BookStatus.AVAILABLE)
            {
                result.add(b);
            }
        }

        return result;
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

// ======================= LIBRARY BRANCH =======================
class LibraryBranch
{
    String branchName;
    Map<String, Book> bookMap = new HashMap<>();
    List<Book> books = new ArrayList<>();

    LibraryBranch(String branchName)
    {
        this.branchName = branchName;
    }

    void addBook(Book book)
    {
        books.add(book);
        bookMap.put(book.getISBN(), book);
        LogManager.logger.info("Book added to " + branchName + ": " + book.getTitle());
    }

    Book findBook(String isbn)
    {
        return bookMap.get(isbn);
    }

    void removeBook(Book book)
    {
        books.remove(book);
        bookMap.remove(book.getISBN());
    }
}

// ======================= LIBRARY SYSTEM =======================
class LibrarySystem
{
    Map<String, LibraryBranch> branches = new HashMap<>();
    PatronManager patronManager = new PatronManager();

    void addBranch(String name)
    {
        branches.put(name, new LibraryBranch(name));
    }

    LibraryBranch getBranch(String name)
    {
        return branches.get(name);
    }

    void transferBook(String from, String to, String isbn)
    {
        LibraryBranch source = branches.get(from);
        LibraryBranch dest = branches.get(to);

        if (source == null || dest == null)
        {
            LogManager.logger.warning("Invalid branch");
            return;
        }

        Book book = source.findBook(isbn);

        if (book == null)
        {
            LogManager.logger.warning("Book not found in source");
            return;
        }

        source.removeBook(book);
        dest.addBook(book);

        LogManager.logger.info("Book transferred from " + from + " to " + to);
    }

    void borrowBook(String branchName, String patronName, String isbn)
    {
        LibraryBranch branch = branches.get(branchName);
        Patron patron = patronManager.FindPatron(patronName);

        if (branch == null || patron == null)
        {
            LogManager.logger.warning("Invalid borrow attempt");
            return;
        }

        Book book = branch.findBook(isbn);

        if (book.getStatus() == BookStatus.NOT_AVAILABLE)
        {
            book.reserveBook(patron);
            return;
        }

        book.UpdateBookStatus(BookStatus.NOT_AVAILABLE);
        patron.AddBorrowedBook(book);
        book.addObserver(new PatronObserver(patronName));

        LogManager.logger.info(patronName + " borrowed " + book.getTitle());
    }

    void returnBook(String branchName, String patronName, String isbn)
    {
        LibraryBranch branch = branches.get(branchName);
        Patron patron = patronManager.FindPatron(patronName);

        Book book = branch.findBook(isbn);

        if (!patron.InHandBooks.contains(book))
        {
            LogManager.logger.warning("Book not with patron");
            return;
        }

        book.UpdateBookStatus(BookStatus.AVAILABLE);
        patron.ReturnBook(book);

        LogManager.logger.info(book.getTitle() + " returned by " + patronName);
    }

    List<Book> recommendBooks(String patronName, String branchName, RecommendationStrategy strategy)
    {
        Patron patron = patronManager.FindPatron(patronName);
        LibraryBranch branch = branches.get(branchName);

        return strategy.recommend(patron, branch.books);
    }
}

// ======================= MAIN =======================
public class Main
{
    public static void main(String[] args)
    {
        LibrarySystem system = new LibrarySystem();

        system.addBranch("BranchA");
        system.addBranch("BranchB");

        system.patronManager.AddPatron("Sanjay");

        LibraryBranch branchA = system.getBranch("BranchA");

        branchA.addBook(BookFactory.createBook("Game Dev", "Sanjay", "123", "2025", Suggestion.FICTION));
        branchA.addBook(BookFactory.createBook("AI Basics", "John", "456", "2023", Suggestion.SCIENCE_FICTION));

        // Borrow
        system.borrowBook("BranchA", "Sanjay", "123");

        // Try reserving same book
        system.borrowBook("BranchA", "Sanjay", "123");

        // Return
        system.returnBook("BranchA", "Sanjay", "123");

        // Transfer
        system.transferBook("BranchA", "BranchB", "123");

        // Recommendation
        List<Book> recs = system.recommendBooks("Sanjay", "BranchA", new GenreBasedRecommendation());

        System.out.println("Recommended Books:");
        for (Book b : recs)
        {
            System.out.println(b.getTitle());
        }
    }
}