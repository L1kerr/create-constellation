package com.limer.createtree.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.limer.createtree.common.TreeLayout;
import com.limer.createtree.config.Category;
import com.limer.createtree.config.SkillEntry;
import com.limer.createtree.net.ClientData;
import com.limer.createtree.net.ModNetwork;
import com.limer.createtree.net.UnlockRequestPayload;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Progression "constellation" screen (key: I), performance-optimized build.
 *
 * Per-frame budget (instead of tens of thousands of 1px fills):
 *  - links are single rotated quads (1 fill each, PoseStack rotation)
 *  - medallion discs use cached per-radius horizontal span tables
 *  - orbit rings use cached unit-circle point tables, drawn dashed
 *  - world positions, parents and per-frame node states are precomputed
 * Blur is impossible: all background render methods are overridden to no-ops.
 */
public class SkillTreeScreen extends Screen {

	private static final int NODE_R = 17;
	private static final int ICON = 16;
	private static final int STAR_COUNT = 150;

	private static final int SKY_TOP = 0xFF0B1026;
	private static final int SKY_BOT = 0xFF05070F;
	private static final int STAR = 0xFFDCE6FF;
	private static final int LINK_OPEN = 0xFFE7C36A;
	private static final int LINK_LOCK = 0x70404A66;
	private static final int MEDALLION = 0xFF141A2E;
	private static final int RIM_UNLOCKED = 0xFFE7C36A;
	private static final int RIM_WAIT = 0xFF4A5066;
	private static final int RIM_POOR = 0xFF8A4A4A;

	// geometry caches keyed by integer radius
	private static final Map<Integer, int[]> DISC_CACHE = new HashMap<>();
	private static final Map<Integer, int[][]> RING_CACHE = new HashMap<>();

	private final List<Node> nodes = new ArrayList<>();
	private final List<float[]> stars = new ArrayList<>();
	private final List<Spark> sparks = new ArrayList<>();
	private final ItemStack rootIcon = new ItemStack(Items.NETHER_STAR);

	// precomputed layout; slot 0 = root, node i lives at slot i+1
	private TreeLayout.Layout layout = new TreeLayout.Layout();
	private float[] worldX = new float[1];
	private float[] worldY = new float[1];
	private int[][] parents = new int[0][];
	/** planet centers, recomputed per frame (planets orbit the Sun) */
	private float[] planetCX = new float[0];
	private float[] planetCY = new float[0];
	private Component[] planetNames = new Component[0];

	// per-frame caches
	private float[] scrX = new float[1];
	private float[] scrY = new float[1];
	private boolean[] stUnlocked = new boolean[0];
	private boolean[] stParentsOk = new boolean[0];
	private boolean[] stAffordable = new boolean[0];

	private float panX, panY;
	private float zoom = 1f;
	private boolean centered = false;
	private double dragStartX, dragStartY, dragPanX, dragPanY, moved;
	private boolean dragging = false;
	private Set<ResourceLocation> lastUnlocked = null;

	// camera focus: -2 = free, -1 = Sun (overview), k >= 0 = follow planet k
	private int focusK = -2;
	private long focusStartMs = 0;
	private float focusFromX, focusFromY, focusFromZoom;
	private static final long FOCUS_MS = 900; // smooth flight duration
	private static final float PLANET_FOCUS_ZOOM = 1.5f;

	// planet-only view when zoomed far out
	private static final float PLANET_VIEW_ZOOM = 0.45f;
	private boolean planetView = false;

	// smooth open/close animation
	private static final long OPEN_MS = 450;
	private static final long CLOSE_MS = 260;
	private boolean opening = true;
	private long openStartMs = -1;
	private boolean closing = false;
	private long closeStartMs = 0;

	public SkillTreeScreen() {
		super(Component.translatable("screen.createtree.title"));
	}

	@Override
	protected void init() {
		super.init();
		rebuild();
		if (stars.isEmpty())
			seedStars();
	}

	private void rebuild() {
		nodes.clear();
		// config order == server order (LinkedHashMap order of the tree)
		List<SkillEntry> ordered = List.copyOf(ClientData.tree().values());
		for (SkillEntry e : ordered) {
			net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(e.item());
			nodes.add(new Node(e, item == null ? ItemStack.EMPTY : new ItemStack(item)));
		}
		int total = nodes.size();

		layout = TreeLayout.compute(ordered);
		planetCX = new float[layout.planetIds.length];
		planetCY = new float[layout.planetIds.length];
		planetNames = new Component[layout.planetIds.length];
		for (int k = 0; k < planetNames.length; k++)
			planetNames[k] = Component.translatable("branch.createtree." + layout.planetIds[k]);

		// slot 0 = Sun core (root at origin); node i lives at slot i+1
		worldX = new float[total + 1];
		worldY = new float[total + 1];
		scrX = new float[total + 1];
		scrY = new float[total + 1];

		// parents once; shift entry indices into slot space (root = slot 0)
		parents = new int[total][];
		for (int i = 0; i < total; i++) {
			int[] src = layout.parents[i];
			parents[i] = new int[src == null ? 0 : src.length];
			for (int k = 0; k < parents[i].length; k++)
				parents[i][k] = src[k] + 1; // -1 (Sun root) -> 0; entry j -> j+1
		}

		stUnlocked = new boolean[total];
		stParentsOk = new boolean[total];
		stAffordable = new boolean[total];
	}

