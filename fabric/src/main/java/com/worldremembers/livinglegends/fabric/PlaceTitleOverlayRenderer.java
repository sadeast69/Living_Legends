package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.PlaceType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

final class PlaceTitleOverlayRenderer {
    private static final int MIN_TEXT_ALPHA_BYTE = 4;
    private static final float MIN_RENDER_ALPHA = 0.02F;
    private static PlaceTitleS2CPayload currentPayload;
    private static Text currentTitle = Text.empty();
    private static Text currentSubtitle = Text.empty();
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
        currentTitle = resolveTitle(payload);
        currentSubtitle = Text.translatable("living_legends.place_type." + payload.placeType());
        WorldRemembersLivingLegendsFabricClientConfig config = WorldRemembersLivingLegendsFabricClientConfig.get();
        ageTicks = 0;
        totalDurationTicks = totalDuration(config);
        invalidateLayoutCache();
        recalculateLayout(MinecraftClient.getInstance(), config);
    }

    static void clear() {
        currentPayload = null;
        currentTitle = Text.empty();
        currentSubtitle = Text.empty();
        ageTicks = 0;
        totalDurationTicks = 0;
        invalidateLayoutCache();
    }

    static void tick(MinecraftClient client) {
        if (currentPayload == null) {
            return;
        }
        ageTicks++;
        if (totalDurationTicks <= 0 || ageTicks >= totalDurationTicks) {
            clear();
        }
    }

    static void render(DrawContext drawContext, float tickDelta) {
        if (currentPayload == null) {
            return;
        }
        WorldRemembersLivingLegendsFabricClientConfig config = WorldRemembersLivingLegendsFabricClientConfig.get();
        if (!config.enabled) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
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

        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();
        int centerX = screenWidth / 2;
        int baseY = Math.max(18, screenHeight / 3 - 56 + config.yOffset);
        double scale = effectiveScale(config);
        int titleColor = withAlpha(config.useStyleColors ? styleTitleColor(currentPayload.nameStyle()) : 0xFFEFEFEF, alpha);
        int lineColor = withAlpha(config.useStyleColors ? styleLineColor(currentPayload.nameStyle()) : 0xFFB8BEC8, alpha * 0.85F);
        int subtitleColor = withAlpha(0xFFE6E2D5, alpha * 0.86F);
        int backgroundColor = withAlpha(0xAA050505, alpha * 0.22F);

        var matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(centerX, baseY, 0.0F);
        matrices.scale((float) scale, (float) scale, 1.0F);

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
            drawContext.fill(-halfBackground, -7, halfBackground, contentHeight + 9, backgroundColor);
        }

        if (config.showDecorativeLines) {
            int lineY = config.showSubtitle ? subtitleY + Math.max(3, subtitleHeight / 2) : titleHeight + 9;
            int lineGap = Math.max(36, (config.showSubtitle ? subtitleVisualWidth : titleVisualWidth) / 2 + 16);
            int lineLength = Math.max(24, config.decorativeLineLength);
            int lineWidth = Math.max(1, config.lineWidth);
            drawContext.fill(-lineGap - lineLength, lineY, -lineGap, lineY + lineWidth, lineColor);
            drawContext.fill(lineGap, lineY, lineGap + lineLength, lineY + lineWidth, lineColor);
        }

        drawScaledText(drawContext, client.textRenderer, currentTitle, cachedTitleWidth, 0, titleScale, titleColor, config.textShadow);
        if (config.showSubtitle) {
            drawScaledText(
                    drawContext,
                    client.textRenderer,
                    currentSubtitle,
                    cachedSubtitleWidth,
                    subtitleY,
                    subtitleScale,
                    subtitleColor,
                    config.textShadow
            );
        }
        matrices.pop();
    }

    private static Text resolveTitle(PlaceTitleS2CPayload payload) {
        if (payload.manualName()) {
            return Text.literal(payload.manualNameText());
        }
        Text resolved = WorldRemembersLivingLegendsFabricNameResolver.resolve(payload.nameRecipe());
        if (resolved.getString().isBlank()) {
            return Text.literal(payload.serverResolvedFallbackName());
        }
        return resolved;
    }

    private static void recalculateLayout(MinecraftClient client, WorldRemembersLivingLegendsFabricClientConfig config) {
        if (currentPayload == null || client == null || client.textRenderer == null) {
            return;
        }
        int windowWidth = client.getWindow() == null ? -1 : client.getWindow().getScaledWidth();
        if (cachedTitleWidth > 0
                && windowWidth == lastWindowWidth
                && config.maxWidth == lastMaxWidth
                && Math.abs(config.scale - lastScale) < 0.0001) {
            return;
        }
        cachedTitleWidth = client.textRenderer.getWidth(currentTitle);
        cachedSubtitleWidth = client.textRenderer.getWidth(currentSubtitle);
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

    private static double effectiveScale(WorldRemembersLivingLegendsFabricClientConfig config) {
        int widest = Math.max(
                (int) Math.round(cachedTitleWidth * config.titleScale),
                config.showSubtitle ? (int) Math.round(cachedSubtitleWidth * config.subtitleScale) : 0
        );
        if (widest <= 0 || widest <= config.maxWidth) {
            return config.scale;
        }
        return config.scale * Math.max(0.55, config.maxWidth / (double) widest);
    }

    private static float alpha(WorldRemembersLivingLegendsFabricClientConfig config, float progress, int total) {
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

    private static int totalDuration(WorldRemembersLivingLegendsFabricClientConfig config) {
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
            DrawContext drawContext,
            TextRenderer textRenderer,
            Text text,
            int rawWidth,
            int y,
            double textScale,
            int color,
            boolean shadow
    ) {
        var matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(0.0F, y, 0.0F);
        matrices.scale((float) textScale, (float) textScale, 1.0F);
        drawContext.drawText(textRenderer, text, -rawWidth / 2, 0, color, shadow);
        matrices.pop();
    }

    private static boolean debugHudOpen(MinecraftClient client) {
        try {
            return client.getDebugHud().shouldShowDebugHud();
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
