package com.limer.createtree.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.limer.createtree.common.GateHelper;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/**
 * Gates the Crushing Wheels: if the crushing/milling output is locked, applyRecipe is cancelled.
 * appliedRecipe is still set to true by the caller, so the raw input item is ejected unchanged
 * on the next tick - no items are lost.
 */
@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
public abstract class CrushingWheelControllerBlockEntityMixin extends BlockEntity {

	@Shadow
	public ProcessingInventory inventory;

	@Shadow
	public Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> findRecipe() {
		throw new AssertionError();
	}

	protected CrushingWheelControllerBlockEntityMixin() {
		super(null, null, null);
	}

	@Inject(method = "applyRecipe", at = @At("HEAD"), cancellable = true)
	private void createtree$blockLocked(CallbackInfo ci) {
		if (this.level == null || this.level.isClientSide)
			return;
		Optional<RecipeHolder<StandardProcessingRecipe<RecipeWrapper>>> recipe = findRecipe();
		if (recipe.isEmpty())
			return;
		ItemStack result = recipe.get().value().getResultItem(this.level.registryAccess());
		if (GateHelper.isBlocked(this, result)) {
			ci.cancel();
		} else {
			int rolls = Math.max(1, inventory.getStackInSlot(0).getCount());
			GateHelper.awardExp(this, result, rolls);
		}
	}
}
