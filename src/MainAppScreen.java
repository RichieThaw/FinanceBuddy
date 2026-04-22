import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainAppScreen extends ThemeGradientPanel {

    private FinanceBuddy rootApp;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JPanel menuPanel;
    private JLabel[] navLinks;
    private JLabel menuTitle;
    private String currentCard = "GENERAL";

    public MainAppScreen(FinanceBuddy app) {
        this.rootApp = app;
        setLayout(new BorderLayout());

        // We listen to the MainAppScreen's background (controlled by SettingsPanel)
        this.addPropertyChangeListener("background", evt -> {
            SwingUtilities.invokeLater(() -> refreshMenuColors());
        });

        // --- 1. LEFT MENU PANEL ---
        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false); // Transparent for glassmorphism!
        menuPanel.setPreferredSize(new Dimension(240, 0));

        menuTitle = new JLabel("Finance Buddy");
        menuTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        menuTitle.setBorder(BorderFactory.createEmptyBorder(25, 20, 30, 20));
        menuTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuPanel.add(menuTitle);

        // --- 2. NAVIGATION LINKS ---
        navLinks = new JLabel[10]; 

        navLinks[0] = createNavLink("🏠   Dashboard", "GENERAL");
        navLinks[1] = createNavLink("💸   Transactions", "EXPENSE"); 
        navLinks[2] = createNavLink("🎯   Budget", "BUDGET");
        navLinks[3] = createNavLink("🎁   Saving", "SAVING");
        navLinks[4] = createNavLink("🔔   Bills", "BILLS"); 
        navLinks[5] = createNavLink("🤝   Debts & Loans", "DEBTS"); 
        // navLinks[6] = createNavLink("⚖️   Split Bills", "SPLIT_BILLS"); 
        navLinks[6] = createNavLink("💵   Currency Converter", "CURRENCY"); 
        navLinks[7] = createNavLink("❓   Help & Guide", "HELP_SUPPORT"); 
        navLinks[8] = createNavLink("⚙️   Settings", "SETTINGS");

        for (JLabel link : navLinks) {
            if (link != null) menuPanel.add(link);
        }
        
        JPanel filler = new JPanel();
        filler.setOpaque(false); 
        menuPanel.add(filler);

        add(menuPanel, BorderLayout.WEST);

        // --- 3. MAIN CONTENT AREA (CARD LAYOUT) ---
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false); // Transparent so the master gradient shows!

        // Initialize Panels
        SettingsPanel settingsPanel = new SettingsPanel(rootApp);
        ExpenseIncomePanel expensePanel = new ExpenseIncomePanel();
        BudgetPanel budgetPanel = new BudgetPanel();
        SavingPanel savingPanel = new SavingPanel();
        BillsPanel billsPanel = new BillsPanel(); 
        DebtLoansPanel debtPanel = new DebtLoansPanel(); 
        // SplitBillsPanel splitPanel = new SplitBillsPanel();
        CurrencyConverterPanel currencyPanel = new CurrencyConverterPanel(); 
        HelpSupportPanel helpPanel = new HelpSupportPanel(); 
        DashboardPanel dashboardPanel = new DashboardPanel(this);

        // Add to CardLayout
        contentPanel.add(dashboardPanel, "GENERAL");
        contentPanel.add(expensePanel, "EXPENSE");
        contentPanel.add(budgetPanel, "BUDGET");
        contentPanel.add(savingPanel, "SAVING");
        contentPanel.add(billsPanel, "BILLS"); 
        contentPanel.add(debtPanel, "DEBTS"); 
        //contentPanel.add(splitPanel, "SPLIT_BILLS");
        contentPanel.add(currencyPanel, "CURRENCY"); 
        contentPanel.add(helpPanel, "HELP_SUPPORT"); 
        contentPanel.add(settingsPanel, "SETTINGS");

        add(contentPanel, BorderLayout.CENTER);
        
        navigateTo("GENERAL"); 
    }

    private JLabel createNavLink(String text, String cardName) {
        // 🔥 Custom painting inside the JLabel prevents the Swing "Black Box" bug
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Color bg = getBackground();
                if (bg != null && bg.getAlpha() > 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bg);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };

        label.setFont(new Font("SansSerif", Font.PLAIN, 15));
        label.setOpaque(false); // CRITICAL: Always keep false for glassmorphism
        label.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setBackground(new Color(0, 0, 0, 0)); // Start fully transparent

        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) { navigateTo(cardName); }
            public void mouseEntered(MouseEvent evt) { 
                boolean isDark = MainAppScreen.this.getBackground().getRed() < 100;
                if (!label.getFont().isBold()) {
                    // Soft white frosted glass hover for BOTH modes
                    label.setBackground(isDark ? new Color(255, 255, 255, 25) : new Color(255, 255, 255, 100)); 
                    label.repaint();
                }
            }
            public void mouseExited(MouseEvent evt) { 
                if (!label.getFont().isBold()) {
                    label.setBackground(new Color(0, 0, 0, 0));
                    label.repaint();
                }
            }
        });
        return label;
    }

    public void navigateInternal(String screenName) { 
        navigateTo(screenName); 
    }

    public void navigateTo(String cardName) {
        this.currentCard = cardName;
        cardLayout.show(contentPanel, cardName);
        refreshMenuColors(); 
    }

    public void refreshMenuColors() {
        if (menuPanel == null || navLinks == null) return;
        
        // Check MainAppScreen's background to determine dark mode
        boolean isDark = getBackground().getRed() < 100;

        if (menuTitle != null) {
            menuTitle.setForeground(isDark ? Color.WHITE : new Color(20, 30, 40));
        }
        
        // Glassmorphism divider line
        menuPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, 
            isDark ? new Color(255, 255, 255, 50) : new Color(0, 0, 0, 30)));

        for (JLabel link : navLinks) {
            if (link == null) continue;
            link.setFont(new Font("SansSerif", Font.PLAIN, 15));
            link.setForeground(isDark ? new Color(200, 210, 220) : new Color(60, 70, 80)); 
            link.setBackground(new Color(0, 0, 0, 0)); // Reset to transparent
            link.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        }

        for (JLabel link : navLinks) {
            if (link == null) continue;
            boolean match = false;
            String text = link.getText();
            
            if (text.contains("Dashboard") && currentCard.equals("GENERAL")) match = true;
            if (text.contains("Transactions") && currentCard.equals("EXPENSE")) match = true;
            if (text.contains("Budget") && currentCard.equals("BUDGET")) match = true;
            if (text.contains("Saving") && currentCard.equals("SAVING")) match = true;
            if (text.contains("Bills") && currentCard.equals("BILLS")) match = true; 
            if (text.contains("Debt") && currentCard.equals("DEBTS")) match = true; 
            // if (text.contains("Split Bills") && currentCard.equals("SPLIT_BILLS")) match = true;
            if (text.contains("Currency") && currentCard.equals("CURRENCY")) match = true; 
            if (text.contains("Help") && currentCard.equals("HELP_SUPPORT")) match = true; 
            if (text.contains("Settings") && currentCard.equals("SETTINGS")) match = true;

            if (match) {
                link.setFont(new Font("SansSerif", Font.BOLD, 15));
                link.setForeground(isDark ? Color.WHITE : Color.BLACK);
                
                // Semi-transparent frosted selection highlight
                link.setBackground(isDark ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 160)); 
                
                // Highlight indicator bar
                link.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, isDark ? new Color(100, 200, 255) : new Color(20, 160, 200)),
                    BorderFactory.createEmptyBorder(15, 16, 15, 20)
                ));
            }
        }
    }
}