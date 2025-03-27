package com.f2pstarhunt;

public class DashboardData {
    private String waveEndsIn = "Unknown";  // C2 in the sheet
    private String timeSinceWaveBegan = "Unknown";  // C3 in the sheet
    private String startScoutingIn = "Unknown";  // C5 in the sheet
    private String spawnPhaseStatus = "Unknown";  // C6 in the sheet

    public DashboardData() {
    }

    public String getWaveEndsIn() {
        return waveEndsIn;
    }

    public void setWaveEndsIn(String waveEndsIn) {
        this.waveEndsIn = waveEndsIn;
    }

    public String getTimeSinceWaveBegan() {
        return timeSinceWaveBegan;
    }

    public void setTimeSinceWaveBegan(String timeSinceWaveBegan) {
        this.timeSinceWaveBegan = timeSinceWaveBegan;
    }

    public String getStartScoutingIn() {
        return startScoutingIn;
    }

    public void setStartScoutingIn(String startScoutingIn) {
        this.startScoutingIn = startScoutingIn;
    }

    public String getSpawnPhaseStatus() {
        return spawnPhaseStatus;
    }

    public void setSpawnPhaseStatus(String spawnPhaseStatus) {
        this.spawnPhaseStatus = spawnPhaseStatus;
    }
}