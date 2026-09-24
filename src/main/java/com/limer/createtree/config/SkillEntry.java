package com.limer.createtree.config;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

public record SkillEntry(ResourceLocation item, Category category, int cost, int exp, String branch,
						 List<ResourceLocation> unlocks) {

	public static final String MAIN_BRANCH = "create";

	public SkillEntry {
		if (branch == null || branch.isEmpty())
			branch = MAIN_BRANCH;
		if (unlocks == null)
			unlocks = List.of();
	}

	public SkillEntry(ResourceLocation item, Category category, int cost, int exp, String branch) {
		this(item, category, cost, exp, branch, List.of());
	}

	public static SkillEntry defaults(ResourceLocation item, Category category) {
		return new SkillEntry(item, category, category.defaultCost(), category.defaultExp(), MAIN_BRANCH);
	}

	public boolean isValid() {
		return item != null && category != null && cost >= 0 && exp >= 0;
	}
}
