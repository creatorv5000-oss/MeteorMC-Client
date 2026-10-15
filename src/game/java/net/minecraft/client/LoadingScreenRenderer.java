package net.minecraft.client;

import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.RealOpenGLEnums;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MinecraftError;

public class LoadingScreenRenderer implements IProgressUpdate {

	// ===================== CUSTOMIZE HERE =====================
	private static final String TITLE = "MeteorMC";

	private static final int BG_TOP = 0xFF0F0D08;
	private static final int BG_BOTTOM = 0xFF2E2208;

	private static final int BAR_BORDER = 0xFF7A6422;
	private static final int BAR_TRACK = 0xFF1A160A;
	private static final int BAR_FILL = 0xFFFFC21F;
	private static final int BAR_FILL_HIGHLIGHT = 0xFFFFE680;

	private static final int TITLE_COLOR = 0xFFD83A;
	private static final int STATUS_COLOR = 0xF2EAD0;
	private static final int MESSAGE_COLOR = 0xB8A878;

	private static final int METEOR_OUTER = 0xFFFF8A1A;
	private static final int METEOR_MID = 0xFFFFC21F;
	private static final int METEOR_CORE = 0xFFFFF4A0;
	private static final int FLAME_HEAD = 0xE0FFB020;
	private static final int FLAME_TAIL = 0x20A02000;

	private static final int BAR_WIDTH = 180;
	private static final int BAR_HEIGHT = 6;
	private static final int GRADIENT_BANDS = 32;
	private static final long REDRAW_INTERVAL = 50L;
	// ==========================================================

	private String message = "";
	private final Minecraft mc;
	private String currentlyDisplayedText = "";
	private long systemTime = Minecraft.getSystemTime();
	private boolean loadingSuccess;

	public LoadingScreenRenderer(Minecraft mcIn) {
		this.mc = mcIn;
	}

	public void resetProgressAndMessage(String message) {
		this.loadingSuccess = false;
		this.displayString(message);
	}

	public void displaySavingString(String message) {
		this.loadingSuccess = true;
		this.displayString(message);
	}

	private void displayString(String message) {
		this.currentlyDisplayedText = message;

		if (!this.mc.running) {
			if (!this.loadingSuccess) {
				throw new MinecraftError();
			}
		} else {
			GlStateManager.clear(256);
			setupMatrices(mc.scaledResolution);
		}
	}

	public void displayLoadingString(String message) {
		if (!this.mc.running) {
			if (!this.loadingSuccess) {
				throw new MinecraftError();
			}
		} else {
			this.systemTime = 0L;
			this.message = message;
			this.setLoadingProgress(-1);
			this.systemTime = 0L;
		}
	}

	public void setLoadingProgress(int progress) {
		if (!this.mc.running) {
			if (!this.loadingSuccess) {
				throw new MinecraftError();
			}
			return;
		}

		long now = Minecraft.getSystemTime();
		if (now - this.systemTime < REDRAW_INTERVAL) {
			return;
		}
		this.systemTime = now;

		ScaledResolution res = mc.scaledResolution;
		int w = res.getScaledWidth();
		int h = res.getScaledHeight();

		GlStateManager.clear(256);
		setupMatrices(res);
		GlStateManager.clear(16640);

		drawBackground(w, h);
		drawMeteor(w / 2, h / 2 - 84, now);

		int cx = w / 2;
		int cy = h / 2;

		// Title (2x scale)
		if (!TITLE.isEmpty()) {
			GlStateManager.pushMatrix();
			GlStateManager.translate((float) cx, (float) (cy - 54), 0.0F);
			GlStateManager.scale(2.0F, 2.0F, 1.0F);
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA,
					RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA, RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);
			this.mc.fontRendererObj.drawStringWithShadow(TITLE,
					(float) (-this.mc.fontRendererObj.getStringWidth(TITLE) / 2), 0.0F, TITLE_COLOR);
			GlStateManager.popMatrix();
		}

		// Status line
		drawCentered(this.currentlyDisplayedText, cx, cy - 22, STATUS_COLOR);

		// Progress bar
		int barW = Math.min(BAR_WIDTH, w - 40);
		int x0 = cx - barW / 2;
		int y0 = cy - 8;
		drawProgressBar(x0, y0, barW, progress, now);

		// Percentage
		if (progress >= 0) {
			drawCentered(Math.min(progress, 100) + "%", cx, y0 + BAR_HEIGHT + 6, STATUS_COLOR);
		}

