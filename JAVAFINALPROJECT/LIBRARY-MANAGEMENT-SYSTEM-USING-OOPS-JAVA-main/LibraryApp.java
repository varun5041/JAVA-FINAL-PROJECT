import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

public class LibraryApp extends JFrame {
    private final admin libraryAdmin;

    // Screens
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);

    // Login components
    private JTextField adminUserField;
    private JPasswordField adminPassField;
    private JTextField memberIdField;
    private JPasswordField memberPassField;

    // Admin dashboard components
    private JTable booksTable;
    private DefaultTableModel booksModel;
    private JTable membersTable;
    private DefaultTableModel membersModel;

    // Member dashboard state
    private member currentMember;
    private JTable memberBooksTable;
    private DefaultTableModel memberBooksModel;
    private JLabel memberWelcomeLabel;
    private DefaultTableModel requestsModel;

    public LibraryApp() {
        // Initialize admin (this will also initialize database connection)
        this.libraryAdmin = new admin("Admin", 1);
        
        // Seed demo data only if database is empty (optional - comment out if you want to use existing data)
        // The database_schema.sql file includes sample data, so this is optional
        try {
            java.util.ArrayList<Book> existingBooks = this.libraryAdmin.getBooks();
            if (existingBooks.isEmpty()) {
                this.libraryAdmin.addbook(101, "Clean Code", "Robert C. Martin", 45.0);
                this.libraryAdmin.addbook(102, "Effective Java", "Joshua Bloch", 50.0);
                this.libraryAdmin.addbook(103, "Head First Design Patterns", "Eric Freeman", 40.0);
            }
            java.util.ArrayList<member> existingMembers = this.libraryAdmin.getMembers();
            if (existingMembers.isEmpty()) {
                // Add sample members (IDs will be auto-generated)
                this.libraryAdmin.AddMember("Alice");
                this.libraryAdmin.AddMember("Bob");
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not seed demo data: " + e.getMessage());
        }

        setTitle("Library Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        root.add(buildLoginPanel(), "login");
        root.add(buildAdminPanel(), "admin");
        root.add(buildMemberPanel(), "member");
        setContentPane(root);

        showLogin();
    }

    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTabbedPane tabs = new JTabbedPane();

        // Admin login tab
        JPanel adminTab = new JPanel(new GridBagLayout());
        adminUserField = new JTextField(15);  // Empty - no pre-filled value
        adminPassField = new JPasswordField(15);  // Empty - no pre-filled value
        JButton adminLoginBtn = new JButton(new AbstractAction("Login as Admin") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String user = adminUserField.getText().trim();
                String pass = new String(adminPassField.getPassword()).trim();
                
                if (user.isEmpty() || pass.isEmpty()) {
                    JOptionPane.showMessageDialog(LibraryApp.this, 
                        "Please enter both username and password", 
                        "Login Error", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Verify credentials from database
                boolean isValid = libraryAdmin.verifyAdminLogin(user, pass);
                if (isValid) {
                    currentMember = null;
                    // Clear password field for security
                    adminPassField.setText("");
                    refreshAdminTables();
                    cardLayout.show(root, "admin");
                } else {
                    JOptionPane.showMessageDialog(LibraryApp.this, 
                        "Invalid username or password", 
                        "Login Failed", 
                        JOptionPane.ERROR_MESSAGE);
                    // Clear password field for security
                    adminPassField.setText("");
                }
            }
        });
        gbc.gridx = 0; gbc.gridy = 0; adminTab.add(new JLabel("Username"), gbc);
        gbc.gridx = 1; adminTab.add(adminUserField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; adminTab.add(new JLabel("Password"), gbc);
        gbc.gridx = 1; adminTab.add(adminPassField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; adminTab.add(adminLoginBtn, gbc);

        // Member login tab
        JPanel memberTab = new JPanel(new GridBagLayout());
        memberIdField = new JTextField(15);  // Empty - no pre-filled value
        memberPassField = new JPasswordField(15);  // Empty - no pre-filled value
        JButton memberLoginBtn = new JButton(new AbstractAction("Login as Member") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String idText = memberIdField.getText().trim();
                    String pass = new String(memberPassField.getPassword()).trim();
                    
                    if (idText.isEmpty() || pass.isEmpty()) {
                        JOptionPane.showMessageDialog(LibraryApp.this, 
                            "Please enter both Member ID and password", 
                            "Login Error", 
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    int id = Integer.parseInt(idText);
                    
                    // Verify credentials from database
                    boolean isValid = libraryAdmin.verifyMemberLogin(id, pass);
                    if (isValid) {
                        member m = libraryAdmin.getMemberById(id);
                        if (m != null) {
                            currentMember = m;
                            if (memberWelcomeLabel != null) {
                                memberWelcomeLabel.setText("Welcome, " + m.Membername + " (ID " + m.MemberId + ")");
                            }
                            // Clear password field for security
                            memberPassField.setText("");
                            refreshMemberTables();
                            cardLayout.show(root, "member");
                        } else {
                            JOptionPane.showMessageDialog(LibraryApp.this, 
                                "Member not found", 
                                "Login Failed", 
                                JOptionPane.ERROR_MESSAGE);
                            memberPassField.setText("");
                        }
                    } else {
                        JOptionPane.showMessageDialog(LibraryApp.this, 
                            "Invalid Member ID or password", 
                            "Login Failed", 
                            JOptionPane.ERROR_MESSAGE);
                        // Clear password field for security
                        memberPassField.setText("");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(LibraryApp.this, 
                        "Please enter a valid numeric Member ID", 
                        "Invalid Input", 
                        JOptionPane.WARNING_MESSAGE);
                    memberPassField.setText("");
                }
            }
        });
        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = 0; memberTab.add(new JLabel("Member ID"), gbc);
        gbc.gridx = 1; memberTab.add(memberIdField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; memberTab.add(new JLabel("Password"), gbc);
        gbc.gridx = 1; memberTab.add(memberPassField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; memberTab.add(memberLoginBtn, gbc);

        tabs.addTab("Admin", adminTab);
        tabs.addTab("Member", memberTab);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(tabs, gbc);
        return panel;
    }

    private JPanel buildAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top controls
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddMember = new JButton("Add Member");
        JButton btnRemoveMember = new JButton("Remove Member");
        JButton btnViewMembers = new JButton("View Members");
        JButton btnShowAllBooks = new JButton("Show All Books");
        JButton btnAddBook = new JButton("Add Book");
        JButton btnRemoveBook = new JButton("Remove Book");
        JButton btnAddExistingStock = new JButton("Add Existing Stock");
        JButton btnIssuedBooks = new JButton("Issued Books");
        JButton btnChangeAdminPassword = new JButton("Change Password");
        JButton btnChangeMemberPassword = new JButton("Change Member Password");
        JButton logout = new JButton("Logout");

        // Button actions
        btnAddMember.addActionListener(e -> onAddMember());
        btnRemoveMember.addActionListener(e -> onRemoveMember());
        btnViewMembers.addActionListener(e -> refreshAdminTables());
        btnShowAllBooks.addActionListener(e -> refreshAdminTables());
        btnAddBook.addActionListener(e -> onAddBook());
        btnAddExistingStock.addActionListener(e -> onAddExistingStock());
        btnIssuedBooks.addActionListener(e -> onShowIssuedBooks());
        btnRemoveBook.addActionListener(e -> onRemoveBook());
        btnChangeAdminPassword.addActionListener(e -> onChangeAdminPassword());
        btnChangeMemberPassword.addActionListener(e -> onChangeMemberPasswordByAdmin());
        logout.addActionListener(e -> showLogin());

        top.add(btnAddMember);
        top.add(btnRemoveMember);
        top.add(btnViewMembers);
        top.add(btnShowAllBooks);
        top.add(btnAddBook);
        top.add(btnAddExistingStock);
        top.add(btnRemoveBook);
        top.add(btnIssuedBooks);
        top.add(Box.createHorizontalStrut(12));
        top.add(btnChangeAdminPassword);
        top.add(btnChangeMemberPassword);
        top.add(Box.createHorizontalStrut(16));
        top.add(logout);
        panel.add(top, BorderLayout.NORTH);

        // Center split: books and members
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        booksModel = new DefaultTableModel(new Object[]{"ID", "Title", "Author", "Price", "Qty", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        booksTable = new JTable(booksModel);
        split.setTopComponent(new JScrollPane(booksTable));

        JPanel bottom = new JPanel(new BorderLayout());
        membersModel = new DefaultTableModel(new Object[]{"ID", "Name"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        membersTable = new JTable(membersModel);
        bottom.add(new JScrollPane(membersTable), BorderLayout.CENTER);

        // Requests panel for admin notifications
        requestsModel = new DefaultTableModel(new Object[]{"ReqID", "Member", "BookID", "Title", "Qty", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable reqTable = new JTable(requestsModel);
        JPanel reqPanel = new JPanel(new BorderLayout());
        reqPanel.add(new JLabel("Requests"), BorderLayout.NORTH);
        reqPanel.add(new JScrollPane(reqTable), BorderLayout.CENTER);
        JButton approve = new JButton("Approve");
        JButton reject = new JButton("Reject");
        JButton clearResolvedRequestsAdmin = new JButton("Clear Resolved");
        approve.addActionListener(e -> {
            String s = JOptionPane.showInputDialog(this, "Approve Request ID:");
            if (s == null) return;
            try {
                int rid = Integer.parseInt(s.trim());
                boolean ok = libraryAdmin.approveRequest(rid);
                if (!ok) JOptionPane.showMessageDialog(this, "Approve failed");
                refreshAdminTables();
                fillRequests(requestsModel);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid numeric Request ID");
            }
        });
        reject.addActionListener(e -> {
            String s = JOptionPane.showInputDialog(this, "Reject Request ID:");
            if (s == null) return;
            try {
                int rid = Integer.parseInt(s.trim());
                boolean ok = libraryAdmin.rejectRequest(rid);
                if (!ok) JOptionPane.showMessageDialog(this, "Reject failed");
                fillRequests(requestsModel);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid numeric Request ID");
            }
        });
        clearResolvedRequestsAdmin.addActionListener(e -> {
            int count = libraryAdmin.clearAllResolvedRequests();
            JOptionPane.showMessageDialog(this, count + " resolved request(s) cleared.");
            fillRequests(requestsModel);
        });
        JPanel reqActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reqActions.add(approve); reqActions.add(reject); reqActions.add(clearResolvedRequestsAdmin);
        reqPanel.add(reqActions, BorderLayout.SOUTH);

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bottomSplit.setLeftComponent(new JScrollPane(membersTable));
        bottomSplit.setRightComponent(reqPanel);
        bottomSplit.setDividerLocation(300);
        bottom.add(bottomSplit, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField issueBookId = new JTextField(6);
        JTextField issueMemberId = new JTextField(6);
        JButton issueBtn = new JButton("Issue Book");
        issueBtn.addActionListener(e -> {
            try {
                int bid = Integer.parseInt(issueBookId.getText().trim());
                int mid = Integer.parseInt(issueMemberId.getText().trim());
                boolean ok = libraryAdmin.issueBookToMember(bid, mid);
                if (!ok) JOptionPane.showMessageDialog(this, "Issue failed. Check availability and IDs.");
                refreshAdminTables();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid numeric IDs");
            }
        });

        JTextField returnBookId = new JTextField(6);
        JTextField returnMemberId = new JTextField(6);
        JButton returnBtn = new JButton("Return Book");
        returnBtn.addActionListener(e -> {
            try {
                int bid = Integer.parseInt(returnBookId.getText().trim());
                int mid = Integer.parseInt(returnMemberId.getText().trim());
                boolean ok = libraryAdmin.returnBookFromMember(bid, mid);
                if (!ok) JOptionPane.showMessageDialog(this, "Return failed. Check IDs.");
                refreshAdminTables();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid numeric IDs");
            }
        });

        actions.add(new JLabel("Issue BookID:")); actions.add(issueBookId);
        actions.add(new JLabel("to MemberID:")); actions.add(issueMemberId);
        actions.add(issueBtn);
        actions.add(Box.createHorizontalStrut(12));
        actions.add(new JLabel("Return BookID:")); actions.add(returnBookId);
        actions.add(new JLabel("from MemberID:")); actions.add(returnMemberId);
        actions.add(returnBtn);
        bottom.add(actions, BorderLayout.SOUTH);

        split.setBottomComponent(bottom);
        split.setDividerLocation(300);
        panel.add(split, BorderLayout.CENTER);

        // initial fill
        fillRequests(requestsModel);
        return panel;
    }

    private void onAddMember() {
        JTextField name = new JTextField(20);
        int ok = JOptionPane.showConfirmDialog(this, 
            new Object[]{"Member Name:", name}, 
            "Add Member", 
            JOptionPane.OK_CANCEL_OPTION);
            
        if (ok == JOptionPane.OK_OPTION) {
            try {
                String n = name.getText().trim();
                if (n.isEmpty()) { 
                    JOptionPane.showMessageDialog(this, "Name is required!"); 
                    return; 
                }
                
                // Add member to database (ID will be auto-generated)
                int generatedId = libraryAdmin.AddMember(n);
                
                if (generatedId > 0) {
                    // Get the generated password for the new member
                    member newMember = libraryAdmin.getMemberById(generatedId);
                    String memberPassword = (newMember != null && newMember.password != null) 
                        ? newMember.password 
                        : "member" + (System.currentTimeMillis() % 1000);
                    
                    // Refresh UI on EDT
                    SwingUtilities.invokeLater(() -> {
                        refreshAdminTables();
                        if (membersTable != null) {
                            membersTable.revalidate();
                            membersTable.repaint();
                        }
                    });
                    
                    JOptionPane.showMessageDialog(this, 
                        "Member added successfully!\n\n" +
                        "Member ID: " + generatedId + "\n" +
                        "Name: " + n + "\n" +
                        "Password: " + memberPassword + "\n\n" +
                        "Please note the password for member login.",
                        "Member Added", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    String errorMsg = "Failed to add member. ";
                    errorMsg += "\n\nPossible issues:";
                    errorMsg += "\n1. Database connection problem";
                    errorMsg += "\n2. Table structure may need AUTO_INCREMENT";
                    errorMsg += "\n3. Check console for detailed error message";
                    errorMsg += "\n\nTry running: ALTER TABLE members MODIFY member_id INT AUTO_INCREMENT;";
                    JOptionPane.showMessageDialog(this, errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error adding member: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void onRemoveMember() {
        String s = JOptionPane.showInputDialog(this, "Member ID to remove:");
        if (s == null) return;
        try {
            int mid = Integer.parseInt(s.trim());
            libraryAdmin.removemember(mid);
            refreshAdminTables();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid numeric ID");
        }
    }

    private void onAddBook() {
        JTextField id = new JTextField();
        JTextField title = new JTextField();
        JTextField author = new JTextField();
        JTextField price = new JTextField();
        JTextField quantity = new JTextField("1");
        int ok = JOptionPane.showConfirmDialog(
            this,
            new Object[]{
                "Book ID (leave blank for auto):", id,
                "Title:", title,
                "Author:", author,
                "Price:", price,
                "Quantity:", quantity
            },
            "Add Book",
            JOptionPane.OK_CANCEL_OPTION
        );
        if (ok == JOptionPane.OK_OPTION) {
            try {
                String idText = id.getText().trim();
                String t = title.getText().trim();
                String a = author.getText().trim();
                String qText = quantity.getText().trim();
                if (t.isEmpty() || a.isEmpty()) { JOptionPane.showMessageDialog(this, "Title and Author are required"); return; }
                double p = Double.parseDouble(price.getText().trim());
                int qty = 1;
                if (!qText.isEmpty()) {
                    qty = Math.max(1, Integer.parseInt(qText));
                }

                if (idText.isEmpty()) {
                    int newId = libraryAdmin.addbook(t, a, p, qty);
                    if (newId > 0) {
                        JOptionPane.showMessageDialog(this, "Book added successfully with ID: " + newId);
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to add book. Check logs/DB schema.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    int bid = Integer.parseInt(idText);
                    boolean idExists = false;
                    for (Book b : libraryAdmin.getBooks()) {
                        if (b.BookId == bid) { idExists = true; break; }
                    }
                    if (idExists) {
                        boolean increased = libraryAdmin.increaseBookQuantityById(bid, qty);
                        if (increased) {
                            JOptionPane.showMessageDialog(this, "Existing book found. Quantity increased by " + qty + ".");
                        } else {
                            JOptionPane.showMessageDialog(this, "Failed to increase quantity for existing Book ID.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        libraryAdmin.addbook(bid, t, a, p, qty);
                        JOptionPane.showMessageDialog(this, "Book added successfully with ID: " + bid);
                    }
                }
                refreshAdminTables();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid numeric values for Price/Quantity/ID");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to add book: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onRemoveBook() {
        String s = JOptionPane.showInputDialog(this, "Book ID to remove:");
        if (s == null) return;
        try {
            int bid = Integer.parseInt(s.trim());
            libraryAdmin.removebookById(bid);
            refreshAdminTables();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid numeric ID");
        }
    }

    private void onAddExistingStock() {
        JTextField id = new JTextField();
        JTextField quantity = new JTextField("1");
        int ok = JOptionPane.showConfirmDialog(
            this,
            new Object[]{
                "Existing Book ID:", id,
                "Add Quantity:", quantity
            },
            "Add Existing Stock",
            JOptionPane.OK_CANCEL_OPTION
        );
        if (ok == JOptionPane.OK_OPTION) {
            try {
                int bid = Integer.parseInt(id.getText().trim());
                int qty = Math.max(1, Integer.parseInt(quantity.getText().trim()));
                boolean increased = libraryAdmin.increaseBookQuantityById(bid, qty);
                if (increased) {
                    JOptionPane.showMessageDialog(this, "Quantity increased by " + qty + " for Book ID " + bid + ".");
                } else {
                    JOptionPane.showMessageDialog(this, "Book ID not found or update failed.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                refreshAdminTables();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid numeric values for ID and Quantity");
            }
        }
    }

    private void onShowIssuedBooks() {
        java.util.List<admin.LoanRecord> loans = libraryAdmin.getActiveLoans();
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Book ID", "Title", "Author", "Member ID", "Member Name", "Qty"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (admin.LoanRecord r : loans) {
            model.addRow(new Object[]{r.bookId, r.title, r.author, r.memberId, r.memberName, r.borrowedQty});
        }
        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new java.awt.Dimension(700, 300));
        JOptionPane.showMessageDialog(this, sp, "Issued Books", JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel buildMemberPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        memberWelcomeLabel = new JLabel("Welcome");
        JButton btnChangePassword = new JButton("Change Password");
        JButton logout = new JButton("Logout");
        btnChangePassword.addActionListener(e -> onChangeMemberPassword());
        logout.addActionListener(e -> showLogin());
        top.add(memberWelcomeLabel); 
        top.add(Box.createHorizontalStrut(16)); 
        top.add(btnChangePassword);
        top.add(Box.createHorizontalStrut(8));
        top.add(logout);
        panel.add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        // All books (read-only view with search)
        JPanel allBooksPanel = new JPanel(new BorderLayout());
        JTextField searchField = new JTextField(20);
        JComboBox<String> searchType = new JComboBox<>(new String[]{"Title", "Author"});
        JButton showAllBtn = new JButton("Show All Books");
        JButton searchBtn = new JButton("Search");
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchBar.add(showAllBtn);
        searchBar.add(Box.createHorizontalStrut(8));
        searchBar.add(searchType); searchBar.add(searchField); searchBar.add(searchBtn);
        allBooksPanel.add(searchBar, BorderLayout.NORTH);
        DefaultTableModel allBooksModel = new DefaultTableModel(new Object[]{"ID", "Title", "Author", "Price", "Availability"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable allBooksTable = new JTable(allBooksModel);
        allBooksPanel.add(new JScrollPane(allBooksTable), BorderLayout.CENTER);
        searchBtn.addActionListener(e -> doMemberSearch(searchType, searchField, allBooksModel));
        showAllBtn.addActionListener(e -> fillAllBooksForMember(allBooksModel));

        // Borrowed books of current member
        JPanel borrowedPanel = new JPanel(new BorderLayout());
        memberBooksModel = new DefaultTableModel(new Object[]{"ID", "Title", "Author", "Price", "Qty"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        memberBooksTable = new JTable(memberBooksModel);
        borrowedPanel.add(new JScrollPane(memberBooksTable), BorderLayout.CENTER);

        JPanel memberActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton requestBtn = new JButton("Request Book");
        requestBtn.addActionListener(e -> onRequestBook());
        memberActions.add(requestBtn);
        borrowedPanel.add(memberActions, BorderLayout.SOUTH);

        split.setTopComponent(allBooksPanel);
        split.setBottomComponent(borrowedPanel);
        split.setDividerLocation(300);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private void doAdminSearch(JComboBox<String> searchType, JTextField searchField) {
        String mode = (String) searchType.getSelectedItem();
        String q = searchField.getText().trim();
        ArrayList<Book> result = "Author".equals(mode)
                ? libraryAdmin.searchBooksByAuthor(q)
                : libraryAdmin.searchBooksByTitle(q);
        fillBooksTable(result);
    }

    private void doMemberSearch(JComboBox<String> searchType, JTextField searchField, DefaultTableModel model) {
        String mode = (String) searchType.getSelectedItem();
        String q = searchField.getText().trim();
        ArrayList<Book> result = "Author".equals(mode)
                ? libraryAdmin.searchBooksByAuthor(q)
                : libraryAdmin.searchBooksByTitle(q);
        model.setRowCount(0);
        for (Book b : result) {
            String avail = availabilityString(b);
            model.addRow(new Object[]{b.BookId, b.Title, b.Author, b.Price, avail});
        }
        // Also inform admin view about any changes in data (no-op for search, but keeps UI coherent if we later track member-side actions)
        if (requestsModel != null) fillRequests(requestsModel);
    }

    private void refreshAdminTables() {
        try {
            System.out.println("Refreshing admin tables...");
            ArrayList<Book> books = libraryAdmin.getBooks();
            ArrayList<member> members = libraryAdmin.getMembers();
            
            System.out.println("Got " + books.size() + " books and " + members.size() + " members from database");
            
            fillBooksTable(books);
            fillMembersTable(members);
            
            // Force UI update
            if (booksTable != null) {
                booksTable.revalidate();
                booksTable.repaint();
            }
            if (membersTable != null) {
                membersTable.revalidate();
                membersTable.repaint();
            }
            
            System.out.println("✓ Admin tables refreshed");
        } catch (Exception e) {
            System.err.println("Error refreshing admin tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void fillBooksTable(java.util.List<Book> list) {
        if (booksModel == null) return;
        booksModel.setRowCount(0);
        for (Book b : list) {
            String status = (b.Quantity > 0) ? "Available" : "Out of stock";
            booksModel.addRow(new Object[]{b.BookId, b.Title, b.Author, b.Price, b.Quantity, status});
        }
    }

    private void fillMembersTable(java.util.List<member> list) {
        if (membersModel == null) {
            System.err.println("ERROR: membersModel is null, cannot fill table!");
            return;
        }
        
        System.out.println("=== FILLING MEMBERS TABLE ===");
        System.out.println("Received " + list.size() + " members");
        
        membersModel.setRowCount(0);
        
        int added = 0;
        for (member m : list) {
            if (m != null) {
                System.out.println("  Adding member: ID=" + m.MemberId + ", Name=" + m.Membername);
                membersModel.addRow(new Object[]{m.MemberId, m.Membername});
                added++;
            } else {
                System.err.println("  WARNING: Null member object found!");
            }
        }
        
        System.out.println("✓ Added " + added + " members to table");
        System.out.println("✓ Table now has " + membersModel.getRowCount() + " total rows");
        System.out.println("=============================");
        
        // Force table update
        if (membersTable != null) {
            membersTable.updateUI();
        }
    }

    private void fillRequests(DefaultTableModel reqModel) {
        reqModel.setRowCount(0);
        for (BookRequest r : libraryAdmin.getRequests()) {
            reqModel.addRow(new Object[]{r.requestId, r.memberName + " (" + r.memberId + ")", r.bookId, r.bookTitle, r.quantity, r.status});
        }
    }

    private void refreshMemberTables() {
        if (currentMember == null) return;
        memberBooksModel.setRowCount(0);
        java.util.Map<Integer, Integer> bookIdToCount = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, Book> bookIdToBook = new java.util.LinkedHashMap<>();
        for (Book b : currentMember.borrowedbooks) {
            bookIdToBook.putIfAbsent(b.BookId, b);
            bookIdToCount.put(b.BookId, bookIdToCount.getOrDefault(b.BookId, 0) + 1);
        }
        for (java.util.Map.Entry<Integer, Book> entry : bookIdToBook.entrySet()) {
            int id = entry.getKey();
            Book b = entry.getValue();
            int qty = bookIdToCount.getOrDefault(id, 0);
            memberBooksModel.addRow(new Object[]{b.BookId, b.Title, b.Author, b.Price, qty});
        }
    }

    private void showLogin() {
        cardLayout.show(root, "login");
    }
    
    private void onChangeAdminPassword() {
        String username = libraryAdmin.getCurrentAdminUsername();
        
        JPasswordField currentPassField = new JPasswordField(20);
        JPasswordField newPassField = new JPasswordField(20);
        JPasswordField confirmPassField = new JPasswordField(20);
        
        int ok = JOptionPane.showConfirmDialog(this, 
            new Object[]{
                "Username: " + username,
                "Current Password:", currentPassField,
                "New Password:", newPassField,
                "Confirm New Password:", confirmPassField
            }, 
            "Change Admin Password", 
            JOptionPane.OK_CANCEL_OPTION);
            
        if (ok == JOptionPane.OK_OPTION) {
            String currentPass = new String(currentPassField.getPassword()).trim();
            String newPass = new String(newPassField.getPassword()).trim();
            String confirmPass = new String(confirmPassField.getPassword()).trim();
            
            if (currentPass.isEmpty() || newPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Password fields cannot be empty!", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, 
                    "New password and confirmation do not match!", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (newPass.length() < 3) {
                JOptionPane.showMessageDialog(this, 
                    "New password must be at least 3 characters long!", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            boolean success = libraryAdmin.changeAdminPassword(username, currentPass, newPass);
            if (success) {
                JOptionPane.showMessageDialog(this, 
                    "Password changed successfully!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Failed to change password. Please check your current password.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void onChangeMemberPasswordByAdmin() {
        JTextField memberIdField = new JTextField(10);
        JPasswordField newPassField = new JPasswordField(20);
        JPasswordField confirmPassField = new JPasswordField(20);
        
        int ok = JOptionPane.showConfirmDialog(this, 
            new Object[]{
                "Member ID:", memberIdField,
                "New Password:", newPassField,
                "Confirm New Password:", confirmPassField
            }, 
            "Change Member Password", 
            JOptionPane.OK_CANCEL_OPTION);
            
        if (ok == JOptionPane.OK_OPTION) {
            try {
                int memberId = Integer.parseInt(memberIdField.getText().trim());
                String newPass = new String(newPassField.getPassword()).trim();
                String confirmPass = new String(confirmPassField.getPassword()).trim();
                
                if (newPass.isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        "Password cannot be empty!", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (!newPass.equals(confirmPass)) {
                    JOptionPane.showMessageDialog(this, 
                        "New password and confirmation do not match!", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (newPass.length() < 3) {
                    JOptionPane.showMessageDialog(this, 
                        "New password must be at least 3 characters long!", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                boolean success = libraryAdmin.changeMemberPasswordByAdmin(memberId, newPass);
                if (success) {
                    member m = libraryAdmin.getMemberById(memberId);
                    String memberName = (m != null) ? m.Membername : "ID " + memberId;
                    JOptionPane.showMessageDialog(this, 
                        "Password changed successfully for:\n" +
                        "Member ID: " + memberId + "\n" +
                        "Name: " + memberName + "\n" +
                        "New Password: " + newPass,
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Failed to change password. Member may not exist.", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter a valid numeric Member ID", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void onChangeMemberPassword() {
        if (currentMember == null) {
            JOptionPane.showMessageDialog(this, 
                "Please login as a member first", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JPasswordField currentPassField = new JPasswordField(20);
        JPasswordField newPassField = new JPasswordField(20);
        JPasswordField confirmPassField = new JPasswordField(20);
        
        int ok = JOptionPane.showConfirmDialog(this, 
            new Object[]{
                "Member ID: " + currentMember.MemberId,
                "Member Name: " + currentMember.Membername,
                "Current Password:", currentPassField,
                "New Password:", newPassField,
                "Confirm New Password:", confirmPassField
            }, 
            "Change Password", 
            JOptionPane.OK_CANCEL_OPTION);
            
        if (ok == JOptionPane.OK_OPTION) {
            String currentPass = new String(currentPassField.getPassword()).trim();
            String newPass = new String(newPassField.getPassword()).trim();
            String confirmPass = new String(confirmPassField.getPassword()).trim();
            
            if (currentPass.isEmpty() || newPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Password fields cannot be empty!", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, 
                    "New password and confirmation do not match!", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (newPass.length() < 3) {
                JOptionPane.showMessageDialog(this, 
                    "New password must be at least 3 characters long!", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            boolean success = libraryAdmin.changeMemberPassword(
                currentMember.MemberId, currentPass, newPass);
            if (success) {
                JOptionPane.showMessageDialog(this, 
                    "Password changed successfully!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Failed to change password. Please check your current password.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String issuedStatus(Book b) {
        return (b.Quantity > 0) ? ("Available (" + b.Quantity + ")") : "Out of stock";
    }

    private String availabilityString(Book b) {
        return (b.Quantity > 0) ? ("Available (" + b.Quantity + ")") : "Out of stock";
    }

    private void fillAllBooksForMember(DefaultTableModel model) {
        model.setRowCount(0);
        for (Book b : libraryAdmin.getBooks()) {
            model.addRow(new Object[]{b.BookId, b.Title, b.Author, b.Price, availabilityString(b)});
        }
    }

    private void onRequestBook() {
        if (currentMember == null) { JOptionPane.showMessageDialog(this, "Please login as member first"); return; }
        JTextField bookId = new JTextField();
        JTextField title = new JTextField();
        JTextField qtyField = new JTextField("1");
        int ok = JOptionPane.showConfirmDialog(this, new Object[]{"Book ID:", bookId, "Title:", title, "Quantity:", qtyField}, "Request Book", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            try {
                int bid = Integer.parseInt(bookId.getText().trim());
                String t = title.getText().trim();
                int q = Math.max(1, Integer.parseInt(qtyField.getText().trim()));
                // Temporarily submit base request (admin will treat default as 1); then update quantity directly
                BookRequest r = libraryAdmin.submitRequest(currentMember.MemberId, bid, t);
                if (r == null) {
                    JOptionPane.showMessageDialog(this, "Failed to submit request");
                } else {
                    // Update quantity in DB for this new request
                    try {
                        java.sql.Connection conn = DatabaseConnection.getConnection();
                        try (java.sql.Statement useStmt = conn.createStatement()) { useStmt.executeUpdate("USE " + getDbName()); }
                        try (java.sql.PreparedStatement ps = conn.prepareStatement("UPDATE book_requests SET quantity = ? WHERE request_id = ?")) {
                            ps.setInt(1, q);
                            ps.setInt(2, r.requestId);
                            ps.executeUpdate();
                        }
                        conn.close();
                        r.quantity = q;
                    } catch (Exception ignore) { }
                    JOptionPane.showMessageDialog(this, "Request submitted: #" + r.requestId);
                    if (requestsModel != null) fillRequests(requestsModel);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid numeric Book ID");
            }
        }
    }

    // Helper to extract DB name for inline update (reuses DatabaseConnection logic)
    private String getDbName() {
        try {
            java.lang.reflect.Method m = DatabaseConnection.class.getDeclaredMethod("extractDatabaseName", String.class);
            m.setAccessible(true);
            java.lang.reflect.Field f = DatabaseConnection.class.getDeclaredField("dbUrl");
            f.setAccessible(true);
            String url = (String) f.get(null);
            return (String) m.invoke(null, url);
        } catch (Exception e) {
            return "library_db";
        }
    }

    private void onClearResolvedRequests() {
        if (currentMember == null) {
            JOptionPane.showMessageDialog(this, "Please login as a member first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int deleted = libraryAdmin.clearResolvedRequestsForMember(currentMember.MemberId);
        JOptionPane.showMessageDialog(this, deleted + " resolved request(s) cleared.");
        if (requestsModel != null) fillRequests(requestsModel);
    }
}


