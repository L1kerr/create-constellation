package com.limer.createtree.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.limer.createtree.CreateTreeMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class SkillTreeLoader extends SimpleJsonResourceReloadListener {

	private static final Gson GSON = new GsonBuilder().create();

	public SkillTreeLoader() {
		super(GSON, "createtree");
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
		int expPerPoint = SkillTreeConfig.DEFAULT_EXP_PER_POINT;
		int startingPoints = SkillTreeConfig.DEFAULT_STARTING_POINTS;
		List<SkillEntry> entries = new ArrayList<>();

		for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
			try {
				if (!file.getValue().isJsonObject()) {
					CreateTreeMod.LOGGER.warn("Skill tree file {} is not a JSON object, skipping", file.getKey());
					continue;
				}
				JsonObject root = file.getValue().getAsJsonObject();

				if (root.has("exp_per_point") && root.get("exp_per_point").isJsonPrimitive())
					expPerPoint = Math.max(1, root.get("exp_per_point").getAsInt());
				if (root.has("starting_points") && root.get("starting_points").isJsonPrimitive())
					startingPoints = Math.max(0, root.get("starting_points").getAsInt());

				if (!root.has("entries") || !root.get("entries").isJsonArray())
					continue;

				for (JsonElement el : root.getAsJsonArray("entries")) {
					if (!el.isJsonObject())
						continue;
					JsonObject obj = el.getAsJsonObject();
					if (!obj.has("item") || !obj.has("category")) {
						CreateTreeMod.LOGGER.warn("Skill tree entry {} in {} is missing item/category, skipping", obj, file.getKey());
						continue;
					}
				ResourceLocation item;
				try {
					item = ResourceLocation.parse(obj.get("item").getAsString());
				} catch (Exception e) {
					CreateTreeMod.LOGGER.warn("Invalid item id '{}' in {}, skipping", obj.get("item").getAsString(), file.getKey());
					continue;
				}

				List<ResourceLocation> unlocks = new ArrayList<>();
				if (obj.has("unlocks") && obj.get("unlocks").isJsonArray()) {
					for (JsonElement u : obj.getAsJsonArray("unlocks")) {
						if (!u.isJsonPrimitive())
							continue;
						try {
							ResourceLocation uid = ResourceLocation.parse(u.getAsString());
							if (!unlocks.contains(uid))
								unlocks.add(uid);
						} catch (Exception e) {
							CreateTreeMod.LOGGER.warn("Invalid unlocks id '{}' for {} in {}", u.getAsString(), item, file.getKey());
						}
					}
				}
					Category category = Category.byJsonName(obj.get("category").getAsString());
					if (category == null) {
						CreateTreeMod.LOGGER.warn("Unknown category '{}' for item {} in {}, skipping",
							obj.get("category").getAsString(), item, file.getKey());
						continue;
					}

				if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(item)) {
					CreateTreeMod.LOGGER.debug("Skill tree: item {} is not registered, skipping", item);
					continue;
				}
				unlocks.removeIf(id -> !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id));
				int cost = obj.has("cost") ? Math.max(0, obj.get("cost").getAsInt()) : category.defaultCost();
				int exp = obj.has("exp") ? Math.max(0, obj.get("exp").getAsInt()) : category.defaultExp();
				String branch = obj.has("branch") ? obj.get("branch").getAsString() : SkillEntry.MAIN_BRANCH;
				entries.add(new SkillEntry(item, category, cost, exp, branch, unlocks));
				}
			} catch (Exception e) {
				CreateTreeMod.LOGGER.error("Failed to parse skill tree file {}", file.getKey(), e);
			}
		}

		SkillTrees.set(new SkillTreeConfig(expPerPoint, startingPoints, entries));
	}
}
