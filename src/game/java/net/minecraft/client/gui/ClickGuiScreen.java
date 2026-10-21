package net.minecraft.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.RealOpenGLEnums;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.StringUtils;

/**
 * MeteorMC ClickGUI (frosted glass).
 *
 * Left navigation: Home, Modules, and at the bottom Credits and Settings.
 * Modules page: search bar + category pills + a two-column grid of module cards.
 *   Click a card (or its switch): turn the module on or off.
 *   Click the gear (or right click a card): open its settings.
 * Type to search. Up / Down arrows or the scrollbar scroll the grid.
 * Esc: back / clear search / close.
 */
public class ClickGuiScreen extends GuiScreen {

	// ===================== STYLE =====================
	private static final int WIN_W = 480;
	private static final int WIN_H = 270;
	private static final int NAV_W = 92;
	private static final int NAV_W_COMPACT = 70;
	private static final int NAV_ROW = 18;
	private static final int GAP = 6;
	private static final int CARD_H = 44;
	private static final int HEAD_Y = 24;
	private static final int HEAD_H = 18;
	private static final int BODY_Y = 50;

	private static final int PANEL_TOP = 0xB4A4AAB0;
	private static final int PANEL_BOTTOM = 0xB4767C84;
	private static final int PANEL_LINE = 0x60FFFFFF;
	private static final int LINE = 0x40FFFFFF;
	private static final int CARD_TOP = 0x34FFFFFF;
	private static final int CARD_BOTTOM = 0x22FFFFFF;
	private static final int CARD_HOVER_TOP = 0x4CFFFFFF;
	private static final int CARD_HOVER_BOTTOM = 0x38FFFFFF;
	private static final int CARD_LINE = 0x48FFFFFF;
	private static final int CARD_LINE_HOVER = 0xA0FFFFFF;
	private static final int PILL = 0x38FFFFFF;
	private static final int PILL_ON = 0x7CFFFFFF;

	private static final int TEXT = 0xFFFFFFFF;
	private static final int TEXT_DIM = 0xFFE0E5EA;
	private static final int TEXT_FAINT = 0xFFC4CAD0;

	private static final int BG_TOP = 0xFF5F86AE;
	private static final int BG_BOTTOM = 0xFFCFE3F5;
	private static final int METEOR_OUTER = 0xFFBEE3FF;
	private static final int METEOR_CORE = 0xFFFFFFFF;
	private static final int FLAME_HEAD = 0xE0BEE3FF;
	private static final int FLAME_TAIL = 0x20FFFFFF;

	private static final int STAR_COUNT = 90;
	private static final int METEOR_COUNT = 4;

	private static final String[] GEAR = { "#..##..#", ".######.", "..#..#..", "###..###", "###..###", "..#..#..",
			".######.", "#..##..#" };
	private static final String[] STAR = { "...#...", "...#...", "..###..", "#######", "..###..", "...#...", "...#..." };
	// =================================================

	private static final int PAGE_HOME = 0;
	private static final int PAGE_MODULES = 1;
	private static final int PAGE_SETTINGS = 2;

	private static final int KEY_ESCAPE = 1;
	private static final int KEY_BACKSPACE = 14;
	private static final int KEY_UP = 200;
	private static final int KEY_DOWN = 208;

	/** Options on the Settings page. */
	private static final MeteorSetting DESCRIPTIONS = MeteorSetting.bool("Descriptions", true);
	private static final MeteorSetting HEADER_GLOW = MeteorSetting.bool("Header Glow", true);

	/** Remembered between openings. */
	private static int page = PAGE_MODULES;
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

	private int winX;
	private int winY;
	private int winW;
	private int winH;

	private int homeFrames;
	private int homeFps;
	private long homeLast = Minecraft.getSystemTime();

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

	/** Narrow windows get a narrower navigation column. */
	private int navW() {
		return this.winW < 380 ? NAV_W_COMPACT : NAV_W;
	}

	/** Two columns of cards when there is room, otherwise one. */
	private int cols() {
		return (this.gridW() - GAP) / 2 >= 125 ? 2 : 1;
	}

	private int mainX() {
		return this.winX + this.navW() + 12;
	}

	private int mainW() {
		return this.winW - this.navW() - 24;
	}

	private int headY() {
		return this.winY + HEAD_Y;
	}

	private int closeX() {
		return this.mainX() + this.mainW() - HEAD_H;
	}

	private String tabLabel(int i) {
		return i == 0 ? "All" : this.tabs.get(i - 1).getLabel();
	}

	private int pillW(int i) {
		return this.fontRendererObj.getStringWidth(this.tabLabel(i)) + 12;
	}

	private int pillsTotal() {
		int total = 0;
		for (int i = 0; i <= this.tabs.size(); ++i) {
			total += this.pillW(i) + (i > 0 ? 4 : 0);
		}
		return total;
	}

	/** When the search bar and all the pills do not fit on one row, the pills move to a second row. */
	private boolean pillsOnOwnRow() {
		return this.pillsTotal() + 60 + 12 > this.closeX() - this.mainX();
	}

	private int searchW() {
		if (this.pillsOnOwnRow()) {
			return this.closeX() - 6 - this.mainX();
		}
		return Math.max(60, this.closeX() - 12 - this.pillsTotal() - this.mainX());
	}

	/** Position of pill i as {x, y}. Pills wrap onto extra rows when the window is narrow. */
	private int[] pillPos(int i) {
		if (!this.pillsOnOwnRow()) {
			int x = this.mainX() + this.searchW() + 6;
			for (int k = 0; k < i; ++k) {
				x += this.pillW(k) + 4;
			}
			return new int[] { x, this.headY() };
		}
		int x = this.mainX();
		int y = this.headY() + HEAD_H + 4;
		int right = this.mainX() + this.mainW();
		for (int k = 0; k <= i; ++k) {
			int w = this.pillW(k);
			if (x > this.mainX() && x + w > right) {
				x = this.mainX();
				y += HEAD_H + 3;
			}
			if (k == i) {
				return new int[] { x, y };
			}
			x += w + 4;
		}
		return new int[] { x, y };
	}

	private int pillX(int i) {
		return this.pillPos(i)[0];
	}

	private int pillY(int i) {
		return this.pillPos(i)[1];
	}

	private int bodyY() {
		boolean pills = page == PAGE_MODULES && this.settingsModule == null && this.pillsOnOwnRow();
		return pills ? this.pillY(this.tabs.size()) + HEAD_H + 8 : this.winY + BODY_Y;
	}

	private int bodyH() {
		return this.winY + this.winH - 10 - this.bodyY();
	}

	private int gridW() {
		return this.mainW() - 8;
	}

	private int cardW() {
		return (this.gridW() - GAP * (this.cols() - 1)) / this.cols();
	}

	private int visibleRows() {
		return Math.max(1, (this.bodyH() + GAP) / (CARD_H + GAP));
	}

	private int navTopY(int i) {
		return this.winY + 34 + i * (NAV_ROW + 3);
	}

	/** j = 0 is the bottom-most item (Settings), j = 1 is above it (Credits). */
	private int navBottomY(int j) {
		return this.winY + this.winH - 10 - NAV_ROW - j * (NAV_ROW + 3);
	}

