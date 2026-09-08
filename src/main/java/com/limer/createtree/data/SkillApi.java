package com.limer.createtree.data;

import java.util.List;
import java.util.UUID;

import com.limer.createtree.config.SkillEntry;
import com.limer.createtree.config.SkillTreeConfig;
import com.limer.createtree.config.SkillTrees;
import com.limer.createtree.net.ModNetwork;
import com.limer.createtree.net.ProgressSyncPayload;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side progression logic: gating checks, EXP awarding, unlocking.
 */
public final class SkillApi {

	private SkillApi() {
	}

	public static PlayerProgress progress(Player player) {
		return player.getData(ModAttachments.PROGRESS);
	}

	/**
	 * Global gate: an item is craftable by automation when it is either
	 * not part of the skill tree, or the tree is empty (nothing configured).
	 */
	public static boolean isLockedGlobal(ItemStack result) {
		SkillTreeConfig tree = SkillTrees.get();
		if (tree.isEmpty())
			return false;
		SkillEntry entry = tree.entry(result);
		return entry != null;
	}

	/** Player-specific gate used for vanilla crafting and mechanical crafters with an owner. */
	public static boolean isLockedFor(Player player, ItemStack result) {
		if (player == null)
			return isLockedGlobal(result);
		SkillTreeConfig tree = SkillTrees.get();
		if (tree.isEmpty())
			return false;
		SkillEntry entry = tree.entry(result);
		if (entry == null)
			return false;
		return !progress(player).isUnlocked(entry.item());
	}

	public static boolean isLockedFor(UUID playerId, ServerLevel level, ItemStack result) {
		if (playerId == null)
			return isLockedGlobal(result);
		Player player = level.getPlayerByUUID(playerId);
		return isLockedFor(player, result);
	}

	/**
	 * Awards EXP for one craft of a gated item; converts EXP to points.
	 * Only gated items yield EXP (crafting non-tree items gives nothing).
	 */
	public static void awardExpFor(Player player, ItemStack result) {
		awardExpFor(player, result, 1);
	}

	/** Awards EXP for {@code times} crafts of a gated item in one operation (batch processing). */
	public static void awardExpFor(Player player, ItemStack result, int times) {
		if (player == null || player.level().isClientSide || result.isEmpty() || times <= 0)
			return;
		SkillTreeConfig tree = SkillTrees.get();
		SkillEntry entry = tree.entry(result);
		if (entry == null || entry.exp() <= 0)
			return;

		PlayerProgress progress = progress(player);
		progress.addExp(entry.exp() * times, tree.expPerPoint());
		if (player instanceof ServerPlayer serverPlayer)
			ModNetwork.sendToPlayer(serverPlayer, new ProgressSyncPayload(progress));
	}

	public static void awardExpFor(UUID playerId, ServerLevel level, ItemStack result) {
		if (playerId == null || level == null)
			return;
		Player player = level.getPlayerByUUID(playerId);
		awardExpFor(player, result);
	}

	/** Tries to spend points and unlock an item; syncs on success. */
	public static boolean unlock(ServerPlayer player, ResourceLocation item) {
		SkillTreeConfig tree = SkillTrees.get();
		SkillEntry entry = tree.entry(item);
		if (entry == null)
			return false;
		PlayerProgress progress = progress(player);
		// tree rule: at least one radial parent (Sun/planet mesh) must be unlocked first.
		// ring-1 nodes have parent -1 (the Sun core) and need no prerequisite.
		List<SkillEntry> ordered = tree.all();
		int idx = ordered.indexOf(entry);
		com.limer.createtree.common.TreeLayout.Layout layout = com.limer.createtree.common.TreeLayout.compute(ordered);
		if (idx >= 0 && layout.parents[idx] != null) {
			boolean anyParent = false;
			for (int p : layout.parents[idx]) {
				if (p < 0 || progress.isUnlocked(ordered.get(p).item())) {
					anyParent = true;
					break;
				}
			}
			if (!anyParent)
				return false;
		}
		if (progress.tryUnlock(item, entry.cost())) {
			ModNetwork.sendToPlayer(player, new ProgressSyncPayload(progress));
			return true;
		}
		return false;
	}

	public static void syncTo(ServerPlayer player) {
		PlayerProgress progress = progress(player);
		progress.ensureInitialized(SkillTrees.get().startingPoints());
		ModNetwork.sendTreeTo(player);
		ModNetwork.sendToPlayer(player, new ProgressSyncPayload(progress));
	}

	/** Nearest online player to a machine position — used to credit EXP to a bystander. */
	public static Player findNearestPlayer(ServerLevel level, net.minecraft.core.BlockPos pos, double radius) {
		Player best = null;
		double bestDist = radius * radius;
		for (Player p : level.players()) {
			double d = p.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
			if (d <= bestDist) {
				bestDist = d;
				best = p;
			}
		}
		return best;
	}
}
