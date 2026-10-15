package net.minecraft.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.RealOpenGLEnums;
import net.minecraft.client.Minecraft;

/**
 * MeteorMC ClickGUI: one centered window with category tabs, a search box and
 * a grid of module cards with toggle switches.
 *
 * Click a card: toggle the module.
 * Right click a card (or click its ".." corner): open its settings.
 * Type to search. Drag the scrollbar if there are many modules.
 * Esc: back / clear search / close.
 */
public class ClickGuiScreen extends GuiScreen {

	// ===================== STYLE =====================
	private static final String TITLE = "MeteorMC";

	private static final int WIN_W = 340;
	private static final int WIN_H = 220;
	private static final int HEADER = 28;
	private static final int COLS = 3;
	private static final int GAP = 6;
	private static final int CARD_H = 44;
	private static final int SEARCH_W = 86;

	private static final int BG_TOP = 0xFF0B0A06;
	private static final int BG_BOTTOM = 0xFF2E2208;

	private static final int BORDER = 0xFF7A6422;
	private static final int BORDER_HOVER = 0xFFFFC21F;
	private static final int CARD_BORDER = 0xFF3A3322;
	private static final int CARD_TOP = 0xF0241D0D;
	private static final int CARD_BOTTOM = 0xF0171207;
	private static final int CARD_HOVER_TOP = 0xF03A2E10;
	private static final int CARD_HOVER_BOTTOM = 0xF0241C0A;

	private static final int GOLD = 0xFFFFD83A;
	private static final int TEXT = 0xFFF2EAD0;
	private static final int TEXT_DIM = 0xFF9A9070;

	private static final int TITLE_COLOR_A = 0xFFFFB020;
	private static final int TITLE_COLOR_B = 0xFFFFF8C0;
	private static final int TITLE_AURA = 0xFF6A00;

	private static final int METEOR_OUTER = 0xFFFF8A1A;
	private static final int METEOR_CORE = 0xFFFFF4A0;
	private static final int FLAME_HEAD = 0xE0FFB020;
	private static final int FLAME_TAIL = 0x20A02000;

	private static final int STAR_COUNT = 90;
	private static final int METEOR_COUNT = 4;
	// =================================================

	private static final int KEY_ESCAPE = 1;
	private static final int KEY_BACKSPACE = 14;

	/** Remembered between openings. */
	private static int selectedTab = 0;
	private static int scrollRow = 0;
	private static final Map<Object, Float> ANIM = new HashMap<Object, Float>();

	private final GuiScreen parent;
	private final List<MeteorModule.Category> tabs = new ArrayList<MeteorModule.Category>();

	private String search = "";
	private MeteorModule settingsModule;
	private MeteorSetting draggingSetting;
	private int sliderX;
	private int sliderW;
	private boolean draggingScroll;

	private String tooltip;
	private int tooltipX;
	private int tooltipY;

	private int winX;
	private int winY;
	private int winW;
	private int winH;

	private int[] starX;
	private int[] starY;
	private int[] starSize;

	public ClickGuiScreen() {
		this(null);
	}

	public ClickGuiScreen(GuiScreen parent) {
		this.parent = parent;
		for (MeteorModule.Category c : MeteorModule.Category.values()) {
			if (!MeteorModuleManager.getModules(c).isEmpty()) {
				this.tabs.add(c);
			}
		}
		if (selectedTab > this.tabs.size()) {
			selectedTab = 0;
		}
	}

	public void initGui() {
		this.generateStars();
	}

	public boolean doesGuiPauseGame() {
		return false;
	}

	// ------------------------- Layout -------------------------

	private void layout() {
		this.winW = Math.min(WIN_W, this.width - 20);
		this.winH = Math.min(WIN_H, this.height - 20);
		this.winX = (this.width - this.winW) / 2;
		this.winY = (this.height - this.winH) / 2;
	}

	private int contentX() {
		return this.winX + 10;
	}

	private int contentY() {
		return this.winY + HEADER + 8;
	}

	private int contentW() {
		return this.winW - 28;
	}

	private int contentH() {
		return this.winH - HEADER - 16;
	}

