package net.minecraft.client.gui;

import java.io.IOException;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import org.lwjgl.opengl.GL11;

public class GuiCustomMainMenu extends GuiScreen {

    // --- Configuration & Theme Constants ---
    private static final String TITLE = "METEOR MC";
    private static final float TITLE_SCALE = 3.5F;
    
    // Core color theme palette (ARGB)
    private static final int TITLE_AURA = 0x55FFAA00;     // Soft Amber
    private static final int TITLE_COLOR_A = 0xFFFFE680;  // Bright Gold
    private static final int TITLE_COLOR_B = 0xFFFF5500;  // Deep Fire Orange
    
    private static final int FLAME_HEAD = 0xFFFFCC00;
    private static final int FLAME_TAIL = 0xFFCC3300;
    private static final int METEOR_OUTER = 0xAAFF3300;
    private static final int METEOR_CORE = 0xFFFFFFFF;

    public GuiCustomMainMenu() {
        // Constructor
    }

    /**
     * Adds the standard singleplayer, multiplayer, and quit buttons down the center.
     */
    @Override
    public void initGui() {
        int buttonHeight = 20;
        int buttonWidth = 200;
        int spacing = 24;
        // Start layout below the animated title subtitles (roughly y=150)
        int startY = this.height / 2 - 10; 

        this.buttonList.clear();
        
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, startY, buttonWidth, buttonHeight, I18n.format("menu.singleplayer")));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 100, startY + spacing, buttonWidth, buttonHeight, I18n.format("menu.multiplayer")));
        this.buttonList.add(new GuiButton(14, this.width / 2 - 100, startY + (spacing * 2), buttonWidth, buttonHeight, "Meteor Options"));
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, startY + (spacing * 3), buttonWidth, buttonHeight, I18n.format("menu.options")));
        this.buttonList.add(new GuiButton(4, this.width / 2 - 100, startY + (spacing * 4), buttonWidth, buttonHeight, I18n.format("menu.quit")));
    }

    /**
     * Handles button click actions natively.
     */
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
        if (button.id == 1) this.mc.displayGuiScreen(new GuiWorldSelection(this));
        if (button.id == 2) this.mc.displayGuiScreen(new GuiMultiplayer(this));
        if (button.id == 4) this.mc.shutdown();
        if (button.id == 14) {
            // Optional: Route to custom client setting panel if you have one
            this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings)); 
        }
    }

    /**
     * Core screen rendering framework.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();

        // 1. Draw solid dark background space aesthetic
        drawRect(0, 0, this.width, this.height, 0xFF0B0C10);

        // 2. Render background meteors & embers layer
        this.drawBackgroundMeteors(now);
        this.drawEmbers(now);

        // 3. Draw ambient vignette cinematic gradient layout
        this.drawGradientRect(0, 0, this.width, this.height, 0x33000000, 0x99000000);

        // 4. Render native button layouts & UI layers
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 5. Draw foreground elements (Animated Titles & Subtext overlay)
        this.drawAnimatedTitle(now);
    }

    /** Background passive meteor showers */
    private void drawBackgroundMeteors(long now) {
        // Fallback placeholder logic for your snippet variables
        for (int i = 0; i < 3; i++) {
            long individualSeed = now + (i * 4500L);
            float progress = (individualSeed % 3500L) / 3500.0F;
            float fade = progress < 0.2F ? progress / 0.2F : (progress > 0.8F ? (1.0F - progress) / 0.2F : 1.0F);
            
            int startX = (this.width / 4) * (i + 1) + 100;
            int startY = -50;
            
            int hx = startX - (int)(progress * 300);
            int hy = startY + (int)(progress * 400);

            for (int j = 0; j < 12; ++j) {
                int tx = hx + (int) (j * 3.5F);
                int ty = hy - (int) (j * 4.5F);
                int size = 2 + (j / 4);
                int c = withAlpha(lerpColor(FLAME_HEAD, FLAME_TAIL, j / 11.0F), fade);
                Gui.drawRect(tx, ty, tx + size, ty + size, c);
            }
            Gui.drawRect(hx - 2, hy - 2, hx + 3, hy + 3, withAlpha(METEOR_OUTER, fade));
            Gui.drawRect(hx - 1, hy - 1, hx + 2, hy + 2, withAlpha(METEOR_CORE, fade));
        }
    }

    /** Small sparks rising around the title, fading as they go. */
    private void drawEmbers(long now) {
        int cx = this.width / 2;
        for (int k = 0; k < 16; ++k) {
            float life = ((now + k * 397L) % 2000L) / 2000.0F;
            int x = cx - 120 + (k * 37) % 241 + (int) (Math.sin(life * 6.0F + k) * 4.0D);
            int y = 82 - (int) (life * 60.0F);
            int a = (int) ((1.0F - life) * 255.0F);
            int size = (k % 3 == 0) ? 2 : 1;
            int rgb = lerpColor(0xFFFFE680, 0xFFFF6A00, life) & 0xFFFFFF;
            Gui.drawRect(x, y, x + size, y + size, (a << 24) | rgb);
        }
    }

    /** Animated gold MeteorMC title with version and client label. */
    private void drawAnimatedTitle(long now) {
        int cx = this.width / 2;
        int total = this.fontRendererObj.getStringWidth(TITLE);
        int len = TITLE.length();
        float sweep = ((now % 2600L) / 2600.0F) * (len + 6) - 3.0F;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) cx, 30.0F, 0.0F);
        GlStateManager.scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

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
        int ly = 30 + (int) (9 * TITLE_SCALE) + 4;
        Gui.drawRect(lx0, ly, lx0 + lineW, ly + 1, 0xFF7A6422);
        
        int streakW = 40;
        int pos = lx0 + (int) (((now % 1800L) / 1800.0F) * (lineW + streakW)) - streakW;
        int s0 = Math.max(lx0, pos);
        int s1 = Math.min(lx0 + lineW, pos + streakW);
        if (s1 > s0) {
            Gui.drawRect(s0, ly, s1, ly + 1, 0xFFFFE680);
        }

        String version = "VERSION 1.12.2";
        int versionWidth = this.fontRendererObj.getStringWidth(version);
        this.fontRendererObj.drawStringWithShadow(version, cx - versionWidth / 2, ly + 7, 0xFFFFD36A);

        String clientLabel = "NON HACKED CLIENT";
        int labelWidth = this.fontRendererObj.getStringWidth(clientLabel);
        this.fontRendererObj.drawStringWithShadow(clientLabel, cx - labelWidth / 2, ly + 19, 0xFFE8E0CC);
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
