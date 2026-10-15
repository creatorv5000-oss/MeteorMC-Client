package com.isacofff.clientbase.modules.features;

import com.isacofff.clientbase.Category;
import com.isacofff.clientbase.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ViaVersion extends Module {
    private static final String MAPPING_VERSION = "26.2";
    private final Map<String, String> itemMap = new HashMap<>();
    private final Map<String, String> textureMap = new HashMap<>();

    public ViaVersion() {
        super("ViaVersion", Category.Client);
        this.description = "Compatibility layer for mappings/textures version " + MAPPING_VERSION;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        loadMappings();
    }

    private void loadMappings() {
        loadProperties("/mappings/" + MAPPING_VERSION + "/items.properties", itemMap);
        loadProperties("/mappings/" + MAPPING_VERSION + "/textures.properties", textureMap);
    }

    private void loadProperties(String resourcePath, Map<String, String> target) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                // resource not found; leave map empty
                return;
            }
            Properties p = new Properties();
            p.load(is);
            for (String key : p.stringPropertyNames()) {
                target.put(key, p.getProperty(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Return the mapped item identifier for the given key. If no mapping is present, returns the original key.
     * Example: getMappedItem("oldmod:old_item") -> "mod:new_item"
     */
    public String getMappedItem(String key) {
        return itemMap.getOrDefault(key, key);
    }

    /**
     * Return a ResourceLocation for a mapped texture key, or null if no mapping exists.
     * Mapped values may be in the form "namespace:path/to/texture.png" or just "path/to/texture.png".
     */
    public ResourceLocation getMappedTextureResource(String key) {
        String mapped = textureMap.get(key);
        if (mapped == null) return null;
        if (mapped.contains(":")) {
            String[] a = mapped.split(":", 2);
            return new ResourceLocation(a[0], a[1]);
        } else {
            return new ResourceLocation(mapped);
        }
    }

    /**
     * Convenience: bind mapped texture if present. Calling code should decide when to call this.
     */
    public void bindMappedTexture(String key) {
        ResourceLocation rl = getMappedTextureResource(key);
        if (rl != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(rl);
        }
    }

}
