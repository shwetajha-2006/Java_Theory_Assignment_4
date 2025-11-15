import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

class Book implements Comparable<Book> {
    int bookId;
    String title;
    String author;
    String category;
    boolean isIssued;

    Book(int bookId, String title, String author, String category, boolean isIssued) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.category = category;
        this.isIssued = isIssued;
    }

    void displayBookDetails() {
        System.out.println("ID: " + bookId);
        System.out.println("Title: " + title);
        System.out.println("Author: " + author);
        System.out.println("Category: " + category);
        System.out.println("Issued: " + (isIssued ? "Yes" : "No"));
    }

    void markAsIssued() {
        isIssued = true;
    }

    void markAsReturned() {
        isIssued = false;
    }

    @Override
    public int compareTo(Book other) {
        return this.title.compareToIgnoreCase(other.title);
    }

    String toFileLine() {
        return bookId + "|" + escape(title) + "|" + escape(author) + "|" + escape(category) + "|" + (isIssued ? "1" : "0");
    }

    static Book fromFileLine(String line) {
      
        String[] parts = splitPreserve(line, '|', 5);
        int id = Integer.parseInt(parts[0]);
        String t = unescape(parts[1]);
        String a = unescape(parts[2]);
        String c = unescape(parts[3]);
        boolean issued = "1".equals(parts[4]);
        return new Book(id, t, a, c, issued);
    }

  
    static String escape(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", "\\n");
    }

    static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\|", "|").replace("\\n", "\n");
    }

    static String[] splitPreserve(String s, char delim, int expectedParts) {
        List<String> parts = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (esc) {
                sb.append(ch);
                esc = false;
            } else {
                if (ch == '\\') {
                    esc = true;
                } else if (ch == delim) {
                    parts.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(ch);
                }
            }
        }
        parts.add(sb.toString());
       
        while (parts.size() < expectedParts) parts.add("");
        return parts.toArray(new String[0]);
    }
}

class Member {
    int memberId;
    String name;
    String email;
    List<Integer> issuedBooks; 

    Member(int memberId, String name, String email, List<Integer> issuedBooks) {
        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.issuedBooks = new ArrayList<>(issuedBooks);
    }

    void displayMemberDetails() {
        System.out.println("Member ID: " + memberId);
        System.out.println("Name: " + name);
        System.out.println("Email: " + email);
        System.out.print("Issued Books: ");
        if (issuedBooks.isEmpty()) System.out.print("None");
        else {
            for (Integer id : issuedBooks) System.out.print(id + " ");
        }
        System.out.println();
    }

    void addIssuedBook(int bookId) {
        if (!issuedBooks.contains(bookId))
            issuedBooks.add(bookId);
    }

    void returnIssuedBook(int bookId) {
        issuedBooks.remove(Integer.valueOf(bookId));
    }