		// Secondary message
		drawCentered(this.message, cx, y0 + BAR_HEIGHT + 20, MESSAGE_COLOR);

		this.mc.updateDisplay();
	}

	private void setupMatrices(ScaledResolution res) {
		GlStateManager.matrixMode(5889);
		GlStateManager.loadIdentity();
		GlStateManager.ortho(0.0D, res.getScaledWidth_double(), res.getScaledHeight_double(), 0.0D, 100.0D, 300.0D);
		GlStateManager.matrixMode(5888);
		GlStateManager.loadIdentity();
		GlStateManager.translate(0.0F, 0.0F, -200.0F);
	}

	private void drawBackground(int w, int h) {
		for (int i = 0; i < GRADIENT_BANDS; ++i) {
			int top = i * h / GRADIENT_BANDS;
			int bottom = (i + 1) * h / GRADIENT_BANDS;
			float t = i / (float) (GRADIENT_BANDS - 1);
			Gui.drawRect(0, top, w, bottom, lerpColor(BG_TOP, BG_BOTTOM, t));
		}
	}

	private void drawProgressBar(int x0, int y0, int barW, int progress, long now) {
		Gui.drawRect(x0 - 1, y0 - 1, x0 + barW + 1, y0 + BAR_HEIGHT + 1, BAR_BORDER);
		Gui.drawRect(x0, y0, x0 + barW, y0 + BAR_HEIGHT, BAR_TRACK);

		int fillX0;
		int fillX1;

		if (progress >= 0) {
			int clamped = Math.min(progress, 100);
			fillX0 = x0;
			fillX1 = x0 + (barW * clamped) / 100;
		} else {
			int segW = Math.max(barW / 4, 8);
			float t = (now % 2000L) / 1000.0F;
			if (t > 1.0F) {
				t = 2.0F - t;
			}
			t = t * t * (3.0F - 2.0F * t);
			fillX0 = x0 + (int) ((barW - segW) * t);
			fillX1 = fillX0 + segW;
		}

		if (fillX1 > fillX0) {
			Gui.drawRect(fillX0, y0, fillX1, y0 + BAR_HEIGHT, BAR_FILL);
			Gui.drawRect(fillX0, y0, fillX1, y0 + BAR_HEIGHT / 3, BAR_FILL_HIGHLIGHT);
		}
	}

	/** Flaming meteor: glowing ball falling down-left with a flickering trail. */
	private void drawMeteor(int cx, int cy, long now) {
		final int segments = 9;

		for (int i = segments - 1; i >= 0; --i) {
			int size = Math.max(2, 9 - i);
			int jx = (int) (((now / 90L) + i * 5L) % 3L) - 1;
			int jy = (int) (((now / 70L) + i * 3L) % 3L) - 1;
			int x = cx + 5 + i * 4 + jx;
			int y = cy - 3 - i * 3 + jy;
			if (y - size / 2 < 0) {
				continue;
			}
			int color = lerpColor(FLAME_HEAD, FLAME_TAIL, i / (float) (segments - 1));
			Gui.drawRect(x - size / 2, y - size / 2, x - size / 2 + size, y - size / 2 + size, color);
		}

		int pulse = ((now / 120L) % 2L == 0L) ? 0 : 1;
		drawDisc(cx, cy, 7, METEOR_OUTER);
		drawDisc(cx, cy, 5, METEOR_MID);
		drawDisc(cx, cy, 2 + pulse, METEOR_CORE);
	}

	private void drawDisc(int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; ++dy) {
			int dx = (int) Math.sqrt((double) (r * r - dy * dy));
			Gui.drawRect(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
		}
	}

	private void drawCentered(String text, int cx, int y, int color) {
		if (text == null || text.isEmpty()) {
			return;
		}
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
				RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);
		this.mc.fontRendererObj.drawStringWithShadow(text,
				(float) (cx - this.mc.fontRendererObj.getStringWidth(text) / 2), (float) y, color);
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

	public void setDoneWorking() {
	}

	public void eaglerShow(String line1, String line2) {
		if (!this.mc.running) {
			if (!this.loadingSuccess) {
				throw new MinecraftError();
			}
		} else {
			this.systemTime = 0L;
			this.currentlyDisplayedText = line1;
			this.message = line2;
			this.setLoadingProgress(-1);
			this.systemTime = 0L;
		}
	}
}
