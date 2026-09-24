package com.limer.createtree.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class SkillTreeConfig {

	public static final int DEFAULT_EXP_PER_POINT = 100;
	public static final int DEFAULT_STARTING_POINTS = 1;

	private final int expPerPoint;

	private final int startingPoints;

	private final Map<ResourceLocation, SkillEntry> byItem;
	private final Map<Category, List<SkillEntry>> byCategory;

	public SkillTreeConfig(int expPerPoint, int startingPoints, List<SkillEntry> entries) {
		this.expPerPoint = Math.max(1, expPerPoint);
		this.startingPoints = Math.max(0, startingPoints);

		Map<ResourceLocation, SkillEntry> items = new java.util.LinkedHashMap<>();
		Map<Category, List<SkillEntry>> cats = new EnumMap<>(Category.class);
		for (Category c : Category.values())
			cats.put(c, new ArrayList<>());

		for (SkillEntry e : entries) {
			if (e == null || !e.isValid())
				continue;
			items.putIfAbsent(e.item(), e);
			cats.get(e.category()).add(e);
		}
		this.byItem = Collections.unmodifiableMap(items);
		cats.replaceAll((c, l) -> Collections.unmodifiableList(l));
		this.byCategory = Collections.unmodifiableMap(cats);
	}

	public static SkillTreeConfig empty() {
		return new SkillTreeConfig(DEFAULT_EXP_PER_POINT, DEFAULT_STARTING_POINTS, List.of());
	}

	public int expPerPoint() {
		return expPerPoint;
	}

	public int startingPoints() {
		return startingPoints;
	}

	public boolean isEmpty() {
		return byItem.isEmpty();
	}

	public boolean isGated(ResourceLocation item) {
		return item != null && byItem.containsKey(item);
	}

	public SkillEntry entry(ResourceLocation item) {
		return item == null ? null : byItem.get(item);
	}

	public SkillEntry entry(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return null;
		return byItem.get(idOf(stack));
	}

	public ResourceLocation idOf(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return null;
		return BuiltInRegistries.ITEM.getKey(stack.getItem());
	}

	public List<SkillEntry> entries(Category category) {
		return byCategory.getOrDefault(category, List.of());
	}

	public Map<ResourceLocation, SkillEntry> allEntries() {
		return byItem;
	}

	public List<SkillEntry> all() {
		return List.copyOf(byItem.values());
	}

	public List<SkillEntry> nodes() {
		java.util.Set<ResourceLocation> grouped = new java.util.HashSet<>();
		for (SkillEntry e : byItem.values())
			grouped.addAll(e.unlocks());
		List<SkillEntry> nodes = new ArrayList<>(byItem.size());
		for (SkillEntry e : byItem.values())
			if (!grouped.contains(e.item()))
				nodes.add(e);
		return Collections.unmodifiableList(nodes);
	}

	public ResourceLocation parentOf(ResourceLocation item) {
		SkillEntry e = byItem.get(item);
		if (e == null)
			return null;
		List<SkillEntry> list = List.copyOf(byItem.values());
		int idx = list.indexOf(e);
		return idx > 0 ? list.get(idx - 1).item() : null;
	}

	public List<ResourceLocation> groupOf(ResourceLocation item) {
		SkillEntry e = byItem.get(item);
		return e == null ? List.of() : e.unlocks();
	}

	public List<ResourceLocation> groupAndSelf(ResourceLocation item) {
		SkillEntry e = byItem.get(item);
		if (e == null)
			return List.of();
		java.util.List<ResourceLocation> all = new ArrayList<>(e.unlocks().size() + 1);
		all.add(e.item());
		all.addAll(e.unlocks());
		return all;
	}
}
