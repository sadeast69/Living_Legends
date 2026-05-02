package com.worldremembers.livinglegends.neoforge.client;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

final class PlaceTitleOverlayConfigScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldRemembersLivingLegends.MOD_ID + "-client-config");
    private final Screen parent;

    PlaceTitleOverlayConfigScreen(Screen parent) {
        super(Component.translatable("living_legends.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        drawCenteredWithShadow(graphics, title, width / 2, 18, 0xFFEADCA8);
        drawCenteredWithShadow(graphics, Component.translatable("living_legends.config.title_overlay"), width / 2, 31, 0xFFD8D8D8);
    }

    private void rebuild() {
        clearWidgets();
        WorldRemembersLivingLegendsNeoForgeClientConfig config = WorldRemembersLivingLegendsNeoForgeClientConfig.get();
        int left = width / 2 - 155;
        int right = width / 2 + 5;
        int y = 54;
        addToggle(left, y, "Enabled", config.enabled, () -> config.enabled = !config.enabled);
        addToggle(right, y, "Background", config.showBackground, () -> config.showBackground = !config.showBackground);
        y += 24;
        addValue(left, y, "Global Scale", String.format(Locale.ROOT, "%.1f", config.scale),
                () -> config.scale = Math.max(0.5, config.scale - 0.1),
                () -> config.scale = Math.min(2.0, config.scale + 0.1));
        addValue(right, y, "Title Scale", String.format(Locale.ROOT, "%.1f", config.titleScale),
                () -> config.titleScale = Math.max(0.8, config.titleScale - 0.1),
                () -> config.titleScale = Math.min(3.5, config.titleScale + 0.1));
        y += 24;
        addValue(left, y, "Subtitle Scale", String.format(Locale.ROOT, "%.1f", config.subtitleScale),
                () -> config.subtitleScale = Math.max(0.6, config.subtitleScale - 0.1),
                () -> config.subtitleScale = Math.min(2.2, config.subtitleScale + 0.1));
        addValue(right, y, "Y Offset", Integer.toString(config.yOffset),
                () -> config.yOffset = Math.max(-120, config.yOffset - 5),
                () -> config.yOffset = Math.min(160, config.yOffset + 5));
        y += 24;
        addToggle(left, y, "Subtitle", config.showSubtitle, () -> config.showSubtitle = !config.showSubtitle);
        addToggle(right, y, "Style Colors", config.useStyleColors, () -> config.useStyleColors = !config.useStyleColors);
        y += 24;
        addToggle(left, y, "Decor Lines", config.showDecorativeLines, () -> config.showDecorativeLines = !config.showDecorativeLines);
        addValue(right, y, "Line Length", Integer.toString(config.decorativeLineLength),
                () -> config.decorativeLineLength = Math.max(24, config.decorativeLineLength - 10),
                () -> config.decorativeLineLength = Math.min(240, config.decorativeLineLength + 10));
        y += 24;
        addValue(left, y, "Line Width", Integer.toString(config.lineWidth),
                () -> config.lineWidth = Math.max(1, config.lineWidth - 1),
                () -> config.lineWidth = Math.min(3, config.lineWidth + 1));
        addValue(right, y, "Opacity", String.format(Locale.ROOT, "%.1f", config.opacity),
                () -> config.opacity = Math.max(0.0, config.opacity - 0.1),
                () -> config.opacity = Math.min(1.0, config.opacity + 0.1));
        y += 24;
        addToggle(left, y, "Text Shadow", config.textShadow, () -> config.textShadow = !config.textShadow);
        addToggle(right, y, "Hide With F3", config.suppressWhenDebugHudOpen,
                () -> config.suppressWhenDebugHudOpen = !config.suppressWhenDebugHudOpen);
        y += 24;
        addValue(left, y, "Fade In", Integer.toString(config.fadeInTicks),
                () -> config.fadeInTicks = Math.max(0, config.fadeInTicks - 5),
                () -> config.fadeInTicks += 5);
        addValue(right, y, "Stay", Integer.toString(config.stayTicks),
                () -> config.stayTicks = Math.max(1, config.stayTicks - 5),
                () -> config.stayTicks += 5);
        y += 24;
        addValue(left, y, "Fade Out", Integer.toString(config.fadeOutTicks),
                () -> config.fadeOutTicks = Math.max(0, config.fadeOutTicks - 5),
                () -> config.fadeOutTicks += 5);
        addValue(right, y, "Max Width", Integer.toString(config.maxWidth),
                () -> config.maxWidth = Math.max(120, config.maxWidth - 20),
                () -> config.maxWidth = Math.min(800, config.maxWidth + 20));
        y += 24;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 75, y, 150, 20)
                .build());
    }

    private void addToggle(int x, int y, String label, boolean value, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label + ": " + (value ? "On" : "Off")), button -> {
            action.run();
            saveAndRebuild();
        }).bounds(x, y, 150, 20).build());
    }

    private void addValue(int x, int y, String label, String value, Runnable minus, Runnable plus) {
        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            minus.run();
            saveAndRebuild();
        }).bounds(x, y, 22, 20).build());
        addRenderableWidget(Button.builder(Component.literal(label + ": " + value), button -> {
            plus.run();
            saveAndRebuild();
        }).bounds(x + 24, y, 102, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            plus.run();
            saveAndRebuild();
        }).bounds(x + 128, y, 22, 20).build());
    }

    private void saveAndRebuild() {
        WorldRemembersLivingLegendsNeoForgeClientConfig.get().normalize();
        WorldRemembersLivingLegendsNeoForgeClientConfig.save(LOGGER);
        rebuild();
    }

    private void drawCenteredWithShadow(GuiGraphics graphics, Component component, int centerX, int y, int color) {
        graphics.drawString(font, component, centerX - font.width(component) / 2, y, color, true);
    }
}
