package com.limer.createtree.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.limer.createtree.common.GateHelper;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = DeployerBlockEntity.class, remap = false)
public abstract class DeployerBlockEntityMixin extends BlockEntity {

	protected DeployerBlockEntityMixin() {
		super(null, null, null);
	}

	@Inject(method = "getRecipe", at = @At("RETURN"), cancellable = true)
	private void createtree$gateDeployer(ItemStack stack,
										 CallbackInfoReturnable<RecipeHolder<? extends Recipe<? extends RecipeInput>>> cir) {
		RecipeHolder<? extends Recipe<? extends RecipeInput>> holder = cir.getReturnValue();
		if (holder == null || this.level == null || this.level.isClientSide)
			return;
		if (GateHelper.isBlockedHolder(this, holder))
			cir.setReturnValue(null);
	}
}
