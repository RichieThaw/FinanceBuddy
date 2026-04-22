import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class HelpSupportPanel extends ThemeGradientPanel { // Inherits master gradient

    private JTabbedPane tabbedPane;
    private JLabel mainTitle;
    private JLabel subTitle;
    private JLabel thankYouLabel;

    private List<JPanel> cardPanels = new ArrayList<>();
    private List<JLabel> cardTitles = new ArrayList<>();
    private List<JLabel> cardBodies = new ArrayList<>();

    public HelpSupportPanel() {
        setLayout(new BorderLayout(20, 20));
        setOpaque(false); // Let gradient show through
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        this.addPropertyChangeListener("background", evt -> {
            SwingUtilities.invokeLater(this::updateThemeColors);
        });
        
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent e) {
                updateThemeColors();
            }
        });

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new GridLayout(2, 1));
        headerPanel.setOpaque(false);

        mainTitle = new JLabel("Help & Support");
        mainTitle.setFont(new Font("SansSerif", Font.BOLD, 28));

        subTitle = new JLabel("Find guides and answers to common questions here.");
        subTitle.setFont(new Font("SansSerif", Font.PLAIN, 16));

        headerPanel.add(mainTitle);
        headerPanel.add(subTitle);

        // --- TABBED PANE ---
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));

        // 🔥 CRITICAL FIXES
        tabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI()); // remove Windows/Nimbus styling
        tabbedPane.setOpaque(false);
        tabbedPane.setFocusable(false);
        tabbedPane.setBorder(null);

        // Optional: remove default insets/padding look
        tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);

        tabbedPane.addTab("📖 Getting Started", createGuidePanel());
        tabbedPane.addTab("❓ FAQs", createFAQPanel());
        tabbedPane.addTab("📞 Contact Us", createContactPanel()); 

        // --- THANK YOU MESSAGE ---
        thankYouLabel = new JLabel("❤️ Thank you for using Finance Buddy. Let's manage your finances!", SwingConstants.CENTER);
        thankYouLabel.setFont(new Font("SansSerif", Font.ITALIC, 17));
        thankYouLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        add(headerPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(thankYouLabel, BorderLayout.SOUTH);
    }

    private void updateThemeColors() {
        boolean isDark = getBackground().getRed() < 100;
        
        Color textColor = isDark ? Color.WHITE : Color.BLACK;
        Color subTextColor = isDark ? new Color(180, 190, 200) : Color.DARK_GRAY;

        mainTitle.setForeground(textColor);
        subTitle.setForeground(subTextColor);
        
        thankYouLabel.setForeground(isDark ? new Color(100, 180, 255) : new Color(30, 60, 100));
        
        UIManager.put("TabbedPane.selected", isDark ? new Color(40, 40, 40) : Color.WHITE);
        UIManager.put("TabbedPane.contentAreaColor", new Color(0, 0, 0, 0));
        UIManager.put("TabbedPane.focus", new Color(0, 0, 0, 0));
        UIManager.put("TabbedPane.highlight", new Color(0, 0, 0, 0));
        UIManager.put("TabbedPane.light", new Color(0, 0, 0, 0));
        UIManager.put("TabbedPane.shadow", new Color(0, 0, 0, 0));
        UIManager.put("TabbedPane.darkShadow", new Color(0, 0, 0, 0));
        
        tabbedPane.setBackground(new Color(0, 0, 0, 0)); // Transparent background
        
        // 🔥 THE FIX: Light Blue tab text colors
        tabbedPane.setForeground(isDark ? new Color(50, 150, 255) : new Color(50, 150, 255)); 

        // --- UPDATE ALL GENERATED GLASS CARDS ---
        Color cardBg = isDark ? new Color(30, 35, 40, 180) : new Color(255, 255, 255, 160);
        Color titleColor = isDark ? new Color(100, 180, 255) : new Color(30, 60, 100); 
        Color bodyColor = isDark ? Color.WHITE : Color.DARK_GRAY;

        for (JPanel p : cardPanels) {
            p.setBackground(cardBg);
        }
        for (JLabel t : cardTitles) t.setForeground(titleColor);
        for (JLabel b : cardBodies) b.setForeground(bodyColor);
        repaint();
    }

    // --- HELPER: Glass Card Generator ---
    private JPanel createGlassCard() {
        JPanel panel = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                g2.dispose();
            }
        };

        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        return panel;
    }

    private JPanel createCard(String title, String body) {
        JPanel card = createGlassCard();
        card.setMaximumSize(new Dimension(800, 150));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel("<html><b>" + title + "</b></html>");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JLabel bodyLabel = new JLabel("<html><div style='width: 450px;'>" + body + "</div></html>");
        bodyLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(bodyLabel, BorderLayout.CENTER);

        cardPanels.add(card);
        cardTitles.add(titleLabel);
        cardBodies.add(bodyLabel);

        return card;
    }

    private JPanel createGuidePanel() {
        JPanel guidePanel = new JPanel();
        guidePanel.setLayout(new BoxLayout(guidePanel, BoxLayout.Y_AXIS));
        guidePanel.setOpaque(false);
        guidePanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] steps = {
            "Step 1: Understand Your Dashboard", "Your Dashboard gives you a bird's-eye view of your finances. You can see your Cash Flow By Each Month, Saving Goal, Currency Rate, and upcoming Bill reminders at a glance.",
            "Step 2: Add Income & Expenses", "Go to the 'Transactions' tab to log your daily transactions. Choose whether it's an Income or an Expense, assign a category, and track your cash flow.",
            "Step 3: Set Up Budgets", "To prevent overspending, go to the 'Budget' panel. Allocate limits for different categories (like Food or Transport). The app will warn you if you go over!",
            "Step 4: Manage Savings Goals", "Have a dream vacation or a new phone in mind? Create a goal in the 'Savings' tab and add deposits to watch your progress bar fill up.",
            "Step 5: Track Debts & Bills", "Never miss a payment. Use the 'Bills' panel for recurring payments, and the 'Debt & Loans' manager to track money you owe or money owed to you.",
            "Step 6: Currency Converter", "Traveling or shopping online? Use the 'Currency Converter' to get instant estimations between Southeast Asian currencies and the US Dollar."
        };

        for (int i = 0; i < steps.length; i += 2) {
            guidePanel.add(createCard(steps[i], steps[i + 1]));
            guidePanel.add(Box.createRigidArea(new Dimension(0, 15)));
        }
        return createScrollWrapper(guidePanel);
    }

    private JPanel createFAQPanel() {
        JPanel faqPanel = new JPanel();
        faqPanel.setLayout(new BoxLayout(faqPanel, BoxLayout.Y_AXIS));
        faqPanel.setOpaque(false);
        faqPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[][] faqs = {
            {"Q: How do I edit a transaction?", "A: Currently, you cannot directly edit a transaction. You need to select the transaction, delete it using the 'Delete' button at the bottom, and re-add it with the correct details."},
            {"Q: What happens when a bill is overdue?", "A: The bill will be highlighted in red on your Dashboard and inside the Bills Panel so you know it requires immediate attention."},
            {"Q: How is my balance calculated?", "A: Your balance is calculated by taking your Total Income and subtracting your Total Expenses."},
            {"Q: Can I charge interest on loans?", "A: Yes! When creating a new Debt/Loan, you can input an Interest Rate (%), and the app will automatically calculate the total amount owed."},
            {"Q: How do I mark a debt as paid?", "A: Go to the 'Debt & Loans' panel, find the specific card, and click the 'Settle' button. It will move to the 'Settled' filter."},
            {"Q: Are the currency exchange rates live?", "A: The Currency Converter uses fixed market estimations for Southeast Asia and USD. They are perfect for quick travel references and budgeting, but shouldn't be used for official trading!"}
        };

        for (String[] faq : faqs) {
            faqPanel.add(createCard(faq[0], faq[1]));
            faqPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        }
        return createScrollWrapper(faqPanel);
    }

    private JPanel createContactPanel() {
        JPanel contactPanel = new JPanel();
        contactPanel.setLayout(new BoxLayout(contactPanel, BoxLayout.Y_AXIS));
        contactPanel.setOpaque(false);
        contactPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel introLabel = new JLabel("<html>Have a question, feedback, or need technical assistance? Reach out to us directly!</html>");
        introLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        cardBodies.add(introLabel); 

        JPanel phoneCard = createCard("📞 Phone Support", "Call or message us anytime at:<br><br><b style='font-size:16px;'>+959 123456789</b>");
        JPanel emailCard = createCard("✉️ Email Support", "Click anywhere on this card to send us an email at:<br><br><b style='font-size:16px;'>richiethaw@gmail.com</b>");
        
        emailCard.setCursor(new Cursor(Cursor.HAND_CURSOR));
        emailCard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) Desktop.getDesktop().mail(new URI("mailto:richiethaw@gmail.com"));
                    else throw new Exception("Mail not supported");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(emailCard, "Could not open your default mail app.\n\nPlease email us directly at: richiethaw@gmail.com", "Email Support", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        contactPanel.add(introLabel);
        contactPanel.add(Box.createRigidArea(new Dimension(0, 25)));
        contactPanel.add(phoneCard);
        contactPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contactPanel.add(emailCard);
        return createScrollWrapper(contactPanel);
    }

    private JPanel createScrollWrapper(JPanel content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(new Color(0,0,0,0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false); 
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }
    
}