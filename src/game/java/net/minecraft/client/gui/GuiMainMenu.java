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
 * MeteorMC ClickGUI: sidebar of categories, a page title with a search box, and
 * a grid of module cards (gear icon for settings, ON/OFF badge to toggle).
 *
 * Click a card (or its badge): toggle the module.
 * Click the gear (or right click a card): open its settings.
 * Sidebar: pick a category, or open the Settings page.
 * Type to search. Up / Down arrows or the scrollbar scroll the grid.
 * Esc: back / clear search / close.
 */
class ClickGuiScreen extends GuiScreen {

	// ===================== STYLE =====================
	private static final String TITLE = "REFLEX";

	private static final int WIN_W = 1236;
	private static final int WIN_H = 604;
	private static final int SB_W = 250;
	private static final int ROW_H = 38;
	private static final int COLS = 2;
	private static final int GAP = 9;
	private static final int CARD_H = 92;
	private static final int SEARCH_W = 380;

	private static final int BG_TOP = 0xFF171819;
	private static final int BG_BOTTOM = 0xFF292B2D;

	private static final int BORDER = 0xFF686B6E;
	private static final int BORDER_HOVER = 0xFFBFC2C5;
	private static final int CARD_BORDER = 0xFF55585B;
	private static final int CARD_TOP = 0xF05B5E61;
	private static final int CARD_BOTTOM = 0xF047494B;
	private static final int CARD_HOVER_TOP = 0xF06A6D70;
	private static final int CARD_HOVER_BOTTOM = 0xF0525558;

	private static final int GOLD = 0xFFF1F2F3;
	private static final int TEXT = 0xFFF4F4F4;
	private static final int TEXT_DIM = 0xFFC4C6C8;

	private static final int TITLE_COLOR_A = 0xFFE6E8EA;
	private static final int TITLE_COLOR_B = 0xFFFFFFFF;
	private static final int TITLE_AURA = 0x70777B;

	private static final int METEOR_OUTER = 0xFFBFC2C5;
	private static final int METEOR_CORE = 0xFFFFFFFF;
	private static final int FLAME_HEAD = 0xE0D7DADD;
	private static final int FLAME_TAIL = 0x203A3C3E;

	private static final int STAR_COUNT = 8;
	private static final int METEOR_COUNT = 0;

	private static final String[] GEAR = { "#..##..#", ".######.", "..#..#..", "###..###", "###..###", "..#..#..",
			".######.", "#..##..#" };
	// =================================================

	private static final int KEY_ESCAPE = 1;
	private static final int KEY_BACKSPACE = 14;
	private static final int KEY_UP = 200;
	private static final int KEY_DOWN = 208;

	/** Options on the Settings page. */
	private static final MeteorSetting TOOLTIPS = MeteorSetting.bool("Tooltips", true);
	private static final MeteorSetting HEADER_GLOW = MeteorSetting.bool("Header Glow", true);

