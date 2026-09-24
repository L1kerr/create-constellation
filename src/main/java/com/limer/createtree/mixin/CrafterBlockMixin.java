package com.limer.createtree.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.limer.createtree.common.GateHelper;
import com.limer.createtree.data.SkillApi;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {

	@Inject(method = "dispenseFrom(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true)
	private void createtree$gateCrafter(BlockState state, ServerLevel level, BlockPos pos,
										CallbackInfo ci) {
		BlockEntity be = level.getBlockEntity(pos);
		if (!(be instanceof CrafterBlockEntity crafter))
			return;

		CraftingInput input = crafter.asCraftInput();
		Optional<RecipeHolder<CraftingRecipe>> optional = CrafterBlock.getPotentialResults(level, input);
		if (optional.isEmpty())
			return;

		ItemStack result = optional.get().value().assemble(input, level.registryAccess());
		if (result.isEmpty())
			return;

		if (GateHelper.isBlocked(crafter, result)) {

			level.levelEvent(1050, pos, 0);
			ci.cancel();
			return;
		}

		Player player = SkillApi.findNearestPlayer(level, pos, GateHelper.RADIUS);
		SkillApi.awardExpFor(player, result);
	}
}
