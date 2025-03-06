package com.f2pstarhunt;

import net.runelite.api.coords.WorldPoint;

public class StarDTO {
    public int world;
    public int tier;
    public int health;
    public String miners;
    public String location;
    public boolean backup;
    public String lastUpdate;
    public String layerTime;
    public String depleteTime;
    public String firstFound;
    public WorldPointDTO worldPoint;

    public static class WorldPointDTO {
        public int x;
        public int y;
        public int plane;
    }

    public Star toStar() {
        Star star = new Star();
        star.setWorld(world);
        star.setRemoteTier(tier);
        star.setHealth(health);
        star.setMiners(miners);
        star.setLocation(location); // This calls your setLocation(String) method
        star.setBackup(backup);
        star.setLastUpdate(lastUpdate);
        star.setLayerTime(layerTime);
        star.setDepleteTime(depleteTime);
        star.setFirstFound(firstFound);

        if (worldPoint != null) {
            star.setWorldPoint(new WorldPoint(worldPoint.x, worldPoint.y, worldPoint.plane));
        }

        return star;
    }
}