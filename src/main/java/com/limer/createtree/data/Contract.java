package com.limer.createtree.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class Contract {

	private ResourceLocation item = null;
	private int target = 0;
	private int progress = 0;
	private int reward = 0;

	public boolean isEmpty() {
		return item == null || target <= 0;
	}

	public ResourceLocation item() {
		return item;
	}

	public int target() {
		return target;
	}

	public int progress() {
		return progress;
	}

	public int reward() {
		return reward;
	}

	public boolean isComplete() {
		return !isEmpty() && progress >= target;
	}

	public void set(ResourceLocation item, int target, int reward) {
		this.item = item;
		this.target = Math.max(1, target);
		this.progress = 0;
		this.reward = Math.max(1, reward);
	}

	public int addProgress(int times) {
		if (isEmpty())
			return 0;
		int room = target - progress;
		int added = Math.min(Math.max(0, times), room);
		progress += added;
		return added;
	}

	public void clear() {
		item = null;
		target = 0;
		progress = 0;
		reward = 0;
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		if (!isEmpty()) {
			tag.putString("Item", item.toString());
			tag.putInt("Target", target);
			tag.putInt("Progress", progress);
			tag.putInt("Reward", reward);
		}
		return tag;
	}

	public void load(CompoundTag tag) {
		clear();
		if (tag == null || !tag.contains("Item"))
			return;
		try {
			item = ResourceLocation.parse(tag.getString("Item"));
		} catch (Exception e) {
			return;
		}
		target = tag.getInt("Target");
		progress = tag.getInt("Progress");
		reward = tag.getInt("Reward");
	}

	public void copyFrom(Contract other) {
		this.item = other.item;
		this.target = other.target;
		this.progress = other.progress;
		this.reward = other.reward;
	}
}
