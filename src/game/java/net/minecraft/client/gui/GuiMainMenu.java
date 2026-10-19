package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.EaglercraftRandom;
import net.lax1dude.eaglercraft.EaglercraftVersion;
import net.lax1dude.eaglercraft.IOUtils;
import net.lax1dude.eaglercraft.internal.PlatformApplication;
import net.lax1dude.eaglercraft.internal.PlatformOpenGL;
import net.lax1dude.eaglercraft.minecraft.MainMenuSkyboxTexture;
import net.lax1dude.eaglercraft.opengl.WorldRenderer;
import net.lax1dude.eaglercraft.sp.SingleplayerServerController;
import net.lax1dude.eaglercraft.sp.gui.GuiScreenIntegratedServerBusy;
import net.lax1dude.eaglercraft.sp.gui.GuiScreenIntegratedServerStartup;
import net.lax1dude.eaglercraft.opengl.EaglercraftGPU;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.lax1dude.eaglercraft.opengl.RealOpenGLEnums;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResource;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServerDemo;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldInfo;
import net.peyton.eagler.gui.GuiCredits;
import net.lax1dude.eaglercraft.profile.*;

public class GuiMainMenu extends GuiScreen {
	private static final EaglercraftRandom RANDOM = new EaglercraftRandom();

	/** Counts the number of screen updates. */
	private final float updateCounter;

	/** The splash message. */
	private String splashText;
	private GuiButton buttonResetDemo;

	/** Timer used to rotate the panorama, increases every tick. */
	private float panoramaTimer;

	/**
	 * Texture allocated for the current viewport of the main menu's panorama
	 * background.
	 */
	private static MainMenuSkyboxTexture viewportTexture = null;
	private static MainMenuSkyboxTexture viewportTexture2 = null;

	/**
	 * The Object object utilized as a thread lock when performing non thread-safe
	 * operations
	 */
	// private final Object threadLock = new Object();
	public static final String MORE_INFO_TEXT = "Please click " + TextFormatting.UNDERLINE + "here"
			+ TextFormatting.RESET + " for more information.";

	private static final ResourceLocation SPLASH_TEXTS = new ResourceLocation("texts/splashes.txt");
	private static final ResourceLocation MINECRAFT_TITLE_TEXTURES = new ResourceLocation(
			"textures/gui/title/minecraft.png");
	private static final ResourceLocation field_194400_H = new ResourceLocation("textures/gui/title/edition.png");

	/** An array of all the paths to the panorama pictures. */
	private static final ResourceLocation[] TITLE_PANORAMA_PATHS = new ResourceLocation[] {
			new ResourceLocation("textures/gui/title/background/panorama_0.png"),
			new ResourceLocation("textures/gui/title/background/panorama_1.png"),
			new ResourceLocation("textures/gui/title/background/panorama_2.png"),
			new ResourceLocation("textures/gui/title/background/panorama_3.png"),
			new ResourceLocation("textures/gui/title/background/panorama_4.png"),
			new ResourceLocation("textures/gui/title/background/panorama_5.png") };
	private ResourceLocation backgroundTexture = null;
	private static ResourceLocation backgroundTexture2 = null;

	private int field_193978_M;
	private int field_193979_N;

	// ===================== METEORMC STYLE =====================
	/** false = animated space background, true = original panorama. */
	private static final boolean USE_PANORAMA = true;

	private static final String TITLE = "MeteorMC";
	private static final float TITLE_SCALE = 5.0F;

	private static final int BG_TOP = 0xFF5F86AE;
	private static final int BG_BOTTOM = 0xFFCFE3F5;

	private static final int TITLE_COLOR_A = 0xFFEAF6FF;
	private static final int TITLE_COLOR_B = 0xFFFFFFFF;
	private static final int TITLE_AURA = 0x4FA8FF;

	private static final int METEOR_OUTER = 0xFFBEE3FF;
	private static final int METEOR_CORE = 0xFFFFFFFF;
	private static final int FLAME_HEAD = 0xE0BEE3FF;
	private static final int FLAME_TAIL = 0x20FFFFFF;