	private void seedStars() {
		// normalized screen-space positions (0..1); parallax is applied at draw time
		Random rnd = new Random(20260908L);
		for (int i = 0; i < STAR_COUNT; i++)
			stars.add(new float[] { rnd.nextFloat(), rnd.nextFloat(), 0.5f + rnd.nextFloat() * 1.4f, rnd.nextFloat() });
	}

	// ------------------------------------------------------------ transforms

	private float sx(float wxv) {
		return wxv * zoom + panX;
	}

	private float sy(float wyv) {
		return wyv * zoom + panY;
	}

	private void centerView() {
		panX = width / 2f;
		panY = height / 2f;
	}

	// ------------------------------------------------------------ blur off

	@Override
	public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	protected void renderBlurredBackground(float partialTick) {
	}

	@Override
	public void renderTransparentBackground(GuiGraphics gui) {
	}

	// ------------------------------------------------------------ render

	@Override
	public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
		long now = Util.getMillis();
		if (openStartMs < 0)
			openStartMs = now;

		// open/close fade factor: 0..1
		float fade;
		if (closing) {
			fade = 1f - Mth.clamp((now - closeStartMs) / (float) CLOSE_MS, 0f, 1f);
		} else {
			float t = Mth.clamp((now - openStartMs) / (float) OPEN_MS, 0f, 1f);
			fade = 1 - (1 - t) * (1 - t); // ease-out
		}

		gui.fillGradient(0, 0, width, height, SKY_TOP, SKY_BOT);
		if (!centered) {
			centerView();
			centered = true;
		}

		updateFrameState();
		applyFocus(now);
		planetView = zoom < PLANET_VIEW_ZOOM && focusK < 0;

		drawStars(gui, now);

		int hovered = -2; // -2 none, -1 root, >=0 node index
		if (planetView) {
			// far view: orbit tracks, the Sun and planets with names - no nodes, links or sun mesh
			drawOrbitTracks(gui);
			drawSun(gui, now);
			drawPlanetsOnly(gui);
			int k = planetAt(mouseX, mouseY);
			if (k >= 0)
				gui.renderTooltip(font, Component.translatable("branch.createtree." + layout.planetIds[k]), mouseX, mouseY);
		} else {
			drawOrbits(gui);
			drawBodies(gui, now);
			drawLinks(gui);

			for (int slot = 0; slot <= nodes.size(); slot++) {
				boolean hov = overSlot(mouseX, mouseY, slot);
				drawMedallion(gui, slot, hov, now);
				if (hov)
					hovered = slot - 1;
			}

			drawSparks(gui, now);
		}

		drawHud(gui);
		super.render(gui, mouseX, mouseY, partialTick);

		// tooltips above widgets, hidden during the fade
		if (!planetView && fade >= 0.999f && !closing) {
			if (hovered == -1)
				gui.renderTooltip(font, Component.translatable("screen.createtree.root"), mouseX, mouseY);
			else if (hovered >= 0)
				renderNodeTooltip(gui, nodes.get(hovered), hovered, mouseX, mouseY);
		}

		// fade layer on top of everything (open/close animation)
		if (fade < 1f) {
			int a = (int) ((1f - fade) * 255) << 24;
			gui.fill(0, 0, width, height, a | 0x030509);
		}

		if (closing && now - closeStartMs >= CLOSE_MS) {
			closing = false;
			super.onClose();
			return;
		}

