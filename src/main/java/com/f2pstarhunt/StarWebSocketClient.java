package com.f2pstarhunt;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StarWebSocketClient extends WebSocketClient {
    private final F2PStarHuntPlugin plugin;
    private final Gson gson = new Gson();

    public static final String MSG_TYPE_STAR_UPDATE = "STAR_UPDATE";
    public static final String MSG_TYPE_STAR_SYNC = "STAR_SYNC";
    public static final String MSG_TYPE_STAR_REMOVE = "STAR_REMOVE";
    public static final String MSG_TYPE_SPAWN_TIMES = "SPAWN_TIMES";

    public StarWebSocketClient(String serverUrl, F2PStarHuntPlugin plugin) throws URISyntaxException {
        super(new URI(serverUrl));
        this.plugin = plugin;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("Connected to star hunt WebSocket server");
        plugin.getClientThread().invokeLater(plugin::updateConnectionStatus);
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonObject json = new JsonParser().parse(message).getAsJsonObject();
            String type = json.get("type").getAsString();
            log.debug("Received message type: {}", type);

            plugin.getClientThread().invokeLater(() -> {
                try {
                    switch (type) {
                        case MSG_TYPE_STAR_UPDATE:
                            log.debug("Sent out star update");
                            break;
                        case MSG_TYPE_STAR_SYNC:
                            log.debug("Received star sync");
                            handleStarSync(json);
                            break;
                        case MSG_TYPE_SPAWN_TIMES:  // Handle spawn times
                            log.debug("Received spawn times update");
                            System.out.println("Spawn times");
                            System.out.println(json);
                            handleSpawnTimes(json);
                            break;
                        default:
                            log.debug("Unknown message type: {}", type);
                            break;
                    }
                } catch (Exception e) {
                    log.error("Error processing message of type {}", type, e);
                }
            });
        } catch (Exception e) {
            log.error("Error parsing WebSocket message", e);
        }
    }

    private void handleSpawnTimes(JsonObject json) {
        if (!json.has("data") || !json.get("data").isJsonArray()) {
            log.warn("Received malformed spawn times data");
            return;
        }

        try {
            JsonArray spawnTimesArray = json.getAsJsonArray("data");
            List<WorldSpawnTime> spawnTimes = new ArrayList<>();

            for (JsonElement element : spawnTimesArray) {
                if (element.isJsonObject()) {
                    JsonObject worldData = element.getAsJsonObject();
                    String world = worldData.has("world") ? worldData.get("world").getAsString() : "";
                    String avgSpawn = worldData.has("avgSpawn") ? worldData.get("avgSpawn").getAsString() : "";

                    spawnTimes.add(new WorldSpawnTime(world, avgSpawn));
                }
            }

            // Update the plugin with the spawn times
            plugin.updateWorldSpawnTimes(spawnTimes);
        } catch (Exception e) {
            log.error("Error processing spawn times data", e);
        }
    }

    private void handleStarSync(JsonObject json) {
        if (!json.has("data") || !json.get("data").isJsonArray()) {
            log.warn("Received malformed star data");
            return;
        }

        try {
            JsonArray starsArray = json.getAsJsonArray("data");

            List<StarDTO> starDTOs = gson.fromJson(starsArray, new TypeToken<List<StarDTO>>(){}.getType());

            // Convert DTOs to stars
            List<Star> remoteStars = new ArrayList<>();
            for (StarDTO dto : starDTOs) {
                remoteStars.add(dto.toStar());
            }

            plugin.updateRemoteStars(remoteStars);
        } catch (Exception e) {
            log.error("Error processing star sync", e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("WebSocket connection closed: " + reason + " (remote: " + remote + ", code: " + code + ")");
        plugin.getClientThread().invokeLater(plugin::updateConnectionStatus);
    }

    @Override
    public void onError(Exception ex) {
        log.error("WebSocket error", ex);
    }

    public void sendStarData(Star star) {
        if (!isOpen()) {
            log.warn("Cannot send star data: WebSocket is not connected");
            return;
        }

        try {
            JsonObject starData = new JsonObject();

            starData.addProperty("world", star.getWorld());
            starData.addProperty("tier", star.getTier());
            starData.addProperty("health", star.getHealth());
            starData.addProperty("miners", star.getMiners());
            starData.addProperty("location", star.getLocation().getDescription());
            starData.addProperty("backup", Integer.parseInt(star.getMiners()) == 0);
            starData.addProperty("lastUpdate", Instant.now().toString());
            starData.addProperty("layerTime", star.getFormattedTimeUntilLayerDone(EstimateConfig.SECONDS));
            starData.addProperty("depleteTime", star.getFormattedTimeUntilDepleted(EstimateConfig.SECONDS));
            starData.addProperty("firstFound", star.getFirstFound());

            JsonObject worldPoint = new JsonObject();
            worldPoint.addProperty("x", star.getWorldPoint().getX());
            worldPoint.addProperty("y", star.getWorldPoint().getY());
            worldPoint.addProperty("plane", star.getWorldPoint().getPlane());
            starData.add("worldPoint", worldPoint);

            JsonObject message = new JsonObject();
            message.addProperty("type", "STAR_UPDATE");
            message.add("data", starData);

            send(gson.toJson(message));
            // log.info("Sent star to server: World " + star.getWorld() + ", Tier " + star.getTier() + ", Backup: " + star.isBackup() + ", Time remaining: " + star.getFormattedTimeUntilLayerDone(EstimateConfig.SECONDS) + " / " + star.getFormattedTimeUntilDepleted(EstimateConfig.SECONDS));
        } catch (Exception e) {
            log.error("Error sending star data", e);
        }
    }

    public void sendStarRemoval(Star star) {
        if (!isOpen()) {
            log.warn("Cannot send star removal: WebSocket is not connected");
            return;
        }

        try {
            JsonObject message = new JsonObject();
            message.addProperty("type", MSG_TYPE_STAR_REMOVE);

            JsonObject data = new JsonObject();
            data.addProperty("world", star.getWorld());
            message.add("data", data);

            send(gson.toJson(message));
            log.debug("Sent star removal to server: World " + star.getWorld());
        } catch (Exception e) {
            log.error("Error sending star removal", e);
        }
    }

    /**
     * Sends a raw message to the WebSocket server
     *
     * @param message The JSON message as a string
     */
    public void send(String message) {
        if (!isOpen()) {
            log.warn("Cannot send message: WebSocket is not connected");
            return;
        }

        try {
            super.send(message);
            log.debug("Message sent: {}", message);
        } catch (Exception e) {
            log.error("Error sending message to WebSocket server", e);
        }
    }
}