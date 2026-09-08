package com.limer.createtree.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.limer.createtree.common.GateHelper;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Gates basin processing (Mechanical Mixer + Mechanical Press in basin mode):
 *  - matchBasinRecipe returns false for locked outputs, so the machine never starts the recipe.
 *  - applyBasinRecipe tail awards EXP for the produced outputs.
 */
@Mixin(value = BasinOperatingBlockEntity.class, remap = false)
public abstract class BasinOperatingBlockEntityMixin extends BlockEntity {

	@Shadow
	protected Recipe<?> currentRecipe;

	protected BasinOperatingBlockEntityMixin() {
		super(null, null, null); // never called, required for compilation only
	}

	@Inject(method = "matchBasinRecipe", at = @At("HEAD"), cancellable = true)
	private void createtree$blockLocked(Recipe<?> recipe, CallbackInfoReturnable<Boolean> cir) {
		if (recipe == null)
			return;
		for (ItemStack result : createtree$resultsOf(recipe))
			if (GateHelper.isBlocked(this, result)) {
				cir.setReturnValue(false);
				return;
			}
	}

	@Inject(method = "applyBasinRecipe", at = @At("TAIL"))
	private void createtree$awardExp(CallbackInfo ci) {
		if (currentRecipe == null)
			return;
		for (ItemStack result : createtree$resultsOf(currentRecipe))
			GateHelper.awardExp(this, result);
	}

	private List<ItemStack> createtree$resultsOf(Recipe<?> recipe) {
		if (recipe instanceof ProcessingRecipe<?, ?> processing)
			return processing.getRollableResultsAsItemStacks();
		if (this.level != null)
			return List.of(recipe.getResultItem(this.level.registryAccess()));
		return List.of();
	}
}
