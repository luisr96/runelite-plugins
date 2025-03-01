package com.f2pstarhunt;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class F2PStarHuntPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(F2PStarHuntPlugin.class);
		RuneLite.main(args);
	}
}