	/** Remembered between openings. */
	private static int selectedTab = 0;
	private static int scrollRow = 0;
	private static boolean settingsPage = false;
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
		this.winW = Math.min(WIN_W, this.width - 24);
		this.winH = Math.min(WIN_H, this.height - 24);
		this.winX = (this.width - this.winW) / 2;
		this.winY = (this.height - this.winH) / 2;
	}

	private int mainX() {
		return this.winX + SB_W + 10;
	}

	private int mainW() {
		return this.winW - SB_W - 20;
	}

	private int contentY() {
		return this.winY + 108;
	}

	private int contentH() {
		return this.winH - 118 - 12;
	}

	private int gridW() {
		return this.mainW() - 8;
	}

	private int cardW() {
		return (this.gridW() - GAP * (COLS - 1)) / COLS;
	}

	private int visibleRows() {
		return Math.max(1, (this.contentH() + GAP) / (CARD_H + GAP));
	}

	private int searchX() {
		return this.mainX() + 4;
	}

	private int sbRowY(int i) {
		return this.winY + 76 + i * ROW_H;
	}

	private int settingsRowY() {
		return this.winY + this.winH - 94;
	}

	private boolean inDone(int mx, int my) {
		return inside(mx, my, this.winX + 8, this.winY + this.winH - 26, SB_W - 16, 18);
	}

	private String tabLabel(int i) {
		return i == 0 ? "All" : this.tabs.get(i - 1).getLabel();
	}

	private String pageTitle() {
		if (this.settingsModule != null) {
			return this.settingsModule.getName();
		}
		if (settingsPage) {
			return "Settings";
		}
		return selectedTab == 0 ? "All Mods" : this.tabLabel(selectedTab);
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
		list.add(TOOLTIPS);
		list.add(HEADER_GLOW);
		return list;
	}

	private static int rowHeight(MeteorSetting s) {
		return s.getType() == MeteorSetting.Type.BOOLEAN ? 24 : 32;
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

	// ------------------------- Drawing -------------------------

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.layout();
		long now = Minecraft.getSystemTime();

		this.drawSpaceBackground(now);
		this.tooltip = null;
		this.drawWindow(mouseX, mouseY, now);

		super.drawScreen(mouseX, mouseY, partialTicks);

		if (this.tooltip != null && TOOLTIPS.getBoolean()) {
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

		// Sidebar
		Gui.drawRect(this.winX, this.winY, this.winX + SB_W, this.winY + this.winH, 0x40333638);
		Gui.drawRect(this.winX + SB_W, this.winY + 20, this.winX + SB_W + 1, this.winY + this.winH - 20, 0xFF85888A);
		this.drawHeaderTitle(now);
		Gui.drawRect(this.winX + 20, this.winY + 64, this.winX + SB_W - 20, this.winY + 65, 0xFF777A7D);

		for (int i = 0; i <= this.tabs.size(); ++i) {
			this.drawSidebarRow(this.tabLabel(i), this.sbRowY(i), !settingsPage && i == selectedTab, mouseX, mouseY);
		}
		int sepY = this.sbRowY(this.tabs.size() + 1) + 4;
		Gui.drawRect(this.winX + 8, sepY, this.winX + SB_W - 8, sepY + 1, CARD_BORDER);
		this.drawSidebarRow("Settings", this.settingsRowY(), settingsPage, mouseX, mouseY);

		boolean doneHover = this.inDone(mouseX, mouseY);
		int dx = this.winX + 8;
		int dy = this.winY + this.winH - 26;
		Gui.drawRect(dx, dy, dx + SB_W - 16, dy + 18, doneHover ? BORDER_HOVER : BORDER);
		this.drawGradientRect(dx + 1, dy + 1, dx + SB_W - 17, dy + 17, doneHover ? CARD_HOVER_TOP : CARD_TOP,
					doneHover ? CARD_HOVER_BOTTOM : CARD_BOTTOM);
		this.drawCenteredString(this.fontRendererObj, "Done", dx + (SB_W - 16) / 2, dy + 5, doneHover ? GOLD : TEXT);

		// Main header: page title + search box
		GlStateManager.pushMatrix();
		GlStateManager.translate((float) this.mainX(), (float) (this.winY + 10), 0.0F);
		GlStateManager.scale(1.3F, 1.3F, 1.0F);
		this.fontRendererObj.drawString(this.fit(this.pageTitle(), 120), 0.0F, 0.0F, TEXT, true);
		GlStateManager.popMatrix();

		int sx = this.searchX();
		int sy = this.winY + 50;
		boolean sHover = inside(mouseX, mouseY, sx, sy, SEARCH_W, 44);
		Gui.drawRect(sx, sy, sx + SEARCH_W, sy + 44, (sHover || !this.search.isEmpty()) ? 0xFF85888A : 0xFF505356);
		Gui.drawRect(sx + 1, sy + 1, sx + SEARCH_W - 1, sy + 43, 0xFF4B4E50);
		if (this.search.isEmpty()) {
			this.drawString(this.fontRendererObj, "⌕  Search modules...", sx + 16, sy + 16, TEXT_DIM);
		} else {
			String caret = ((now / 500L) % 2L == 0L) ? "_" : "";
			this.drawString(this.fontRendererObj, this.search + caret, sx + 16, sy + 16, TEXT);
		}
		this.drawTopTabs(mouseX, mouseY);
		Gui.drawRect(this.mainX() - 2, this.winY + 102, this.winX + this.winW - 16, this.winY + 103, 0xFF777A7D);

		if (settingsPage) {
			this.drawSettingRows(this.settingsPageList(), this.mainX(), this.contentY(), this.mainW(), mouseX, mouseY);
		} else if (this.settingsModule != null) {
			this.drawModuleSettings(mouseX, mouseY);
		} else {
			this.drawGrid(mouseX, mouseY);
		}
	}

	private void drawSidebarRow(String label, int y, boolean selected, int mouseX, int mouseY) {
		boolean hover = inside(mouseX, mouseY, this.winX + 4, y, SB_W - 8, ROW_H);
		if (selected) {
			Gui.drawRect(this.winX + 4, y, this.winX + SB_W - 4, y + ROW_H, 0x609DA0A3);
			Gui.drawRect(this.winX + 4, y, this.winX + 6, y + ROW_H, 0xFFE4E6E8);
		} else if (hover) {
			Gui.drawRect(this.winX + 4, y, this.winX + SB_W - 4, y + ROW_H, 0x20FFFFFF);
		}
		this.drawString(this.fontRendererObj, label, this.winX + 14, y + 4,
					selected ? GOLD : (hover ? TEXT : TEXT_DIM));
	}

	private void drawGrid(int mouseX, int mouseY) {
		List<MeteorModule> list = this.filtered();
		int rows = (list.size() + COLS - 1) / COLS;
		int vis = this.visibleRows();
		int maxScroll = Math.max(0, rows - vis);
		scrollRow = Math.max(0, Math.min(maxScroll, scrollRow));

		if (list.isEmpty()) {
			this.drawCenteredString(this.fontRendererObj, "No modules found", this.mainX() + this.mainW() / 2,
					this.winY + this.winH / 2, TEXT_DIM);
		}

		int cw = this.cardW();
		int start = scrollRow * COLS;
		int end = Math.min(list.size(), (scrollRow + vis) * COLS);
		for (int i = start; i < end; ++i) {
			int r = i / COLS - scrollRow;
			int c = i % COLS;
			int x = this.mainX() + c * (cw + GAP);
			int y = this.contentY() + r * (CARD_H + GAP);
			this.drawCard(list.get(i), x, y, cw, mouseX, mouseY);
		}

		int sx = this.mainX() + this.gridW() + 2;
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
		boolean on = m.isEnabled();
		int border = on ? BORDER_HOVER : (hover ? BORDER : CARD_BORDER);
		Gui.drawRect(x, y, x + cw, y + CARD_H, border);
		this.drawGradientRect(x + 1, y + 1, x + cw - 1, y + CARD_H - 1, hover ? CARD_HOVER_TOP : CARD_TOP,
					hover ? CARD_HOVER_BOTTOM : CARD_BOTTOM);

		this.drawString(this.fontRendererObj, this.fit(m.getName(), cw - 72), x + 18, y + 15, TEXT);
		String desc = m.getDescription();
		this.drawString(this.fontRendererObj, this.fit(desc == null ? "" : desc, cw - 28), x + 18, y + 52, TEXT_DIM);

		boolean hasSettings = !m.getSettings().isEmpty();
		boolean gearHover = hasSettings && inside(mouseX, mouseY, x + cw - 31, y + 15, 14, 14);
		this.drawGear(x + 7, y + CARD_H - 14, hasSettings ? (gearHover ? GOLD : TEXT_DIM) : 0xFF4A4432);
		this.drawBadge(m, on, x + cw - 31, y + CARD_H - 16);

		if (hover) {
			this.tooltip = m.getDescription();
			this.tooltipX = mouseX;
			this.tooltipY = mouseY;
		}
	}

	private void drawGear(int px, int py, int color) {
		for (int r = 0; r < GEAR.length; ++r) {
			for (int c = 0; c < GEAR[r].length(); ++c) {
				if (GEAR[r].charAt(c) == '#') {
					Gui.drawRect(px + c, py + r, px + c + 1, py + r + 1, color);
				}
			}
		}
	}

	/** Animated ON / OFF badge. */
	private void drawBadge(Object key, boolean on, int bx, int by) {
		float t = this.anim(key, on);
		int bg = lerpColor(0xFF8A8D90, 0xFFF7F7F7, t);
		int bd = lerpColor(0xFF777A7D, 0xFFEDEEEF, t);
		Gui.drawRect(bx, by, bx + 26, by + 11, bd);
		Gui.drawRect(bx + 1, by + 1, bx + 25, by + 10, bg);
		String s = on ? "ON" : "OFF";
		int tw = this.fontRendererObj.getStringWidth(s);
		this.drawString(this.fontRendererObj, s, bx + (26 - tw) / 2, by + 2, on ? 0xFF2A1800 : TEXT_DIM);
	}

	private float anim(Object key, boolean on) {
		float target = on ? 1.0F : 0.0F;
		Float f = ANIM.get(key);
		float cur = f == null ? target : f.floatValue();
		cur += (target - cur) * 0.35F;
		ANIM.put(key, Float.valueOf(cur));
		return cur;
	}

	private void drawModuleSettings(int mouseX, int mouseY) {
		int x = this.mainX();
		int y = this.contentY();

		boolean hb = inside(mouseX, mouseY, x, y, 44, 14);
		Gui.drawRect(x, y, x + 44, y + 14, hb ? BORDER_HOVER : BORDER);
		this.drawGradientRect(x + 1, y + 1, x + 43, y + 13, hb ? CARD_HOVER_TOP : CARD_TOP,
					hb ? CARD_HOVER_BOTTOM : CARD_BOTTOM);
		this.drawCenteredString(this.fontRendererObj, "< Back", x + 22, y + 3, hb ? GOLD : TEXT);
		this.drawString(this.fontRendererObj, this.settingsModule.getDescription(), x + 54, y + 3, TEXT_DIM);

		this.drawSettingRows(this.settingsModule.getSettings(), x, y + 22, this.mainW(), mouseX, mouseY);
	}

	private void drawSettingRows(List<MeteorSetting> list, int x, int y, int w, int mouseX, int mouseY) {
		int ry = y;
		for (MeteorSetting s : list) {
			int rh = rowHeight(s);
			boolean hover = inside(mouseX, mouseY, x, ry, w, rh);
			Gui.drawRect(x, ry, x + w, ry + rh, hover ? BORDER : CARD_BORDER);
			this.drawGradientRect(x + 1, ry + 1, x + w - 1, ry + rh - 1, hover ? CARD_HOVER_TOP : CARD_TOP,
					hover ? CARD_HOVER_BOTTOM : CARD_BOTTOM);

			if (s.getType() == MeteorSetting.Type.BOOLEAN) {
				this.drawString(this.fontRendererObj, s.getName(), x + 8, ry + (rh - 8) / 2, TEXT);
				this.drawBadge(s, s.getBoolean(), x + w - 34, ry + (rh - 11) / 2);
			} else {
				this.drawString(this.fontRendererObj, s.getName() + ": " + fmt(s.getDouble()), x + 8, ry + 5, TEXT);
				int tx0 = x + 8;
				int tw = w - 16;
				double t = (s.getDouble() - s.getMin()) / (s.getMax() - s.getMin());
				int fx = tx0 + (int) (tw * t);
				Gui.drawRect(tx0, ry + rh - 11, tx0 + tw, ry + rh - 8, 0xFF2A220C);
				Gui.drawRect(tx0, ry + rh - 11, fx, ry + rh - 8, GOLD);
				Gui.drawRect(fx - 1, ry + rh - 13, fx + 2, ry + rh - 6, 0xFFFFFFFF);
			}
			ry += rh + GAP;
		}
	}

	/** Small animated glowing title at the top of the sidebar. */
	private void drawHeaderTitle(long now) {
		float scale = 1.0F;
		int len = TITLE.length();

		GlStateManager.pushMatrix();
		int titleWidth = this.fontRendererObj.getStringWidth(TITLE);
		GlStateManager.translate((float) (this.winX + (this.winW - titleWidth) / 2), (float) (this.winY + 18), 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
				RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);

		if (!HEADER_GLOW.getBoolean()) {
			this.fontRendererObj.drawString(TITLE, 0.0F, 0.0F, GOLD, true);
			GlStateManager.popMatrix();
			return;
		}

		float sweep = ((now % 2600L) / 2600.0F) * (len + 6) - 3.0F;
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

		// Sidebar
		if (button == 0) {
			for (int i = 0; i <= this.tabs.size(); ++i) {
				if (inside(mouseX, mouseY, this.winX + 4, this.sbRowY(i), SB_W - 8, ROW_H)) {
					selectedTab = i;
					scrollRow = 0;
					settingsPage = false;
					this.settingsModule = null;
					return;
				}
			}
			if (inside(mouseX, mouseY, this.winX + 4, this.settingsRowY(), SB_W - 8, ROW_H)) {
				settingsPage = true;
				this.settingsModule = null;
				return;
			}
			if (this.inDone(mouseX, mouseY)) {
				this.mc.displayGuiScreen(this.parent);
				return;
			}
		}

		if (button == 0 && mouseY >= this.winY + 50 && mouseY < this.winY + 94) {
			int tx = this.searchX() + SEARCH_W + 10;
			for (int i = 0; i <= this.tabs.size(); ++i) {
				String label = this.tabLabel(i);
				int tw = Math.max(66, this.fontRendererObj.getStringWidth(label) + 18);
				if (tx + tw > this.winX + this.winW - 12) {
					break;
				}
				if (inside(mouseX, mouseY, tx, this.winY + 50, tw, 44)) {
					selectedTab = i;
					scrollRow = 0;
					settingsPage = false;
					this.settingsModule = null;
					return;
				}
				tx += tw + 8;
			}
		}

		if (settingsPage) {
			if (button == 0) {
				this.clickSettingRows(this.settingsPageList(), this.mainX(), this.contentY(), this.mainW(), mouseX,
							mouseY);
			}
			return;
		}

		if (this.settingsModule != null) {
			if (button == 0) {
				if (inside(mouseX, mouseY, this.mainX(), this.contentY(), 44, 14)) {
					this.settingsModule = null;
				} else {
					this.clickSettingRows(this.settingsModule.getSettings(), this.mainX(), this.contentY() + 22,
							this.mainW(), mouseX, mouseY);
				}
			}
			return;
		}

		List<MeteorModule> list = this.filtered();
		int rows = (list.size() + COLS - 1) / COLS;
		int vis = this.visibleRows();

		// Scrollbar
		if (button == 0 && rows > vis
					&& inside(mouseX, mouseY, this.mainX() + this.gridW(), this.contentY(), 10, this.contentH())) {
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
			int x = this.mainX() + c * (cw + GAP);
			int y = this.contentY() + r * (CARD_H + GAP);
			if (inside(mouseX, mouseY, x, y, cw, CARD_H)) {
				MeteorModule m = list.get(i);
				boolean hasSettings = !m.getSettings().isEmpty();
				if (button == 1) {
					if (hasSettings) {
						this.settingsModule = m;
					}
				} else if (hasSettings && inside(mouseX, mouseY, x + cw - 31, y + 15, 14, 14)) {
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
			} else if (settingsPage) {
				settingsPage = false;
			} else if (!this.search.isEmpty()) {
				this.search = "";
				scrollRow = 0;
			} else {
				this.mc.displayGuiScreen(this.parent);
			}
			return;
		}

		boolean gridShown = this.settingsModule == null && !settingsPage;
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
		if (typedChar >= 32 && typedChar != 127 && this.fontRendererObj.getStringWidth(this.search) < SEARCH_W - 20) {
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
 * Minimal stub of GuiMainMenu to match the file name and restore expected symbol.
 * Replace this stub with the real implementation if available.
 */
public class GuiMainMenu extends GuiScreen {
	public GuiMainMenu() {
	}

	@Override
	public void initGui() {
		// stub
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}
}
