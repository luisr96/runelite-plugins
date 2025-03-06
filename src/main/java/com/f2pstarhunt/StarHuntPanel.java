package com.f2pstarhunt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;

public class StarHuntPanel extends PluginPanel
{
    private final F2PStarHuntPlugin plugin;
    private final F2PStarHuntConfig config;

    private final JPanel starsContainer = new JPanel();
    private final JPanel noStarsPanel = new JPanel();
    private final JLabel connectionStatusLabel = new JLabel();
    private final JPanel connectionPanel = new JPanel();

    @Inject
    public StarHuntPanel(F2PStarHuntPlugin plugin, F2PStarHuntConfig config)
    {
        this.plugin = plugin;
        this.config = config;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel titlePanel = new JPanel();
        titlePanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        titlePanel.setLayout(new BorderLayout());
        titlePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("Shooting Stars");
        title.setForeground(Color.WHITE);
        title.setFont(FontManager.getRunescapeBoldFont());
        titlePanel.add(title, BorderLayout.WEST);

        connectionPanel.setLayout(new BorderLayout(5, 0));
        connectionPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        connectionStatusLabel.setForeground(Color.RED);
        connectionStatusLabel.setText("Disconnected");
        connectionStatusLabel.setFont(FontManager.getRunescapeSmallFont());
        connectionPanel.add(connectionStatusLabel, BorderLayout.WEST);

        titlePanel.add(connectionPanel, BorderLayout.EAST);

        add(titlePanel, BorderLayout.NORTH);

        starsContainer.setLayout(new BoxLayout(starsContainer, BoxLayout.Y_AXIS));
        starsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        noStarsPanel.setLayout(new BorderLayout());
        noStarsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        PluginErrorPanel errorPanel = new PluginErrorPanel();
        errorPanel.setContent("No Stars Found", "No shooting stars are currently being tracked.");
        noStarsPanel.add(errorPanel, BorderLayout.CENTER);

        add(starsContainer, BorderLayout.CENTER);

        updatePanel();
    }

