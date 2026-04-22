import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsPanel extends ThemeGradientPanel { // Inherits master gradient
    
    public static String currentCurrency = "MMK";
    
    private FinanceBuddy rootApp;
    private JTextField userField;
    private JTextField emailField;
    
    // Storing components to dynamically change them later
    private JLabel mainTitle;
    private JLabel dangerDesc;
    private JPanel togglePanel;
    private JPanel currencyPanel;
    private JPanel accPanel;
    private JPanel dangerPanel;

    public SettingsPanel(FinanceBuddy app) {
        this.rootApp = app;
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false); // CRITICAL: Allows gradient to pass through
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Hook to intercept and protect our Glassmorphism panels from the old theme engine!
        this.addPropertyChangeListener("background", evt -> {
            SwingUtilities.invokeLater(this::updateThemeColors);
        });

        mainTitle = new JLabel("General Settings");
        mainTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        mainTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- 1. DARK MODE TOGGLE ---
        togglePanel = createGlassPanel(new BorderLayout());
        togglePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); 
        togglePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel modeLabel = new JLabel("Dark Mode");
        modeLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        togglePanel.add(modeLabel, BorderLayout.WEST);
        
        JToggleButton modeToggle = new JToggleButton("OFF"); 
        modeToggle.setBackground(Color.LIGHT_GRAY); 
        modeToggle.setForeground(Color.BLACK); 
        modeToggle.setFocusPainted(false);
        modeToggle.setOpaque(true);
        modeToggle.setBorderPainted(false); 
        
        modeToggle.addActionListener(e -> {
            boolean isDark = modeToggle.isSelected(); 
            modeToggle.setText(isDark ? "ON" : "OFF");
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) { 
                applyTheme(window, isDark); 
                
                try {
                    Component parent = this;
                    while (parent != null && !parent.getClass().getName().endsWith("MainAppScreen")) {
                        parent = parent.getParent();
                    }
                    if (parent != null) {
                        parent.getClass().getMethod("refreshMenuColors").invoke(parent);
                    }
                } catch (Exception ex) {}

                window.revalidate();
                window.repaint();
            }
        });
        togglePanel.add(modeToggle, BorderLayout.EAST);
        togglePanel.putClientProperty("innerLabel", modeLabel);

        // --- 2. CURRENCY SETTINGS ---
        currencyPanel = createGlassPanel(new GridLayout(2, 2, 10, 10));
        currencyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100)); 
        currencyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel curLabel = new JLabel("Default Currency:");
        currencyPanel.add(curLabel);
        
        String[] currencies = {
            "MMK", "USD ($)", "EUR (€)", "GBP (£)", "JPY (¥)", "SGD ($)",
            "THB (฿)", "VND (₫)", "IDR (Rp)", "MYR (RM)", "PHP (₱)", "KRW (₩)"
        };
        JComboBox<String> currencyBox = new JComboBox<>(currencies);
        currencyBox.setSelectedItem("MMK");
        
        JButton saveCurrencyBtn = new JButton("Save Currency"); 
        saveCurrencyBtn.setBackground(Color.DARK_GRAY); saveCurrencyBtn.setForeground(Color.WHITE); 
        saveCurrencyBtn.setOpaque(true); saveCurrencyBtn.setBorderPainted(false);
        saveCurrencyBtn.addActionListener(e -> {
            String selected = (String) currencyBox.getSelectedItem();
            if (selected.contains("MMK")) currentCurrency = "MMK";
            else if (selected.contains("USD")) currentCurrency = "USD";
            else if (selected.contains("EUR")) currentCurrency = "EUR";
            else if (selected.contains("GBP")) currentCurrency = "GBP";
            else if (selected.contains("JPY")) currentCurrency = "JPY";
            else if (selected.contains("SGD")) currentCurrency = "SGD";
            else if (selected.contains("THB")) currentCurrency = "THB";
            else if (selected.contains("VND")) currentCurrency = "VND";
            else if (selected.contains("IDR")) currentCurrency = "IDR";
            else if (selected.contains("MYR")) currentCurrency = "MYR";
            else if (selected.contains("PHP")) currentCurrency = "PHP";
            else if (selected.contains("KRW")) currentCurrency = "KRW";
            
            JOptionPane.showMessageDialog(this, "Currency successfully changed to " + currentCurrency + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });
        
        currencyPanel.add(currencyBox); 
        currencyPanel.add(saveCurrencyBtn);
        currencyPanel.putClientProperty("innerLabel", curLabel);

        // --- 3. ACCOUNT SETTINGS ---
        accPanel = createGlassPanel(new GridLayout(3, 2, 10, 10));
        accPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130)); 
        accPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel userLbl = new JLabel("Username:");
        JLabel emailLbl = new JLabel("Email:");
        JLabel passLbl = new JLabel("Password:");
        
        accPanel.add(userLbl);
        userField = new JTextField("Guest"); userField.setEditable(false); accPanel.add(userField);
        accPanel.add(emailLbl);
        emailField = new JTextField("Not Logged In"); emailField.setEditable(false); accPanel.add(emailField);
        accPanel.add(passLbl);
        
        JButton changePassBtn = new JButton("Change Password"); 
        changePassBtn.setBackground(Color.LIGHT_GRAY); 
        changePassBtn.setForeground(Color.BLACK);
        changePassBtn.setOpaque(true);
        changePassBtn.setBorderPainted(false);
        changePassBtn.setRolloverEnabled(false);
        changePassBtn.addActionListener(e -> showChangePasswordDialog());
        accPanel.add(changePassBtn);
        
        accPanel.putClientProperty("userLbl", userLbl);
        accPanel.putClientProperty("emailLbl", emailLbl);
        accPanel.putClientProperty("passLbl", passLbl);

        // --- 4. DANGER ZONE ---
        dangerPanel = createGlassPanel(new BorderLayout(15, 0));
        dangerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80)); 
        dangerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        dangerDesc = new JLabel("<html><span style='color:white;'>Permanently wipe all expenses, budgets, bills, reports, and savings goals.</span></html>");
        
        JButton clearDataBtn = new JButton("Clear All Data");
        clearDataBtn.setBackground(new Color(220, 53, 69)); clearDataBtn.setForeground(Color.WHITE);
        clearDataBtn.setFont(new Font("SansSerif", Font.BOLD, 12)); clearDataBtn.setOpaque(true); clearDataBtn.setBorderPainted(false);
        
        clearDataBtn.addActionListener(e -> {
            if (!SessionManager.isLoggedIn()) return;
            int confirm = JOptionPane.showConfirmDialog(this, "Wipe ALL financial data?\nThis CANNOT be undone!", "WARNING", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                String username = SessionManager.getCurrentUser().getUsername();
                try (Connection conn = DatabaseHelper.connect()) {
                    conn.prepareStatement("DELETE FROM expenses WHERE username = '" + username + "'").executeUpdate();
                    conn.prepareStatement("DELETE FROM budgets WHERE username = '" + username + "'").executeUpdate();
                    conn.prepareStatement("DELETE FROM goals WHERE username = '" + username + "'").executeUpdate();
                    conn.prepareStatement("DELETE FROM savings_transactions WHERE username = '" + username + "'").executeUpdate();
                    conn.prepareStatement("DELETE FROM bills WHERE username = '" + username + "'").executeUpdate();
                    conn.prepareStatement("DELETE FROM debts WHERE username = '" + username + "'").executeUpdate();
                    JOptionPane.showMessageDialog(this, "Data permanently deleted.", "Cleared", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {}
            }
        });

        dangerPanel.add(dangerDesc, BorderLayout.CENTER); dangerPanel.add(clearDataBtn, BorderLayout.EAST);

        // --- 5. LOGOUT BUTTON ---
        JButton logoutBtn = new JButton("Logout"); 
        logoutBtn.setBackground(Color.BLACK); logoutBtn.setForeground(Color.WHITE); 
        logoutBtn.setOpaque(true); logoutBtn.setBorderPainted(false);
        logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); 
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        logoutBtn.addActionListener(e -> {
            if (rootApp != null) {
                if (modeToggle.isSelected()) modeToggle.doClick(); 
                SessionManager.logout();
                JFrame mainFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
                if (mainFrame != null) {
                    mainFrame.getContentPane().removeAll();
                    mainFrame.setContentPane(new LoginScreen(rootApp));
                    mainFrame.revalidate();
                    mainFrame.repaint();
                }
            }
        });

        add(mainTitle); add(Box.createRigidArea(new Dimension(0, 15)));
        add(togglePanel); add(Box.createRigidArea(new Dimension(0, 15)));
        add(currencyPanel); add(Box.createRigidArea(new Dimension(0, 15)));
        add(accPanel); add(Box.createRigidArea(new Dimension(0, 15)));
        add(dangerPanel); add(Box.createRigidArea(new Dimension(0, 20))); add(logoutBtn);

        this.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                if (SessionManager.isLoggedIn()) {
                    userField.setText(SessionManager.getCurrentUser().getUsername()); 
                    emailField.setText(SessionManager.getCurrentUser().getEmail());
                }
                updateThemeColors(); // Fix initial colors
            }
        });
    }

    // --- HELPER: Glass Panel Generator ---
    private JPanel createGlassPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
            	super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setComposite(AlphaComposite.SrcOver);
                
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setDoubleBuffered(true);
        return panel;
    }

    // --- OVERRIDE OLD THEME ENGINE WITH GLASSMORPHISM ---
    private void updateThemeColors() {
        boolean isDark = getBackground().getRed() < 100;
        
        mainTitle.setForeground(isDark ? Color.WHITE : Color.BLACK);
        
        // Setup Glass Background Colors
        Color cardBg = isDark ? new Color(60, 63, 65, 140) : new Color(255, 255, 255, 160);
        
        togglePanel.setBackground(cardBg);
        currencyPanel.setBackground(cardBg);
        accPanel.setBackground(cardBg);
        dangerPanel.setBackground(isDark ? new Color(220, 53, 69, 50) : new Color(255, 100, 100, 40));

        // Setup Glass Borders instead of Solid Lines
        Color titleColor = isDark ? Color.WHITE : Color.BLACK;
        togglePanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        currencyPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Currency Selection", 0, 0, null, titleColor), BorderFactory.createEmptyBorder(5, 15, 15, 15)));
        accPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Account Settings", 0, 0, null, titleColor), BorderFactory.createEmptyBorder(5, 15, 15, 15)));
        dangerPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Delete All Your Data", 0, 0, null, new Color(220, 53, 69)), BorderFactory.createEmptyBorder(5, 15, 15, 15)));

        // Fix inner text colors
        ((JLabel) togglePanel.getClientProperty("innerLabel")).setForeground(titleColor);
        ((JLabel) currencyPanel.getClientProperty("innerLabel")).setForeground(titleColor);
        ((JLabel) accPanel.getClientProperty("userLbl")).setForeground(titleColor);
        ((JLabel) accPanel.getClientProperty("emailLbl")).setForeground(titleColor);
        ((JLabel) accPanel.getClientProperty("passLbl")).setForeground(titleColor);
        
        dangerDesc.setForeground(isDark ? new Color(200, 200, 200) : Color.DARK_GRAY);
        
        // Translucent input fields
        Color fieldBg = isDark ? new Color(50, 50, 50) : new Color(255, 255, 255);

        userField.setOpaque(true);
        userField.setBackground(fieldBg);
        userField.setForeground(titleColor);

        emailField.setOpaque(true);
        emailField.setBackground(fieldBg);
        emailField.setForeground(titleColor);
        
        userField.setForeground(titleColor);
        userField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, titleColor));
        
        emailField.setForeground(titleColor);
        emailField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, titleColor));
    }

    private void showChangePasswordDialog() {
        if (!SessionManager.isLoggedIn()) return;

        JPasswordField currentPassField = new JPasswordField(15);
        JPasswordField newPassField = new JPasswordField(15);
        JPasswordField confirmPassField = new JPasswordField(15);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.add(new JLabel("Current Password:"));
        panel.add(createPasswordPanel(currentPassField));
        
        panel.add(new JLabel("New Password:"));
        panel.add(createPasswordPanel(newPassField));
        
        panel.add(new JLabel("Confirm New Password:"));
        panel.add(createPasswordPanel(confirmPassField));

        int result = JOptionPane.showConfirmDialog(this, panel, "Change Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String currentPass = new String(currentPassField.getPassword());
            String newPass = new String(newPassField.getPassword());
            String confirmPass = new String(confirmPassField.getPassword());

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "New passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!isValidPassword(newPass)) {
                JOptionPane.showMessageDialog(this, "Password is too weak. It must contain:\n- At least 8 characters\n- At least one uppercase letter\n- At least one number\n- At least one special character (!@#$%^&*)", "Weak Password", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String username = SessionManager.getCurrentUser().getUsername();

            try (Connection conn = DatabaseHelper.connect()) {
                String verifySql = "SELECT password FROM users WHERE username = ?";
                PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
                verifyStmt.setString(1, username);
                ResultSet rs = verifyStmt.executeQuery();

                if (rs.next()) {
                    String dbPass = rs.getString("password");
                    if (!dbPass.equals(currentPass)) {
                        JOptionPane.showMessageDialog(this, "Incorrect current password.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    String updateSql = "UPDATE users SET password = ? WHERE username = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                    updateStmt.setString(1, newPass);
                    updateStmt.setString(2, username);
                    updateStmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Password updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (SQLException ex) {}
        }
    }

    private JPanel createPasswordPanel(JPasswordField passField) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setOpaque(false);
        
        JToggleButton toggleBtn = new JToggleButton("Show");
        toggleBtn.setMargin(new Insets(2, 5, 2, 5));
        toggleBtn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        toggleBtn.setFocusPainted(false);
        toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        char defaultEchoChar = passField.getEchoChar();
        
        toggleBtn.addActionListener(e -> {
            if (toggleBtn.isSelected()) {
                passField.setEchoChar((char) 0);
                toggleBtn.setText("Hide");
            } else {
                passField.setEchoChar(defaultEchoChar);
                toggleBtn.setText("Show");
            }
        });
        
        panel.add(passField, BorderLayout.CENTER);
        panel.add(toggleBtn, BorderLayout.EAST);
        return panel;
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) return false;
        if (!password.matches(".*[A-Z].*")) return false;
        if (!password.matches(".*[0-9].*")) return false; 
        if (!password.matches(".*[^a-zA-Z0-9].*")) return false;
        return true;
    }

    // THEME ENGINE CODE (Kept for external panel updates)
    private boolean isProtectedPastel(Color c) {
        if (c == null) return false;
        int max = Math.max(c.getRed(), Math.max(c.getGreen(), c.getBlue())); int min = Math.min(c.getRed(), Math.min(c.getGreen(), c.getBlue()));
        if (c.equals(Color.WHITE) || c.equals(Color.BLACK) || c.equals(new Color(248, 249, 250)) || c.equals(new Color(245, 245, 245))) return false; 
        return max > 200 && (max - min) > 10;
    }

    private void applyTheme(Component c, boolean isDark) {
        if (c instanceof JLabel) {
            JLabel lbl = (JLabel) c;
            if (isGrayscale(lbl.getForeground())) {
                Color effectiveBg = lbl.isOpaque() ? lbl.getBackground() : (lbl.getParent() != null ? lbl.getParent().getBackground() : Color.WHITE);
                if (isLightColor(effectiveBg) || isProtectedPastel(effectiveBg)) { if (lbl.getForeground().equals(Color.WHITE)) lbl.setForeground(Color.BLACK); } 
                else lbl.setForeground(isDark ? Color.WHITE : Color.BLACK);
            }
        }
        else if (c instanceof JTextField) {
            JTextField tf = (JTextField) c;
            if (isDark) { tf.setBackground(new Color(60, 60, 60)); tf.setForeground(Color.WHITE); tf.setCaretColor(Color.WHITE); } 
            else { tf.setBackground(new Color(245, 245, 245)); tf.setForeground(Color.BLACK); tf.setCaretColor(Color.BLACK); }
        }
        else if (c instanceof JComboBox) { ((JComboBox<?>) c).setForeground(Color.BLACK); }
        else if (c instanceof JButton || c instanceof JToggleButton) {
            AbstractButton btn = (AbstractButton) c; String text = btn.getText();
            if (text != null && text.toLowerCase().contains("account")) {
                btn.setBorderPainted(false); if (btn instanceof JButton) ((JButton)btn).setContentAreaFilled(false);
                btn.setOpaque(true); btn.setBackground(btn.getParent() != null ? btn.getParent().getBackground() : Color.WHITE); btn.setForeground(Color.DARK_GRAY);
            } else {
                btn.setOpaque(true); btn.setBorderPainted(false);
                if (isDark) {
                    if (text != null && text.equals("Clear All Data")) { btn.setBackground(new Color(220, 53, 69)); btn.setForeground(Color.WHITE); } 
                    else if (text != null && (text.equals("Logout") || text.equals("Login") || text.equals("Register"))) { btn.setBackground(Color.BLACK); btn.setForeground(Color.WHITE); } 
                    else { btn.setBackground(new Color(80, 80, 80)); btn.setForeground(Color.WHITE); }
                } else {
                    if (text != null && text.equals("Clear All Data")) { btn.setBackground(new Color(220, 53, 69)); btn.setForeground(Color.WHITE); } 
                    else if (text != null && (text.equals("Logout") || text.equals("Login") || text.equals("Register"))) { btn.setBackground(Color.BLACK); btn.setForeground(Color.WHITE); } 
                    else if (text != null && text.equals("Save Currency")) { btn.setBackground(Color.DARK_GRAY); btn.setForeground(Color.WHITE); } 
                    else { btn.setBackground(Color.LIGHT_GRAY); btn.setForeground(Color.BLACK); }
                }
            }
        }
        else if (c instanceof JPanel || c instanceof JScrollPane || c.getClass().getName().equals("javax.swing.JViewport") || c instanceof JTable) {
            Color bg = c.getBackground();
            if (bg != null && isGrayscale(bg) && !isProtectedPastel(bg)) {
                if (isDark && isLightColor(bg)) c.setBackground(new Color(43, 43, 43));
                else if (!isDark && !isLightColor(bg)) c.setBackground(Color.WHITE);
            }
            if (c instanceof JTable) {
                JTable table = (JTable) c; table.setForeground(isDark ? Color.WHITE : Color.BLACK);
                table.getTableHeader().setBackground(isDark ? new Color(43, 43, 43) : Color.LIGHT_GRAY);
                table.getTableHeader().setForeground(isDark ? Color.WHITE : Color.BLACK);
            }
            if (c instanceof JComponent && ((JComponent)c).getBorder() != null) { updateBorderColor(((JComponent)c).getBorder(), isDark); c.repaint(); }
        }
        if (c instanceof Container) for (Component child : ((Container) c).getComponents()) applyTheme(child, isDark);
    }

    private boolean isGrayscale(Color c) {
        if (c == null) return false;
        int max = Math.max(c.getRed(), Math.max(c.getGreen(), c.getBlue())); int min = Math.min(c.getRed(), Math.min(c.getGreen(), c.getBlue()));
        return (max - min) <= 30; 
    }

    private boolean isLightColor(Color c) {
        if (c == null) return true;
        return (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) > 128;
    }

    private void updateBorderColor(javax.swing.border.Border b, boolean isDark) {
        if (b instanceof javax.swing.border.TitledBorder) ((javax.swing.border.TitledBorder) b).setTitleColor(isDark ? Color.WHITE : Color.BLACK);
        else if (b instanceof javax.swing.border.CompoundBorder) {
            updateBorderColor(((javax.swing.border.CompoundBorder) b).getOutsideBorder(), isDark); updateBorderColor(((javax.swing.border.CompoundBorder) b).getInsideBorder(), isDark);
        }
    }
}