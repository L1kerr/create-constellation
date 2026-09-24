package com.limer.createtree.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.limer.createtree.common.GateHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

@Mixin(value = BeltDeployerCallbacks.class, remap = false)
public abstract class BeltDeployerCallbacksMixin {

	@Redirect(
		method = "activate",
		at = @At(
			value = "INVOKE",
			target = "Lcom/simibubi/create/foundation/recipe/RecipeApplier;applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;"
		)
	)
	private static List<ItemStack> createtree$applyAndAward(
		Level level, ItemStack input, Recipe<?> recipe, boolean simulate,

		TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler,
		DeployerBlockEntity blockEntity, Recipe<?> enclosingRecipe) {

		List<ItemStack> results = RecipeApplier.applyRecipeOn(level, input, recipe, simulate);
		for (ItemStack result : results)
			GateHelper.awardExp(blockEntity, result);
		return results;
	}
}
