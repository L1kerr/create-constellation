package com.limer.createtree.common;

import com.limer.createtree.data.SkillApi;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shared gating + EXP-credit logic for Create machines.
 *
 * Design: the skill tree is per-player. A machine has no intrinsic player, so gating and
 * EXP credit resolve to the nearest player within {@link #RADIUS} blocks.
 *  - Non-gated items (not in the tree) always pass: automation is untouched.
 *  - Gated items require a nearby player who has unlocked them; otherwise the craft is blocked.
 *  - EXP for a gated craft is credited to that nearest player.
 */
public final class GateHelper {

	private GateHelper() {
	}

	/** Radius (blocks) used to find the player that "owns" a machine craft. */
	public static final double RADIUS = 64.0;

	/** True when the machine must NOT produce this result right now. */
	public static boolean isBlocked(BlockEntity be, ItemStack result) {
		Level level = be.getLevel();
		if (level == null || level.isClientSide)
			return false; // server is authoritative; never gate on client
		if (!SkillApi.isLockedGlobal(result))
			return false; // not part of the tree -> no change to automation
		Player player = nearest(level, be.getBlockPos());
		if (player == null)
			return true; // gated item, nobody around to have unlocked it -> block
		return SkillApi.isLockedFor(player, result);
	}

	/** Credits EXP for a completed gated craft to the nearest player. */
	public static void awardExp(BlockEntity be, ItemStack result) {
		awardExp(be, result, 1);
	}

	/** Credits EXP for {@code times} completed gated crafts (batch machines like saw/crusher). */
	public static void awardExp(BlockEntity be, ItemStack result, int times) {
		Level level = be.getLevel();
		if (level == null || level.isClientSide)
			return;
		Player player = nearest(level, be.getBlockPos());
		SkillApi.awardExpFor(player, result, times); // no-op when player == null or result not gated
	}

	private static Player nearest(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel serverLevel))
			return null;
		return SkillApi.findNearestPlayer(serverLevel, pos, RADIUS);
	}
}