	private static final int STAR_COUNT = 90;
	private static final int METEOR_COUNT = 4;
	// ==========================================================

	private int[] starX;
	private int[] starY;
	private int[] starSize;

	public GuiMainMenu() {
		this.splashText = "missingno";
		IResource iresource = null;

		try {
			List<String> list = Lists.<String>newArrayList();
			iresource = Minecraft.getMinecraft().getResourceManager().getResource(SPLASH_TEXTS);
			BufferedReader bufferedreader = new BufferedReader(
					new InputStreamReader(iresource.getInputStream(), StandardCharsets.UTF_8));
			String s;

			while ((s = bufferedreader.readLine()) != null) {
				s = s.trim();

				if (!s.isEmpty()) {
					list.add(s);
				}
			}

			if (!list.isEmpty()) {
				while (true) {
					this.splashText = list.get(RANDOM.nextInt(list.size()));

					if (this.splashText.hashCode() != 125780783) {
						break;
					}
				}
			}
		} catch (IOException var8) {
			;
		} finally {
			IOUtils.closeQuietly((Closeable) iresource);
		}

		this.updateCounter = RANDOM.nextFloat();
	}

	/**
	 * Called from the main game loop to update the screen.
	 */
	public void updateScreen() {
	}

	/**
	 * Returns true if this GUI should pause the game when it is displayed in
	 * single-player
	 */
	public boolean doesGuiPauseGame() {
		return false;
	}