		detectUnlocks();
	}

	/** Planet body under the cursor (planet-only view), -1 = none. */
	private int planetAt(double mx, double my) {
		for (int k = 0; k < layout.planetIds.length; k++) {
			float dx = (float) (mx - sx(planetCX[k]));
			float dy = (float) (my - sy(planetCY[k]));
			float r = Math.max(26 * zoom, 10);
			if (dx * dx + dy * dy <= r * r)
				return k;
		}
		return -1;
	}

	/** Recompute planet centers, world/screen positions and node states once per frame. */
	private void updateFrameState() {
		int total = nodes.size();
		long now = Util.getMillis();

		// planets orbit the Sun: center positions rotate over time
		for (int k = 0; k < layout.planetIds.length; k++) {
			planetCX[k] = TreeLayout.planetX(layout, k, now);
			planetCY[k] = TreeLayout.planetY(layout, k, now);
		}
		worldX[0] = 0;
		worldY[0] = 0;
		for (int i = 0; i < total; i++) {
			int k = layout.entryPlanet[i];
			if (k < 0) {
				worldX[i + 1] = layout.relX[i];
				worldY[i + 1] = layout.relY[i];
			} else {
				worldX[i + 1] = planetCX[k] + layout.relX[i];
				worldY[i + 1] = planetCY[k] + layout.relY[i];
			}
		}
		// NOTE: screen coords are computed in applyFocus AFTER the camera moves this frame

		int points = ClientData.points();
		for (int i = 0; i < total; i++) {
			stUnlocked[i] = ClientData.isUnlocked(nodes.get(i).entry.item());
			stAffordable[i] = points >= nodes.get(i).entry.cost();
			boolean ok = false;
			for (int p : parents[i])
				if (p == 0 || stUnlocked[p - 1]) {
					ok = true;
					break;
				}
			stParentsOk[i] = ok;
		}
	}

	/**
	 * Camera: smooth flight to the focused body, then follow it while it orbits.
	 * focusK: -2 free (user pans), -1 Sun, k >= 0 planet k.
	 */
	private void applyFocus(long now) {
		if (focusK >= -1) {
			float tx, ty;
			float targetZoom;
			if (focusK == -1) {
				tx = 0;
				ty = 0;
				targetZoom = 1f;
			} else {
				tx = planetCX[focusK];
				ty = planetCY[focusK];
				targetZoom = PLANET_FOCUS_ZOOM;
			}
			float t = Mth.clamp((now - focusStartMs) / (float) FOCUS_MS, 0f, 1f);
			float e = 1 - (1 - t) * (1 - t) * (1 - t); // ease-out cubic
			zoom = focusFromZoom + (targetZoom - focusFromZoom) * e;
			panX = focusFromX + (width / 2f - tx * zoom - focusFromX) * e;
			panY = focusFromY + (height / 2f - ty * zoom - focusFromY) * e;
			// after the flight completes, keep the planet centered as it moves along its orbit
			if (t >= 1f) {
				zoom = targetZoom;
				panX = width / 2f - tx * zoom;
				panY = height / 2f - ty * zoom;
			}
		}
		for (int slot = 0; slot < scrX.length; slot++) {
			scrX[slot] = sx(worldX[slot]);
			scrY[slot] = sy(worldY[slot]);
		}
	}

	private void setFocus(int k) {
		focusK = k;
		focusStartMs = Util.getMillis();
		focusFromX = panX;
		focusFromY = panY;
		focusFromZoom = zoom;
	}

	/** Body under the cursor: -2 none, -1 Sun core, k >= 0 planet body. */
	private int bodyAt(double mx, double my) {
		for (int k = 0; k < layout.planetIds.length; k++) {
			float dx = (float) (mx - sx(planetCX[k]));
			float dy = (float) (my - sy(planetCY[k]));
			float r = 24 * zoom;
			if (dx * dx + dy * dy <= r * r)
				return k;
		}
		float dx = (float) (mx - scrX[0]);
		float dy = (float) (my - scrY[0]);
		float r = 34 * zoom;
		if (dx * dx + dy * dy <= r * r)
			return -1;
		return -2;
	}

	private void detectUnlocks() {
		Set<ResourceLocation> now = ClientData.unlocked();
		if (lastUnlocked == null) {
			lastUnlocked = new HashSet<>(now);
			return;
		}
		if (now.size() == lastUnlocked.size())
			return; // quiet frame fast path, no allocation
		for (int i = 0; i < nodes.size(); i++) {
			ResourceLocation id = nodes.get(i).entry.item();
			if (now.contains(id) && !lastUnlocked.contains(id)) {
				playSound(SoundEvents.PLAYER_LEVELUP, 1.4f);
				burst(i);
			}
		}
		lastUnlocked = new HashSet<>(now);
	}

	private void drawStars(GuiGraphics gui, long now) {
		float phase = now / 700f;
		// slow parallax: stars drift with panning, wrapping around the screen edges
		float ox = -panX * 0.15f;
		float oy = -panY * 0.15f;
		for (float[] s : stars) {
			int x = (int) Mth.positiveModulo(s[0] * width + ox, width);
			int y = (int) Mth.positiveModulo(s[1] * height + oy, height);
			float tw = 0.5f + 0.5f * Mth.sin(phase + s[3] * 12f);
			int size = Math.max(1, Math.round(s[2] * (0.7f + 0.3f * tw)));
			int color = ((int) (120 + 135 * tw) << 24) | (STAR & 0x00FFFFFF);
			gui.fill(x, y, x + size, y + size, color);
		}
	}

	private void drawOrbits(GuiGraphics gui) {
		if (nodes.isEmpty())
			return;
		int cx = Math.round(scrX[0]);
		int cy = Math.round(scrY[0]);

		// Sun rings (main branch)
		for (int r = 1; r <= layout.mainRings; r++) {
			int rad = Math.round(TreeLayout.RING_STEP * r * zoom);
			if (rad < 8)
				continue;
			drawDashedCircle(gui, cx, cy, rad, 0xFF2A3350);
		}

		// every planet: its OWN orbit path around the Sun (solid, visible) + local rings + name
		for (int k = 0; k < layout.planetIds.length; k++) {
			int orbitR = Math.round(layout.planetOrbitR[k] * zoom);
			if (orbitR >= 8) {
				// bright 2px orbit track, tinted with the planet's own color; gold when focused
				int track = focusK == k ? 0xE0FFD24E : ((PLANET_PALETTE[k % PLANET_PALETTE.length] & 0x00FFFFFF) | 0xA0000000);
				drawSolidCircle(gui, cx, cy, orbitR, track);
				drawSolidCircle(gui, cx, cy, orbitR + 1, track & 0x60FFFFFF);
			}

			int px = Math.round(sx(planetCX[k]));
			int py = Math.round(sy(planetCY[k]));
			for (int r = 1; r <= layout.planetRings[k]; r++) {
				int rad = Math.round(TreeLayout.PLANET_STEP * r * zoom);
				if (rad < 8)
					continue;
				drawDashedCircle(gui, px, py, rad, 0xFF3A4568);
			}
		}
	}

	/**
	 * The Sun: warm glow + body, drawn procedurally.
	 * Drawn in BOTH normal and planet-only views. When zoomed far out the Sun keeps a
	 * healthy minimum size (it is the anchor of the whole system, not a node).
	 */
	private void drawSun(GuiGraphics gui, long now) {
		int cx = Math.round(scrX[0]);
		int cy = Math.round(scrY[0]);
		float pulse = 0.5f + 0.5f * Mth.sin(now / 900f);
		// non-linear: full size while zoomed in, floor of 14px when far out
		int sunR = zoom >= 0.5f ? Math.round(30 * zoom) : Math.round(7 + 14 * zoom);
		sunR = Math.max(14, sunR);
		fillDisc(gui, cx, cy, sunR + Math.round(14 * zoom), 0x14FFB040);
		fillDisc(gui, cx, cy, sunR + Math.round(7 * zoom), 0x22FFC050);
		fillDisc(gui, cx, cy, sunR, lerpColor(0xFFFFA030, 0xFFFFC060, pulse));
		if (sunR >= 6)
			fillDisc(gui, cx - Math.round(sunR * 0.25f), cy - Math.round(sunR * 0.25f),
				Math.max(2, Math.round(sunR * 0.45f)), 0x50FFE0A0);
	}

	/** Planet and Sun bodies: colored spheres with atmosphere glow, highlight and names. */
	private void drawBodies(GuiGraphics gui, long now) {
		// --- Sun core (drawn under the root medallion)
		drawSun(gui, now);

		// --- planets
		for (int k = 0; k < layout.planetIds.length; k++) {
			int px = Math.round(sx(planetCX[k]));
			int py = Math.round(sy(planetCY[k]));
			int pr = Math.round(20 * zoom);
			if (pr < 2)
				continue;
			if (px + pr * 2 < 0 || py + pr * 2 < 0 || px - pr * 2 > width || py - pr * 2 > height)
				continue;

			int base = PLANET_PALETTE[k % PLANET_PALETTE.length];
			// atmosphere glow
			fillDisc(gui, px, py, pr + Math.round(6 * zoom), (base & 0x00FFFFFF) | 0x18000000);
			// body: dark limb then lit face
			fillDisc(gui, px, py, pr, darken(base, 0.45f));
			fillDisc(gui, px - Math.round(pr * 0.18f), py - Math.round(pr * 0.18f),
				Math.round(pr * 0.82f), base);
			// specular highlight toward the Sun
			int hx = px - Math.round(pr * 0.35f * Math.signum(planetCX[k] == 0 ? 1 : planetCX[k]));
			int hy = py - Math.round(pr * 0.35f);
			fillDisc(gui, hx, hy, Math.max(2, Math.round(pr * 0.3f)), 0x60FFFFFF);
			// focus marker
			if (focusK == k)
				drawSolidCircle(gui, px, py, pr + Math.round(9 * zoom), 0xC0FFE9A0);

			// name below the body
			if (zoom >= 0.5f) {
				gui.drawCenteredString(font, planetNames[k], px, py + pr + Math.round(10 * zoom),
					focusK == k ? 0xFFE9A0 : 0xC8CCDD);
			}
		}
	}

	private static final int[] PLANET_PALETTE = {
		0xFF4A90D9, 0xFFD9834A, 0xFF6AB04A, 0xFFB04AD9, 0xFFD94A6A, 0xFF4AD9C8
	};

	/** Far view: just the orbit tracks around the Sun. */
	private void drawOrbitTracks(GuiGraphics gui) {
		int cx = Math.round(scrX[0]);
		int cy = Math.round(scrY[0]);
		for (int k = 0; k < layout.planetIds.length; k++) {
			int orbitR = Math.round(layout.planetOrbitR[k] * zoom);
			if (orbitR < 4)
				continue;
			int track = focusK == k ? 0xE0FFD24E : ((PLANET_PALETTE[k % PLANET_PALETTE.length] & 0x00FFFFFF) | 0x80000000);
			drawSolidCircle(gui, cx, cy, orbitR, track);
		}
	}

	/** Far view: planets (min size so they stay visible) + names, no local node rings. */
	private void drawPlanetsOnly(GuiGraphics gui) {
		for (int k = 0; k < layout.planetIds.length; k++) {
			int px = Math.round(sx(planetCX[k]));
			int py = Math.round(sy(planetCY[k]));
			int pr = Math.max(4, Math.round(20 * zoom)); // never smaller than 4px
			if (px + pr * 2 < 0 || py + pr * 2 < 0 || px - pr * 2 > width || py - pr * 2 > height)
				continue;
			int base = PLANET_PALETTE[k % PLANET_PALETTE.length];
			fillDisc(gui, px, py, pr + 2, (base & 0x00FFFFFF) | 0x18000000);
			fillDisc(gui, px, py, pr, base);
			fillDisc(gui, px - pr / 3, py - pr / 3, Math.max(1, pr / 2), 0x40FFFFFF);
			if (focusK == k)
				drawSolidCircle(gui, px, py, pr + 4, 0xC0FFE9A0);
			gui.drawCenteredString(font, planetNames[k], px, py + pr + 4, focusK == k ? 0xFFE9A0 : 0xC8CCDD);
		}
	}

	private void drawDashedCircle(GuiGraphics gui, int cx, int cy, int rad, int color) {
		// cull whole circle when off-screen
		if (cx + rad < 0 || cy + rad < 0 || cx - rad > width || cy - rad > height)
			return;
		int[][] pts = ringPoints(rad);
		for (int i = 0; i < pts.length; i += 4) {
			// per-point screen test: big circles are mostly off-screen while panning
			int x = cx + pts[i][0], y = cy + pts[i][1];
			if (x < 0 || y < 0 || x >= width || y >= height)
				continue;
			gui.fill(x, y, x + 1, y + 1, color);
		}
	}

	private void drawSolidCircle(GuiGraphics gui, int cx, int cy, int rad, int color) {
		if (cx + rad < 0 || cy + rad < 0 || cx - rad > width || cy - rad > height)
			return;
		int[][] pts = ringPoints(rad);
		for (int[] p : pts) {
			int x = cx + p[0], y = cy + p[1];
			if (x < 0 || y < 0 || x >= width || y >= height)
				continue;
			gui.fill(x, y, x + 1, y + 1, color);
		}
	}

	private void drawLinks(GuiGraphics gui) {
		PoseStack pose = gui.pose();
		for (int i = 0; i < nodes.size(); i++) {
			int slot = i + 1;
			float x1 = scrX[slot], y1 = scrY[slot];
			for (int p : parents[i]) {
				// no line from the Sun to planet ring-1 nodes: planets orbit on their own
				if (p == 0 && layout.entryPlanet[i] >= 0)
					continue;
				float x0 = scrX[p], y0 = scrY[p];
				if (Math.max(x0, x1) < 0 || Math.min(x0, x1) > width || Math.max(y0, y1) < 0 || Math.min(y0, y1) > height)
					continue;
				float dx = x1 - x0, dy = y1 - y0;
				float len = Mth.sqrt(dx * dx + dy * dy);
				if (len < 1)
					continue;
				int color = (p == 0 || stUnlocked[p - 1]) ? LINK_OPEN : LINK_LOCK;
				// one rotated quad per link instead of ~100 pixel fills
				pose.pushPose();
				pose.translate(x0, y0, 0);
				pose.mulPose(Axis.ZP.rotation((float) Mth.atan2(dy, dx)));
				gui.fill(0, -1, Math.round(len) + 1, 1, color);
				pose.popPose();
			}
		}
	}

	private void drawMedallion(GuiGraphics gui, int slot, boolean hov, long now) {
		float cx = scrX[slot];
		float cy = scrY[slot];
		float rad = NODE_R * zoom;
		if (cx + rad < 0 || cy + rad < 0 || cx - rad > width || cy - rad > height)
			return;

		int i = slot - 1;
		boolean unlocked;
		boolean parentsOk;
		boolean affordable;
		ItemStack icon;
		Category category;
		int cost;
		if (i < 0) {
			unlocked = true;
			parentsOk = true;
			affordable = true;
			icon = rootIcon;
			category = null;
			cost = 0;
		} else {
			unlocked = stUnlocked[i];
			parentsOk = stParentsOk[i];
			affordable = stAffordable[i];
			icon = nodes.get(i).icon;
			category = nodes.get(i).entry.category();
			cost = nodes.get(i).entry.cost();
		}

		int rim;
		if (unlocked)
			rim = RIM_UNLOCKED;
		else if (!parentsOk)
			rim = RIM_WAIT;
		else if (affordable)
			rim = pulse(now);
		else
			rim = RIM_POOR;

		int rcx = Math.round(cx);
		int rcy = Math.round(cy);
		int rr = Math.max(4, Math.round(rad));

		// two cached-span discs: rim color then inner face -> a 2px rim ring
		fillDisc(gui, rcx, rcy, rr, rim);
		fillDisc(gui, rcx, rcy, rr - 2, MEDALLION);
		if (hov)
			fillDisc(gui, rcx, rcy, rr, 0x28FFFFFF);

		// icon + all badges share the disc's integer center (rcx/rcy) ->
		// no relative drift between texture and medallion while panning
		float iconScale = Mth.clamp(zoom, 0.55f, 2.5f);
		if (Math.abs(iconScale - 1f) < 0.01f) {
			gui.renderItem(icon, rcx - ICON / 2, rcy - ICON / 2);
		} else {
			PoseStack pose = gui.pose();
			pose.pushPose();
			// integer translate: fractional offsets make the texture swim during panning
			pose.translate(Math.round(rcx - 8 * iconScale), Math.round(rcy - 8 * iconScale), 150);
			pose.scale(iconScale, iconScale, 1);
			gui.renderItem(icon, 0, 0);
			pose.popPose();
		}

		if (unlocked && i >= 0) {
			int gx = rcx + rr - 8, gy = rcy + rr - 8;
			gui.fill(gx, gy + 2, gx + 2, gy + 4, 0xFF38B038);
			gui.fill(gx + 2, gy + 4, gx + 4, gy + 6, 0xFF38B038);
			gui.fill(gx + 4, gy, gx + 6, gy + 6, 0xFF38B038);
		} else if (!parentsOk) {
			drawPadlock(gui, rcx + Math.round(rr * 0.4f), rcy + Math.round(rr * 0.4f));
		} else if (!unlocked && zoom >= 0.8f) {
			String c = String.valueOf(cost);
			int cw = font.width(c);
			int bx = rcx + rr - cw - 2, by = rcy + rr - 9;
			gui.fill(bx - 1, by - 1, bx + cw + 1, by + 9, 0xC0000000);
			gui.drawString(font, c, bx, by, affordable ? 0xFFE070 : 0xFFB07070, true);
		}

		if (category != null && zoom >= 0.8f) {
			int dx = rcx - rr + 3, dy = rcy - rr + 3;
			gui.fill(dx, dy, dx + 3, dy + 3, categoryColor(category));
		}
	}

	private boolean overSlot(double mx, double my, int slot) {
		float dx = (float) (mx - scrX[slot]);
		float dy = (float) (my - scrY[slot]);
		float r = NODE_R * zoom + 2;
		return dx * dx + dy * dy <= r * r;
	}

	private boolean anyParentUnlocked(int i) {
		for (int p : parents[i])
			if (p == 0 || ClientData.isUnlocked(nodes.get(p - 1).entry.item()))
				return true;
		return false;
	}

	private static int lerpColor(int a, int b, float t) {
		int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return 0xFF000000
			| ((int) (ar + (br - ar) * t) << 16)
			| ((int) (ag + (bg - ag) * t) << 8)
			| (int) (ab + (bb - ab) * t);
	}

	private static int darken(int c, float f) {
		return 0xFF000000
			| ((int) (((c >> 16) & 0xFF) * f) << 16)
			| ((int) (((c >> 8) & 0xFF) * f) << 8)
			| (int) ((c & 0xFF) * f);
	}

	// ------------------------------------------------------------ cached shapes

	private static int[] discSpans(int r) {
		return DISC_CACHE.computeIfAbsent(r, rad -> {
			int[] spans = new int[rad * 2 + 1];
			for (int dy = -rad; dy <= rad; dy++)
				spans[dy + rad] = (int) Math.sqrt(Math.max(0, rad * rad + rad - dy * dy));
			return spans;
		});
	}

	private void fillDisc(GuiGraphics gui, int cx, int cy, int r, int color) {
		if (r <= 0)
			return;
		// screen cull: discs fully outside the viewport cost nothing
		if (cx + r < 0 || cy + r < 0 || cx - r > width || cy - r > height)
			return;
		int[] spans = discSpans(r);
		// rows are filled 2px tall: half the fill calls, visually identical at this size
		for (int dy = -r; dy <= r; dy += 2) {
			int hw = spans[dy + r];
			gui.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 2, color);
		}
	}

	private static int[][] ringPoints(int r) {
		return RING_CACHE.computeIfAbsent(r, rad -> {
			// enough points to keep the circle continuous: ~1 point per 2px of circumference
			int n = Mth.clamp((int) (Math.PI * rad), 48, 512);
			int[][] pts = new int[n][2];
			for (int i = 0; i < n; i++) {
				double a = 2 * Math.PI * i / n;
				pts[i][0] = (int) Math.round(Math.cos(a) * rad);
				pts[i][1] = (int) Math.round(Math.sin(a) * rad);
			}
			return pts;
		});
	}

	private void drawPadlock(GuiGraphics gui, int x, int y) {
		gui.fill(x + 1, y, x + 2, y + 3, 0xFFB0B0B8);
		gui.fill(x + 6, y, x + 7, y + 3, 0xFFB0B0B8);
		gui.fill(x + 2, y - 1, x + 6, y, 0xFFB0B0B8);
		gui.fill(x, y + 3, x + 8, y + 9, 0xFFD8D8E0);
		gui.fill(x + 3, y + 5, x + 5, y + 7, 0xFF404048);
	}

	private int pulse(long now) {
		float t = (now % 1200) / 1200f;
		float s = 0.5f + 0.5f * Mth.sin(t * Mth.TWO_PI);
		int r = (int) (200 + 55 * s);
		int g = (int) (160 + 60 * s);
		return 0xFF000000 | (r << 16) | (g << 8) | 0x40;
	}

	private void burst(int index) {
		if (index < 0)
			return;
		float x = worldX[index + 1], y = worldY[index + 1];
		Random rnd = new Random();
		for (int k = 0; k < 14; k++) {
			float a = rnd.nextFloat() * Mth.TWO_PI;
			float sp = 0.6f + rnd.nextFloat() * 1.8f;
			sparks.add(new Spark(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp, 500 + rnd.nextInt(300)));
		}
	}

	private void drawSparks(GuiGraphics gui, long now) {
		sparks.removeIf(s -> now - s.born > s.life);
		for (Spark s : sparks) {
			float age = (now - s.born) / (float) s.life;
			int px = Math.round(sx(s.x + s.vx * age * 40));
			int py = Math.round(sy(s.y + s.vy * age * 40));
			int color = ((int) (255 * (1 - age)) << 24) | 0xFFE7A3;
			gui.fill(px, py, px + 2, py + 2, color);
		}
	}

	private void drawHud(GuiGraphics gui) {
		gui.fill(0, 0, width, 30, 0xC0070B18);
		gui.drawString(font, title, 8, 5, 0xFFE7C3, true);
		gui.drawString(font, Component.translatable("screen.createtree.points", ClientData.points()), 8, 17, 0xFFE070, true);

		int exp = ClientData.exp();
		int per = Math.max(1, ClientData.expPerPoint());
		int barW = Math.min(200, width / 3);
		int barX = width / 2 - barW / 2;
		gui.fill(barX - 1, 9, barX + barW + 1, 16, 0xFF000000);
		gui.fill(barX, 10, barX + barW, 15, 0xFF141A2E);
		int fill = (int) (barW * Mth.clamp((float) exp / per, 0f, 1f));
		gui.fill(barX, 10, barX + fill, 15, 0xFF7FE7A3);
		gui.drawCenteredString(font, Component.translatable("screen.createtree.exp", exp, per), width / 2, 19, 0x9FE8B0);

		// footer hint: split into two lines when it would overflow the screen width
		Component hint = Component.translatable("screen.createtree.hint");
		if (font.width(hint) > width - 20) {
			gui.fill(0, height - 34, width, height, 0xC0070B18);
			String s = hint.getString();
			int cut = s.indexOf(" - ", s.length() / 3);
			if (cut > 0) {
				gui.drawCenteredString(font, s.substring(0, cut), width / 2, height - 31, 0x8A90A8);
				gui.drawCenteredString(font, s.substring(cut + 3), width / 2, height - 21, 0x8A90A8);
			} else {
				gui.drawCenteredString(font, s, width / 2, height - 26, 0x8A90A8);
			}
		} else {
			gui.fill(0, height - 26, width, height, 0xC0070B18);
			gui.drawCenteredString(font, hint, width / 2, height - 19, 0x8A90A8);
		}
	}

	private void renderNodeTooltip(GuiGraphics gui, Node node, int index, int mouseX, int mouseY) {
		List<Component> lines = new ArrayList<>();
		lines.add(node.icon.isEmpty() ? Component.literal(node.entry.item().toString()) : node.icon.getHoverName());
		lines.add(Component.translatable("category.createtree." + node.entry.category().jsonName())
			.withStyle(s -> s.withColor(categoryColor(node.entry.category()))));
		if (stUnlocked[index]) {
			lines.add(Component.translatable("screen.createtree.unlocked").withStyle(s -> s.withColor(0x70FF70)));
		} else if (!stParentsOk[index]) {
			lines.add(Component.translatable("screen.createtree.requires_any").withStyle(s -> s.withColor(0xFF9A9A)));
		} else {
			lines.add(Component.translatable("screen.createtree.cost", node.entry.cost(), node.entry.exp()));
			lines.add(stAffordable[index]
				? Component.translatable("screen.createtree.click_to_unlock").withStyle(s -> s.withColor(0xFFE070))
				: Component.translatable("message.createtree.not_enough_points").withStyle(s -> s.withColor(0xFF7A7A)));
		}
		gui.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
	}

	private int categoryColor(Category c) {
		return switch (c) {
			case LIGHT -> 0x9FE8A3;
			case MEDIUM -> 0xFFE070;
			case COMPLEX -> 0xFF9A9A;
		};
	}

	private void playSound(SoundEvent event, float pitch) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch));
	}

	// ------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button))
			return true;
		if (button == 0 || button == 1 || button == 2) {
			dragging = true;
			moved = 0;
			dragStartX = mouseX;
			dragStartY = mouseY;
			dragPanX = panX;
			dragPanY = panY;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (dragging) {
			if (moved >= 5)
				focusK = -2; // manual pan takes over from focus flight/follow
			panX = (float) (dragPanX + (mouseX - dragStartX));
			panY = (float) (dragPanY + (mouseY - dragStartY));
			moved = Math.max(moved, Math.hypot(mouseX - dragStartX, mouseY - dragStartY));
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging && button == 0 && moved < 5) {
			dragging = false;
			if (planetView) {
				// far view: clicking a planet flies to it
				int k = planetAt(mouseX, mouseY);
				if (k >= 0) {
					setFocus(k);
					playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.9f);
				}
				return true;
			}
			// nodes take priority over bodies
			int idx = nodeAt(mouseX, mouseY);
			if (idx >= 0) {
				Node node = nodes.get(idx);
				if (!stUnlocked[idx] && anyParentUnlocked(idx)) {
					ModNetwork.sendToServer(new UnlockRequestPayload(node.entry.item()));
					playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1f);
				} else if (!anyParentUnlocked(idx)) {
					playSound(SoundEvents.VILLAGER_NO, 0.8f);
				}
				return true;
			}
			// clicking a planet body focuses (and follows) it; the Sun returns to overview
			int body = bodyAt(mouseX, mouseY);
			if (body >= -1) {
				setFocus(body);
				playSound(SoundEvents.UI_BUTTON_CLICK.value(), body == -1 ? 1.2f : 0.9f);
				return true;
			}
			return true;
		}
		if (dragging && button == 0 && moved >= 5) {
			// manual pan breaks the follow
			dragging = false;
			focusK = -2;
			return true;
		}
		if (button == 1) {
			// right-click releases focus back to free camera
			focusK = -2;
			return true;
		}
		dragging = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private int nodeAt(double mouseX, double mouseY) {
		for (int i = nodes.size() - 1; i >= 0; i--)
			if (overSlot(mouseX, mouseY, i + 1))
				return i;
		return -1;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		// zooming breaks the follow: free camera from the current pose
		focusK = -2;
		float old = zoom;
		zoom = Mth.clamp(zoom * (scrollY > 0 ? 1.15f : 1 / 1.15f), 0.15f, 2.5f);
		panX = (float) (mouseX - (mouseX - panX) * (zoom / old));
		panY = (float) (mouseY - (mouseY - panY) * (zoom / old));
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (closing)
			return true; // swallow input during the close animation
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_C) {
			setFocus(-1); // smooth flight back to the Sun overview
			return true;
		}
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
			if (focusK >= -1) {
				focusK = -2; // first Esc frees the camera
				return true;
			}
			// second Esc: fade out, then really close
			closing = true;
			closeStartMs = Util.getMillis();
			playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		if (closing) {
			super.onClose();
			return;
		}
		// any other close path (e.g. widget) also animates
		closing = true;
		closeStartMs = Util.getMillis();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private record Node(SkillEntry entry, ItemStack icon) {
	}

	private static final class Spark {
		final float x, y, vx, vy;
		final long born;
		final int life;

		Spark(float x, float y, float vx, float vy, int life) {
			this.x = x;
			this.y = y;
			this.vx = vx;
			this.vy = vy;
			this.life = life;
			this.born = Util.getMillis();
		}
	}
}
