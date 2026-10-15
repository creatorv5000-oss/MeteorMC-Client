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
 * MeteorMC ClickGUI: vertical category sidebar on the left, gold theme.
 *
 * Click a card: toggle the module.
 * Right click a card (or click its ".." corner): open its settings.
 * Type to search. Drag the scrollbar if there are many modules.
 * Esc: back / clear search / close.
 */
public class ClickGuiScreen extends GuiScreen {

	// ===================== STYLE =====================
	private static final String TITLE = "MeteorMC";

	private static final int WIN_W = 380;
	private static final int WIN_H = 240;
	private static final int HEADER = 28;
	private static final int COLS = 3;
	private static final int GAP = 6;
	private static final int CARD_H = 44;
	private static final int SEARCH_W = 86;

	// Sidebar width for vertical categories
	private static final int SIDEBAR_W = 96;
	private static final int TAB_H = 20;
	private static final int TAB_GAP = 6;

	// Colors - tuned for a gold theme
	private static final int BG_TOP = 0xFF0B0703;
	private static final int BG_BOTTOM = 0xFF2E2208;

	private static final int BORDER = 0xFF6B4E12;
	private static final int BORDER_HOVER = 0xFFFFD83A;
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
		}*
