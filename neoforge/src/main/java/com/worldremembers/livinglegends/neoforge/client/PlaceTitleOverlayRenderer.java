package com.worldremembers.livinglegends.neoforge.client;

import com.worldremembers.livinglegends.neoforge.network.PlaceTitleS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class PlaceTitleOverlayRenderer {
    private static final int MIN_TEXT_ALPHA_BYTE = 4;
    private static final float MIN_RENDER_ALPHA = 0.02F;
    private static PlaceTitleS2CPayload currentPayload;
    private static Component currentTitle = Component.empty();
    private static Component currentSubtitle = Component.empty();
    private static int ageTicks;
    private static int totalDurationTicks;
    private static int cachedTitleWidth;
    private static int cachedSubtitleWidth;
    private static int lastWindowWidth = -1;
    private static int lastMaxWidth = -1;
    private static double lastScale = -1.0;

    private PlaceTitleOverlayRenderer() {
    }

    static void show(PlaceTitleS2CPayload payload) {
        if (payload == null || payload.reason() == PlaceTitleS2CPayload.Reason.CLEAR) {
            clear();
            return;
        }
        if (currentPayload != null && (totalDurationTicks <= 0 || ageTicks >= totalDurationTicks)) {
            clear();
        }
        if (currentPayload != null
                && payload.reason() != PlaceTitleS2CPayload.Reason.DEBUG
                && !payload.placeId().isBlank()
                && payload.placeId().equals(currentPayload.placeId())) {
            return;
        }
        currentPayload = payload;
        currentTitle = WorldRemembersLivingLegendsNeoForgeClientState.titleFor(payload);
        currentSubtitle = WorldRemembersLivingLegendsNeoForgeClientState.subtitleFor(payload);
        WorldRemembersLivingLegendsNeoForgeClientConfig config = WorldRemembersLivingLegendsNeoForgeClientConfig.get();
        ageTicks = 0;
        totalDurationTicks = totalDuration(config);
        invalidateLayoutCache();
        recalculateLayout(Minecraft.getInstance(), config);
    }

    static void clear() {
        currentPayload = null;
        currentTitle = Component.empty();
        currentSubtitle = Component.empty();
        ageTicks = 0;
        totalDurationTicks = 0;
        invalidateLayoutCache();
    }

    static void tick(Minecraft client) {
        if (currentPayload == null) {
            return;
        }
        ageTicks++;
        if (totalDurationTicks <= 0 || ageTicks >= totalDurationTicks) {
            clear();
        }
    }

    static void render(GuiGraphics guiGraphics, float tickDelta) {
        if (currentPayload == null) {
            return;
        }
        WorldRemembersLivingLegendsNeoForgeClientConfig config = WorldRemembersLivingLegendsNeoForgeClientConfig.get();
        if (!config.enabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return;
        }
        if (config.suppressWhenDebugHudOpen && debugHudOpen(client)) {
            return;
        }

        int total = totalDurationTicks > 0 ? totalDurationTicks : totalDuration(config);
        float elapsed = ageTicks + tickDelta(tickDelta);
        if (total <= 0 || expired(elapsed, total)) {
            clear();
            return;
        }

        recalculateLayout(client, config);

        float alpha = clamp01(alpha(config, elapsed, total) * (float) config.opacity);
        if (alpha <= MIN_RENDER_ALPHA || unsafeTextAlpha(alpha, config.showSubtitle)) {
            return;
        }

        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        int centerX = screenWidth / 2;
        int baseY = Math.max(18, screenHeight / 3 - 56 + config.yOffset);
        double scale = effectiveScale(config);
        int titleColor = withAlpha(config.useStyleColors ? styleTitleColor(currentPayload.nameStyle()) : 0xFFEFEFEF, alpha);
        int lineColor = withAlpha(config.useStyleColors ? styleLineColor(currentPayload.nameStyle()) : 0xFFB8BEC8, alpha * 0.85F);
        int subtitleColor = withAlpha(0xFFE6E2D5, alpha * 0.86F);
        int backgroundColor = withAlpha(0xAA050505, alpha * 0.22F);

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(centerX, baseY, 0.0F);
        pose.scale((float) scale, (float) scale, 1.0F);

        double titleScale = config.titleScale;
        double subtitleScale = config.subtitleScale;
        int titleVisualWidth = (int) Math.round(cachedTitleWidth * titleScale);
        int subtitleVisualWidth = config.showSubtitle ? (int) Math.round(cachedSubtitleWidth * subtitleScale) : 0;
        int titleHeight = (int) Math.round(9 * titleScale);
        int subtitleY = titleHeight + 13;
        int subtitleHeight = config.showSubtitle ? (int) Math.round(9 * subtitleScale) : 0;
        int contentWidth = Math.max(titleVisualWidth, subtitleVisualWidth);
        int contentHeight = config.showSubtitle ? subtitleY + subtitleHeight : titleHeight;

        if (config.showBackground) {
            int halfBackground = Math.max(70, contentWidth / 2 + 22);
            guiGraphics.fill(-halfBackground, -7, halfBackground, contentHeight + 9, backgroundColor);
        }

        if (config.showDecorativeLines) {
            int lineY = config.showSubtitle ? subtitleY + Math.max(3, subtitleHeight / 2) : titleHeight + 9;
            int lineGap = Math.max(36, (config.showSubtitle ? subtitleVisualWidth : titleVisualWidth) / 2 + 16);
            int lineLength = Math.max(24, config.decorativeLineLength);
            int lineWidth = Math.max(1, config.lineWidth);
            guiGraphics.fill(-lineGap - lineLength, lineY, -lineGap, lineY + lineWidth, lineColor);
            guiGraphics.fill(lineGap, lineY, lineGap + lineLength, lineY + lineWidth, lineColor);
        }

        drawScaledText(guiGraphics, client.font, currentTitle, cachedTitleWidth, 0, titleScale, titleColor, config.textShadow);
        if (config.showSubtitle) {
            drawScaledText(
                    guiGraphics,
                    client.font,
                    currentSubtitle,
                    cachedSubtitleWidth,
                    subtitleY,
                    subtitleScale,
                    subtitleColor,
                    config.textShadow
            );
        }
        pose.popPose();
    }

    private static void recalculateLayout(Minecraft client, WorldRemembersLivingLegendsNeoForgeClientConfig config) {
        if (currentPayload == null || client == null || client.font == null) {
            return;
        }
        int windowWidth = client.getWindow() == null ? -1 : client.getWindow().getGuiScaledWidth();
        if (cachedTitleWidth > 0
                && windowWidth == lastWindowWidth
                && config.maxWidth == lastMaxWidth
                && Math.abs(config.scale - lastScale) < 0.0001) {
            return;
        }
        cachedTitleWidth = client.font.width(currentTitle.getVisualOrderText());
        cachedSubtitleWidth = client.font.width(currentSubtitle.getVisualOrderText());
        lastWindowWidth = windowWidth;
        lastMaxWidth = config.maxWidth;
        lastScale = config.scale;
    }

    private static void invalidateLayoutCache() {
        cachedTitleWidth = 0;
        cachedSubtitleWidth = 0;
        lastWindowWidth = -1;
        lastMaxWidth = -1;
        lastScale = -1.0;
    }

    private static double effectiveScale(WorldRemembersLivingLegendsNeoForgeClientConfig config) {
        int widest = Math.max(
                (int) Math.round(cachedTitleWidth * config.titleScale),
                config.showSubtitle ? (int) Math.round(cachedSubtitleWidth * config.subtitleScale) : 0
        );
        if (widest <= 0 || widest <= config.maxWidth) {
            return config.scale;
        }
        return config.scale * Math.max(0.55, config.maxWidth / (double) widest);
    }

    private static float alpha(WorldRemembersLivingLegendsNeoForgeClientConfig config, float progress, int total) {
        int fadeIn = Math.max(0, config.fadeInTicks);
        int stay = Math.max(0, config.stayTicks);
        int fadeOut = Math.max(0, config.fadeOutTicks);
        if (total <= 0 || progress < 0.0F || progress >= total) {
            return 0.0F;
        }
        if (fadeIn > 0 && progress < fadeIn) {
            return clamp01(progress / fadeIn);
        }
        float stayEnd = fadeIn + stay;
        if (progress < stayEnd || fadeOut <= 0) {
            return 1.0F;
        }
        return clamp01(1.0F - ((progress - stayEnd) / fadeOut));
    }

    private static boolean expired(float elapsed, int total) {
        return elapsed >= total;
    }

    private static int totalDuration(WorldRemembersLivingLegendsNeoForgeClientConfig config) {
        int fadeIn = Math.max(0, config.fadeInTicks);
        int stay = Math.max(0, config.stayTicks);
        int fadeOut = Math.max(0, config.fadeOutTicks);
        return fadeIn + stay + fadeOut;
    }

    private static float tickDelta(float tickDelta) {
        if (Float.isNaN(tickDelta) || Float.isInfinite(tickDelta)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, tickDelta));
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static boolean unsafeTextAlpha(float alpha, boolean subtitleVisible) {
        return alphaByte(alpha) < MIN_TEXT_ALPHA_BYTE
                || (subtitleVisible && alphaByte(alpha * 0.86F) < MIN_TEXT_ALPHA_BYTE);
    }

    private static int alphaByte(float alpha) {
        return Math.round(clamp01(alpha) * 255.0F);
    }

    private static void drawScaledText(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            int rawWidth,
            int y,
            double textScale,
            int color,
            boolean shadow
    ) {
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0.0F, y, 0.0F);
        pose.scale((float) textScale, (float) textScale, 1.0F);
        guiGraphics.drawString(font, text, -rawWidth / 2, 0, color, shadow);
        pose.popPose();
    }

    private static boolean debugHudOpen(Minecraft client) {
        try {
            return client.getDebugOverlay().showDebugScreen();
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static int styleTitleColor(String styleId) {
        if (styleId == null) {
            return 0xFFFFD88A;
        }
        return switch (styleId) {
            case "neutral_server" -> 0xFFE6E6E6;
            case "dark_fantasy" -> 0xFFD19A8E;
            case "cozy_survival" -> 0xFFFFD99B;
            case "epic_mythology" -> 0xFFFFD76A;
            case "funny_community" -> 0xFFFFE37A;
            default -> 0xFFFFD88A;
        };
    }

    private static int styleLineColor(String styleId) {
        if (styleId == null) {
            return 0xFFE0B45D;
        }
        return switch (styleId) {
            case "neutral_server" -> 0xFFB8BEC8;
            case "dark_fantasy" -> 0xFF8D5252;
            case "cozy_survival" -> 0xFFA9C88A;
            case "epic_mythology" -> 0xFFC8B0F0;
            case "funny_community" -> 0xFF7ED8D4;
            default -> 0xFFE0B45D;
        };
    }

    private static int withAlpha(int rgbOrArgb, float alpha) {
        int rgb = rgbOrArgb & 0x00FF_FFFF;
        int a = alphaByte(alpha);
        return (a << 24) | rgb;
    }
}
