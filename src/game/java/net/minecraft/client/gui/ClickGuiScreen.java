package net.minecraft.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.RealOpenGLEnums;
import net.minecraft.client.Minecraft;

/**
 * MeteorMC ClickGUI, styled like the main menu: space background with stars
 * and shooting meteors, glowing animated title, gold buttons.
 *
 * Left click a module: toggle it.
 * Right click a module: show/hide its settings.
 * Drag a panel header to move it. Right click a header to collapse it.
 * Drag a slider to change a number. Esc, Right Shift or Done closes the GUI.
 */
public class ClickGuiScreen extends GuiScreen {

	// ===================== STYLE =====================
	private static final String TITLE = "MeteorMC";
	private static final float TITLE_SCALE = 3.0F;

	private static final int PANEL_WIDTH = 112;
	private static final int HEADER_HEIGHT = 16;
	private static final int ROW_HEIGHT = 14;
	private static final int PANEL_START_Y = 56;

	private static final int BG_TOP = 0xFF0B0A06;
	private static final int BG_BOTTOM = 0xFF2E2208;

	// Same palette as the gold buttons
	private static final int BORDER = 0xFF7A6422;
	private static final int BORDER_HOVER = 0xFFFFC21F;
	private static final int FILL_TOP = 0xE02E260E;
	private static final int FILL_BOTTOM = 0xE01A150A;
	private static final int FILL_HOVER_TOP = 0xE05A4512;
	private static final int FILL_HOVER_BOTTOM = 0xE033270A;
	private static final int FILL_ON_TOP = 0xFF8A6A14;
	private static final int FILL_ON_BOTTOM = 0xFF5A4512;
	private static final int HIGHLIGHT = 0x40FFE680;
	private static final int SETTING_BG = 0xE0120F07;

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
	private static final int KEY_RSHIFT = 54;

	private static class Panel {
		final MeteorModule.Category category;
		int x;
		int y;
		boolean open = true;
		final Set<MeteorModule> expanded = new HashSet<MeteorModule>();

		Panel(MeteorModule.Category category, int x, int y) {
			this.category = category;
			this.x = x;
			this.y = y;
		}
	}

	/** Static so panel positions are remembered between openings. */
	private static final List<Panel> PANELS = new ArrayList<Panel>();

	private final GuiScreen parent;
	private Panel draggingPanel;
	private int dragOffX;
	private int dragOffY;
	private MeteorSetting draggingSetting;
	private int sliderX;
	private int sliderW;

	private String tooltip;
	private int tooltipX;
	private int tooltipY;

	private int[] starX;
	private int[] starY;
	private int[] starSize;

	public ClickGuiScreen() {
		this(null);
	}

	public ClickGuiScreen(GuiScreen parent) {
		this.parent = parent;
		if (PANELS.isEmpty()) {
			int x = 20;
			for (MeteorModule.Category c : MeteorModule.Category.values()) {
				if (!MeteorModuleManager.getModules(c).isEmpty()) {
					PANELS.add(new Panel(c, x, PANEL_START_Y));
					x += PANEL_WIDTH + 12;
				}
			}
		}
	}

	public void initGui() {
		this.generateStars();
		this.buttonList.add(new GuiButton(0, this.width / 2 - 50, this.height - 28, 100, 20, "Done"));
	}

	public boolean doesGuiPauseGame() {
		return false;
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) {
			this.mc.displayGuiScreen(this.parent);
		}
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		long now = Minecraft.getSystemTime();

		this.drawSpaceBackground(now);
		if (MeteorModuleManager.menuEmbers()) {
			this.drawEmbers(now);
		}
		this.drawAnimatedTitle(now);

		this.tooltip = null;
		for (Panel p : PANELS) {
			this.drawPanel(p, mouseX, mouseY);
		}

		super.drawScreen(mouseX, mouseY, partialTicks);