	private int cardW() {
		return (this.contentW() - GAP * (COLS - 1)) / COLS;
	}

	private int visibleRows() {
		return Math.max(1, (this.contentH() + GAP) / (CARD_H + GAP));
	}

	private int searchX() {
		return this.winX + this.winW - 26 - SEARCH_W;
	}

	private String tabLabel(int i) {
		return i == 0 ? "All" : this.tabs.get(i - 1).getLabel();
	}

	private int tabX(int i) {
		int x = this.winX + 92;
		for (int k = 0; k < i; ++k) {
			x += this.fontRendererObj.getStringWidth(this.tabLabel(k)) + 14;
		}
		return x;
	}

	private int tabW(int i) {
		return this.fontRendererObj.getStringWidth(this.tabLabel(i)) + 14;
	}

	private List<MeteorModule> filtered() {
		List<MeteorModule> out = new ArrayList<MeteorModule>();
		String q = this.search.toLowerCase();
		for (MeteorModule m : MeteorModuleManager.getModules()) {
			if (selectedTab > 0 && m.getCategory() != this.tabs.get(selectedTab - 1)) {
				continue;
			}
			if (!q.isEmpty() && !m.getName().toLowerCase().contains(q)) {
				continue;
			}
			out.add(m);
		}
		return out;
	}

	private static int rowHeight(MeteorSetting s) {
		return s.getType() == MeteorSetting.Type.BOOLEAN ? 22 : 30;
	}

	// ------------------------- Drawing -------------------------

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.layout();
		long now = Minecraft.getSystemTime();

		this.drawSpaceBackground(now);
		this.tooltip = null;
		this.drawWindow(mouseX, mouseY, now);

		super.drawScreen(mouseX, mouseY, partialTicks);

