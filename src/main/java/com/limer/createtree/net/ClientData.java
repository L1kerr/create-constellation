package com.limer.createtree.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.limer.createtree.config.Category;
import com.limer.createtree.config.SkillEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side cache of the synced tree + progress. Used to render the GUI and to
 * pre-check gating on the client (the server is still authoritative).
 * Not annotated @OnlyIn because the class is referenced from common network code;
 * it only ever holds meaningful data on the client.
 */
public final class ClientData {

	private ClientData() {
	}

	private static volatile Map<ResourceLocation, SkillEntry> tree = Map.of();
	private static volatile Map<Category, List<SkillEntry>> byCategory = new EnumMap<>(Category.class);
	private static volatile int expPerPoint = 100;
	private static volatile int exp = 0;
	private static volatile int points = 0;
	private static volatile Set<ResourceLocation> unlocked = new HashSet<>();

	public static void applyTree(TreeSyncPayload payload) {
		Map<ResourceLocation, SkillEntry> map = new java.util.LinkedHashMap<>();
		Map<Category, List<SkillEntry>> cats = new EnumMap<>(Category.class);
		for (Category c : Category.values())
			cats.put(c, new ArrayList<>());
		for (SkillEntry e : payload.entries()) {
			map.putIfAbsent(e.item(), e);
			cats.get(e.category()).add(e);
		}
		tree = Collections.unmodifiableMap(map);
		cats.replaceAll((c, l) -> Collections.unmodifiableList(l));
		byCategory = cats;
		expPerPoint = payload.expPerPoint();
		layout = com.limer.createtree.common.TreeLayout.compute(List.copyOf(map.values()));
	}

	public static void applyProgress(ProgressSyncPayload payload) {
		unlocked = new HashSet<>(payload.unlocked());
		exp = payload.exp();
		points = payload.points();
		expPerPoint = payload.expPerPoint();
	}

	public static Map<ResourceLocation, SkillEntry> tree() {
		return tree;
	}

	public static List<SkillEntry> entries(Category category) {
		return byCategory.getOrDefault(category, List.of());
	}

	public static int expPerPoint() {
		return expPerPoint;
	}

	public static int exp() {
		return exp;
	}

	public static int points() {
		return points;
	}

	public static boolean isUnlocked(ResourceLocation item) {
		return unlocked.contains(item);
	}

	public static Set<ResourceLocation> unlocked() {
		return unlocked;
	}

	public static SkillEntry entry(ResourceLocation item) {
		return tree.get(item);
	}

	/** Position of an entry in the config order, -1 when unknown. */
	public static int orderOf(ResourceLocation item) {
		int i = 0;
		for (ResourceLocation rl : tree.keySet()) {
			if (rl.equals(item))
				return i;
			i++;
		}
		return -1;
	}

	/** Cached solar-system layout; recomputed only when the tree payload changes. */
	private static volatile com.limer.createtree.common.TreeLayout.Layout layout =
		com.limer.createtree.common.TreeLayout.compute(List.of());

	public static com.limer.createtree.common.TreeLayout.Layout layout() {
		return layout;
	}

	/** Client-side lock preview for rendering; server is authoritative. */
	public static boolean isLocked(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return false;
		SkillEntry e = entry(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
		return e != null && !isUnlocked(e.item());
	}
}
