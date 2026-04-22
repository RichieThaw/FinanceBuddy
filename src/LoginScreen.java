import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginScreen extends JPanel {
    private FinanceBuddy rootApp;
    private JTextField userField;
    private JPasswordField passField;
    private Image backgroundImage;

    public LoginScreen(FinanceBuddy app) {
        this.rootApp = app;
        
        // Load the background image
        try {
            backgroundImage = new ImageIcon("rmbg.png").getImage();
        } catch (Exception e) {
            System.out.println("Background image not found. Ensure rmbg.png is in the project folder.");
        }
        
        setLayout(new GridBagLayout()); 
        // We set opaque to false so the custom paintComponent works perfectly
        setOpaque(false); 

        // --- Main Container ---
        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setOpaque(false);

        // --- Header ---
        JLabel titleLabel = new JLabel("Finance Buddy");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        titleLabel.setForeground(new Color(20, 30, 40)); 
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subLabel = new JLabel("Login to manage your finances.");
        subLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        subLabel.setForeground(new Color(60, 70, 80));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Card Panel (Now truly semi-transparent for Glassmorphism) ---
        GlassLoginCard cardPanel = new GlassLoginCard();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

        // --- Form Fields ---
        userField = new JTextField();
        styleTextField(userField);

        passField = new JPasswordField();
        JPanel passWrapper = wrapPasswordField(passField);

        addLabeledField(cardPanel, "Username/Email", userField);
        addLabeledField(cardPanel, "Password", passWrapper);

        // --- Buttons ---
        JButton loginBtn = new NeonButton("Login");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(300, 45));
        loginBtn.addActionListener(e -> attemptLogin());

        JButton registerBtn = new JButton("Don't have an account? Register Here");
        registerBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        registerBtn.setForeground(new Color(80, 90, 100));
        registerBtn.setContentAreaFilled(false);
        registerBtn.setBorderPainted(false);
        registerBtn.setFocusPainted(false);
        registerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        registerBtn.addActionListener(e -> {
            rootApp.setContentPane(new RegisterScreen(rootApp));
            rootApp.revalidate();
            rootApp.repaint();
        });

        // --- Assemble Everything ---
        cardPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        cardPanel.add(loginBtn);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        cardPanel.add(registerBtn);

        // Add to main container with spacing
        mainContainer.add(titleLabel);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 5)));
        mainContainer.add(subLabel);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 30))); 
        mainContainer.add(cardPanel);

        add(mainContainer);
    }

    // --- Draw the Background Image & Gradient ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Draw the beautiful Green-to-Blue photo gradient background
        Color color1 = new Color(175, 240, 185); // Soft pastel green from the left of your photo
        Color color2 = new Color(135, 205, 245); // Soft pastel blue from the right of your photo
        GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
        
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // 2. Draw the transparent robot image over the gradient
        if (backgroundImage != null) {
            int imgWidth = backgroundImage.getWidth(this);
            int imgHeight = backgroundImage.getHeight(this);
            
            // Calculate scale to make it fit nicely in the background
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
        // Semi-transparent field background
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
        wrapper.add(Box.createRigidArea(new Dimension(0, 8))); 
        wrapper.add(field);

        parent.add(wrapper);
        parent.add(Box.createRigidArea(new Dimension(0, 20))); 
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
        panel.setOpaque(false); // Let the custom paint through

        pf.setBorder(null); 
        pf.setBackground(new Color(255, 255, 255, 0)); // Fully transparent so parent draws it
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

        // We need to override the wrapper panel to draw its own background properly
        JPanel customWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 220)); // Semi-transparent white
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
    
    private void attemptLogin() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both Username/Email and Password.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (username.length() < 6) {
            JOptionPane.showMessageDialog(this, "Username must be at least 6 characters long.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean loggedIn = false;
        String sql = "SELECT * FROM users WHERE (username = ? OR email = ?) AND password = ?";
        
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, username);
            pstmt.setString(2, username); 
            pstmt.setString(3, password);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String savedUser = rs.getString("username");
                String savedEmail = rs.getString("email");
                String savedPass = rs.getString("password");

                SessionManager.login(new User(savedUser, savedEmail, savedPass));
                loggedIn = true;
            }
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        if (loggedIn) {
            userField.setText("");
            passField.setText("");
            MainAppScreen mainScreen = new MainAppScreen(rootApp);
            rootApp.setContentPane(mainScreen);
            rootApp.revalidate();
            rootApp.repaint();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid Username or Password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    // CUSTOM UI COMPONENTS
    // =========================================================

    /** Custom Login Card with rounded corners and true frosted glass feel */
    class GlassLoginCard extends JPanel {
        public GlassLoginCard() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
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

    /** Custom Styled Login Button */
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