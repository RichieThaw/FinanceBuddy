import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BudgetPanel extends ThemeGradientPanel { // Inherits master gradient

    private JLabel titleLabel, viewLabel;
    private JLabel allocatedLabel, spentLabel;
    private JComboBox<String> viewFilterBox;
    private JTable budgetTable;
    private DefaultTableModel tableModel;
    private JPanel alertsPanel;
    private JButton addBtn;
    
    private String currentFilterType = "ALL";
    private String currentFilterValue = "";

    public BudgetPanel() {
        initDatabase();

        setLayout(new BorderLayout(20, 20));
        setOpaque(false); // CRITICAL: Allows the master gradient to show through
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Listen for Theme Changes from SettingsPanel
        this.addPropertyChangeListener("background", evt -> {
            SwingUtilities.invokeLater(() -> {
                updateThemeColors();
                budgetTable.repaint();
            });
        });

        // --- 1. HEADER SECTION ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        titleLabel = new JLabel("Budget Management");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filterPanel.setOpaque(false);
        viewLabel = new JLabel("View:");
        filterPanel.add(viewLabel);
        
        String[] viewOptions = {"All Time", "Search by Day...", "Search by Month..."};
        viewFilterBox = new JComboBox<>(viewOptions);
        viewFilterBox.setPreferredSize(new Dimension(150, 30));
        viewFilterBox.addActionListener(e -> handleFilterSelection());
        
        filterPanel.add(viewFilterBox);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(filterPanel, BorderLayout.EAST);

        // --- 2. STATS SECTION ---
        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        allocatedLabel = new JLabel("Total Budgets: 0 MMK");
        allocatedLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        spentLabel = new JLabel("Total Spent: 0 MMK");
        spentLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        statsPanel.add(allocatedLabel);
        statsPanel.add(spentLabel);

        // --- 3. BUTTONS SECTION ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        buttonPanel.setOpaque(false);

        addBtn = new JButton("Add New Budget");
        
        addBtn.setForeground(Color.WHITE);
        addBtn.setOpaque(true);
        addBtn.setContentAreaFilled(true);
        addBtn.setBackground(new Color(40, 50, 60));
        addBtn.setRolloverEnabled(false);
        addBtn.setFocusPainted(false);
        addBtn.setBorderPainted(false);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.setPreferredSize(new Dimension(150, 35));
        addBtn.addActionListener(e -> showAddBudgetDialog());

        // 🔥 THE NUCLEAR OPTION: Manually painted button (Bulletproof for Mac/Dark Mode)
        JButton deleteBtn = new JButton("Delete Selected Budget") {
            @Override
            protected void paintComponent(Graphics g) {
                // Always paint RED background manually
                if (getModel().isPressed()) {
                    g.setColor(new Color(180, 40, 55)); // darker red when pressed
                } else {
                    g.setColor(new Color(220, 53, 69)); // normal red
                }
                g.fillRect(0, 0, getWidth(), getHeight());

                // Draw text manually
                FontMetrics fm = g.getFontMetrics();
                Rectangle r = getBounds();
                String text = getText();

                int x = (r.width - fm.stringWidth(text)) / 2;
                int y = (r.height + fm.getAscent()) / 2 - 2;

                g.setColor(Color.WHITE);
                g.setFont(getFont());
                g.drawString(text, x, y);
            }
        };

        deleteBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setContentAreaFilled(false);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setOpaque(false);
        deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteBtn.setBorder(BorderFactory.createLineBorder(new Color(180, 40, 55), 1));
        deleteBtn.setPreferredSize(new Dimension(180, 35));
        deleteBtn.addActionListener(e -> deleteSelectedBudget());
        
        buttonPanel.add(addBtn);
        buttonPanel.add(deleteBtn);

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.add(headerPanel);
        topContainer.add(statsPanel);
        topContainer.add(buttonPanel);

        // --- 4. GLASS TABLE SECTION ---
        String[] columns = {"ID", "Date", "Category", "Spent / Limit", "Progress", "Status", "RawPct"}; 
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        budgetTable = new JTable(tableModel);
        budgetTable.setRowHeight(40);
        budgetTable.setOpaque(false); // CRITICAL for glass table
        budgetTable.setBackground(new Color(0, 0, 0, 0)); // Fully transparent base
        budgetTable.setShowVerticalLines(false);
        budgetTable.setGridColor(new Color(200, 200, 200, 50)); 
        
        budgetTable.getColumnModel().getColumn(0).setMinWidth(0);
        budgetTable.getColumnModel().getColumn(0).setMaxWidth(0);
        budgetTable.getColumnModel().getColumn(6).setMinWidth(0);
        budgetTable.getColumnModel().getColumn(6).setMaxWidth(0);

        budgetTable.setDefaultRenderer(Object.class, new BudgetCellRenderer());
        budgetTable.getColumnModel().getColumn(4).setCellRenderer(new ProgressCellRenderer());
        budgetTable.getColumnModel().getColumn(4).setPreferredWidth(180);

        JScrollPane scrollPane = new JScrollPane(budgetTable);
        scrollPane.setOpaque(false); 
        scrollPane.getViewport().setOpaque(false); 
        scrollPane.getViewport().setBackground(new Color(0, 0, 0, 0)); 
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200, 80)));

        alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        alertsPanel.setOpaque(false);
        alertsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        add(topContainer, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(alertsPanel, BorderLayout.SOUTH);

        this.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) { 
                loadBudgetData(); 
                updateThemeColors();
            }
        });
    }

    private void updateThemeColors() {
        boolean isDark = getBackground().getRed() < 100;
        Color textColor = isDark ? Color.WHITE : Color.BLACK;

        titleLabel.setForeground(textColor);
        viewLabel.setForeground(textColor);
        allocatedLabel.setForeground(textColor);
        spentLabel.setForeground(textColor);

        if (addBtn != null) {
            addBtn.setBackground(new Color(40, 50, 60));
            addBtn.setForeground(Color.WHITE);
        }
    }

    private void initDatabase() {
        try (Connection conn = DatabaseHelper.connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS budgets (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), date VARCHAR(50), category VARCHAR(255), spent_amount DOUBLE, limit_amount DOUBLE)");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleFilterSelection() {
        String selected = (String) viewFilterBox.getSelectedItem();
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner spinner = new JSpinner(model);

        if ("Search by Day...".equals(selected)) {
            spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
            if (JOptionPane.showConfirmDialog(this, spinner, "Select Day", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                currentFilterType = "DAY";
                currentFilterValue = new SimpleDateFormat("dd/MM/yyyy").format((Date)spinner.getValue());
                loadBudgetData();
            } else { viewFilterBox.setSelectedIndex(0); }
        } 
        else if ("Search by Month...".equals(selected)) {
            spinner.setEditor(new JSpinner.DateEditor(spinner, "MM/yyyy"));
            if (JOptionPane.showConfirmDialog(this, spinner, "Select Month", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                currentFilterType = "MONTH";
                currentFilterValue = new SimpleDateFormat("MM/yyyy").format((Date)spinner.getValue());
                loadBudgetData();
            } else { viewFilterBox.setSelectedIndex(0); }
        } 
        else {
            currentFilterType = "ALL";
            currentFilterValue = "";
            loadBudgetData();
        }
    }

    private void loadBudgetData() {
        if (!SessionManager.isLoggedIn()) return;
        tableModel.setRowCount(0);
        alertsPanel.removeAll();
        
        String username = SessionManager.getCurrentUser().getUsername();
        double totalAllocated = 0, totalSpent = 0;
        String query = "SELECT * FROM budgets WHERE username = ?";
        if (currentFilterType.equals("DAY")) query += " AND date = ?";
        else if (currentFilterType.equals("MONTH")) query += " AND date LIKE ?";

        try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            if (currentFilterType.equals("DAY")) pstmt.setString(2, currentFilterValue);
            else if (currentFilterType.equals("MONTH")) pstmt.setString(2, "%" + currentFilterValue);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String date = rs.getString("date");
                String cat = rs.getString("category");
                double s = rs.getDouble("spent_amount");
                double l = rs.getDouble("limit_amount");
                int pct = (l > 0) ? (int)((s/l)*100) : 0;

                totalAllocated += l;
                totalSpent += s;

                tableModel.addRow(new Object[]{id, date, cat, String.format("%,.0f / %,.0f", s, l), pct, pct > 100 ? "Over" : "Under", pct});

                if (pct > 100) {
                    JLabel al = new JLabel("⚠️ Overdue: " + cat + " is over by " + String.format("%,.0f", (s-l)) + " MMK!");
                    al.setFont(new Font("SansSerif", Font.BOLD, 16));
                    al.setForeground(new Color(255, 0, 0)); 
                    alertsPanel.add(al);
                }
            }
            allocatedLabel.setText(String.format("Total Budgets: %,.0f %s", totalAllocated, SettingsPanel.currentCurrency));
            spentLabel.setText(String.format("Total Spent: %,.0f %s", totalSpent, SettingsPanel.currentCurrency));
            alertsPanel.revalidate(); alertsPanel.repaint();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAddBudgetDialog() {
        JTextField cat = new JTextField();
        JTextField lim = new JTextField();
        JTextField spentField = new JTextField("0"); 
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        JTextField dateField = new JTextField(sdf.format(new Date()));

        JPanel p = new JPanel(new GridLayout(4, 2, 10, 10)); 
        p.add(new JLabel("Category:")); 
        p.add(cat);
        p.add(new JLabel("Limit:")); 
        p.add(lim);
        p.add(new JLabel("Current Spent:")); 
        p.add(spentField);                   
        p.add(new JLabel("Date (DD/MM/YYYY):")); 
        p.add(dateField);

        if (JOptionPane.showConfirmDialog(this, p, "Add Budget", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try (Connection conn = DatabaseHelper.connect(); 
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO budgets (username, date, category, limit_amount, spent_amount) VALUES (?, ?, ?, ?, ?)")) {
                
                ps.setString(1, SessionManager.getCurrentUser().getUsername());
                ps.setString(2, dateField.getText()); 
                ps.setString(3, cat.getText());
                ps.setDouble(4, Double.parseDouble(lim.getText()));
                ps.setDouble(5, Double.parseDouble(spentField.getText())); 
                
                ps.executeUpdate();
                loadBudgetData();
            } catch (Exception e) { 
                JOptionPane.showMessageDialog(this, "Error: Please check your input format.");
                e.printStackTrace(); 
            }
        }
    }

    private void deleteSelectedBudget() {
        int row = budgetTable.getSelectedRow();
        if (row == -1) return;
        int id = (int) tableModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Delete selected budget?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseHelper.connect(); PreparedStatement ps = conn.prepareStatement("DELETE FROM budgets WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                loadBudgetData();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // --- TRANSLUCENT GLASS CELL RENDERER ---
    private class BudgetCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            int pct = (int) table.getModel().getValueAt(row, 6);
            boolean isDark = BudgetPanel.this.getBackground().getRed() < 100;
            
            if (!isSelected) {
                if (pct > 100) {
                    // Translucent Red for Over Budget
                    c.setBackground(isDark ? new Color(255, 100, 100, 50) : new Color(255, 100, 100, 80));
                } else {
                    // Alternating Glass Colors
                    if (row % 2 == 0) {
                        c.setBackground(isDark ? new Color(255, 255, 255, 20) : new Color(255, 255, 255, 140)); 
                    } else {
                        c.setBackground(isDark ? new Color(255, 255, 255, 5) : new Color(255, 255, 255, 70)); 
                    }
                }
                
                if (col == 5) c.setForeground(pct > 100 ? (isDark ? new Color(255, 100, 100) : Color.RED) : (isDark ? new Color(100, 255, 100) : new Color(40, 167, 69)));
                else c.setForeground(isDark ? Color.WHITE : Color.BLACK);
            } else {
                c.setBackground(new Color(50, 130, 255, 150)); // Glassy selection
                c.setForeground(Color.WHITE);
            }
            
            if (c instanceof JComponent) {
                ((JComponent) c).setOpaque(true); 
            }
            return c;
        }
    }

    // --- TRANSLUCENT PROGRESS BAR RENDERER (FIXED COLORS) ---
    private class ProgressCellRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private JProgressBar progressBar;
        private JLabel percentLabel;

        public ProgressCellRenderer() { 
            setLayout(new BorderLayout(10, 0)); // Adds a gap between the bar and the text
            setOpaque(true); // Needed to paint the custom row background
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            progressBar = new JProgressBar(0, 100); 
            progressBar.setStringPainted(false); // Hide text inside the bar
            progressBar.setOpaque(false);
            
            // 🔥 CRITICAL FOR MAC: Removes the OS override so custom colors actually show up!
            progressBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI());
            
            percentLabel = new JLabel();
            percentLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            percentLabel.setPreferredSize(new Dimension(45, 20)); // Ensure text doesn't jitter
            
            add(progressBar, BorderLayout.CENTER);
            add(percentLabel, BorderLayout.EAST); // Place text beside the bar
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            int pct = (int) value;
            progressBar.setValue(Math.min(pct, 100));
            percentLabel.setText(pct + "%");
            
            // --- DYNAMIC COLOR SWITCHING (Stronger Blue added here) ---
            if (pct > 100) {
                progressBar.setForeground(new Color(220, 53, 69)); // Red Bar
                percentLabel.setForeground(new Color(255, 80, 80)); // Red Text
            } else {
                progressBar.setForeground(new Color(0, 122, 255)); // Blue Bar
                percentLabel.setForeground(new Color(50, 150, 255)); // Strong Blue Text
            }
            
            // Replicate the BudgetCellRenderer background logic so it blends in perfectly
            boolean isDark = BudgetPanel.this.getBackground().getRed() < 100;
            
            if (!isSelected) {
                if (pct > 100) {
                    setBackground(isDark ? new Color(255, 100, 100, 50) : new Color(255, 100, 100, 80));
                } else {
                    if (row % 2 == 0) {
                        setBackground(isDark ? new Color(255, 255, 255, 20) : new Color(255, 255, 255, 140)); 
                    } else {
                        setBackground(isDark ? new Color(255, 255, 255, 5) : new Color(255, 255, 255, 70)); 
                    }
                }
            } else {
                setBackground(new Color(50, 130, 255, 150)); // Glassy selection
            }
            
            return this;
        }
    }
}