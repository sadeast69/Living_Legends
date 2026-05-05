package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.map.MapPlaceDescriptor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class WorldJournalScreen extends Screen {
    private static final Identifier BOOK_TEXTURE = Identifier.of(WorldRemembersLivingLegends.MOD_ID, "textures/gui/world_journal_book.png");
    private static final int BOOK_TEXTURE_WIDTH = 1306;
    private static final int BOOK_TEXTURE_HEIGHT = 816;
    private static final int BOOK_TEXTURE_U = 14;
    private static final int BOOK_TEXTURE_V = 12;
    private static final int BOOK_TEXTURE_REGION_WIDTH = 1278;
    private static final int BOOK_TEXTURE_REGION_HEIGHT = 782;
    private static final int BOOK_WIDTH = 409;
    private static final int BOOK_HEIGHT = 250;
    private static final int PAGE_WIDTH = 166;
    private static final int BOOK_PAGE_SIZE = 4;
    private static final int LIST_VISIBLE_ROWS = 4;
    private static final int LIST_ROW_HEIGHT = 31;
    private static final int TEXT_DARK = 0xFF3D2815;
    private static final int TEXT_MUTED = 0xFF765A35;
    private static final int TEXT_SOFT = 0xFF92764B;
    private static final int LINE = 0xFFB88E55;
    private static final int SOFT_LINE = 0x66B88E55;
    private static final int SELECTED = 0x3CA3763D;
    private static final String[] TYPE_FILTERS = {
            "",
            "general_landmark",
            "death_site",
            "battlefield",
            "slaughter_field",
            "settlement",
            "portal_landmark",
            "first_discovery",
            "boss_site",
            "mining_site",
            "custom"
    };
    private static final String[] DIMENSION_FILTERS = {
            "",
            "minecraft:overworld",
            "minecraft:the_nether",
            "minecraft:the_end"
    };
    private static final String[] SORT_MODES = {
            "DISTANCE",
            "TYPE",
            "NAME",
            "CREATED_TIME",
            "UPDATED_TIME"
    };

    private final Screen parent;
    private final List<WorldJournalS2CPayload.Entry> entries = new ArrayList<>();
    private TextFieldWidget searchField;
    private String tab = "ALL_PLACES";
    private String selectedPlaceId = "";
    private String typeFilter = "";
    private String dimensionFilter = "";
    private String sortMode = "NAME";
    private String sortDirection = "ASC";
    private int page;
    private int pageSize = BOOK_PAGE_SIZE;
    private int totalCount;
    private int listScroll;
    private boolean canManage;
    private boolean canTeleport;
    private boolean showExactCoordinates = true;
    private boolean moreOpen;
    private String lastDeletedPlaceId = "";

    WorldJournalScreen(Screen parent) {
        super(Text.translatable("living_legends.journal.title"));
        this.parent = parent;
    }

    void apply(WorldJournalS2CPayload payload) {
        if (payload == null) {
            return;
        }
        this.pageSize = BOOK_PAGE_SIZE;
        this.canManage = payload.canManage();
        this.canTeleport = payload.canTeleport();
        this.showExactCoordinates = payload.showExactCoordinates();
        if (payload.action() == WorldJournalS2CPayload.Action.PAGE
                || payload.action() == WorldJournalS2CPayload.Action.UPDATED
                || payload.action() == WorldJournalS2CPayload.Action.DELETED) {
            entries.clear();
            entries.addAll(payload.places());
            totalCount = payload.totalCount();
            page = payload.page();
            listScroll = Math.min(listScroll, Math.max(0, entries.size() - LIST_VISIBLE_ROWS));
            selectedPlaceId = payload.selectedPlaceId();
            if (payload.action() == WorldJournalS2CPayload.Action.DELETED) {
                lastDeletedPlaceId = payload.selectedPlaceId();
                moreOpen = false;
            }
            rebuild();
        }
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (!"SETTINGS".equals(tab)) {
            int listX = bookLeft() + 32;
            int listY = bookTop() + 100;
            int visibleRows = Math.min(LIST_VISIBLE_ROWS, Math.max(0, entries.size() - listScroll));
            if (mouseX >= listX
                    && mouseX <= listX + PAGE_WIDTH - 18
                    && mouseY >= listY
                    && mouseY <= listY + visibleRows * LIST_ROW_HEIGHT) {
                int index = listScroll + (int) ((mouseY - listY) / LIST_ROW_HEIGHT);
                if (index >= 0 && index < entries.size()) {
                    selectedPlaceId = entries.get(index).placeId();
                    moreOpen = false;
                    rebuild();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!"SETTINGS".equals(tab) && mouseX >= bookLeft() + 20 && mouseX <= bookLeft() + BOOK_WIDTH / 2 - 14) {
            int maxScroll = Math.max(0, entries.size() - LIST_VISIBLE_ROWS);
            int nextScroll = listScroll + (verticalAmount < 0 ? 1 : -1);
            nextScroll = Math.max(0, Math.min(maxScroll, nextScroll));
            if (nextScroll != listScroll) {
                listScroll = nextScroll;
                rebuild();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        drawCrispDimBackground(context, width, height);
        int left = bookLeft();
        int top = bookTop();
        drawBook(context, left, top);
        drawLeftPage(context, left, top);
        drawRightPage(context, left, top);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // The journal draws its own background before content; vanilla Screen background applies blur.
    }

    static void drawCrispDimBackground(DrawContext context, int width, int height) {
        context.fill(0, 0, width, height, 0x8A000000);
    }

    void refreshMapIntegrationControls() {
        rebuild();
    }

    private void rebuild() {
        clearChildren();
        FabricMapIntegrationClient.refreshDestinationUiState();
        int left = bookLeft();
        int top = bookTop();
        addTabs(left, top);
        if ("SETTINGS".equals(tab)) {
            addSettingsControls(left, top);
            return;
        }
        addListControls(left, top);
        addDetailsControls(left, top);
    }

    private void addTabs(int left, int top) {
        int y = top + 24;
        addDrawableChild(new JournalButtonWidget(left + 28, y, 50, 16, Text.translatable("living_legends.journal.tab.places"), button -> {
            tab = "ALL_PLACES";
            sortMode = "NAME";
            moreOpen = false;
            page = 0;
            requestPage();
        }, "ALL_PLACES".equals(tab) ? JournalButtonStyle.ACTIVE_TAB : JournalButtonStyle.TAB));
        addDrawableChild(new JournalButtonWidget(left + 80, y, 50, 16, Text.translatable("living_legends.journal.tab.nearby"), button -> {
            tab = "NEARBY";
            sortMode = "DISTANCE";
            moreOpen = false;
            page = 0;
            requestPage();
        }, "NEARBY".equals(tab) ? JournalButtonStyle.ACTIVE_TAB : JournalButtonStyle.TAB));
        addDrawableChild(new JournalButtonWidget(left + 132, y, 58, 16, Text.translatable("living_legends.journal.tab.settings"), button -> {
            tab = "SETTINGS";
            moreOpen = false;
            rebuild();
        }, "SETTINGS".equals(tab) ? JournalButtonStyle.ACTIVE_TAB : JournalButtonStyle.TAB));
    }

    private void addListControls(int left, int top) {
        searchField = new TextFieldWidget(
                textRenderer,
                left + 30,
                top + 52,
                104,
                16,
                Text.translatable("living_legends.journal.search")
        );
        searchField.setMaxLength(64);
        searchField.setText(searchQuery());
        searchField.setDrawsBackground(false);
        addDrawableChild(searchField);
        addDrawableChild(new JournalButtonWidget(left + 140, top + 52, 28, 16, Text.translatable("living_legends.journal.button.search"), button -> {
            page = 0;
            listScroll = 0;
            requestPage();
        }, JournalButtonStyle.SEARCH));
        addDrawableChild(new JournalButtonWidget(left + 30, top + 73, 48, 14, Text.literal(ellipsize(filterSummary(), 42)), button -> {
            typeFilter = next(TYPE_FILTERS, typeFilter);
            page = 0;
            listScroll = 0;
            requestPage();
        }, JournalButtonStyle.SMALL));
        addDrawableChild(new JournalButtonWidget(left + 82, top + 73, 42, 14, Text.literal(ellipsize(dimensionSummary(), 36)), button -> {
            dimensionFilter = next(DIMENSION_FILTERS, dimensionFilter);
            page = 0;
            listScroll = 0;
            requestPage();
        }, JournalButtonStyle.SMALL));
        addDrawableChild(new JournalButtonWidget(left + 128, top + 73, 48, 14, Text.literal(ellipsize(sortSummary(), 42)), button -> {
            sortMode = next(SORT_MODES, sortMode);
            page = 0;
            listScroll = 0;
            requestPage();
        }, JournalButtonStyle.SMALL));
        JournalButtonWidget previous = addDrawableChild(new JournalButtonWidget(left + 34, top + 224, 22, 15, Text.literal("<"), button -> {
            if (page > 0) {
                page--;
                listScroll = 0;
                requestPage();
            }
        }, JournalButtonStyle.PAGE));
        previous.active = page > 0;
        JournalButtonWidget next = addDrawableChild(new JournalButtonWidget(left + 154, top + 224, 22, 15, Text.literal(">"), button -> {
            if (page < totalPages() - 1) {
                page++;
                listScroll = 0;
                requestPage();
            }
        }, JournalButtonStyle.PAGE));
        next.active = page < totalPages() - 1;
    }

    private void addDetailsControls(int left, int top) {
        WorldJournalS2CPayload.Entry selected = selectedEntry();
        int x = left + 232;
        int y = top + 170;
        JournalButtonWidget coordinates = addDrawableChild(new JournalButtonWidget(x, y, 82, 17, Text.translatable("living_legends.journal.button.show_coordinates"), button -> showCoordinates(), JournalButtonStyle.NORMAL));
        coordinates.active = selected != null && showExactCoordinates;
        JournalButtonWidget more = addDrawableChild(new JournalButtonWidget(x + 88, y, 54, 17, Text.translatable("living_legends.journal.button.more"), button -> {
            moreOpen = !moreOpen;
            rebuild();
        }, moreOpen ? JournalButtonStyle.ACTIVE_TAB : JournalButtonStyle.NORMAL));
        more.active = selected != null;
        addDestinationControl(selected, x, top + 190);
        if (moreOpen && selected != null) {
            int menuY = top + 108;
            JournalButtonWidget copy = addDrawableChild(new JournalButtonWidget(x, menuY, 68, 15, Text.translatable("living_legends.journal.button.copy_coordinates"), button -> copyCoordinates(), JournalButtonStyle.SMALL));
            copy.active = showExactCoordinates;
            JournalButtonWidget rename = addDrawableChild(new JournalButtonWidget(x + 74, menuY, 68, 15, Text.translatable("living_legends.journal.button.rename"), button -> {
                if (client != null) {
                    client.setScreen(new WorldJournalRenameScreen(this, selected));
                }
            }, JournalButtonStyle.SMALL));
            rename.active = canManage;
            menuY += 18;
            JournalButtonWidget restore = addDrawableChild(new JournalButtonWidget(x, menuY, 68, 15, Text.translatable("living_legends.journal.button.restore_generated"), button ->
                    WorldJournalClient.send(actionPayload(WorldJournalC2SPayload.Action.RESTORE_GENERATED, selected.placeId(), ""))
            , JournalButtonStyle.SMALL));
            restore.active = canManage && selected.manualName();
            JournalButtonWidget teleport = addDrawableChild(new JournalButtonWidget(x + 74, menuY, 68, 15, Text.translatable("living_legends.journal.button.teleport"), button ->
                    WorldJournalClient.send(WorldJournalC2SPayload.action(WorldJournalC2SPayload.Action.TELEPORT, selected.placeId(), ""))
            , JournalButtonStyle.SMALL));
            teleport.active = canTeleport;
            menuY += 18;
            JournalButtonWidget delete = addDrawableChild(new JournalButtonWidget(x, menuY, 142, 15, Text.translatable("living_legends.journal.button.delete"), button -> {
                if (client != null) {
                    client.setScreen(new WorldJournalConfirmDeleteScreen(this, selected));
                }
            }, JournalButtonStyle.DANGER));
            delete.active = canManage;
        }
        JournalButtonWidget create = addDrawableChild(new JournalButtonWidget(left + BOOK_WIDTH - 148, top + 224, 64, 16, Text.translatable("living_legends.journal.button.create_place"), button -> {
            if (client != null) {
                client.setScreen(new WorldJournalCreatePlaceScreen(this));
            }
        }, JournalButtonStyle.NORMAL));
        create.active = true;
        addDrawableChild(new JournalButtonWidget(left + BOOK_WIDTH - 78, top + 224, 58, 16, Text.translatable("gui.done"), button -> close(), JournalButtonStyle.NORMAL));
    }

    private void addDestinationControl(WorldJournalS2CPayload.Entry selected, int x, int y) {
        int width = 142;
        int height = 15;
        boolean showDestinationButton = selected != null && FabricMapIntegrationClient.destinationButtonVisible();
        if (!showDestinationButton) {
            return;
        }
        boolean active = FabricMapIntegrationClient.isDestinationActive(selected.placeId());
        JournalButtonWidget destination = addDrawableChild(new JournalButtonWidget(
                x,
                y,
                width,
                height,
                Text.translatable(active
                        ? "living_legends.journal.button.remove_destination"
                        : "living_legends.journal.button.set_destination"),
                button -> {
                    FabricMapIntegrationClient.toggleDestination(mapDescriptor(selected));
                    rebuild();
                },
                active ? JournalButtonStyle.ACTIVE_TAB : JournalButtonStyle.SMALL
        ));
        destination.active = true;
    }

    private void addSettingsControls(int left, int top) {
        addDrawableChild(new JournalButtonWidget(left + 226, top + 172, 132, 18, Text.translatable("living_legends.journal.button.open_title_overlay_settings"), button ->
                WorldRemembersLivingLegendsFabricClient.openTitleOverlaySettings(this)
        , JournalButtonStyle.NORMAL));
        addDrawableChild(new JournalButtonWidget(left + BOOK_WIDTH - 78, top + 224, 58, 16, Text.translatable("gui.done"), button -> close(), JournalButtonStyle.NORMAL));
    }

    private void drawBook(DrawContext context, int left, int top) {
        context.drawTexture(
                BOOK_TEXTURE,
                left,
                top,
                BOOK_WIDTH,
                BOOK_HEIGHT,
                (float) BOOK_TEXTURE_U,
                (float) BOOK_TEXTURE_V,
                BOOK_TEXTURE_REGION_WIDTH,
                BOOK_TEXTURE_REGION_HEIGHT,
                BOOK_TEXTURE_WIDTH,
                BOOK_TEXTURE_HEIGHT
        );
    }

    private void drawLeftPage(DrawContext context, int left, int top) {
        if ("SETTINGS".equals(tab)) {
            return;
        }

        int searchX = left + 30;
        int searchY = top + 52;
        context.fill(searchX - 1, searchY - 1, searchX + 105, searchY + 17, 0xCCB88E55);
        context.fill(searchX, searchY, searchX + 104, searchY + 16, 0xCCF9EDCB);

        int listX = left + 32;
        int listY = top + 100;
        if (entries.isEmpty()) {
            drawText(context, Text.translatable(lastDeletedPlaceId.isBlank()
                    ? "living_legends.journal.empty"
                    : "living_legends.journal.place_deleted"), listX, listY, TEXT_MUTED);
        }
        int visibleRows = Math.min(LIST_VISIBLE_ROWS, Math.max(0, entries.size() - listScroll));
        for (int visibleIndex = 0; visibleIndex < visibleRows; visibleIndex++) {
            int index = listScroll + visibleIndex;
            WorldJournalS2CPayload.Entry entry = entries.get(index);
            int y = listY + visibleIndex * LIST_ROW_HEIGHT;
            boolean selected = entry.placeId().equals(selectedPlaceId);
            if (selected) {
                context.fill(listX - 4, y - 3, listX + PAGE_WIDTH - 18, y + LIST_ROW_HEIGHT - 4, SELECTED);
                context.fill(listX - 4, y - 3, listX + PAGE_WIDTH - 18, y - 2, 0x44B88E55);
            }
            int accent = typeColor(entry.placeType());
            context.fill(listX, y + 4, listX + 3, y + LIST_ROW_HEIGHT - 8, accent);
            drawText(context, Text.literal(ellipsize(displayName(entry), PAGE_WIDTH - 52)), listX + 8, y + 2, TEXT_DARK);
            String subtitle = Text.translatable(placeTypeKey(entry.placeType())).getString();
            if ("NEARBY".equals(tab) && entry.distanceToPlayer() >= 0.0) {
                subtitle = subtitle + " - " + distanceText(entry);
            }
            drawText(context, Text.literal(ellipsize(subtitle, PAGE_WIDTH - 52)), listX + 8, y + 14, TEXT_SOFT);
        }
        String pageText = Text.translatable("living_legends.journal.page").getString()
                + " " + (page + 1) + "/" + totalPages();
        drawCenteredText(context, Text.literal(pageText), left + 105, top + 227, TEXT_MUTED);
    }

    private void drawRightPage(DrawContext context, int left, int top) {
        int x = left + 232;
        int y = top + 62;
        if ("SETTINGS".equals(tab)) {
            drawText(context, Text.translatable("living_legends.journal.settings_title"), x, y, TEXT_DARK);
            context.fill(x, y + 15, x + 142, y + 16, SOFT_LINE);
            y += 28;
            for (String line : wrap(Text.translatable("living_legends.journal.settings_overlay").getString(), 142, 2)) {
                drawText(context, Text.literal(line), x, y, TEXT_MUTED);
                y += 12;
            }
            y += 8;
            for (String line : wrap(Text.translatable("living_legends.journal.settings_hint").getString(), 142, 3)) {
                drawText(context, Text.literal(line), x, y, TEXT_SOFT);
                y += 12;
            }
            return;
        }

        WorldJournalS2CPayload.Entry entry = selectedEntry();
        if (entry == null) {
            for (String line : wrap(Text.translatable("living_legends.journal.no_selected_place").getString(), 132, 3)) {
                drawText(context, Text.literal(line), x, y, TEXT_MUTED);
                y += 12;
            }
            return;
        }

        List<String> titleLines = wrap(displayName(entry), 120, 2);
        int titleColor = typeColor(entry.placeType());
        for (String line : titleLines) {
            drawText(context, Text.literal(line), x, y, titleColor);
            y += 12;
        }
        context.fill(x, y + 4, x + 142, y + 5, LINE);
        context.fill(x + 18, y + 7, x + 124, y + 8, SOFT_LINE);
        y += 15;
        drawText(context, Text.translatable(placeTypeKey(entry.placeType())), x, y, TEXT_MUTED);
        y += 19;
        drawDetail(context, x, y, "dimension", dimensionLabel(entry.dimension()));
        y += 14;
        if (showExactCoordinates) {
            drawDetail(context, x, y, "coordinates", coordinatesText(entry));
            y += 14;
        }
        if (entry.distanceToPlayer() >= 0.0) {
            drawDetail(context, x, y, "distance", distanceText(entry));
        }
        if (moreOpen) {
            drawMorePanelBackground(context, x - 4, top + 102);
        } else {
            drawFlavor(context, entry, x, top + 147);
        }
    }

    private void drawFlavor(DrawContext context, WorldJournalS2CPayload.Entry entry, int x, int y) {
        for (String line : wrap(journalFlavor(entry), 142, 2)) {
            drawText(context, Text.literal(line), x, y, TEXT_SOFT);
            y += 12;
        }
    }

    private String journalFlavor(WorldJournalS2CPayload.Entry entry) {
        String type = journalFlavorType(entry.placeType());
        String key = "living_legends.journal.flavor." + type;
        String variants = Text.translatable(key).getString();
        if (variants == null || variants.isBlank() || variants.equals(key)) {
            variants = Text.translatable("living_legends.journal.flavor.remembered").getString();
        }
        return selectFlavorVariant(variants, entry.placeId().hashCode());
    }

    private static String journalFlavorType(String type) {
        return switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
            case "death_site",
                 "battlefield",
                 "slaughter_field",
                 "pvp_arena",
                 "mining_site",
                 "portal_landmark",
                 "general_landmark",
                 "settlement",
                 "first_discovery",
                 "boss_site",
                 "pet_memorial",
                 "named_mob_memorial",
                 "raid_site",
                 "dimension_threshold" -> type.toLowerCase(Locale.ROOT);
            default -> "custom";
        };
    }

    private static String selectFlavorVariant(String variants, int seed) {
        int count = 1;
        for (int index = 0; index < variants.length(); index++) {
            if (variants.charAt(index) == '|') {
                count++;
            }
        }
        int target = Math.floorMod(seed, count);
        int start = 0;
        int current = 0;
        for (int index = 0; index <= variants.length(); index++) {
            if (index == variants.length() || variants.charAt(index) == '|') {
                if (current == target) {
                    String value = variants.substring(start, index).trim();
                    return value.isBlank() ? variants : value;
                }
                current++;
                start = index + 1;
            }
        }
        return variants;
    }

    private void drawMorePanelBackground(DrawContext context, int x, int y) {
        context.fill(x, y, x + 150, y + 62, 0xFFE9D0A0);
        context.fill(x + 1, y + 1, x + 149, y + 61, 0xFFF7E8BE);
        context.fill(x + 4, y + 4, x + 146, y + 5, SOFT_LINE);
        context.fill(x + 4, y + 57, x + 146, y + 58, SOFT_LINE);
    }

    private void drawDetail(DrawContext context, int x, int y, String key, String value) {
        String label = Text.translatable("living_legends.journal.details." + key).getString() + ":";
        drawText(context, Text.literal(label), x, y, TEXT_MUTED);
        drawText(context, Text.literal(ellipsize(value, 74)), x + 66, y, TEXT_DARK);
    }

    private void showCoordinates() {
        WorldJournalS2CPayload.Entry entry = selectedEntry();
        if (client == null || entry == null) {
            return;
        }
        client.inGameHud.getChatHud().addMessage(Text.literal(Text.translatable("living_legends.journal.title").getString()
                + ": " + displayName(entry) + " - " + coordinateString(entry)));
    }

    private void copyCoordinates() {
        WorldJournalS2CPayload.Entry entry = selectedEntry();
        if (client == null || entry == null) {
            return;
        }
        client.keyboard.setClipboard(coordinateString(entry));
        client.inGameHud.getChatHud().addMessage(Text.translatable("living_legends.journal.coordinates_copied"));
    }

    private String coordinateString(WorldJournalS2CPayload.Entry entry) {
        return entry.dimension() + " " + coordinatesText(entry);
    }

    void requestPage() {
        requestPage("");
    }

    void requestPage(String focusedPlaceId) {
        WorldJournalClient.send(WorldJournalC2SPayload.page(
                page,
                pageSize,
                tab,
                searchQuery(),
                typeFilter,
                dimensionFilter,
                sortMode,
                sortDirection,
                focusedPlaceId
        ));
    }

    WorldJournalC2SPayload actionPayload(WorldJournalC2SPayload.Action action, String placeId, String text) {
        return new WorldJournalC2SPayload(
                action,
                page,
                pageSize,
                tab,
                searchQuery(),
                typeFilter,
                dimensionFilter,
                sortMode,
                sortDirection,
                placeId,
                text
        );
    }

    void createCustomPlace(String name) {
        tab = "ALL_PLACES";
        page = 0;
        listScroll = 0;
        selectedPlaceId = "";
        typeFilter = "";
        dimensionFilter = "";
        sortMode = "NAME";
        sortDirection = "ASC";
        if (searchField != null) {
            searchField.setText("");
        }
        WorldJournalClient.send(new WorldJournalC2SPayload(
                WorldJournalC2SPayload.Action.CREATE_CUSTOM,
                page,
                pageSize,
                tab,
                "",
                "",
                "",
                sortMode,
                sortDirection,
                "",
                name
        ));
    }

    private String searchQuery() {
        return searchField == null ? "" : searchField.getText();
    }

    private WorldJournalS2CPayload.Entry selectedEntry() {
        for (WorldJournalS2CPayload.Entry entry : entries) {
            if (entry.placeId().equals(selectedPlaceId)) {
                return entry;
            }
        }
        return entries.isEmpty() ? null : entries.get(0);
    }

    private MapPlaceDescriptor mapDescriptor(WorldJournalS2CPayload.Entry entry) {
        return new MapPlaceDescriptor(
                entry.placeId(),
                displayName(entry),
                PlaceType.fromId(entry.placeType()),
                entry.dimension(),
                entry.centerX(),
                entry.centerY(),
                entry.centerZ(),
                entry.radius(),
                entry.manualName(),
                entry.manualNameText(),
                entry.nameRecipe(),
                entry.serverResolvedFallbackName(),
                ""
        );
    }

    private int totalPages() {
        int resolvedPageSize = Math.max(1, pageSize);
        return Math.max(1, (totalCount + resolvedPageSize - 1) / resolvedPageSize);
    }

    private int bookLeft() {
        return Math.max(4, (width - BOOK_WIDTH) / 2);
    }

    private int bookTop() {
        return Math.max(4, (height - BOOK_HEIGHT) / 2);
    }

    static String displayName(WorldJournalS2CPayload.Entry entry) {
        if (entry.manualName()) {
            return entry.manualNameText();
        }
        String resolved = NameResolver.resolve(entry.nameRecipe()).getString();
        return resolved == null || resolved.isBlank() || resolved.startsWith("living_legends.name.")
                ? entry.serverResolvedFallbackName()
                : resolved;
    }

    private static String placeTypeKey(String type) {
        return "living_legends.place_type." + (type == null || type.isBlank() ? "custom" : type);
    }

    private String filterSummary() {
        return typeFilter == null || typeFilter.isBlank()
                ? Text.translatable("living_legends.journal.filter.all").getString()
                : shortType(typeFilter);
    }

    private String dimensionSummary() {
        return dimensionFilter == null || dimensionFilter.isBlank()
                ? Text.translatable("living_legends.journal.filter.all").getString()
                : dimensionLabel(dimensionFilter);
    }

    private String sortSummary() {
        return sortLabel();
    }

    private String sortLabel() {
        return switch (sortMode) {
            case "DISTANCE" -> Text.translatable("living_legends.journal.sort.distance").getString();
            case "TYPE" -> Text.translatable("living_legends.journal.sort.type").getString();
            case "CREATED_TIME" -> Text.translatable("living_legends.journal.sort.created").getString();
            case "UPDATED_TIME" -> Text.translatable("living_legends.journal.sort.updated").getString();
            default -> Text.translatable("living_legends.journal.sort.name").getString();
        };
    }

    private static String shortType(String type) {
        if (type == null || type.isBlank()) {
            return "*";
        }
        return Text.translatable(placeTypeKey(type)).getString();
    }

    private static String next(String[] values, String current) {
        for (int index = 0; index < values.length; index++) {
            if (values[index].equalsIgnoreCase(current == null ? "" : current)) {
                return values[(index + 1) % values.length];
            }
        }
        return values.length == 0 ? "" : values[0];
    }

    private static String shortDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) {
            return "*";
        }
        int index = dimension.indexOf(':');
        return index >= 0 ? dimension.substring(index + 1) : dimension;
    }

    private static String dimensionLabel(String dimension) {
        if (dimension == null || dimension.isBlank()) {
            return Text.translatable("living_legends.journal.filter.all").getString();
        }
        return switch (dimension) {
            case "minecraft:overworld" -> Text.translatable("living_legends.journal.dimension.overworld").getString();
            case "minecraft:the_nether" -> Text.translatable("living_legends.journal.dimension.nether").getString();
            case "minecraft:the_end" -> Text.translatable("living_legends.journal.dimension.end").getString();
            default -> shortDimension(dimension);
        };
    }

    private static String coordinatesText(WorldJournalS2CPayload.Entry entry) {
        return entry.centerX() + ", " + entry.centerY() + ", " + entry.centerZ();
    }

    private static String distanceText(WorldJournalS2CPayload.Entry entry) {
        if (entry.distanceToPlayer() < 0) {
            return "-";
        }
        return Text.translatable(
                "living_legends.journal.distance.blocks",
                String.format(Locale.ROOT, "%.0f", entry.distanceToPlayer())
        ).getString();
    }

    private void drawText(DrawContext context, Text text, int x, int y, int color) {
        context.drawText(textRenderer, text, x, y, color, false);
    }

    private void drawCenteredText(DrawContext context, Text text, int centerX, int y, int color) {
        context.drawText(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, y, color, false);
    }

    private String ellipsize(String text, int maxWidth) {
        if (text == null || textRenderer.getWidth(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        String suffix = "...";
        int suffixWidth = textRenderer.getWidth(suffix);
        String result = text;
        while (!result.isEmpty() && textRenderer.getWidth(result) + suffixWidth > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + suffix;
    }

    private List<String> wrap(String text, int maxWidth, int maxLines) {
        String remaining = text == null ? "" : text.trim();
        List<String> lines = new ArrayList<>();
        while (!remaining.isBlank() && lines.size() < maxLines) {
            if (textRenderer.getWidth(remaining) <= maxWidth) {
                lines.add(remaining);
                break;
            }
            int split = remaining.length();
            while (split > 1 && textRenderer.getWidth(remaining.substring(0, split)) > maxWidth) {
                split--;
            }
            int space = remaining.lastIndexOf(' ', split);
            if (space > 4) {
                split = space;
            }
            String line = remaining.substring(0, split).trim();
            remaining = remaining.substring(split).trim();
            if (lines.size() == maxLines - 1 && !remaining.isBlank()) {
                line = ellipsize(line + " " + remaining, maxWidth);
                remaining = "";
            }
            lines.add(line);
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private static int typeColor(String typeId) {
        PlaceType type = PlaceType.fromId(typeId);
        return switch (type) {
            case DEATH_SITE -> 0xFF8F5037;
            case BATTLEFIELD, PVP_ARENA, RAID_SITE -> 0xFF9A3E2E;
            case SLAUGHTER_FIELD -> 0xFF8E6A35;
            case PORTAL_LANDMARK, DIMENSION_THRESHOLD -> 0xFF5E62A8;
            case FIRST_DISCOVERY, BOSS_SITE -> 0xFF9B792E;
            case SETTLEMENT -> 0xFF477B46;
            case GENERAL_LANDMARK -> 0xFF5A6D3C;
            default -> TEXT_DARK;
        };
    }

}

final class WorldJournalRenameScreen extends Screen {
    private final WorldJournalScreen parent;
    private final WorldJournalS2CPayload.Entry entry;
    private TextFieldWidget nameField;

    WorldJournalRenameScreen(WorldJournalScreen parent, WorldJournalS2CPayload.Entry entry) {
        super(Text.translatable("living_legends.journal.rename_title"));
        this.parent = parent;
        this.entry = entry;
    }

    @Override
    protected void init() {
        int x = width / 2 - 110;
        int y = height / 2 - 26;
        nameField = new TextFieldWidget(textRenderer, x, y, 220, 20, Text.translatable("living_legends.journal.rename_title"));
        nameField.setMaxLength(64);
        nameField.setText(entry.manualName() ? entry.manualNameText() : NameResolver.resolve(entry.nameRecipe()).getString());
        addDrawableChild(nameField);
        setInitialFocus(nameField);
        addDrawableChild(new JournalButtonWidget(x, y + 30, 104, 20, Text.translatable("living_legends.journal.button.confirm"), button -> confirm(), JournalButtonStyle.NORMAL));
        addDrawableChild(new JournalButtonWidget(x + 116, y + 30, 104, 20, Text.translatable("living_legends.journal.button.cancel"), button -> close(), JournalButtonStyle.NORMAL));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        WorldJournalScreen.drawCrispDimBackground(context, width, height);
        context.fill(width / 2 - 132, height / 2 - 58, width / 2 + 132, height / 2 + 58, 0xFFE8D4A5);
        context.fill(width / 2 - 126, height / 2 - 52, width / 2 + 126, height / 2 + 52, 0xFFF7E9C3);
        super.render(context, mouseX, mouseY, delta);
        context.drawText(textRenderer, title, width / 2 - textRenderer.getWidth(title) / 2, height / 2 - 46, 0xFF3D2815, false);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void confirm() {
        WorldJournalClient.send(parent.actionPayload(
                WorldJournalC2SPayload.Action.RENAME,
                entry.placeId(),
                nameField == null ? "" : nameField.getText()
        ));
        if (client != null) {
            client.setScreen(parent);
        }
    }
}

final class WorldJournalCreatePlaceScreen extends Screen {
    private final WorldJournalScreen parent;
    private TextFieldWidget nameField;

    WorldJournalCreatePlaceScreen(WorldJournalScreen parent) {
        super(Text.translatable("living_legends.journal.create_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 110;
        int y = height / 2 - 26;
        nameField = new TextFieldWidget(textRenderer, x, y, 220, 20, Text.translatable("living_legends.journal.create_title"));
        nameField.setMaxLength(64);
        addDrawableChild(nameField);
        setInitialFocus(nameField);
        addDrawableChild(new JournalButtonWidget(x, y + 30, 104, 20, Text.translatable("living_legends.journal.button.create_place"), button -> confirm(), JournalButtonStyle.NORMAL));
        addDrawableChild(new JournalButtonWidget(x + 116, y + 30, 104, 20, Text.translatable("living_legends.journal.button.cancel"), button -> close(), JournalButtonStyle.NORMAL));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        WorldJournalScreen.drawCrispDimBackground(context, width, height);
        context.fill(width / 2 - 132, height / 2 - 58, width / 2 + 132, height / 2 + 58, 0xFFE8D4A5);
        context.fill(width / 2 - 126, height / 2 - 52, width / 2 + 126, height / 2 + 52, 0xFFF7E9C3);
        super.render(context, mouseX, mouseY, delta);
        context.drawText(textRenderer, title, width / 2 - textRenderer.getWidth(title) / 2, height / 2 - 46, 0xFF3D2815, false);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void confirm() {
        parent.createCustomPlace(nameField == null ? "" : nameField.getText());
        if (client != null) {
            client.setScreen(parent);
        }
    }
}

final class WorldJournalConfirmDeleteScreen extends Screen {
    private final WorldJournalScreen parent;
    private final WorldJournalS2CPayload.Entry entry;

    WorldJournalConfirmDeleteScreen(WorldJournalScreen parent, WorldJournalS2CPayload.Entry entry) {
        super(Text.translatable("living_legends.journal.delete_confirm", WorldJournalScreen.displayName(entry)));
        this.parent = parent;
        this.entry = entry;
    }

    @Override
    protected void init() {
        int x = width / 2 - 108;
        int y = height / 2 + 12;
        addDrawableChild(new JournalButtonWidget(x, y, 100, 20, Text.translatable("living_legends.journal.button.delete"), button -> {
            WorldJournalClient.send(parent.actionPayload(WorldJournalC2SPayload.Action.DELETE, entry.placeId(), ""));
            close();
        }, JournalButtonStyle.DANGER));
        addDrawableChild(new JournalButtonWidget(x + 116, y, 100, 20, Text.translatable("living_legends.journal.button.cancel"), button -> close(), JournalButtonStyle.NORMAL));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        WorldJournalScreen.drawCrispDimBackground(context, width, height);
        context.fill(width / 2 - 142, height / 2 - 54, width / 2 + 142, height / 2 + 54, 0xFFE8D4A5);
        context.fill(width / 2 - 136, height / 2 - 48, width / 2 + 136, height / 2 + 48, 0xFFF7E9C3);
        super.render(context, mouseX, mouseY, delta);
        context.drawText(textRenderer, title, width / 2 - textRenderer.getWidth(title) / 2, height / 2 - 24, 0xFF7A2E21, false);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void close() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(parent);
        }
    }
}

enum JournalButtonStyle {
    NORMAL,
    SMALL,
    SEARCH,
    PAGE,
    TAB,
    ACTIVE_TAB,
    DANGER
}

final class JournalButtonWidget extends ButtonWidget {
    private static final int TEXT = 0xFF3D2815;
    private static final int TEXT_DISABLED = 0xFF9A8667;
    private static final int BORDER = 0xFF8F6A3E;
    private static final int BORDER_DANGER = 0xFF8A3A2C;
    private static final int FILL = 0xFFEED7A2;
    private static final int FILL_HOVER = 0xFFF8E8BE;
    private static final int FILL_DISABLED = 0xFFE2CDA1;
    private static final int FILL_SELECTED = 0xFFE0BC75;
    private static final int FILL_DANGER = 0xFFE4B39D;
    private final JournalButtonStyle style;

    JournalButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, JournalButtonStyle style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.style = style;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int border = style == JournalButtonStyle.DANGER ? BORDER_DANGER : BORDER;
        int fill = fillColor();
        int x = getX();
        int y = getY();
        int right = x + getWidth();
        int bottom = y + getHeight();
        context.fill(x, y, right, bottom, active ? border : 0xFFB59A73);
        context.fill(x + 1, y + 1, right - 1, bottom - 1, fill);
        if (style == JournalButtonStyle.ACTIVE_TAB) {
            context.fill(x + 2, bottom - 2, right - 2, bottom - 1, 0xFFA07133);
        } else if (isHovered() && active) {
            context.fill(x + 2, y + 2, right - 2, y + 3, 0x66FFFFFF);
        }
        if (style == JournalButtonStyle.SEARCH) {
            renderSearchIcon(context, x, y, getWidth(), getHeight(), active ? TEXT : TEXT_DISABLED);
            return;
        }
        String label = trimToWidth(getMessage().getString(), getWidth() - 8);
        int color = active ? TEXT : TEXT_DISABLED;
        var renderer = MinecraftClient.getInstance().textRenderer;
        context.drawText(
                renderer,
                Text.literal(label),
                x + (getWidth() - renderer.getWidth(label)) / 2,
                y + (getHeight() - 8) / 2,
                color,
                false
        );
    }

    private void renderSearchIcon(DrawContext context, int x, int y, int width, int height, int color) {
        int iconX = x + (width - 10) / 2;
        int iconY = y + (height - 10) / 2;
        context.fill(iconX + 2, iconY, iconX + 6, iconY + 1, color);
        context.fill(iconX, iconY + 2, iconX + 1, iconY + 6, color);
        context.fill(iconX + 7, iconY + 2, iconX + 8, iconY + 6, color);
        context.fill(iconX + 2, iconY + 7, iconX + 6, iconY + 8, color);
        context.fill(iconX + 6, iconY + 6, iconX + 8, iconY + 8, color);
        context.fill(iconX + 8, iconY + 8, iconX + 10, iconY + 10, color);
    }

    private int fillColor() {
        if (!active) {
            return FILL_DISABLED;
        }
        if (style == JournalButtonStyle.ACTIVE_TAB) {
            return FILL_SELECTED;
        }
        if (style == JournalButtonStyle.DANGER) {
            return isHovered() ? 0xFFF0C0A8 : FILL_DANGER;
        }
        if (style == JournalButtonStyle.TAB) {
            return isHovered() ? FILL_HOVER : 0xFFEAD0A0;
        }
        return isHovered() ? FILL_HOVER : FILL;
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        var renderer = MinecraftClient.getInstance().textRenderer;
        if (renderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = renderer.getWidth(suffix);
        String result = text;
        while (!result.isEmpty() && renderer.getWidth(result) + suffixWidth > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + suffix;
    }
}
