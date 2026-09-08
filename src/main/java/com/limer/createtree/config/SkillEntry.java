package com.limer.createtree.config;

import net.minecraft.resources.ResourceLocation;

/**
 * A single gated item in the skill tree.
 *
 * @param item     the result item id (namespace:path) that is gated
 * @param category light / medium / complex
 * @param cost     skill points required to unlock
 * @param exp      EXP granted each time this item is crafted
 * @param branch   branch id; "create" is the main branch (the Sun), every other
 *                 branch is a planet orbiting it. Entries keep their config order
 *                 inside a branch, which defines the branch mesh layout.
 */
public record SkillEntry(ResourceLocation item, Category category, int cost, int exp, String branch) {

	public static final String MAIN_BRANCH = "create";

	public SkillEntry {
		if (branch == null || branch.isEmpty())
			branch = MAIN_BRANCH;
	}

	public static SkillEntry defaults(ResourceLocation item, Category category) {
		return new SkillEntry(item, category, category.defaultCost(), category.defaultExp(), MAIN_BRANCH);
	}

	public boolean isValid() {
		return item != null && category != null && cost >= 0 && exp >= 0;
	}
}
