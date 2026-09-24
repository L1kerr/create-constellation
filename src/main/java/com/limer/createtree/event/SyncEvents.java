package com.limer.createtree.event;

import com.limer.createtree.data.SkillApi;
import com.limer.createtree.net.ModNetwork;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;

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

	@SubscribeEvent
	public static void onRegisterCommands(final net.neoforged.neoforge.event.RegisterCommandsEvent event) {
		com.limer.createtree.command.SkillTreeCommands.register(event.getDispatcher());
	}
}
