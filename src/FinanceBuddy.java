import javax.swing.*;
import java.awt.*;

public class FinanceBuddy extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    public FinanceBuddy() {
        try {
            java.net.URL imgURL = getClass().getResource("logo.jpeg");
            if (imgURL != null) {
                Image originalImage = Toolkit.getDefaultToolkit().getImage(imgURL);
                Image scaledImage = originalImage.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                javax.swing.ImageIcon imgIcon = new javax.swing.ImageIcon(scaledImage);
                this.setIconImage(imgIcon.getImage());
                
                if (Taskbar.isTaskbarSupported()) {
                    Taskbar.getTaskbar().setIconImage(imgIcon.getImage());
                }
            } else {
                System.out.println("Can't Find logo");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        setTitle("Finance Buddy");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Add Screens
        mainPanel.add(new LoginScreen(this), "LOGIN");
        mainPanel.add(new RegisterScreen(this), "REGISTER");
        mainPanel.add(new MainAppScreen(this), "MAIN_APP");

        add(mainPanel);
        
        // Start at Login
        cardLayout.show(mainPanel, "LOGIN");
    }

    public void navigateTo(String screenName) {
        cardLayout.show(mainPanel, screenName);
    }

    public static void main(String[] args) {
        DatabaseHelper.initDatabase(); 
        
        SwingUtilities.invokeLater(() -> {
            new FinanceBuddy().setVisible(true);
        });
    }

    void navigateInternal(String general) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}