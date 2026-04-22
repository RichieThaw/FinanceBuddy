import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReportsPanel extends ThemeGradientPanel {
    
    private CustomPieChart incExpChart, budgetChart;
    private JLabel insightLabel1, insightLabel2;
    private String currentFilter = "All Time";
    private String customSearchValue = ""; // Stores the user's input (date or month)

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 🔥 THE FIX: Listener to update text colors instantly when Dark Mode is toggled
        this.addPropertyChangeListener("background", evt -> {
            SwingUtilities.invokeLater(() -> {
                boolean isDark = getBackground().getRed() < 100;
                Color fg = isDark ? Color.WHITE : Color.BLACK;
                insightLabel1.setForeground(fg);
                insightLabel2.setForeground(fg);
                repaint(); // Forces charts to redraw with new dynamic text colors
            });
        });

        // --- FILTER PANEL ---
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); 
        filterPanel.setOpaque(false); // Inherits background from ReportsPanel
        
        JButton allBtn = new JButton("All Time"); 
        JButton dayBtn = new JButton("Search by Day..."); 
        JButton monthBtn = new JButton("Search by Month...");
        
        ActionListener filterAction = e -> {
            JButton source = (JButton)e.getSource();
            String selected = source.getText();
            
            if (selected.equals("All Time")) {
                currentFilter = "All Time";
                customSearchValue = "";
            } else if (selected.equals("Search by Day...")) {
                String input = JOptionPane.showInputDialog(this, "Enter date (dd/MM/yyyy):", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                if (input != null && !input.trim().isEmpty()) {
                    currentFilter = "Day";
                    customSearchValue = input.trim();
                } else return; // User cancelled
            } else if (selected.equals("Search by Month...")) {
                String input = JOptionPane.showInputDialog(this, "Enter month (MM/yyyy):", LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy")));
                if (input != null && !input.trim().isEmpty()) {
                    currentFilter = "Month";
                    customSearchValue = input.trim();
                } else return; // User cancelled
            }

            // Update UI Button states
            allBtn.setBackground(Color.LIGHT_GRAY); 
            dayBtn.setBackground(Color.LIGHT_GRAY); 
            monthBtn.setBackground(Color.LIGHT_GRAY);
            source.setBackground(Color.GRAY);
            
            calculateFilteredCharts();
        };

        allBtn.addActionListener(filterAction); 
        dayBtn.addActionListener(filterAction); 
        monthBtn.addActionListener(filterAction);
        
        allBtn.setBackground(Color.GRAY); 
        dayBtn.setBackground(Color.LIGHT_GRAY); 
        monthBtn.setBackground(Color.LIGHT_GRAY);
        
        filterPanel.add(allBtn); 
        filterPanel.add(dayBtn); 
        filterPanel.add(monthBtn);

        // --- CHARTS CONTAINER ---
        JPanel chartsContainer = new JPanel(new GridLayout(1, 2, 20, 0)); 
        chartsContainer.setOpaque(false);
        chartsContainer.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Integrated Visual Analysis"));
        
        Color softGreen = new Color(180, 240, 180); 
        Color softYellow = new Color(255, 245, 170); 
        Color softLavender = new Color(230, 210, 255); 
        Color softBlue = new Color(190, 225, 255); 
        
        incExpChart = new CustomPieChart("Income vs Expenses", softGreen, softYellow, "Income", "Expense");
        budgetChart = new CustomPieChart("Budget Status", softLavender, softBlue, "Spent", "Remaining Budget");
        
        chartsContainer.add(incExpChart); 
        chartsContainer.add(budgetChart);

        // --- INSIGHTS PANEL ---
        JPanel insights = new JPanel(); 
        insights.setLayout(new BoxLayout(insights, BoxLayout.Y_AXIS)); 
        insights.setOpaque(false);
        insights.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Live Report Insights"));
        
        insightLabel1 = new JLabel("Awaiting data..."); 
        insightLabel1.setFont(new Font("SansSerif", Font.PLAIN, 14));
        insightLabel2 = new JLabel("Awaiting data..."); 
        insightLabel2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        insights.add(Box.createRigidArea(new Dimension(0, 10))); 
        insights.add(insightLabel1); 
        insights.add(Box.createRigidArea(new Dimension(0, 10))); 
        insights.add(insightLabel2);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 20)); 
        centerPanel.setOpaque(false);
        centerPanel.add(chartsContainer); 
        centerPanel.add(insights);
        
        add(filterPanel, BorderLayout.NORTH); 
        add(centerPanel, BorderLayout.CENTER);

        this.addComponentListener(new ComponentAdapter() { 
            public void componentShown(ComponentEvent e) { calculateFilteredCharts(); } 
        });
    }

    private boolean checkDate(String recordDate) {
        if (currentFilter.equals("All Time")) return true;
        if (customSearchValue.isEmpty()) return true;
        
        if (currentFilter.equals("Day")) {
            return recordDate.equals(customSearchValue); // Matches exact date
        } else if (currentFilter.equals("Month")) {
            return recordDate.endsWith(customSearchValue); // Matches MM/yyyy suffix
        }
        return true;
    }

    private void calculateFilteredCharts() {
        if (!SessionManager.isLoggedIn()) return;
        String username = SessionManager.getCurrentUser().getUsername();
        double filterInc = 0, filterExp = 0, filterBudgAlloc = 0, filterBudgSpent = 0;
        
        try (Connection conn = DatabaseHelper.connect()) {
            // Fetch Expenses and Income
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM expenses WHERE username = ?")) {
                pstmt.setString(1, username); 
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    if (checkDate(rs.getString("date"))) {
                        if (rs.getString("type").equals("Income")) filterInc += rs.getDouble("amount"); 
                        else filterExp += rs.getDouble("amount");
                    }
                }
            }
            // Fetch Budget Data
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM budgets WHERE username = ?")) {
                pstmt.setString(1, username); 
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    if (checkDate(rs.getString("date"))) {
                        filterBudgAlloc += rs.getDouble("limit_amount"); 
                        filterBudgSpent += rs.getDouble("spent_amount");
                    }
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }

        String titleSuffix = currentFilter.equals("All Time") ? "All Time" : customSearchValue;
        incExpChart.setTitle("Inc vs Exp (" + titleSuffix + ")");
        incExpChart.updateData(filterInc, filterExp);
        
        if (filterExp > filterInc && filterInc > 0) insightLabel1.setText("⚠️ Warning: Your Expenses exceeded your Income for " + titleSuffix + "!");
        else if (filterInc > 0) insightLabel1.setText("✅ Looking good! Your Income covers your current expenses.");
        else insightLabel1.setText("ℹ️ No financial records found for " + titleSuffix);

        budgetChart.setTitle("Budget Status (" + titleSuffix + ")");
        budgetChart.updateData(filterBudgSpent, Math.max(0, filterBudgAlloc - filterBudgSpent));
        
        if (filterBudgSpent > filterBudgAlloc && filterBudgAlloc > 0) insightLabel2.setText("⚠️ DANGER: You are over your allocated budgets!");
        else if (filterBudgAlloc > 0) insightLabel2.setText("✅ You are safely within your budget limits.");
        else insightLabel2.setText("ℹ️ No budgets found for " + titleSuffix);
    }

    private class CustomPieChart extends JPanel {
        private double v1 = 0, v2 = 0; 
        private Color c1, c2; 
        private String title, l1, l2;
        
        public CustomPieChart(String title, Color c1, Color c2, String l1, String l2) { 
            this.title = title; this.c1 = c1; this.c2 = c2; this.l1 = l1; this.l2 = l2; 
            setOpaque(false); // Important for inheriting parent background correctly
        }
        public void setTitle(String newTitle) { this.title = newTitle; }
        public void updateData(double val1, double val2) { this.v1 = val1; this.v2 = val2; repaint(); }
        
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); 
            Graphics2D g2 = (Graphics2D) g; 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(); int h = getHeight(); int size = Math.min(w, h) - 60; int x = (w - size) / 2; int y = 30;
            
            // 🔥 THE FIX: Detect theme and set dynamic text color based on background brightness
            boolean isDark = getBackground().getRed() < 100;
            Color textColor = isDark ? Color.WHITE : Color.BLACK;
            
            g2.setColor(textColor); 
            g2.setFont(new Font("SansSerif", Font.BOLD, 14)); 
            g2.drawString(title, (w - g2.getFontMetrics().stringWidth(title))/2, 20);
            
            double total = v1 + v2;
            if (total == 0) {
                // Dynamic placeholder for empty charts
                g2.setColor(isDark ? new Color(60, 60, 60) : new Color(240, 240, 240)); 
                g2.fillArc(x, y, size, size, 0, 360);
                g2.setColor(Color.GRAY); 
                g2.drawString("No Data", w/2 - 25, y + size/2 + 5); return;
            }
            int angle1 = (int) Math.round((v1 / total) * 360); int angle2 = 360 - angle1;
            g2.setColor(c1); g2.fillArc(x, y, size, size, 90, -angle1); 
            g2.setColor(c2); g2.fillArc(x, y, size, size, 90 - angle1, -angle2);
            
            int legendY = y + size + 15; g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(c1); g2.fillRect(w/2 - 60, legendY, 10, 10); 
            g2.setColor(textColor); // Legend text color now dynamic
            g2.drawString(l1, w/2 - 45, legendY + 10);
            g2.setColor(c2); g2.fillRect(w/2 + 15, legendY, 10, 10); 
            g2.setColor(textColor); // Legend text color now dynamic
            g2.drawString(l2, w/2 + 30, legendY + 10);
        }
    }
}