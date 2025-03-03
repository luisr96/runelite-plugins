package com.f2pstarhunt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

@Slf4j
public class StarWebSocketClient extends WebSocketClient {
    private final F2PStarHuntPlugin plugin;
    private final Gson gson = new Gson();

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
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
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