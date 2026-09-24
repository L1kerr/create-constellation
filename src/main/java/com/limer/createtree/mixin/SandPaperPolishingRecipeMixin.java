package com.limer.createtree.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.limer.createtree.common.GateHelper;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = SandPaperPolishingRecipe.class, remap = false)
public abstract class SandPaperPolishingRecipeMixin {

	@Inject(method = "applyPolish", at = @At("RETURN"), cancellable = true)
	private static void createtree$gatePolish(Level world, Vec3 position, ItemStack stack, ItemStack sandPaperStack,
											  CallbackInfoReturnable<ItemStack> cir) {
		ItemStack polished = cir.getReturnValue();
		if (polished.isEmpty() || world.isClientSide)
			return;
		if (GateHelper.isBlocked(world, BlockPos.containing(position), polished))
			cir.setReturnValue(stack);
	}
}
