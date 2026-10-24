package net.minecraft.client.gui;

/**
 * Public entry points that the renderer classes call for the MeteorMC PvP mods.
 * Every method returns the normal game value (or does nothing) while its mod is off.
 */
public final class MeteorHooks {

	private MeteorHooks() {
	}

	/** Hit flash tint. The normal game value is 0.4 / 0 / 0. */
	public static float hitRed() {
		return MeteorModuleManager.hitRed();
	}

	public static float hitGreen() {
		return MeteorModuleManager.hitGreen();
	}

	public static float hitBlue() {
		return MeteorModuleManager.hitBlue();
	}

	/** 1.0 normally, 0.0 for no camera shake when hurt. */
	public static float hurtCamScale() {
		return MeteorModuleManager.hurtCamScale();
	}

	/** True to ignore the speed / sprint FOV change. */
	public static boolean staticFov() {
		return MeteorModuleManager.staticFov();
	}

	/** Extra distance to lower the first-person fire overlay (0 normally). */
	public static float fireOffset() {
		return MeteorModuleManager.fireOffset();
	}

	/** True to keep the hand up instead of dipping after a swing. */
	public static boolean fixedHand() {
		return MeteorModuleManager.fixedHand();
	}

	public static void applyHandOffset(boolean mainHand) {
		MeteorModuleManager.applyHandOffset(mainHand);
	}

	public static void applyItemScale(boolean mainHand) {
		MeteorModuleManager.applyItemScale(mainHand);
	}
}
