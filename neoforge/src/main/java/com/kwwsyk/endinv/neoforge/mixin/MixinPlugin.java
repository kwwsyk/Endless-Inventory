package com.kwwsyk.endinv.neoforge.mixin;

import com.kwwsyk.endinv.neoforge.options.StartupConfig;
import com.mojang.logging.LogUtils;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("ExperienceOrbMixin") || mixinClassName.endsWith("ItemEntityMixin")) {
            return true;
        }
        try {
            return StartupConfig.enableVanillaRecipeBookTransfer();
        } catch (IllegalStateException e) {
            Boolean v = readBooleanFromToml("mixin", "vanillaRecipeBookTransfer");
            if (v != null) {
                LOGGER.info("NeoForge mixin fallback read: mixin.vanillaRecipeBookTransfer={} ", v);
                return v;
            }
            // default to true to match config default
            LOGGER.warn("NeoForge mixin config not yet loaded; defaulting vanillaRecipeBookTransfer=true");
            return true;
        }
    }

    private static Boolean readBooleanFromToml(String section, String key) {
        String fileName = "endless_inventory-common.toml";
        Path[] candidates = new Path[]{
                Paths.get("run", "config", fileName),
                Paths.get("config", fileName),
                Paths.get(fileName)
        };
        for (Path p : candidates) {
            try {
                if (Files.isRegularFile(p)) {
                    String content = Files.readString(p);
                    Boolean b = parseEnableFromToml(content, section, key);
                    if (b != null) return b;
                }
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private static Boolean parseEnableFromToml(String content, String section, String key) {
        boolean inSection = false;
        for (String raw : content.split("\r?\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("[")) {
                int r = line.indexOf(']');
                if (r > 1) {
                    String sect = line.substring(1, r).trim();
                    inSection = section.equals(sect);
                } else inSection = false;
                continue;
            }
            if (!inSection) continue;
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash).trim();
            if (line.startsWith(key)) {
                int eq = line.indexOf('=');
                if (eq >= 0) {
                    String rhs = line.substring(eq + 1).trim();
                    if (rhs.equalsIgnoreCase("true")) return Boolean.TRUE;
                    if (rhs.equalsIgnoreCase("false")) return Boolean.FALSE;
                }
            }
        }
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
