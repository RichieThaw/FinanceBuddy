import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CurrencyConverterPanel extends ThemeGradientPanel { // Inherits master gradient

    private JLabel mainTitle, subTitle;
    private JPanel converterCard;
    private JLabel resultAmountLabel, resultCurrencyLabel, rateInfoLabel;
    
    private JTextField amountField;
    private JComboBox<String> fromCurrencyBox;
    private JComboBox<String> toCurrencyBox;
    private JButton convertBtn, swapBtn;

    private final Map<String, Double> exchangeRates = new LinkedHashMap<>();
    
    private List<JPanel> quickRateCards = new ArrayList<>();
    private List<JLabel> quickRateLabels = new ArrayList<>();

    public CurrencyConverterPanel() {
        // --- REAL-WORLD SELLING PRICES ---
        exchangeRates.put("MMK - Myanmar Kyat", 1.0); 
        exchangeRates.put("USD - US Dollar", 4290.0);
        exchangeRates.put("EUR - Euro", 4910.0);
        exchangeRates.put("GBP - Great Britain Pound", 5627.0);
        exchangeRates.put("SGD - Singapore Dollar", 3310.0);
        exchangeRates.put("THB - Thai Baht", 130.72);
        exchangeRates.put("MYR - Malaysian Ringgit", 1055.0);
        exchangeRates.put("CNY - Chinese Yuan", 619.0);
        exchangeRates.put("JPY - Japanese Yen", 26.68);
        exchangeRates.put("KRW - South Korean Won", 2.83);
        exchangeRates.put("PHP - Philippine Peso", 70.64);
        exchangeRates.put("VND - Vietnamese Dong", 0.16);
        exchangeRates.put("LAK - Laotian Kip", 0.20);
        exchangeRates.put("KHR - Cambodian Riel", 1.06);
        exchangeRates.put("IDR - Indonesian Rupiah", 0.27); 
        exchangeRates.put("BND - Brunei Dollar", 3310.0); 

        setLayout(new BorderLayout(20, 20));
        setOpaque(false); // CRITICAL: Let gradient show through
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Listener for Theme Changes
        this.addPropertyChangeListener("background", evt -> {
            SwingUtilities.invokeLater(this::updateThemeColors);
        });

        // --- 1. HEADER & QUICK RATES ---
        JPanel northWrapper = new JPanel();
        northWrapper.setLayout(new BoxLayout(northWrapper, BoxLayout.Y_AXIS));
        northWrapper.setOpaque(false);

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.setOpaque(false);
        
        mainTitle = new JLabel("Currency Converter", SwingConstants.CENTER);
        mainTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        
        subTitle = new JLabel("Real-time estimations for currencies.", SwingConstants.CENTER);
        subTitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
        
        headerPanel.add(mainTitle);
        headerPanel.add(subTitle);
        
        // Quick Rates Bar
        JPanel quickRatesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        quickRatesPanel.setOpaque(false);
        
        quickRatesPanel.add(createQuickRateCard("USD", "US Dollar"));
        quickRatesPanel.add(createQuickRateCard("SGD", "Singapore Dollar"));
        quickRatesPanel.add(createQuickRateCard("THB", "Thai Baht"));
        quickRatesPanel.add(createQuickRateCard("MYR", "Malaysian Ringgit"));
        quickRatesPanel.add(createQuickRateCard("PHP", "Philippine Peso"));

        northWrapper.add(headerPanel);
        northWrapper.add(Box.createRigidArea(new Dimension(0, 25)));
        northWrapper.add(quickRatesPanel);

        // --- 2. CONVERTER CARD ---
        converterCard = createGlassCard(null);
        converterCard.setLayout(new BoxLayout(converterCard, BoxLayout.Y_AXIS));
        converterCard.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Input Fields
        JPanel inputGrid = new JPanel(new GridLayout(2, 3, 15, 10));
        inputGrid.setOpaque(false);

        inputGrid.add(new JLabel("Amount"));
        inputGrid.add(new JLabel("From"));
        inputGrid.add(new JLabel("To"));

        amountField = new JTextField("100");
        amountField.setFont(new Font("SansSerif", Font.BOLD, 16));
        amountField.setOpaque(false); // Make field blend in
        
        String[] currencies = exchangeRates.keySet().toArray(new String[0]);
        fromCurrencyBox = new JComboBox<>(currencies);
        fromCurrencyBox.setSelectedItem("USD - US Dollar");
        
        toCurrencyBox = new JComboBox<>(currencies);
        toCurrencyBox.setSelectedItem("MMK - Myanmar Kyat");

        inputGrid.add(amountField);
        inputGrid.add(fromCurrencyBox);
        inputGrid.add(toCurrencyBox);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        swapBtn = new JButton("⇄ Swap Currency");
        swapBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        swapBtn.setFocusPainted(false);
        swapBtn.addActionListener(e -> swapCurrencies());

        convertBtn = new JButton("Convert");
        convertBtn.setBackground(new Color(50, 130, 255));
        convertBtn.setForeground(Color.WHITE);
        convertBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        convertBtn.setFocusPainted(false);
        convertBtn.setBorderPainted(false);
        convertBtn.setOpaque(true);
        convertBtn.setPreferredSize(new Dimension(150, 40));
        convertBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        convertBtn.addActionListener(e -> performConversion());

        buttonPanel.add(swapBtn);
        buttonPanel.add(convertBtn);

        // Result Display
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setOpaque(false);
        
        resultAmountLabel = new JLabel("429,000.00", SwingConstants.CENTER);
        resultAmountLabel.setFont(new Font("SansSerif", Font.BOLD, 48));

        resultCurrencyLabel = new JLabel("Myanmar Kyat", SwingConstants.CENTER);
        resultCurrencyLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));

        rateInfoLabel = new JLabel("Selling Price: 1 USD = 4,290.00 MMK", SwingConstants.CENTER);
        rateInfoLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        rateInfoLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        resultPanel.add(resultAmountLabel, BorderLayout.CENTER);
        resultPanel.add(resultCurrencyLabel, BorderLayout.SOUTH);

        converterCard.add(inputGrid);
        converterCard.add(buttonPanel);
        converterCard.add(Box.createRigidArea(new Dimension(0, 20)));
        converterCard.add(resultPanel);
        converterCard.add(rateInfoLabel);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(converterCard);

        add(northWrapper, BorderLayout.NORTH);
        add(centerWrapper, BorderLayout.CENTER);

        this.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) { 
                updateThemeColors(); 
                performConversion();
            }
        });
    }

    // --- HELPER: Glass Card Generator ---
    private JPanel createGlassCard(LayoutManager layout) {
        JPanel panel = new JPanel(layout) {
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
        return panel;
    }

    private JPanel createQuickRateCard(String code, String name) {
        JPanel card = createGlassCard(new BorderLayout(5, 5));
        card.setPreferredSize(new Dimension(155, 90));
        card.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        
        double finalRate = exchangeRates.get(code + " - " + name);
        
        JLabel topLabel = new JLabel("1 " + code, SwingConstants.CENTER);
        topLabel.setFont(new Font("SansSerif", Font.BOLD, 22)); 
        
        JLabel bottomLabel = new JLabel(String.format("%,.2f MMK", finalRate), SwingConstants.CENTER);
        bottomLabel.setFont(new Font("SansSerif", Font.BOLD, 16)); 
        
        card.add(topLabel, BorderLayout.NORTH);
        card.add(bottomLabel, BorderLayout.SOUTH);
        
        quickRateCards.add(card);
        quickRateLabels.add(topLabel); 
        quickRateLabels.add(bottomLabel); 
        
        return card;
    }

    private void swapCurrencies() {
        int fromIndex = fromCurrencyBox.getSelectedIndex();
        fromCurrencyBox.setSelectedIndex(toCurrencyBox.getSelectedIndex());
        toCurrencyBox.setSelectedIndex(fromIndex);
        performConversion();
    }

    private void performConversion() {
        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            String fromKey = (String) fromCurrencyBox.getSelectedItem();
            String toKey = (String) toCurrencyBox.getSelectedItem();

            double fromRate = exchangeRates.get(fromKey);
            double toRate = exchangeRates.get(toKey);

            double convertedAmount = amount * (fromRate / toRate);
            double singleUnitRate = fromRate / toRate;

            String fromCode = fromKey.split(" - ")[0];
            String toCode = toKey.split(" - ")[0];
            String toName = toKey.split(" - ")[1];

            resultAmountLabel.setText(String.format("%,.2f", convertedAmount));
            resultCurrencyLabel.setText(toCode + " (" + toName + ")");
            rateInfoLabel.setText(String.format("Selling Price: 1 %s = %,.4f %s", fromCode, singleUnitRate, toCode));

        } catch (NumberFormatException ex) {
            resultAmountLabel.setText("Invalid Amount");
            resultCurrencyLabel.setText("");
            rateInfoLabel.setText("Please enter a valid number.");
        }
    }

    private void updateThemeColors() {
        boolean isDark = getBackground().getRed() < 100;
        Color textColor = isDark ? Color.WHITE : Color.BLACK;
        Color subTextColor = isDark ? new Color(180, 190, 200) : Color.DARK_GRAY;
        
        mainTitle.setForeground(textColor);
        subTitle.setForeground(subTextColor);
        
        // Translucent Glass Colors
        Color cardBg = isDark ? new Color(60, 63, 65, 140) : new Color(255, 255, 255, 160);
        
        converterCard.setBackground(cardBg);
        
        amountField.setBackground(new Color(0, 0, 0, 0)); // Pure transparent
        amountField.setForeground(textColor);
        amountField.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, isDark ? Color.WHITE : Color.BLACK));
        
        resultAmountLabel.setForeground(isDark ? new Color(100, 200, 120) : new Color(40, 167, 69));
        resultCurrencyLabel.setForeground(subTextColor);
        rateInfoLabel.setForeground(subTextColor);

        for (JPanel card : quickRateCards) {
            card.setBackground(cardBg);
        }
        for (int i = 0; i < quickRateLabels.size(); i++) {
            if (i % 2 == 0) quickRateLabels.get(i).setForeground(textColor);
            else quickRateLabels.get(i).setForeground(isDark ? new Color(100, 200, 120) : new Color(40, 167, 69));
        }
    }
}