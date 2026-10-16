package net.minecraft.client.gui;

import java.io.IOException;

/**
 * GuiMainMenu redirects to ClickGuiScreen to show the modded main menu.
 */
public class GuiMainMenu extends GuiScreen {
    public GuiMainMenu() {
    }

    @Override
    public void initGui() {
        // Open the ClickGuiScreen so the main menu is not a black stub.
        this.mc.displayGuiScreen(new ClickGuiScreen());
    }
}