	private boolean navHit(int mx, int my, int y) {
		return inside(mx, my, this.winX + 8, y, this.navW() - 16, NAV_ROW);
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

	private List<MeteorSetting> settingsPageList() {
		List<MeteorSetting> list = new ArrayList<MeteorSetting>(MeteorModuleManager.menuEffectSettings());
		list.add(DESCRIPTIONS);
		list.add(HEADER_GLOW);
		return list;
	}

	private static int rowHeight(MeteorSetting s) {
		return s.getType() == MeteorSetting.Type.BOOLEAN ? 22 : 30;
	}

	private String fit(String s, int maxW) {
		if (this.fontRendererObj.getStringWidth(s) <= maxW) {
			return s;
		}
		while (s.length() > 1 && this.fontRendererObj.getStringWidth(s + "..") > maxW) {
			s = s.substring(0, s.length() - 1);
		}
		return s + "..";
	}

	private List<String> wrap(String text, int maxW, int maxLines) {
		List<String> lines = new ArrayList<String>();
		String line = "";
		for (String w : text.split(" ")) {
			String test = line.isEmpty() ? w : line + " " + w;
			if (line.isEmpty() || this.fontRendererObj.getStringWidth(test) <= maxW) {
				line = test;
			} else {
				lines.add(line);
				line = w;
			}
		}
		if (!line.isEmpty()) {
			lines.add(line);
		}
		if (lines.size() > maxLines) {
			List<String> cut = new ArrayList<String>(lines.subList(0, maxLines));
			cut.set(maxLines - 1, cut.get(maxLines - 1) + "...");
			return cut;
		}
		return lines;
	}

	// ------------------------- Drawing helpers -------------------------

	private static void outline(int x, int y, int w, int h, int color) {
		Gui.drawRect(x + 1, y, x + w - 1, y + 1, color);
		Gui.drawRect(x + 1, y + h - 1, x + w - 1, y + h, color);
		Gui.drawRect(x, y + 1, x + 1, y + h - 1, color);
		Gui.drawRect(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	private void panel(int x, int y, int w, int h, int top, int bottom, int line) {
		this.drawGradientRect(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
		outline(x, y, w, h, line);
	}

	/** Rounded fill without overlapping rectangles, so translucent colors stay even. */
	private static void fillRound(int x, int y, int w, int h, int color) {
		Gui.drawRect(x + 1, y, x + w - 1, y + 1, color);
		Gui.drawRect(x, y + 1, x + w, y + h - 1, color);
		Gui.drawRect(x + 1, y + h - 1, x + w - 1, y + h, color);
	}

	private static void bitmap(String[] rows, int px, int py, int color) {
		for (int r = 0; r < rows.length; ++r) {
			for (int c = 0; c < rows[r].length(); ++c) {
				if (rows[r].charAt(c) == '#') {
					Gui.drawRect(px + c, py + r, px + c + 1, py + r + 1, color);
				}
			}
		}
	}

	private float anim(Object key, boolean on) {
		float target = on ? 1.0F : 0.0F;
		Float f = ANIM.get(key);
		float cur = f == null ? target : f.floatValue();
		cur += (target - cur) * 0.35F;
		ANIM.put(key, Float.valueOf(cur));
		return cur;
	}

	/** Animated pill switch: white with a dark knob when on. */
	private void drawSwitch(Object key, boolean on, int x, int y) {
		float t = this.anim(key, on);
		int w = 22;
		int h = 11;
		fillRound(x, y, w, h, lerpColor(0x50FFFFFF, 0xFFFFFFFF, t));
		int kx = x + 2 + (int) ((w - 4 - 7) * t);
		Gui.drawRect(kx, y + 2, kx + 7, y + h - 2, lerpColor(0xFFF2F5F8, 0xFF2A3440, t));
	}

	private int textSpan(String text, int spacing) {
		int total = 0;
		for (int i = 0; i < text.length(); ++i) {
			total += this.fontRendererObj.getStringWidth(String.valueOf(text.charAt(i))) + (i < text.length() - 1 ? spacing : 0);
		}
		return total;
	}

	/** Centered, letter-spaced text with an animated glow sweep (switch off in Settings). */
	private void drawGlowText(String text, int cx, int top, float scale, int spacing, long now) {
		int len = text.length();
		int total = this.textSpan(text, spacing);
		float sweep = ((now % 2600L) / 2600.0F) * (len + 6) - 3.0F;
		boolean glow = HEADER_GLOW.getBoolean();

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) cx, (float) top, 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
				RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);
		int x = -total / 2;
		for (int i = 0; i < len; ++i) {
			String s = String.valueOf(text.charAt(i));
			int cw = this.fontRendererObj.getStringWidth(s);
			float bob = glow ? (float) Math.sin(now * 0.006D + i * 0.7D) * 0.4F : 0.0F;
			float g = glow ? Math.max(0.0F, 1.0F - Math.abs(i - sweep) / 2.5F) : 0.0F;
			if (glow) {
				int auraColor = ((int) (50.0F + 100.0F * g) << 24) | 0x7FC4FF;
				this.fontRendererObj.drawString(s, (float) x - 0.5F, bob, auraColor, false);
				this.fontRendererObj.drawString(s, (float) x + 0.5F, bob, auraColor, false);
				this.fontRendererObj.drawString(s, (float) x, bob - 0.5F, auraColor, false);
				this.fontRendererObj.drawString(s, (float) x, bob + 0.5F, auraColor, false);
			}
			int rgb = lerpColor(0xFFDCE8F4, 0xFFFFFFFF, g) & 0xFFFFFF;
			this.fontRendererObj.drawString(s, (float) x, bob, 0xFF000000 | rgb, false);
			x += cw + spacing;
		}
		GlStateManager.popMatrix();
	}

	// ------------------------- Drawing -------------------------

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.layout();
		long now = Minecraft.getSystemTime();

		this.homeFrames++;
		if (now - this.homeLast >= 1000L) {
			this.homeFps = this.homeFrames;
			this.homeFrames = 0;
			this.homeLast = now;
		}

		this.drawBackdrop(mouseX, mouseY, partialTicks, now);
		this.drawWindow(mouseX, mouseY, now);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	/**
	 * See-through backdrop: the menu panorama behind the main menu, or the game
	 * world itself when you open this in-game.
	 */
	private void drawBackdrop(int mouseX, int mouseY, float partialTicks, long now) {
		if (this.parent instanceof GuiMainMenu) {
			((GuiMainMenu) this.parent).drawMenuBackground(mouseX, mouseY, partialTicks);
			this.drawGradientRect(0, 0, this.width, this.height, 0x28000000, 0x58000000);
		} else if (this.mc.world != null) {
			this.drawGradientRect(0, 0, this.width, this.height, 0x30000000, 0x70000000);
		} else {
			this.drawSpaceBackground(now);
		}
		if (MeteorModuleManager.menuEmbers()) {
			MeteorModuleManager.drawSnow(this.width, this.height, now);
		}
	}

	private void drawWindow(int mouseX, int mouseY, long now) {
		this.panel(this.winX, this.winY, this.winW, this.winH, PANEL_TOP, PANEL_BOTTOM, PANEL_LINE);
		Gui.drawRect(this.winX + 1, this.winY + 1, this.winX + this.winW - 1, this.winY + 2, 0x30FFFFFF);

		// Navigation column
		Gui.drawRect(this.winX + 1, this.winY + 2, this.winX + this.navW(), this.winY + this.winH - 1, 0x14000000);
		Gui.drawRect(this.winX + this.navW(), this.winY + 28, this.winX + this.navW() + 1, this.winY + this.winH - 8, LINE);

		// Logo
		int logoCx = this.winX + this.winW / 2;
		int span = this.textSpan("METEORMC", 3);
		bitmap(STAR, logoCx - span / 2 - 12, this.winY + 8, TEXT);
		this.drawGlowText("METEORMC", logoCx + 4, this.winY + 8, 1.0F, 3, now);

		this.drawNavItem("Home", this.navTopY(0), page == PAGE_HOME, mouseX, mouseY);
		this.drawNavItem("Modules", this.navTopY(1), page == PAGE_MODULES, mouseX, mouseY);
		this.drawNavItem("Credits", this.navBottomY(1), false, mouseX, mouseY);
		this.drawNavItem("Settings", this.navBottomY(0), page == PAGE_SETTINGS, mouseX, mouseY);

		this.drawHeader(mouseX, mouseY, now);

		if (page == PAGE_HOME) {
			this.drawHome(now);
		} else if (page == PAGE_SETTINGS) {
			this.drawSettingRows(this.settingsPageList(), this.mainX(), this.bodyY(), this.mainW(), mouseX, mouseY);
		} else if (this.settingsModule != null) {
			this.drawModuleSettings(mouseX, mouseY);
		} else {
			this.drawGrid(mouseX, mouseY);
		}
	}

	private void drawNavItem(String label, int y, boolean selected, int mouseX, int mouseY) {
		boolean hover = this.navHit(mouseX, mouseY, y);
		int x = this.winX + 8;
		int w = this.navW() - 16;
		if (selected) {
			fillRound(x, y, w, NAV_ROW, 0x58FFFFFF);
		} else if (hover) {
			fillRound(x, y, w, NAV_ROW, 0x30FFFFFF);
		}
		this.drawString(this.fontRendererObj, label, x + (this.navW() < NAV_W ? 6 : 10), y + (NAV_ROW - 8) / 2,
				selected || hover ? TEXT : TEXT_DIM);
	}

	private void drawHeader(int mouseX, int mouseY, long now) {
		int hy = this.headY();
		boolean closeHover = inside(mouseX, mouseY, this.closeX(), hy, HEAD_H, HEAD_H);
		fillRound(this.closeX(), hy, HEAD_H, HEAD_H, closeHover ? 0x70FFFFFF : PILL);
		this.drawCenteredString(this.fontRendererObj, "x", this.closeX() + HEAD_H / 2, hy + (HEAD_H - 8) / 2, TEXT);

		boolean modulesList = page == PAGE_MODULES && this.settingsModule == null;
		if (!modulesList) {
			String title = page == PAGE_HOME ? "Home" : (page == PAGE_SETTINGS ? "Settings" : this.settingsModule.getName());
			GlStateManager.pushMatrix();
			GlStateManager.translate((float) this.mainX(), (float) (hy + 3), 0.0F);
			GlStateManager.scale(1.3F, 1.3F, 1.0F);
			this.fontRendererObj.drawString(this.fit(title, 140), 0.0F, 0.0F, TEXT, false);
			GlStateManager.popMatrix();
			return;
		}

		// Search bar
		int sw = this.searchW();
		boolean sHover = inside(mouseX, mouseY, this.mainX(), hy, sw, HEAD_H);
		fillRound(this.mainX(), hy, sw, HEAD_H, sHover || !this.search.isEmpty() ? 0x78000000 : 0x60000000);
		if (this.search.isEmpty()) {
			this.drawString(this.fontRendererObj, "Search modules...", this.mainX() + 8, hy + (HEAD_H - 8) / 2, TEXT_FAINT);
		} else {
			String caret = ((now / 500L) % 2L == 0L) ? "_" : "";
			this.drawString(this.fontRendererObj, this.fit(this.search + caret, sw - 14), this.mainX() + 8,
					hy + (HEAD_H - 8) / 2, TEXT);
		}

		// Category pills
		for (int i = 0; i <= this.tabs.size(); ++i) {
			int px = this.pillX(i);
			int py = this.pillY(i);
			boolean on = i == selectedTab;
			boolean hover = inside(mouseX, mouseY, px, py, this.pillW(i), HEAD_H);
			fillRound(px, py, this.pillW(i), HEAD_H, on ? PILL_ON : (hover ? 0x50FFFFFF : PILL));
			this.drawCenteredString(this.fontRendererObj, this.tabLabel(i), px + this.pillW(i) / 2, py + (HEAD_H - 8) / 2, TEXT);
		}
	}

	private void drawHome(long now) {
		int x = this.mainX();
		int y = this.bodyY();
		int w = this.mainW();

		this.drawGlowText("MeteorMC", x + w / 2, y + 2, 2.5F, 1, now);
		this.drawCenteredString(this.fontRendererObj, "Welcome back", x + w / 2, y + 28, TEXT_DIM);

		int total = 0;
		int on = 0;
		for (MeteorModule m : MeteorModuleManager.getModules()) {
			total++;
			if (m.isEnabled()) {
				on++;
			}
		}
		int cw = (w - 2 * GAP) / 3;
		int cy = y + 46;
		this.drawStatCard(x, cy, cw, "MODS ON", String.valueOf(on));
		this.drawStatCard(x + cw + GAP, cy, cw, "MODS", String.valueOf(total));
		this.drawStatCard(x + 2 * (cw + GAP), cy, cw, "FPS", String.valueOf(this.homeFps));

		int ty = cy + 52;
		String[] tips = { "Click a card or its switch to turn a module on or off.", "Click the gear to change a module's options.",
				"Type in the search bar to find a module. Esc closes." };
		for (String tip : tips) {
			this.drawString(this.fontRendererObj, this.fit(tip, w), x, ty, TEXT_DIM);
			ty += 12;
		}
	}

	private void drawStatCard(int x, int y, int w, String label, String value) {
		this.panel(x, y, w, 40, CARD_TOP, CARD_BOTTOM, CARD_LINE);
		this.drawString(this.fontRendererObj, label, x + 8, y + 7, TEXT_FAINT);
		GlStateManager.pushMatrix();
		GlStateManager.translate((float) (x + 8), (float) (y + 19), 0.0F);
		GlStateManager.scale(1.8F, 1.8F, 1.0F);
		this.fontRendererObj.drawString(value, 0.0F, 0.0F, TEXT, false);
		GlStateManager.popMatrix();
	}

	private void drawGrid(int mouseX, int mouseY) {
		List<MeteorModule> list = this.filtered();
		int rows = (list.size() + this.cols() - 1) / this.cols();
		int vis = this.visibleRows();
		int maxScroll = Math.max(0, rows - vis);
		scrollRow = Math.max(0, Math.min(maxScroll, scrollRow));

		if (list.isEmpty()) {
			this.drawCenteredString(this.fontRendererObj, "No modules found", this.mainX() + this.mainW() / 2,
					this.winY + this.winH / 2, TEXT_DIM);
		}

		int cw = this.cardW();
		int start = scrollRow * this.cols();
		int end = Math.min(list.size(), (scrollRow + vis) * this.cols());
		for (int i = start; i < end; ++i) {
			int r = i / this.cols() - scrollRow;
			int c = i % this.cols();
			int x = this.mainX() + c * (cw + GAP);
			int y = this.bodyY() + r * (CARD_H + GAP);
			this.drawCard(list.get(i), x, y, cw, mouseX, mouseY);
		}

		if (rows > vis) {
			int sx = this.mainX() + this.gridW() + 3;
			int sy = this.bodyY();
			int sh = this.bodyH();
			Gui.drawRect(sx, sy, sx + 3, sy + sh, 0x28000000);
			int th = Math.max(16, sh * vis / rows);
			int ty = sy + (maxScroll == 0 ? 0 : (sh - th) * scrollRow / maxScroll);
			Gui.drawRect(sx, ty, sx + 3, ty + th, 0xC0FFFFFF);
		}
	}

	private void drawCard(MeteorModule m, int x, int y, int cw, int mouseX, int mouseY) {
		boolean hover = inside(mouseX, mouseY, x, y, cw, CARD_H);
		this.panel(x, y, cw, CARD_H, hover ? CARD_HOVER_TOP : CARD_TOP, hover ? CARD_HOVER_BOTTOM : CARD_BOTTOM,
				hover ? CARD_LINE_HOVER : CARD_LINE);

		this.drawString(this.fontRendererObj, this.fit(m.getName(), cw - 62), x + 8, y + 7, TEXT);
		this.drawSwitch(m, m.isEnabled(), x + cw - 42, y + 6);

		boolean hasSettings = !m.getSettings().isEmpty();
		boolean gearHover = hasSettings && inside(mouseX, mouseY, x + cw - 17, y + 4, 14, 16);
		bitmap(GEAR, x + cw - 14, y + 7, hasSettings ? (gearHover ? TEXT : TEXT_DIM) : 0x50FFFFFF);

		if (DESCRIPTIONS.getBoolean()) {
			List<String> lines = this.wrap(m.getDescription(), cw - 16, 2);
			int ly = y + 21;
			for (String line : lines) {
				this.drawString(this.fontRendererObj, this.fit(line, cw - 16), x + 8, ly, TEXT_DIM);
				ly += 10;
			}
		}
	}

	private void drawModuleSettings(int mouseX, int mouseY) {
		int x = this.mainX();
		int y = this.bodyY();

		boolean hb = inside(mouseX, mouseY, x, y, 44, 14);
		fillRound(x, y, 44, 14, hb ? 0x70FFFFFF : PILL);
		this.drawCenteredString(this.fontRendererObj, "< Back", x + 22, y + 3, TEXT);
		this.drawString(this.fontRendererObj, this.fit(this.settingsModule.getDescription(), this.mainW() - 54), x + 54,
				y + 3, TEXT_DIM);

		this.drawSettingRows(this.settingsModule.getSettings(), x, y + 22, this.mainW(), mouseX, mouseY);
	}

	private void drawSettingRows(List<MeteorSetting> list, int x, int y, int w, int mouseX, int mouseY) {
		int ry = y;
		for (MeteorSetting s : list) {
			int rh = rowHeight(s);
			boolean hover = inside(mouseX, mouseY, x, ry, w, rh);
			this.panel(x, ry, w, rh, hover ? CARD_HOVER_TOP : CARD_TOP, hover ? CARD_HOVER_BOTTOM : CARD_BOTTOM,
					hover ? CARD_LINE_HOVER : CARD_LINE);

			if (s.getType() == MeteorSetting.Type.BOOLEAN) {
				this.drawString(this.fontRendererObj, s.getName(), x + 8, ry + (rh - 8) / 2, TEXT);
				this.drawSwitch(s, s.getBoolean(), x + w - 30, ry + (rh - 11) / 2);
			} else {
				this.drawString(this.fontRendererObj, s.getName() + ": " + fmt(s.getDouble()), x + 8, ry + 5, TEXT);
				int tx0 = x + 8;
				int tw = w - 16;
				double t = (s.getDouble() - s.getMin()) / (s.getMax() - s.getMin());
				int fx = tx0 + (int) (tw * t);
				Gui.drawRect(tx0, ry + rh - 10, tx0 + tw, ry + rh - 7, 0x50000000);
				Gui.drawRect(tx0, ry + rh - 10, fx, ry + rh - 7, 0xFFFFFFFF);
				Gui.drawRect(fx - 1, ry + rh - 12, fx + 2, ry + rh - 5, 0xFFFFFFFF);
			}
			ry += rh + GAP;
		}
	}

	// ------------------------- Input -------------------------

	protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
		super.mouseClicked(mouseX, mouseY, button);
		this.layout();

		if (button != 0 && button != 1) {
			return;
		}

		if (button == 0) {
			if (inside(mouseX, mouseY, this.closeX(), this.headY(), HEAD_H, HEAD_H)) {
				this.mc.displayGuiScreen(this.parent);
				return;
			}
			if (this.navHit(mouseX, mouseY, this.navTopY(0))) {
				page = PAGE_HOME;
				this.settingsModule = null;
				return;
			}
			if (this.navHit(mouseX, mouseY, this.navTopY(1))) {
				page = PAGE_MODULES;
				this.settingsModule = null;
				return;
			}
			if (this.navHit(mouseX, mouseY, this.navBottomY(0))) {
				page = PAGE_SETTINGS;
				this.settingsModule = null;
				return;
			}
			if (this.navHit(mouseX, mouseY, this.navBottomY(1))) {
				this.mc.displayGuiScreen(new GuiMeteorCredits(this));
				return;
			}
		}

		if (page == PAGE_HOME) {
			return;
		}

		if (page == PAGE_SETTINGS) {
			if (button == 0) {
				this.clickSettingRows(this.settingsPageList(), this.mainX(), this.bodyY(), this.mainW(), mouseX, mouseY);
			}
			return;
		}

		if (this.settingsModule != null) {
			if (button == 0) {
				if (inside(mouseX, mouseY, this.mainX(), this.bodyY(), 44, 14)) {
					this.settingsModule = null;
				} else {
					this.clickSettingRows(this.settingsModule.getSettings(), this.mainX(), this.bodyY() + 22,
							this.mainW(), mouseX, mouseY);
				}
			}
			return;
		}

		// Category pills
		if (button == 0) {
			for (int i = 0; i <= this.tabs.size(); ++i) {
				if (inside(mouseX, mouseY, this.pillX(i), this.pillY(i), this.pillW(i), HEAD_H)) {
					selectedTab = i;
					scrollRow = 0;
					return;
				}
			}
		}

		List<MeteorModule> list = this.filtered();
		int rows = (list.size() + this.cols() - 1) / this.cols();
		int vis = this.visibleRows();

		// Scrollbar
		if (button == 0 && rows > vis
				&& inside(mouseX, mouseY, this.mainX() + this.gridW(), this.bodyY(), 12, this.bodyH())) {
			this.draggingScroll = true;
			this.updateScroll(mouseY);
			return;
		}

		// Cards
		int cw = this.cardW();
		int start = scrollRow * this.cols();
		int end = Math.min(list.size(), (scrollRow + vis) * this.cols());
		for (int i = start; i < end; ++i) {
			int r = i / this.cols() - scrollRow;
			int c = i % this.cols();
			int x = this.mainX() + c * (cw + GAP);
			int y = this.bodyY() + r * (CARD_H + GAP);
			if (inside(mouseX, mouseY, x, y, cw, CARD_H)) {
				MeteorModule m = list.get(i);
				boolean hasSettings = !m.getSettings().isEmpty();
				if (button == 1) {
					if (hasSettings) {
						this.settingsModule = m;
					}
				} else if (hasSettings && inside(mouseX, mouseY, x + cw - 17, y + 4, 14, 16)) {
					this.settingsModule = m;
				} else {
					m.toggle();
				}
				return;
			}
		}
	}

	private void clickSettingRows(List<MeteorSetting> list, int x, int y, int w, int mouseX, int mouseY) {
		int ry = y;
		for (MeteorSetting s : list) {
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
			} else if (page != PAGE_MODULES) {
				page = PAGE_MODULES;
			} else if (!this.search.isEmpty()) {
				this.search = "";
				scrollRow = 0;
			} else {
				this.mc.displayGuiScreen(this.parent);
			}
			return;
		}

		boolean gridShown = page == PAGE_MODULES && this.settingsModule == null;
		if (!gridShown) {
			return;
		}

		if (keyCode == KEY_UP || keyCode == KEY_DOWN) {
			scrollRow = Math.max(0, scrollRow + (keyCode == KEY_DOWN ? 1 : -1));
			return;
		}
		if (keyCode == KEY_BACKSPACE) {
			if (!this.search.isEmpty()) {
				this.search = this.search.substring(0, this.search.length() - 1);
				scrollRow = 0;
			}
			return;
		}
		if (typedChar >= 32 && typedChar != 127 && this.fontRendererObj.getStringWidth(this.search) < this.searchW() - 24) {
			this.search += typedChar;
			scrollRow = 0;
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
		int rows = (list.size() + this.cols() - 1) / this.cols();
		int vis = this.visibleRows();
		int maxScroll = Math.max(0, rows - vis);
		int sh = this.bodyH();
		int th = Math.max(16, sh * vis / Math.max(1, rows));
		double t = (mouseY - this.bodyY() - th / 2) / (double) Math.max(1, sh - th);
		t = Math.max(0.0D, Math.min(1.0D, t));
		scrollRow = (int) Math.round(t * maxScroll);
	}

	// ------------------------- Background (only used with no menu and no world behind) -------------------------

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
						(a << 24) | 0xFFFFFF);
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

