package com.limer.createtree.data;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Server-side progression state for one player.
 * Stored as a serializable data attachment on the player entity, so it persists with the world.
 * Not synced automatically; explicit payload sync is used instead.
 */
public class PlayerProgress implements INBTSerializable<CompoundTag> {

	private final Set<ResourceLocation> unlocked = new HashSet<>();
	private int exp = 0;
	private int points = 0;
	private boolean initialized = false;

	/** Grants starting points once, on the player's first join. */
	public void ensureInitialized(int startingPoints) {
		if (!initialized) {
			initialized = true;
			points += Math.max(0, startingPoints);
		}
	}

	public boolean isInitialized() {
		return initialized;
	}

	public boolean isUnlocked(ResourceLocation item) {
		return unlocked.contains(item);
	}

	public Set<ResourceLocation> unlocked() {
		return unlocked;
	}

	public int exp() {
		return exp;
	}

	public int points() {
		return points;
	}

	/**
	 * Adds EXP and converts whole multiples of expPerPoint into skill points.
	 * @return true if the point balance changed (a new point was earned).
	 */
	public boolean addExp(int amount, int expPerPoint) {
		if (amount <= 0)
			return false;
		exp += amount;
		boolean changed = false;
		if (expPerPoint > 0) {
			while (exp >= expPerPoint) {
				exp -= expPerPoint;
				points++;
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * Spends points to unlock an item.
	 * @return true if unlocked now, false if already unlocked or not enough points.
	 */
	public boolean tryUnlock(ResourceLocation item, int cost) {
		if (item == null || unlocked.contains(item))
			return false;
		if (points < cost)
			return false;
		points -= cost;
		unlocked.add(item);
		return true;
	}

	@Override
	public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Exp", exp);
		tag.putInt("Points", points);
		tag.putBoolean("Init", initialized);
		ListTag list = new ListTag();
		for (ResourceLocation rl : unlocked)
			list.add(StringTag.valueOf(rl.toString()));
		tag.put("Unlocked", list);
		return tag;
	}

	@Override
	public void deserializeNBT(net.minecraft.core.HolderLookup.Provider registries, CompoundTag tag) {
		unlocked.clear();
		exp = tag.getInt("Exp");
		points = tag.getInt("Points");
		initialized = tag.getBoolean("Init");
		ListTag list = tag.getList("Unlocked", Tag.TAG_STRING);
		for (int i = 0; i < list.size(); i++) {
			try {
				unlocked.add(ResourceLocation.parse(list.getString(i)));
			} catch (Exception ignored) {
			}
		}
	}

	/** Copy state into this instance (used when respawning with copyOnDeath). */
	public void copyFrom(PlayerProgress other) {
		this.unlocked.clear();
		this.unlocked.addAll(other.unlocked);
		this.exp = other.exp;
		this.points = other.points;
	}
}
