package com.f2pstarhunt;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;

import java.time.Instant;
public class Star
{
	public static final String UNKNOWN_MINERS = "0";

	private static final int[] TIER_IDS = new int[]{
		ObjectID.CRASHED_STAR_41229,
		ObjectID.CRASHED_STAR_41228,
		ObjectID.CRASHED_STAR_41227,
		ObjectID.CRASHED_STAR_41226,
		ObjectID.CRASHED_STAR_41225,
		ObjectID.CRASHED_STAR_41224,
		ObjectID.CRASHED_STAR_41223,
		ObjectID.CRASHED_STAR_41021,
		ObjectID.CRASHED_STAR,
	};

	@Getter @Setter
	private WorldPoint worldPoint;
	@Getter @Setter @SerializedName("world") private int world;
	@Getter @Setter @SerializedName("tier") private int remoteTier;
	@Getter @Setter @SerializedName("health") private int health;
	@Getter @Setter @SerializedName("miners") private String miners;
	@Getter @SerializedName("location") Location location;
	@Getter @Setter @SerializedName("backup") private boolean isBackup = true;
	@Getter @Setter @SerializedName("lastUpdate") private String lastUpdate;
	@Getter @Setter @SerializedName("layerTime") private String layerTime;
	@Getter @Setter @SerializedName("depleteTime") private String depleteTime;
	@Getter @Setter @SerializedName("firstFound") private String firstFound = Instant.now().toString();

	@Getter
	@Setter
	private NPC npc;
	@Getter
	@Setter
	private GameObject object;
	@Getter
	@Setter
	private int[] tierTicksEstimate;
	@Getter
	@Setter
	private String worldInfo = "";

	public void setLocation(String locationStr) {
		this.location = Location.fromDescription(locationStr);
	}


	public Star(NPC npc, int world)
	{
		this.npc = npc;
		this.worldPoint = npc.getWorldLocation();
		this.location = Location.forLocation(worldPoint);
		this.world = world;
	}

	public Star(GameObject gameObject, int world)
	{
		this.object = gameObject;
		this.worldPoint = gameObject.getWorldLocation();
		this.location = Location.forLocation(worldPoint);
		this.world = world;
	}

	// For gson serialization
	public Star() {}

	public int getTier()
	{
		if (object == null)
		{
			return -1;
		}
		return getTier(object.getId());
	}

	public static int getTier(int id)
	{
		for (int i = 0; i < TIER_IDS.length; i++)
		{
			if (id == TIER_IDS[i])
			{
				return i + 1;
			}
		}
		return -1;
	}

	/**
	 * @return Health Percentage 0-100 of current layer. -1 if not known.
	 */
	public int getHealth()
	{
		if (npc == null)
		{
			return health;
		}
		if (npc.getHealthRatio() >= 0)
		{
			health = 100 * npc.getHealthRatio() / npc.getHealthScale();
		}
		else if (npc.isDead())
		{
			health = -1;
		}
		return health;
	}

	public String getMessage()
	{
		return "Star Found: W" + world + " T" + getTier() + " " + location.getDescription() + worldInfo;
	}

	public void resetHealth()
	{
		health = 100;
	}

	/**
	 * Gets the formatted time until the current layer of the star is completed.
	 *
	 * @param star The star to calculate the time for
	 * @param format The time format (TICKS or SECONDS)
	 * @return A formatted string representing the time until the current layer is done, or null if unavailable
	 */
	public String getTimeUntilLayerDone(Star star, EstimateConfig format) {
		if (star.getTierTicksEstimate() == null || star.getTier() <= 0 ||
				star.getTier() > star.getTierTicksEstimate().length) {
			return null;
		}

		int layerTicks = star.getTierTicksEstimate()[star.getTier() - 1];

		if (format == EstimateConfig.TICKS) {
			return String.valueOf(layerTicks);
		} else if (format == EstimateConfig.SECONDS) {
			// Convert ticks to minutes and seconds
			int seconds = (layerTicks % 100) * 3 / 5;
			int minutes = layerTicks / 100;
			return minutes + ":" + String.format("%02d", seconds);
		} else {
			return null;
		}
	}

	/**
	 * Gets the time in ticks until the current layer of the star is completed.
	 *
	 * @return Time in ticks until the current layer is done, or -1 if unavailable
	 */
	public int getTimeUntilLayerDone() {
		if (getTierTicksEstimate() == null || getTier() <= 0 ||
				getTier() > getTierTicksEstimate().length) {
			return -1;
		}

		return getTierTicksEstimate()[getTier() - 1];
	}

	/**
	 * Gets the time in ticks until the star is fully depleted.
	 *
	 * @return Time in ticks until the star depletes, or -1 if unavailable
	 */
	public int getTimeUntilDepleted() {
		if (getTierTicksEstimate() == null || getTierTicksEstimate().length == 0) {
			return -1;
		}

		return getTierTicksEstimate()[0];
	}

	/**
	 * Gets the formatted time until the current layer of the star is completed.
	 *
	 * @param format The time format (TICKS or SECONDS)
	 * @return A formatted string representing the time until the current layer is done, or null if unavailable
	 */
	public String getFormattedTimeUntilLayerDone(EstimateConfig format) {
		int layerTicks = getTimeUntilLayerDone();
		if (layerTicks < 0) {
			return null;
		}

		if (format == EstimateConfig.TICKS) {
			return String.valueOf(layerTicks);
		} else if (format == EstimateConfig.SECONDS) {
			// Convert ticks to minutes and seconds
			int seconds = (layerTicks % 100) * 3 / 5;
			int minutes = layerTicks / 100;
			return minutes + ":" + String.format("%02d", seconds);
		} else {
			return null;
		}
	}

	/**
	 * Gets the formatted time until the star is fully depleted.
	 *
	 * @param format The time format (TICKS or SECONDS)
	 * @return A formatted string representing the time until the star depletes, or null if unavailable
	 */
	public String getFormattedTimeUntilDepleted(EstimateConfig format) {
		int depletesTicks = getTimeUntilDepleted();
		if (depletesTicks < 0) {
			return null;
		}

		if (format == EstimateConfig.TICKS) {
			return String.valueOf(depletesTicks);
		} else if (format == EstimateConfig.SECONDS) {
			// Convert ticks to minutes and seconds
			int seconds = (depletesTicks % 100) * 3 / 5;
			int minutes = depletesTicks / 100;
			return minutes + ":" + String.format("%02d", seconds);
		} else {
			return null;
		}
	}
}