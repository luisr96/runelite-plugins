package com.f2pstarhunt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
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
    public void updatePanel()
    {
        starsContainer.removeAll();

        if (plugin.stars.isEmpty())
        {
            starsContainer.add(noStarsPanel);
        }
        else
        {
            for (Star star : plugin.stars)
            {
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

        // World and Tier
        JPanel worldTierPanel = new JPanel(new BorderLayout());
        worldTierPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel worldLabel = new JLabel("World: " + star.getWorld());
        worldLabel.setForeground(Color.WHITE);
        worldTierPanel.add(worldLabel, BorderLayout.WEST);

        JLabel tierLabel = new JLabel("Tier: " + star.getTier());
        tierLabel.setForeground(Color.WHITE);
        worldTierPanel.add(tierLabel, BorderLayout.EAST);

        infoPanel.add(worldTierPanel);

        // Location
        JLabel locationLabel = new JLabel("Location: " + star.getLocation().getDescription());
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

        // Estimated time remaining based on config settings
        if (star.getTierTicksEstimate() != null)
        {
            // Time until layer finishes
            if (star.getTier() <= star.getTierTicksEstimate().length && star.getTier() > 0 &&
                    config.estimateLayerTime() != EstimateConfig.NONE)
            {
                int layerTicks = star.getTierTicksEstimate()[star.getTier() - 1];
                String timeText = "Layer done in: ";

                if (config.estimateLayerTime() == EstimateConfig.TICKS)
                {
                    timeText += layerTicks;
                }
                else // SECONDS
                {
                    int seconds = (layerTicks % 100) * 3 / 5;
                    int minutes = layerTicks / 100;
                    timeText += minutes + ":" + String.format("%02d", seconds);
                }

                JLabel layerTimeLabel = new JLabel(timeText);
                layerTimeLabel.setForeground(Color.WHITE);
                infoPanel.add(layerTimeLabel);
            }

            // Time until star depletes
            if (config.estimateDeathTime() != EstimateConfig.NONE)
            {
                int depletesTicks = star.getTierTicksEstimate()[0];
                String timeText = "Depletes in: ";

                if (config.estimateDeathTime() == EstimateConfig.TICKS)
                {
                    timeText += depletesTicks;
                }
                else // SECONDS
                {
                    int seconds = (depletesTicks % 100) * 3 / 5;
                    int minutes = depletesTicks / 100;
                    timeText += minutes + ":" + String.format("%02d", seconds);
                }

                JLabel depletesLabel = new JLabel(timeText);
                depletesLabel.setForeground(Color.WHITE);
                infoPanel.add(depletesLabel);
            }
        }

        panel.add(infoPanel);
        return panel;
    }
}