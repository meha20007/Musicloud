package cloud;

import javax.swing.*;
import java.awt.*;

/**
 * Cloud Status Banner — a small non-intrusive indicator shown at the top
 * of the Swing window to let the user know if they are connected to the cloud.
 *
 * SIMPLE EXPLANATION:
 *   - 🟢 Green pill = Connected to MusicLoud cloud (all data synced)
 *   - 🟡 Yellow pill = Offline mode (using local files, works fine!)
 *
 * Usage: add CloudStatusBanner.createBanner() to your window's NORTH area.
 */
public class CloudStatusBanner extends JPanel {

    private final JLabel label = new JLabel();

    public CloudStatusBanner() {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        setOpaque(false);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        add(label);
        refresh();
    }

    public void refresh() {
        if (ApiClient.isCloudAvailable()) {
            label.setText("🟢 Cloud Connected — " + ApiClient.BASE_URL);
            label.setForeground(new Color(80, 200, 120));
        } else {
            label.setText("🟡 Offline Mode — data saved locally");
            label.setForeground(new Color(255, 200, 50));
        }
    }

    /** Create a banner and start a background health-check on first use */
    public static CloudStatusBanner createBanner() {
        CloudStatusBanner banner = new CloudStatusBanner();
        // Check health once in background — won't block the UI
        new Thread(() -> {
            ApiClient.checkHealth();
            SwingUtilities.invokeLater(banner::refresh);
        }, "cloud-health-check").start();
        return banner;
    }
}
