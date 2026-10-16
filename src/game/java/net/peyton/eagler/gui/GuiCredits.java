package net.peyton.eagler.gui;

import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;

/**
 * Minimal standalone GuiCredits shim to satisfy builds when the original
 * Eagler GUI package is not present in the classpath. This provides a
 * simple credits screen and two constructors (GuiScreen) and
 * (GuiScreen, String) to match the calls in GuiMainMenu.
 *
 * If your project ships a richer GuiCredits implementation, replace
 * this file with that implementation instead.
 */
public class GuiCredits extends GuiScreen {
	private final GuiScreen parent;
	private final String resourcePath;

	public GuiCredits(GuiScreen parent) {
		this(parent, null);
	}

	public GuiCredits(GuiScreen parent, String resourcePath) {
		this.parent = parent;
		this.resourcePath = resourcePath;
	}

	@Override
	public void initGui() {
		// no-op: keep screen responsive. A fuller implementation could load
		// and parse the credits file referenced by resourcePath here.
	}

	@Override
	public void onGuiClosed() {
		// return to parent when closed
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		// allow default behavior (e.g., ESC handled by parent gui framework)
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(this.fontRendererObj, "Credits", this.width / 2, 20, 0xFFFFFF);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
