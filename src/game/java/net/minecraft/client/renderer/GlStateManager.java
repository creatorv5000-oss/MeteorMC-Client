package net.minecraft.client.renderer;

/**
 * Minimal compile-time stub for CI. Does not perform real GL operations.
 * Replace or remove when real Minecraft/LWJGL dependencies are available.
 */
public final class GlStateManager {
    private GlStateManager() {}

    public static void pushMatrix() {}
    public static void popMatrix() {}
    public static void translate(float x, float y, float z) {}
    public static void scale(float x, float y, float z) {}
    public static void enableBlend() {}
    public static void tryBlendFuncSeparate(int src, int dst, int srcAlpha, int dstAlpha) {}
}
