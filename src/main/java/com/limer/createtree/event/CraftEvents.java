package com.limer.createtree.event;

import com.limer.createtree.data.SkillApi;

import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent;

/**
 * Grants EXP for vanilla crafting of gated items.
 * (Create machine crafting is credited through dedicated mixins in the Create package.)
 */
public class CraftEvents {

	@SubscribeEvent
	public static void onItemCrafted(final ItemCraftedEvent event) {
		Player player = event.getEntity();
		if (player == null || player.level().isClientSide)
			return;
		SkillApi.awardExpFor(player, event.getCrafting());
	}
}
