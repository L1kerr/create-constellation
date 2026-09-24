package com.limer.createtree.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.limer.createtree.config.SkillEntry;

public final class TreeLayout {

	private TreeLayout() {
	}

	public static final String AERO_BRANCH = "aeronautics";

	public static final float RING_STEP = 104f;

	public static final float PLANET_STEP = 52f;

	private static int sunCapacity(int ring) {
		return 6 * ring;
	}

	private static int planetCapacity(int ring) {
		return 5 * ring;
	}

	public static Layout compute(List<SkillEntry> ordered) {
		int total = ordered.size();
		Layout out = new Layout();
		out.x = new float[total];
		out.y = new float[total];
		out.parents = new int[total][];
		if (total == 0)
			return out;

		Map<String, List<Integer>> branches = new LinkedHashMap<>();
		for (int i = 0; i < total; i++)
			branches.computeIfAbsent(ordered.get(i).branch(), b -> new ArrayList<>()).add(i);

		List<Integer> main = branches.remove(SkillEntry.MAIN_BRANCH);
		int mainRings = 0;
		if (main != null && !main.isEmpty())
			mainRings = layoutRings(main, 0, 0, RING_STEP, TreeLayout::sunCapacity, -1, out);

		List<Integer> aero = branches.remove(AERO_BRANCH);
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

		if (main != null)
			for (int idx : main) {
				out.relX[idx] = out.x[idx];
				out.relY[idx] = out.y[idx];
			}

		float sunEdge = (mainRings + 1) * RING_STEP;
		float maxOrbit = sunEdge;
		for (int k = 0; k < p; k++) {
			List<Integer> entries = branches.get(planetIds.get(k));
			out.planetCounts[k] = entries.size();
			out.planetOrbitR[k] = sunEdge + 220f + k * 150f;
			out.planetBaseAngle[k] = (float) (2 * Math.PI * k / Math.max(1, p) - Math.PI / 2);
			out.planetSpeed[k] = (float) (2 * Math.PI / (90_000L + k * 45_000L));
			out.planetRings[k] = layoutRings(entries, 0, 0, PLANET_STEP, TreeLayout::planetCapacity, -1, out);
			for (int idx : entries) {
				out.entryPlanet[idx] = k;
				out.relX[idx] = out.x[idx];
				out.relY[idx] = out.y[idx];
			}
			maxOrbit = Math.max(maxOrbit, out.planetOrbitR[k]);
		}
		out.mainRings = mainRings;

		out.aeroPresent = aero != null && !aero.isEmpty();
		if (out.aeroPresent) {
			out.aeroCX = maxOrbit + 560f;
			out.aeroRings = layoutRings(aero, 0, 0, RING_STEP, TreeLayout::sunCapacity, -2, out);
			for (int idx : aero) {
				out.entryPlanet[idx] = -2;
				out.relX[idx] = out.x[idx];
				out.relY[idx] = out.y[idx];
			}
		}
		return out;
	}

	private interface CapacityFn {
		int capacity(int ring);
	}

	private static int layoutRings(List<Integer> entries, float cx, float cy,
								   float step, CapacityFn cap, int rootId, Layout out) {
		int n = entries.size();
		if (n == 0)
			return 0;

		List<int[]> ringRanges = new ArrayList<>();
		int ring = 1;
		int placed = 0;
		while (placed < n) {
			int count = Math.min(cap.capacity(ring), n - placed);
			ringRanges.add(new int[] { placed, count });
			placed += count;
			ring++;
		}

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
					out.parents[idx] = new int[] { rootId };
					continue;
				}

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

	public static float planetX(Layout l, int k, long now) {
		double a = l.planetBaseAngle[k] + (double) l.planetSpeed[k] * now;
		return (float) (l.planetOrbitR[k] * Math.cos(a));
	}

	public static float planetY(Layout l, int k, long now) {
		double a = l.planetBaseAngle[k] + (double) l.planetSpeed[k] * now;
		return (float) (l.planetOrbitR[k] * Math.sin(a));
	}

	public static final class Layout {
		public float[] x = new float[0];
		public float[] y = new float[0];

		public int[][] parents = new int[0][];
		public int mainRings = 0;
		public String[] planetIds = new String[0];

		public float[] planetOrbitR = new float[0];

		public float[] planetSpeed = new float[0];

		public float[] planetBaseAngle = new float[0];
		public int[] planetRings = new int[0];
		public int[] planetCounts = new int[0];

		public int[] entryPlanet = new int[0];

		public float[] relX = new float[0];
		public float[] relY = new float[0];

		public boolean aeroPresent = false;
		public float aeroCX = 0;
		public float aeroCY = 0;
		public int aeroRings = 0;
	}
}
