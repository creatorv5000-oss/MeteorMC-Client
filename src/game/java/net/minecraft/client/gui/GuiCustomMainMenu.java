package net.minecraft.client.gui;

import java.io.IOException;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import org.lwjgl.opengl.GL11;

/**
 * A separate Meteor-styled main menu. This file provides a visually distinct
 * GUI while keeping the original GuiMainMenu in place for compatibility.
 */
public class GuiCustomMainMenu extends GuiScreen {

    private static final String TITLE = "METEOR MC";
    private static final float TITLE_SCALE = 3.2F;

    private static final int BG_COLOR = 0xFF071018; // deep meteor-space
    private static final int PANEL_COLOR = 0xAA111218;
    private static final int ACCENT_A = 0xFFFF8C42; // bright ember
    private static final int ACCENT_B = 0xFFFFD28A; // pale gold

    public GuiCustomMainMenu() {
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int bw = 210;
        int bh = 20;
        int cx = this.width / 2;
        int startY = this.height / 2 - 8;

        this.buttonList.add(new GuiButton(1, cx - bw / 2, startY - 26, bw, bh, I18n.format("menu.singleplayer")));
        this.buttonList.add(new GuiButton(2, cx - bw / 2, startY - 26 + 26, bw, bh, I18n.format("menu.multiplayer")));
        this.buttonList.add(new GuiButton(14, cx - bw / 2, startY - 26 + 52, bw, bh, "Meteor Settings"));
        this.buttonList.add(new GuiButton(0, cx - bw / 2, startY - 26 + 78, bw, bh, I18n.format("menu.options")));
        this.buttonList.add(new GuiButton(4, cx - bw / 2, startY - 26 + 104, bw, bh, I18n.format("menu.quit")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) this.mc.displayGuiScreen(new GuiWorldSelection(this));
        if (button.id == 2) this.mc.displayGuiScreen(new GuiMultiplayer(this));
        if (button.id == 0) this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
        if (button.id == 4) this.mc.shutdown();
        if (button.id == 14) this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();

        // background
        drawRect(0, 0, this.width, this.height, BG_COLOR);

        // soft panel behind buttons
        int panelW = 260;
        int panelH = 180;
        int px = this.width / 2 - panelW / 2;
        int py = this.height / 2 - panelH / 2;
        drawRect(px, py, px + panelW, py + panelH, PANEL_COLOR);

        // subtle meteor streaks
        drawMeteorStreaks(now);

        // draw buttons (super draws the buttons)
        super.drawScreen(mouseX, mouseY, partialTicks);

        // draw title with Meteor styling
        drawMeteorTitle(now);

        // version and credit
        String ver = "VERSION 1.12.2";
        this.fontRendererObj.drawStringWithShadow(ver, 8, this.height - 12, 0xFFBFA06B);
        String credit = "Meteor Client";
        int cw = this.fontRendererObj.getStringWidth(credit);
        this.fontRendererObj.drawStringWithShadow(credit, this.width - cw - 8, this.height - 12, 0xFFBFC9D6);
    }

    private void drawMeteorTitle(long now) {
        int cx = this.width / 2;
        int total = this.fontRendererObj.getStringWidth(TITLE);

        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, 50.0F, 0.0F);
        GlStateManager.scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        int x = -total / 2;

        // draw glow passes
        for (int g = 0; g < 3; g++) {
            float alphaFactor = 0.06F + 0.04F * g;
            int glow = withAlpha(ACCENT_A, alphaFactor);
            this.fontRendererObj.drawString(TITLE, x - g, 0 - g * 0.4F, glow, false);
        }

        // main gradient draw per-character
        for (int i = 0; i < TITLE.length(); i++) {
            String s = String.valueOf(TITLE.charAt(i));
            float t = (float) i / Math.max(1, TITLE.length() - 1);
            int col = lerpColor(ACCENT_A, ACCENT_B, t) | 0xFF000000;
            float bob = (float) Math.sin((now + i * 150L) * 0.004D) * 1.2F;
            this.fontRendererObj.drawString(s, x, bob, col, true);
            x += this.fontRendererObj.getStringWidth(s);
        }

        GlStateManager.popMatrix();

        // small underline
        int lineW = (int) (total * TITLE_SCALE);
        int lx0 = cx - lineW / 2;
        int ly = 50 + (int) (9 * TITLE_SCALE) + 6;
        Gui.drawRect(lx0, ly, lx0 + lineW, ly + 2, ACCENT_A);
    }

    private void drawMeteorStreaks(long now) {
        int w = this.width;
        int h = this.height;
        for (int s = 0; s < 6; s++) {
            long seed = now + s * 1999L;
            float p = (seed % 3000L) / 3000.0F;
            int sx = (int) (p * (w + 200)) - 100;
            int sy = 30 + (s * 24);
            int ex = sx - 120;
            int ey = sy + 18;
            int alpha = 40 + (int) (120 * (1.0F - Math.abs(0.5F - p) * 2.0F));
            int color = (alpha << 24) | (ACCENT_A & 0xFFFFFF);
            drawGradientRect(sx, sy, ex, sy + 2, color, 0);
            drawGradientRect(sx, sy + 2, ex + 10, sy + 3, color, 0);
        }
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

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
