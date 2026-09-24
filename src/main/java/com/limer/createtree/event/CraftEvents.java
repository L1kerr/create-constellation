package com.limer.createtree.event;

import com.limer.createtree.data.SkillApi;

import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent;

public class CraftEvents {

	@SubscribeEvent
	public static void onItemCrafted(final ItemCraftedEvent event) {
		Player player = event.getEntity();
		if (player == null || player.level().isClientSide)
			return;
		SkillApi.awardExpFor(player, event.getCrafting());
	}
}
