package com.limer.createtree.config;

/**
 * The three unlock categories of the skill tree.
 * Each entry costs points to unlock and grants EXP when crafted.
 */
public enum Category {

	LIGHT("light", 1, 5),
	MEDIUM("medium", 2, 12),
	COMPLEX("complex", 3, 25);

	private final String jsonName;
	private final int defaultCost;
	private final int defaultExp;

	Category(String jsonName, int defaultCost, int defaultExp) {
		this.jsonName = jsonName;
		this.defaultCost = defaultCost;
		this.defaultExp = defaultExp;
	}

	public String jsonName() {
		return jsonName;
	}

	/** Points required to unlock an item of this category unless overridden per entry. */
	public int defaultCost() {
		return defaultCost;
	}

	/** EXP granted per craft of this category unless overridden per entry. */
	public int defaultExp() {
		return defaultExp;
	}

	public static Category byJsonName(String name) {
		if (name == null)
			return null;
		String lower = name.toLowerCase(java.util.Locale.ROOT);
		for (Category c : values())
			if (c.jsonName.equals(lower))
				return c;
		return null;
	}
}