		if (this.tooltip != null) {
			int tw = this.fontRendererObj.getStringWidth(this.tooltip);
			int tx = Math.min(this.tooltipX + 8, this.width - tw - 8);
			int ty = this.tooltipY + 10;
			Gui.drawRect(tx - 4, ty - 3, tx + tw + 4, ty + 11, BORDER);
			Gui.drawRect(tx - 3, ty - 2, tx + tw + 3, ty + 10, 0xFF0E0B05);
			this.drawString(this.fontRendererObj, this.tooltip, tx, ty, TEXT);
		}
	}

	private void drawWindow(int mouseX, int mouseY, long now) {
		Gui.drawRect(this.winX - 1, this.winY - 1, this.winX + this.winW + 1, this.winY + this.winH + 1, BORDER);
		this.drawGradientRect(this.winX, this.winY, this.winX + this.winW, this.winY + this.winH, 0xF0181309,
				0xF00E0B05);

		this.drawHeaderTitle(now);

		// Tabs
		for (int i = 0; i <= this.tabs.size(); ++i) {
			int tx = this.tabX(i);
			int tw = this.tabW(i);
			boolean sel = i == selectedTab;
			boolean hover = inside(mouseX, mouseY, tx, this.winY + 4, tw, HEADER - 6);
			this.drawCenteredString(this.fontRendererObj, this.tabLabel(i), tx + tw / 2, this.winY + 10,
					sel ? GOLD : (hover ? TEXT : TEXT_DIM));
			if (sel) {
				Gui.drawRect(tx + 3, this.winY + HEADER - 3, tx + tw - 3, this.winY + HEADER - 1, GOLD);
			}
		}

		// Search box
		int sx = this.searchX();
		int sy = this.winY + 7;
		boolean sHover = inside(mouseX, mouseY, sx, sy, SEARCH_W, 14);
		Gui.drawRect(sx, sy, sx + SEARCH_W, sy + 14, (sHover || !this.search.isEmpty()) ? BORDER_HOVER : BORDER);
		Gui.drawRect(sx + 1, sy + 1, sx + SEARCH_W - 1, sy + 13, 0xFF14110A);
		if (this.search.isEmpty()) {
			this.drawString(this.fontRendererObj, "Search Mods", sx + 5, sy + 3, TEXT_DIM);
		} else {
			String caret = ((now / 500L) % 2L == 0L) ? "_" : "";
			this.drawString(this.fontRendererObj, this.search + caret, sx + 5, sy + 3, TEXT);
		}

		// Close button
		int cx = this.winX + this.winW - 18;
		boolean cHover = inside(mouseX, mouseY, cx, this.winY + 6, 12, 14);
		this.drawCenteredString(this.fontRendererObj, "x", cx + 6, this.winY + 9, cHover ? GOLD : TEXT_DIM);

		// Divider
		Gui.drawRect(this.winX + 8, this.winY + HEADER, this.winX + this.winW - 8, this.winY + HEADER + 1,
				CARD_BORDER);

		if (this.settingsModule != null) {
			this.drawSettingsView(mouseX, mouseY);
		} else {
			this.drawGrid(mouseX, mouseY);
		}
	}

	private void drawGrid(int mouseX, int mouseY) {
		List<MeteorModule> list = this.filtered();
		int rows = (list.size() + COLS - 1) / COLS;
		int vis = this.visibleRows();
		int maxScroll = Math.max(0, rows - vis);
		scrollRow = Math.max(0, Math.min(maxScroll, scrollRow));

		if (list.isEmpty()) {
			this.drawCenteredString(this.fontRendererObj, "No modules found", this.winX + this.winW / 2,
					this.winY + this.winH / 2, TEXT_DIM);
		}

		int cw = this.cardW();
		int start = scrollRow * COLS;
		int end = Math.min(list.size(), (scrollRow + vis) * COLS);
		for (int i = start; i < end; ++i) {
			int r = i / COLS - scrollRow;
			int c = i % COLS;
			int x = this.contentX() + c * (cw + GAP);
			int y = this.contentY() + r * (CARD_H + GAP);
			this.drawCard(list.get(i), x, y, cw, mouseX, mouseY);
		}

		// Scrollbar
		int sx = this.contentX() + this.contentW() + 4;
		int sy = this.contentY();
		int sh = this.contentH();
		Gui.drawRect(sx, sy, sx + 4, sy + sh, 0xFF1A150A);
		if (rows > vis) {
			int th = Math.max(16, sh * vis / rows);
			int ty = sy + (maxScroll == 0 ? 0 : (sh - th) * scrollRow / maxScroll);
			Gui.drawRect(sx, ty, sx + 4, ty + th, GOLD);
		}
	}

	private void drawCard(MeteorModule m, int x, int y, int cw, int mouseX, int mouseY) {
		boolean hover = inside(mouseX, mouseY, x, y, cw, CARD_H);
		int border = hover ? BORDER_HOVER : (m.isEnabled() ? BORDER : CARD_BORDER);
		Gui.drawRect(x, y, x + cw, y + CARD_H, border);
		this.drawGradientRect(x + 1, y + 1, x + cw - 1, y + CARD_H - 1, hover ? CARD_HOVER_TOP : CARD_TOP,
				hover ? CARD_HOVER_BOTTOM : CARD_BOTTOM);

		this.drawCenteredString(this.fontRendererObj, m.getName(), x + cw / 2, y + 9, m.isEnabled() ? GOLD : TEXT);
		this.drawSwitch(m, m.isEnabled(), x + cw / 2 - 12, y + 25);

		if (!m.getSettings().isEmpty()) {
			boolean cornerHover = inside(mouseX, mouseY, x + cw - 18, y + 2, 16, 10);
			this.drawString(this.fontRendererObj, "..", x + cw - 12, y + 3, cornerHover ? GOLD : TEXT_DIM);
		}
		if (hover) {
			this.tooltip = m.getDescription();
			this.tooltipX = mouseX;
			this.tooltipY = mouseY;
		}
	}

	private void drawSettingsView(int mouseX, int mouseY) {
		int x = this.contentX();
		int y = this.contentY();
		int w = this.contentW() + 8;

		boolean hb = inside(mouseX, mouseY, x, y, 44, 14);
		Gui.drawRect(x, y, x + 44, y + 14, hb ? BORDER_HOVER : BORDER);
		this.drawGradientRect(x + 1, y + 1, x + 43, y + 13, hb ? CARD_HOVER_TOP : CARD_TOP,
				hb ? CARD_HOVER_BOTTOM : CARD_BOTTOM);
		this.drawCenteredString(this.fontRendererObj, "< Back", x + 22, y + 3, hb ? GOLD : TEXT);
		this.drawString(this.fontRendererObj, this.settingsModule.getName(), x + 54, y + 3, GOLD);

		int ry = y + 22;
		for (MeteorSetting s : this.settingsModule.getSettings()) {
			int rh = rowHeight(s);
			Gui.drawRect(x, ry, x + w, ry + rh, CARD_BORDER);
			this.drawGradientRect(x + 1, ry + 1, x + w - 1, ry + rh - 1, CARD_TOP, CARD_BOTTOM);

			if (s.getType() == MeteorSetting.Type.BOOLEAN) {
				this.drawString(this.fontRendererObj, s.getName(), x + 8, ry + 7, TEXT);
				this.drawSwitch(s, s.getBoolean(), x + w - 32, ry + 6);
			} else {
				this.drawString(this.fontRendererObj, s.getName() + ": " + fmt(s.getDouble()), x + 8, ry + 5, TEXT);
				int tx0 = x + 8;
				int tw = w - 16;
				double t = (s.getDouble() - s.getMin()) / (s.getMax() - s.getMin());
				int fx = tx0 + (int) (tw * t);
				Gui.drawRect(tx0, ry + rh - 10, tx0 + tw, ry + rh - 7, 0xFF2A220C);
				Gui.drawRect(tx0, ry + rh - 10, fx, ry + rh - 7, GOLD);
				Gui.drawRect(fx - 1, ry + rh - 12, fx + 2, ry + rh - 5, 0xFFFFFFFF);
			}
			ry += rh + GAP;
		}
	}

	/** Animated pill switch. */
	private void drawSwitch(Object key, boolean on, int x, int y) {
		float t = this.anim(key, on);
		int w = 24;
		int h = 10;
		int track = lerpColor(0xFF3A3322, 0xFFFFC21F, t);
		Gui.drawRect(x + 1, y, x + w - 1, y + h, track);
		Gui.drawRect(x, y + 1, x + w, y + h - 1, track);
		int kx = x + 1 + (int) ((w - h) * t);
		int knob = lerpColor(0xFF9A9070, 0xFFFFFFFF, t);
		Gui.drawRect(kx, y + 1, kx + h - 2, y + h - 1, knob);
	}

	private float anim(Object key, boolean on) {
		float target = on ? 1.0F : 0.0F;
		Float f = ANIM.get(key);
		float cur = f == null ? target : f.floatValue();
		cur += (target - cur) * 0.35F;
		ANIM.put(key, Float.valueOf(cur));
		return cur;
	}

	/** Small animated glowing title in the window header. */
	private void drawHeaderTitle(long now) {
		int total = this.fontRendererObj.getStringWidth(TITLE);
		int len = TITLE.length();
		float sweep = ((now % 2600L) / 2600.0F) * (len + 6) - 3.0F;
		float scale = 1.4F;

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) (this.winX + 10), (float) (this.winY + 8), 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
				RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);

		int x = 0;
		for (int i = 0; i < len; ++i) {
			String s = String.valueOf(TITLE.charAt(i));
			int cw = this.fontRendererObj.getStringWidth(s);
			float bob = (float) Math.sin(now * 0.006D + i * 0.7D) * 0.5F;
			float glow = Math.max(0.0F, 1.0F - Math.abs(i - sweep) / 2.5F);

			int auraAlpha = (int) (60.0F + 90.0F * glow);
			int auraColor = (auraAlpha << 24) | TITLE_AURA;
			this.fontRendererObj.drawString(s, (float) x - 0.5F, bob, auraColor, false);
			this.fontRendererObj.drawString(s, (float) x + 0.5F, bob, auraColor, false);
			this.fontRendererObj.drawString(s, (float) x, bob - 0.5F, auraColor, false);
			this.fontRendererObj.drawString(s, (float) x, bob + 0.5F, auraColor, false);

			int rgb = lerpColor(TITLE_COLOR_A, TITLE_COLOR_B, glow) & 0xFFFFFF;
			this.fontRendererObj.drawString(s, (float) x, bob, 0xFF000000 | rgb, true);
			x += cw;
		}
		GlStateManager.popMatrix();
	}

	// ------------------------- Input -------------------------

	protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
		super.mouseClicked(mouseX, mouseY, button);
		this.layout();

		if (button != 0 && button != 1) {
			return;
		}

		// Close button
		if (button == 0 && inside(mouseX, mouseY, this.winX + this.winW - 18, this.winY + 6, 12, 14)) {
			this.mc.displayGuiScreen(this.parent);
			return;
		}

		// Tabs
		if (button == 0) {
			for (int i = 0; i <= this.tabs.size(); ++i) {
				if (inside(mouseX, mouseY, this.tabX(i), this.winY + 4, this.tabW(i), HEADER - 6)) {
					selectedTab = i;
					scrollRow = 0;
					this.settingsModule = null;
					return;
				}
			}
		}

		if (this.settingsModule != null) {
			this.clickSettings(mouseX, mouseY, button);
			return;
		}

		List<MeteorModule> list = this.filtered();
		int rows = (list.size() + COLS - 1) / COLS;
		int vis = this.visibleRows();

		// Scrollbar
		if (button == 0 && rows > vis
				&& inside(mouseX, mouseY, this.contentX() + this.contentW() + 2, this.contentY(), 8, this.contentH())) {
			this.draggingScroll = true;
			this.updateScroll(mouseY);
			return;
		}

		// Cards
		int cw = this.cardW();
		int start = scrollRow * COLS;
		int end = Math.min(list.size(), (scrollRow + vis) * COLS);
		for (int i = start; i < end; ++i) {
			int r = i / COLS - scrollRow;
			int c = i % COLS;
			int x = this.contentX() + c * (cw + GAP);
			int y = this.contentY() + r * (CARD_H + GAP);
			if (inside(mouseX, mouseY, x, y, cw, CARD_H)) {
				MeteorModule m = list.get(i);
				boolean hasSettings = !m.getSettings().isEmpty();
				if (button == 1 && hasSettings) {
					this.settingsModule = m;
				} else if (button == 0) {
					if (hasSettings && inside(mouseX, mouseY, x + cw - 18, y + 2, 16, 10)) {
						this.settingsModule = m;
					} else {
						m.toggle();
					}
				}
				return;
			}
		}
	}

	private void clickSettings(int mouseX, int mouseY, int button) {
		if (button != 0) {
			return;
		}
		int x = this.contentX();
		int y = this.contentY();
		int w = this.contentW() + 8;

		if (inside(mouseX, mouseY, x, y, 44, 14)) {
			this.settingsModule = null;
			return;
		}

		int ry = y + 22;
		for (MeteorSetting s : this.settingsModule.getSettings()) {
			int rh = rowHeight(s);
			if (inside(mouseX, mouseY, x, ry, w, rh)) {
				if (s.getType() == MeteorSetting.Type.BOOLEAN) {
					s.toggle();
				} else {
					this.draggingSetting = s;
					this.sliderX = x + 8;
					this.sliderW = w - 16;
					this.updateSlider(mouseX);
				}
				return;
			}
			ry += rh + GAP;
		}
	}

	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
		if (this.draggingScroll) {
			this.updateScroll(mouseY);
		}
		if (this.draggingSetting != null) {
			this.updateSlider(mouseX);
		}
	}

	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		this.draggingScroll = false;
		this.draggingSetting = null;
	}

	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == KEY_ESCAPE) {
			if (this.settingsModule != null) {
				this.settingsModule = null;
			} else if (!this.search.isEmpty()) {
				this.search = "";
				scrollRow = 0;
			} else {
				this.mc.displayGuiScreen(this.parent);
			}
			return;
		}

		if (this.settingsModule == null) {
			if (keyCode == KEY_BACKSPACE) {
				if (!this.search.isEmpty()) {
					this.search = this.search.substring(0, this.search.length() - 1);
					scrollRow = 0;
				}
				return;
			}
			if (typedChar >= 32 && typedChar != 127
					&& this.fontRendererObj.getStringWidth(this.search) < SEARCH_W - 20) {
				this.search += typedChar;
				scrollRow = 0;
			}
		}
	}

	private void updateSlider(int mouseX) {
		double t = (mouseX - this.sliderX) / (double) this.sliderW;
		t = Math.max(0.0D, Math.min(1.0D, t));
		this.draggingSetting.setValue(
				this.draggingSetting.getMin() + t * (this.draggingSetting.getMax() - this.draggingSetting.getMin()));
	}

	private void updateScroll(int mouseY) {
		List<MeteorModule> list = this.filtered();
		int rows = (list.size() + COLS - 1) / COLS;
		int vis = this.visibleRows();
		int maxScroll = Math.max(0, rows - vis);
		int sh = this.contentH();
		int th = Math.max(16, sh * vis / Math.max(1, rows));
		double t = (mouseY - this.contentY() - th / 2) / (double) Math.max(1, sh - th);
		t = Math.max(0.0D, Math.min(1.0D, t));
		scrollRow = (int) Math.round(t * maxScroll);
	}

	// ------------------------- Background -------------------------

	private void generateStars() {
		this.starX = new int[STAR_COUNT];
		this.starY = new int[STAR_COUNT];
		this.starSize = new int[STAR_COUNT];
		long seed = 424242L;
		int w = Math.max(1, this.width);
		int h = Math.max(1, this.height);
		for (int i = 0; i < STAR_COUNT; ++i) {
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			this.starX[i] = (int) ((seed >>> 33) % w);
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			this.starY[i] = (int) ((seed >>> 33) % h);
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			this.starSize[i] = ((seed >>> 40) % 5L == 0L) ? 2 : 1;
		}
	}

	private void drawSpaceBackground(long now) {
		this.drawGradientRect(0, 0, this.width, this.height, BG_TOP, BG_BOTTOM);

		if (this.starX == null) {
			this.generateStars();
		}

		if (MeteorModuleManager.menuStars()) {
			for (int i = 0; i < STAR_COUNT; ++i) {
				int a = (int) (90.0D + 110.0D * (0.5D + 0.5D * Math.sin(now * 0.003D + i * 1.7D)));
				int sz = this.starSize[i];
				Gui.drawRect(this.starX[i], this.starY[i], this.starX[i] + sz, this.starY[i] + sz,
						(a << 24) | 0xFFF0C0);
			}
		}

		if (MeteorModuleManager.menuMeteors()) {
			for (int m = 0; m < METEOR_COUNT; ++m) {
				float phase = ((now + m * 1900L) % 7600L) / 7600.0F;
				float fade = Math.min(1.0F, Math.min(phase * 10.0F, (1.0F - phase) * 10.0F));
				int sx = (int) (this.width * (0.45F + 0.55F * (((m * 53) % 100) / 100.0F)));
				int hx = sx - (int) (phase * this.width * 0.75F);
				int hy = -10 + (int) (phase * (this.height * 0.8F));

				for (int i = 11; i >= 0; --i) {
					int size = Math.max(1, 4 - i / 3);
					int tx = hx + i * 4;
					int ty = hy - i * 2;
					int c = withAlpha(lerpColor(FLAME_HEAD, FLAME_TAIL, i / 11.0F), fade);
					Gui.drawRect(tx, ty, tx + size, ty + size, c);
				}
				Gui.drawRect(hx - 2, hy - 2, hx + 3, hy + 3, withAlpha(METEOR_OUTER, fade));
				Gui.drawRect(hx - 1, hy - 1, hx + 2, hy + 2, withAlpha(METEOR_CORE, fade));
			}
		}

		this.drawGradientRect(0, 0, this.width, this.height, 0x00000000, 0xA0000000);
	}

	// ------------------------- Helpers -------------------------

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	private static String fmt(double v) {
		return String.valueOf(Math.round(v * 100.0D) / 100.0D);
	}

	private static int withAlpha(int color, float factor) {
		int a = (int) (((color >>> 24) & 255) * factor);
		return (a << 24) | (color & 0xFFFFFF);
	}

	private static int lerpColor(int a, int b, float t) {
		int aa = (a >>> 24) & 255, ar = (a >>> 16) & 255, ag = (a >>> 8) & 255, ab = a & 255;
		int ba = (b >>> 24) & 255, br = (b >>> 16) & 255, bg = (b >>> 8) & 255, bb = b & 255;
		int ra = aa + (int) ((ba - aa) * t);
		int rr = ar + (int) ((br - ar) * t);
		int rg = ag + (int) ((bg - ag) * t);
		int rb = ab + (int) ((bb - ab) * t);
		return (ra << 24) | (rr << 16) | (rg << 8) | rb;
	}
}

