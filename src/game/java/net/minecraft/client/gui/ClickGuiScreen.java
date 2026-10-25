package net.minecraft.client.gui;

/**
 * Compatibility layer for the Meteor click GUI and menu hook defaults.
 *
 * This keeps the older {@code new ClickGuiScreen()} / {@code new ClickGuiScreen(this)}
 * constructor usage working while reusing the actual GUI implementation in
 * {@link GuiClickGui}.
 */
public class ClickGuiScreen extends GuiClickGui {
	public ClickGuiScreen() {
		super();
	}

	public ClickGuiScreen(GuiScreen parent) {
		this();
	}

	/** Menu effect switches, read by the main menu and the ClickGUI. */
	public static boolean menuStars() {
		return false;
	}

	public static boolean menuMeteors() {
		return false;
	}

	public static boolean menuEmbers() {
		return false;
	}

	/** Renderer hook defaults. These are the normal vanilla values when no effect is enabled. */
	public static float hitRed() {
		return 0.4F;
	}

	public static float hitGreen() {
		return 0.0F;
	}

	public static float hitBlue() {
		return 0.0F;
	}

	public static float hurtCamScale() {
		return 1.0F;
	}

	public static boolean staticFov() {
		return false;
	}

	public static float fireOffset() {
		return 0.0F;
	}

	public static boolean fixedHand() {
		return false;
	}

	public static void applyHandOffset(boolean mainHand) {
	}

	public static void applyItemScale(boolean mainHand) {
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
}
