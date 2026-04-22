import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DebtLoansPanel extends ThemeGradientPanel { // Inherits master gradient

    private JPanel cardsPanel;
    private JLabel totalOwedToMeLabel;
    private JLabel totalIOweLabel;
    private JComboBox<String> filterBox;
    
    private JPanel owedToMeBox;
    private JPanel iOweBox;
    
    private JLabel mainTitle;
    private JLabel summaryTitleLabel;
    private JLabel tLabelOwed;
    private JLabel tLabelIow;

    public DebtLoansPanel() {
        setLayout(new BorderLayout(20, 20));
        setOpaque(false); // CRITICAL: Let gradient show through
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Listen for Theme Changes
        this.addPropertyChangeListener("background", evt -> {
            SwingUtilities.invokeLater(() -> {
                updateThemeColors();
                loadData();
            });
        });

        // --- 1. HEADER (Title + Filters) ---
        JPanel headerPanel = new JPanel(new BorderLayout(0, 20));
        headerPanel.setOpaque(false);

        mainTitle = new JLabel("Debts & Loans Manager");
        mainTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        headerPanel.add(mainTitle, BorderLayout.NORTH);

        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.setOpaque(false);

        filterBox = new JComboBox<>(new String[]{
            "All Active", 
            "Active Loans (Owed to Me)", 
            "Active Debts (I Owe)", 
            "Settled"
        });
        filterBox.setPreferredSize(new Dimension(200, 35));
        filterBox.addActionListener(e -> loadData());
        controlsPanel.add(filterBox, BorderLayout.WEST);

        headerPanel.add(controlsPanel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // --- 2. CENTER: DEBT/LOAN CARDS GRID (HORIZONTAL ONLY) ---
        cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.X_AXIS));
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JScrollPane scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(new Color(0, 0, 0, 0));
        
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16); 
        
        add(scrollPane, BorderLayout.CENTER);

        // --- 3. BOTTOM: SUMMARY & ADD BUTTON ---
        JPanel bottomWrapper = new JPanel(new BorderLayout(0, 20));
        bottomWrapper.setOpaque(false);

        // Summary Boxes
        JPanel summaryPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        summaryPanel.setOpaque(false);
        
        owedToMeBox = createSummaryBox("Total Loans (Owed to Me)", "0", new Color(40, 167, 69), "↑");
        totalOwedToMeLabel = (JLabel) owedToMeBox.getClientProperty("amountLabel");
        tLabelOwed = (JLabel) owedToMeBox.getClientProperty("titleLabel");
        
        iOweBox = createSummaryBox("Total Debts (I Owe)", "0", new Color(220, 53, 69), "↓");
        totalIOweLabel = (JLabel) iOweBox.getClientProperty("amountLabel");
        tLabelIow = (JLabel) iOweBox.getClientProperty("titleLabel");

        summaryPanel.add(owedToMeBox);
        summaryPanel.add(iOweBox);

        // Action Button
        JButton addBtn = new JButton("+ Add New Debt/Loan");
        
        addBtn.setForeground(Color.WHITE);
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        addBtn.setOpaque(true);
        addBtn.setContentAreaFilled(true);
        addBtn.setBackground(new Color(40, 50, 60));
        addBtn.setRolloverEnabled(false);
        addBtn.setFocusPainted(false);
        addBtn.setBorderPainted(false);
        addBtn.setPreferredSize(new Dimension(0, 50));
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.addActionListener(e -> showAddDialog());

        summaryTitleLabel = new JLabel("Summary");
        summaryTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        bottomWrapper.add(summaryTitleLabel, BorderLayout.NORTH);
        bottomWrapper.add(summaryPanel, BorderLayout.CENTER);
        bottomWrapper.add(addBtn, BorderLayout.SOUTH);

        add(bottomWrapper, BorderLayout.SOUTH);

        this.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) { 
                updateThemeColors();
                loadData(); 
            }
        });
    }

    private void updateThemeColors() {
        boolean isDark = getBackground().getRed() < 100;
        
        Color titleColor = isDark ? Color.WHITE : Color.BLACK;
        mainTitle.setForeground(titleColor);
        summaryTitleLabel.setForeground(titleColor);
        
        // Translucent Glass Colors for Summary Boxes
        Color cardBg = isDark ? new Color(60, 63, 65, 140) : new Color(255, 255, 255, 160);
        
        owedToMeBox.setBackground(cardBg);
        tLabelOwed.setForeground(isDark ? new Color(180, 190, 200) : Color.DARK_GRAY);
        totalOwedToMeLabel.setForeground(titleColor);

        iOweBox.setBackground(cardBg);
        tLabelIow.setForeground(isDark ? new Color(180, 190, 200) : Color.DARK_GRAY);
        totalIOweLabel.setForeground(titleColor);
    }

    private void loadData() {
        if (!SessionManager.isLoggedIn()) return;
        cardsPanel.removeAll();
        
        boolean isDark = getBackground().getRed() < 100;
        
        double totalOwedToMe = 0;
        double totalIOwe = 0;
        
        String username = SessionManager.getCurrentUser().getUsername();
        
        String selectedFilter = (String) filterBox.getSelectedItem();
        boolean showActive = selectedFilter.contains("Active");
        boolean showSettled = selectedFilter.equals("Settled");
        boolean showLoansOnly = selectedFilter.contains("Loans");
        boolean showDebtsOnly = selectedFilter.contains("Debts");

        String sql = "SELECT * FROM debts WHERE username = ?";

        try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate today = LocalDate.now();

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("person_name");
                String type = rs.getString("debt_type");
                double amount = rs.getDouble("amount");
                double interestRate = rs.getDouble("interest_rate");
                String dateStr = rs.getString("due_date");
                String status = rs.getString("status");

                boolean isLoan = type.equals("Owes Me") || type.equals("Loan (Owed to Me)");
                double totalAmount = amount + (amount * (interestRate / 100.0));

                if (!status.equals("SETTLED")) {
                    try {
                        LocalDate dueDate = LocalDate.parse(dateStr, dtf);
                        if (dueDate.isBefore(today)) status = "Overdue";
                    } catch (Exception e) { }
                }

                if (showActive && status.equals("SETTLED")) continue;
                if (showSettled && !status.equals("SETTLED")) continue;
                if (showLoansOnly && !isLoan) continue;
                if (showDebtsOnly && isLoan) continue;

                if (!status.equals("SETTLED")) {
                    if (isLoan) totalOwedToMe += totalAmount; 
                    else totalIOwe += totalAmount;
                }

                cardsPanel.add(createCard(id, name, isLoan, amount, interestRate, totalAmount, dateStr, status, isDark));
                cardsPanel.add(Box.createRigidArea(new Dimension(20, 0)));
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        totalOwedToMeLabel.setText(String.format("%,.0f %s", totalOwedToMe, SettingsPanel.currentCurrency));
        totalIOweLabel.setText(String.format("%,.0f %s", totalIOwe, SettingsPanel.currentCurrency));
        
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    // --- UI: CREATE INDIVIDUAL GLASS CARD ---
    private JPanel createCard(int dbId, String name, boolean isLoan, double baseAmount, double interestRate, double totalAmount, String date, String status, boolean isDark) {
        JPanel card = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBackground(isDark ? new Color(60, 63, 65, 140) : new Color(255, 255, 255, 160));
        card.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        Dimension cardSize = new Dimension(350, 180);
        card.setMinimumSize(cardSize);
        card.setPreferredSize(cardSize);
        card.setMaximumSize(cardSize);
        card.setAlignmentY(Component.TOP_ALIGNMENT); 

        // 1. Top Row
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        
        JLabel nameLabel = new JLabel("👤 " + name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        nameLabel.setForeground(isDark ? Color.WHITE : Color.BLACK); 
        
        String tagText = isLoan ? "Loan (Owed to me)" : "Debt (I Owe)";
        Color tagBg = isLoan ? new Color(0, 100, 200, 40) : new Color(200, 50, 50, 40);
        Color tagFg = isLoan ? new Color(50, 130, 255) : new Color(220, 53, 69);
        JLabel typeTag = createTag(tagText, tagBg, tagFg);
        
        topRow.add(nameLabel, BorderLayout.WEST);
        topRow.add(typeTag, BorderLayout.EAST);

        // 2. Middle Row
        JPanel midRow = new JPanel(new BorderLayout());
        midRow.setOpaque(false);
        
        JLabel amtLabel = new JLabel(String.format("%,.0f %s", totalAmount, SettingsPanel.currentCurrency));
        amtLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        amtLabel.setForeground(isDark ? Color.WHITE : Color.BLACK); 
        
        String htmlColor = isDark ? "#C0C0C0" : "gray";
        String interestHtml = interestRate > 0 ? "<br>Base: " + String.format("%,.0f", baseAmount) + " (" + interestRate + "%)" : "";
        JLabel dateLabel = new JLabel("<html><div style='text-align:right; color:" + htmlColor + "; font-size:10px;'>Due Date<br><b>" + date + "</b>" + interestHtml + "</div></html>");
        
        midRow.add(amtLabel, BorderLayout.WEST);
        midRow.add(dateLabel, BorderLayout.EAST);

        // 3. Bottom Row
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        
        Color bg = new Color(180, 120, 0, 40); 
        Color fg = new Color(200, 140, 0);
        if (status.equals("Overdue")) { bg = new Color(200, 50, 50, 40); fg = new Color(220, 53, 69); }
        if (status.equals("Partially Paid")) { bg = new Color(40, 150, 40, 40); fg = new Color(40, 167, 69); }
        if (status.equals("SETTLED")) { bg = isDark ? new Color(80, 83, 85, 80) : new Color(200, 200, 200, 80); fg = isDark ? Color.LIGHT_GRAY : Color.DARK_GRAY; }
        
        JLabel statusTag = createTag(status.equals("SETTLED") ? "Settled" : status, bg, fg);
        
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        actions.setOpaque(false);
        
        JLabel settleBtn = new JLabel("<html><div style='text-align:center; color:" + htmlColor + "; font-size:9px;'>✅<br>Settle</div></html>");
        settleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        settleBtn.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { settleAccount(dbId); } });
        
        JLabel deleteBtn = new JLabel(
        	    "<html><div style='text-align:center; width:50px;'>" +
        	    "🗑<br><span style='font-size:9px; white-space:nowrap;'>Delete</span>" +
        	    "</div></html>"
        	);
        deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteBtn.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { deleteAccount(dbId); } });

        if (!status.equals("SETTLED")) actions.add(settleBtn);
        actions.add(deleteBtn);

        bottomRow.add(statusTag, BorderLayout.WEST);
        bottomRow.add(actions, BorderLayout.EAST);

        card.add(topRow, BorderLayout.NORTH);
        card.add(midRow, BorderLayout.CENTER);
        card.add(bottomRow, BorderLayout.SOUTH);

        return card;
    }

    private JLabel createTag(String text, Color bg, Color fg) {
        JLabel tag = new JLabel(" " + text + " ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tag.setFont(new Font("SansSerif", Font.BOLD, 11));
        tag.setOpaque(false);
        tag.setBackground(bg);
        tag.setForeground(fg);
        tag.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        return tag;
    }

    private JPanel createSummaryBox(String title, String defaultAmt, Color iconColor, String iconStr) {
        JPanel box = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel tLabel = new JLabel(title);
        tLabel.setForeground(Color.GRAY);
        tLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JLabel amtLabel = new JLabel(defaultAmt + " " + SettingsPanel.currentCurrency);
        amtLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        
        JLabel icon = new JLabel(" " + iconStr + " ");
        icon.setOpaque(false); // Make icon background naturally blend
        icon.setForeground(iconColor);
        icon.setFont(new Font("SansSerif", Font.BOLD, 22));
        
        box.add(tLabel, BorderLayout.NORTH);
        box.add(amtLabel, BorderLayout.CENTER);
        box.add(icon, BorderLayout.EAST);
        
        box.putClientProperty("titleLabel", tLabel); 
        box.putClientProperty("amountLabel", amtLabel); 
        return box;
    }

    // --- ACTIONS ---
    private void showAddDialog() {
        JTextField nameField = new JTextField(15);
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Loan (Owed to Me)", "Debt (I Owe)"});
        JTextField amountField = new JTextField(15);
        JTextField interestField = new JTextField("0.0", 15); 
        JTextField dateField = new JTextField(LocalDate.now().plusMonths(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        JPanel p = new JPanel(new GridLayout(5, 2, 10, 10)); 
        p.add(new JLabel("Person Name:")); p.add(nameField);
        p.add(new JLabel("Type of Record:")); p.add(typeBox);
        p.add(new JLabel("Principal Amount (" + SettingsPanel.currentCurrency + "):")); p.add(amountField);
        p.add(new JLabel("Interest Rate (%):")); p.add(interestField);
        p.add(new JLabel("Due Date (dd/MM/yyyy):")); p.add(dateField);

        if (JOptionPane.showConfirmDialog(this, p, "Add New Debt or Loan", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                String type = (String) typeBox.getSelectedItem();
                double amt = Double.parseDouble(amountField.getText().trim());
                double interest = Double.parseDouble(interestField.getText().trim()); 
                String date = dateField.getText().trim();
                String user = SessionManager.getCurrentUser().getUsername();

                String sql = "INSERT INTO debts (username, person_name, debt_type, amount, interest_rate, due_date, status) VALUES (?, ?, ?, ?, ?, ?, 'Pending')";
                try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, user); 
                    pstmt.setString(2, name); 
                    pstmt.setString(3, type);
                    pstmt.setDouble(4, amt); 
                    pstmt.setDouble(5, interest); 
                    pstmt.setString(6, date); 
                    pstmt.executeUpdate();
                }
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please check your inputs.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void settleAccount(int id) {
        if (JOptionPane.showConfirmDialog(this, "Mark this record as Settled?", "Settle", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement("UPDATE debts SET status = 'SETTLED' WHERE id = ?")) {
                pstmt.setInt(1, id); pstmt.executeUpdate();
            } catch (SQLException ex) { ex.printStackTrace(); }
            loadData();
        }
    }

    private void deleteAccount(int id) {
        if (JOptionPane.showConfirmDialog(this, "Permanently delete this record?", "Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement("DELETE FROM debts WHERE id = ?")) {
                pstmt.setInt(1, id); pstmt.executeUpdate();
            } catch (SQLException ex) { ex.printStackTrace(); }
            loadData();
        }
    }
}