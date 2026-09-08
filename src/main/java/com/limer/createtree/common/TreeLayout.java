package com.limer.createtree.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.limer.createtree.config.SkillEntry;

/**
 * Deterministic solar-system layout shared by server and client.
 *
 * The main branch ("create") is the Sun: its entries form radial rings around the origin
 * (ring 1 = 6 nodes, ring r = 6r nodes) - the constellation mesh.
 *
 * Every other branch is a planet orbiting the Sun. Planet centers sit on one big orbit
 * circle; a planet's entries form their own smaller rings around the planet center.
 *
 * Parents:
 *  - ring 1 (Sun or planet) -> root (index -1, the Sun core): every branch hangs on the Sun
 *  - ring r >= 2            -> two angularly closest nodes of the previous ring (branch-local)
 *
 * Entry order is the config order; planets are ordered by first appearance in the config.
 */
public final class TreeLayout {

	private TreeLayout() {
	}

	/** Distance between rings of the Sun (main branch), world units. */
	public static final float RING_STEP = 104f;
	/** Distance between rings of a planet. */
	public static final float PLANET_STEP = 52f;

	private static int sunCapacity(int ring) {
		return 6 * ring;
	}

	private static int planetCapacity(int ring) {
		return 5 * ring;
	}

	/** Full computed layout for an ordered entry list. */
	public static Layout compute(List<SkillEntry> ordered) {
		int total = ordered.size();
		Layout out = new Layout();
		out.x = new float[total];
		out.y = new float[total];
		out.parents = new int[total][];
		if (total == 0)
			return out;

		// group entry indices by branch, preserving config order
		Map<String, List<Integer>> branches = new LinkedHashMap<>();
		for (int i = 0; i < total; i++)
			branches.computeIfAbsent(ordered.get(i).branch(), b -> new ArrayList<>()).add(i);

		// --- Sun: main branch rings around the origin
		List<Integer> main = branches.remove(SkillEntry.MAIN_BRANCH);
		int mainRings = 0;
		if (main != null && !main.isEmpty())
			mainRings = layoutRings(main, 0, 0, RING_STEP, TreeLayout::sunCapacity, out);

		// --- Planets: remaining branches in first-appearance order.
		// Each planet gets its OWN orbit radius and angular speed; positions rotate over time.
		List<String> planetIds = new ArrayList<>(branches.keySet());
		int p = planetIds.size();
		out.planetIds = planetIds.toArray(new String[0]);
		out.planetOrbitR = new float[p];
		out.planetSpeed = new float[p];
		out.planetBaseAngle = new float[p];
		out.planetRings = new int[p];
		out.planetCounts = new int[p];
		out.entryPlanet = new int[total];
		java.util.Arrays.fill(out.entryPlanet, -1);
		out.relX = new float[total];
		out.relY = new float[total];

		// Sun entries: relative coords are absolute (center is the origin)
		if (main != null)
			for (int idx : main) {
				out.relX[idx] = out.x[idx];
				out.relY[idx] = out.y[idx];
			}

		float sunEdge = (mainRings + 1) * RING_STEP;
		for (int k = 0; k < p; k++) {
			List<Integer> entries = branches.get(planetIds.get(k));
			out.planetCounts[k] = entries.size();
			out.planetOrbitR[k] = sunEdge + 220f + k * 150f; // own orbit per planet
			out.planetBaseAngle[k] = (float) (2 * Math.PI * k / Math.max(1, p) - Math.PI / 2);
			out.planetSpeed[k] = (float) (2 * Math.PI / ((90_000L + k * 45_000L))); // rad per ms, slower outward
			// local rings around the planet center (0,0); absolute position computed per frame
			out.planetRings[k] = layoutRings(entries, 0, 0, PLANET_STEP, TreeLayout::planetCapacity, out);
			for (int idx : entries) {
				out.entryPlanet[idx] = k;
				out.relX[idx] = out.x[idx]; // x/y currently hold planet-local coords
				out.relY[idx] = out.y[idx];
			}
		}
		out.mainRings = mainRings;
		return out;
	}