    String toFileLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(memberId).append("|").append(escape(name)).append("|").append(escape(email)).append("|");
        for (int i = 0; i < issuedBooks.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(issuedBooks.get(i));
        }
        return sb.toString();
    }

    static Member fromFileLine(String line) {
        // id|name|email|id1,id2,...
        String[] parts = Book.splitPreserve(line, '|', 4);
        int id = Integer.parseInt(parts[0]);
        String n = Book.unescape(parts[1]);
        String e = Book.unescape(parts[2]);
        List<Integer> issued = new ArrayList<>();
        String issuedPart = parts.length > 3 ? parts[3] : "";
        if (!issuedPart.trim().isEmpty()) {
            String[] arr = issuedPart.split(",");
            for (String s : arr) {
                if (!s.trim().isEmpty()) {
                    try {
                        issued.add(Integer.parseInt(s.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return new Member(id, n, e, issued);
    }

    static String escape(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", "\\n");
    }
}

public class LibraryManager {
    private static final String BOOKS_FILE = "books.txt";
    private static final String MEMBERS_FILE = "members.txt";

    private Map<Integer, Book> books = new HashMap<>();
    private Map<Integer, Member> members = new HashMap<>();
    private Set<String> categories = new HashSet<>();

   
    private int nextBookId = 100;
    private int nextMemberId = 1000;

    private Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        LibraryManager app = new LibraryManager();
        app.loadFromFile();
        app.mainMenu();
    }

   
    void loadFromFile() {
        // Load books
        File bf = new File(BOOKS_FILE);
        if (bf.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(bf))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    try {
                        Book b = Book.fromFileLine(line);
                        books.put(b.bookId, b);
                        categories.add(b.category);
                        nextBookId = Math.max(nextBookId, b.bookId + 1);
                    } catch (Exception e) {
                        System.out.println("Skipping invalid book line: " + line);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error loading books: " + e.getMessage());
            }
        }

        // Load members
        File mf = new File(MEMBERS_FILE);
        if (mf.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(mf))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    try {
                        Member m = Member.fromFileLine(line);
                        members.put(m.memberId, m);
                        nextMemberId = Math.max(nextMemberId, m.memberId + 1);
                    } catch (Exception e) {
                        System.out.println("Skipping invalid member line: " + line);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error loading members: " + e.getMessage());
            }
        }
    }

    void saveToFile() {
        // Save books
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BOOKS_FILE))) {
            for (Book b : books.values()) {
                bw.write(b.toFileLine());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving books: " + e.getMessage());
        }

        // Save members
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(MEMBERS_FILE))) {
            for (Member m : members.values()) {
                bw.write(m.toFileLine());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving members: " + e.getMessage());
        }
    }

 
    void mainMenu() {
        try {
            while (true) {
                System.out.println("\n===== City Library Digital Management System =====");
                System.out.println("1. Add Book");
                System.out.println("2. Add Member");
                System.out.println("3. Issue Book");
                System.out.println("4. Return Book");
                System.out.println("5. Search Books");
                System.out.println("6. Sort Books");
                System.out.println("7. Show All Books");
                System.out.println("8. Show All Members");
                System.out.println("9. Exit");
                System.out.print("Enter your choice: ");

                String choiceLine = sc.nextLine().trim();
                int choice;
                try {
                    choice = Integer.parseInt(choiceLine);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Enter numeric choice.");
                    continue;
                }

                switch (choice) {
                    case 1 -> addBook();
                    case 2 -> addMember();
                    case 3 -> issueBook();
                    case 4 -> returnBook();
                    case 5 -> searchBooks();
                    case 6 -> sortBooksMenu();
                    case 7 -> showAllBooks();
                    case 8 -> showAllMembers();
                    case 9 -> {
                        System.out.println("Saving data and exiting...");
                        saveToFile();
                        System.out.println("Saved. Bye!");
                        return;
                    }
                    default -> System.out.println("Invalid choice. Try again.");
                }
            }
        } finally {
            sc.close();
        }
    }

    void addBook() {
        try {
            System.out.print("Enter Book Title: ");
            String title = sc.nextLine().trim();
            System.out.print("Enter Author: ");
            String author = sc.nextLine().trim();
            System.out.print("Enter Category: ");
            String category = sc.nextLine().trim();
            if (title.isEmpty() || author.isEmpty() || category.isEmpty()) {
                System.out.println("All fields are required. Book not added.");
                return;
            }

            int id = nextBookId++;
            Book b = new Book(id, title, author, category, false);
            books.put(id, b);
            categories.add(category);
            saveToFile();
            System.out.println("Book added successfully with ID: " + id);
        } catch (Exception e) {
            System.out.println("Error adding book: " + e.getMessage());
        }
    }

    void addMember() {
        try {
            System.out.print("Enter Member Name: ");
            String name = sc.nextLine().trim();
            System.out.print("Enter Email: ");
            String email = sc.nextLine().trim();

            if (name.isEmpty() || email.isEmpty()) {
                System.out.println("Name and email required. Member not added.");
                return;
            }

            if (!isValidEmail(email)) {
                System.out.println("Invalid email format. Member not added.");
                return;
            }

            int id = nextMemberId++;
            Member m = new Member(id, name, email, new ArrayList<>());
            members.put(id, m);
            saveToFile();
            System.out.println("Member added successfully with ID: " + id);
        } catch (Exception e) {
            System.out.println("Error adding member: " + e.getMessage());
        }
    }

    void issueBook() {
        try {
            System.out.print("Enter Member ID: ");
            int mid = Integer.parseInt(sc.nextLine().trim());
            Member m = members.get(mid);
            if (m == null) {
                System.out.println("Member not found.");
                return;
            }

            System.out.print("Enter Book ID to issue: ");
            int bid = Integer.parseInt(sc.nextLine().trim());
            Book b = books.get(bid);
            if (b == null) {
                System.out.println("Book not found.");
                return;
            }
            if (b.isIssued) {
                System.out.println("Book already issued.");
                return;
            }

            // Issue
            b.markAsIssued();
            m.addIssuedBook(bid);
            saveToFile();
            System.out.println("Book ID " + bid + " issued to Member ID " + mid);
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID - must be numeric.");
        } catch (Exception e) {
            System.out.println("Error issuing book: " + e.getMessage());
        }
    }

    void returnBook() {
        try {
            System.out.print("Enter Member ID: ");
            int mid = Integer.parseInt(sc.nextLine().trim());
            Member m = members.get(mid);
            if (m == null) {
                System.out.println("Member not found.");
                return;
            }

            System.out.print("Enter Book ID to return: ");
            int bid = Integer.parseInt(sc.nextLine().trim());
            Book b = books.get(bid);
            if (b == null) {
                System.out.println("Book not found.");
                return;
            }

            if (!m.issuedBooks.contains(bid)) {
                System.out.println("This member doesn't have that book issued.");
                return;
            }

            b.markAsReturned();
            m.returnIssuedBook(bid);
            saveToFile();
            System.out.println("Book ID " + bid + " returned by Member ID " + mid);
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID - must be numeric.");
        } catch (Exception e) {
            System.out.println("Error returning book: " + e.getMessage());
        }
    }

    void searchBooks() {
        System.out.println("Search by: 1) Title  2) Author  3) Category");
        System.out.print("Choice: ");
        String c = sc.nextLine().trim();
        List<Book> result = new ArrayList<>();

        switch (c) {
            case "1" -> {
                System.out.print("Enter title keyword: ");
                String key = sc.nextLine().trim().toLowerCase();
                for (Book b : books.values()) {
                    if (b.title.toLowerCase().contains(key)) result.add(b);
                }
            }
            case "2" -> {
                System.out.print("Enter author keyword: ");
                String key = sc.nextLine().trim().toLowerCase();
                for (Book b : books.values()) {
                    if (b.author.toLowerCase().contains(key)) result.add(b);
                }
            }
            case "3" -> {
                System.out.print("Enter category: ");
                String cat = sc.nextLine().trim().toLowerCase();
                for (Book b : books.values()) {
                    if (b.category.toLowerCase().contains(cat)) result.add(b);
                }
            }
            default -> {
                System.out.println("Invalid choice.");
                return;
            }
        }

        if (result.isEmpty()) {
            System.out.println("No books found.");
        } else {
            System.out.println("Found " + result.size() + " books:");
            for (Book b : result) {
                System.out.println("-------");
                b.displayBookDetails();
            }
        }
    }

    void sortBooksMenu() {
        System.out.println("Sort by: 1) Title (default) 2) Author 3) Category");
        System.out.print("Choice: ");
        String ch = sc.nextLine().trim();
        List<Book> list = new ArrayList<>(books.values());
        switch (ch) {
            case "2" -> list.sort(Comparator.comparing(b -> b.author.toLowerCase()));
            case "3" -> list.sort(Comparator.comparing(b -> b.category.toLowerCase()));
            default -> Collections.sort(list);
        }

        System.out.println("Sorted books:");
        for (Book b : list) {
            System.out.println("-------");
            b.displayBookDetails();
        }
    }

    void showAllBooks() {
        if (books.isEmpty()) {
            System.out.println("No books in library.");
            return;
        }
        System.out.println("All books:");
        for (Book b : books.values()) {
            System.out.println("-------");
            b.displayBookDetails();
        }
    }

    void showAllMembers() {
        if (members.isEmpty()) {
            System.out.println("No members registered.");
            return;
        }
        System.out.println("All members:");
        for (Member m : members.values()) {
            System.out.println("-------");
            m.displayMemberDetails();
        }
    }

    boolean isValidEmail(String email) {
        
        if (email == null) return false;
        String regex = "^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(regex, email);
    }
}

