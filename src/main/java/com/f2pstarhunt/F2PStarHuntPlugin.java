/*
 * Copyright (c) 2022, Cute Rock
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.f2pstarhunt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import static net.runelite.api.ItemID.GOLDEN_PROSPECTOR_BOOTS;
import static net.runelite.api.ItemID.GOLDEN_PROSPECTOR_HELMET;
import static net.runelite.api.ItemID.GOLDEN_PROSPECTOR_JACKET;
import static net.runelite.api.ItemID.GOLDEN_PROSPECTOR_LEGS;
import static net.runelite.api.ItemID.PROSPECTOR_BOOTS;
import static net.runelite.api.ItemID.PROSPECTOR_HELMET;
import static net.runelite.api.ItemID.PROSPECTOR_JACKET;
import static net.runelite.api.ItemID.PROSPECTOR_LEGS;
import static net.runelite.api.ItemID.VARROCK_ARMOUR_4;

import net.runelite.api.coords.Angle;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
		name = "Star Hunt Panel",
		description = "Displays shooting star information in a panel"
)
@Slf4j
public class F2PStarHuntPlugin extends Plugin
{
	public static final FriendsChatRankRequirement BACKUP_STAR_RANK_REQUIREMENT = FriendsChatRankRequirement.RECRUIT;
	private static final int NPC_ID = NullNpcID.NULL_10629;
	private static final int MAX_PLAYER_LOAD_DIST = 13;
	private static final Queue<Star> despawnQueue = new LinkedList<>();

	@Getter
	private final Map<String, String> worldSpawnTimes = new HashMap<>();

	private static final Set<Integer> dragonPickSpecAnims = ImmutableSet.of(
			7138, // Dragon pickaxe
			334, // Dragon pickaxe (upgraded)
			8781, // Dragon pickaxe (or) (Trailblazer) / Infernal pickaxe (or)
			8330, // Dragon pickaxe (or)
			8329, // Crystal pickaxe
			3410 // infernal pickaxe
			// 3rd age pickaxe
	);

	private static final Map<Integer, Double> pickAnims = ImmutableMap.<Integer, Double>builder().
			put(AnimationID.MINING_CRASHEDSTAR_ADAMANT, 3.0).
			put(AnimationID.MINING_TRAILBLAZER_PICKAXE, 17.0 / 6).
			put(AnimationID.MINING_CRASHEDSTAR_BLACK, 5.0).
			put(AnimationID.MINING_CRASHEDSTAR_BRONZE, 8.0).
			put(AnimationID.MINING_CRASHEDSTAR_GILDED, 3.0).
			put(AnimationID.MINING_CRASHEDSTAR_CRYSTAL, 2.75).
			put(AnimationID.MINING_CRASHEDSTAR_3A, 17.0 / 6).
			put(AnimationID.MINING_CRASHEDSTAR_DRAGON, 17.0 / 6).
			put(AnimationID.MINING_CRASHEDSTAR_DRAGON_OR, 17.0 / 6).
			put(AnimationID.MINING_CRASHEDSTAR_DRAGON_OR_TRAILBLAZER, 17.0 / 6).
			put(AnimationID.MINING_CRASHEDSTAR_DRAGON_UPGRADED, 17.0 / 6).
			put(AnimationID.MINING_CRASHEDSTAR_INFERNAL, 17.0 / 6).
			put(AnimationID.MINING_CRASHEDSTAR_MITHRIL, 5.0).
			put(AnimationID.MINING_CRASHEDSTAR_IRON, 7.0).
			put(AnimationID.MINING_CRASHEDSTAR_RUNE, 3.0).
			put(AnimationID.MINING_CRASHEDSTAR_STEEL, 6.0).
			put(AnimationID.MINING_TRAILBLAZER_PICKAXE_3, 17.0 / 6).
			put(7138, 17.0 / 6).
			put(334, 17.0 / 6).
			put(8781, 17.0 / 6).
			put(8330, 17.0 / 6).
			put(3410, 17.0 / 6).
			put(8329, 2.75).
			build();

	private static final int MINING_CACHE_TIME = 13; // count player as a miner if they have done mining anim within this many ticks ago
	private static final Map<String, Integer> playerLastMined = new HashMap<>();

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;

	public final List<Star> stars = new ArrayList<>();

	@Getter
	private double xpPerHour = -1;

	@Getter
	private double dustPerHour = -1;

	public int layerTimer = 0;

	public int bonusCount;

	private StarWebSocketClient webSocketClient;

	@Inject
	private ItemManager itemManager;

	@Inject
	Client client;

	@Getter
	@Inject
	ClientThread clientThread;

	@Inject
	private F2PStarHuntConfig config;

	@Inject
	private WorldInfo worldInfo;

	@Inject
	private Hooks hooks;

	// Panel fields
	@Inject
	private ClientToolbar clientToolbar;

	private StarHuntPanel panel;

	private NavigationButton navButton;

	@Getter
	private final List<Star> remoteStars = new ArrayList<>();

	@Provides
	F2PStarHuntConfig
	provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(F2PStarHuntConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		hooks.registerRenderableDrawListener(drawListener);

		// Initialize and add panel
		panel = injector.getInstance(StarHuntPanel.class);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/shooting_star_icon.png");
		navButton = NavigationButton.builder()
				.tooltip("Star Hunt Panel")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();
		clientToolbar.addNavigation(navButton);
		updateConnectionStatus();
	}

	@Override
	protected void shutDown() throws Exception
	{
		clear();
		hooks.unregisterRenderableDrawListener(drawListener);

		// Remove panel
		clientToolbar.removeNavigation(navButton);

		// Close WebSocket connection
		if (webSocketClient != null) {
			webSocketClient.close();
			webSocketClient = null;
		}
	}

	private void clear()
	{
		playerLastMined.clear();
		stars.clear();
		layerTimer = 0;
	}

	private boolean shouldDraw(Renderable renderable, boolean b)
	{
		if (!(renderable instanceof NPC))
		{
			return true;
		}

		NPC npc = (NPC) renderable;
		return npc.getId() != NPC_ID;
	}

	public boolean isWebSocketConnected() {
		return webSocketClient != null && webSocketClient.isOpen();
	}

	public void updateConnectionStatus() {
		if (panel != null) {
			boolean connected = isWebSocketConnected();
			panel.updateConnectionStatus(connected);
		}
	}

	public void connectToWebSocket() {
		// Close any existing connection
		if (webSocketClient != null) {
			try {
				webSocketClient.close();
			} catch (Exception e) {
				log.error("Error closing existing WebSocket connection", e);
			}
		}

		String websocketUrl = config.websocketUrl();

		if (websocketUrl == null || websocketUrl.isEmpty()) {
			log.warn("Cannot connect: WebSocket URL is not configured");
			return;
		}

		try {
			webSocketClient = new StarWebSocketClient(websocketUrl, this);
			webSocketClient.connect();
			log.info("Connecting to WebSocket server: {}", websocketUrl);
		} catch (Exception e) {
			log.error("Failed to connect to WebSocket server", e);
		} finally {
			updateConnectionStatus();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (event.getNpc().getId() != NPC_ID)
		{
			return;
		}
		for (Star star : stars)
		{
			if (star.getWorldPoint().equals(event.getNpc().getWorldLocation()))
			{
				star.setNpc(event.getNpc());
				updatePanel();
				return;
			}
		}
		Star star = new Star(event.getNpc(), client.getWorld());
		worldInfo.update(star);
		stars.add(0, star);
		updatePanel();
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc().getId() != NPC_ID)
		{
			return;
		}
		for (Star star : stars)
		{
			if (star.getWorldPoint().equals(event.getNpc().getWorldLocation()))
			{
				star.setNpc(null);
				updatePanel();
				return;
			}
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		int tier = Star.getTier(event.getGameObject().getId());
		if (tier < 0)
		{
			return;
		}

		boolean newStar = false;
		Star star = null;
		for (Star s : stars)
		{
			if (s.getWorldPoint().equals(event.getGameObject().getWorldLocation()))
			{
				s.setObject(event.getGameObject());
				s.resetHealth();
				star = s;
				layerTimer = 0;
				despawnQueue.remove(star);
				break;
			}
		}
		if (star == null)
		{
			star = new Star(event.getGameObject(), client.getWorld());
			worldInfo.update(star);
			stars.add(0, star);
			newStar = true;
		}

		if (newStar)
		{
			client.addChatMessage(ChatMessageType.CONSOLE, "", star.getMessage(), "");
		}

		if (isWebSocketConnected()) {
			try {
				webSocketClient.sendStarData(star);
			} catch (Exception e) {
				log.error("Error sending star data to WebSocket server", e);
			}
		}

		updatePanel();
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		int tier = Star.getTier(event.getGameObject().getId());
		if (tier < 0)
		{
			return;
		}

		Star starToRemove = null;
		for (Star star : stars)
		{
			if (event.getGameObject().equals(event.getGameObject()) || event.getGameObject().getWorldLocation().equals(star.getWorldPoint()))
			{
				starToRemove = star;
				break;
			}
		}

		if (starToRemove != null) {
			// Send removal message to ws server when star is gone
			if (tier == 1) {
				if (isWebSocketConnected()) {
					webSocketClient.sendStarRemoval(starToRemove);
				}
				stars.remove(starToRemove);
				layerTimer = 0;
			} else {
				// For other tiers, add to despawn queue for further processing
				despawnQueue.add(starToRemove);
			}
		}
	}

	void updateMiners(Star star)
	{
		int distToStar = client.getLocalPlayer().getWorldLocation().distanceTo(new WorldArea(star.getWorldPoint(), 2, 2));
		if (distToStar > MAX_PLAYER_LOAD_DIST)
		{
			star.setMiners(Star.UNKNOWN_MINERS);
			return;
		}
		WorldArea areaH = new WorldArea(star.getWorldPoint().dx(-1), 4, 2);
		WorldArea areaV = new WorldArea(star.getWorldPoint().dy(-1), 2, 4);
		int count = 0;
		int tickCount = client.getTickCount();
		for (Player p : client.getPlayers())
		{
			if (!p.getWorldLocation().isInArea2D(areaH, areaV)) // Skip players not next to the star
			{
				continue;
			}
			if (!facingObject(p.getWorldLocation(), p.getOrientation(), star.getWorldPoint()))
			{
				continue;
			}
			if (pickAnims.containsKey(p.getAnimation())) // count anyone that is doing mining animation
			{
				count++;
				playerLastMined.put(p.getName(), tickCount);
				continue;
			}
			if (p.getHealthRatio() < 0 || !playerLastMined.containsKey(p.getName()))
			{
				continue;
			}
			int ticksSinceMinedLast = tickCount - playerLastMined.get(p.getName());
			if (ticksSinceMinedLast < MINING_CACHE_TIME)
			{
				count++;
			}
		}
		if (config.estimateDeathTime() != EstimateConfig.NONE || config.estimateLayerTime() != EstimateConfig.NONE)
		{
			refreshEstimate(star);
		}
		layerTimer += 1;
		star.setMiners(Integer.toString(count));

		if (isWebSocketConnected()) {
			webSocketClient.sendStarData(star);
		}
	}

	public void refreshEstimate(Star star)
	{
		int[] ticks = getTicksEstimates(star);
		star.setTierTicksEstimate(ticks);
		updatePanel();
	}

	private int[] getTicksEstimates(Star star)
	{
		if (star.getHealth() < 0) {
			return null;
		}
		int startTier = star.getTier();
		if (startTier < 0) {
			return null;
		}
		int tier = startTier;
		int[] ticks = new int[startTier];
		int totalTicks = 0;
		while (tier > 0) {
			TierData tierData = TierData.get(tier);
			if (tierData == null) {
				return null;
			}
			double healthScale = tier == startTier ? (star.getHealth() / 100.0) : 1;

			totalTicks += (int) Math.ceil(healthScale * tierData.tickTime);
			tier--;
			ticks[tier] = totalTicks;
		}
		return ticks;
	}

	private boolean facingObject(WorldPoint p1, int orientation, WorldPoint p2)
	{
		Direction dir = new Angle(orientation).getNearestDirection();
		WorldPoint dif = p2.dx(-p1.getX()).dy(-p1.getY());
		switch (dir)
		{
			case NORTH:
				return dif.getY() > 0;
			case SOUTH:
				return dif.getY() < 0;
			case EAST:
				return dif.getX() > 0;
			case WEST:
				return dif.getX() < 0;
		}
		return false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged state)
	{
		if (state.getGameState() == GameState.HOPPING || state.getGameState() == GameState.LOGGING_IN)
		{
			clear();
			updatePanel();
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (stars.isEmpty())
		{
			return;
		}
		Iterator<Star> it = stars.iterator();
		while (it.hasNext())
		{
			Star star = it.next();
			if (despawnQueue.contains(star))
			{
				layerTimer = 0;
				it.remove();
				despawnQueue.remove(star);
			}
		}

		if (!stars.isEmpty())
		{
			Star star = stars.get(0);
			updateMiners(star);
		}
		updatePanel();
		updateConnectionStatus();
	}

	private boolean nextToStar(Star star, WorldPoint worldPoint) {
		WorldArea areaH = new WorldArea(star.getWorldPoint().dx(-1), 4, 2);
		WorldArea areaV = new WorldArea(star.getWorldPoint().dy(-1), 2, 4);
		return worldPoint.isInArea2D(areaH, areaV);
	}

	private void updatePanel() {
		if (panel != null) {
			panel.updatePanel();
		}
	}

	public void updateRemoteStars(List<Star> newRemoteStars) {
		remoteStars.clear();
		remoteStars.addAll(newRemoteStars);
		updatePanel();
	}

	public boolean canSeeBackupStars() {
		FriendsChatRank playerRank = null;

		// Get the player's current friends chat rank
		if (client.getFriendsChatManager() != null) {
			playerRank = client.getFriendsChatManager().findByName(client.getLocalPlayer().getName()).getRank();
		}

		// Check if the player meets the rank requirement
		return BACKUP_STAR_RANK_REQUIREMENT.meetsRequirement(playerRank);
	}

	public void updateWorldSpawnTimes(List<WorldSpawnTime> spawnTimes) {
		worldSpawnTimes.clear();
		for (WorldSpawnTime spawnTime : spawnTimes) {
			if (spawnTime.getWorld() != null && !spawnTime.getWorld().isEmpty()) {
				worldSpawnTimes.put(spawnTime.getWorld(), spawnTime.getAvgSpawn());
			}
		}
		updatePanel();
	}
}