/**
 * Base class for every feature that can be toggled from the ClickGUI.
 */
abstract class MeteorModule {

	public enum Category {
		VISUAL("Visual"), HUD("HUD"), MISC("Misc");

		private final String label;

		Category(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	private final String name;
	private final String description;
	private final Category category;
	private boolean enabled;
	private final List<MeteorSetting> settings = new ArrayList<MeteorSetting>();

	protected MeteorModule(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	protected Minecraft mc() {
		return Minecraft.getMinecraft();
	}

	protected MeteorSetting addSetting(MeteorSetting setting) {
		this.settings.add(setting);
		return setting;
	}

	public void toggle() {
		this.setEnabled(!this.enabled);
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		if (enabled) {
			this.onEnable();
		} else {
			this.onDisable();
		}
	}

	/** Called once when the module is switched on. */
	protected void onEnable() {
	}

	/** Called once when the module is switched off. */
	protected void onDisable() {
	}

	/** Called every frame while enabled, from the in-game HUD render hook. */
	public void onRender() {
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Category getCategory() {
		return category;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public List<MeteorSetting> getSettings() {
		return settings;
	}
}

/**
 * A single option on a module: either an on/off switch or a number slider.
 */
class MeteorSetting {

	public enum Type {
		BOOLEAN, NUMBER
	}

	private final String name;
	private final Type type;
	private boolean boolValue;
	private double numberValue;
	private double min;
	private double max;
	private double step;

	private MeteorSetting(String name, Type type) {
		this.name = name;
		this.type = type;
	}

	public static MeteorSetting bool(String name, boolean defaultValue) {
		MeteorSetting s = new MeteorSetting(name, Type.BOOLEAN);
		s.boolValue = defaultValue;
		return s;
	}

	public static MeteorSetting number(String name, double defaultValue, double min, double max, double step) {
		MeteorSetting s = new MeteorSetting(name, Type.NUMBER);
		s.min = min;
		s.max = max;
		s.step = step;
		s.setValue(defaultValue);
		return s;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public boolean getBoolean() {
		return boolValue;
	}

	public void toggle() {
		this.boolValue = !this.boolValue;
	}

	public double getDouble() {
		return numberValue;
	}

	public float getFloat() {
		return (float) numberValue;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public void setValue(double value) {
		if (step > 0.0D) {
			value = Math.round((value - min) / step) * step + min;
		}
		this.numberValue = Math.max(min, Math.min(max, value));
	}
}

/**
 * Holds every module. To add a feature, write a MeteorModule subclass below and
 * register it in the static block.
 */
final class MeteorModuleManager {

	private static final List<MeteorModule> MODULES = new ArrayList<MeteorModule>();
	private static final MenuEffects MENU_FX = new MenuEffects();

	static {
		register(MENU_FX);
		MENU_FX.setEnabled(true);
		register(new Watermark());
		register(new FpsDisplay());
		register(new Clock());
	}

	private MeteorModuleManager() {
	}

	private static void register(MeteorModule module) {
		MODULES.add(module);
	}

	public static List<MeteorModule> getModules() {
		return MODULES;
	}

	public static List<MeteorModule> getModules(MeteorModule.Category category) {
		List<MeteorModule> out = new ArrayList<MeteorModule>();
		for (MeteorModule m : MODULES) {
			if (m.getCategory() == category) {
				out.add(m);
			}
		}
		return out;
	}

	/** Menu effect switches, read by the main menu and the ClickGUI. */
	public static boolean menuStars() {
		return MENU_FX.isEnabled() && MENU_FX.stars.getBoolean();
	}

	public static boolean menuMeteors() {
		return MENU_FX.isEnabled() && MENU_FX.meteors.getBoolean();
	}

	public static boolean menuEmbers() {
		return MENU_FX.isEnabled() && MENU_FX.embers.getBoolean();
	}

	/**
	 * Call this once per frame from the in-game HUD (end of
	 * GuiIngame.renderGameOverlay).
	 */
	public static void renderHud() {
		for (MeteorModule m : MODULES) {
			if (m.isEnabled()) {
				m.onRender();
			}
		}
	}

	private static void drawHudText(String text, int x, int y, int color, boolean shadow, float scale) {
		Minecraft mc = Minecraft.getMinecraft();
		GlStateManager.pushMatrix();
		GlStateManager.scale(scale, scale, 1.0F);
		mc.fontRendererObj.drawString(text, (float) x / scale, (float) y / scale, color, shadow);
		GlStateManager.popMatrix();
	}

	// ------------------------- Modules -------------------------

	/** Stars, shooting meteors and embers on the menus. */
	private static class MenuEffects extends MeteorModule {
		final MeteorSetting stars = addSetting(MeteorSetting.bool("Stars", true));
		final MeteorSetting meteors = addSetting(MeteorSetting.bool("Meteors", true));
		final MeteorSetting embers = addSetting(MeteorSetting.bool("Embers", true));

		MenuEffects() {
			super("Menu Effects", "Stars, meteors and embers on menus.", Category.VISUAL);
		}
	}

	/** Shows the client name in the corner. */
	private static class Watermark extends MeteorModule {
		private final MeteorSetting shadow = addSetting(MeteorSetting.bool("Shadow", true));

		Watermark() {
			super("Watermark", "Shows the client name.", Category.HUD);
		}

		public void onRender() {
			drawHudText("MeteorMC", 4, 4, 0xFFFFB020, shadow.getBoolean(), 1.0F);
		}
	}

	/** Shows frames per second in the corner. */
	private static class FpsDisplay extends MeteorModule {
		private final MeteorSetting shadow = addSetting(MeteorSetting.bool("Shadow", true));
		private final MeteorSetting scale = addSetting(MeteorSetting.number("Scale", 1.0D, 0.5D, 2.0D, 0.25D));
		private int frames;
		private int fps;
		private long last = Minecraft.getSystemTime();

		FpsDisplay() {
			super("FPS Display", "Shows your frame rate.", Category.HUD);
		}

		public void onRender() {
			frames++;
			long now = Minecraft.getSystemTime();
			if (now - last >= 1000L) {
				fps = frames;
				frames = 0;
				last = now;
			}
			drawHudText("FPS: " + fps, 4, 16, 0xFFFFD83A, shadow.getBoolean(), scale.getFloat());
		}
	}

	/** Shows the current time. */
	private static class Clock extends MeteorModule {
		private final MeteorSetting shadow = addSetting(MeteorSetting.bool("Shadow", true));
		private final MeteorSetting hour24 = addSetting(MeteorSetting.bool("24 Hour", false));

		Clock() {
			super("Clock", "Shows the current time.", Category.HUD);
		}

		public void onRender() {
			Calendar c = Calendar.getInstance();
			int h = c.get(Calendar.HOUR_OF_DAY);
			int min = c.get(Calendar.MINUTE);
			String suffix = "";
			if (!hour24.getBoolean()) {
				suffix = h >= 12 ? " PM" : " AM";
				h = h % 12;
				if (h == 0) {
					h = 12;
				}
			}
			String text = h + ":" + (min < 10 ? "0" : "") + min + suffix;
			drawHudText(text, 4, 28, 0xFFFFD83A, shadow.getBoolean(), 1.0F);
		}
	}
}
