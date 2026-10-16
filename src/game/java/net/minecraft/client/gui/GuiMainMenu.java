package net.minecraft.client.gui;

import java.io.IOException;

/**
 * Minimal stub that preserves the expected GuiMainMenu symbol without duplicating ClickGuiScreen.
 */
public class GuiMainMenu extends GuiScreen {
    public GuiMainMenu() {
    }

    @Override
    public void initGui() {
        // stub
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
