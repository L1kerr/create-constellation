package com.limer.createtree.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ServerTickEvents {

	@SubscribeEvent
	public static void onServerTick(final ServerTickEvent.Post event) {
		var server = event.getServer();
		if (server.getTickCount() % 20 == 0)
			WorldEvents.serverTick(server);
	}
}
