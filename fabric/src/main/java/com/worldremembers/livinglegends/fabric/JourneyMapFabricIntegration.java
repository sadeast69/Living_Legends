package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.map.MapDestinationDescriptor;
import com.worldremembers.livinglegends.map.MapIntegrationSettings;
import com.worldremembers.livinglegends.map.MapPlaceDescriptor;
import com.worldremembers.livinglegends.map.PlaceMapIntegration;
import com.worldremembers.livinglegends.visual.PlaceVisualTheme;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.DisplayType;
import journeymap.api.v2.client.display.IOverlayListener;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.event.FullscreenRenderEvent;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.util.UIState;
import journeymap.api.v2.common.event.CommonEventRegistry;
import journeymap.api.v2.common.event.FullscreenEventRegistry;
import journeymap.api.v2.common.event.common.WaypointEvent;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class JourneyMapFabricIntegration implements PlaceMapIntegration {
    private static final String MOD_ID = WorldRemembersLivingLegends.MOD_ID;
    private static final String PROVIDER_ID = "journeymap";
    private static final String GROUP_NAME = "World Remembers";
    private static final String DESTINATION_DATA_KEY = "living_legends_destination";
    private static final String PLACE_ID_DATA_KEY = "living_legends_place_id";
    private static final Identifier JOURNAL_ICON = Identifier.of(MOD_ID, "textures/item/world_journal.png");
    private static final int LABEL_TEXTURE_HEIGHT = 78;
    private static final int LABEL_MIN_TEXTURE_WIDTH = 180;
    private static final int LABEL_MAX_TEXTURE_WIDTH = 420;
    private static final int LABEL_CACHE_LIMIT = 256;
    private static final int TOOLTIP_MIN_WIDTH = 138;
    private static final int TOOLTIP_MAX_WIDTH = 238;
    private static final int TOOLTIP_PADDING_X = 8;
    private static final int TOOLTIP_PADDING_Y = 6;
    private static final int TOOLTIP_LINE_HEIGHT = 10;
    private static final int TOOLTIP_LINE_GAP = 2;
    private static final int TOOLTIP_EDGE_MARGIN = 6;
    private static final int TOOLTIP_MOUSE_OFFSET_X = 12;
    private static final int TOOLTIP_MOUSE_OFFSET_Y = 10;
    private static final int TOOLTIP_DESCRIPTION_LINES = 2;
    private static final long TOOLTIP_STALE_MILLIS = 350L;
    private static final Map<String, MapImage> LABEL_IMAGE_CACHE = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, MapImage> eldest) {
            return size() > LABEL_CACHE_LIMIT;
        }
    };

    private final IClientAPI api;
    private final Map<String, MarkerOverlay> labels = new LinkedHashMap<>();
    private final Map<String, Waypoint> destinations = new LinkedHashMap<>();
    private HoveredLabel hoveredLabel;

    JourneyMapFabricIntegration(IClientAPI api) {
        this.api = api;
        try {
            CommonEventRegistry.WAYPOINT_EVENT.subscribe(this, MOD_ID, this::onWaypointEvent);
        } catch (RuntimeException | LinkageError ignored) {
        }
        try {
            FullscreenEventRegistry.FULLSCREEN_RENDER_EVENT.subscribe(this, MOD_ID, this::renderHoveredTooltip);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean available() {
        return api != null;
    }

    @Override
    public boolean supportsPlaceLabels() {
        return true;
    }

    @Override
    public boolean supportsDestinations() {
        return true;
    }

    @Override
    public void replacePlaceLabels(Collection<MapPlaceDescriptor> places, MapIntegrationSettings settings) {
        clearPlaceLabels();
        if (!available() || settings == null || !settings.placeLabelsEnabledFor(PROVIDER_ID) || places == null) {
            return;
        }
        for (MapPlaceDescriptor place : places) {
            if (place == null || place.placeId().isBlank()) {
                continue;
            }
            MarkerOverlay marker = createMarker(place, settings);
            labels.put(place.placeId(), marker);
            showMarker(marker);
        }
    }

    @Override
    public void clearPlaceLabels() {
        labels.clear();
        hoveredLabel = null;
        if (!available()) {
            return;
        }
        try {
            api.removeAll(MOD_ID, DisplayType.Marker);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    @Override
    public void addOrUpdateDestination(MapDestinationDescriptor destination, MapIntegrationSettings settings) {
        if (!available()) {
            return;
        }
        if (settings == null || !settings.destinationsEnabledFor(PROVIDER_ID)) {
            clearDestinations();
            return;
        }
        if (destination == null || destination.placeId().isBlank()) {
            return;
        }
        try {
            Waypoint waypoint = destinations.get(destination.placeId());
            if (waypoint == null || api.getWaypoint(MOD_ID, waypoint.getGuid()) == null) {
                waypoint = WaypointFactory.createClientWaypoint(
                        MOD_ID,
                        blockPos(destination.centerX(), destination.centerY(), destination.centerZ()),
                        destinationName(destination),
                        destination.dimensionId(),
                        false
                );
                destinations.put(destination.placeId(), waypoint);
            }
            configureDestinationWaypoint(waypoint, destination);
            api.addWaypoint(MOD_ID, waypoint);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    @Override
    public void removeDestination(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            return;
        }
        Waypoint waypoint = destinations.remove(placeId);
        if (available() && waypoint != null) {
            try {
                api.removeWaypoint(MOD_ID, waypoint);
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
    }

    @Override
    public void clearDestinations() {
        if (!available()) {
            destinations.clear();
            return;
        }
        List<Waypoint> removed = new ArrayList<>(destinations.values());
        destinations.clear();
        for (Waypoint waypoint : removed) {
            try {
                api.removeWaypoint(MOD_ID, waypoint);
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
    }

    @Override
    public boolean hasDestination(String placeId) {
        if (!available() || placeId == null || placeId.isBlank()) {
            return false;
        }
        Waypoint waypoint = destinations.get(placeId);
        if (waypoint == null) {
            return false;
        }
        boolean exists;
        try {
            exists = api.getWaypoint(MOD_ID, waypoint.getGuid()) != null;
        } catch (RuntimeException | LinkageError ignored) {
            exists = false;
        }
        if (!exists) {
            destinations.remove(placeId);
        }
        return exists;
    }

    private MarkerOverlay createMarker(MapPlaceDescriptor place, MapIntegrationSettings settings) {
        TooltipContent tooltip = TooltipContent.from(place.tooltip());
        PlaceVisualTheme theme = place.visualTheme();
        MarkerOverlay marker = new MarkerOverlay(
                MOD_ID,
                blockPos(place.centerX(), place.centerY(), place.centerZ()),
                labelImage(place)
        );
        marker.setDimension(dimension(place.dimensionId()))
                .setLabel(null)
                .setTitle(null)
                .setOverlayGroupName(GROUP_NAME)
                .setDisplayOrder(1100)
                .setActiveUIs(Context.UI.Fullscreen, Context.UI.Minimap)
                .setActiveMapTypes(Context.MapType.all());
        if (settings.placeLabels().showTooltips() && !tooltip.empty()) {
            marker.setOverlayListener(new LabelTooltipListener(place.placeId(), tooltip, theme));
        }
        return marker;
    }

    private void renderHoveredTooltip(FullscreenRenderEvent event) {
        HoveredLabel hover = hoveredLabel;
        if (hover == null || hover.stale() || event == null || event.getFullscreen() == null) {
            return;
        }
        UIState state = event.getFullscreen().getUiState();
        if (state == null || state.ui != Context.UI.Fullscreen || !sameDimension(state, hover.mapState())) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }
        TooltipLayout layout = TooltipLayout.create(client.textRenderer, hover.content());
        if (layout.empty()) {
            return;
        }
        DrawContext context = event.getGraphics();
        Rectangle2D.Double bounds = state.displayBounds;
        int minX = bounds == null ? 0 : (int) Math.floor(bounds.x);
        int minY = bounds == null ? 0 : (int) Math.floor(bounds.y);
        int maxX = bounds == null
                ? client.getWindow().getScaledWidth()
                : (int) Math.ceil(bounds.x + bounds.width);
        int maxY = bounds == null
                ? client.getWindow().getScaledHeight()
                : (int) Math.ceil(bounds.y + bounds.height);
        int x = clamp(
                event.getMouseX() + TOOLTIP_MOUSE_OFFSET_X,
                minX + TOOLTIP_EDGE_MARGIN,
                maxX - layout.width() - TOOLTIP_EDGE_MARGIN
        );
        int aboveY = event.getMouseY() - layout.height() - TOOLTIP_MOUSE_OFFSET_Y;
        int belowY = event.getMouseY() + TOOLTIP_MOUSE_OFFSET_Y;
        int y = aboveY >= minY + TOOLTIP_EDGE_MARGIN
                ? aboveY
                : clamp(belowY, minY + TOOLTIP_EDGE_MARGIN, maxY - layout.height() - TOOLTIP_EDGE_MARGIN);
        drawTooltipCard(context, client.textRenderer, layout, x, y, hover.theme());
    }

    private static void drawTooltipCard(
            DrawContext context,
            TextRenderer textRenderer,
            TooltipLayout layout,
            int x,
            int y,
            PlaceVisualTheme theme
    ) {
        PlaceVisualTheme resolvedTheme = theme == null ? PlaceVisualTheme.DEFAULT : theme;
        int right = x + layout.width();
        int bottom = y + layout.height();
        context.fill(x + 2, y + 2, right + 2, bottom + 2, 0x66000000);
        context.fill(x, y, right, bottom, 0xF222170C);
        context.fill(x + 1, y + 1, right - 1, bottom - 1, withAlpha(resolvedTheme.outlineColor(), 0xF6));
        context.fill(x + 3, y + 3, right - 3, bottom - 3, 0xF8E9D0A0);
        context.fill(x + 6, y + 5, right - 6, y + 6, withAlpha(resolvedTheme.tooltipAccentColor(), 0xB8));

        int lineY = y + TOOLTIP_PADDING_Y;
        context.drawText(textRenderer, layout.title(), x + TOOLTIP_PADDING_X, lineY, resolvedTheme.outlineColor(), false);
        lineY += TOOLTIP_LINE_HEIGHT + TOOLTIP_LINE_GAP;
        context.drawText(textRenderer, layout.type(), x + TOOLTIP_PADDING_X, lineY, blend(resolvedTheme.tooltipAccentColor(), 0xFF6B4A28), false);
        for (String line : layout.descriptionLines()) {
            lineY += TOOLTIP_LINE_HEIGHT + TOOLTIP_LINE_GAP;
            context.drawText(textRenderer, line, x + TOOLTIP_PADDING_X, lineY, 0xFF6D5636, false);
        }
    }

    private final class LabelTooltipListener implements IOverlayListener {
        private final String placeId;
        private final TooltipContent content;
        private final PlaceVisualTheme theme;

        private LabelTooltipListener(String placeId, TooltipContent content, PlaceVisualTheme theme) {
            this.placeId = placeId == null ? "" : placeId;
            this.content = content == null ? TooltipContent.none() : content;
            this.theme = theme == null ? PlaceVisualTheme.DEFAULT : theme;
        }

        @Override
        public void onMouseMove(UIState mapState, Point2D.Double mousePosition, BlockPos blockPosition) {
            if (mapState == null || mousePosition == null || mapState.ui != Context.UI.Fullscreen || content.empty()) {
                clearHover();
                return;
            }
            hoveredLabel = new HoveredLabel(
                    placeId,
                    content,
                    theme,
                    mapState,
                    (int) Math.round(mousePosition.x),
                    (int) Math.round(mousePosition.y),
                    System.currentTimeMillis()
            );
        }

        @Override
        public void onMouseOut(UIState mapState, Point2D.Double mousePosition, BlockPos blockPosition) {
            clearHover();
        }

        @Override
        public void onDeactivate(UIState mapState) {
            clearHover();
        }

        private void clearHover() {
            if (hoveredLabel != null && hoveredLabel.placeId().equals(placeId)) {
                hoveredLabel = null;
            }
        }
    }

    private record HoveredLabel(
            String placeId,
            TooltipContent content,
            PlaceVisualTheme theme,
            UIState mapState,
            int mouseX,
            int mouseY,
            long updatedAt
    ) {
        private boolean stale() {
            return System.currentTimeMillis() - updatedAt > TOOLTIP_STALE_MILLIS;
        }
    }

    private record TooltipContent(String title, String type, String description) {
        private static TooltipContent none() {
            return new TooltipContent("", "", "");
        }

        private static TooltipContent from(String raw) {
            if (raw == null || raw.isBlank()) {
                return none();
            }
            List<String> lines = new ArrayList<>();
            for (String part : raw.split("\\R")) {
                String line = cleanTooltipText(part);
                if (!line.isBlank() && !isTechnicalTooltipLine(line)) {
                    lines.add(line);
                }
            }
            if (lines.isEmpty()) {
                return none();
            }
            String title = lines.get(0);
            String type = lines.size() > 1 ? lines.get(1) : "";
            String description = lines.size() > 2
                    ? cleanTooltipText(String.join(" ", lines.subList(2, lines.size())))
                    : "";
            return new TooltipContent(title, type, description);
        }

        private boolean empty() {
            return title.isBlank() && type.isBlank() && description.isBlank();
        }
    }

    private record TooltipLayout(String title, String type, List<String> descriptionLines, int width, int height) {
        private static TooltipLayout create(TextRenderer textRenderer, TooltipContent content) {
            if (textRenderer == null || content == null || content.empty()) {
                return new TooltipLayout("", "", List.of(), 0, 0);
            }
            int maxContentWidth = TOOLTIP_MAX_WIDTH - TOOLTIP_PADDING_X * 2;
            int minContentWidth = TOOLTIP_MIN_WIDTH - TOOLTIP_PADDING_X * 2;
            String title = ellipsizeToWidth(cleanTooltipText(content.title()), maxContentWidth, textRenderer);
            String type = ellipsizeToWidth(cleanTooltipText(content.type()), maxContentWidth, textRenderer);

            int preferredWidth = Math.max(textRenderer.getWidth(title), textRenderer.getWidth(type));
            if (!content.description().isBlank()) {
                int descriptionTarget = Math.max(168, textRenderer.getWidth(content.description()) / TOOLTIP_DESCRIPTION_LINES);
                preferredWidth = Math.max(preferredWidth, descriptionTarget);
            }
            preferredWidth = clamp(preferredWidth, minContentWidth, maxContentWidth);

            List<String> descriptionLines = wrapDescription(content.description(), preferredWidth, textRenderer);
            int contentWidth = Math.max(textRenderer.getWidth(title), textRenderer.getWidth(type));
            for (String line : descriptionLines) {
                contentWidth = Math.max(contentWidth, textRenderer.getWidth(line));
            }
            contentWidth = clamp(contentWidth, minContentWidth, maxContentWidth);

            int lineCount = 0;
            if (!title.isBlank()) {
                lineCount++;
            }
            if (!type.isBlank()) {
                lineCount++;
            }
            lineCount += descriptionLines.size();
            if (lineCount == 0) {
                return new TooltipLayout("", "", List.of(), 0, 0);
            }
            int gaps = Math.max(0, lineCount - 1);
            int height = TOOLTIP_PADDING_Y * 2 + lineCount * TOOLTIP_LINE_HEIGHT + gaps * TOOLTIP_LINE_GAP;
            return new TooltipLayout(title, type, List.copyOf(descriptionLines), contentWidth + TOOLTIP_PADDING_X * 2, height);
        }

        private boolean empty() {
            return width <= 0 || height <= 0;
        }
    }

    private static List<String> wrapDescription(String raw, int maxWidth, TextRenderer textRenderer) {
        String description = cleanTooltipText(raw);
        if (description.isBlank()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        String[] words = description.split("\\s+");
        String current = "";
        boolean truncated = false;
        int index = 0;
        while (index < words.length) {
            String word = words[index];
            String candidate = current.isBlank() ? word : current + " " + word;
            if (textRenderer.getWidth(candidate) <= maxWidth) {
                current = candidate;
                index++;
                continue;
            }

            if (current.isBlank()) {
                current = ellipsizeToWidth(word, maxWidth, textRenderer);
                index++;
            }
            lines.add(current);
            current = "";
            if (lines.size() == TOOLTIP_DESCRIPTION_LINES) {
                truncated = index < words.length;
                break;
            }
        }
        if (!truncated && !current.isBlank()) {
            if (lines.size() < TOOLTIP_DESCRIPTION_LINES) {
                lines.add(current);
            } else {
                truncated = true;
            }
        }
        if (truncated && !lines.isEmpty()) {
            int lastIndex = lines.size() - 1;
            lines.set(lastIndex, appendEllipsisToFit(lines.get(lastIndex), maxWidth, textRenderer));
        }
        return lines;
    }

    private static String cleanTooltipText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("(?i)\\u00A7[0-9A-FK-OR]", "").trim().replaceAll("\\s+", " ");
    }

    private static boolean isTechnicalTooltipLine(String value) {
        String clean = cleanTooltipText(value).toLowerCase(Locale.ROOT);
        if (clean.isBlank()) {
            return true;
        }
        if (clean.matches(".*\\b-?\\d+\\s*,\\s*-?\\d+\\s*,\\s*-?\\d+\\b.*")) {
            return true;
        }
        return clean.matches(".*\\b[a-z0-9_.-]+:[a-z0-9_./-]+\\b.*");
    }

    private static String ellipsizeToWidth(String value, int maxWidth, TextRenderer textRenderer) {
        String clean = cleanTooltipText(value);
        if (clean.isBlank() || textRenderer.getWidth(clean) <= maxWidth) {
            return clean;
        }
        return appendEllipsisToFit(clean, maxWidth, textRenderer);
    }

    private static String appendEllipsisToFit(String value, int maxWidth, TextRenderer textRenderer) {
        String clean = cleanTooltipText(value);
        while (clean.endsWith(".")) {
            clean = clean.substring(0, clean.length() - 1).trim();
        }
        while (!clean.isEmpty() && textRenderer.getWidth(clean + "...") > maxWidth) {
            clean = clean.substring(0, clean.length() - 1).trim();
        }
        return clean.isEmpty() ? "..." : clean + "...";
    }

    private static boolean sameDimension(UIState current, UIState hover) {
        if (current == null || hover == null) {
            return false;
        }
        return current.dimension == hover.dimension
                || (current.dimension != null && current.dimension.equals(hover.dimension));
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private void configureDestinationWaypoint(Waypoint waypoint, MapDestinationDescriptor destination) {
        waypoint.setName(destinationName(destination));
        waypoint.setBlockPos(blockPos(destination.centerX(), destination.centerY(), destination.centerZ()));
        waypoint.setPrimaryDimension(destination.dimensionId());
        waypoint.setEnabled(true);
        waypoint.setPersistent(false);
        waypoint.setShowDeviation(true);
        waypoint.setColor(typeColor(destination.placeType()));
        waypoint.setIconResourceLoctaion(JOURNAL_ICON);
        waypoint.setIconTextureSize(32, 32);
        waypoint.setIconColor(typeColor(destination.placeType()));
        waypoint.setIconOpacity(1.0f);
        waypoint.setCustomData(DESTINATION_DATA_KEY, "true");
        waypoint.setCustomData(PLACE_ID_DATA_KEY, destination.placeId());
    }

    private void onWaypointEvent(WaypointEvent event) {
        if (event == null
                || event.getContext() != WaypointEvent.Context.DELETED
                || event.getWaypoint() == null
                || !MOD_ID.equals(event.getWaypoint().getModId())) {
            return;
        }
        String placeId = event.getWaypoint().getCustomData(PLACE_ID_DATA_KEY);
        if (placeId == null || placeId.isBlank()) {
            removeDestinationByGuid(event.getWaypoint().getGuid());
            return;
        }
        destinations.remove(placeId);
    }

    private void removeDestinationByGuid(String guid) {
        Iterator<Map.Entry<String, Waypoint>> iterator = destinations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Waypoint> entry = iterator.next();
            if (entry.getValue() != null && entry.getValue().getGuid().equals(guid)) {
                iterator.remove();
                return;
            }
        }
    }

    private void showMarker(MarkerOverlay marker) {
        try {
            api.show(marker);
        } catch (Exception ignored) {
        }
    }

    private static BlockPos blockPos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    private static RegistryKey<World> dimension(String dimensionId) {
        Identifier id = Identifier.tryParse(dimensionId == null || dimensionId.isBlank()
                ? "minecraft:overworld"
                : dimensionId);
        if (id == null) {
            id = World.OVERWORLD.getValue();
        }
        return RegistryKey.of(RegistryKeys.WORLD, id);
    }

    private static MapImage labelImage(MapPlaceDescriptor place) {
        String displayName = cleanLabelText(place == null ? "" : place.displayName());
        PlaceType type = place == null ? PlaceType.CUSTOM : place.placeType();
        PlaceVisualTheme theme = place == null || place.visualTheme() == null ? PlaceVisualTheme.DEFAULT : place.visualTheme();
        String cacheKey = type.idString() + "|" + theme.fingerprintKey() + "|" + displayName;
        synchronized (LABEL_IMAGE_CACHE) {
            return LABEL_IMAGE_CACHE.computeIfAbsent(cacheKey, ignored -> renderLabelImage(displayName, theme));
        }
    }

    private static MapImage renderLabelImage(String displayName, PlaceVisualTheme theme) {
        try {
            BufferedImage image = createLabelBitmap(displayName, theme);
            NativeImage nativeImage = readNativeImage(image);
            return new MapImage(nativeImage)
                    .setDisplayWidth(Math.max(142.0, Math.min(236.0, image.getWidth() * 0.62)))
                    .setDisplayHeight(46.0)
                    .setColor(0xFFFFFF)
                    .setOpacity(1.0f)
                    .setBlur(true)
                    .centerAnchors();
        } catch (IOException | RuntimeException exception) {
            return new MapImage(JOURNAL_ICON, 32, 32)
                    .setDisplayWidth(1.0)
                    .setDisplayHeight(1.0)
                    .setOpacity(0.0f)
                    .centerAnchors();
        }
    }

    private static BufferedImage createLabelBitmap(String displayName, PlaceVisualTheme theme) {
        PlaceVisualTheme resolvedTheme = theme == null ? PlaceVisualTheme.DEFAULT : theme;
        String baseLabel = displayName.isBlank() ? Text.translatable("living_legends.map.destination_name_fallback").getString() : displayName;
        String label = resolvedTheme.decoratedLabel(baseLabel);
        Font font = labelFont(label);
        FontMetrics metrics = metrics(font);
        int padding = labelTexturePadding(resolvedTheme);
        int width = Math.max(
                LABEL_MIN_TEXTURE_WIDTH,
                Math.min(LABEL_MAX_TEXTURE_WIDTH, metrics.stringWidth(label) + padding)
        );
        BufferedImage image = new BufferedImage(width, LABEL_TEXTURE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            drawFantasyLabel(graphics, label, font, width, LABEL_TEXTURE_HEIGHT, resolvedTheme);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static Font labelFont(String label) {
        int size = 34;
        Font font = new Font(Font.SERIF, Font.BOLD | Font.ITALIC, size);
        while (size > 24 && metrics(font).stringWidth(label) > LABEL_MAX_TEXTURE_WIDTH - 76) {
            size -= 2;
            font = font.deriveFont((float) size);
        }
        return font;
    }

    private static FontMetrics metrics(Font font) {
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scratch.createGraphics();
        try {
            return graphics.getFontMetrics(font);
        } finally {
            graphics.dispose();
        }
    }

    private static void drawFantasyLabel(Graphics2D graphics, String label, Font font, int width, int height, PlaceVisualTheme theme) {
        PlaceVisualTheme resolvedTheme = theme == null ? PlaceVisualTheme.DEFAULT : theme;
        FontMetrics metrics = graphics.getFontMetrics(font);
        int textWidth = metrics.stringWidth(label);
        int baseline = height / 2 + 16;
        int arcHeight = curveHeight(resolvedTheme);
        int sideInset = "line_ends".equals(resolvedTheme.glyphPlacement()) ? 38 : 22;
        int left = Math.max(sideInset, (width - textWidth) / 2);
        int right = Math.min(width - sideInset, left + textWidth);

        drawDecorativeLine(graphics, left, right, baseline, font, resolvedTheme);

        graphics.setFont(font);
        double cursor = left;
        double halfWidth = Math.max(1.0, textWidth / 2.0);
        for (int offset = 0; offset < label.length(); ) {
            int codePoint = label.codePointAt(offset);
            String glyph = new String(Character.toChars(codePoint));
            int glyphWidth = Math.max(1, metrics.stringWidth(glyph));
            double center = cursor + glyphWidth / 2.0;
            double normalized = (center - (left + textWidth / 2.0)) / halfWidth;
            double y = baseline - arcHeight * (1.0 - normalized * normalized);
            double angle = arcHeight <= 0 ? 0.0 : Math.atan((2.0 * arcHeight * normalized) / halfWidth) * 0.72;
            if (!glyph.isBlank()) {
                drawGlyph(graphics, glyph, center, y, angle, glyphWidth, resolvedTheme);
            }
            cursor += glyphWidth;
            offset += Character.charCount(codePoint);
        }
    }

    private static void drawGlyph(
            Graphics2D graphics,
            String glyph,
            double centerX,
            double baselineY,
            double angle,
            int glyphWidth,
            PlaceVisualTheme theme
    ) {
        PlaceVisualTheme resolvedTheme = theme == null ? PlaceVisualTheme.DEFAULT : theme;
        AffineTransform old = graphics.getTransform();
        graphics.translate(centerX, baselineY);
        graphics.rotate(angle);
        int x = -glyphWidth / 2;
        int shadowOffset = "heavy".equals(resolvedTheme.shadowStyle()) ? 3 : 2;
        graphics.setColor(new Color(withAlpha(resolvedTheme.shadowColor(), shadowAlpha(resolvedTheme)), true));
        graphics.drawString(glyph, x + shadowOffset, shadowOffset);
        graphics.setColor(new Color(withAlpha(resolvedTheme.outlineColor(), outlineAlpha(resolvedTheme)), true));
        int outlineRadius = outlineRadius(resolvedTheme);
        for (int ox = -outlineRadius; ox <= outlineRadius; ox++) {
            for (int oy = -outlineRadius; oy <= outlineRadius; oy++) {
                if (ox != 0 || oy != 0) {
                    graphics.drawString(glyph, x + ox, oy);
                }
            }
        }
        graphics.setColor(new Color(withAlpha(blend(resolvedTheme.mainColor(), resolvedTheme.secondaryColor()), textAlpha(resolvedTheme)), true));
        graphics.drawString(glyph, x, 0);
        graphics.setTransform(old);
    }

    private static void drawDecorativeLine(
            Graphics2D graphics,
            int left,
            int right,
            int baseline,
            Font font,
            PlaceVisualTheme theme
    ) {
        int width = Math.max(20, right - left);
        int lineLeft = left - lineExtension(theme);
        int lineRight = right + lineExtension(theme);
        int arcY = baseline - 30;
        int arcH = switch (theme.curveStyle()) {
            case "soft" -> 30;
            case "tense" -> 40;
            case "none" -> 0;
            default -> 34;
        };
        int lineY = baseline + switch (theme.curveStyle()) {
            case "soft" -> 11;
            case "tense" -> 15;
            default -> 13;
        };
        int lineAlpha = lineAlpha(theme);
        Color line = new Color(withAlpha(theme.lineColor(), lineAlpha), true);
        Color accent = new Color(withAlpha(theme.accentColor(), Math.min(215, lineAlpha + 34)), true);
        Color shadow = new Color(withAlpha(theme.shadowColor(), shadowLineAlpha(theme)), true);

        graphics.setStroke(new BasicStroke(strokeWidth(theme) + 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(shadow);
        if (arcH > 0) {
            graphics.drawArc(lineLeft - 4, arcY + 2, width + lineExtension(theme) * 2 + 8, arcH, 202, 136);
        }
        graphics.drawLine(lineLeft, lineY + 3, lineRight, lineY + 3);

        switch (theme.lineStyle()) {
            case "faded" -> {
                graphics.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.setColor(line);
                graphics.drawLine(lineLeft + 8, lineY, lineRight - 8, lineY);
                if (arcH > 0) {
                    graphics.drawArc(lineLeft + 8, arcY, width + lineExtension(theme) * 2 - 16, arcH, 204, 132);
                }
            }
            case "double" -> {
                graphics.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.setColor(line);
                graphics.drawArc(lineLeft - 4, arcY, width + lineExtension(theme) * 2 + 8, arcH, 202, 136);
                graphics.setColor(accent);
                graphics.drawLine(lineLeft, lineY, lineRight, lineY);
                graphics.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.drawLine(lineLeft + 18, lineY + 5, lineRight - 18, lineY + 5);
            }
            case "broken" -> {
                graphics.setStroke(dashedStroke(theme, 9.0f, 5.0f));
                graphics.setColor(line);
                if (arcH > 0) {
                    graphics.drawArc(lineLeft, arcY, width + lineExtension(theme) * 2, arcH, 202, 136);
                }
                graphics.setStroke(new BasicStroke(strokeWidth(theme), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                drawSegmentedLine(graphics, lineLeft, lineRight, lineY, 18, 8);
                graphics.setColor(accent);
                graphics.drawLine(left - 12, lineY + 6, left - 5, lineY - 1);
                graphics.drawLine(right + 5, lineY - 1, right + 13, lineY + 7);
            }
            case "sharp" -> {
                graphics.setStroke(new BasicStroke(strokeWidth(theme), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                graphics.setColor(line);
                graphics.drawLine(lineLeft, lineY + 2, left - 8, lineY - 3);
                graphics.drawLine(left - 8, lineY - 3, right + 8, lineY - 3);
                graphics.drawLine(right + 8, lineY - 3, lineRight, lineY + 2);
                graphics.setColor(accent);
                graphics.drawLine(lineLeft + 10, lineY + 6, lineLeft + 24, lineY + 1);
                graphics.drawLine(lineRight - 24, lineY + 1, lineRight - 10, lineY + 6);
            }
            case "rough" -> {
                graphics.setStroke(new BasicStroke(strokeWidth(theme), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                graphics.setColor(line);
                drawRoughLine(graphics, lineLeft, lineRight, lineY);
                graphics.setColor(accent);
                drawSegmentedLine(graphics, lineLeft + 8, lineRight - 8, lineY + 6, 11, 7);
            }
            case "soft" -> {
                graphics.setStroke(new BasicStroke(strokeWidth(theme), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.setColor(line);
                if (arcH > 0) {
                    graphics.drawArc(lineLeft + 2, arcY + 1, width + lineExtension(theme) * 2 - 4, arcH, 204, 132);
                }
                graphics.setColor(accent);
                graphics.drawLine(lineLeft + 14, lineY, lineRight - 14, lineY);
            }
            case "mystic" -> {
                graphics.setStroke(dashedStroke(theme, 6.0f, 6.0f));
                graphics.setColor(line);
                if (arcH > 0) {
                    graphics.drawArc(lineLeft - 2, arcY, width + lineExtension(theme) * 2 + 4, arcH, 202, 136);
                }
                graphics.setStroke(new BasicStroke(strokeWidth(theme), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.setColor(accent);
                int gap = Math.max(18, width / 5);
                graphics.drawLine(lineLeft, lineY, left + width / 2 - gap, lineY);
                graphics.drawLine(left + width / 2 + gap, lineY, lineRight, lineY);
                graphics.drawLine(left + width / 2 - 8, lineY + 5, left + width / 2 + 8, lineY + 5);
            }
            default -> {
                graphics.setStroke(new BasicStroke(strokeWidth(theme), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.setColor(line);
                if (arcH > 0) {
                    graphics.drawArc(lineLeft, arcY, width + lineExtension(theme) * 2, arcH, 202, 136);
                }
                graphics.setColor(accent);
                graphics.drawLine(lineLeft + 10, lineY, lineRight - 10, lineY);
            }
        }
        drawIntegratedGlyphs(graphics, left, right, baseline, font, theme);
    }

    private static void drawIntegratedGlyphs(Graphics2D graphics, int left, int right, int baseline, Font font, PlaceVisualTheme theme) {
        if (theme.glyph().isBlank()) {
            return;
        }
        if ("line_ends".equals(theme.glyphPlacement())) {
            drawFlatGlyph(graphics, theme.glyph(), left - 27, baseline + 18, font, theme, 0.58f);
            drawFlatGlyph(graphics, theme.glyph(), right + 27, baseline + 18, font, theme, 0.58f);
        } else if ("center_mark".equals(theme.glyphPlacement())) {
            drawFlatGlyph(graphics, theme.glyph(), (left + right) / 2, baseline + 24, font, theme, 0.52f);
        }
    }

    private static void drawFlatGlyph(
            Graphics2D graphics,
            String glyph,
            int centerX,
            int baselineY,
            Font font,
            PlaceVisualTheme theme,
            float scale
    ) {
        Font oldFont = graphics.getFont();
        Font glyphFont = font.deriveFont(Font.BOLD, Math.max(15.0f, font.getSize2D() * scale));
        graphics.setFont(glyphFont);
        FontMetrics metrics = graphics.getFontMetrics(glyphFont);
        int x = centerX - metrics.stringWidth(glyph) / 2;
        graphics.setColor(new Color(withAlpha(theme.shadowColor(), shadowAlpha(theme) / 2), true));
        graphics.drawString(glyph, x + 1, baselineY + 1);
        graphics.setColor(new Color(withAlpha(theme.outlineColor(), outlineAlpha(theme) / 2), true));
        graphics.drawString(glyph, x - 1, baselineY);
        graphics.drawString(glyph, x + 1, baselineY);
        graphics.setColor(new Color(withAlpha(blend(theme.accentColor(), theme.mainColor()), Math.min(220, textAlpha(theme))), true));
        graphics.drawString(glyph, x, baselineY);
        graphics.setFont(oldFont);
    }

    private static int labelTexturePadding(PlaceVisualTheme theme) {
        return switch (theme.glyphPlacement()) {
            case "line_ends" -> 118;
            case "sides" -> 92;
            case "center_mark" -> 86;
            default -> 76;
        };
    }

    private static int curveHeight(PlaceVisualTheme theme) {
        return switch (theme.curveStyle()) {
            case "none" -> 0;
            case "soft" -> 6;
            case "tense" -> 13;
            default -> 9;
        };
    }

    private static int lineExtension(PlaceVisualTheme theme) {
        return switch (theme.labelWeight()) {
            case "faint" -> 18;
            case "legendary" -> 32;
            case "emphasized" -> 28;
            default -> 24;
        };
    }

    private static float strokeWidth(PlaceVisualTheme theme) {
        return switch (theme.labelWeight()) {
            case "faint" -> 1.15f;
            case "legendary" -> 2.85f;
            case "emphasized" -> 2.25f;
            default -> 1.75f;
        };
    }

    private static int lineAlpha(PlaceVisualTheme theme) {
        return switch (theme.labelWeight()) {
            case "faint" -> 88;
            case "legendary" -> 190;
            case "emphasized" -> 165;
            default -> 135;
        };
    }

    private static int textAlpha(PlaceVisualTheme theme) {
        return switch (theme.labelWeight()) {
            case "faint" -> 198;
            case "normal" -> 236;
            default -> 255;
        };
    }

    private static int shadowAlpha(PlaceVisualTheme theme) {
        return switch (theme.shadowStyle()) {
            case "subtle" -> 62;
            case "heavy" -> 132;
            case "glowing_soft" -> 104;
            default -> 96;
        };
    }

    private static int shadowLineAlpha(PlaceVisualTheme theme) {
        return switch (theme.shadowStyle()) {
            case "subtle" -> 38;
            case "heavy" -> 96;
            case "glowing_soft" -> 88;
            default -> 62;
        };
    }

    private static int outlineAlpha(PlaceVisualTheme theme) {
        return switch (theme.labelWeight()) {
            case "faint" -> 160;
            case "legendary" -> 238;
            default -> 218;
        };
    }

    private static int outlineRadius(PlaceVisualTheme theme) {
        return "legendary".equals(theme.labelWeight()) || "heavy".equals(theme.shadowStyle()) ? 2 : 1;
    }

    private static BasicStroke dashedStroke(PlaceVisualTheme theme, float dash, float gap) {
        return new BasicStroke(
                strokeWidth(theme),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                10.0f,
                new float[]{dash, gap},
                0.0f
        );
    }

    private static void drawSegmentedLine(Graphics2D graphics, int left, int right, int y, int segment, int gap) {
        for (int x = left; x < right; x += segment + gap) {
            graphics.drawLine(x, y, Math.min(right, x + segment), y);
        }
    }

    private static void drawRoughLine(Graphics2D graphics, int left, int right, int y) {
        int previousX = left;
        int previousY = y;
        boolean up = true;
        for (int x = left + 10; x <= right; x += 10) {
            int currentY = y + (up ? -2 : 2);
            graphics.drawLine(previousX, previousY, Math.min(x, right), currentY);
            previousX = Math.min(x, right);
            previousY = currentY;
            up = !up;
        }
    }

    private static NativeImage readNativeImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return NativeImage.read(bytes.toByteArray());
    }

    private static String cleanLabelText(String value) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (clean.length() <= 42) {
            return clean;
        }
        return clean.substring(0, 39).trim() + "...";
    }

    private static int blend(int base, int highlight) {
        int r = (((base >> 16) & 0xFF) + ((highlight >> 16) & 0xFF) * 2) / 3;
        int g = (((base >> 8) & 0xFF) + ((highlight >> 8) & 0xFF) * 2) / 3;
        int b = ((base & 0xFF) + (highlight & 0xFF) * 2) / 3;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int argb, int alpha) {
        return ((alpha & 0xFF) << 24) | (argb & 0x00FF_FFFF);
    }

    private static int typeColor(PlaceType type) {
        PlaceType resolved = type == null ? PlaceType.CUSTOM : type;
        return switch (resolved) {
            case DEATH_SITE -> 0xC76042;
            case BATTLEFIELD, PVP_ARENA, RAID_SITE -> 0xB94B38;
            case SLAUGHTER_FIELD -> 0xA98243;
            case PORTAL_LANDMARK, DIMENSION_THRESHOLD -> 0x777DE2;
            case FIRST_DISCOVERY, BOSS_SITE -> 0xC09338;
            case SETTLEMENT -> 0x5A9C58;
            case GENERAL_LANDMARK -> 0x73894D;
            default -> 0xE7C27A;
        };
    }

    private static String destinationName(MapDestinationDescriptor destination) {
        String displayName = destination == null ? "" : destination.displayName();
        if (displayName == null || displayName.isBlank()) {
            return Text.translatable("living_legends.map.destination_name_fallback").getString();
        }
        return Text.translatable("living_legends.map.destination_name", displayName).getString();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