    /**
     * Updates the panel with current star data
     */
    public void updatePanel() {
        starsContainer.removeAll();

        List<Star> allStars = new ArrayList<>(plugin.getRemoteStars());
        if (allStars.isEmpty()) {
            starsContainer.add(noStarsPanel);
        } else {
            for (Star star : allStars) {
                starsContainer.add(createStarPanel(star));
                starsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        starsContainer.revalidate();
        starsContainer.repaint();
        this.revalidate();
        this.repaint();
    }

    /**
     * Updates the connection status display
     */
    public void updateConnectionStatus(boolean connected) {
        connectionPanel.removeAll();

        if (connected) {
            connectionStatusLabel.setText("Connected");
            connectionStatusLabel.setForeground(new Color(0, 150, 0)); // Dark green
            connectionPanel.add(connectionStatusLabel, BorderLayout.WEST);
        } else {
            JButton connectButton = new JButton("Connect");
            connectButton.setFocusPainted(false);
            connectButton.setFont(FontManager.getRunescapeSmallFont());
            connectButton.addActionListener(e -> plugin.connectToWebSocket());

            connectionPanel.add(connectButton, BorderLayout.EAST);
        }

        connectionPanel.revalidate();
        connectionPanel.repaint();
        revalidate();
        repaint();
    }

    /**
     * Creates a panel for an individual star
     */
    private JPanel createStarPanel(Star star)
    {
        JPanel panel = new JPanel();
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_HOVER_COLOR, 1));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Info section
        JPanel infoPanel = new JPanel(new GridLayout(0, 1, 0, 3));
        infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // World information
        JLabel worldLabel = new JLabel("World: " + star.getWorld());
        worldLabel.setForeground(Color.WHITE);
        infoPanel.add(worldLabel);

        // Tier information
        int tier = star.getTier() != -1 ? star.getTier() : star.getRemoteTier();
        JLabel tierLabel = new JLabel("Tier: " + tier);
        tierLabel.setForeground(Color.WHITE);
        infoPanel.add(tierLabel);

        // Location
        String locationDesc = (star.getLocation() != null)
                ? star.getLocation().getDescription()
                : "Unknown Location";
        JLabel locationLabel = new JLabel("Location: " + locationDesc);
        locationLabel.setForeground(Color.WHITE);
        infoPanel.add(locationLabel);

        // Health
        int health = star.getHealth();
        if (health >= 0)
        {
            JLabel healthLabel = new JLabel("Health: " + health + "%");
            healthLabel.setForeground(Color.WHITE);
            infoPanel.add(healthLabel);
        }

        // Miners
        if (!star.getMiners().equals(Star.UNKNOWN_MINERS))
        {
            JLabel minersLabel = new JLabel("Miners: " + star.getMiners());
            minersLabel.setForeground(Color.WHITE);
            infoPanel.add(minersLabel);
        }

        // Direct time information from server
        if (star.getLayerTime() != null && !star.getLayerTime().isEmpty())
        {
            JLabel layerTimeLabel = new JLabel("Layer done in: " + star.getLayerTime());
            layerTimeLabel.setForeground(Color.WHITE);
            infoPanel.add(layerTimeLabel);
        }

        if (star.getDepleteTime() != null && !star.getDepleteTime().isEmpty())
        {
            JLabel depleteLabel = new JLabel("Depletes in: " + star.getDepleteTime());
            depleteLabel.setForeground(Color.WHITE);
            infoPanel.add(depleteLabel);
        }

        // First found time
        if (star.getFirstFound() != null && !star.getFirstFound().isEmpty())
        {
            try {
                // Parse the ISO date string
                Instant firstFoundTime = Instant.parse(star.getFirstFound());
                // Get current time
                Instant now = Instant.now();
                // Calculate duration between now and when the star was found
                long durationSeconds = java.time.Duration.between(firstFoundTime, now).getSeconds();

                // Format the duration nicely
                String formattedDuration;
                if (durationSeconds < 60) {
                    formattedDuration = durationSeconds + " sec ago";
                } else if (durationSeconds < 3600) {
                    formattedDuration = (durationSeconds / 60) + " min ago";
                } else {
                    formattedDuration = (durationSeconds / 3600) + " hr " +
                            ((durationSeconds % 3600) / 60) + " min ago";
                }

                JLabel foundLabel = new JLabel("Found: " + formattedDuration);
                foundLabel.setForeground(Color.WHITE);
                infoPanel.add(foundLabel);
            } catch (Exception e) {
                // If date parsing fails, just display the raw date
                JLabel foundLabel = new JLabel("Found: " + star.getFirstFound());
                foundLabel.setForeground(Color.WHITE);
                infoPanel.add(foundLabel);
            }
        }

        // Last update time
        if (star.getLastUpdate() != null && !star.getLastUpdate().isEmpty())
        {
            try {
                // Parse the ISO date string
                Instant lastUpdateTime = Instant.parse(star.getLastUpdate());
                // Get current time
                Instant now = Instant.now();
                // Calculate duration between now and when the star was last updated
                long durationSeconds = java.time.Duration.between(lastUpdateTime, now).getSeconds();

                // Format the duration nicely
                String formattedDuration;
                if (durationSeconds < 60) {
                    formattedDuration = durationSeconds + " sec ago";
                } else if (durationSeconds < 3600) {
                    formattedDuration = (durationSeconds / 60) + " min ago";
                } else {
                    formattedDuration = (durationSeconds / 3600) + " hr " +
                            ((durationSeconds % 3600) / 60) + " min ago";
                }

                JLabel updateLabel = new JLabel("Updated: " + formattedDuration);
                updateLabel.setForeground(new Color(190, 190, 190));  // Light gray
                updateLabel.setFont(FontManager.getRunescapeSmallFont());
                infoPanel.add(updateLabel);
            } catch (Exception e) {
                // If date parsing fails, just display the raw date
                JLabel updateLabel = new JLabel("Updated: " + star.getLastUpdate());
                updateLabel.setForeground(new Color(190, 190, 190));  // Light gray
                updateLabel.setFont(FontManager.getRunescapeSmallFont());
                infoPanel.add(updateLabel);
            }
        }

        // Backup indicator
        if (star.isBackup()) {
            JLabel backupLabel = new JLabel("Backup Star");
            backupLabel.setForeground(new Color(255, 200, 0));  // Gold color
            backupLabel.setFont(FontManager.getRunescapeSmallFont());
            infoPanel.add(backupLabel);
        }

        panel.add(infoPanel);
        return panel;
    }
}