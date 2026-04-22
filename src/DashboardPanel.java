import javax.swing.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardPanel extends ThemeGradientPanel { // Inherits master gradient

    private MainAppScreen parent;
    private JLabel greetingLabel;
    private JPanel chartWrapper, remindersPanel, savingsGoalPanel, quickRatesPanel;
    private JLabel chartTitle;
    
    // Insight Labels for below the chart
    private JLabel highestIncomeLabel;
    private JLabel highestExpenseLabel;
    
    private Color cardBg, textColor, subTextColor;

    // DYNAMIC CHART ARRAYS
    private double[] monthlySpending = new double[12];
    private double[] monthlyIncome = new double[12];
    private String[] monthLabels = new String[12];
    private double maxChartValue = 1000000; 
    
    private JPanel dynamicChart;

    public DashboardPanel(MainAppScreen parent) {
        this.parent = parent;
        setLayout(new BorderLayout(20, 20));
        setOpaque(false); // Let the gradient show through!
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        this.addPropertyChangeListener("background", evt -> {
            SwingUtilities.invokeLater(() -> {
                updateThemeColors();
                loadDashboardData();
            });
        });

        // --- 1. TOP HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        greetingLabel = new JLabel("Hello User !");
        greetingLabel.setFont(new Font("SansSerif", Font.BOLD, 30));

        JLabel subtitleLabel = new JLabel("Welcome To FinanceBuddy");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        subtitleLabel.setForeground(Color.GRAY);
        
        JPanel titleWrapper = new JPanel(new GridLayout(2, 1, 0, 5));
        titleWrapper.setOpaque(false);
        titleWrapper.add(greetingLabel);
        titleWrapper.add(subtitleLabel);

        // Styled to match the new glass UI
        JButton addExpBtn = new JButton("Transactions");

        addExpBtn.setForeground(Color.WHITE);
        addExpBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        addExpBtn.setOpaque(true);
        addExpBtn.setBackground(new Color(40, 50, 60));
        addExpBtn.setBorderPainted(false);
        addExpBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addExpBtn.setPreferredSize(new Dimension(150, 40));
        addExpBtn.addActionListener(e -> parent.navigateInternal("EXPENSE"));

        headerPanel.add(titleWrapper, BorderLayout.WEST);
        headerPanel.add(addExpBtn, BorderLayout.EAST);

        // --- 2. MAIN CONTENT SECTION ---
        JPanel mainContentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        mainContentPanel.setOpaque(false);

        // 2a. Chart Panel (Using our new Glass Card)
        chartWrapper = createGlassCard(new BorderLayout());
        chartTitle = new JLabel("Cash Flow (" + LocalDate.now().getYear() + ")");
        chartTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        chartWrapper.add(chartTitle, BorderLayout.NORTH);
        
        dynamicChart = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isDark = getBackground().getRed() < 100;

                int w = getWidth(); int h = getHeight();
                int paddingLeft = 50; int paddingBottom = 30; 
                int paddingTop = 30; int paddingRight = 20;

                int numYLabels = 11; 
                double stepValue = maxChartValue / 10.0;

                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));

                for (int i = 0; i < numYLabels; i++) {
                    int y = h - paddingBottom - (i * (h - paddingBottom - paddingTop) / (numYLabels - 1));
                    g2.setColor(isDark ? new Color(255, 255, 255, 30) : new Color(0, 0, 0, 20));
                    g2.drawLine(paddingLeft, y, w - paddingRight, y);
                    
                    g2.setColor(isDark ? new Color(200, 210, 220) : Color.DARK_GRAY);
                    double labelVal = i * stepValue; 
                    String yLabel = formatYLabel(labelVal);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(yLabel, paddingLeft - fm.stringWidth(yLabel) - 8, y + 4);
                }

                int numXLabels = monthLabels.length;
                for (int i = 0; i < numXLabels; i++) {
                    int x = paddingLeft + (i * (w - paddingLeft - paddingRight) / (numXLabels - 1));
                    g2.setColor(isDark ? new Color(200, 210, 220) : Color.DARK_GRAY);
                    FontMetrics fm = g2.getFontMetrics();
                    String monthTxt = monthLabels[i] != null ? monthLabels[i] : "";
                    g2.drawString(monthTxt, x - (fm.stringWidth(monthTxt) / 2), h - 10);
                }

                int[] xPoints = new int[numXLabels];
                int[] yPointsExp = new int[numXLabels];
                int[] yPointsInc = new int[numXLabels];

                for (int i = 0; i < numXLabels; i++) {
                    xPoints[i] = paddingLeft + (i * (w - paddingLeft - paddingRight) / (numXLabels - 1));
                    yPointsExp[i] = h - paddingBottom - (int)((monthlySpending[i] * (h - paddingBottom - paddingTop)) / maxChartValue);
                    yPointsInc[i] = h - paddingBottom - (int)((monthlyIncome[i] * (h - paddingBottom - paddingTop)) / maxChartValue);
                }

                // Draw Expense Line (RED)
                g2.setColor(new Color(230, 70, 70));
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < numXLabels - 1; i++) {
                    g2.drawLine(xPoints[i], yPointsExp[i], xPoints[i+1], yPointsExp[i+1]);
                }
                for (int i = 0; i < numXLabels; i++) {
                    g2.fillOval(xPoints[i] - 5, yPointsExp[i] - 5, 10, 10);
                }

                // Draw Income Line (GREEN)
                g2.setColor(new Color(40, 180, 80));
                for (int i = 0; i < numXLabels - 1; i++) {
                    g2.drawLine(xPoints[i], yPointsInc[i], xPoints[i+1], yPointsInc[i+1]);
                }
                for (int i = 0; i < numXLabels; i++) {
                    g2.fillOval(xPoints[i] - 5, yPointsInc[i] - 5, 10, 10);
                }

                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                g2.setColor(new Color(40, 180, 80));
                g2.fillOval(w - 170, 5, 10, 10);
                g2.setColor(isDark ? Color.WHITE : Color.BLACK);
                g2.drawString("Income", w - 155, 15);

                g2.setColor(new Color(230, 70, 70));
                g2.fillOval(w - 90, 5, 10, 10);
                g2.setColor(isDark ? Color.WHITE : Color.BLACK);
                g2.drawString("Expense", w - 75, 15);
            }
        };
        dynamicChart.setOpaque(false);
        chartWrapper.add(dynamicChart, BorderLayout.CENTER);
        
        JPanel chartInsightsPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        chartInsightsPanel.setOpaque(false);
        chartInsightsPanel.setBorder(BorderFactory.createEmptyBorder(15, 5, 0, 0));
        
        highestIncomeLabel = new JLabel("🌟 Highest Income: No Data");
        highestIncomeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        highestExpenseLabel = new JLabel("⚠️ Highest Expense: No Data");
        highestExpenseLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        
        chartInsightsPanel.add(highestIncomeLabel);
        chartInsightsPanel.add(highestExpenseLabel);
        chartWrapper.add(chartInsightsPanel, BorderLayout.SOUTH);

        // 2b. Right Side Panels (Using Glass Cards)
        JPanel rightSideLayout = new JPanel(new GridLayout(3, 1, 0, 20));
        rightSideLayout.setOpaque(false);

        remindersPanel = createGlassCard(null);
        remindersPanel.setLayout(new BoxLayout(remindersPanel, BoxLayout.Y_AXIS));
        
        savingsGoalPanel = createGlassCard(new BorderLayout());

        quickRatesPanel = createGlassCard(new BorderLayout());
        JLabel ratesTitle = new JLabel("📈 Today Currency Rate");
        ratesTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        
        JPanel ratesList = new JPanel(new GridLayout(3, 1, 0, 5));
        ratesList.setOpaque(false);
        JLabel usd = new JLabel("1 USD  =  4,290.00 MMK"); usd.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel sgd = new JLabel("1 SGD  =  3,310.00 MMK"); sgd.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel thb = new JLabel("1 THB  =  130.72 MMK"); thb.setFont(new Font("SansSerif", Font.BOLD, 14));
        ratesList.add(usd); ratesList.add(sgd); ratesList.add(thb);
        
        quickRatesPanel.add(ratesTitle, BorderLayout.NORTH);
        quickRatesPanel.add(ratesList, BorderLayout.CENTER);
        quickRatesPanel.putClientProperty("usdLabel", usd);
        quickRatesPanel.putClientProperty("sgdLabel", sgd);
        quickRatesPanel.putClientProperty("thbLabel", thb);
        quickRatesPanel.putClientProperty("titleLabel", ratesTitle);

        rightSideLayout.add(remindersPanel);
        rightSideLayout.add(savingsGoalPanel);
        rightSideLayout.add(quickRatesPanel);

        mainContentPanel.add(chartWrapper);
        mainContentPanel.add(rightSideLayout);

        add(headerPanel, BorderLayout.NORTH);
        add(mainContentPanel, BorderLayout.CENTER);

        this.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) { updateThemeColors(); loadDashboardData(); }
        });
    }

    // --- NEW HELPER: Creates a smooth glass panel with rounded corners ---
    private JPanel createGlassCard(LayoutManager layout) {
        JPanel panel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
            	super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); // 20px Rounded corners!
                g2.dispose();
            }
        };
        panel.setOpaque(false); // CRITICAL: Allows gradient to pass through empty spaces
        return panel;
    }

    private String formatYLabel(double val) {
        if (val == 0) return "0";
        return String.format("%.0fk", val / 1000.0);
    }

    private void updateThemeColors() {
        boolean isDark = getBackground().getRed() < 100;
        
        // 🔥 The Magic: Semi-transparent background colors! (140 out of 255 opacity)
        cardBg = isDark ? new Color(40, 45, 50, 140) : new Color(255, 255, 255, 140);
        
        textColor = isDark ? Color.WHITE : new Color(20, 30, 40);
        subTextColor = isDark ? new Color(180, 190, 200) : Color.DARK_GRAY;
        
        greetingLabel.setForeground(textColor);
        
        // Remove hard borders, replace with empty padding
        chartWrapper.setBackground(cardBg);
        chartWrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        chartTitle.setForeground(textColor);
        
        highestIncomeLabel.setForeground(isDark ? new Color(100, 220, 120) : new Color(30, 140, 60));
        highestExpenseLabel.setForeground(isDark ? new Color(255, 100, 100) : new Color(200, 40, 40));
        
        remindersPanel.setBackground(cardBg);
        remindersPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        savingsGoalPanel.setBackground(cardBg);
        savingsGoalPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        quickRatesPanel.setBackground(cardBg);
        quickRatesPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        ((JLabel) quickRatesPanel.getClientProperty("titleLabel")).setForeground(textColor);
        Color rateColor = isDark ? new Color(100, 220, 120) : new Color(30, 140, 60);
        ((JLabel) quickRatesPanel.getClientProperty("usdLabel")).setForeground(rateColor);
        ((JLabel) quickRatesPanel.getClientProperty("sgdLabel")).setForeground(rateColor);
        ((JLabel) quickRatesPanel.getClientProperty("thbLabel")).setForeground(rateColor);
    }

    private void loadDashboardData() {
        if (!SessionManager.isLoggedIn()) return;
        String username = SessionManager.getCurrentUser().getUsername();
        greetingLabel.setText("Hello " + username + " !");

        try (Connection conn = DatabaseHelper.connect()) {
            loadAlertsAndReminders(conn, username);
            loadTopSavingsGoal(conn, username);
            loadChartData(conn, username); 
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private LocalDate parseDateFlexible(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        dateStr = dateStr.trim().replace("-", "/").replace(".", "/");
        String[] formats = {"dd/MM/yyyy", "d/M/yyyy", "MM/dd/yyyy", "M/d/yyyy", "yyyy/MM/dd", "yyyy-MM-dd"};
        for (String format : formats) {
            try { return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format)); } catch (Exception e) {}
        }
        return null; 
    }

    private void loadChartData(Connection conn, String username) {
        try {
            int currentYear = LocalDate.now().getYear();
            chartTitle.setText("Cash Flow (" + currentYear + ")");
            
            for (int i = 0; i < 12; i++) {
                monthLabels[i] = LocalDate.of(currentYear, i + 1, 1).format(DateTimeFormatter.ofPattern("MMM"));
                monthlySpending[i] = 0.0; monthlyIncome[i] = 0.0; 
            }

            PreparedStatement pstmt = conn.prepareStatement("SELECT date, amount, type FROM expenses WHERE username = ?");
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                try {
                    String dateStr = rs.getString("date"); String type = rs.getString("type");
                    double amount = 0;
                    try { amount = rs.getDouble("amount"); } catch (Exception e) {
                        String amtStr = rs.getString("amount");
                        if (amtStr != null) amount = Double.parseDouble(amtStr.replaceAll("[^\\d.]", ""));
                    }

                    LocalDate date = parseDateFlexible(dateStr);
                    if (date != null && date.getYear() == currentYear) {
                        int monthIndex = date.getMonthValue() - 1; 
                        if (type != null && type.trim().equalsIgnoreCase("Income")) monthlyIncome[monthIndex] += amount;
                        else monthlySpending[monthIndex] += amount;
                    }
                } catch (Exception parseEx) {}
            }

            double highestVal = 0; double maxInc = 0; double maxExp = 0;
            int maxIncIdx = -1; int maxExpIdx = -1;
            
            for (int i = 0; i < 12; i++) {
                if (monthlySpending[i] > highestVal) highestVal = monthlySpending[i];
                if (monthlyIncome[i] > highestVal) highestVal = monthlyIncome[i];
                
                if (monthlyIncome[i] > maxInc) { maxInc = monthlyIncome[i]; maxIncIdx = i; }
                if (monthlySpending[i] > maxExp) { maxExp = monthlySpending[i]; maxExpIdx = i; }
            }

            if (highestVal == 0) maxChartValue = 1000000; 
            else maxChartValue = Math.ceil(highestVal / 1000000.0) * 1000000.0;

            if (maxIncIdx != -1) highestIncomeLabel.setText("🌟 Highest Income: " + monthLabels[maxIncIdx] + " (" + String.format("%,.0f", maxInc) + " " + SettingsPanel.currentCurrency + ")");
            else highestIncomeLabel.setText("🌟 Highest Income: No Data");
            
            if (maxExpIdx != -1) highestExpenseLabel.setText("⚠️ Highest Expense: " + monthLabels[maxExpIdx] + " (" + String.format("%,.0f", maxExp) + " " + SettingsPanel.currentCurrency + ")");
            else highestExpenseLabel.setText("⚠️ Highest Expense: No Data");
            
            dynamicChart.repaint();

        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadAlertsAndReminders(Connection conn, String username) throws SQLException {
        remindersPanel.removeAll();
        JLabel title = new JLabel("🔔 Alerts & Reminders");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(textColor);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        remindersPanel.add(title);
        remindersPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        int alertCount = 0;
        PreparedStatement budgetStmt = conn.prepareStatement("SELECT category, spent_amount, limit_amount FROM budgets WHERE username = ? AND spent_amount > limit_amount");
        budgetStmt.setString(1, username); ResultSet budgetRs = budgetStmt.executeQuery();

        while (budgetRs.next() && alertCount < 4) {
            String category = budgetRs.getString("category");
            double over = budgetRs.getDouble("spent_amount") - budgetRs.getDouble("limit_amount");
            JLabel alertLabel = new JLabel("⚠️ Over Budget: " + category + " (by " + String.format("%,.0f", over) + ")");
            alertLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            alertLabel.setForeground(new Color(220, 50, 50)); 
            alertLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            remindersPanel.add(alertLabel); alertCount++;
        }

        List<BillAlert> billAlerts = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate today = LocalDate.now();
        PreparedStatement billStmt = conn.prepareStatement("SELECT * FROM bills WHERE username = ? AND status != 'SETTLED'");
        billStmt.setString(1, username); ResultSet billRs = billStmt.executeQuery();

        while (billRs.next()) {
            try {
                String biller = billRs.getString("biller");
                LocalDate dueDate = LocalDate.parse(billRs.getString("due_date"), dtf);
                long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);
                if (daysUntilDue <= billRs.getInt("reminder_days")) billAlerts.add(new BillAlert(biller, daysUntilDue, dueDate));
            } catch (Exception e) {}
        }
        Collections.sort(billAlerts);

        for (BillAlert alert : billAlerts) {
            if (alertCount >= 4) break; 
            String msg; Color msgColor = textColor;
            if (alert.daysDiff < 0) { msg = "⚠️ " + alert.name + " is overdue!"; msgColor = new Color(220, 50, 50); }
            else if (alert.daysDiff == 0) { msg = "🔔 " + alert.name + " is due today!"; msgColor = new Color(220, 120, 0); }
            else { msg = "📅 " + alert.name + " due in " + alert.daysDiff + " days"; }
            
            JLabel alertLabel = new JLabel(msg);
            alertLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            alertLabel.setForeground(msgColor);
            alertLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            remindersPanel.add(alertLabel); alertCount++;
        }

        if (alertCount == 0) {
            JLabel empty = new JLabel("No alerts or upcoming bills! 🎉");
            empty.setForeground(subTextColor);
            remindersPanel.add(empty);
        }
        remindersPanel.revalidate(); remindersPanel.repaint();
    }

    private void loadTopSavingsGoal(Connection conn, String username) throws SQLException {
        savingsGoalPanel.removeAll();
        savingsGoalPanel.setBackground(cardBg);

        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM goals WHERE username = ? ORDER BY current_amount DESC LIMIT 1");
        pstmt.setString(1, username); ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            String name = rs.getString("name");
            double target = rs.getDouble("target_amount"); double current = rs.getDouble("current_amount");

            JLabel title = new JLabel("Savings Goal", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 16));
            title.setForeground(textColor);

            JLabel amountLabel = new JLabel(String.format("%,.0f %s", target, SettingsPanel.currentCurrency), SwingConstants.CENTER);
            amountLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
            amountLabel.setForeground(textColor);

            int pct = target > 0 ? (int) ((current / target) * 100) : 0;
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(Math.min(pct, 100));
            bar.setForeground(new Color(40, 150, 255));
            bar.setPreferredSize(new Dimension(200, 10));

            JPanel bottomRow = new JPanel(new BorderLayout());
            bottomRow.setOpaque(false);
            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            nameLabel.setForeground(textColor);
            JLabel curLabel = new JLabel(String.format("%,.0f", current));
            curLabel.setForeground(subTextColor);
            bottomRow.add(nameLabel, BorderLayout.WEST); bottomRow.add(curLabel, BorderLayout.EAST);

            savingsGoalPanel.add(title, BorderLayout.NORTH);
            JPanel centerP = new JPanel(); centerP.setLayout(new BoxLayout(centerP, BoxLayout.Y_AXIS)); centerP.setOpaque(false);
            amountLabel.setAlignmentX(Component.CENTER_ALIGNMENT); bar.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerP.add(Box.createRigidArea(new Dimension(0, 15))); centerP.add(amountLabel);
            centerP.add(Box.createRigidArea(new Dimension(0, 15))); centerP.add(bar);
            savingsGoalPanel.add(centerP, BorderLayout.CENTER);
            savingsGoalPanel.add(bottomRow, BorderLayout.SOUTH);

        } else {
            JLabel empty = new JLabel("No Savings Goals Set", SwingConstants.CENTER);
            empty.setForeground(subTextColor);
            savingsGoalPanel.add(empty, BorderLayout.CENTER);
        }
        savingsGoalPanel.revalidate(); savingsGoalPanel.repaint();
    }

    private class BillAlert implements Comparable<BillAlert> {
        String name; long daysDiff; LocalDate date;
        public BillAlert(String name, long daysDiff, LocalDate date) { this.name = name; this.daysDiff = daysDiff; this.date = date; }
        @Override public int compareTo(BillAlert other) { return this.date.compareTo(other.date); }
    }
}