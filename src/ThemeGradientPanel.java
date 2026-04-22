import javax.swing.*;
import java.awt.*;

public class ThemeGradientPanel extends JPanel {

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // The Dark Mode engine in SettingsPanel sets the background color. 
        // We read that color to decide which gradient to draw.
        boolean isDark = getBackground().getRed() < 100;

        if (isDark) {
            // Dark Mode Gradient: Deep Slate to Dark Neon Teal/Blue
            Color color1 = new Color(20, 25, 30); 
            Color color2 = new Color(15, 45, 55); 
            GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
            g2d.setPaint(gp);
        } else {
            // Light Mode Gradient: Pastel Green to Pastel Blue (Matches your Login)
            Color color1 = new Color(175, 240, 185); 
            Color color2 = new Color(135, 205, 245); // Adjust if you want a different tint
            GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
            g2d.setPaint(gp);
        }

        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.dispose();
    }
}