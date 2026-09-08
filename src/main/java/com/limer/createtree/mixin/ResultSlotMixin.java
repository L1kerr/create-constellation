package com.limer.createtree.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.limer.createtree.data.SkillApi;
import com.limer.createtree.net.ClientData;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Blocks vanilla crafting table output pickup when the result item is locked for that player.
 * ResultSlot does not override mayPickup, so we target Slot and filter to ResultSlot instances.
 * mayPickup == false also blocks shift-click quick move, covering all vanilla crafting paths
 * (crafting table, 2x2 inventory grid, recipe book, quick crafting).
 *
 * Client side uses the synced ClientData cache for immediate UI feedback;
 * the server uses authoritative per-player progress.
 */
@Mixin(Slot.class)
public abstract class ResultSlotMixin {

	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void createtree$blockLockedPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof ResultSlot resultSlot))
			return;
		ItemStack result = resultSlot.getItem();
		if (result.isEmpty())
			return;

		boolean locked;
		if (player.level().isClientSide)
			locked = ClientData.isLocked(result);
		else
			locked = SkillApi.isLockedFor(player, result);

		if (locked)
			cir.setReturnValue(false);
	}
}
