package com.limer.createtree.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.limer.createtree.common.GateHelper;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = SawBlockEntity.class, remap = false)
public abstract class SawBlockEntityMixin extends BlockEntity {

	@Shadow
	private int recipeIndex;

	@Shadow
	public ProcessingInventory inventory;

	@Shadow
	private List<RecipeHolder<? extends Recipe<?>>> getRecipes() {
		throw new AssertionError();
	}

	protected SawBlockEntityMixin() {
		super(null, null, null);
	}

	@Inject(method = "applyRecipe", at = @At("HEAD"), cancellable = true)
	private void createtree$blockLocked(CallbackInfo ci) {
		if (this.level == null || this.level.isClientSide)
			return;
		List<RecipeHolder<? extends Recipe<?>>> recipes = getRecipes();
		if (recipes.isEmpty())
			return;
		int index = Math.min(recipeIndex, recipes.size() - 1);
		RecipeHolder<? extends Recipe<?>> holder = recipes.get(index);
		ItemStack result = holder.value().getResultItem(this.level.registryAccess());

		boolean blocked = GateHelper.isBlocked(this, result) || GateHelper.isBlockedHolder(this, holder);
		if (blocked) {
			ci.cancel();
		} else {
			int rolls = Math.max(1, inventory.getStackInSlot(0).getCount());
			GateHelper.awardExp(this, result, rolls);
		}
	}
}