	/** Planet center position at time `now` (ms). */
	public static float planetX(Layout l, int k, long now) {
		float a = l.planetBaseAngle[k] + l.planetSpeed[k] * now;
		return (float) (l.planetOrbitR[k] * Math.cos(a));
	}

	public static float planetY(Layout l, int k, long now) {
		float a = l.planetBaseAngle[k] + l.planetSpeed[k] * now;
		return (float) (l.planetOrbitR[k] * Math.sin(a));
	}

	private interface CapacityFn {
		int capacity(int ring);
	}

	/**
	 * Places a branch's entries on rings around (cx, cy) and fills parents.
	 * Returns the number of rings used.
	 */
	private static int layoutRings(List<Integer> entries, float cx, float cy,
								   float step, CapacityFn cap, Layout out) {
		int n = entries.size();
		if (n == 0)
			return 0;

		// split entries into rings: {startInBranch, count}
		List<int[]> ringRanges = new ArrayList<>();
		int ring = 1;
		int placed = 0;
		while (placed < n) {
			int count = Math.min(cap.capacity(ring), n - placed);
			ringRanges.add(new int[] { placed, count });
			placed += count;
			ring++;
		}

		// angles of nodes per ring (branch-local, radians)
		List<double[]> ringAngles = new ArrayList<>(ringRanges.size());
		for (int r = 0; r < ringRanges.size(); r++) {
			int ringNo = r + 1;
			int count = ringRanges.get(r)[1];
			float offset = ringNo * 0.35f;
			double[] angles = new double[count];
			for (int j = 0; j < count; j++)
				angles[j] = offset + 2 * Math.PI * j / count;
			ringAngles.add(angles);

			float rad = step * ringNo;
			for (int j = 0; j < count; j++) {
				int idx = entries.get(ringRanges.get(r)[0] + j);
				out.x[idx] = cx + (float) (rad * Math.cos(angles[j]));
				out.y[idx] = cy + (float) (rad * Math.sin(angles[j]));

				if (ringNo == 1) {
					out.parents[idx] = new int[] { -1 }; // hangs on the Sun core
					continue;
				}
				// two angularly closest nodes of the previous ring
				int pCount = ringRanges.get(r - 1)[1];
				int pStart = ringRanges.get(r - 1)[0];
				double[] pAngles = ringAngles.get(r - 1);
				final double myAngle = angles[j];
				List<Integer> order = new ArrayList<>(pCount);
				for (int q = 0; q < pCount; q++)
					order.add(q);
				order.sort(Comparator.comparingDouble(q -> angleDist(myAngle, pAngles[q])));
				if (pCount == 1)
					out.parents[idx] = new int[] { entries.get(pStart + order.get(0)) };
				else
					out.parents[idx] = new int[] { entries.get(pStart + order.get(0)), entries.get(pStart + order.get(1)) };
			}
		}
		return ringRanges.size();
	}

	private static double angleDist(double a, double b) {
		double d = Math.abs(a - b) % (2 * Math.PI);
		return d > Math.PI ? 2 * Math.PI - d : d;
	}

	/** Result of a layout computation. */
	public static final class Layout {
		public float[] x = new float[0];
		public float[] y = new float[0];
		/** parents per entry: entry indices, or -1 for the Sun core (root). */
		public int[][] parents = new int[0][];
		public int mainRings = 0;
		public String[] planetIds = new String[0];
		/** own orbit radius per planet */
		public float[] planetOrbitR = new float[0];
		/** angular speed per planet, radians per ms */
		public float[] planetSpeed = new float[0];
		/** starting angle per planet */
		public float[] planetBaseAngle = new float[0];
		public int[] planetRings = new int[0];
		public int[] planetCounts = new int[0];
		/** planet index per entry, -1 for Sun entries */
		public int[] entryPlanet = new int[0];
		/** entry position relative to its branch center (Sun: absolute; planet: local) */
		public float[] relX = new float[0];
		public float[] relY = new float[0];
	}
}
