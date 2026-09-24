package com.limer.createtree.mixin;

import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.limer.createtree.common.GateHelper;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;

@Mixin(value = SpoutBlockEntity.class, remap = false)
public abstract class SpoutBlockEntityMixin extends BlockEntity {

	protected SpoutBlockEntityMixin() {
		super(null, null, null);
	}

	@Redirect(
		method = { "onItemReceived", "whenItemHeld" },
		at = @At(
			value = "INVOKE",
			target = "Lcom/simibubi/create/content/fluids/spout/FillingBySpout;canItemBeFilled(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Z"
		)
	)
	private boolean createtree$canFill(Level level, ItemStack stack) {
		if (!FillingBySpout.canItemBeFilled(level, stack))
			return false;
		if (level.isClientSide)
			return true;
		return !createtree$allOutputsLocked(level, stack);
	}

	@Redirect(
		method = "whenItemHeld",
		at = @At(
			value = "INVOKE",
			target = "Lcom/simibubi/create/content/fluids/spout/FillingBySpout;fillItem(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;"
		)
	)
	private ItemStack createtree$fillAndAward(Level level, int requiredAmount, ItemStack stack, FluidStack fluid) {
		ItemStack out = FillingBySpout.fillItem(level, requiredAmount, stack, fluid);
		if (!out.isEmpty())
			GateHelper.awardExp(this, out);
		return out;
	}

	private boolean createtree$allOutputsLocked(Level level, ItemStack stack) {
		SingleRecipeInput input = new SingleRecipeInput(stack);
		boolean anyCandidate = false;
		boolean anyAllowed = false;

		Optional<RecipeHolder<FillingRecipe>> seq = SequencedAssemblyRecipe.getRecipe(
			level, input, AllRecipeTypes.FILLING.getType(), FillingRecipe.class);
		if (seq.isPresent()) {
			anyCandidate = true;
			ItemStack subResult = seq.get().value().getResultItem(level.registryAccess());
			ItemStack finalResult = level.getRecipeManager().byKey(seq.get().id())
				.map(h -> h.value().getResultItem(level.registryAccess()))
				.orElse(subResult);
			if (!GateHelper.isBlocked(this, subResult) && !GateHelper.isBlocked(this, finalResult))
				anyAllowed = true;
		}

		List<RecipeHolder<Recipe<SingleRecipeInput>>> recipes =
			level.getRecipeManager().getRecipesFor(AllRecipeTypes.FILLING.getType(), input, level);
		for (RecipeHolder<Recipe<SingleRecipeInput>> rh : recipes) {
			anyCandidate = true;
			boolean blocked = false;
			if (rh.value() instanceof ProcessingRecipe<?, ?> pr) {
				for (ItemStack out : pr.getRollableResultsAsItemStacks())
					if (GateHelper.isBlocked(this, out)) {
						blocked = true;
						break;
					}
			} else if (GateHelper.isBlocked(this, rh.value().getResultItem(level.registryAccess()))) {
				blocked = true;
			}
			if (!blocked)
				anyAllowed = true;
		}

		return anyCandidate && !anyAllowed;
	}
}
