package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PlaceTitleOverlayConfigScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldRemembersLivingLegends.MOD_ID + "-client-config");
    private final Screen parent;

    PlaceTitleOverlayConfigScreen(Screen parent) {
        super(Text.translatable("living_legends.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFEADCA8);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("living_legends.config.title_overlay"), width / 2, 31, 0xFFD8D8D8);
    }

    private void rebuild() {
        clearChildren();
        WorldRemembersLivingLegendsFabricClientConfig config = WorldRemembersLivingLegendsFabricClientConfig.get();
        int left = width / 2 - 155;
        int right = width / 2 + 5;
        int y = 54;
        addToggle(left, y, "Enabled", config.enabled, () -> config.enabled = !config.enabled);
        addToggle(right, y, "Background", config.showBackground, () -> config.showBackground = !config.showBackground);
        y += 24;
        addValue(left, y, "Global Scale", String.format(java.util.Locale.ROOT, "%.1f", config.scale), -0.1, 0.1,
                () -> config.scale = Math.max(0.5, config.scale - 0.1),
                () -> config.scale = Math.min(2.0, config.scale + 0.1));
        addValue(right, y, "Title Scale", String.format(java.util.Locale.ROOT, "%.1f", config.titleScale), -0.1, 0.1,
                () -> config.titleScale = Math.max(0.8, config.titleScale - 0.1),
                () -> config.titleScale = Math.min(3.5, config.titleScale + 0.1));
        y += 24;
        addValue(left, y, "Subtitle Scale", String.format(java.util.Locale.ROOT, "%.1f", config.subtitleScale), -0.1, 0.1,
                () -> config.subtitleScale = Math.max(0.6, config.subtitleScale - 0.1),
                () -> config.subtitleScale = Math.min(2.2, config.subtitleScale + 0.1));
        addValue(right, y, "Y Offset", Integer.toString(config.yOffset), -5, 5,
                () -> config.yOffset = Math.max(-120, config.yOffset - 5),
                () -> config.yOffset = Math.min(160, config.yOffset + 5));
        y += 24;
        addToggle(left, y, "Subtitle", config.showSubtitle, () -> config.showSubtitle = !config.showSubtitle);
        addToggle(right, y, "Style Colors", config.useStyleColors, () -> config.useStyleColors = !config.useStyleColors);
        y += 24;
        addToggle(left, y, "Decor Lines", config.showDecorativeLines, () -> config.showDecorativeLines = !config.showDecorativeLines);
        addValue(right, y, "Line Length", Integer.toString(config.decorativeLineLength), -10, 10,
                () -> config.decorativeLineLength = Math.max(24, config.decorativeLineLength - 10),
                () -> config.decorativeLineLength = Math.min(240, config.decorativeLineLength + 10));
        y += 24;
        addValue(left, y, "Line Width", Integer.toString(config.lineWidth), -1, 1,
                () -> config.lineWidth = Math.max(1, config.lineWidth - 1),
                () -> config.lineWidth = Math.min(3, config.lineWidth + 1));
        addValue(right, y, "Opacity", String.format(java.util.Locale.ROOT, "%.1f", config.opacity), -0.1, 0.1,
                () -> config.opacity = Math.max(0.0, config.opacity - 0.1),
                () -> config.opacity = Math.min(1.0, config.opacity + 0.1));
        y += 24;
        addToggle(left, y, "Text Shadow", config.textShadow, () -> config.textShadow = !config.textShadow);
        addToggle(right, y, "Hide With F3", config.suppressWhenDebugHudOpen,
                () -> config.suppressWhenDebugHudOpen = !config.suppressWhenDebugHudOpen);
        y += 24;
        addValue(left, y, "Fade In", Integer.toString(config.fadeInTicks), -5, 5,
                () -> config.fadeInTicks = Math.max(0, config.fadeInTicks - 5),
                () -> config.fadeInTicks += 5);
        addValue(right, y, "Stay", Integer.toString(config.stayTicks), -5, 5,
                () -> config.stayTicks = Math.max(1, config.stayTicks - 5),
                () -> config.stayTicks += 5);
        y += 24;
        addValue(left, y, "Fade Out", Integer.toString(config.fadeOutTicks), -5, 5,
                () -> config.fadeOutTicks = Math.max(0, config.fadeOutTicks - 5),
                () -> config.fadeOutTicks += 5);
        addValue(right, y, "Max Width", Integer.toString(config.maxWidth), -20, 20,
                () -> config.maxWidth = Math.max(120, config.maxWidth - 20),
                () -> config.maxWidth = Math.min(800, config.maxWidth + 20));
        y += 24;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 75, y, 150, 20)
                .build());
    }

    private void addToggle(int x, int y, String label, boolean value, Runnable action) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label + ": " + (value ? "On" : "Off")), button -> {
            action.run();
            saveAndRebuild();
        }).dimensions(x, y, 150, 20).build());
    }

    private void addValue(int x, int y, String label, String value, double unusedMinus, double unusedPlus, Runnable minus, Runnable plus) {
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
            minus.run();
            saveAndRebuild();
        }).dimensions(x, y, 22, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(label + ": " + value), button -> {
            plus.run();
            saveAndRebuild();
        }).dimensions(x + 24, y, 102, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
            plus.run();
            saveAndRebuild();
        }).dimensions(x + 128, y, 22, 20).build());
    }

    private void saveAndRebuild() {
        WorldRemembersLivingLegendsFabricClientConfig.get().normalize();
        WorldRemembersLivingLegendsFabricClientConfig.save(LOGGER);
        rebuild();
    }
}
