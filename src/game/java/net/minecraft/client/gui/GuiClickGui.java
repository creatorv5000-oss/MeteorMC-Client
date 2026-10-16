package net.minecraft.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Meteor-styled Click GUI screen.
 *
 * This is intentionally additive: it does not remove or modify existing mods,
 * it only adds a new GUI panel system for toggles and visual settings.
 */
public class GuiClickGui extends GuiScreen {

    private final List<Panel> panels = new ArrayList<>();

    public GuiClickGui() {
    }

    @Override
    public void initGui() {
        this.panels.clear();

        Panel combat = new Panel("Combat", 20, 40, 150, 170);
        combat.addButton(new Toggle("KillAura", false));
        combat.addButton(new Toggle("AutoClicker", true));
        combat.addButton(new Toggle("Criticals", false));
        combat.addButton(new Toggle("Velocity", false));

        Panel render = new Panel("Render", 190, 40, 160, 170);
        render.addButton(new Toggle("FullBright", true));
        render.addButton(new Toggle("ESP", false));
        render.addButton(new Toggle("Nametags", true));
        render.addButton(new Toggle("Tracers", false));

        Panel movement = new Panel("Movement", 370, 40, 170, 160);
        movement.addButton(new Toggle("Sprint", true));
        movement.addButton(new Toggle("Fly", false));
        movement.addButton(new Toggle("Speed", false));

        this.panels.add(combat);
        this.panels.add(render);
        this.panels.add(movement);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, this.width, this.height, 0xB0000000);

        for (Panel panel : this.panels) {
            panel.draw(this.fontRendererObj, mouseX, mouseY);
        }

        String hint = "Meteor Click GUI - ESC to close";
        int hintWidth = this.fontRendererObj.getStringWidth(hint);
        this.fontRendererObj.drawStringWithShadow(hint, (this.width - hintWidth) / 2, this.height - 14, 0xFFBFA06B);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (int i = this.panels.size() - 1; i >= 0; --i) {
            Panel panel = this.panels.get(i);
            if (panel.isOverHeader(mouseX, mouseY) && mouseButton == 0) {
                panel.startDrag(mouseX, mouseY);
                this.panels.remove(i);
                this.panels.add(panel);
                return;
            }
            if (panel.mouseClicked(mouseX, mouseY, mouseButton)) {
                this.panels.remove(i);
                this.panels.add(panel);
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (Panel panel : this.panels) {
            panel.stopDrag();
            panel.mouseReleased(mouseX, mouseY, state);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        for (Panel panel : this.panels) {
            if (panel.isDragging()) {
                panel.dragTo(mouseX, mouseY);
            }
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.mc.displayGuiScreen((GuiScreen) null);
            if (this.mc.currentScreen == null) {
                this.mc.setIngameFocus();
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private static class Panel {
        private final String title;
        private int x;
        private int y;
        private final int width;
        private final int height;
        private final int headerHeight = 18;
        private boolean dragging;
        private int dragOffsetX;
        private int dragOffsetY;
        private final List<Toggle> buttons = new ArrayList<>();

        Panel(String title, int x, int y, int width, int height) {
            this.title = title;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        void addButton(Toggle toggle) {
            this.buttons.add(toggle);
        }

        void draw(FontRenderer fontRendererObj, int mouseX, int mouseY) {
            Gui.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xAA0F1115);
            Gui.drawRect(this.x, this.y, this.x + this.width, this.y + this.headerHeight, 0xFF2B1A0F);
            drawRectOutline(this.x, this.y, this.x + this.width, this.y + this.height, 0x44000000);

            fontRendererObj.drawStringWithShadow(this.title, this.x + 6, this.y + 4, 0xFFFFC080);

            int by = this.y + this.headerHeight + 6;
            int pad = 6;
            for (Toggle toggle : this.buttons) {
                int btnX = this.x + pad;
                int btnY = by;
                int btnW = this.width - (pad * 2);
                int btnH = 16;

                Gui.drawRect(btnX, btnY, btnX + btnW, btnY + btnH, toggle.active ? 0xDD2B1A0F : 0x882B2B2B);
                fontRendererObj.drawStringWithShadow(toggle.label, btnX + 6, btnY + 3, toggle.active ? 0xFFFFFFFF : 0xFFD0D0D0);

                int indX = btnX + btnW - 12;
                int indY = btnY + 4;
                Gui.drawRect(indX, indY, indX + 8, indY + 8, toggle.active ? 0xFF8CF0FF : 0x44000000);

                by += btnH + 6;
            }
        }

        boolean isOverHeader(int mouseX, int mouseY) {
            return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.headerHeight;
        }

        void startDrag(int mouseX, int mouseY) {
            this.dragging = true;
            this.dragOffsetX = mouseX - this.x;
            this.dragOffsetY = mouseY - this.y;
        }

        void stopDrag() {
            this.dragging = false;
        }

        boolean isDragging() {
            return this.dragging;
        }

        void dragTo(int mouseX, int mouseY) {
            this.x = mouseX - this.dragOffsetX;
            this.y = mouseY - this.dragOffsetY;
        }

        boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
            if (mouseButton != 0) return false;

            int by = this.y + this.headerHeight + 6;
            int pad = 6;
            int btnW = this.width - (pad * 2);
            for (Toggle toggle : this.buttons) {
                int btnX = this.x + pad;
                int btnY = by;
                int btnH = 16;

                if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                    toggle.active = !toggle.active;
                    return true;
                }

                by += btnH + 6;
            }
            return false;
        }

        void mouseReleased(int mouseX, int mouseY, int state) {
            // no-op; drag is stopped on release
        }

        private void drawRectOutline(int x0, int y0, int x1, int y1, int color) {
            Gui.drawRect(x0, y0, x1, y0 + 1, color);
            Gui.drawRect(x0, y1 - 1, x1, y1, color);
            Gui.drawRect(x0, y0, x0 + 1, y1, color);
            Gui.drawRect(x1 - 1, y0, x1, y1, color);
        }
    }

    private static class Toggle {
        private final String label;
        private boolean active;

        Toggle(String label, boolean active) {
            this.label = label;
            this.active = active;
        }
    }
}
