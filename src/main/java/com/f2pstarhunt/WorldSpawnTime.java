package com.f2pstarhunt;

import lombok.Getter;

public class WorldSpawnTime {
    @Getter
    private final String world;
    @Getter private final String avgSpawn;

    public WorldSpawnTime(String world, String avgSpawn) {
        this.world = world;
        this.avgSpawn = avgSpawn;
    }
}
