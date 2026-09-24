package com.limer.createtree.common;

import com.limer.createtree.data.SkillApi;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class GateHelper {

	private GateHelper() {
	}

	public static final double RADIUS = 64.0;

	public static boolean isBlocked(BlockEntity be, ItemStack result) {
		Level level = be.getLevel();
		if (level == null || level.isClientSide)
			return false;
		if (!SkillApi.isLockedGlobal(result))
			return false;
		Player player = nearest(level, be.getBlockPos());
		if (player == null)
			return true;
		return SkillApi.isLockedFor(player, result);
	}

	public static void awardExp(BlockEntity be, ItemStack result) {
		awardExp(be, result, 1);
	}

	public static void awardExp(BlockEntity be, ItemStack result, int times) {
		Level level = be.getLevel();
		if (level == null || level.isClientSide)
			return;
		Player player = nearest(level, be.getBlockPos());
		SkillApi.awardExpFor(player, result, times);
	}

	private static Player nearest(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel serverLevel))
			return null;
		return SkillApi.findNearestPlayer(serverLevel, pos, RADIUS);
	}

	public static boolean isBlockedHolder(BlockEntity be, net.minecraft.world.item.crafting.RecipeHolder<?> holder) {
		if (holder == null)
			return false;
		Level level = be.getLevel();
		if (level == null || level.isClientSide)
			return false;

		net.minecraft.world.item.crafting.Recipe<?> recipe = holder.value();
		if (isBlocked(be, recipe.getResultItem(level.registryAccess())))
			return true;

		var parentOpt = level.getRecipeManager().byKey(holder.id());
		if (parentOpt.isPresent()
			&& parentOpt.get().value() instanceof com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe seq)
			return isBlocked(be, seq.getResultItem(level.registryAccess()));
		return false;
	}

	public static void awardExpHolder(BlockEntity be, net.minecraft.world.item.crafting.RecipeHolder<?> holder) {
		if (holder == null)
			return;
		Level level = be.getLevel();
		if (level == null || level.isClientSide)
			return;
		awardExp(be, holder.value().getResultItem(level.registryAccess()));
		var parentOpt = level.getRecipeManager().byKey(holder.id());
		if (parentOpt.isPresent()
			&& parentOpt.get().value() instanceof com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe seq)
			awardExp(be, seq.getResultItem(level.registryAccess()));
	}

	public static boolean isBlocked(Level level, BlockPos pos, ItemStack result) {
		if (level == null || level.isClientSide)
			return false;
		if (!SkillApi.isLockedGlobal(result))
			return false;
		Player player = nearest(level, pos);
		if (player == null)
			return true;
		return SkillApi.isLockedFor(player, result);
	}

	public static boolean isBlockedFor(Player player, ItemStack result) {
		if (player == null || result.isEmpty())
			return false;
		if (player.level().isClientSide)
			return com.limer.createtree.net.ClientData.isLocked(result);
		return SkillApi.isLockedFor(player, result);
	}

	public static void awardExp(Level level, BlockPos pos, ItemStack result) {
		if (level == null || level.isClientSide)
			return;
		Player player = nearest(level, pos);
		SkillApi.awardExpFor(player, result);
	}
}
