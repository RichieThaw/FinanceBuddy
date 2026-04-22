import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ExpenseIncomePanel extends ThemeGradientPanel { // Inherits master gradient

    private JLabel totalIncLabel;
    private JLabel totalExpLabel;
    private JComboBox<String> timeFilterBox;
    private DefaultTableModel tableModel;
    private JTable table;
    
    // UI Elements
    private JPanel incCard;
    private JPanel expCard;
    private JLabel addTxTitle;
    private JLabel mainTitleLabel;
    private JLabel filterLabel;
    private JLabel[] formLabels = new JLabel[4];
    
    // Promoted buttons so we can strictly lock their colors
    private JButton addBtn;
    private JButton deleteBtn;
    
    private String customSearchDate = null;

    public ExpenseIncomePanel() {
        setLayout(new BorderLayout(20, 20));
        setOpaque(false); // CRITICAL: Allows the master gradient to show through
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        this.addPropertyChangeListener("background", evt -> {
            SwingUtilities.invokeLater(() -> {
                updateThemeColors();
                table.repaint(); 
            });
        });

        // --- 1. TOP HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        mainTitleLabel = new JLabel("Transactions");
        mainTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        
        timeFilterBox = new JComboBox<>(new String[]{"All Time", "Search by Day...", "Search by Month..."});
        timeFilterBox.addActionListener(e -> {
            String selected = (String) timeFilterBox.getSelectedItem();
            if ("Search by Day...".equals(selected)) {
                String input = JOptionPane.showInputDialog(this, 
                    "Enter exact date to search (dd/MM/yyyy):", 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                if (input != null && !input.trim().isEmpty()) {
                    customSearchDate = input.trim();
                    loadDatabaseData();
                } else timeFilterBox.setSelectedItem("All Time");
            } else if ("Search by Month...".equals(selected)) {
                String input = JOptionPane.showInputDialog(this, 
                    "Enter month and year to search (MM/yyyy):", 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy")));
                if (input != null && !input.trim().isEmpty()) {
                    customSearchDate = input.trim();
                    loadDatabaseData();
                } else timeFilterBox.setSelectedItem("All Time");
            } else { 
                customSearchDate = null;
                loadDatabaseData();
            }
        });

        JPanel filterP = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filterP.setOpaque(false);
        filterLabel = new JLabel("View:");
        filterP.add(filterLabel);
        filterP.add(timeFilterBox);

        headerPanel.add(mainTitleLabel, BorderLayout.WEST);
        headerPanel.add(filterP, BorderLayout.EAST);

        // --- 2. SUMMARY CARDS (Glassmorphism) ---
        JPanel summaryPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        summaryPanel.setOpaque(false);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        incCard = createGlassCard();
        totalIncLabel = new JLabel("Total Incomes: 0 MMK", SwingConstants.CENTER);
        totalIncLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        incCard.add(totalIncLabel, BorderLayout.CENTER);

        expCard = createGlassCard();
        totalExpLabel = new JLabel("Total Expenses: 0 MMK", SwingConstants.CENTER);
        totalExpLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        expCard.add(totalExpLabel, BorderLayout.CENTER);

        summaryPanel.add(incCard);
        summaryPanel.add(expCard);

        // --- 3. ADD TRANSACTION FORM ---
        JPanel addTxPanel = new JPanel();
        addTxPanel.setLayout(new BoxLayout(addTxPanel, BoxLayout.Y_AXIS));
        addTxPanel.setOpaque(false);

        addTxTitle = new JLabel("Add Transaction");
        addTxTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        addTxTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        addTxPanel.add(addTxTitle);
        addTxPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel formGrid = new JPanel(new GridLayout(2, 5, 10, 5));
        formGrid.setOpaque(false);

        formLabels[0] = new JLabel("Amount");
        formLabels[1] = new JLabel("Category");
        formLabels[2] = new JLabel("Type");
        formLabels[3] = new JLabel("Date (dd/MM/yyyy)");

        for (JLabel lbl : formLabels) {
            formGrid.add(lbl);
        }
        formGrid.add(new JLabel("")); // Empty spot above button

        JTextField amountField = new JTextField();
        JTextField catField = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Income", "Expense"});
        JTextField dateField = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        
        addBtn = new JButton("Add");
        
        addBtn.setForeground(Color.WHITE);
        addBtn.setOpaque(true);
        addBtn.setContentAreaFilled(true);
        addBtn.setBackground(new Color(40, 50, 60));
        addBtn.setRolloverEnabled(false);
        addBtn.setFocusPainted(false);
        addBtn.setBorderPainted(false);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.addActionListener(e -> {
            addTransaction(amountField.getText(), catField.getText(), (String) typeBox.getSelectedItem(), dateField.getText());
            amountField.setText(""); catField.setText("");
        });

        formGrid.add(amountField);
        formGrid.add(catField);
        formGrid.add(typeBox);
        formGrid.add(dateField);
        formGrid.add(addBtn);

        addTxPanel.add(formGrid);
        addTxPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // --- 4. DATA TABLE (Glass Effect) ---
        String[] cols = {"Date", "Type", "Category", "Amount", "DB_ID"};
        tableModel = new DefaultTableModel(new Object[][]{}, cols) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                boolean isDark = ExpenseIncomePanel.this.getBackground().getRed() < 100;
                
                if (!isRowSelected(row)) {
                    // Frosty translucent rows!
                    if (row % 2 == 0) {
                        c.setBackground(isDark ? new Color(255, 255, 255, 20) : new Color(255, 255, 255, 140)); 
                    } else {
                        c.setBackground(isDark ? new Color(255, 255, 255, 5) : new Color(255, 255, 255, 70)); 
                    }
                    c.setForeground(isDark ? Color.WHITE : Color.BLACK); 
                } else {
                    c.setBackground(new Color(50, 130, 255, 150)); // Glassy selection color
                    c.setForeground(Color.WHITE);
                }
                
                if (c instanceof JComponent) {
                    ((JComponent) c).setOpaque(true); // Required for the alpha colors to render properly
                }
                return c;
            }
        };
        
        table.setRowHeight(35); 
        table.setOpaque(false); // CRITICAL for glass table
        table.setBackground(new Color(0, 0, 0, 0)); // Fully transparent base
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(200, 200, 200, 50)); // Subtle grid
        
        table.getColumnModel().getColumn(4).setMinWidth(0);
        table.getColumnModel().getColumn(4).setMaxWidth(0);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false); // Remove the giant white box!
        scrollPane.getViewport().setOpaque(false); 
        scrollPane.getViewport().setBackground(new Color(0, 0, 0, 0)); 
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200, 80)));
        
        // --- 5. DELETE BUTTON ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        
        deleteBtn = new JButton("Delete Selected Transaction");
        deleteBtn.setBackground(new Color(200, 50, 50));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setOpaque(true);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteBtn.addActionListener(e -> deleteSelectedTransaction());
        bottomPanel.add(deleteBtn);

        // Combine everything
        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setOpaque(false);
        topWrapper.add(headerPanel, BorderLayout.NORTH);
        topWrapper.add(summaryPanel, BorderLayout.CENTER);
        topWrapper.add(addTxPanel, BorderLayout.SOUTH);

        add(topWrapper, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        this.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) { 
                loadDatabaseData(); 
                updateThemeColors();
            }
        });
    }

    // --- HELPER: Glass Card Generator ---
    private JPanel createGlassCard() {
        JPanel panel = new JPanel(new BorderLayout()) {
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
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    // --- LOGIC METHODS ---

    private void updateThemeColors() {
        boolean isDark = getBackground().getRed() < 100;
        Color textColor = isDark ? Color.WHITE : Color.BLACK;

        mainTitleLabel.setForeground(textColor);
        addTxTitle.setForeground(textColor);
        filterLabel.setForeground(textColor);
        
        for (JLabel lbl : formLabels) {
            if (lbl != null) lbl.setForeground(textColor);
        }

        // Glass colors for Summary Cards
        Color incBg = isDark ? new Color(40, 180, 80, 120) : new Color(40, 180, 80, 80);
        Color expBg = isDark ? new Color(230, 70, 70, 120) : new Color(230, 70, 70, 80);

        incCard.setBackground(incBg);
        totalIncLabel.setForeground(isDark ? new Color(180, 255, 180) : new Color(0, 80, 0)); 
        
        expCard.setBackground(expBg);
        totalExpLabel.setForeground(isDark ? new Color(255, 180, 180) : new Color(120, 0, 0)); 
        
        if (addBtn != null) {
            addBtn.setBackground(new Color(40, 50, 60));
            addBtn.setForeground(Color.WHITE);
        }
        if (deleteBtn != null) {
            deleteBtn.setBackground(new Color(200, 50, 50)); 
            deleteBtn.setForeground(Color.WHITE);
        }
    }

    private boolean checkDateFilter(String dateStr) {
        String filter = (String) timeFilterBox.getSelectedItem();
        if ("All Time".equals(filter)) return true;
        if (("Search by Day...".equals(filter) || "Search by Month...".equals(filter)) && customSearchDate != null) {
            return dateStr.contains(customSearchDate);
        }
        return true;
    }

    private void loadDatabaseData() {
        if (!SessionManager.isLoggedIn()) return;
        tableModel.setRowCount(0);
        
        double totalInc = 0;
        double totalExp = 0;
        
        String username = SessionManager.getCurrentUser().getUsername();
        String sql = "SELECT * FROM expenses WHERE username = ?";

        try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String date = rs.getString("date");
                String type = rs.getString("type");
                String category = rs.getString("category");
                double amount = rs.getDouble("amount");

                if (checkDateFilter(date)) {
                    if (type.equals("Income")) totalInc += amount;
                    else totalExp += amount;

                    String formattedAmt = String.format("%,.0f %s", amount, SettingsPanel.currentCurrency);
                    tableModel.addRow(new Object[]{date, type, category, formattedAmt, id});
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        totalIncLabel.setText(String.format("Income: %,.0f %s", totalInc, SettingsPanel.currentCurrency));
        totalExpLabel.setText(String.format("Expenses: %,.0f %s", totalExp, SettingsPanel.currentCurrency));
    }

    private void addTransaction(String amtStr, String category, String type, String date) {
        if (amtStr.isEmpty() || category.isEmpty() || date.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
            return;
        }

        try {
            double amount = Double.parseDouble(amtStr.trim());
            String username = SessionManager.getCurrentUser().getUsername();

            String sql = "INSERT INTO expenses (username, date, type, category, amount) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, date);
                pstmt.setString(3, type);
                pstmt.setString(4, category);
                pstmt.setDouble(5, amount);
                pstmt.executeUpdate();
            }
            loadDatabaseData();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for amount.");
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void deleteSelectedTransaction() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            int confirm = JOptionPane.showConfirmDialog(this, "Delete this transaction?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                int dbId = (int) tableModel.getValueAt(row, 4);
                try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement("DELETE FROM expenses WHERE id = ?")) {
                    pstmt.setInt(1, dbId);
                    pstmt.executeUpdate();
                } catch (SQLException ex) { ex.printStackTrace(); }
                loadDatabaseData();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a transaction to delete.");
        }
    }
}