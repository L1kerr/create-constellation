package com.limer.createtree.mixin;

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
		RecipeHolder<PressingRecipe> holder = recipe.get();

		if (GateHelper.isBlockedHolder(this, holder))
			cir.setReturnValue(Optional.empty());
	}

	@Inject(method = "onItemPressed", at = @At("HEAD"))
	private void createtree$awardExp(ItemStack result, CallbackInfo ci) {
		GateHelper.awardExp(this, result);
	}
}