		this.drawGradientRect(0, 0, this.width, this.height, 0x00FFFFFF, 0x30FFFFFF);
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
		VISUAL("Visual"), HUD("HUD"), COMBAT("Combat"), MOVEMENT("Movement"), PERFORMANCE("FPS"), MISC("Misc");

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
 *
 * HUD text modules stack down the left side of the screen in the order they
 * are registered (see nextHudY / hudLine).
 */
final class MeteorModuleManager {

	private static final List<MeteorModule> MODULES = new ArrayList<MeteorModule>();
	private static final MenuEffects MENU_FX = new MenuEffects();
	private static final int HUD_COLOR = 0xFFFFD83A;
	private static int hudY = 4;
	private static FpsBoost fpsBoost;
	private static boolean boostStarted;
	private static boolean bulk;

	static {
		register(MENU_FX);
		MENU_FX.setEnabled(true);

		// FPS
		MeteorModule[] tweaks = { new FastGraphics(), new NoClouds(), new MinimalParticles(), new NoShadows(),
				new FlatLighting() };
		fpsBoost = new FpsBoost(tweaks);
		register(fpsBoost);
		for (MeteorModule t : tweaks) {
			register(t);
		}

		// HUD
		registerOn(new Watermark());
		registerOn(new FpsDisplay());
		register(new Clock());
		register(new Coordinates());
		register(new Compass());
		register(new Keystrokes());
		register(new ArmorStatus());
		register(new HeldItem());
		register(new PotionStatus());
		register(new HealthDisplay());
		register(new HungerDisplay());
		register(new ArmorPoints());
		register(new DayCounter());
		register(new GameTime());
		register(new EntityCount());
		register(new PlayerCount());
		register(new SessionTimer());

		// Combat (information and quality of life only)
		register(new CpsCounter());
		register(new TargetHud());
		register(new TargetDistance());
		register(new AttackIndicator());
		register(new LowHealthWarning());
		register(new ArmorWarning());
		register(new ShowHitboxes());
		register(new ReachDisplay());
		register(new HitCounter());
		register(new DamageTaken());
		register(new CombatTimer());
		register(new CritIndicator());
		register(new AttackCooldown());
		register(new HitSound());
		register(new CritSound());
		register(new KillSound());
		register(new ComboSound());
		register(new CooldownSound());
		register(new LowHealthSound());
		register(new ArmorAlertSound());
		register(new PotionExpirySound());

		// Movement
		register(new ToggleSprint());
		register(new SprintStatus());
		register(new SpeedDisplay());
		register(new DistanceTraveled());
		register(new FallDistance());

		// Visual
		register(new Zoom());
		register(new NoRain());
		register(new TimeChanger());
	}

