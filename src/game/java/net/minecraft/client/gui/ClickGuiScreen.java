	/** Menu effect switches, read by the main menu and the ClickGUI. */
	public static boolean menuStars() {
		return MENU_FX.isEnabled() && MENU_FX.stars.getBoolean();
	}

	public static boolean menuMeteors() {
		return MENU_FX.isEnabled() && MENU_FX.meteors.getBoolean();
	}

	public static boolean menuEmbers() {
		return MENU_FX.isEnabled() && MENU_FX.embers.getBoolean();
	}

	/** Renderer hook defaults. These are the normal vanilla values when no effect is enabled. */
	public static float hitRed() {
		return 0.4F;
	}

	public static float hitGreen() {
		return 0.0F;
	}

	public static float hitBlue() {
		return 0.0F;
	}

	public static float hurtCamScale() {
		return 1.0F;
	}

	public static boolean staticFov() {
		return false;
	}

	public static float fireOffset() {
		return 0.0F;
	}

	public static boolean fixedHand() {
		return false;
	}

	public static void applyHandOffset(boolean mainHand) {
	}

	public static void applyItemScale(boolean mainHand) {
	}
