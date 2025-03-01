package com.f2pstarhunt;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Counter;
import javax.annotation.Nonnull;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class BonusCounter extends Counter
{
	private final F2PStarHuntPlugin plugin;

	public BonusCounter(BufferedImage image, @Nonnull Plugin plugin, int bonus)
	{
		super(image, plugin, bonus);
		this.plugin = (F2PStarHuntPlugin) plugin;
	}

	@Override
	public String getText()
	{
		return String.valueOf(plugin.bonusCount);
	}

	@Override
	public Color getTextColor()
	{
		return Color.WHITE;
	}
}
