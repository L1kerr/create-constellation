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
