package net.minecraft.client.gui;

import java.io.IOException;

import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.Minecraft;
import net.peyton.eagler.gui.GuiCredits;

/**
 * MeteorMC credits screen (Credits button on the main menu).
 *
 * To change what it says, edit the CREDITS list below.
 * A line starting with "#" is a gold heading, "" is a blank gap.
 * The "Eaglercraft Credits" button opens the original Eaglercraft credits.
 */
public class GuiMeteorCredits extends GuiScreen {

	// ===================== EDIT THE CREDITS HERE =====================
	private static final String[] CREDITS = {
			"#MeteorMC Client",
			"Created by none Client Owner",
			"Pinkys-Client Founder/Dev",
			"",
			"#Eaglercraft",
			"EaglercraftX by lax1dude and contributors",
			"Compiled for the web with TeaVM",
			"",
			"#Minecraft",
			"Minecraft is a trademark of Mojang AB / Microsoft",
			"MeteorMC is not affiliated with Mojang or Microsoft",
			"",
			"#Special thanks",
			"Everyone who tested the client",
	};
	// =================================================================

	private static final int BG_TOP = 0xFF5F86AE;
	private static final int BG_BOTTOM = 0xFFCFE3F5;
	private static final int BORDER = 0xFFA9C7E4;
	private static final int GOLD = 0xFF1E88E5;
	private static final int TEXT = 0xFF1E2A38;

	private static final int METEOR_OUTER = 0xFFBEE3FF;
	private static final int METEOR_CORE = 0xFFFFFFFF;
	private static final int FLAME_HEAD = 0xE0BEE3FF;
	private static final int FLAME_TAIL = 0x20FFFFFF;

	private static final int STAR_COUNT = 90;
	private static final int METEOR_COUNT = 4;
	private static final int LINE_HEIGHT = 11;

	private final GuiScreen parent;
	private int[] starX;
	private int[] starY;
	private int[] starSize;

	public GuiMeteorCredits(GuiScreen parent) {
		this.parent = parent;
	}

	public void initGui() {
		this.generateStars();
		this.buttonList.add(new GuiMeteorButton(0, this.width / 2 - 104, this.height - 28, 100, 20, "Back", GuiMeteorButton.Style.GLASS,
				GuiMeteorButton.ICON_NONE));
		this.buttonList.add(new GuiMeteorButton(1, this.width / 2 + 4, this.height - 28, 100, 20, "Eaglercraft Credits",
				GuiMeteorButton.Style.GLASS, GuiMeteorButton.ICON_NONE));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) {
			this.mc.displayGuiScreen(this.parent);
		} else if (button.id == 1) {
			this.mc.displayGuiScreen(new GuiCredits(this, "/assets/eagler/credits.txt"));
		}
	}

	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == 1) {
			this.mc.displayGuiScreen(this.parent);
		}
	}

	public boolean doesGuiPauseGame() {
		return false;
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		long now = Minecraft.getSystemTime();
		this.drawBackdrop(mouseX, mouseY, partialTicks, now);

		int winW = Math.min(320, this.width - 20);
		int winH = Math.max(100, Math.min(220, this.height - 60));
		int winX = (this.width - winW) / 2;
		int winY = 12;

		Gui.drawRect(winX - 1, winY - 1, winX + winW + 1, winY + winH + 1, BORDER);
		this.drawGradientRect(winX, winY, winX + winW, winY + winH, 0xA8F4F9FF, 0x98DCEBFA);

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) (this.width / 2), (float) (winY + 8), 0.0F);
		GlStateManager.scale(2.0F, 2.0F, 1.0F);
		this.fontRendererObj.drawString("Credits", (float) (-this.fontRendererObj.getStringWidth("Credits") / 2), 0.0F,
				GOLD, false);
		GlStateManager.popMatrix();
		Gui.drawRect(winX + 12, winY + 30, winX + winW - 12, winY + 31, BORDER);

		int y = winY + 38;
		for (String line : CREDITS) {
			if (y + 9 > winY + winH - 4) {
				break;
			}
			if (line.startsWith("#")) {
				this.drawCenteredString(this.fontRendererObj, line.substring(1), this.width / 2, y, GOLD);
			} else if (!line.isEmpty()) {
				this.drawCenteredString(this.fontRendererObj, line, this.width / 2, y, TEXT);
			}
			y += LINE_HEIGHT;
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	/** See-through backdrop: the menu panorama, or the game world when opened in-game. */
	private void drawBackdrop(int mouseX, int mouseY, float partialTicks, long now) {
		if (this.parent instanceof GuiMainMenu) {
			((GuiMainMenu) this.parent).drawMenuBackground(mouseX, mouseY, partialTicks);
			this.drawGradientRect(0, 0, this.width, this.height, 0x30FFFFFF, 0x50DCEBFA);
		} else if (this.mc.world != null) {
			this.drawGradientRect(0, 0, this.width, this.height, 0x28FFFFFF, 0x50E6F0FA);
		} else {
			this.drawSpaceBackground(now);
		}
		if (MeteorModuleManager.menuEmbers()) {
			MeteorModuleManager.drawSnow(this.width, this.height, now);
		}
	}

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
