import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavingPanel extends ThemeGradientPanel { // Inherits master gradient

    private List<SavingGoal> goalsList;
    private JPanel goalsPanel;
    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel mainTitle;
    private JButton deleteTransBtn; 
    
    // Setup for dynamic category coloring (Now using translucent glass colors!)
    private Map<String, Color> categoryColors;
    private int colorIndex = 0;
    private final Color[] softPalette = {
        new Color(150, 200, 255, 80), // Glass Blue
        new Color(150, 255, 150, 80), // Glass Green
        new Color(255, 255, 150, 80), // Glass Yellow
        new Color(255, 180, 180, 80), // Glass Pink
        new Color(220, 180, 255, 80)  // Glass Purple
    };

    public SavingPanel() {
        goalsList = new ArrayList<>();
        categoryColors = new HashMap<>();
        
        setLayout(new BorderLayout(20, 20));
        setOpaque(false); // Let gradient show through
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        this.addPropertyChangeListener("background", evt -> {
            SwingUtilities.invokeLater(() -> {
                updateThemeColors();
                refreshGoalsUI();
                table.repaint(); 
            });
        });

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        mainTitle = new JLabel("Saving Goals & Plans");
        mainTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        mainTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel contentWrapper = new JPanel();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.setOpaque(false);
        
        goalsPanel = new JPanel();
        goalsPanel.setLayout(new BoxLayout(goalsPanel, BoxLayout.X_AXIS));
        goalsPanel.setOpaque(false);
        
        contentWrapper.add(goalsPanel);
        contentWrapper.add(Box.createRigidArea(new Dimension(0, 15)));
        
        JScrollPane goalsScroll = new JScrollPane(contentWrapper);
        goalsScroll.setOpaque(false);
        goalsScroll.getViewport().setOpaque(false);
        goalsScroll.getViewport().setBackground(new Color(0,0,0,0));
        
        goalsScroll.setPreferredSize(new Dimension(0, 200));
        goalsScroll.setMinimumSize(new Dimension(0, 200));
        goalsScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        goalsScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        
        goalsScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(200, 200, 200, 80)), "Category Progress & Balances"));
        goalsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        goalsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        goalsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        goalsScroll.getHorizontalScrollBar().setUnitIncrement(16);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        actionPanel.setOpaque(false);
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addGoalBtn = createBlackButton("Set New Goal");
        addGoalBtn.addActionListener(e -> showAddGoalDialog());
        JButton addSavingBtn = createBlackButton("Add Saving to Goal");
        addSavingBtn.addActionListener(e -> showAddSavingDialog());

        actionPanel.add(addGoalBtn); 
        actionPanel.add(Box.createRigidArea(new Dimension(15, 0))); 
        actionPanel.add(addSavingBtn);

        topContainer.add(mainTitle); 
        topContainer.add(Box.createRigidArea(new Dimension(0, 15)));
        topContainer.add(goalsScroll); 
        topContainer.add(Box.createRigidArea(new Dimension(0, 5)));
        topContainer.add(actionPanel);

        // --- GLASS TABLE ---
        String[] columns = {"Date", "Goal Category", "Description", "Amount", "Raw Value", "DB_ID"};
        tableModel = new DefaultTableModel(new Object[][]{}, columns) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                boolean isDark = SavingPanel.this.getBackground().getRed() < 100;
                
                if (!isRowSelected(row)) {
                    String category = (String) getModel().getValueAt(row, 1); 
                    if (category != null) {
                        if (!categoryColors.containsKey(category)) {
                            categoryColors.put(category, softPalette[colorIndex % softPalette.length]);
                            colorIndex++;
                        }
                        c.setBackground(categoryColors.get(category));
                    } else {
                        c.setBackground(isDark ? new Color(255, 255, 255, 15) : new Color(255, 255, 255, 100));
                    }
                    c.setForeground(isDark ? Color.WHITE : Color.BLACK);
                } else {
                    c.setBackground(new Color(50, 130, 255, 150));
                    c.setForeground(Color.WHITE);
                }
                
                if (c instanceof JComponent) {
                    ((JComponent) c).setOpaque(true);
                }
                return c;
            }
        };
        
        table.setRowHeight(35); 
        table.setOpaque(false);
        table.setBackground(new Color(0, 0, 0, 0)); 
        table.setShowVerticalLines(false);
        
        table.getColumnModel().getColumn(4).setMinWidth(0); 
        table.getColumnModel().getColumn(4).setMaxWidth(0); 
        table.getColumnModel().getColumn(5).setMinWidth(0); 
        table.getColumnModel().getColumn(5).setMaxWidth(0); 
        
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(200, 200, 200, 80)), "Transaction List"));
        tableScroll.setOpaque(false);
        tableScroll.getViewport().setOpaque(false);
        tableScroll.getViewport().setBackground(new Color(0, 0, 0, 0)); 

        deleteTransBtn = new JButton("Delete Selected Transaction"); 
        deleteTransBtn.setBackground(new Color(200, 50, 50));
        deleteTransBtn.setForeground(Color.WHITE); 
        deleteTransBtn.setOpaque(true); 
        deleteTransBtn.setBorderPainted(false);
        deleteTransBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteTransBtn.addActionListener(e -> deleteSelectedTransaction());
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); 
        bottomPanel.setOpaque(false); 
        bottomPanel.add(deleteTransBtn);

        add(topContainer, BorderLayout.NORTH); 
        add(tableScroll, BorderLayout.CENTER); 
        add(bottomPanel, BorderLayout.SOUTH);
        
        this.addComponentListener(new ComponentAdapter() { 
            public void componentShown(ComponentEvent e) { 
                updateThemeColors();
                loadUserData(); 
            } 
        });
    }
    
    private void updateThemeColors() {
        boolean isDark = getBackground().getRed() < 100;
        mainTitle.setForeground(isDark ? Color.WHITE : Color.BLACK);
        
        if (deleteTransBtn != null) {
            deleteTransBtn.setBackground(new Color(200, 50, 50));
            deleteTransBtn.setForeground(Color.WHITE);
        }
    }

    private void loadUserData() {
        if (!SessionManager.isLoggedIn()) return;
        String username = SessionManager.getCurrentUser().getUsername();
        
        categoryColors.clear();
        colorIndex = 0;
        
        goalsList.clear();
        String sqlGoals = "SELECT * FROM goals WHERE username = ?";
        try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement(sqlGoals)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                SavingGoal sg = new SavingGoal(rs.getString("name"), rs.getDouble("target_amount"));
                sg.currentAmount = rs.getDouble("current_amount");
                sg.dbId = rs.getInt("id");
                sg.date = rs.getString("date");
                goalsList.add(sg);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        tableModel.setRowCount(0);
        String sqlTrans = "SELECT * FROM savings_transactions WHERE username = ?";
        try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement(sqlTrans)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                tableModel.addRow(new Object[]{rs.getString("date"), rs.getString("goal_name"), rs.getString("description"), 
                String.format("%,.0f %s", rs.getDouble("amount"), SettingsPanel.currentCurrency), rs.getDouble("amount"), id});
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        refreshGoalsUI();
    }

    private void refreshGoalsUI() {
        goalsPanel.removeAll(); 
        boolean isDark = getBackground().getRed() < 100;
        if (goalsList.isEmpty()) {
            JLabel emptyMsg = new JLabel("No goals set yet. Click 'Set New Goal' to begin!");
            emptyMsg.setForeground(isDark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
            goalsPanel.add(emptyMsg);
        } else {
            for (SavingGoal goal : goalsList) {
                goalsPanel.add(createGoalCard(goal, isDark));
                goalsPanel.add(Box.createRigidArea(new Dimension(15, 0))); 
            }
        }
        goalsPanel.revalidate(); goalsPanel.repaint();
    }

    // Creates a Glassmorphism Card for each goal
    private JPanel createGoalCard(SavingGoal goal, boolean isDark) {
        JPanel p = new JPanel() {
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
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false); // Important for glass effect
        p.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        
        // Translucent background
        Color cardBg = isDark ? new Color(60, 63, 65, 140) : new Color(255, 255, 255, 160); 
        p.setBackground(cardBg); 
        
        Dimension cardSize = new Dimension(320, 160);
        p.setMinimumSize(cardSize);
        p.setPreferredSize(cardSize);
        p.setMaximumSize(cardSize);
        p.setAlignmentY(Component.TOP_ALIGNMENT); 

        JPanel headerRow = new JPanel(new BorderLayout()); 
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        String dateColor = isDark ? "#A0A0A0" : "#707070";
        JLabel nameLabel = new JLabel("<html><b>" + goal.name + "</b> <font color='" + dateColor + "' size='2'>&nbsp;Created: " + goal.date + "</font></html>"); 
        nameLabel.setForeground(isDark ? Color.WHITE : Color.BLACK);
        
        JButton delGoalBtn = new JButton("✖"); 
        delGoalBtn.setForeground(new Color(220, 50, 50)); 
        delGoalBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        delGoalBtn.setContentAreaFilled(false); 
        delGoalBtn.setBorderPainted(false);
        delGoalBtn.setMargin(new Insets(0,0,0,0));
        delGoalBtn.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
        delGoalBtn.addActionListener(e -> deleteGoal(goal));
        
        headerRow.add(nameLabel, BorderLayout.CENTER); 
        headerRow.add(delGoalBtn, BorderLayout.EAST);
        
        JLabel targetLabel = new JLabel(String.format("Target: %,.0f %s", goal.targetAmount, SettingsPanel.currentCurrency));
        targetLabel.setForeground(isDark ? new Color(200, 200, 200) : Color.DARK_GRAY);

        JLabel currentLabel = new JLabel(String.format("Current Balance: %,.0f %s", goal.currentAmount, SettingsPanel.currentCurrency));
        currentLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        currentLabel.setForeground(isDark ? new Color(120, 220, 120) : new Color(0, 120, 0)); 
        
        int percent = goal.targetAmount > 0 ? (int) ((goal.currentAmount / goal.targetAmount) * 100) : 0;
        JProgressBar bar = new JProgressBar(0, 100); 
        bar.setValue(Math.min(percent, 100)); 
        bar.setForeground(percent >= 100 ? new Color(40, 167, 69) : new Color(0, 122, 255)); 
        bar.setOpaque(false);
        bar.setBackground(new Color(0, 0, 0, 0));

        JPanel barRow = new JPanel(); 
        barRow.setLayout(new BoxLayout(barRow, BoxLayout.X_AXIS)); 
        barRow.setOpaque(false);
        barRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        barRow.add(bar); 
        barRow.add(Box.createRigidArea(new Dimension(8, 0))); 
        
        JLabel percentLabel = new JLabel(percent + "%"); 
        percentLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        percentLabel.setForeground(isDark ? Color.WHITE : Color.BLACK);
        barRow.add(percentLabel);

        JLabel motivationLabel = new JLabel(getMotivationalMessage(percent)); 
        motivationLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        motivationLabel.setForeground(isDark ? new Color(200, 200, 200) : Color.DARK_GRAY);
        
        p.add(headerRow); 
        p.add(Box.createRigidArea(new Dimension(0, 8))); 
        p.add(targetLabel); 
        p.add(currentLabel);
        p.add(Box.createRigidArea(new Dimension(0, 12))); 
        p.add(barRow); 
        p.add(Box.createRigidArea(new Dimension(0, 5))); 
        p.add(motivationLabel); 
        return p;
    }

    private String getMotivationalMessage(int percent) {
        if (percent == 0) return "🌱 Every bit counts. Let's go!";
        else if (percent < 50) return "💪 Making solid progress!";
        else if (percent < 100) return "🔥 Almost there! Keep it up!";
        else return "🎉 Goal achieved! Superstar! 🥳";
    }

    private void deleteGoal(SavingGoal goal) {
        if (JOptionPane.showConfirmDialog(this, "Delete '" + goal.name + "' permanently?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement("DELETE FROM goals WHERE id = ?")) {
                pstmt.setInt(1, goal.dbId); pstmt.executeUpdate();
            } catch (SQLException ex) {}
            loadUserData();
        }
    }

    private void deleteSelectedTransaction() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            if (JOptionPane.showConfirmDialog(this, "Delete this deposit?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                String goalName = (String) tableModel.getValueAt(row, 1);
                double amount = (double) tableModel.getValueAt(row, 4);
                int dbId = (int) tableModel.getValueAt(row, 5);
                try (Connection conn = DatabaseHelper.connect()) {
                    PreparedStatement p1 = conn.prepareStatement("DELETE FROM savings_transactions WHERE id = ?");
                    p1.setInt(1, dbId); p1.executeUpdate();
                    PreparedStatement p2 = conn.prepareStatement("UPDATE goals SET current_amount = MAX(0, current_amount - ?) WHERE username = ? AND name = ?");
                    p2.setDouble(1, amount); p2.setString(2, SessionManager.getCurrentUser().getUsername()); p2.setString(3, goalName); p2.executeUpdate();
                } catch (SQLException ex) {}
                loadUserData();
            }
        } else {
             JOptionPane.showMessageDialog(this, "Please select a transaction to delete.");
        }
    }

    private void showAddGoalDialog() {
        JTextField nameField = new JTextField(); JTextField targetField = new JTextField();
        JTextField dateField = new JTextField(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.add(new JLabel("Goal Name:")); panel.add(nameField);
        panel.add(new JLabel("Target Amount:")); panel.add(targetField);
        panel.add(new JLabel("Date Created:")); panel.add(dateField);
        if (JOptionPane.showConfirmDialog(this, panel, "Set Goal", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                try (Connection conn = DatabaseHelper.connect(); PreparedStatement pstmt = conn.prepareStatement("INSERT INTO goals (username, name, target_amount, current_amount, date) VALUES (?, ?, ?, 0, ?)")) {
                    pstmt.setString(1, SessionManager.getCurrentUser().getUsername());
                    pstmt.setString(2, nameField.getText()); pstmt.setDouble(3, Double.parseDouble(targetField.getText()));
                    pstmt.setString(4, dateField.getText()); pstmt.executeUpdate();
                }
                loadUserData();
            } catch (Exception ex) {}
        }
    }

    private void showAddSavingDialog() {
        if (goalsList.isEmpty()) return;
        JComboBox<String> goalDropdown = new JComboBox<>(); for (SavingGoal g : goalsList) goalDropdown.addItem(g.name);
        JTextField amountField = new JTextField(); JTextField descField = new JTextField();
        JTextField dateField = new JTextField(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.add(new JLabel("Goal:")); panel.add(goalDropdown);
        panel.add(new JLabel("Amount:")); panel.add(amountField);
        panel.add(new JLabel("Description:")); panel.add(descField);
        panel.add(new JLabel("Date:")); panel.add(dateField);
        if (JOptionPane.showConfirmDialog(this, panel, "Add Saving", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                double amt = Double.parseDouble(amountField.getText());
                String gName = (String) goalDropdown.getSelectedItem();
                try (Connection conn = DatabaseHelper.connect()) {
                    PreparedStatement p1 = conn.prepareStatement("INSERT INTO savings_transactions (username, date, goal_name, description, amount) VALUES (?, ?, ?, ?, ?)");
                    p1.setString(1, SessionManager.getCurrentUser().getUsername()); p1.setString(2, dateField.getText());
                    p1.setString(3, gName); p1.setString(4, descField.getText()); p1.setDouble(5, amt); p1.executeUpdate();
                    PreparedStatement p2 = conn.prepareStatement("UPDATE goals SET current_amount = current_amount + ? WHERE username = ? AND name = ?");
                    p2.setDouble(1, amt); p2.setString(2, SessionManager.getCurrentUser().getUsername()); p2.setString(3, gName); p2.executeUpdate();
                }
                loadUserData();
            } catch (Exception ex) {}
        }
    }

    private JButton createBlackButton(String text) {
        JButton btn = new JButton(text); 
        
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBackground(new Color(40, 50, 60));
        btn.setRolloverEnabled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false); 
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private class SavingGoal {
        int dbId; String name; double targetAmount; double currentAmount; String date;
        public SavingGoal(String name, double targetAmount) { this.name = name; this.targetAmount = targetAmount; }
    }
}