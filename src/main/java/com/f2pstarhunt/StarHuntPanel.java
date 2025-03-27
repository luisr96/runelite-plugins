package com.f2pstarhunt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import net.runelite.api.FriendsChatRank;
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

    // Dashboard information labels
    private final JLabel waveBeganLabel = new JLabel();
    private final JLabel waveEndsLabel = new JLabel();
    private final JLabel spawnPhaseEndsLabel = new JLabel();
    private final JLabel scoutTimeLabel = new JLabel();

    // Rank info panel
    private JPanel rankInfoPanel;
    private JLabel rankInfoLabel;
    private boolean rankInfoAdded = false;

    // Spawn times panel
    private JPanel spawnTimesPanel;
    private boolean showingSpawnTimes = false;
    private JButton toggleViewButton;

    @Inject
    public StarHuntPanel(F2PStarHuntPlugin plugin, F2PStarHuntConfig config)
    {
        this.plugin = plugin;
        this.config = config;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Create a top section with vertical layout
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // 1. Title panel (first row of top section)
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
        topSection.add(titlePanel);

        // 2. Wave timing information panel (second row of top section)
        JPanel timingPanel = new JPanel();
        timingPanel.setLayout(new GridLayout(3, 1, 0, 3));
        timingPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        timingPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_HOVER_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Initialize timing labels with placeholder values
        scoutTimeLabel.setText("When to scout: Loading...");
        scoutTimeLabel.setForeground(Color.WHITE);
        scoutTimeLabel.setFont(FontManager.getRunescapeSmallFont());

        waveBeganLabel.setText("Wave began: Loading...");
        waveBeganLabel.setForeground(Color.WHITE);
        waveBeganLabel.setFont(FontManager.getRunescapeSmallFont());

        waveEndsLabel.setText("Wave ends: Loading...");
        waveEndsLabel.setForeground(Color.WHITE);
        waveEndsLabel.setFont(FontManager.getRunescapeSmallFont());

        spawnPhaseEndsLabel.setText("Spawn phase ends: Loading...");
        spawnPhaseEndsLabel.setForeground(Color.WHITE);
        spawnPhaseEndsLabel.setFont(FontManager.getRunescapeSmallFont());

        timingPanel.setLayout(new GridLayout(4, 1, 0, 3)); // Update to 4 rows instead of 3
        timingPanel.add(waveBeganLabel);
        timingPanel.add(waveEndsLabel);
        timingPanel.add(scoutTimeLabel); // Add the new label
        timingPanel.add(spawnPhaseEndsLabel);

        JPanel timingContainer = new JPanel(new BorderLayout());
        timingContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        timingContainer.setBorder(new EmptyBorder(0, 0, 10, 0));
        timingContainer.add(timingPanel, BorderLayout.CENTER);

        topSection.add(timingContainer);

        // 3. Button panel (third row of top section)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Create toggle button here
        toggleViewButton = new JButton("View Spawn Times");
        toggleViewButton.setFocusPainted(false);
        toggleViewButton.addActionListener(e -> toggleView());
        buttonPanel.add(toggleViewButton, BorderLayout.CENTER);

        topSection.add(buttonPanel);

        // Add the complete top section to the main panel
        add(topSection, BorderLayout.NORTH);

        // Initialize stars container
        starsContainer.setLayout(new BoxLayout(starsContainer, BoxLayout.Y_AXIS));
        starsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Initialize no stars panel
        noStarsPanel.setLayout(new BorderLayout());
        noStarsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        PluginErrorPanel errorPanel = new PluginErrorPanel();
        errorPanel.setContent("No Stars Found", "No shooting stars are currently being tracked.");
        noStarsPanel.add(errorPanel, BorderLayout.CENTER);

        add(starsContainer, BorderLayout.CENTER);

        // Initialize rank info
        updateRankInfo();

        // Initialize spawn times panel
        initializeSpawnTimesPanel();

        updatePanel();
    }

    /**
     * Updates the wave timing information displayed at the top of the panel
     */
    public void updateWaveTimings(String timeSinceWaveBegan, String waveEndsIn, String spawnPhaseStatus, String startScoutingIn) {
        // Update labels with the data from the server
        if (timeSinceWaveBegan != null && !timeSinceWaveBegan.equals("Unknown")) {
            waveBeganLabel.setText("Wave began: " + timeSinceWaveBegan + " min ago");
        } else {
            waveBeganLabel.setText("Wave began: Unknown");
        }

        if (waveEndsIn != null && !waveEndsIn.equals("Unknown")) {
            waveEndsLabel.setText("Wave ends in: " + waveEndsIn + " min");
        } else {
            waveEndsLabel.setText("Wave ends in: Unknown");
        }

        // Handle the scout time label
        if (startScoutingIn != null && !startScoutingIn.equals("Unknown")) {
            if (startScoutingIn.equalsIgnoreCase("Scout now")) {
                scoutTimeLabel.setText("When to scout: Scout now");
                scoutTimeLabel.setForeground(new Color(255, 200, 0)); // Gold/yellow for emphasis
            } else {
                scoutTimeLabel.setText("When to scout: " + startScoutingIn + " min");
                scoutTimeLabel.setForeground(Color.WHITE);
            }
        } else {
            scoutTimeLabel.setText("When to scout: Unknown");
            scoutTimeLabel.setForeground(Color.WHITE);
        }

        // Use special formatting for spawn phase status based on the value
        if (spawnPhaseStatus != null) {
            if ("Fully spawned".equalsIgnoreCase(spawnPhaseStatus)) {
                spawnPhaseEndsLabel.setText("Spawn phase: " + spawnPhaseStatus);
                spawnPhaseEndsLabel.setForeground(new Color(100, 255, 100)); // Light green for fully spawned
            } else if (spawnPhaseStatus.equals("Unknown")) {
                spawnPhaseEndsLabel.setText("Spawn phase ends: Unknown");
                spawnPhaseEndsLabel.setForeground(Color.WHITE);
            } else {
                spawnPhaseEndsLabel.setText("Spawn phase ends in: " + spawnPhaseStatus + " min");
                spawnPhaseEndsLabel.setForeground(Color.WHITE);
            }
        } else {
            spawnPhaseEndsLabel.setText("Spawn phase ends: Unknown");
            spawnPhaseEndsLabel.setForeground(Color.WHITE);
        }

        revalidate();
        repaint();
    }

    /**
     * Updates the panel with current star data, organizing into sections
     */
    public void updatePanel() {
        // Update rank info first
        updateRankInfo();

        // Clear the container
        starsContainer.removeAll();

        // Get all stars and split into primary and backup lists
        List<Star> primaryStars = new ArrayList<>();
        List<Star> backupStars = new ArrayList<>();

        for (Star star : plugin.getRemoteStars()) {
            if (star.isBackup()) {
                backupStars.add(star);
            } else {
                primaryStars.add(star);
            }
        }

        // Sort the primary stars list (higher tier first, then by miners)
        primaryStars.sort((a, b) -> {
            int tierA = a.getTier() != -1 ? a.getTier() : a.getRemoteTier();
            int tierB = b.getTier() != -1 ? b.getTier() : b.getRemoteTier();
            if (tierA != tierB) {
                return tierB - tierA;
            }

            // If tiers are the same, sort by miners
            try {
                int minersA = Integer.parseInt(a.getMiners());
                int minersB = Integer.parseInt(b.getMiners());
                return minersB - minersA;
            } catch (NumberFormatException e) {
                return 0;
            }
        });

        // Sort the backup stars list
        backupStars.sort((a, b) -> {
            int tierA = a.getTier() != -1 ? a.getTier() : a.getRemoteTier();
            int tierB = b.getTier() != -1 ? b.getTier() : b.getRemoteTier();
            if (tierA != tierB) {
                return tierB - tierA;
            }

            // If tiers are the same, sort by miners
            try {
                int minersA = Integer.parseInt(a.getMiners());
                int minersB = Integer.parseInt(b.getMiners());
                return minersB - minersA;
            } catch (NumberFormatException e) {
                return 0;
            }
        });

        // If no stars at all, show the empty message
        if (primaryStars.isEmpty() && (backupStars.isEmpty() || !plugin.canSeeBackupStars())) {
            starsContainer.add(noStarsPanel);
        } else {
            // Add primary stars section header if there are any primary stars
            if (!primaryStars.isEmpty()) {
                JPanel primaryHeader = createSectionHeader("Primary Stars", new Color(220, 220, 255));
                starsContainer.add(primaryHeader);
                starsContainer.add(Box.createRigidArea(new Dimension(0, 5)));

                // Add all primary stars
                for (Star star : primaryStars) {
                    starsContainer.add(createStarPanel(star));
                    starsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
                }
            }

            // Add backup stars if player has sufficient rank
            if (!backupStars.isEmpty() && plugin.canSeeBackupStars()) {
                // Add some extra space between sections
                starsContainer.add(Box.createRigidArea(new Dimension(0, 10)));

                // Add backup stars section header
                JPanel backupHeader = createSectionHeader("Backup Stars", new Color(255, 220, 180));
                starsContainer.add(backupHeader);
                starsContainer.add(Box.createRigidArea(new Dimension(0, 5)));

                // Add all backup stars
                for (Star star : backupStars) {
                    starsContainer.add(createStarPanel(star));
                    starsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
                }
            }
        }

        // Also update the spawn times panel if it's being shown
        if (showingSpawnTimes) {
            updateSpawnTimesPanel();
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
     * Creates a panel for an individual star with enhanced information
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

        // World information with spawn time
        String worldText = "World: " + star.getWorld();

        // Add spawn time info if available
        String worldStr = String.valueOf(star.getWorld());
        if (plugin.getWorldSpawnTimes().containsKey(worldStr)) {
            String spawnTime = plugin.getWorldSpawnTimes().get(worldStr);
            if (!spawnTime.isEmpty()) {
                worldText += " (Avg spawn: " + spawnTime + " min)";
            }
        }

        JLabel worldLabel = new JLabel(worldText);
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

    /**
     * Creates a section header panel
     */
    private JPanel createSectionHeader(String text, Color color) {
        JPanel header = new JPanel();
        header.setLayout(new BorderLayout());
        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.LIGHT_GRAY_COLOR),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));

        JLabel label = new JLabel(text);
        label.setFont(FontManager.getRunescapeBoldFont());
        label.setForeground(color);
        header.add(label, BorderLayout.CENTER);

        return header;
    }

    /**
     * Updates rank information display in the panel
     */
    public void updateRankInfo() {
        // Check if we need to add rank requirement info
        boolean canSeeBackupStars = plugin.canSeeBackupStars();

        // If this component doesn't exist yet, create it
        if (rankInfoPanel == null) {
            rankInfoPanel = new JPanel();
            rankInfoPanel.setLayout(new BorderLayout());
            rankInfoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            rankInfoPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

            // Create label for rank information
            rankInfoLabel = new JLabel();
            rankInfoLabel.setForeground(Color.LIGHT_GRAY);
            rankInfoLabel.setFont(FontManager.getRunescapeSmallFont());
            rankInfoPanel.add(rankInfoLabel, BorderLayout.CENTER);
        }

        // Update rank info text
//        if (!canSeeBackupStars) {
//            rankInfoLabel.setText("Need " + plugin.BACKUP_STAR_RANK_REQUIREMENT.toString() + "+ rank to see backup stars");
//            rankInfoLabel.setForeground(new Color(255, 190, 100)); // Orange-yellow
//        } else {
//            rankInfoLabel.setText("Showing all stars including backups");
//            rankInfoLabel.setForeground(new Color(150, 250, 150)); // Light green
//        }

        // Add panel if it's not already there
        if (!rankInfoAdded) {
            add(rankInfoPanel, BorderLayout.SOUTH);
            rankInfoAdded = true;
            revalidate();
        }
    }

    /**
     * Initialize the spawn times panel
     */
    private void initializeSpawnTimesPanel() {
        // Create the spawn times panel
        spawnTimesPanel = new JPanel(new BorderLayout());
        spawnTimesPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // No need to create button here since it's now in the constructor
    }

    /**
     * Toggle between stars view and spawn times view
     */
    private void toggleView() {
        if (showingSpawnTimes) {
            // Switch to stars view
            remove(spawnTimesPanel);
            add(starsContainer, BorderLayout.CENTER);
            toggleViewButton.setText("View Spawn Times");
            showingSpawnTimes = false;
        } else {
            // Switch to spawn times view
            remove(starsContainer);
            add(spawnTimesPanel, BorderLayout.CENTER);
            toggleViewButton.setText("View Stars");
            showingSpawnTimes = true;
            updateSpawnTimesPanel();
        }

        revalidate();
        repaint();
    }

    /**
     * Update the spawn times panel with the latest data
     */
    private void updateSpawnTimesPanel() {
        spawnTimesPanel.removeAll();

        // Create header for the panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        headerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel titleLabel = new JLabel("World Spawn Times");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        headerPanel.add(titleLabel, BorderLayout.WEST);

        spawnTimesPanel.add(headerPanel, BorderLayout.NORTH);

        // Create the table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Create the table headers
        JPanel tableHeader = new JPanel(new GridLayout(1, 2));
        tableHeader.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        tableHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.LIGHT_GRAY_COLOR));

        JLabel worldHeader = new JLabel("World");
        worldHeader.setForeground(Color.WHITE);
        worldHeader.setFont(FontManager.getRunescapeBoldFont());
        worldHeader.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel spawnHeader = new JLabel("Avg Spawn (min)");
        spawnHeader.setForeground(Color.WHITE);
        spawnHeader.setFont(FontManager.getRunescapeBoldFont());
        spawnHeader.setBorder(new EmptyBorder(5, 5, 5, 5));

        tableHeader.add(worldHeader);
        tableHeader.add(spawnHeader);

        tablePanel.add(tableHeader, BorderLayout.NORTH);

        // Create the table content
        JPanel tableContent = new JPanel();
        tableContent.setLayout(new BoxLayout(tableContent, BoxLayout.Y_AXIS));
        tableContent.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Get spawn times data
        Map<String, String> spawnTimes = plugin.getWorldSpawnTimes();

        if (spawnTimes.isEmpty()) {
            JLabel emptyLabel = new JLabel("No spawn time data available");
            emptyLabel.setForeground(Color.LIGHT_GRAY);
            emptyLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
            tableContent.add(emptyLabel);
        } else {
            // Sort worlds by number
            List<String> worlds = new ArrayList<>(spawnTimes.keySet());
            worlds.sort((a, b) -> {
                try {
                    return Integer.parseInt(a) - Integer.parseInt(b);
                } catch (NumberFormatException e) {
                    return a.compareTo(b);
                }
            });

            // Create a row for each world with spawn data
            for (String world : worlds) {
                // Skip worlds with empty spawn times
                String spawnTime = spawnTimes.get(world);

                // Skip empty world entries (but keep worlds with empty spawn times)
                if (world == null || world.isEmpty()) {
                    continue;
                }

                JPanel rowPanel = new JPanel(new GridLayout(1, 2));
                rowPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                rowPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR));

                JLabel worldLabel = new JLabel(world);
                worldLabel.setForeground(Color.WHITE);
                worldLabel.setBorder(new EmptyBorder(3, 5, 3, 5));

                JLabel spawnLabel = new JLabel(spawnTime != null && !spawnTime.isEmpty() ? spawnTime : "-");
                spawnLabel.setForeground(Color.WHITE);
                spawnLabel.setBorder(new EmptyBorder(3, 5, 3, 5));

                rowPanel.add(worldLabel);
                rowPanel.add(spawnLabel);

                tableContent.add(rowPanel);
            }
        }

        // Add the table content to a scroll pane
        JScrollPane scrollPane = new JScrollPane(tableContent);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setBorder(null);

        tablePanel.add(scrollPane, BorderLayout.CENTER);
        spawnTimesPanel.add(tablePanel, BorderLayout.CENTER);
    }

    /**
     * Helper method to extract minutes from a time string like "5:30"
     */
    private int getMinutesFromTimeString(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return -1;
        }

        try {
            String[] parts = timeStr.split(":");
            if (parts.length == 2) {
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                return minutes * 60 + seconds;
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }

        return -1;
    }
}