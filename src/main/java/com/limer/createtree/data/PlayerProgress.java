package com.limer.createtree.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.common.util.INBTSerializable;

public class PlayerProgress implements INBTSerializable<CompoundTag> {

	private final Set<ResourceLocation> unlocked = new HashSet<>();
	private final Map<ResourceLocation, Integer> craftCounts = new HashMap<>();
	private final Contract contract = new Contract();
	private int exp = 0;
	private int points = 0;
	private boolean initialized = false;
	private boolean sunIgnited = false;

	public Contract contract() {
		return contract;
	}

	public boolean sunIgnited() {
		return sunIgnited;
	}

	public boolean ignite() {
		if (sunIgnited)
			return false;
		sunIgnited = true;
		return true;
	}

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

	public int craftCount(ResourceLocation item) {
		return craftCounts.getOrDefault(item, 0);
	}

	public double registerCrafts(ResourceLocation item, int times) {
		if (item == null || times <= 0)
			return 1.0;
		int before = craftCounts.getOrDefault(item, 0);
		craftCounts.put(item, before + times);

		double sum = 0;
		for (int c = before; c < before + times; c++)
			sum += tierMultiplier(c);
		return sum / times;
	}

	private static double tierMultiplier(int craftsBefore) {
		if (craftsBefore < 10)
			return 1.0;
		if (craftsBefore < 30)
			return 0.5;
		if (craftsBefore < 60)
			return 0.25;
		return 0.05;
	}

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
		tag.putBoolean("Ignited", sunIgnited);
		tag.put("Contract", contract.save());
		ListTag list = new ListTag();
		for (ResourceLocation rl : unlocked)
			list.add(StringTag.valueOf(rl.toString()));
		tag.put("Unlocked", list);
		CompoundTag crafts = new CompoundTag();
		for (Map.Entry<ResourceLocation, Integer> e : craftCounts.entrySet())
			crafts.putInt(e.getKey().toString(), e.getValue());
		tag.put("CraftCounts", crafts);
		return tag;
	}

	@Override
	public void deserializeNBT(net.minecraft.core.HolderLookup.Provider registries, CompoundTag tag) {
		unlocked.clear();
		exp = tag.getInt("Exp");
		points = tag.getInt("Points");
		initialized = tag.getBoolean("Init");
		sunIgnited = tag.getBoolean("Ignited");
		contract.load(tag.getCompound("Contract"));
		ListTag list = tag.getList("Unlocked", Tag.TAG_STRING);
		for (int i = 0; i < list.size(); i++) {
			try {
				unlocked.add(ResourceLocation.parse(list.getString(i)));
			} catch (Exception ignored) {
			}
		}
		craftCounts.clear();
		CompoundTag crafts = tag.getCompound("CraftCounts");
		for (String key : crafts.getAllKeys()) {
			try {
				craftCounts.put(ResourceLocation.parse(key), crafts.getInt(key));
			} catch (Exception ignored) {
			}
		}
	}

	public void copyFrom(PlayerProgress other) {
		this.unlocked.clear();
		this.unlocked.addAll(other.unlocked);
		this.craftCounts.clear();
		this.craftCounts.putAll(other.craftCounts);
		this.contract.copyFrom(other.contract);
		this.exp = other.exp;
		this.points = other.points;
		this.initialized = other.initialized;
		this.sunIgnited = other.sunIgnited;
	}

	public void addPoints(int amount) {
		points = Math.max(0, points + amount);
	}

	public void setPoints(int amount) {
		points = Math.max(0, amount);
	}

	public void softReset(int startingPoints) {
		unlocked.clear();
		exp = 0;
		points = Math.max(0, startingPoints);
	}

	public void hardReset() {
		unlocked.clear();
		craftCounts.clear();
		exp = 0;
		points = 0;
		initialized = false;
		sunIgnited = false;
	}

	public void forceUnlock(ResourceLocation item) {
		if (item != null)
			unlocked.add(item);
	}

	public void forceUnlockAll(java.util.Collection<ResourceLocation> items) {
		if (items != null)
			unlocked.addAll(items);
	}

	public void forceLock(ResourceLocation item) {
		unlocked.remove(item);
	}
}