	/**
	 * Fired when a key is typed (except F11 which toggles full screen). This is the
	 * equivalent of KeyListener.keyTyped(KeyEvent e). Args : character (character
	 * on the key), keyCode (lwjgl Keyboard key code)
	 */
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
	}

	/**
	 * Adds the buttons (and other controls) to the screen in question. Called when
	 * the GUI is displayed and when the window resizes, the buttonList is cleared
	 * beforehand.
	 */
	public void initGui() {
		if (!this.mc.gameSettings.hasSeenFirstLoad) {
			this.mc.displayGuiScreen(new GuiScreenFirstLoad(this.mc.gameSettings));
			return;
		}

		viewportTexture = new MainMenuSkyboxTexture(256, 256);
		this.backgroundTexture = this.mc.getTextureManager().getDynamicTextureLocation("background", this.viewportTexture);
		viewportTexture2 = new MainMenuSkyboxTexture(256, 256);
		backgroundTexture2 = this.mc.getTextureManager().getDynamicTextureLocation("background", viewportTexture2);
		this.field_193978_M = this.fontRendererObj.getStringWidth("Resources copyright Mojang AB");
		this.field_193979_N = this.width - this.field_193978_M - 2;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());

		if (calendar.get(2) + 1 == 12 && calendar.get(5) == 24) {
			this.splashText = I18n.format("eaglercraft.splashes.xmas");
		} else if (calendar.get(2) + 1 == 1 && calendar.get(5) == 1) {
			this.splashText = I18n.format("eaglercraft.splashes.newyear");
		} else if (calendar.get(2) + 1 == 10 && calendar.get(5) == 31) {
			this.splashText = I18n.format("eaglercraft.splashes.halloween");
		}

		this.generateStars();

		int colX = this.menuLeft();
		int colW = this.menuWidth();
		int y = this.menuTop();
		int rh = this.rowH();
		int step = this.rowStep();
		boolean demo = this.mc.isDemo();

		this.buttonList.add(new GuiMeteorButton(demo ? 11 : 1, colX, y, colW, rh,
				demo ? "Play Demo" : I18n.format("menu.singleplayer"), GuiMeteorButton.Style.GLASS,
				GuiMeteorButton.ICON_PERSON));
		y += step;

		if (demo) {
			this.buttonResetDemo = this.addButton(new GuiMeteorButton(12, colX, y, colW, rh,
					I18n.format("menu.resetdemo"), GuiMeteorButton.Style.GLASS, GuiMeteorButton.ICON_GLOBE));
			ISaveFormat isaveformat = this.mc.getSaveLoader();
			WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");

			if (worldinfo == null) {
				this.buttonResetDemo.enabled = false;
			}
		} else {
			this.buttonList.add(new GuiMeteorButton(2, colX, y, colW, rh, I18n.format("menu.multiplayer"),
					GuiMeteorButton.Style.GLASS, GuiMeteorButton.ICON_GLOBE));
		}
		y += step;
		this.buttonList.add(new GuiMeteorButton(20, colX, y, colW, rh, "Mods & Client Settings",
				GuiMeteorButton.Style.GLASS, GuiMeteorButton.ICON_GRID));
		y += step;
		this.buttonList.add(new GuiMeteorButton(0, colX, y, colW, rh, I18n.format("menu.options"),
				GuiMeteorButton.Style.GLASS, GuiMeteorButton.ICON_GEAR));
		y += step;
		this.buttonList.add(new GuiMeteorButton(21, colX, y, colW, rh, "Credits", GuiMeteorButton.Style.GLASS,
				GuiMeteorButton.ICON_STAR));
		y += rh + 8;

		int ib = rh;
		int ix = colX + (colW - (3 * ib + 10)) / 2;
		this.buttonList.add(new GuiMeteorButton(4, ix, y, ib, ib, "Edit Profile", GuiMeteorButton.Style.ICON,
				GuiMeteorButton.ICON_PERSON));
		this.buttonList.add(new GuiMeteorButton(5, ix + ib + 5, y, ib, ib, "Language", GuiMeteorButton.Style.ICON,
				GuiMeteorButton.ICON_GLOBE));
		this.buttonList.add(new GuiMeteorButton(14, ix + 2 * (ib + 5), y, ib, ib, "Eaglercraft Credits",
				GuiMeteorButton.Style.ICON, GuiMeteorButton.ICON_INFO));
	}

	/**
	 * Adds Singleplayer and Multiplayer buttons on Main Menu for players who have
	 * bought the game.
	 */
	private void addSingleplayerMultiplayerButtons(int p_73969_1_, int p_73969_2_) {
		this.buttonList.add(new GuiButton(1, this.width / 2 - 100, p_73969_1_, I18n.format("menu.singleplayer")));
		this.buttonList.add(
				new GuiButton(2, this.width / 2 - 100, p_73969_1_ + p_73969_2_ * 1, I18n.format("menu.multiplayer")));
		this.buttonList
				.add(new GuiButton(14, this.width / 2 - 100, p_73969_1_ + p_73969_2_ * 2, I18n.format("menu.credits")));
	}

	/**
	 * Adds Demo buttons on Main Menu for players who are playing Demo.
	 */
	private void addDemoButtons(int p_73972_1_, int p_73972_2_) {
		this.buttonList.add(new GuiButton(11, this.width / 2 - 100, p_73972_1_, I18n.format("menu.playdemo")));
		this.buttonResetDemo = this.addButton(
				new GuiButton(12, this.width / 2 - 100, p_73972_1_ + p_73972_2_ * 1, I18n.format("menu.resetdemo")));
		ISaveFormat isaveformat = this.mc.getSaveLoader();
		WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");

		if (worldinfo == null) {
			this.buttonResetDemo.enabled = false;
		}
	}

	/**
	 * Called by the controls from the buttonList when activated. (Mouse pressed for
	 * buttons)
	 */
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) {
			this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
		}

		if (button.id == 5) {
			this.mc.displayGuiScreen(new GuiLanguage(this, this.mc.gameSettings, this.mc.getLanguageManager()));
		}

		if (button.id == 1) {
			this.mc.displayGuiScreen(new GuiScreenIntegratedServerStartup(this));
		}

		if (button.id == 2) {
			this.mc.displayGuiScreen(new GuiMultiplayer(this));
		}

		if (button.id == 4) {
			this.mc.displayGuiScreen(new GuiScreenEditProfile(this));
		}

		if (button.id == 11) {
			this.mc.launchIntegratedServer("Demo_World", "Demo_World", WorldServerDemo.DEMO_WORLD_SETTINGS);
		}

		if (button.id == 12) {
			ISaveFormat isaveformat = this.mc.getSaveLoader();
			WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");

			if (worldinfo != null) {
				this.mc.displayGuiScreen(new GuiYesNo(this, I18n.format("selectWorld.deleteQuestion"),
						"'" + worldinfo.getWorldName() + "' " + I18n.format("selectWorld.deleteWarning"),
						I18n.format("selectWorld.deleteButton"), I18n.format("gui.cancel"), 12));
			}
		}

		if (button.id == 20) {
			this.mc.displayGuiScreen(new ClickGuiScreen(this));
		}

		if (button.id == 21) {
			this.mc.displayGuiScreen(new GuiMeteorCredits(this));
		}

		if (button.id == 14) {
			this.mc.displayGuiScreen(new GuiCredits(this, "/assets/eagler/credits.txt"));
		}
	}

	public void confirmClicked(boolean result, int id) {
		if (result && id == 12) {
			ISaveFormat isaveformat = this.mc.getSaveLoader();
			isaveformat.flushCache();
			isaveformat.deleteWorldDirectory("Demo_World");
			this.mc.displayGuiScreen(new GuiScreenIntegratedServerBusy(this, "singleplayer.busy.deleting",
					"singleplayer.failed.deleting", SingleplayerServerController::isReady));
		} else if (id == 12) {
			this.mc.displayGuiScreen(this);
		}
	}

	/**
	 * Draws the main menu panorama
	 */
	private void drawPanorama(int mouseX, int mouseY, float partialTicks) {
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer bufferbuilder = tessellator.getBuffer();
		GlStateManager.matrixMode(5889);
		GlStateManager.pushMatrix();
		GlStateManager.loadIdentity();
		GlStateManager.gluPerspective(120.0F, 1.0F, 0.05F, 10.0F);
		GlStateManager.matrixMode(5888);
		GlStateManager.pushMatrix();
		GlStateManager.loadIdentity();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.disableAlpha();
		GlStateManager.disableCull();
		GlStateManager.depthMask(false);
		GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
				RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);
		byte b0 = 4;

		for (int i = 0; i < b0 * b0; ++i) {
			GlStateManager.pushMatrix();
			float f = ((float) (i % b0) / (float) b0 - 0.5F) / 64.0F;
			float f1 = ((float) (i / b0) / (float) b0 - 0.5F) / 64.0F;
			float f2 = 0.0F;
			GlStateManager.translate(f, f1, f2);
			GlStateManager.rotate(MathHelper.sin(((float) this.panoramaTimer + partialTicks) / 400.0F) * 25.0F + 20.0F,
					1.0F, 0.0F, 0.0F);
			GlStateManager.rotate(-((float) this.panoramaTimer + partialTicks) * 0.1F, 0.0F, 1.0F, 0.0F);

			for (int j = 0; j < 6; ++j) {
				GlStateManager.pushMatrix();
				if (j == 1) {
					GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
				}

				if (j == 2) {
					GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
				}

				if (j == 3) {
					GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
				}

				if (j == 4) {
					GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
				}

				if (j == 5) {
					GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
				}

				this.mc.getTextureManager().bindTexture(TITLE_PANORAMA_PATHS[j]);

				bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
				int k = 255 / (i + 1);
				float f3 = 0.0F;
				bufferbuilder.pos(-1.0D, -1.0D, 1.0D).tex(0.0D, 0.0D).color(255, 255, 255, k).endVertex();
				bufferbuilder.pos(1.0D, -1.0D, 1.0D).tex(1.0D, 0.0D).color(255, 255, 255, k).endVertex();
				bufferbuilder.pos(1.0D, 1.0D, 1.0D).tex(1.0D, 1.0D).color(255, 255, 255, k).endVertex();
				bufferbuilder.pos(-1.0D, 1.0D, 1.0D).tex(0.0D, 1.0D).color(255, 255, 255, k).endVertex();
				tessellator.draw();
				GlStateManager.popMatrix();
			}

			GlStateManager.popMatrix();
			GlStateManager.colorMask(true, true, true, false);
		}

		bufferbuilder.setTranslation(0.0D, 0.0D, 0.0D);
		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.matrixMode(5889);
		GlStateManager.popMatrix();
		GlStateManager.matrixMode(5888);
		GlStateManager.popMatrix();
		GlStateManager.depthMask(true);
		GlStateManager.enableCull();
		GlStateManager.enableDepth();
	}

	/**
	 * Rotate and blurs the skybox view in the main menu
	 */
	private void rotateAndBlurSkybox() {
		EaglercraftGPU.glTexParameteri(3553, 10241, 9729);
		EaglercraftGPU.glTexParameteri(3553, 10240, 9729);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
				RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);
		GlStateManager.colorMask(true, true, true, false);
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer bufferbuilder = tessellator.getBuffer();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		GlStateManager.disableAlpha();
		int i = 3;

		for (int j = 0; j < 3; ++j) {
			float f = 1.0F / (float) (j + 1);
			int k = this.width;
			int l = this.height;
			float f1 = (float) (j - 1) / 256.0F;
			bufferbuilder.pos((double) k, (double) l, (double) this.zLevel).tex((double) (0.0F + f1), 1.0D)
					.color(1.0F, 1.0F, 1.0F, f).endVertex();
			bufferbuilder.pos((double) k, 0.0D, (double) this.zLevel).tex((double) (1.0F + f1), 1.0D)
					.color(1.0F, 1.0F, 1.0F, f).endVertex();
			bufferbuilder.pos(0.0D, 0.0D, (double) this.zLevel).tex((double) (1.0F + f1), 0.0D)
					.color(1.0F, 1.0F, 1.0F, f).endVertex();
			bufferbuilder.pos(0.0D, (double) l, (double) this.zLevel).tex((double) (0.0F + f1), 0.0D)
					.color(1.0F, 1.0F, 1.0F, f).endVertex();
		}

		tessellator.draw();
		GlStateManager.enableAlpha();
		GlStateManager.colorMask(true, true, true, true);
	}

	/**
	 * Renders the skybox in the main menu
	 */
	private void renderSkybox(int mouseX, int mouseY, float partialTicks) {
		viewportTexture.bindFramebuffer();
		GlStateManager.viewport(0, 0, 256, 256);
		GlStateManager.clearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GlStateManager.clear(RealOpenGLEnums.GL_COLOR_BUFFER_BIT);
		this.drawPanorama(mouseX, mouseY, partialTicks);
		viewportTexture2.bindFramebuffer();
		GlStateManager.clearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GlStateManager.clear(RealOpenGLEnums.GL_COLOR_BUFFER_BIT);
		this.mc.getTextureManager().bindTexture(backgroundTexture);
		this.rotateAndBlurSkybox();
		viewportTexture.bindFramebuffer();
		this.mc.getTextureManager().bindTexture(backgroundTexture2);
		this.rotateAndBlurSkybox();
		viewportTexture2.bindFramebuffer();
		this.mc.getTextureManager().bindTexture(backgroundTexture);
		this.rotateAndBlurSkybox();
		viewportTexture.bindFramebuffer();
		this.mc.getTextureManager().bindTexture(backgroundTexture2);
		this.rotateAndBlurSkybox();
		viewportTexture2.bindFramebuffer();
		this.mc.getTextureManager().bindTexture(backgroundTexture);
		this.rotateAndBlurSkybox();
		viewportTexture.bindFramebuffer();
		this.mc.getTextureManager().bindTexture(backgroundTexture2);
		this.rotateAndBlurSkybox();

		PlatformOpenGL._wglBindFramebuffer(0x8D40, null);
		this.mc.getTextureManager().bindTexture(backgroundTexture);
		
		GlStateManager.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
		float f = 120.0F / (float) (this.width > this.height ? this.width : this.height);
		float f1 = (float) this.height * f / 256.0F;
		float f2 = (float) this.width * f / 256.0F;
		int i = this.width;
		int j = this.height;
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer bufferbuilder = tessellator.getBuffer();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		bufferbuilder.pos(0.0D, (double) j, (double) this.zLevel).tex((double) (0.5F - f1), (double) (0.5F + f2))
				.color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
		bufferbuilder.pos((double) i, (double) j, (double) this.zLevel).tex((double) (0.5F - f1), (double) (0.5F - f2))
				.color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
		bufferbuilder.pos((double) i, 0.0D, (double) this.zLevel).tex((double) (0.5F + f1), (double) (0.5F - f2))
				.color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
		bufferbuilder.pos(0.0D, 0.0D, (double) this.zLevel).tex((double) (0.5F + f1), (double) (0.5F + f2))
				.color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
		tessellator.draw();
	}

	/**
	 * Draws the screen and all the components in it.
	 */
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		long now = Minecraft.getSystemTime();
		this.drawMenuBackground(mouseX, mouseY, partialTicks);

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		if (MeteorModuleManager.menuEmbers()) {
			MeteorModuleManager.drawSnow(this.width, this.height, now);
		}
		this.drawCenterTitle(now);

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) (this.width / 2 + 100), (float) (this.titleTop() + 34), 0.0F);
		GlStateManager.rotate(-20.0F, 0.0F, 0.0F, 1.0F);
		float f = 1.8F - MathHelper.abs(
				MathHelper.sin((float) (Minecraft.getSystemTime() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
		f = f * 100.0F / (float) (this.fontRendererObj.getStringWidth(this.splashText) + 32);
		GlStateManager.scale(f, f, f);
		this.drawCenteredString(this.fontRendererObj, this.splashText, 0, -8, 0xFF1E88E5);
		GlStateManager.popMatrix();
		String s = "Minecraft 1.12.2";

		if (this.mc.isDemo()) {
			s = s + " Demo";
		}

		this.drawString(this.fontRendererObj, s, 2, this.height - 20, 0xFF1E2A38);
		this.drawString(this.fontRendererObj, "Eaglercraft 1.12.2 (" + EaglercraftVersion.projectOriginVersion + ")", 2,
				this.height - 10, 0xFF1E2A38);
		this.drawString(this.fontRendererObj, "Resources copyright Mojang AB", this.field_193979_N, this.height - 10,
				0xFF1E2A38);

		super.drawScreen(mouseX, mouseY, partialTicks);
		this.drawIconTooltip(mouseX, mouseY);
	}

	// ------------------------- Glass (Feather-style) layout -------------------------

	private static final int MM_TEXT = 0xFF1E2A38;

	private int menuWidth() {
		return Math.min(200, Math.max(120, this.width - 40));
	}

	private int menuLeft() {
		return (this.width - this.menuWidth()) / 2;
	}

	/** Short screens (GUI scale 4 on a small display) get a tighter layout. */
	private boolean compact() {
		return this.height < 244;
	}

	private int rowH() {
		return this.compact() ? 18 : 20;
	}

	private int rowStep() {
		return this.compact() ? 21 : 24;
	}

	private float titleScale() {
		return this.compact() ? 3.0F : 4.0F;
	}

	/** Top of the logo. The logo and the buttons together are centered on screen. */
	private int titleTop() {
		return this.compact() ? 6 : Math.max(8, (this.height - 234) / 2);
	}

	private int menuTop() {
		return this.titleTop() + (this.compact() ? 50 : 84);
	}

	/**
	 * Draws only the menu background (panorama or space). The ClickGUI and the
	 * credits screen call this too, so they can be see-through over the menu.
	 */
	void drawMenuBackground(int mouseX, int mouseY, float partialTicks) {
		this.panoramaTimer += partialTicks;
		if (USE_PANORAMA) {
			GlStateManager.disableAlpha();
			this.renderSkybox(mouseX, mouseY, partialTicks);
			GlStateManager.enableAlpha();
			this.drawGradientRect(0, 0, this.width, this.height, -2130706433, 16777215);
			this.drawGradientRect(0, 0, this.width, this.height, 0x00FFFFFF, 0x60FFFFFF);
		} else {
			this.drawSpaceBackground(Minecraft.getSystemTime());
		}
	}

	/** Big glowing centered title with a CLIENT subtitle. */
	private void drawCenterTitle(long now) {
		String title = "MeteorMC";
		int len = title.length();
		float sweep = ((now % 2600L) / 2600.0F) * (len + 6) - 3.0F;
		int top = this.titleTop();
		int total = this.fontRendererObj.getStringWidth(title);

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) (this.width / 2), (float) top, 0.0F);
		float ts = this.titleScale();
		GlStateManager.scale(ts, ts, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
				RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);

		int x = -total / 2;
		for (int i = 0; i < len; ++i) {
			String s = String.valueOf(title.charAt(i));
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

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) (this.width / 2), (float) (top + (int) (9.0F * ts) + 4), 0.0F);
		GlStateManager.scale(1.5F, 1.5F, 1.0F);
		this.fontRendererObj.drawString("CLIENT", (float) (-this.fontRendererObj.getStringWidth("CLIENT") / 2), 0.0F,
				0xFFDDEFFF, true);
		GlStateManager.popMatrix();
	}

	/** Shows the name of the small icon button the mouse is over. */
	private void drawIconTooltip(int mouseX, int mouseY) {
		for (GuiButton b : this.buttonList) {
			if (b instanceof GuiMeteorButton && ((GuiMeteorButton) b).isIconStyle() && b.isMouseOver()) {
				int tw = this.fontRendererObj.getStringWidth(b.displayString);
				int tx = Math.min(mouseX + 8, this.width - tw - 8);
				int ty = mouseY + 10;
				Gui.drawRect(tx - 4, ty - 3, tx + tw + 4, ty + 11, 0xFF4FA3E0);
				Gui.drawRect(tx - 3, ty - 2, tx + tw + 3, ty + 10, 0xF2FFFFFF);
				this.drawString(this.fontRendererObj, b.displayString, tx, ty, MM_TEXT);
			}
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

	/** Dark gradient, twinkling stars, and shooting meteors with flame trails. */
	private void drawSpaceBackground(long now) {
		this.drawGradientRect(0, 0, this.width, this.height, BG_TOP, BG_BOTTOM);

		if (this.starX == null) {
			this.generateStars();
		}

		for (int i = 0; MeteorModuleManager.menuStars() && i < STAR_COUNT; ++i) {
			int a = (int) (90.0D + 110.0D * (0.5D + 0.5D * Math.sin(now * 0.003D + i * 1.7D)));
			int sz = this.starSize[i];
			Gui.drawRect(this.starX[i], this.starY[i], this.starX[i] + sz, this.starY[i] + sz,
					(a << 24) | 0xFFFFFF);
		}

		for (int m = 0; MeteorModuleManager.menuMeteors() && m < METEOR_COUNT; ++m) {
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

		this.drawGradientRect(0, 0, this.width, this.height, 0x00FFFFFF, 0x30FFFFFF);
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

	/** Wavy, shimmering, glowing title with an underline streak. */
	private void drawAnimatedTitle(long now) {
		int cx = this.width / 2;
		int total = this.fontRendererObj.getStringWidth(TITLE);
		int len = TITLE.length();
		float sweep = ((now % 2600L) / 2600.0F) * (len + 6) - 3.0F;

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) cx, 30.0F, 0.0F);
		GlStateManager.scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(RealOpenGLEnums.GL_SRC_ALPHA, RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
				RealOpenGLEnums.GL_ONE, RealOpenGLEnums.GL_ZERO);

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

	/**
	 * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
	 */
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);

		if (mouseX > this.field_193979_N && mouseX < this.field_193979_N + this.field_193978_M
				&& mouseY > this.height - 10 && mouseY < this.height) {
			this.mc.displayGuiScreen(new GuiWinGame(false, null));
		}

	}

	/**
	 * Called when the screen is unloaded. Used to disable keyboard repeat events
	 */
	public void onGuiClosed() {
	}
}
