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
public class ClickGuiScreen extends GuiScreen {

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
		final MeteorSetting stars = addSetting(MeteorSetting.bool("Stars", true));
		final MeteorSetting meteors = addSetting(MeteorSetting.bool("Meteors", true));
		final MeteorSetting embers = addSetting(MeteorSetting.bool("Embers", true));

		MenuEffects() {
			super("Menu Effects", "Stars, meteors and embers on menus.", Category.VISUAL);
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
