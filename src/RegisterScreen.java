import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterScreen extends JPanel {
    private FinanceBuddy rootApp;
    private JTextField userField;
    private JTextField emailField;
    private JPasswordField passField;
    private JPasswordField confirmPassField;
    private Image backgroundImage;

    public RegisterScreen(FinanceBuddy app) {
        this.rootApp = app;
        
        // Load the background image
        try {
            backgroundImage = new ImageIcon("rmbg.png").getImage();
        } catch (Exception e) {
            System.out.println("Background image not found. Ensure rmbg.png is in the project folder.");
        }
        
        setLayout(new GridBagLayout()); 
        setOpaque(false); // Let the custom paintComponent show through

        // --- Main Container ---
        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setOpaque(false);

        // --- Header ---
        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        titleLabel.setForeground(new Color(20, 30, 40)); 
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subLabel = new JLabel("Join Finance Buddy today.");
        subLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        subLabel.setForeground(new Color(60, 70, 80));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Card Panel (Frosted Glass) ---
        GlassRegisterCard cardPanel = new GlassRegisterCard();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

        // --- Form Fields ---
        userField = new JTextField();
        styleTextField(userField);
        
        emailField = new JTextField();
        styleTextField(emailField);

        passField = new JPasswordField();
        JPanel passWrapper = wrapPasswordField(passField);

        confirmPassField = new JPasswordField();
        JPanel confirmPassWrapper = wrapPasswordField(confirmPassField);

        addLabeledField(cardPanel, "Username (Min 6 chars)", userField);
        addLabeledField(cardPanel, "Email Address", emailField);
        addLabeledField(cardPanel, "Password", passWrapper);
        addLabeledField(cardPanel, "Confirm Password", confirmPassWrapper);

        // --- Buttons ---
        JButton registerBtn = new NeonButton("Register");
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(300, 45));
        registerBtn.addActionListener(e -> attemptRegistration());

        JButton backBtn = new JButton("Already have an account? Login Here");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        backBtn.setForeground(new Color(80, 90, 100));
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        backBtn.addActionListener(e -> {
            rootApp.setContentPane(new LoginScreen(rootApp));
            rootApp.revalidate();
            rootApp.repaint();
        });

        // --- Assemble Everything ---
        cardPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        cardPanel.add(registerBtn);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        cardPanel.add(backBtn);

        // Add to main container with spacing
        mainContainer.add(titleLabel);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 5)));
        mainContainer.add(subLabel);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 20))); 
        mainContainer.add(cardPanel);

        add(mainContainer);
    }

    // --- Draw the Background Image & Gradient ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Draw the Green-to-Blue photo gradient background
        Color color1 = new Color(175, 240, 185); 
        Color color2 = new Color(135, 205, 245); 
        GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
        
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // 2. Draw the transparent robot image over the gradient
        if (backgroundImage != null) {
            int imgWidth = backgroundImage.getWidth(this);
            int imgHeight = backgroundImage.getHeight(this);
            
            double scale = Math.min((double)getWidth() / imgWidth, (double)getHeight() / imgHeight) * 0.9;
            int newWidth = (int)(imgWidth * scale);
            int newHeight = (int)(imgHeight * scale);
            int x = (getWidth() - newWidth) / 2;
            int y = (getHeight() - newHeight) / 2;

            g2d.drawImage(backgroundImage, x, y, newWidth, newHeight, this);
        }
        
        g2d.dispose();
    }

    // --- Layout Helpers ---
    
    private void styleTextField(JTextField tf) {
        tf.setMaximumSize(new Dimension(300, 40));
        tf.setBackground(new Color(255, 255, 255, 220)); 
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 215, 225), 1, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        tf.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void addLabeledField(JPanel parent, String labelText, Component field) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(300, 70));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(new Color(40, 50, 60));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        wrapper.add(label);
        wrapper.add(Box.createRigidArea(new Dimension(0, 5))); 
        wrapper.add(field);

        parent.add(wrapper);
        parent.add(Box.createRigidArea(new Dimension(0, 15))); 
    }

    private JPanel wrapPasswordField(JPasswordField pf) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(255, 255, 255, 220));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 215, 225), 1, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        panel.setMaximumSize(new Dimension(300, 40));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setOpaque(false);

        pf.setBorder(null); 
        pf.setBackground(new Color(255, 255, 255, 0)); 
        pf.setOpaque(false);
        pf.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        char defaultEchoChar = pf.getEchoChar();
        if (defaultEchoChar == 0) defaultEchoChar = '•';
        pf.setEchoChar(defaultEchoChar);

        JButton toggleButton = new JButton("Show");
        toggleButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        toggleButton.setForeground(new Color(80, 90, 100));
        toggleButton.setFocusPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        char finalDefaultEchoChar = defaultEchoChar;
        toggleButton.addActionListener(e -> {
            if (pf.getEchoChar() != 0) {
                pf.setEchoChar((char) 0);
                toggleButton.setText("Hide");
            } else {
                pf.setEchoChar(finalDefaultEchoChar);
                toggleButton.setText("Show");
            }
        });

        // Override wrapper to draw semi-transparent background
        JPanel customWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 220)); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        customWrapper.setOpaque(false);
        customWrapper.setBorder(panel.getBorder());
        customWrapper.setMaximumSize(panel.getMaximumSize());
        customWrapper.setAlignmentX(panel.getAlignmentX());
        
        customWrapper.add(pf, BorderLayout.CENTER);
        customWrapper.add(toggleButton, BorderLayout.EAST);
        return customWrapper;
    }

    // --- Logic ---
    
    private boolean validateInput(String username, String email, String password, String confirmPass) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (username.length() < 6) {
            JOptionPane.showMessageDialog(this, "Username must be at least 6 characters long.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address format (e.g., user@example.com).", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (!password.matches(".*[A-Z].*")) {
            JOptionPane.showMessageDialog(this, "Password must contain at least one Capital Letter (A-Z).", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (!password.matches(".*[0-9].*")) {
            JOptionPane.showMessageDialog(this, "Password must contain at least one Number (0-9).", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            JOptionPane.showMessageDialog(this, "Password must contain at least one Special Character (e.g., !@#$%).", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (!password.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match. Please try again.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }

    private void attemptRegistration() {
        String username = userField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passField.getPassword());
        String confirmPass = new String(confirmPassField.getPassword());

        if (!validateInput(username, email, password, confirmPass)) {
            return; 
        }

        try (Connection conn = DatabaseHelper.connect()) {
            PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Username already exists. Please choose a different one.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO users (username, password, email) VALUES (?, ?, ?)");
            insertStmt.setString(1, username);
            insertStmt.setString(2, password);
            insertStmt.setString(3, email);
            insertStmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Registration Successful! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
            
            rootApp.setContentPane(new LoginScreen(rootApp));
            rootApp.revalidate();
            rootApp.repaint();

        } catch (SQLException ex) {
            if (ex.getMessage().contains("UNIQUE constraint failed") || ex.getMessage().contains("Duplicate")) {
                JOptionPane.showMessageDialog(this, "Username already exists. Please choose another one.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================
    // CUSTOM UI COMPONENTS
    // =========================================================

    /** Custom Registration Card with rounded corners and frosted glass feel */
    class GlassRegisterCard extends JPanel {
        public GlassRegisterCard() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw Frosted Glass Background (White with 80% opacity)
            g2.setColor(new Color(255, 255, 255, 200));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 30, 30));
            
            // Draw Subtle Border to catch the "glass" edge
            g2.setColor(new Color(255, 255, 255, 255));
            g2.setStroke(new BasicStroke(2));
            g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 3, getHeight() - 3, 30, 30));
            
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Custom Styled Registration Button */
    class NeonButton extends JButton {
        public NeonButton(String text) {
            super(text);
            setFont(new Font("SansSerif", Font.BOLD, 16));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (getModel().isRollover()) {
                g2.setColor(new Color(50, 190, 230)); 
            } else {
                g2.setColor(new Color(20, 160, 200)); 
            }
            
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
            super.paintComponent(g);
            g2.dispose();
        }
    }
}