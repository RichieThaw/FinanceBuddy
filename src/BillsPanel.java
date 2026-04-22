import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class BillsPanel extends ThemeGradientPanel { // Inherits master gradient

    private DefaultTableModel tableModel;
    private JTable table;
    private JPanel remindersPanel;
    private JLabel mainTitle;
    private JLabel remTitle;

    public BillsPanel() {
        setLayout(new BorderLayout(20, 20));
        setOpaque(false); // Let gradient show through
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        this.addPropertyChangeListener("background", evt -> {
            SwingUtilities.invokeLater(() -> {
                updateThemeColors();
                table.repaint();
                loadBills(); // Reload to refresh reminder panel colors
            });
        });

        // --- TOP HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        mainTitle = new JLabel("Bill Reminders");
        mainTitle.setFont(new Font("SansSerif", Font.BOLD, 28));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        actionPanel.setOpaque(false);

        // Add Bill Button
        JButton addBillBtn = new JButton("+ Add Bill");
        
        addBillBtn.setForeground(Color.WHITE);
        addBillBtn.setFont(new Font("SansSerif", Font.PLAIN, 16));
        addBillBtn.setFocusPainted(false);
        addBillBtn.setOpaque(true);
        addBillBtn.setContentAreaFilled(true);
        addBillBtn.setBackground(new Color(40, 50, 60));
        addBillBtn.setRolloverEnabled(false);
        addBillBtn.setFocusPainted(false);
        addBillBtn.setBorderPainted(false);
        addBillBtn.setPreferredSize(new Dimension(120, 40));
        addBillBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBillBtn.addActionListener(e -> showAddBillDialog());

        actionPanel.add(addBillBtn);

        headerPanel.add(mainTitle, BorderLayout.WEST);
        headerPanel.add(actionPanel, BorderLayout.EAST);

        // --- GLASS TABLE ---
        String[] columns = {"Biller", "Amount", "Due Date", "Reminder", "Delete", "DB_ID", "IsOverdue"};
        tableModel = new DefaultTableModel(new Object[][]{}, columns) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; } 
        };
        
        table = new JTable(tableModel);
        table.setRowHeight(45);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setShowVerticalLines(false);
        table.setOpaque(false); // Make table transparent
        table.setBackground(new Color(0, 0, 0, 0)); 
        table.setGridColor(new Color(200, 200, 200, 50));
        table.setIntercellSpacing(new Dimension(0, 0));
        
        table.getColumnModel().getColumn(5).setMinWidth(0);
        table.getColumnModel().getColumn(5).setMaxWidth(0);
        table.getColumnModel().getColumn(6).setMinWidth(0);
        table.getColumnModel().getColumn(6).setMaxWidth(0);

        // Auto-Highlight Overdue Rows with Glass Effect
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                boolean isDark = BillsPanel.this.getBackground().getRed() < 100;
                
                boolean isOverdue = false;
                if (table.getModel().getRowCount() > row) {
                    isOverdue = (boolean) table.getModel().getValueAt(row, 6);
                }

                if (isOverdue) {
                    // Translucent Red for overdue bills
                    c.setBackground(isDark ? new Color(255, 100, 100, 50) : new Color(255, 100, 100, 80));
                    c.setForeground(isDark ? new Color(255, 100, 100) : new Color(180, 40, 40));  
                } else {
                    if (!isSelected) {
                        // Alternating glass rows
                        if (row % 2 == 0) {
                            c.setBackground(isDark ? new Color(255, 255, 255, 20) : new Color(255, 255, 255, 140)); 
                        } else {
                            c.setBackground(isDark ? new Color(255, 255, 255, 5) : new Color(255, 255, 255, 70)); 
                        }
                    } else {
                        c.setBackground(new Color(50, 130, 255, 150)); 
                    }
                    c.setForeground(isDark && !isSelected ? Color.WHITE : Color.BLACK);
                    if (isSelected) c.setForeground(Color.WHITE);
                }
                
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); 
                if (c instanceof JComponent) {
                    ((JComponent) c).setOpaque(true);
                }
                return c;
            }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int col = table.columnAtPoint(evt.getPoint());
                if (col == 4) { // Delete column
                    int row = table.rowAtPoint(evt.getPoint());
                    int dbId = (int) tableModel.getValueAt(row, 5);
                    deleteBill(dbId); 
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(new Color(0,0,0,0));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200, 80), 1, true));

        // --- REMINDERS SECTION ---
        JPanel bottomWrapper = new JPanel(new BorderLayout());
        bottomWrapper.setOpaque(false);
        
        remTitle = new JLabel("Late & Upcoming Reminders");
        remTitle.setFont(new Font("SansSerif", Font.PLAIN, 20));
        remTitle.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        bottomWrapper.add(remTitle, BorderLayout.NORTH);

        remindersPanel = new JPanel();
        remindersPanel.setLayout(new BoxLayout(remindersPanel, BoxLayout.Y_AXIS));
        remindersPanel.setOpaque(false);
        
        bottomWrapper.add(remindersPanel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomWrapper, BorderLayout.SOUTH);

        this.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) { 
                updateThemeColors();
                loadBills(); 
            }
        });
    }

    private void updateThemeColors() {
        boolean isDark = getBackground().getRed() < 100;
        Color textColor = isDark ? Color.WHITE : Color.BLACK;
        mainTitle.setForeground(textColor);
        remTitle.setForeground(textColor);
    }

    private void loadBills() {
        if (!SessionManager.isLoggedIn()) return;
        tableModel.setRowCount(0);
        remindersPanel.removeAll();

        String username = SessionManager.getCurrentUser().getUsername();
        String sql = "SELECT * FROM bills WHERE username = ? ORDER BY due_date ASC";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd MMM");

            while (rs.next()) {
                int id = rs.getInt("id");
                String biller = rs.getString("biller");
                double amount = rs.getDouble("amount");
                String dateStr = rs.getString("due_date");
                int remDays = rs.getInt("reminder_days");

                String displayDate = dateStr;
                String reminderText = remDays + " days before due";
                boolean isOverdue = false;
                
                try {
                    LocalDate dueDate = LocalDate.parse(dateStr, dbFormatter);
                    displayDate = dueDate.format(displayFormatter);
                    long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
                    
                    if (daysUntilDue < 0) {
                        isOverdue = true;
                        long daysLate = Math.abs(daysUntilDue);
                        String msg = biller + " is " + daysLate + (daysLate == 1 ? " day " : " days ") + "late!";
                        addReminderRow("⚠️ Overdue:", msg, true); 
                        
                    } else if (daysUntilDue <= remDays) {
                        String dayName = dueDate.getDayOfWeek().name();
                        dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1).toLowerCase();
                        String msg = biller + " due " + (daysUntilDue == 0 ? "today!" : (daysUntilDue == 1 ? "tomorrow." : "in " + daysUntilDue + " days."));
                        addReminderRow(dayName + ":", msg, false); 
                    }
                } catch (Exception e) {}

                String amtStr = String.format("%,.0f %s", amount, SettingsPanel.currentCurrency);
                tableModel.addRow(new Object[]{biller, amtStr, displayDate, reminderText, "🗑️", id, isOverdue});
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        remindersPanel.revalidate();
        remindersPanel.repaint();
    }

    private void addReminderRow(String labelText, String message, boolean isLate) {
        boolean isDark = getBackground().getRed() < 100;
        
        JPanel row = new JPanel(new BorderLayout());
        // Translucent background for each reminder row
        row.setBackground(isDark ? new Color(255, 255, 255, 10) : new Color(255, 255, 255, 120));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200, 80)),
            BorderFactory.createEmptyBorder(10, 5, 10, 5)
        ));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setOpaque(false);
        
        Color normalText = isDark ? Color.WHITE : Color.DARK_GRAY;
        Color lateText = isDark ? new Color(255, 100, 100) : new Color(180, 40, 40);
        Color textColor = isLate ? lateText : normalText;
        
        JLabel dayLabel = new JLabel(labelText);
        dayLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        dayLabel.setPreferredSize(new Dimension(110, 20));
        dayLabel.setForeground(textColor);
        
        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        msgLabel.setForeground(textColor);
        
        leftPanel.add(dayLabel);
        leftPanel.add(msgLabel);

        row.add(leftPanel, BorderLayout.WEST);
        remindersPanel.add(row);
    }

    private void showAddBillDialog() {
        JTextField billerField = new JTextField(15);
        JTextField amountField = new JTextField(15);
        JTextField dateField = new JTextField(LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        String[] remOpts = {"1", "2", "3", "5", "7"};
        JComboBox<String> remBox = new JComboBox<>(remOpts);

        JPanel p = new JPanel(new GridLayout(4, 2, 10, 10));
        p.add(new JLabel("Biller (e.g., Internet):")); p.add(billerField);
        p.add(new JLabel("Amount:")); p.add(amountField);
        p.add(new JLabel("Due Date (dd/MM/yyyy):")); p.add(dateField);
        p.add(new JLabel("Remind me X days before:")); p.add(remBox);

        if (JOptionPane.showConfirmDialog(this, p, "Add New Bill", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                String biller = billerField.getText().trim();
                double amt = Double.parseDouble(amountField.getText().trim());
                String date = dateField.getText().trim();
                int remDays = Integer.parseInt((String) remBox.getSelectedItem());
                String user = SessionManager.getCurrentUser().getUsername();

                String sql = "INSERT INTO bills (username, biller, amount, due_date, reminder_days, status) VALUES (?, ?, ?, ?, ?, 'PENDING')";
                try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, user); pstmt.setString(2, biller); pstmt.setDouble(3, amt);
                    pstmt.setString(4, date); pstmt.setInt(5, remDays); pstmt.executeUpdate();
                }
                loadBills();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please check your inputs.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteBill(int dbId) {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this bill?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement("DELETE FROM bills WHERE id = ?")) {
                pstmt.setInt(1, dbId); pstmt.executeUpdate();
            } catch (SQLException ex) { ex.printStackTrace(); }
            loadBills();
        }
    }
}