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

public final class ClientData {

	private ClientData() {
	}

	private static volatile Map<ResourceLocation, SkillEntry> tree = Map.of();

	private static volatile Map<ResourceLocation, SkillEntry> owners = Map.of();
	private static volatile Map<Category, List<SkillEntry>> byCategory = new EnumMap<>(Category.class);
	private static volatile int expPerPoint = 100;
	private static volatile int exp = 0;
	private static volatile int points = 0;
	private static volatile Set<ResourceLocation> unlocked = new HashSet<>();
	private static volatile boolean sunIgnited = false;

	private static volatile ResourceLocation contractItem = null;
	private static volatile int contractTarget = 0;
	private static volatile int contractProgress = 0;
	private static volatile int contractReward = 0;

	public static void applyTree(TreeSyncPayload payload) {
		Map<ResourceLocation, SkillEntry> map = new java.util.LinkedHashMap<>();
		Map<ResourceLocation, SkillEntry> own = new java.util.HashMap<>();
		Map<Category, List<SkillEntry>> cats = new EnumMap<>(Category.class);
		for (Category c : Category.values())
			cats.put(c, new ArrayList<>());
		for (SkillEntry e : payload.entries()) {
			map.putIfAbsent(e.item(), e);
			cats.get(e.category()).add(e);
			for (ResourceLocation u : e.unlocks())
				own.put(u, e);
		}
		tree = Collections.unmodifiableMap(map);
		owners = Collections.unmodifiableMap(own);
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
		sunIgnited = payload.sunIgnited();
		contractItem = payload.contractItem();
		contractTarget = payload.contractTarget();
		contractProgress = payload.contractProgress();
		contractReward = payload.contractReward();
	}

	public static ResourceLocation contractItem() {
		return contractItem;
	}

	public static int contractTarget() {
		return contractTarget;
	}

	public static int contractProgress() {
		return contractProgress;
	}

	public static int contractReward() {
		return contractReward;
	}

	public static boolean sunIgnited() {
		return sunIgnited;
	}

	private static volatile int eventType = 0;
	private static volatile int eventSeconds = 0;
	private static volatile long eventReceivedAt = 0;
	private static volatile String goldenBranch = "";
	private static volatile Map<ResourceLocation, Integer> discounts = Map.of();

	public static void applyEvent(EventSyncPayload payload) {
		eventType = payload.eventType();
		eventSeconds = payload.secondsLeft();
		eventReceivedAt = System.currentTimeMillis();
		goldenBranch = payload.branch();
		Map<ResourceLocation, Integer> map = new java.util.HashMap<>();
		for (int i = 0; i < payload.discountItems().size(); i++)
			map.put(payload.discountItems().get(i), payload.discountPrices()[i]);
		discounts = Collections.unmodifiableMap(map);
	}

	public static int eventType() {
		return eventSecondsLeft() > 0 ? eventType : 0;
	}

	public static String goldenBranch() {
		return eventType() == 1 ? goldenBranch : "";
	}

	public static int discountPrice(ResourceLocation item) {
		return eventType() == 2 ? discounts.getOrDefault(item, -1) : -1;
	}

	public static Map<ResourceLocation, Integer> discounts() {
		return eventType() == 2 ? discounts : Map.of();
	}

	public static int eventSecondsLeft() {
		if (eventSeconds <= 0)
			return 0;
		int elapsed = (int) ((System.currentTimeMillis() - eventReceivedAt) / 1000);
		return Math.max(0, eventSeconds - elapsed);
	}

	public static void setSunIgnitedLocal() {
		sunIgnited = true;
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

	public static SkillEntry ownerOf(ResourceLocation item) {
		return owners.get(item);
	}

	public static int orderOf(ResourceLocation item) {
		int i = 0;
		for (ResourceLocation rl : tree.keySet()) {
			if (rl.equals(item))
				return i;
			i++;
		}
		return -1;
	}

	private static volatile com.limer.createtree.common.TreeLayout.Layout layout =
		com.limer.createtree.common.TreeLayout.compute(List.of());

	public static com.limer.createtree.common.TreeLayout.Layout layout() {
		return layout;
	}

	public static boolean isLocked(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return false;
		ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
		SkillEntry e = entry(id);
		if (e != null)
			return !isUnlocked(e.item());

		SkillEntry owner = owners.get(id);
		return owner != null && !isUnlocked(owner.item());
	}
}
