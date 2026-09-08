package com.limer.createtree.mixin;

import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.limer.createtree.common.GateHelper;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Gates the press in world / belt mode (basin mode is handled by BasinOperatingBlockEntityMixin):
 *  - getRecipe returns empty for locked outputs, so nothing is pressed.
 *  - onItemPressed awards EXP for the pressed result.
 */
@Mixin(value = MechanicalPressBlockEntity.class, remap = false)
public abstract class MechanicalPressBlockEntityMixin extends BlockEntity {

	protected MechanicalPressBlockEntityMixin() {
		super(null, null, null);
	}

	@Inject(method = "getRecipe", at = @At("RETURN"), cancellable = true)
	private void createtree$blockLocked(ItemStack item, CallbackInfoReturnable<Optional<RecipeHolder<PressingRecipe>>> cir) {
		Optional<RecipeHolder<PressingRecipe>> recipe = cir.getReturnValue();
		if (recipe.isEmpty() || this.level == null)
			return;
		for (ItemStack result : createtree$resultsOf(recipe.get()))
			if (GateHelper.isBlocked(this, result)) {
				cir.setReturnValue(Optional.empty());
				return;
			}
	}

	@Inject(method = "onItemPressed", at = @At("HEAD"))
	private void createtree$awardExp(ItemStack result, CallbackInfo ci) {
		GateHelper.awardExp(this, result);
	}

	private List<ItemStack> createtree$resultsOf(RecipeHolder<PressingRecipe> holder) {
		return holder.value().getRollableResultsAsItemStacks();
	}
}
