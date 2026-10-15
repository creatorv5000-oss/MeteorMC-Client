package net.minecraft.client.gui;

import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.RealOpenGLEnums;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;

public class GuiButton extends Gui {
	protected static final ResourceLocation BUTTON_TEXTURES = new ResourceLocation("textures/gui/widgets.png");

	/** Button width in pixels */
	public int width;

	/** Button height in pixels */
	protected int height;

	/** The x position of this control. */
	public int xPosition;

	/** The y position of this control. */
	public int yPosition;

	/** The string displayed on this control. */
	public String displayString;
	public int id;

	/** True if this control is enabled, false to disable. */
	public boolean enabled;

	/** Hides the button completely if false. */
	public boolean visible;
	protected boolean hovered;

	public GuiButton(int buttonId, int x, int y, String buttonText) {
		this(buttonId, x, y, 200, 20, buttonText);
	}

	public GuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
		this.width = 200;
		this.height = 20;
		this.enabled = true;
		this.visible = true;
		this.id = buttonId;
		this.xPosition = x;
		this.yPosition = y;
		this.width = widthIn;
		this.height = heightIn;
		this.displayString = buttonText;
	}

	/**
	 * Returns 0 if the button is disabled, 1 if the mouse is NOT hovering over this
	 * button and 2 if it IS hovering over this button.
	 */
	protected int getHoverState(boolean mouseOver) {
		int i = 1;

		if (!this.enabled) {
			i = 0;
		} else if (mouseOver) {
			i = 2;
		}

		return i;
	}

	// ===================== BUTTON STYLE =====================
	private static final int BORDER = 0xFF7A6422;
	private static final int BORDER_HOVER = 0xFFFFC21F;
	private static final int BORDER_DISABLED = 0xFF3A3322;

	private static final int FILL_TOP = 0xFF2E260E;
	private static final int FILL_BOTTOM = 0xFF1A150A;
	private static final int FILL_HOVER_TOP = 0xFF5A4512;
	private static final int FILL_HOVER_BOTTOM = 0xFF33270A;
	private static final int FILL_DISABLED_TOP = 0xFF1C1A12;
	private static final int FILL_DISABLED_BOTTOM = 0xFF14120C;

	private static final int HIGHLIGHT = 0x40FFE680;

	private static final int TEXT = 0xF2EAD0;
	private static final int TEXT_HOVER = 0xFFD83A;
	private static final int TEXT_DISABLED = 0x7A7458;
	// ========================================================

	public void func_191745_a(Minecraft p_191745_1_, int p_191745_2_, int p_191745_3_, float p_191745_4_) {
		if (this.visible) {
			FontRenderer fontrenderer = p_191745_1_.fontRendererObj;
			this.hovered = p_191745_2_ >= this.xPosition && p_191745_3_ >= this.yPosition
					&& p_191745_2_ < this.xPosition + this.width && p_191745_3_ < this.yPosition + this.height;
			int i = this.getHoverState(this.hovered);

			int x0 = this.xPosition;
			int y0 = this.yPosition;
			int x1 = x0 + this.width;
			int y1 = y0 + this.height;

			int border = i == 0 ? BORDER_DISABLED : (i == 2 ? BORDER_HOVER : BORDER);
			int top = i == 0 ? FILL_DISABLED_TOP : (i == 2 ? FILL_HOVER_TOP : FILL_TOP);
			int bottom = i == 0 ? FILL_DISABLED_BOTTOM : (i == 2 ? FILL_HOVER_BOTTOM : FILL_BOTTOM);

			drawRect(x0, y0, x1, y1, border);
			this.drawGradientRect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, top, bottom);
			if (i != 0) {
				drawRect(x0 + 1, y0 + 1, x1 - 1, y0 + 2, HIGHLIGHT);
			}

			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
					RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);
			GlStateManager.blendFunc(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA);
			this.mouseDragged(p_191745_1_, p_191745_2_, p_191745_3_);

			int j = TEXT;

			if (!this.enabled) {
				j = TEXT_DISABLED;
			} else if (this.hovered) {
				j = TEXT_HOVER;
			}

			this.drawCenteredString(fontrenderer, this.displayString, this.xPosition + this.width / 2,
					this.yPosition + (this.height - 8) / 2, j);
		}
	}

	/**
	 * Fired when the mouse button is dragged. Equivalent of
	 * MouseListener.mouseDragged(MouseEvent e).
	 */
	protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
	}

	/**
	 * Fired when the mouse button is released. Equivalent of
	 * MouseListener.mouseReleased(MouseEvent e).
	 */
	public void mouseReleased(int mouseX, int mouseY) {
	}

	/**
	 * Returns true if the mouse has been pressed on this control. Equivalent of
	 * MouseListener.mousePressed(MouseEvent e).
	 */
	public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
		return this.enabled && this.visible && mouseX >= this.xPosition && mouseY >= this.yPosition
				&& mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
	}

	/**
	 * Whether the mouse cursor is currently over the button.
	 */
	public boolean isMouseOver() {
		return this.hovered;
	}

	public void drawButtonForegroundLayer(int mouseX, int mouseY) {
	}

	public void playPressSound(SoundHandler soundHandlerIn) {
		soundHandlerIn.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	public int getButtonWidth() {
		return this.width;
	}

	public void setWidth(int width) {
		this.width = width;
	}
}