	private MeteorModuleManager() {
	}

	private static void register(MeteorModule module) {
		MODULES.add(module);
	}

	/** Registers a module and switches it on by default. */
	private static void registerOn(MeteorModule module) {
		MODULES.add(module);
		module.setEnabled(true);
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

	/** Snowfall over the menus (shown when the Snow option of Menu Effects is on). */
	static void drawSnow(int w, int h, long now) {
		if (w <= 0 || h <= 0) {
			return;
		}
		for (int i = 0; i < 70; ++i) {
			float speed = 0.012F + (i % 5) * 0.006F;
			int x = (i * 97) % w + (int) (Math.sin(now * 0.001D + i) * 6.0D);
			int y = (int) ((now * speed + i * 41) % (h + 8)) - 4;
			int size = (i % 4 == 0) ? 2 : 1;
			int a = 150 + (i % 4) * 28;
			Gui.drawRect(x, y, x + size, y + size, (a << 24) | 0xFFFFFF);
		}
	}

	/** Menu Effects options, shown on the ClickGUI Settings page. */
	static List<MeteorSetting> menuEffectSettings() {
		return MENU_FX.getSettings();
	}

	/**
	 * Called once per frame from GuiIngame.renderGameOverlay.
	 */
	public static void renderHud() {
		hudY = 4;
		if (!boostStarted) {
			boostStarted = true;
			try {
				fpsBoost.setEnabled(true);
			} catch (Throwable t) {
			}
		}
		GlStateManager.enableBlend();
		GlStateManager.enableAlpha();
		GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
				RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);
		for (MeteorModule m : MODULES) {
			if (m.isEnabled()) {
				try {
					m.onRender();
				} catch (Throwable t) {
					m.setEnabled(false);
				}
			}
		}
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/** Reserves a block of the left-side HUD stack and returns its top Y. */
	static int nextHudY(int height) {
		int y = hudY;
		hudY += height + 1;
		return y;
	}

	static void hudLine(String text, int color, boolean shadow) {
		drawHudText(text, 4, nextHudY(10), color, shadow, 1.0F);
	}

	private static void drawHudText(String text, int x, int y, int color, boolean shadow, float scale) {
		Minecraft mc = Minecraft.getMinecraft();
		GlStateManager.pushMatrix();
		GlStateManager.scale(scale, scale, 1.0F);
		mc.fontRendererObj.drawString(text, (float) x / scale, (float) y / scale, color, shadow);
		GlStateManager.popMatrix();
	}

	private static void drawCentered(String text, int cx, int y, int color, boolean shadow) {
		FontRenderer f = Minecraft.getMinecraft().fontRendererObj;
		f.drawString(text, (float) (cx - f.getStringWidth(text) / 2), (float) y, color, shadow);
	}

	private static String one(double v) {
		return String.valueOf(Math.round(v * 10.0D) / 10.0D);
	}

	/** Rebuilds the chunk meshes so graphics changes take effect. */
	private static void reloadChunks() {
		Minecraft m = Minecraft.getMinecraft();
		if (!bulk && m != null && m.world != null && m.renderGlobal != null) {
			m.renderGlobal.loadRenderers();
		}
	}

	/** Plays one of six built-in feedback sounds on your client only. */
	private static void playFx(int which, float pitch) {
		Minecraft m = Minecraft.getMinecraft();
		if (m == null) {
			return;
		}
		switch (which) {
		case 1:
			m.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_NOTE_PLING, pitch));
			break;
		case 2:
			m.getSoundHandler()
					.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pitch));
			break;
		case 3:
			m.getSoundHandler()
					.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_ARROW_HIT_PLAYER, pitch));
			break;
		case 4:
			m.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_PLAYER_LEVELUP, pitch));
			break;
		case 5:
			m.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_NOTE_HAT, pitch));
			break;
		default:
			m.getSoundHandler()
					.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, pitch));
			break;
		}
	}

	private static boolean blink() {
		return (Minecraft.getSystemTime() / 350L) % 2L == 0L;
	}

	// ------------------------- Base for simple text modules -------------------------

	private static abstract class HudText extends MeteorModule {
		final MeteorSetting shadow = addSetting(MeteorSetting.bool("Shadow", true));

		HudText(String name, String description, Category category) {
			super(name, description, category);
		}

		/** Return the line to show, or null to show nothing. */
		abstract String text(Minecraft m);

		int color(Minecraft m) {
			return HUD_COLOR;
		}

		public void onRender() {
			Minecraft m = mc();
			String t = text(m);
			if (t != null) {
				hudLine(t, color(m), shadow.getBoolean());
			}
		}
	}


	// ------------------------- FPS -------------------------

	/**
	 * Master FPS switch. Turns on every tweak below, lowers the render distance and
	 * (optionally) keeps adjusting it to chase your target FPS. It cannot go above
	 * what your device and display can do.
	 */
	private static class FpsBoost extends MeteorModule {
		private final MeteorSetting target = addSetting(MeteorSetting.number("Target FPS", 60.0D, 30.0D, 100.0D, 5.0D));
		private final MeteorSetting maxDist = addSetting(MeteorSetting.number("Max Distance", 6.0D, 2.0D, 12.0D, 1.0D));
		private final MeteorSetting auto = addSetting(MeteorSetting.bool("Auto Adjust", true));
		private final MeteorModule[] tweaks;
		private int savedDistance = 8;
		private int dist = 6;
		private int frames;
		private int fpsSum;
		private int samples;
		private long lastSecond;
		private long lastAdjust;
		private long lastLower;
		private long warmupUntil;

		FpsBoost(MeteorModule[] tweaks) {
			super("FPS Boost", "Lowers detail to raise your frame rate.", Category.PERFORMANCE);
			this.tweaks = tweaks;
		}

		protected void onEnable() {
			bulk = true;
			for (MeteorModule t : tweaks) {
				t.setEnabled(true);
			}
			bulk = false;
			savedDistance = mc().gameSettings.renderDistanceChunks;
			dist = (int) maxDist.getDouble();
			mc().gameSettings.renderDistanceChunks = dist;
			frames = 0;
			fpsSum = 0;
			samples = 0;
			lastSecond = Minecraft.getSystemTime();
			lastAdjust = lastSecond;
			warmupUntil = lastSecond + 4000L;
			reloadChunks();
		}

		protected void onDisable() {
			bulk = true;
			for (MeteorModule t : tweaks) {
				t.setEnabled(false);
			}
			bulk = false;
			mc().gameSettings.renderDistanceChunks = savedDistance;
			reloadChunks();
		}

		private void applyDistance() {
			mc().gameSettings.renderDistanceChunks = dist;
			reloadChunks();
			warmupUntil = Minecraft.getSystemTime() + 4000L;
			fpsSum = 0;
			samples = 0;
		}

		public void onRender() {
			long now = Minecraft.getSystemTime();
			frames++;
			if (now - lastSecond >= 1000L) {
				if (now >= warmupUntil) {
					fpsSum += frames;
					samples++;
				}
				frames = 0;
				lastSecond = now;
			}

			int cap = (int) maxDist.getDouble();
			if (!auto.getBoolean()) {
				if (dist != cap) {
					dist = cap;
					applyDistance();
				}
			} else if (dist > cap) {
				dist = cap;
				applyDistance();
			}

			if (auto.getBoolean() && now - lastAdjust >= 5000L && samples > 0) {
				int avg = fpsSum / samples;
				fpsSum = 0;
				samples = 0;
				lastAdjust = now;
				if (avg < target.getDouble() && dist > 2) {
					dist--;
					lastLower = now;
					applyDistance();
				} else if (avg > target.getDouble() + 40.0D && dist < cap && now - lastLower >= 30000L) {
					dist++;
					applyDistance();
				}
			}
		}
	}

	/** Fast (non-fancy) graphics: simpler leaves and blocks. */
	private static class FastGraphics extends MeteorModule {
		private boolean saved = true;

		FastGraphics() {
			super("Fast Graphics", "Simpler leaves and blocks.", Category.PERFORMANCE);
		}

		protected void onEnable() {
			saved = mc().gameSettings.fancyGraphics;
			mc().gameSettings.fancyGraphics = false;
			reloadChunks();
		}

		protected void onDisable() {
			mc().gameSettings.fancyGraphics = saved;
			reloadChunks();
		}
	}

	/** Hides clouds. */
	private static class NoClouds extends MeteorModule {
		private int saved = 2;

		NoClouds() {
			super("No Clouds", "Hides the clouds.", Category.PERFORMANCE);
		}

		protected void onEnable() {
			saved = mc().gameSettings.clouds;
			mc().gameSettings.clouds = 0;
		}

		protected void onDisable() {
			mc().gameSettings.clouds = saved;
		}
	}

	/** Minimal particles. */
	private static class MinimalParticles extends MeteorModule {
		private int saved = 0;

		MinimalParticles() {
			super("Minimal Particles", "Shows far fewer particles.", Category.PERFORMANCE);
		}

		protected void onEnable() {
			saved = mc().gameSettings.particleSetting;
			mc().gameSettings.particleSetting = 2;
		}

		protected void onDisable() {
			mc().gameSettings.particleSetting = saved;
		}
	}

	/** Turns off the shadows under mobs and players. */
	private static class NoShadows extends MeteorModule {
		private boolean saved = true;

		NoShadows() {
			super("No Entity Shadows", "Removes shadows under entities.", Category.PERFORMANCE);
		}

		protected void onEnable() {
			saved = mc().gameSettings.entityShadows;
			mc().gameSettings.entityShadows = false;
		}

		protected void onDisable() {
			mc().gameSettings.entityShadows = saved;
		}
	}

	/** Turns off smooth lighting. */
	private static class FlatLighting extends MeteorModule {
		private int saved = 1;

		FlatLighting() {
			super("Flat Lighting", "Turns off smooth lighting.", Category.PERFORMANCE);
		}

		protected void onEnable() {
			saved = mc().gameSettings.ambientOcclusion;
			mc().gameSettings.ambientOcclusion = 0;
			reloadChunks();
		}

		protected void onDisable() {
			mc().gameSettings.ambientOcclusion = saved;
			reloadChunks();
		}
	}

	// ------------------------- Visual -------------------------

	/** Stars, shooting meteors and embers on the menus. */
	private static class MenuEffects extends MeteorModule {
		final MeteorSetting stars = addSetting(MeteorSetting.bool("Sparkles", true));
		final MeteorSetting meteors = addSetting(MeteorSetting.bool("Shooting Stars", true));
		final MeteorSetting embers = addSetting(MeteorSetting.bool("Snow", true));

		MenuEffects() {
			super("Menu Effects", "Sparkles, shooting stars and snow on menus.", Category.VISUAL);
		}
	}

	/** Zooms in by lowering the field of view. Switch it on and off here. */
	private static class Zoom extends MeteorModule {
		private final MeteorSetting zoomFov = addSetting(MeteorSetting.number("Zoom FOV", 30.0D, 10.0D, 60.0D, 1.0D));
		private float savedFov = 70.0F;

		Zoom() {
			super("Zoom", "Zooms in. Turn off to reset.", Category.VISUAL);
		}

		protected void onEnable() {
			savedFov = mc().gameSettings.fovSetting;
			mc().gameSettings.fovSetting = zoomFov.getFloat();
		}

		protected void onDisable() {
			mc().gameSettings.fovSetting = savedFov;
		}

		public void onRender() {
			mc().gameSettings.fovSetting = zoomFov.getFloat();
		}
	}

	/** Hides rain and thunder on your screen (visual only). */
	private static class NoRain extends MeteorModule {
		NoRain() {
			super("No Rain", "Hides rain and thunder visuals.", Category.VISUAL);
		}

		public void onRender() {
			Minecraft m = mc();
			if (m.world != null) {
				m.world.setRainStrength(0.0F);
				m.world.setThunderStrength(0.0F);
			}
		}
	}

	/** Sets the time of day on your screen only. */
	private static class TimeChanger extends MeteorModule {
		private final MeteorSetting time = addSetting(MeteorSetting.number("Time", 6000.0D, 0.0D, 24000.0D, 1000.0D));

		TimeChanger() {
			super("Time Changer", "Pick the time of day you see.", Category.VISUAL);
		}

		public void onRender() {
			Minecraft m = mc();
			if (m.world != null) {
				m.world.setWorldTime((long) time.getDouble());
			}
		}
	}

	// ------------------------- HUD -------------------------

	/** Shows the client name in the corner. */
	private static class Watermark extends HudText {
		Watermark() {
			super("Watermark", "Shows the client name.", Category.HUD);
		}

		String text(Minecraft m) {
			return "MeteorMC";
		}

		int color(Minecraft m) {
			return 0xFFFFB020;
		}
	}

	/** Shows frames per second. */
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
			int y = nextHudY((int) (9.0F * scale.getFloat()) + 1);
			drawHudText("FPS: " + fps, 4, y, HUD_COLOR, shadow.getBoolean(), scale.getFloat());
		}
	}

	/** Shows the current time. */
	private static class Clock extends HudText {
		private final MeteorSetting hour24 = addSetting(MeteorSetting.bool("24 Hour", false));

		Clock() {
			super("Clock", "Shows the current time.", Category.HUD);
		}

		String text(Minecraft m) {
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
			return h + ":" + (min < 10 ? "0" : "") + min + suffix;
		}
	}

	/** Shows your block position and the direction you face. */
	private static class Coordinates extends HudText {
		private static final String[] DIRS = { "S", "W", "N", "E" };
		private final MeteorSetting direction = addSetting(MeteorSetting.bool("Direction", true));

		Coordinates() {
			super("Coordinates", "Shows your XYZ position.", Category.HUD);
		}

		String text(Minecraft m) {
			if (m.player == null) {
				return null;
			}
			int x = (int) Math.floor(m.player.posX);
			int y = (int) Math.floor(m.player.posY);
			int z = (int) Math.floor(m.player.posZ);
			String t = "XYZ: " + x + " / " + y + " / " + z;
			if (direction.getBoolean()) {
				int d = (int) Math.floor(m.player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
				t += "  " + DIRS[d];
			}
			return t;
		}
	}

	/** Shows which way you are facing, with eight directions and degrees. */
	private static class Compass extends HudText {
		private static final String[] DIRS = { "S", "SW", "W", "NW", "N", "NE", "E", "SE" };

		Compass() {
			super("Compass", "Shows your facing direction.", Category.HUD);
		}

		String text(Minecraft m) {
			if (m.player == null) {
				return null;
			}
			float yaw = ((m.player.rotationYaw % 360.0F) + 360.0F) % 360.0F;
			int d = (int) Math.floor(yaw / 45.0F + 0.5F) & 7;
			return "Facing: " + DIRS[d] + " (" + (int) yaw + ")";
		}
	}

	/** On-screen WASD and mouse button display. */
	private static class Keystrokes extends MeteorModule {
		private final MeteorSetting mouse = addSetting(MeteorSetting.bool("Mouse Buttons", true));

		Keystrokes() {
			super("Keystrokes", "Shows the keys you are pressing.", Category.HUD);
		}

		public void onRender() {
			Minecraft m = mc();
			int x = 4;
			int y = nextHudY(mouse.getBoolean() ? 58 : 40);
			drawKey(m.gameSettings.keyBindForward.isKeyDown(), "W", x + 20, y, 18, 18);
			drawKey(m.gameSettings.keyBindLeft.isKeyDown(), "A", x, y + 20, 18, 18);
			drawKey(m.gameSettings.keyBindBack.isKeyDown(), "S", x + 20, y + 20, 18, 18);
			drawKey(m.gameSettings.keyBindRight.isKeyDown(), "D", x + 40, y + 20, 18, 18);
			if (mouse.getBoolean()) {
				drawKey(m.gameSettings.keyBindAttack.isKeyDown(), "LMB", x, y + 40, 29, 16);
				drawKey(m.gameSettings.keyBindUseItem.isKeyDown(), "RMB", x + 31, y + 40, 27, 16);
			}
		}

		private void drawKey(boolean down, String label, int x, int y, int w, int h) {
			Gui.drawRect(x, y, x + w, y + h, down ? 0xD0FFC21F : 0x90000000);
			Gui.drawRect(x, y, x + w, y + 1, down ? 0xFFFFE680 : 0x607A6422);
			FontRenderer f = mc().fontRendererObj;
			f.drawString(label, (float) (x + (w - f.getStringWidth(label)) / 2), (float) (y + (h - 8) / 2),
					down ? 0xFF1A1208 : 0xFFF2EAD0, false);
		}
	}

	/** Armor pieces with durability. */
	private static class ArmorStatus extends MeteorModule {
		private final MeteorSetting numbers = addSetting(MeteorSetting.bool("Durability Numbers", true));

		ArmorStatus() {
			super("Armor Status", "Shows your armor and its durability.", Category.HUD);
		}

		public void onRender() {
			Minecraft m = mc();
			if (m.player == null) {
				return;
			}
			int count = 0;
			for (int slot = 0; slot < 4; ++slot) {
				if (!m.player.inventory.armorItemInSlot(slot).func_190926_b()) {
					count++;
				}
			}
			if (count == 0) {
				return;
			}

			int y0 = nextHudY(count * 18);
			RenderItem ri = m.getRenderItem();
			GlStateManager.enableRescaleNormal();
			RenderHelper.enableGUIStandardItemLighting();
			int row = 0;
			for (int slot = 3; slot >= 0; --slot) {
				ItemStack stack = m.player.inventory.armorItemInSlot(slot);
				if (stack.func_190926_b()) {
					continue;
				}
				int y = y0 + row * 18;
				ri.renderItemAndEffectIntoGUI(m.player, stack, 4, y);
				ri.renderItemOverlays(m.fontRendererObj, stack, 4, y);
				row++;
			}
			RenderHelper.disableStandardItemLighting();
			GlStateManager.disableRescaleNormal();

			if (numbers.getBoolean()) {
				row = 0;
				for (int slot = 3; slot >= 0; --slot) {
					ItemStack stack = m.player.inventory.armorItemInSlot(slot);
					if (stack.func_190926_b()) {
						continue;
					}
					if (stack.isItemStackDamageable()) {
						int max = stack.getMaxDamage();
						int left = max - stack.getItemDamage();
						int pct = max > 0 ? left * 100 / max : 100;
						int color = pct > 50 ? 0xFF55FF55 : (pct > 25 ? 0xFFFFAA00 : 0xFFFF5555);
						m.fontRendererObj.drawString(left + "/" + max, 24.0F, (float) (y0 + row * 18 + 4), color, true);
					}
					row++;
				}
			}
		}
	}

	/** Name and durability of the item in your hand. */
	private static class HeldItem extends HudText {
		HeldItem() {
			super("Held Item", "Shows the item you are holding.", Category.HUD);
		}

		String text(Minecraft m) {
			if (m.player == null) {
				return null;
			}
			ItemStack stack = m.player.inventory.getCurrentItem();
			if (stack.func_190926_b()) {
				return null;
			}
			String s = stack.getDisplayName();
			if (stack.isItemStackDamageable()) {
				int max = stack.getMaxDamage();
				s += " (" + (max - stack.getItemDamage()) + "/" + max + ")";
			}
			return s;
		}
	}

	/** Active potion effects with time left. */
	private static class PotionStatus extends MeteorModule {
		private final MeteorSetting shadow = addSetting(MeteorSetting.bool("Shadow", true));

		PotionStatus() {
			super("Potion Status", "Shows active potion effects.", Category.HUD);
		}

		public void onRender() {
			Minecraft m = mc();
			if (m.player == null) {
				return;
			}
			Collection<PotionEffect> effects = m.player.getActivePotionEffects();
			for (PotionEffect e : effects) {
				Potion p = e.getPotion();
				String t = I18n.format(p.getName()) + " " + (e.getAmplifier() + 1) + "  "
						+ StringUtils.ticksToElapsedTime(e.getDuration());
				hudLine(t, p.isBeneficial() ? 0xFF55FFFF : 0xFFFF7777, shadow.getBoolean());
			}
		}
	}

	/** Your health as a number. */
	private static class HealthDisplay extends HudText {
		HealthDisplay() {
			super("Health", "Shows your health as a number.", Category.HUD);
		}

		String text(Minecraft m) {
			if (m.player == null) {
				return null;
			}
			String t = "HP: " + one(m.player.getHealth());
			float abs = m.player.getAbsorptionAmount();
			if (abs > 0.0F) {
				t += " +" + one(abs);
			}
			return t;
		}

		int color(Minecraft m) {
			if (m.player == null) {
				return HUD_COLOR;
			}
			float h = m.player.getHealth();
			return h > 12.0F ? 0xFF55FF55 : (h > 6.0F ? 0xFFFFAA00 : 0xFFFF5555);
		}
	}

	/** Food level and saturation. */
	private static class HungerDisplay extends HudText {
		HungerDisplay() {
			super("Hunger", "Shows food level and saturation.", Category.HUD);
		}

		String text(Minecraft m) {
			if (m.player == null) {
				return null;
			}
			return "Food: " + m.player.getFoodStats().getFoodLevel() + "  Sat: "
					+ one(m.player.getFoodStats().getSaturationLevel());
		}
	}

	/** Total armor points. */
	private static class ArmorPoints extends HudText {
		ArmorPoints() {
			super("Armor Points", "Shows your total armor value.", Category.HUD);
		}

		String text(Minecraft m) {
			return m.player == null ? null : "Armor: " + m.player.getTotalArmorValue();
		}
	}

	/** In-game day number. */
	private static class DayCounter extends HudText {
		DayCounter() {
			super("Day Counter", "Shows the current world day.", Category.HUD);
		}

		String text(Minecraft m) {
			return m.world == null ? null : "Day " + (m.world.getTotalWorldTime() / 24000L + 1L);
		}
	}

	/** In-game time of day. */
	private static class GameTime extends HudText {
		GameTime() {
			super("Game Time", "Shows the in-game time of day.", Category.HUD);
		}

		String text(Minecraft m) {
			if (m.world == null) {
				return null;
			}
			long t = m.world.getTotalWorldTime() % 24000L;
			int hours = (int) ((t / 1000L + 6L) % 24L);
			int mins = (int) ((t % 1000L) * 60L / 1000L);
			return "Time: " + hours + ":" + (mins < 10 ? "0" : "") + mins;
		}
	}

	/** Number of loaded entities. */
	private static class EntityCount extends HudText {
		EntityCount() {
			super("Entity Count", "Shows how many entities are loaded.", Category.HUD);
		}

		String text(Minecraft m) {
			return m.world == null ? null : "Entities: " + m.world.loadedEntityList.size();
		}
	}

	/** Number of players on the server. */
	private static class PlayerCount extends HudText {
		PlayerCount() {
			super("Player Count", "Shows players online.", Category.HUD);
		}

		String text(Minecraft m) {
			if (m.player == null || m.player.connection == null) {
				return null;
			}
			return "Players: " + m.player.connection.getPlayerInfoMap().size();
		}
	}

	/** Time since you switched it on. */
	private static class SessionTimer extends HudText {
		private long start = Minecraft.getSystemTime();

		SessionTimer() {
			super("Session Timer", "Shows time since you turned it on.", Category.HUD);
		}

		protected void onEnable() {
			start = Minecraft.getSystemTime();
		}

		String text(Minecraft m) {
			long s = (Minecraft.getSystemTime() - start) / 1000L;
			long mm = s / 60L;
			long ss = s % 60L;
			return "Session: " + mm + ":" + (ss < 10L ? "0" : "") + ss;
		}
	}

	// ------------------------- Combat (info and quality of life) -------------------------

	/** Clicks per second, counted from your attack / use button presses. */
	private static class CpsCounter extends HudText {
		private final MeteorSetting rightClick = addSetting(MeteorSetting.bool("Right Click", false));
		private final List<Long> leftClicks = new ArrayList<Long>();
		private final List<Long> rightClicks = new ArrayList<Long>();
		private boolean lastLeft;
		private boolean lastRight;

		CpsCounter() {
			super("CPS Counter", "Shows your clicks per second.", Category.COMBAT);
		}

		String text(Minecraft m) {
			long now = Minecraft.getSystemTime();

			boolean left = m.gameSettings.keyBindAttack.isKeyDown();
			if (left && !lastLeft) {
				leftClicks.add(Long.valueOf(now));
			}
			lastLeft = left;

			boolean right = m.gameSettings.keyBindUseItem.isKeyDown();
			if (right && !lastRight) {
				rightClicks.add(Long.valueOf(now));
			}
			lastRight = right;

			while (!leftClicks.isEmpty() && now - leftClicks.get(0).longValue() > 1000L) {
				leftClicks.remove(0);
			}
			while (!rightClicks.isEmpty() && now - rightClicks.get(0).longValue() > 1000L) {
				rightClicks.remove(0);
			}

			String t = "CPS: " + leftClicks.size();
			if (rightClick.getBoolean()) {
				t += " | " + rightClicks.size();
			}
			return t;
		}
	}

	/** Health bar for the mob or player you are looking at. */
	private static class TargetHud extends MeteorModule {
		TargetHud() {
			super("Target HUD", "Shows health of what you look at.", Category.COMBAT);
		}

		public void onRender() {
			Minecraft m = mc();
			Entity e = m.pointedEntity;
			if (!(e instanceof EntityLivingBase)) {
				return;
			}
			EntityLivingBase t = (EntityLivingBase) e;
			int cx = m.scaledResolution.getScaledWidth() / 2;
			int y = m.scaledResolution.getScaledHeight() / 2 + 18;
			float hp = t.getHealth();
			float max = Math.max(1.0F, t.getMaxHealth());
			float frac = Math.max(0.0F, Math.min(1.0F, hp / max));

			drawCentered(t.getName(), cx, y, 0xFFF2EAD0, true);
			int bw = 60;
			int bx = cx - bw / 2;
			int by = y + 11;
			Gui.drawRect(bx - 1, by - 1, bx + bw + 1, by + 5, 0xC0000000);
			int fill = frac > 0.5F ? 0xFF55FF55 : (frac > 0.25F ? 0xFFFFAA00 : 0xFFFF5555);
			Gui.drawRect(bx, by, bx + (int) (bw * frac), by + 4, fill);
			drawCentered(one(hp) + " / " + one(max), cx, by + 7, HUD_COLOR, true);
		}
	}

	/** Distance to the mob or player you are looking at. */
	private static class TargetDistance extends HudText {
		TargetDistance() {
			super("Target Distance", "Shows distance to your target.", Category.COMBAT);
		}

		String text(Minecraft m) {
			Entity e = m.pointedEntity;
			if (e == null || m.player == null) {
				return null;
			}
			double dx = e.posX - m.player.posX;
			double dy = e.posY - m.player.posY;
			double dz = e.posZ - m.player.posZ;
			return "Target: " + one(Math.sqrt(dx * dx + dy * dy + dz * dz)) + " m";
		}
	}

	/** Turns on the vanilla crosshair attack-cooldown indicator. */
	private static class AttackIndicator extends MeteorModule {
		private int saved = 1;

		AttackIndicator() {
			super("Attack Indicator", "Shows the attack cooldown by the crosshair.", Category.COMBAT);
		}

		protected void onEnable() {
			saved = mc().gameSettings.attackIndicator;
			mc().gameSettings.attackIndicator = 1;
		}

		protected void onDisable() {
			mc().gameSettings.attackIndicator = saved;
		}
	}

	/** Flashing warning when your health is low. */
	private static class LowHealthWarning extends MeteorModule {
		private final MeteorSetting hearts = addSetting(MeteorSetting.number("Hearts", 3.0D, 1.0D, 8.0D, 0.5D));

		LowHealthWarning() {
			super("Low Health Warning", "Flashes a warning at low health.", Category.COMBAT);
		}

		public void onRender() {
			Minecraft m = mc();
			if (m.player == null || m.player.getHealth() > hearts.getFloat() * 2.0F || !blink()) {
				return;
			}
			drawCentered("LOW HEALTH", m.scaledResolution.getScaledWidth() / 2,
					m.scaledResolution.getScaledHeight() / 2 - 50, 0xFFFF5555, true);
		}
	}

	/** Flashing warning when a piece of armor is close to breaking. */
	private static class ArmorWarning extends MeteorModule {
		private final MeteorSetting percent = addSetting(MeteorSetting.number("Percent", 15.0D, 5.0D, 50.0D, 5.0D));

		ArmorWarning() {
			super("Armor Warning", "Warns when armor is nearly broken.", Category.COMBAT);
		}

		public void onRender() {
			Minecraft m = mc();
			if (m.player == null || !blink()) {
				return;
			}
			boolean low = false;
			for (int slot = 0; slot < 4; ++slot) {
				ItemStack stack = m.player.inventory.armorItemInSlot(slot);
				if (!stack.func_190926_b() && stack.isItemStackDamageable()) {
					int max = stack.getMaxDamage();
					int left = max - stack.getItemDamage();
					if (max > 0 && left * 100 < max * percent.getDouble()) {
						low = true;
					}
				}
			}
			if (low) {
				drawCentered("ARMOR LOW", m.scaledResolution.getScaledWidth() / 2,
						m.scaledResolution.getScaledHeight() / 2 - 40, 0xFFFFAA00, true);
			}
		}
	}


	/** Draws the vanilla hitbox outlines (same as pressing F3 + B). Visual only. */
	private static class ShowHitboxes extends MeteorModule {
		ShowHitboxes() {
			super("Show Hitboxes", "Outlines mob and player hitboxes.", Category.COMBAT);
		}

		protected void onEnable() {
			mc().getRenderManager().setDebugBoundingBox(true);
		}

		protected void onDisable() {
			mc().getRenderManager().setDebugBoundingBox(false);
		}
	}

	/** Distance of your last hit (approximate, measured to the target's center). */
	private static class ReachDisplay extends HudText {
		private boolean last;
		private boolean any;
		private double dist;

		ReachDisplay() {
			super("Reach Display", "Shows the distance of your last hit.", Category.COMBAT);
		}

		String text(Minecraft m) {
			if (m.player == null) {
				return null;
			}
			boolean down = m.gameSettings.keyBindAttack.isKeyDown();
			Entity e = m.pointedEntity;
			if (down && !last && e != null) {
				double dx = e.posX - m.player.posX;
				double dy = (e.posY + e.height / 2.0F) - (m.player.posY + m.player.getEyeHeight());
				double dz = e.posZ - m.player.posZ;
				dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
				any = true;
			}
			last = down;
			return any ? "Reach: " + one(dist) + " m" : "Reach: -";
		}
	}

	/** Counts your clicks on a target, with a combo that resets after 3 seconds. */
	private static class HitCounter extends HudText {
		private int hits;
		private int combo;
		private long lastHit;
		private boolean last;

		HitCounter() {
			super("Hit Counter", "Counts your hits and combo.", Category.COMBAT);
		}

		protected void onEnable() {
			hits = 0;
			combo = 0;
		}

		String text(Minecraft m) {
			long now = Minecraft.getSystemTime();
			boolean down = m.gameSettings.keyBindAttack.isKeyDown();
			if (down && !last && m.pointedEntity != null) {
				hits++;
				combo++;
				lastHit = now;
			}
			last = down;
			if (combo > 0 && now - lastHit > 3000L) {
				combo = 0;
			}
			return "Hits: " + hits + "  Combo: " + combo;
		}
	}

	/** Shows the last hit of damage you took and the total. */
	private static class DamageTaken extends HudText {
		private float lastHealth = -1.0F;
		private float lastDamage;
		private float total;

		DamageTaken() {
			super("Damage Taken", "Shows damage you have taken.", Category.COMBAT);
		}

		protected void onEnable() {
			lastHealth = -1.0F;
			lastDamage = 0.0F;
			total = 0.0F;
		}

		String text(Minecraft m) {
			if (m.player == null) {
				return null;
			}
			float h = m.player.getHealth();
			if (lastHealth >= 0.0F && h < lastHealth) {
				lastDamage = lastHealth - h;
				total += lastDamage;
			}
			lastHealth = h;
			return "Damage: -" + one(lastDamage) + "  Total: " + one(total);
		}
	}

	/** Countdown since your last hit or the last time you took damage. */
	private static class CombatTimer extends HudText {
		private final MeteorSetting seconds = addSetting(MeteorSetting.number("Seconds", 10.0D, 3.0D, 30.0D, 1.0D));
		private long lastEvent;
		private boolean last;
		private float lastHealth = -1.0F;

		CombatTimer() {
			super("Combat Timer", "Shows time left in combat.", Category.COMBAT);
		}

		private boolean inCombat(long now) {
			return lastEvent != 0L && now - lastEvent < (long) (seconds.getDouble() * 1000.0D);
		}

		String text(Minecraft m) {
			if (m.player == null) {
				return null;
			}
			long now = Minecraft.getSystemTime();
			boolean down = m.gameSettings.keyBindAttack.isKeyDown();
			if (down && !last && m.pointedEntity != null) {
				lastEvent = now;
			}
			last = down;
			float h = m.player.getHealth();
			if (lastHealth >= 0.0F && h < lastHealth) {
				lastEvent = now;
			}
			lastHealth = h;
			if (!inCombat(now)) {
				return "Combat: Safe";
			}
			long left = (long) (seconds.getDouble() * 1000.0D) - (now - lastEvent);
			return "Combat: " + (left / 1000L + 1L) + "s";
		}

		int color(Minecraft m) {
			return inCombat(Minecraft.getSystemTime()) ? 0xFFFF5555 : 0xFF55FF55;
		}
	}

	/** Shows CRIT when a hit right now would be a critical hit. */
	private static class CritIndicator extends MeteorModule {
		CritIndicator() {
			super("Crit Indicator", "Shows when your next hit would crit.", Category.COMBAT);
		}

		public void onRender() {
			Minecraft m = mc();
			if (m.player == null) {
				return;
			}
			boolean crit = m.player.fallDistance > 0.0F && !m.player.onGround && !m.player.isInWater()
					&& !m.player.isSprinting() && m.player.getCooledAttackStrength(0.0F) > 0.9F;
			if (crit) {
				drawCentered("CRIT", m.scaledResolution.getScaledWidth() / 2,
						m.scaledResolution.getScaledHeight() / 2 - 28, 0xFFFFAA00, true);
			}
		}
	}

	/** Your attack cooldown as a percentage. */
	private static class AttackCooldown extends HudText {
		AttackCooldown() {
			super("Attack Cooldown", "Shows your weapon cooldown.", Category.COMBAT);
		}

		String text(Minecraft m) {
			if (m.player == null) {
				return null;
			}
			return "Cooldown: " + Math.round(m.player.getCooledAttackStrength(0.0F) * 100.0F) + "%";
		}

		int color(Minecraft m) {
			return m.player != null && m.player.getCooledAttackStrength(0.0F) >= 1.0F ? 0xFF55FF55 : HUD_COLOR;
		}
	}


	// ------------------------- Combat feedback sounds (client side only, no HUD) -------------------------

	/**
	 * Base for the sound mods. Each has a Sound option (1 Pling, 2 XP orb, 3 Ding, 4 Level up, 5 Tick,
	 * 6 Crit) and a Pitch option. "Hit" means you pressed attack while aiming at something.
	 */
	private static abstract class SoundMod extends MeteorModule {
		final MeteorSetting sound;
		final MeteorSetting pitch;
		private boolean lastDown;

		SoundMod(String name, String description, int defaultSound) {
			super(name, description, Category.COMBAT);
			this.sound = addSetting(MeteorSetting.number("Sound", defaultSound, 1.0D, 6.0D, 1.0D));
			this.pitch = addSetting(MeteorSetting.number("Pitch", 1.0D, 0.5D, 2.0D, 0.1D));
		}

		void beep() {
			playFx((int) sound.getDouble(), pitch.getFloat());
		}

		/** True on the frame you press attack while aiming at something. */
		boolean hitClick(Minecraft m) {
			boolean down = m.gameSettings.keyBindAttack.isKeyDown();
			boolean hit = down && !lastDown && m.pointedEntity != null;
			lastDown = down;
			return hit;
		}
	}

	/** A sound every time you click on a target. */
	private static class HitSound extends SoundMod {
		HitSound() {
			super("Hit Sound", "Plays a sound when you hit a target.", 2);
		}

		public void onRender() {
			if (hitClick(mc())) {
				beep();
			}
		}
	}

	/** A different sound when the hit would be a critical. */
	private static class CritSound extends SoundMod {
		CritSound() {
			super("Crit Sound", "Plays a sound on critical hits.", 6);
		}

		public void onRender() {
			Minecraft m = mc();
			if (m.player == null) {
				return;
			}
			boolean crit = m.player.fallDistance > 0.0F && !m.player.onGround && !m.player.isInWater()
					&& !m.player.isSprinting() && m.player.getCooledAttackStrength(0.0F) > 0.9F;
			if (hitClick(m) && crit) {
				beep();
			}
		}
	}

	/** A sound when the last target you hit dies. */
	private static class KillSound extends SoundMod {
		private EntityLivingBase lastTarget;
		private long lastHit;

		KillSound() {
			super("Kill Sound", "Plays a sound when your target dies.", 4);
		}

		protected void onDisable() {
			lastTarget = null;
		}

		public void onRender() {
			Minecraft m = mc();
			long now = Minecraft.getSystemTime();
			if (hitClick(m) && m.pointedEntity instanceof EntityLivingBase) {
				lastTarget = (EntityLivingBase) m.pointedEntity;
				lastHit = now;
			}
			if (lastTarget != null) {
				if (now - lastHit > 5000L) {
					lastTarget = null;
				} else if (lastTarget.getHealth() <= 0.0F || lastTarget.isDead) {
					beep();
					lastTarget = null;
				}
			}
		}
	}

	/** A sound every few hits in a row. */
	private static class ComboSound extends SoundMod {
		private final MeteorSetting every = addSetting(MeteorSetting.number("Every Hits", 5.0D, 2.0D, 10.0D, 1.0D));
		private int combo;
		private long lastHit;

		ComboSound() {
			super("Combo Sound", "Plays a sound every few hits in a row.", 1);
		}

		protected void onEnable() {
			combo = 0;
		}

		public void onRender() {
			long now = Minecraft.getSystemTime();
			if (combo > 0 && now - lastHit > 3000L) {
				combo = 0;
			}
			if (hitClick(mc())) {
				combo++;
				lastHit = now;
				if (combo % (int) every.getDouble() == 0) {
					beep();
				}
			}
		}
	}

	/** A quiet tick when your weapon is ready to swing again. */
	private static class CooldownSound extends SoundMod {
		private float prev = 1.0F;

		CooldownSound() {
			super("Cooldown Sound", "Ticks when your weapon is fully charged.", 5);
		}

		public void onRender() {
			Minecraft m = mc();
			if (m.player == null) {
				return;
			}
			float now = m.player.getCooledAttackStrength(0.0F);
			if (prev < 1.0F && now >= 1.0F) {
				beep();
			}
			prev = now;
		}
	}

	/** A repeating beep while your health is low. */
	private static class LowHealthSound extends SoundMod {
		private final MeteorSetting hearts = addSetting(MeteorSetting.number("Hearts", 3.0D, 1.0D, 8.0D, 0.5D));
		private long lastBeep;

		LowHealthSound() {
			super("Low Health Sound", "Beeps while your health is low.", 1);
		}

		public void onRender() {
			Minecraft m = mc();
			long now = Minecraft.getSystemTime();
			if (m.player != null && m.player.getHealth() <= hearts.getFloat() * 2.0F && now - lastBeep >= 1500L) {
				lastBeep = now;
				beep();
			}
		}
	}

	/** A beep when a piece of armor is close to breaking. */
	private static class ArmorAlertSound extends SoundMod {
		private final MeteorSetting percent = addSetting(MeteorSetting.number("Percent", 15.0D, 5.0D, 50.0D, 5.0D));
		private long lastBeep;

		ArmorAlertSound() {
			super("Armor Alert Sound", "Beeps when armor is nearly broken.", 5);
		}

		public void onRender() {
			Minecraft m = mc();
			long now = Minecraft.getSystemTime();
			if (m.player == null || now - lastBeep < 3000L) {
				return;
			}
			for (int slot = 0; slot < 4; ++slot) {
				ItemStack stack = m.player.inventory.armorItemInSlot(slot);
				if (!stack.func_190926_b() && stack.isItemStackDamageable()) {
					int max = stack.getMaxDamage();
					int left = max - stack.getItemDamage();
					if (max > 0 && left * 100 < max * percent.getDouble()) {
						lastBeep = now;
						beep();
						return;
					}
				}
			}
		}
	}

	/** A sound when one of your potion effects runs out. */
	private static class PotionExpirySound extends SoundMod {
		private int lastCount = -1;

		PotionExpirySound() {
			super("Potion Expiry Sound", "Plays a sound when an effect ends.", 3);
		}

		protected void onEnable() {
			lastCount = -1;
		}

		public void onRender() {
			Minecraft m = mc();
			if (m.player == null) {
				return;
			}
			int count = m.player.getActivePotionEffects().size();
			if (lastCount >= 0 && count < lastCount) {
				beep();
			}
			lastCount = count;
		}
	}

	// ------------------------- Movement -------------------------

	/** Keeps you sprinting while you hold forward. */
	private static class ToggleSprint extends MeteorModule {
		ToggleSprint() {
			super("Toggle Sprint", "Sprints automatically while moving forward.", Category.MOVEMENT);
		}

		public void onRender() {
			Minecraft m = mc();
			if (m.player == null) {
				return;
			}
			if (m.gameSettings.keyBindForward.isKeyDown() && !m.player.isSneaking()
					&& !m.player.isCollidedHorizontally && m.player.getFoodStats().getFoodLevel() > 6) {
				m.player.setSprinting(true);
			}
		}
	}

	/** Shows whether you are sprinting, sneaking or walking. */
	private static class SprintStatus extends HudText {
		SprintStatus() {
			super("Sprint Status", "Shows sprint / sneak state.", Category.MOVEMENT);
		}

		String text(Minecraft m) {
			if (m.player == null) {
				return null;
			}
			if (m.player.isSneaking()) {
				return "[Sneaking]";
			}
			return m.player.isSprinting() ? "[Sprinting]" : "[Walking]";
		}
	}

	/** Horizontal speed in blocks per second. */
	private static class SpeedDisplay extends HudText {
		private double lastX;
		private double lastZ;
		private long lastTime;
		private double speed;
		private boolean init;

		SpeedDisplay() {
			super("Speed", "Shows your speed in blocks per second.", Category.MOVEMENT);
		}

		String text(Minecraft m) {
			if (m.player == null) {
				return null;
			}
			long now = Minecraft.getSystemTime();
			if (!init) {
				lastX = m.player.posX;
				lastZ = m.player.posZ;
				lastTime = now;
				init = true;
			}
			if (now - lastTime >= 500L) {
				double dx = m.player.posX - lastX;
				double dz = m.player.posZ - lastZ;
				speed = Math.sqrt(dx * dx + dz * dz) / ((now - lastTime) / 1000.0D);
				lastX = m.player.posX;
				lastZ = m.player.posZ;
				lastTime = now;
			}
			return "Speed: " + one(speed) + " b/s";
		}
	}

	/** Blocks walked since you switched it on. */
	private static class DistanceTraveled extends HudText {
		private double lastX;
		private double lastZ;
		private double total;
		private boolean init;

		DistanceTraveled() {
			super("Distance Traveled", "Counts blocks you have moved.", Category.MOVEMENT);
		}

		protected void onEnable() {
			total = 0.0D;
			init = false;
		}

		String text(Minecraft m) {
			if (m.player == null) {
				return null;
			}
			if (init) {
				double dx = m.player.posX - lastX;
				double dz = m.player.posZ - lastZ;
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < 20.0D) {
					total += d;
				}
			}
			lastX = m.player.posX;
			lastZ = m.player.posZ;
			init = true;
			return "Moved: " + (int) total + " m";
		}
	}

	/** How far you have fallen. */
	private static class FallDistance extends HudText {
		FallDistance() {
			super("Fall Distance", "Shows how far you are falling.", Category.MOVEMENT);
		}

		String text(Minecraft m) {
			return m.player == null ? null : "Fall: " + one(m.player.fallDistance);
		}
	}
}
