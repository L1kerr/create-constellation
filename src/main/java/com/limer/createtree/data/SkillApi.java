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

public final class SkillApi {

	private SkillApi() {
	}

	public static PlayerProgress progress(Player player) {
		return player.getData(ModAttachments.PROGRESS);
	}

	public static boolean isLockedGlobal(ItemStack result) {
		SkillTreeConfig tree = SkillTrees.get();
		if (tree.isEmpty())
			return false;
		SkillEntry entry = tree.entry(result);
		return entry != null;
	}

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

	public static void awardExpFor(Player player, ItemStack result) {
		awardExpFor(player, result, 1);
	}

	public static void awardExpFor(Player player, ItemStack result, int times) {
		if (player == null || player.level().isClientSide || result.isEmpty() || times <= 0)
			return;
		SkillTreeConfig tree = SkillTrees.get();
		SkillEntry entry = tree.entry(result);
		if (entry == null || entry.exp() <= 0)
			return;

		PlayerProgress progress = progress(player);

		Contract contract = progress.contract();
		if (!contract.isEmpty() && contract.item().equals(entry.item())) {
			contract.addProgress(times);
			if (contract.isComplete()) {
				int reward = contract.reward();
				contract.clear();
				progress.addPoints(reward);
				if (player instanceof ServerPlayer sp)
					sp.displayClientMessage(net.minecraft.network.chat.Component.translatable(
						"message.createtree.contract_done", reward), true);
			}
		}

		double golden = com.limer.createtree.event.WorldEvents.multiplierFor(entry.branch());

		double diminish = progress.registerCrafts(entry.item(), times);
		progress.addExp((int) Math.max(1, Math.round(entry.exp() * golden * diminish * times)), tree.expPerPoint());
		if (player instanceof ServerPlayer serverPlayer)
			ModNetwork.sendToPlayer(serverPlayer, new ProgressSyncPayload(progress));
	}

	public static void awardExpFor(UUID playerId, ServerLevel level, ItemStack result) {
		if (playerId == null || level == null)
			return;
		Player player = level.getPlayerByUUID(playerId);
		awardExpFor(player, result);
	}

	public static boolean unlock(ServerPlayer player, ResourceLocation item) {
		SkillTreeConfig tree = SkillTrees.get();
		SkillEntry entry = tree.entry(item);
		if (entry == null)
			return false;
		PlayerProgress progress = progress(player);

		List<SkillEntry> ordered = tree.nodes();
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
		if (progress.tryUnlock(item, effectiveCost(entry))) {

			progress.forceUnlockAll(entry.unlocks());
			ModNetwork.sendToPlayer(player, new ProgressSyncPayload(progress));
			return true;
		}
		return false;
	}

	public static int effectiveCost(SkillEntry entry) {
		int discount = com.limer.createtree.event.WorldEvents.discountPrice(entry.item());
		return discount >= 0 ? discount : entry.cost();
	}

	public static void ignite(ServerPlayer player) {
		PlayerProgress progress = progress(player);
		if (progress.ignite())
			ModNetwork.sendToPlayer(player, new ProgressSyncPayload(progress));
	}

	public static void syncTo(ServerPlayer player) {
		PlayerProgress progress = progress(player);
		progress.ensureInitialized(SkillTrees.get().startingPoints());
		ModNetwork.sendTreeTo(player);
		ModNetwork.sendToPlayer(player, new ProgressSyncPayload(progress));

		com.limer.createtree.event.WorldEvents.syncTo(player);
	}

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
