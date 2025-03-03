package com.f2pstarhunt;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("starinfoplugin")
public interface F2PStarHuntConfig extends Config
{
	@ConfigItem(
			position = 1,
			keyName = "estimateLayerTime",
			name = "Estimate Layer Time",
			description = "Display estimated time till the current layer finishes"
	)
	default EstimateConfig estimateLayerTime()
	{
		return EstimateConfig.SECONDS;
	}

	@ConfigItem(
			position = 2,
			keyName = "estimateDepletion_time",
			name = "Estimate Depletion Time",
			description = "Display estimated time till the star depletes"
	)
	default EstimateConfig estimateDeathTime()
	{
		return EstimateConfig.SECONDS;
	}

	@ConfigItem(
			position = 10,
			keyName = "websocketUrl",
			name = "WebSocket Server URL",
			description = "URL of the WebSocket server to connect to (e.g., ws://localhost:8080)"
	)
	default String websocketUrl()
	{
		return "ws://localhost:8080";
	}
}