		if (this.tooltip != null) {
			int tw = this.fontRendererObj.getStringWidth(this.tooltip);
			int tx = Math.min(this.tooltipX + 8, this.width - tw - 6);
			int ty = this.tooltipY + 8;
			Gui.drawRect(tx - 3, ty - 3, tx + tw + 3, ty + 11, BORDER);
			Gui.drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xFF14110A);
			this.drawString(this.fontRendererObj, this.tooltip, tx, ty, TEXT);
		}
	}

	// ------------------------- Panels -------------------------

	private int panelHeight(Panel p) {
		int h = HEADER_HEIGHT;
		if (p.open) {
			for (MeteorModule m : MeteorModuleManager.getModules(p.category)) {
				h += ROW_HEIGHT;
				if (p.expanded.contains(m)) {
					h += m.getSettings().size() * ROW_HEIGHT;
				}
			}
		}
		return h;
	}

	private void drawPanel(Panel p, int mouseX, int mouseY) {
		int h = this.panelHeight(p);
		Gui.drawRect(p.x - 1, p.y - 1, p.x + PANEL_WIDTH + 1, p.y + h + 1, BORDER);

		// Header
		this.drawGradientRect(p.x, p.y, p.x + PANEL_WIDTH, p.y + HEADER_HEIGHT, 0xFF4A3A10, 0xFF2E260E);
		Gui.drawRect(p.x, p.y, p.x + PANEL_WIDTH, p.y + 1, HIGHLIGHT);
		this.drawCenteredString(this.fontRendererObj, p.category.getLabel(), p.x + PANEL_WIDTH / 2, p.y + 4, GOLD);
		this.drawString(this.fontRendererObj, p.open ? "-" : "+", p.x + PANEL_WIDTH - 10, p.y + 4, TEXT);

		if (!p.open) {
			return;
		}

		int y = p.y + HEADER_HEIGHT;
		for (MeteorModule m : MeteorModuleManager.getModules(p.category)) {
			boolean hover = inside(mouseX, mouseY, p.x, y, PANEL_WIDTH, ROW_HEIGHT);

			if (m.isEnabled()) {
				this.drawGradientRect(p.x, y, p.x + PANEL_WIDTH, y + ROW_HEIGHT, FILL_ON_TOP, FILL_ON_BOTTOM);
				Gui.drawRect(p.x, y, p.x + 2, y + ROW_HEIGHT, BORDER_HOVER);
			} else if (hover) {
				this.drawGradientRect(p.x, y, p.x + PANEL_WIDTH, y + ROW_HEIGHT, FILL_HOVER_TOP, FILL_HOVER_BOTTOM);
			} else {
				this.drawGradientRect(p.x, y, p.x + PANEL_WIDTH, y + ROW_HEIGHT, FILL_TOP, FILL_BOTTOM);
			}
			Gui.drawRect(p.x, y, p.x + PANEL_WIDTH, y + 1, HIGHLIGHT);
			this.drawString(this.fontRendererObj, m.getName(), p.x + 6, y + 3,
					m.isEnabled() ? GOLD : (hover ? GOLD : TEXT));

			if (!m.getSettings().isEmpty()) {
				this.drawString(this.fontRendererObj, p.expanded.contains(m) ? "v" : ">", p.x + PANEL_WIDTH - 10,
						y + 3, TEXT_DIM);
			}
			if (hover) {
				this.tooltip = m.getDescription();
				this.tooltipX = mouseX;
				this.tooltipY = mouseY;
			}
			y += ROW_HEIGHT;

			if (p.expanded.contains(m)) {
				for (MeteorSetting s : m.getSettings()) {
					this.drawSetting(s, p.x, y);
					y += ROW_HEIGHT;
				}
			}
		}
	}

	private void drawSetting(MeteorSetting s, int x, int y) {
		Gui.drawRect(x, y, x + PANEL_WIDTH, y + ROW_HEIGHT, SETTING_BG);

		if (s.getType() == MeteorSetting.Type.BOOLEAN) {
			this.drawString(this.fontRendererObj, s.getName(), x + 10, y + 3, TEXT);
			String state = s.getBoolean() ? "ON" : "OFF";
			int w = this.fontRendererObj.getStringWidth(state);
			this.drawString(this.fontRendererObj, state, x + PANEL_WIDTH - w - 6, y + 3,
					s.getBoolean() ? GOLD : TEXT_DIM);
		} else {
			this.drawString(this.fontRendererObj, s.getName() + ": " + fmt(s.getDouble()), x + 10, y + 1, TEXT);
			int tx0 = x + 4;
			int tw = PANEL_WIDTH - 8;
			double t = (s.getDouble() - s.getMin()) / (s.getMax() - s.getMin());
			Gui.drawRect(tx0, y + ROW_HEIGHT - 4, tx0 + tw, y + ROW_HEIGHT - 2, 0xFF2A220C);
			Gui.drawRect(tx0, y + ROW_HEIGHT - 4, tx0 + (int) (tw * t), y + ROW_HEIGHT - 2, GOLD);
		}
	}

	// ------------------------- Input -------------------------

	protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
		super.mouseClicked(mouseX, mouseY, button);

		for (int i = PANELS.size() - 1; i >= 0; --i) {
			Panel p = PANELS.get(i);

			if (inside(mouseX, mouseY, p.x, p.y, PANEL_WIDTH, HEADER_HEIGHT)) {
				if (button == 0) {
					this.draggingPanel = p;
					this.dragOffX = mouseX - p.x;
					this.dragOffY = mouseY - p.y;
					PANELS.remove(i);
					PANELS.add(p); // bring to front
				} else if (button == 1) {
					p.open = !p.open;
				}
				return;
			}

			if (!p.open) {
				continue;
			}

			int y = p.y + HEADER_HEIGHT;
			for (MeteorModule m : MeteorModuleManager.getModules(p.category)) {
				if (inside(mouseX, mouseY, p.x, y, PANEL_WIDTH, ROW_HEIGHT)) {
					if (button == 0) {
						m.toggle();
					} else if (button == 1 && !m.getSettings().isEmpty()) {
						if (!p.expanded.remove(m)) {
							p.expanded.add(m);
						}
					}
					return;
				}
				y += ROW_HEIGHT;

				if (p.expanded.contains(m)) {
					for (MeteorSetting s : m.getSettings()) {
						if (inside(mouseX, mouseY, p.x, y, PANEL_WIDTH, ROW_HEIGHT)) {
							if (button == 0) {
								if (s.getType() == MeteorSetting.Type.BOOLEAN) {
									s.toggle();
								} else {
									this.draggingSetting = s;
									this.sliderX = p.x + 4;
									this.sliderW = PANEL_WIDTH - 8;
									this.updateSlider(mouseX);
								}
							}
							return;
						}
						y += ROW_HEIGHT;
					}
				}
			}
		}
	}

	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

		if (this.draggingPanel != null) {
			this.draggingPanel.x = Math.max(0, Math.min(this.width - PANEL_WIDTH, mouseX - this.dragOffX));
			this.draggingPanel.y = Math.max(0, Math.min(this.height - HEADER_HEIGHT, mouseY - this.dragOffY));
		}
		if (this.draggingSetting != null) {
			this.updateSlider(mouseX);
		}
	}

	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		this.draggingPanel = null;
		this.draggingSetting = null;
	}

	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == KEY_ESCAPE || keyCode == KEY_RSHIFT) {
			this.mc.displayGuiScreen(this.parent);
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	private void updateSlider(int mouseX) {
		double t = (mouseX - this.sliderX) / (double) this.sliderW;
		t = Math.max(0.0D, Math.min(1.0D, t));
		this.draggingSetting.setValue(
				this.draggingSetting.getMin() + t * (this.draggingSetting.getMax() - this.draggingSetting.getMin()));
	}

	// ------------------------- Menu-style visuals -------------------------

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

	private void drawEmbers(long now) {
		int cx = this.width / 2;
		for (int k = 0; k < 14; ++k) {
			float life = ((now + k * 397L) % 2000L) / 2000.0F;
			int x = cx - 90 + (k * 37) % 181 + (int) (Math.sin(life * 6.0F + k) * 4.0D);
			int y = 42 - (int) (life * 40.0F);
			int a = (int) ((1.0F - life) * 255.0F);
			int size = (k % 3 == 0) ? 2 : 1;
			int rgb = lerpColor(0xFFFFE680, 0xFFFF6A00, life) & 0xFFFFFF;
			Gui.drawRect(x, y, x + size, y + size, (a << 24) | rgb);
		}
	}

	private void drawAnimatedTitle(long now) {
		int cx = this.width / 2;
		int total = this.fontRendererObj.getStringWidth(TITLE);
		int len = TITLE.length();
		float sweep = ((now % 2600L) / 2600.0F) * (len + 6) - 3.0F;

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) cx, 10.0F, 0.0F);
		GlStateManager.scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
				RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);

		int x = -total / 2;
		for (int i = 0; i < len; ++i) {
			String s = String.valueOf(TITLE.charAt(i));
			int cw = this.fontRendererObj.getStringWidth(s);

			float bob = (float) Math.sin(now * 0.006D + i * 0.7D) * 0.8F;
			float glow = Math.max(0.0F, 1.0F - Math.abs(i - sweep) / 2.5F);

			int auraAlpha = (int) (50.0F + 90.0F * glow + 20.0F * (float) Math.sin(now * 0.01D + i));
			auraAlpha = Math.max(8, Math.min(200, auraAlpha));
			int auraColor = (auraAlpha << 24) | TITLE_AURA;
			this.fontRendererObj.drawString(s, (float) x - 0.6F, bob, auraColor, false);
			this.fontRendererObj.drawString(s, (float) x + 0.6F, bob, auraColor, false);
			this.fontRendererObj.drawString(s, (float) x, bob - 0.6F, auraColor, false);
			this.fontRendererObj.drawString(s, (float) x, bob + 0.6F, auraColor, false);

			int rgb = lerpColor(TITLE_COLOR_A, TITLE_COLOR_B, glow) & 0xFFFFFF;
			this.fontRendererObj.drawString(s, (float) x, bob, 0xFF000000 | rgb, true);

			x += cw;
		}

		GlStateManager.popMatrix();

		int lineW = (int) (total * TITLE_SCALE);
		int lx0 = cx - lineW / 2;
		int ly = 10 + (int) (9 * TITLE_SCALE) + 4;
		Gui.drawRect(lx0, ly, lx0 + lineW, ly + 1, 0xFF7A6422);
		int streakW = 30;
		int pos = lx0 + (int) (((now % 1800L) / 1800.0F) * (lineW + streakW)) - streakW;
		int s0 = Math.max(lx0, pos);
		int s1 = Math.min(lx0 + lineW, pos + streakW);
		if (s1 > s0) {
			Gui.drawRect(s0, ly, s1, ly + 1, 0xFFFFE680);
		}
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
