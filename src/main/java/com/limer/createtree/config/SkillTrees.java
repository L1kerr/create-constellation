package com.limer.createtree.config;

import com.limer.createtree.CreateTreeMod;

public final class SkillTrees {

	private SkillTrees() {
	}

	private static volatile SkillTreeConfig INSTANCE = SkillTreeConfig.empty();

	public static SkillTreeConfig get() {
		return INSTANCE;
	}

	public static void set(SkillTreeConfig config) {
		INSTANCE = config == null ? SkillTreeConfig.empty() : config;
		CreateTreeMod.LOGGER.info("Skill tree loaded: {} gated items, {} EXP per point, {} starting points",
			INSTANCE.allEntries().size(), INSTANCE.expPerPoint(), INSTANCE.startingPoints());
	}
}
