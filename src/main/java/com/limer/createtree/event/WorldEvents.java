package com.limer.createtree.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.limer.createtree.CreateTreeMod;
import com.limer.createtree.config.SkillEntry;
import com.limer.createtree.config.SkillTrees;
import com.limer.createtree.net.EventSyncPayload;
import com.limer.createtree.net.ModNetwork;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class WorldEvents {

	private WorldEvents() {
	}

	public enum Type {
		NONE, GOLDEN_PLANET, DISCOUNT_NIGHT
	}

	private static final int GOLDEN_DURATION_TICKS = 2 * 60 * 20;
	private static final int DISCOUNT_DURATION_TICKS = 3 * 60 * 20;

	private static final int BASE_COOLDOWN_TICKS = 10 * 60 * 20;
	private static final int JITTER_TICKS = 4 * 60 * 20;

	private static final int W_GOLDEN = 5;
	private static final int W_DISCOUNT = 3;

	private static final Random RND = new Random();

	private static Type active = Type.NONE;
	private static int remainingTicks = 0;
	private static int cooldownTicks = 60 * 20;

	private static String goldenBranch = null;

	private static final Map<ResourceLocation, Integer> discountPrices = new HashMap<>();

	public static void serverTick(MinecraftServer server) {
		if (server.overworld().getGameTime() < 600)
			return;

		if (active != Type.NONE) {
			remainingTicks -= 20;
			if (remainingTicks <= 0)
				end(server);
			return;
		}

		cooldownTicks -= 20;
		if (cooldownTicks <= 0)
			startRandom(server);
	}

	public static Type active() {
		return active;
	}

	public static String goldenBranch() {
		return goldenBranch == null ? "" : goldenBranch;
	}

	public static int secondsLeft() {
		return Math.max(0, remainingTicks / 20);
	}

	public static double multiplierFor(String branch) {
		return active == Type.GOLDEN_PLANET && branch.equals(goldenBranch) ? 2.0 : 1.0;
	}

	public static int discountPrice(ResourceLocation item) {
		if (active != Type.DISCOUNT_NIGHT)
			return -1;
		return discountPrices.getOrDefault(item, -1);
	}

	private static void startRandom(MinecraftServer server) {
		if (SkillTrees.get().isEmpty()) {
			cooldownTicks = BASE_COOLDOWN_TICKS;
			return;
		}
		int roll = RND.nextInt(W_GOLDEN + W_DISCOUNT);
		Type type = roll < W_GOLDEN ? Type.GOLDEN_PLANET : Type.DISCOUNT_NIGHT;
		start(server, type, null);
	}

	private static boolean start(MinecraftServer server, Type type, String forcedBranch) {
		List<SkillEntry> all = SkillTrees.get().all();
		if (all.isEmpty())
			return false;

		switch (type) {
			case GOLDEN_PLANET -> {
				List<String> branches = new ArrayList<>();
				for (SkillEntry e : all)
					if (!branches.contains(e.branch()))
						branches.add(e.branch());
				if (forcedBranch != null) {
					if (!branches.contains(forcedBranch))
						return false;
					goldenBranch = forcedBranch;
				} else {
					goldenBranch = branches.get(RND.nextInt(branches.size()));
				}
				remainingTicks = GOLDEN_DURATION_TICKS;
			}
			case DISCOUNT_NIGHT -> {

				discountPrices.clear();
				List<SkillEntry> pool = new ArrayList<>(all);
				Collections.shuffle(pool, RND);
				for (int i = 0; i < pool.size() && discountPrices.size() < 3; i++) {
					SkillEntry e = pool.get(i);
					int price = 1 + RND.nextInt(Math.max(1, e.cost()));
					discountPrices.put(e.item(), price);
				}
				if (discountPrices.isEmpty())
					return false;
				remainingTicks = DISCOUNT_DURATION_TICKS;
			}
			case NONE -> {
				return false;
			}
		}

		active = type;
		cooldownTicks = BASE_COOLDOWN_TICKS + RND.nextInt(JITTER_TICKS * 2) - JITTER_TICKS;
		syncAll(server);
		CreateTreeMod.LOGGER.info("World event started: {} ({} ticks)", type, remainingTicks);
		return true;
	}

	private static void end(MinecraftServer server) {
		CreateTreeMod.LOGGER.info("World event ended: {}", active);
		active = Type.NONE;
		remainingTicks = 0;
		goldenBranch = null;
		discountPrices.clear();
		syncAll(server);
	}

	private static void syncAll(MinecraftServer server) {
		EventSyncPayload payload = buildPayload();
		for (ServerPlayer p : server.getPlayerList().getPlayers())
			ModNetwork.sendToPlayer(p, payload);
	}

	public static void syncTo(ServerPlayer player) {
		ModNetwork.sendToPlayer(player, buildPayload());
	}

	private static EventSyncPayload buildPayload() {
		return new EventSyncPayload(
			active.ordinal(), secondsLeft(), goldenBranch() == null ? "" : goldenBranch(),
			new ArrayList<>(discountPrices.keySet()),
			discountPrices.values().stream().mapToInt(Integer::intValue).toArray());
	}

	public static boolean adminStart(MinecraftServer server, String name, String branch) {
		if (active != Type.NONE)
			return false;
		Type type;
		try {
			type = Type.valueOf(name.toUpperCase(java.util.Locale.ROOT));
		} catch (Exception e) {
			return false;
		}
		if (type == Type.NONE)
			return false;
		return start(server, type, type == Type.GOLDEN_PLANET ? branch : null);
	}

	public static boolean adminStop(MinecraftServer server) {
		if (active == Type.NONE)
			return false;
		end(server);
		return true;
	}
}
