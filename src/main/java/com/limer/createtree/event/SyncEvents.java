package com.limer.createtree.event;

import com.limer.createtree.data.SkillApi;
import com.limer.createtree.net.ModNetwork;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;

/**
 * Sends the tree definition and the player's progress on login, respawn and datapack reload.
 */
public class SyncEvents {

	@SubscribeEvent
	public static void onLogin(final PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			SkillApi.syncTo(player);
	}

	@SubscribeEvent
	public static void onRespawn(final PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			SkillApi.syncTo(player);
	}

	@SubscribeEvent
	public static void onDatapackSync(final OnDatapackSyncEvent event) {
		event.getRelevantPlayers().forEach(player -> {
			ModNetwork.sendTreeTo(player);
			SkillApi.syncTo(player);
		});
	}
}
