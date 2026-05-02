package com.worldremembers.livinglegends.neoforge.client;

import com.worldremembers.livinglegends.config.SimpleJson;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WorldRemembersLivingLegendsNeoForgeClientConfig {
    private static final String FILE_NAME = "client_title_overlay.json5";
    private static final double DEFAULT_TITLE_SCALE = 2.35;
    private static final double DEFAULT_SUBTITLE_SCALE = 1.25;
    private static final int DEFAULT_MAX_WIDTH = 640;
    private static final double LEGACY_TITLE_SCALE = 2.0;
    private static final double LEGACY_SUBTITLE_SCALE = 1.1;
    private static final int LEGACY_MAX_WIDTH = 520;
    private static WorldRemembersLivingLegendsNeoForgeClientConfig current = defaults();
    private static Path currentPath;

    public boolean enabled = true;
    public double scale = 1.0;
    public int yOffset = 0;
    public boolean showSubtitle = true;
    public boolean useStyleColors = true;
    public boolean showDecorativeLines = true;
    public boolean showBackground = false;
    public double titleScale = DEFAULT_TITLE_SCALE;
    public double subtitleScale = DEFAULT_SUBTITLE_SCALE;
    public int decorativeLineLength = 120;
    public int lineWidth = 1;
    public int fadeInTicks = 20;
    public int stayTicks = 60;
    public int fadeOutTicks = 40;
    public double opacity = 1.0;
    public boolean textShadow = true;
    public boolean suppressWhenDebugHudOpen = true;
    public int maxWidth = DEFAULT_MAX_WIDTH;

    private WorldRemembersLivingLegendsNeoForgeClientConfig() {
    }

    public static WorldRemembersLivingLegendsNeoForgeClientConfig get() {
        return current;
    }

    public static WorldRemembersLivingLegendsNeoForgeClientConfig load(Logger logger) {
        Path path = configPath();
        currentPath = path;
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                current = defaults();
                save(logger);
                return current;
            }
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                current = fromMap(SimpleJson.parseObjectWithComments(reader)).normalize();
            }
            save(logger);
            return current;
        } catch (IOException | RuntimeException exception) {
            if (logger != null) {
                logger.warn("Could not load World Remembers client title overlay config: " + exception.getMessage());
            }
            current = defaults();
            return current;
        }
    }

    public static void save(Logger logger) {
        Path path = currentPath == null ? configPath() : currentPath;
        currentPath = path;
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write("// World Remembers client title overlay settings.\n");
                writer.write("// This only affects your local HUD rendering, not server-side place generation.\n");
                writer.write(SimpleJson.stringify(current.normalize().toMap()));
                writer.newLine();
            }
        } catch (IOException exception) {
            if (logger != null) {
                logger.warn("Could not save World Remembers client title overlay config: " + exception.getMessage());
            }
        }
    }

    public static Path currentPath() {
        return currentPath == null ? configPath() : currentPath;
    }

    private static WorldRemembersLivingLegendsNeoForgeClientConfig defaults() {
        return new WorldRemembersLivingLegendsNeoForgeClientConfig().normalize();
    }

    private static WorldRemembersLivingLegendsNeoForgeClientConfig fromMap(Map<String, Object> values) {
        WorldRemembersLivingLegendsNeoForgeClientConfig config = defaults();
        Map<String, Object> root = values == null ? Map.of() : values;
        config.enabled = bool(root, "enabled", config.enabled);
        config.scale = decimal(root, "scale", config.scale);
        config.yOffset = integer(root, "yOffset", config.yOffset);
        config.showSubtitle = bool(root, "showSubtitle", config.showSubtitle);
        config.useStyleColors = bool(root, "useStyleColors", config.useStyleColors);
        config.showDecorativeLines = bool(root, "showDecorativeLines", config.showDecorativeLines);
        config.showBackground = bool(root, "showBackground", config.showBackground);
        config.titleScale = decimal(root, "titleScale", config.titleScale);
        config.subtitleScale = decimal(root, "subtitleScale", config.subtitleScale);
        config.decorativeLineLength = integer(root, "decorativeLineLength", config.decorativeLineLength);
        config.lineWidth = integer(root, "lineWidth", config.lineWidth);
        config.fadeInTicks = integer(root, "fadeInTicks", config.fadeInTicks);
        config.stayTicks = integer(root, "stayTicks", config.stayTicks);
        config.fadeOutTicks = integer(root, "fadeOutTicks", config.fadeOutTicks);
        config.opacity = decimal(root, "opacity", config.opacity);
        config.textShadow = bool(root, "textShadow", config.textShadow);
        config.suppressWhenDebugHudOpen = bool(root, "suppressWhenDebugHudOpen", config.suppressWhenDebugHudOpen);
        config.maxWidth = integer(root, "maxWidth", config.maxWidth);
        if (closeTo(config.titleScale, LEGACY_TITLE_SCALE)) {
            config.titleScale = DEFAULT_TITLE_SCALE;
        }
        if (closeTo(config.subtitleScale, LEGACY_SUBTITLE_SCALE)) {
            config.subtitleScale = DEFAULT_SUBTITLE_SCALE;
        }
        if (config.maxWidth == LEGACY_MAX_WIDTH) {
            config.maxWidth = DEFAULT_MAX_WIDTH;
        }
        return config;
    }

    Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", enabled);
        values.put("scale", scale);
        values.put("yOffset", yOffset);
        values.put("showSubtitle", showSubtitle);
        values.put("useStyleColors", useStyleColors);
        values.put("showDecorativeLines", showDecorativeLines);
        values.put("showBackground", showBackground);
        values.put("titleScale", titleScale);
        values.put("subtitleScale", subtitleScale);
        values.put("decorativeLineLength", decorativeLineLength);
        values.put("lineWidth", lineWidth);
        values.put("fadeInTicks", fadeInTicks);
        values.put("stayTicks", stayTicks);
        values.put("fadeOutTicks", fadeOutTicks);
        values.put("opacity", opacity);
        values.put("textShadow", textShadow);
        values.put("suppressWhenDebugHudOpen", suppressWhenDebugHudOpen);
        values.put("maxWidth", maxWidth);
        return values;
    }

    WorldRemembersLivingLegendsNeoForgeClientConfig normalize() {
        scale = clamp(scale, 0.5, 2.0);
        titleScale = clamp(titleScale, 0.8, 3.5);
        subtitleScale = clamp(subtitleScale, 0.6, 2.2);
        decorativeLineLength = Math.max(24, Math.min(240, decorativeLineLength));
        lineWidth = Math.max(1, Math.min(3, lineWidth));
        yOffset = Math.max(-120, Math.min(160, yOffset));
        fadeInTicks = Math.max(0, fadeInTicks);
        stayTicks = Math.max(1, stayTicks);
        fadeOutTicks = Math.max(0, fadeOutTicks);
        opacity = clamp(opacity, 0.0, 1.0);
        maxWidth = Math.max(120, Math.min(800, maxWidth));
        return this;
    }

    int totalDurationTicks() {
        return Math.max(1, fadeInTicks + stayTicks + fadeOutTicks);
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(Path.of("world_remembers", "living_legends"))
                .resolve(FILE_NAME);
    }

    private static boolean bool(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private static int integer(Map<String, Object> values, String key, int fallback) {
        Object value = values.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static double decimal(Map<String, Object> values, String key, double fallback) {
        Object value = values.get(key);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static boolean closeTo(double value, double expected) {
        return Math.abs(value - expected) < 0.000_001;
    }
}
