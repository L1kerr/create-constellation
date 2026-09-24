package com.limer.createtree.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.limer.createtree.common.GateHelper;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public abstract class MechanicalCrafterBlockEntityMixin extends BlockEntity {

	protected MechanicalCrafterBlockEntityMixin() {
		super(null, null, null);
	}

	@Redirect(
		method = "tick",
		at = @At(
			value = "INVOKE",
			target = "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler;tryToApplyRecipe(Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)Lnet/minecraft/world/item/ItemStack;"
		)
	)
	private ItemStack createtree$gatedApply(Level level, GroupedItems items) {
		ItemStack result = RecipeGridHandler.tryToApplyRecipe(level, items);
		if (result == null)
			return null;
		if (GateHelper.isBlocked(this, result))
			return null;
		GateHelper.awardExp(this, result);
		return result;
	}
}
