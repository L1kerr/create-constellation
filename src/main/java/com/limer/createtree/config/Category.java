package com.limer.createtree.config;

public enum Category {

	LIGHT("light", 1, 2),
	MEDIUM("medium", 2, 5),
	COMPLEX("complex", 3, 10);

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

	public int defaultCost() {
		return defaultCost;
	}

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
