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

    private static final int PANEL_W = 620;
    private static final int PANEL_H = 310;
    private static final int SIDEBAR_W = 124;
    private static final int CARD_H = 50;
    private static final int GAP = 7;
    private static final int WHITE = 0xFFF2F2F2;
    private static final int MUTED = 0xFFBDBDBD;
    private static final int PANEL = 0xE85A5C5E;
    private static final int CARD = 0xB84B4D4F;
    private static final int CARD_HOVER = 0xD05C5E60;
    private static final int BORDER = 0x507F8183;
    private static final int SELECTED = 0xB5A6A8AA;
    private static final int SWITCH_ON = 0xFFF5F5F5;
    private static final int SWITCH_OFF = 0xFF929496;

    private static int selectedTab = 0;
    private static int scrollRow = 0;
    private final GuiScreen parent;
    private String search = "";
    private boolean searchFocused;
    private MeteorModule settingsModule;
    private int winX, winY, winW, winH;
    private int contentX, contentY, contentW, contentH;
    private static final String[] TABS = { "All", "HUD", "GUI", "PVP", "FPS", "Misc" };
    private static final String[] SIDEBAR = { "Home", "Modules", "Cosmetics", "Profiles", "Edit HUD", "Settings" };

    public ClickGuiScreen() {
        this(null);
    }

    public ClickGuiScreen(GuiScreen parent) {
        this.parent = parent;
    }

    public void initGui() {
        this.searchFocused = false;
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private void layout() {
        this.winW = Math.min(PANEL_W, this.width - 24);
        this.winH = Math.min(PANEL_H, this.height - 24);
        this.winX = (this.width - this.winW) / 2;
        this.winY = (this.height - this.winH) / 2;
        this.contentX = this.winX + SIDEBAR_W + 12;
        this.contentY = this.winY + 56;
        this.contentW = this.winW - SIDEBAR_W - 24;
        this.contentH = this.winH - 68;
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private List<MeteorModule> filtered() {
        List<MeteorModule> out = new ArrayList<MeteorModule>();
        String q = this.search.toLowerCase();
        for (MeteorModule m : MeteorModuleManager.getModules()) {
            if (selectedTab == 1 && m.getCategory() != MeteorModule.Category.HUD) continue;
            if (selectedTab == 2 && m.getCategory() != MeteorModule.Category.VISUAL) continue;
            if (selectedTab == 3 && m.getCategory() != MeteorModule.Category.COMBAT) continue;
            if (selectedTab == 4 && m.getCategory() != MeteorModule.Category.PERFORMANCE) continue;
            if (selectedTab == 5 && m.getCategory() != MeteorModule.Category.MISC
                    && m.getCategory() != MeteorModule.Category.MOVEMENT) continue;
            if (!q.isEmpty() && !m.getName().toLowerCase().contains(q)
                    && !m.getDescription().toLowerCase().contains(q)) continue;
            out.add(m);
        }
        return out;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.layout();
        this.drawGradientRect(0, 0, this.width, this.height, 0xB8000000, 0xD9000000);

        // Main translucent grey panel.
        Gui.drawRect(this.winX, this.winY, this.winX + this.winW, this.winY + this.winH, PANEL);
        Gui.drawRect(this.winX, this.winY, this.winX + this.winW, this.winY + 1, 0x507F7F7F);
        Gui.drawRect(this.winX, this.winY + this.winH - 1, this.winX + this.winW, this.winY + this.winH, 0x407F7F7F);

        // Sidebar and separator.
        Gui.drawRect(this.winX, this.winY, this.winX + SIDEBAR_W, this.winY + this.winH, 0x1CFFFFFF);
        Gui.drawRect(this.winX + SIDEBAR_W, this.winY + 12, this.winX + SIDEBAR_W + 1,
                this.winY + this.winH - 12, BORDER);
        this.drawCenteredString(this.fontRendererObj, "\u2726 R E F L E X",
                this.winX + SIDEBAR_W / 2, this.winY + 17, WHITE);

        int[] sideYs = { this.winY + 48, this.winY + 68, this.winY + 88,
                this.winY + 108, this.winY + 128, this.winY + this.winH - 34 };
        for (int i = 0; i < SIDEBAR.length; i++) {
            int y = sideYs[i];
            boolean active = (i == 1 && this.settingsModule == null);
            boolean hover = inside(mouseX, mouseY, this.winX + 8, y - 3, SIDEBAR_W - 16, 17);
            if (active) {
                Gui.drawRect(this.winX + 8, y - 3, this.winX + SIDEBAR_W - 8, y + 14, SELECTED);
            } else if (hover) {
                Gui.drawRect(this.winX + 8, y - 3, this.winX + SIDEBAR_W - 8, y + 14, 0x247F7F7F);
            }
            this.drawString(this.fontRendererObj, SIDEBAR[i], this.winX + 17, y + 1,
                    active ? WHITE : (hover ? WHITE : 0xFFD7D7D7));
        }

        // Search bar.
        int searchX = this.contentX;
        int searchY = this.winY + 15;
        int searchW = Math.max(105, this.contentW - 285);
        Gui.drawRect(searchX, searchY, searchX + searchW, searchY + 23,
                this.searchFocused ? 0xD0444648 : 0xB9444648);
        Gui.drawRect(searchX, searchY, searchX + searchW, searchY + 1, BORDER);
        this.drawString(this.fontRendererObj, "\u2315", searchX + 9, searchY + 7, MUTED);
        String shownSearch = this.search.isEmpty() ? "Search modules..." : this.search;
        if (this.searchFocused && (Minecraft.getSystemTime() / 500L) % 2L == 0L) shownSearch += "_";
        this.drawString(this.fontRendererObj, shownSearch, searchX + 25, searchY + 7,
                this.search.isEmpty() ? MUTED : WHITE);

        // Category pills.
        int tabX = searchX + searchW + 6;
        int tabGap = 4;
        int available = this.winX + this.winW - 12 - tabX;
        int tabW = Math.max(28, (available - tabGap * (TABS.length - 1)) / TABS.length);
        for (int i = 0; i < TABS.length; i++) {
            int x = tabX + i * (tabW + tabGap);
            boolean active = selectedTab == i;
            boolean hover = inside(mouseX, mouseY, x, searchY, tabW, 23);
            Gui.drawRect(x, searchY, x + tabW, searchY + 23,
                    active ? 0xD4A8AAAC : (hover ? 0x9A77797B : 0x604F5153));
            Gui.drawRect(x, searchY, x + tabW, searchY + 1, BORDER);
            this.drawCenteredString(this.fontRendererObj, TABS[i], x + tabW / 2, searchY + 7,
                    active ? WHITE : 0xFFE1E1E1);
        }

        if (this.settingsModule != null) {
            this.drawModuleSettings(mouseX, mouseY);
        } else {
            this.drawModuleGrid(mouseX, mouseY);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawModuleGrid(int mouseX, int mouseY) {
        List<MeteorModule> modules = this.filtered();
        int cols = 2;
        int cardW = (this.contentW - 8) / cols;
        int rows = (modules.size() + cols - 1) / cols;
        int visibleRows = Math.max(1, (this.contentH + GAP) / (CARD_H + GAP));
        int maxScroll = Math.max(0, rows - visibleRows);
        scrollRow = Math.max(0, Math.min(scrollRow, maxScroll));

        if (modules.isEmpty()) {
            this.drawCenteredString(this.fontRendererObj, "No modules found",
                    this.contentX + this.contentW / 2, this.contentY + 20, MUTED);
            return;
        }

        int first = scrollRow * cols;
        int last = Math.min(modules.size(), (scrollRow + visibleRows) * cols);
        for (int i = first; i < last; i++) {
            int row = i / cols - scrollRow;
            int col = i % cols;
            int x = this.contentX + col * (cardW + GAP);
            int y = this.contentY + row * (CARD_H + GAP);
            MeteorModule module = modules.get(i);
            boolean hover = inside(mouseX, mouseY, x, y, cardW, CARD_H);
            Gui.drawRect(x, y, x + cardW, y + CARD_H, hover ? CARD_HOVER : CARD);
            Gui.drawRect(x, y, x + cardW, y + 1, module.isEnabled() ? 0xD0EEEEEE : BORDER);
            this.drawString(this.fontRendererObj, trim(module.getName(), cardW - 52), x + 9, y + 7, WHITE);
            this.drawString(this.fontRendererObj, trim(module.getDescription(), cardW - 18),
                    x + 9, y + 27, 0xFFD0D0D0);

            // Gear/settings affordance.
            boolean hasSettings = !module.getSettings().isEmpty();
            this.drawString(this.fontRendererObj, "\u2699", x + cardW - 32, y + 7,
                    hasSettings ? 0xFFE0E0E0 : 0xFF8A8A8A);

            // Toggle switch.
            int sx = x + cardW - 49;
            int sy = y + 25;
            Gui.drawRect(sx, sy, sx + 28, sy + 13, module.isEnabled() ? SWITCH_ON : SWITCH_OFF);
            Gui.drawRect(sx + 1, sy + 1, sx + 27, sy + 12, module.isEnabled() ? 0xFFF7F7F7 : 0xFF77797B);
            int knobX = module.isEnabled() ? sx + 16 : sx + 3;
            Gui.drawRect(knobX, sy + 2, knobX + 9, sy + 11, module.isEnabled() ? 0xFF202124 : 0xFFF4F4F4);
        }

        if (rows > visibleRows) {
            int barX = this.contentX + this.contentW - 3;
            Gui.drawRect(barX, this.contentY, barX + 3, this.contentY + this.contentH, 0x44303030);
            int thumbH = Math.max(18, this.contentH * visibleRows / rows);
            int thumbY = this.contentY + (this.contentH - thumbH) * scrollRow / Math.max(1, maxScroll);
            Gui.drawRect(barX, thumbY, barX + 3, thumbY + thumbH, 0xFFD0D0D0);
        }
    }

    private void drawModuleSettings(int mouseX, int mouseY) {
        int x = this.contentX;
        int y = this.contentY;
        Gui.drawRect(x, y, x + this.contentW, y + 24, CARD);
        this.drawString(this.fontRendererObj, "< Back", x + 8, y + 8, WHITE);
        this.drawString(this.fontRendererObj, this.settingsModule.getName(), x + 62, y + 8, WHITE);
        y += 32;
        for (MeteorSetting setting : this.settingsModule.getSettings()) {
            int h = setting.getType() == MeteorSetting.Type.BOOLEAN ? 23 : 34;
            boolean hover = inside(mouseX, mouseY, x, y, this.contentW, h);
            Gui.drawRect(x, y, x + this.contentW, y + h, hover ? CARD_HOVER : CARD);
            this.drawString(this.fontRendererObj, setting.getName(), x + 8, y + 7, WHITE);
            if (setting.getType() == MeteorSetting.Type.BOOLEAN) {
                this.drawString(this.fontRendererObj, setting.getBoolean() ? "ON" : "OFF",
                        x + this.contentW - 30, y + 7, setting.getBoolean() ? WHITE : MUTED);
            } else {
                String value = String.valueOf(Math.round(setting.getDouble() * 100.0D) / 100.0D);
                this.drawString(this.fontRendererObj, value, x + this.contentW - 42, y + 7, WHITE);
                int sx = x + 8, sw = this.contentW - 16;
                double t = (setting.getDouble() - setting.getMin()) / Math.max(0.0001D, setting.getMax() - setting.getMin());
                Gui.drawRect(sx, y + h - 8, sx + sw, y + h - 5, 0xFF77797B);
                Gui.drawRect(sx, y + h - 8, sx + (int)(sw * t), y + h - 5, 0xFFE8E8E8);
            }
            y += h + 5;
        }
    }

    private String trim(String text, int maxWidth) {
        if (this.fontRendererObj.getStringWidth(text) <= maxWidth) return text;
        while (text.length() > 1 && this.fontRendererObj.getStringWidth(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        this.layout();
        if (button != 0) {
            if (button == 1 && this.settingsModule == null) {
                this.openModuleAt(mouseX, mouseY, true);
            }
            return;
        }

        int sideY = this.winY + 48;
        for (int i = 0; i < 5; i++) {
            if (inside(mouseX, mouseY, this.winX + 8, sideY + i * 20 - 3, SIDEBAR_W - 16, 17)) {
                if (i == 1) {
                    this.settingsModule = null;
                } else if (i == 5) {
                    this.settingsModule = null;
                }
                return;
            }
        }
        if (inside(mouseX, mouseY, this.winX + 8, this.winY + this.winH - 37, SIDEBAR_W - 16, 20)) {
            this.settingsModule = null;
            return;
        }

        int searchX = this.contentX;
        int searchY = this.winY + 15;
        int searchW = Math.max(105, this.contentW - 285);
        this.searchFocused = inside(mouseX, mouseY, searchX, searchY, searchW, 23);
        int tabX = searchX + searchW + 6;
        int available = this.winX + this.winW - 12 - tabX;
        int tabW = Math.max(28, (available - 4 * (TABS.length - 1)) / TABS.length);
        for (int i = 0; i < TABS.length; i++) {
            int x = tabX + i * (tabW + 4);
            if (inside(mouseX, mouseY, x, searchY, tabW, 23)) {
                selectedTab = i;
                scrollRow = 0;
                this.settingsModule = null;
                return;
            }
        }

        if (this.settingsModule != null) {
            if (inside(mouseX, mouseY, this.contentX, this.contentY, this.contentW, 24)) {
                this.settingsModule = null;
                return;
            }
            int y = this.contentY + 32;
            for (MeteorSetting setting : this.settingsModule.getSettings()) {
                int h = setting.getType() == MeteorSetting.Type.BOOLEAN ? 23 : 34;
                if (inside(mouseX, mouseY, this.contentX, y, this.contentW, h)) {
                    if (setting.getType() == MeteorSetting.Type.BOOLEAN) {
                        setting.toggle();
                    } else {
                        double t = (mouseX - this.contentX - 8) / (double)Math.max(1, this.contentW - 16);
                        t = Math.max(0.0D, Math.min(1.0D, t));
                        setting.setValue(setting.getMin() + t * (setting.getMax() - setting.getMin()));
                    }
                    return;
                }
                y += h + 5;
            }
            return;
        }
        this.openModuleAt(mouseX, mouseY, false);
    }

    private void openModuleAt(int mouseX, int mouseY, boolean settingsOnly) {
        List<MeteorModule> modules = this.filtered();
        int cols = 2;
        int cardW = (this.contentW - 8) / cols;
        int visibleRows = Math.max(1, (this.contentH + GAP) / (CARD_H + GAP));
        int first = scrollRow * cols;
        int last = Math.min(modules.size(), (scrollRow + visibleRows) * cols);
        for (int i = first; i < last; i++) {
            int row = i / cols - scrollRow;
            int col = i % cols;
            int x = this.contentX + col * (cardW + GAP);
            int y = this.contentY + row * (CARD_H + GAP);
            if (inside(mouseX, mouseY, x, y, cardW, CARD_H)) {
                MeteorModule module = modules.get(i);
                boolean gear = mouseX >= x + cardW - 39 && mouseY < y + 24;
                if ((settingsOnly || gear) && !module.getSettings().isEmpty()) {
                    this.settingsModule = module;
                } else if (!settingsOnly) {
                    module.toggle();
                }
                return;
            }
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            if (this.settingsModule != null) this.settingsModule = null;
            else this.mc.displayGuiScreen(this.parent);
            return;
        }
        if (!this.searchFocused || this.settingsModule != null) return;
        if (keyCode == 14) {
            if (!this.search.isEmpty()) this.search = this.search.substring(0, this.search.length() - 1);
            scrollRow = 0;
            return;
        }
        if (typedChar >= 32 && typedChar != 127 && this.fontRendererObj.getStringWidth(this.search) < 180) {
            this.search += typedChar;
            scrollRow = 0;
        }
    }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0 && this.settingsModule == null) {
            scrollRow = Math.max(0, scrollRow + (wheel < 0 ? 1 : -1));
        }
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
