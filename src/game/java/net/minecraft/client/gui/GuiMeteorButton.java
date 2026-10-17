package net.minecraft.client.gui;

import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;

/**
 * Main menu button in the MeteorMC style.
 *
 * GLASS: translucent centered button (used by the main menu).
 * PRIMARY: big gold button. ROW: dark row with an icon and left-aligned text.
 * ICON: small square button with just an icon (its text shows as a tooltip).
 */
public class GuiMeteorButton extends GuiButton {

	public enum Style {
		PRIMARY, ROW, ICON, GLASS
	}

	public static final int ICON_NONE = -1;
	public static final int ICON_PERSON = 0;
	public static final int ICON_GLOBE = 1;
	public static final int ICON_INFO = 2;
	public static final int ICON_GRID = 3;
	public static final int ICON_GEAR = 4;
	public static final int ICON_STAR = 5;
	public static final int ICON_METEOR = 6;

	/** 8x8 pixel icons. '#' is a lit pixel. */
	private static final String[][] ICONS = {
			// person
			{ "..####..", "..####..", "..####..", "...##...", ".######.", "########", "########", "........" },
			// globe
			{ "..####..", ".#.##.#.", "#..##..#", "########", "#..##..#", ".#.##.#.", "..####..", "........" },
			// info
			{ "...##...", "........", "..###...", "...##...", "...##...", "...##...", "..####..", "........" },
			// grid
			{ "###.###.", "###.###.", "###.###.", "........", "###.###.", "###.###.", "###.###.", "........" },
			// gear
			{ "#..##..#", ".######.", "..#..#..", "###..###", "###..###", "..#..#..", ".######.", "#..##..#" },
			// star
			{ "...##...", "...##...", "########", ".######.", "..####..", ".##..##.", "##....##", "........" },
			// meteor
			{ "......##", ".....##.", "....##..", "..####..", ".######.", ".######.", "..####..", "........" } };

	private final Style style;
	private final int icon;

	public GuiMeteorButton(int id, int x, int y, int w, int h, String text, Style style, int icon) {
		super(id, x, y, w, h, text);
		this.style = style;
		this.icon = icon;
	}

	public boolean isIconStyle() {
		return this.style == Style.ICON;
	}

	public void func_191745_a(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (!this.visible) {
			return;
		}
		FontRenderer f = mc.fontRendererObj;
		this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width
				&& mouseY < this.yPosition + this.height;
		boolean on = this.enabled;
		boolean hot = on && this.hovered;

		int x0 = this.xPosition;
		int y0 = this.yPosition;
		int x1 = x0 + this.width;
		int y1 = y0 + this.height;

		if (this.style == Style.PRIMARY) {
			Gui.drawRect(x0, y1 - 3, x1, y1, hot ? 0xFFD07800 : 0xFFB86A00);
			this.drawGradientRect(x0, y0, x1, y1 - 3, hot ? 0xFFFFCF4A : 0xFFFFB820, hot ? 0xFFFFA814 : 0xFFE89000);
			Gui.drawRect(x0, y0, x1, y0 + 1, 0x60FFFFFF);
			this.drawIcon(x0 + 10, y0 + (this.height - 3 - 16) / 2, 2, 0xFF3A2200);

			GlStateManager.pushMatrix();
			GlStateManager.translate((float) (x0 + this.width / 2 + 10), (float) (y0 + (this.height - 3) / 2.0F), 0.0F);
			GlStateManager.scale(1.4F, 1.4F, 1.0F);
			f.drawString(this.displayString, (float) (-f.getStringWidth(this.displayString) / 2), -4.0F, 0xFF2A1800,
					false);
			GlStateManager.popMatrix();
			return;
		}

		if (this.style == Style.GLASS || this.style == Style.ICON) {
			int gborder = !on ? 0x40FFFFFF : (hot ? 0xFF4FA3E0 : 0xA0FFFFFF);
			Gui.drawRect(x0, y0, x1, y1, gborder);
			this.drawGradientRect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, hot ? 0xC8FFFFFF : 0x98FFFFFF,
					hot ? 0xB0DCEEFF : 0x70EAF4FF);
			int gcolor = !on ? 0xFF8A99A8 : (hot ? 0xFF0D47A1 : 0xFF1E2A38);
			if (this.style == Style.GLASS) {
				int sw = f.getStringWidth(this.displayString);
				this.drawIcon(x0 + 8, y0 + (this.height - 8) / 2, 1, gcolor);
				f.drawString(this.displayString, (float) (x0 + (this.width - sw) / 2),
						(float) (y0 + (this.height - 8) / 2), gcolor, false);
			} else {
				this.drawIcon(x0 + (this.width - 8) / 2, y0 + (this.height - 8) / 2, 1, gcolor);
			}
			return;
		}

		int border = !on ? 0xFF2A261A : (hot ? 0xFFFFC21F : 0xFF3A3322);
		Gui.drawRect(x0, y0, x1, y1, border);
		this.drawGradientRect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, hot ? 0xE0332810 : 0xE0201A0C,
				hot ? 0xE0221A0A : 0xE0151008);
		int color = !on ? 0xFF6A6450 : (hot ? 0xFFFFD83A : 0xFFF2EAD0);

		if (this.style == Style.ROW) {
			this.drawIcon(x0 + 8, y0 + (this.height - 8) / 2, 1, color);
			f.drawString(this.displayString, (float) (x0 + 24), (float) (y0 + (this.height - 8) / 2), color, true);
		} else {
			this.drawIcon(x0 + (this.width - 8) / 2, y0 + (this.height - 8) / 2, 1, color);
		}
	}

	private void drawIcon(int px, int py, int scale, int color) {
		if (this.icon < 0 || this.icon >= ICONS.length) {
			return;
		}
		String[] rows = ICONS[this.icon];
		for (int r = 0; r < rows.length; ++r) {
			for (int c = 0; c < rows[r].length(); ++c) {
				if (rows[r].charAt(c) == '#') {
					Gui.drawRect(px + c * scale, py + r * scale, px + (c + 1) * scale, py + (r + 1) * scale, color);
				}
			}
		}
	}
}
