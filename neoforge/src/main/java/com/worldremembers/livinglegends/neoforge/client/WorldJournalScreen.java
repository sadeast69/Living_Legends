package com.worldremembers.livinglegends.neoforge.client;

import com.worldremembers.livinglegends.PlaceType;
import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import com.worldremembers.livinglegends.neoforge.network.WorldJournalC2SPayload;
import com.worldremembers.livinglegends.neoforge.network.WorldJournalS2CPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class WorldJournalScreen extends Screen {
    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            WorldRemembersLivingLegends.MOD_ID,
            "textures/gui/world_journal_book.png"
    );
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
    private static final int DETAIL_WIDTH = 142;
    private static final int DETAIL_TITLE_WIDTH = 120;
    private static final int DETAIL_TITLE_MAX_LINES = 2;
    private static final int DETAIL_LINE_HEIGHT = 12;
    private static final int DETAIL_FONT_HEIGHT = 9;
    private static final int DETAIL_BODY_TOP_GAP = 2;
    private static final int DETAIL_ACTION_BUTTON_Y_OFFSET = 170;
    private static final int DETAIL_ACTION_BUTTON_HEIGHT = 17;
    private static final int DETAIL_BODY_BUTTON_GAP = 4;
    private static final int DETAIL_BOTTOM_BUTTON_Y_OFFSET = 224;
    private static final int DETAIL_BOTTOM_BUTTON_GAP = 8;
    private static final int DETAIL_BODY_MAX_LINES = 2;
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
    private EditBox searchField;
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
    private String statusMessage = "";

    WorldJournalScreen(Screen parent) {
        super(Component.translatable("living_legends.journal.title"));
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
        if (payload.action() == WorldJournalS2CPayload.Action.ERROR) {
            this.statusMessage = messageText(payload);
            rebuild();
            return;
        }
        if (!payload.messageText().isBlank() || !payload.messageKey().isBlank()) {
            this.statusMessage = messageText(payload);
        } else {
            this.statusMessage = "";
        }
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
        } else if (payload.action() == WorldJournalS2CPayload.Action.OPEN) {
            rebuild();
        }
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        drawCrispDimBackground(graphics, width, height);
        int left = bookLeft();
        int top = bookTop();
        drawBook(graphics, left, top);
        drawLeftPage(graphics, left, top);
        drawRightPage(graphics, left, top);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // The journal draws its own crisp dimmed background and book texture.
    }

    static void drawCrispDimBackground(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, 0x8A000000);
    }

    private void rebuild() {
        if (minecraft == null || font == null) {
            return;
        }
        clearWidgets();
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
        addRenderableWidget(new JournalButton(left + 28, y, 50, 16, Component.translatable("living_legends.journal.tab.places"), button -> {
            tab = "ALL_PLACES";
            sortMode = "NAME";
            moreOpen = false;
            page = 0;
            requestPage();
        }, "ALL_PLACES".equals(tab) ? JournalButtonStyle.ACTIVE_TAB : JournalButtonStyle.TAB));
        addRenderableWidget(new JournalButton(left + 80, y, 50, 16, Component.translatable("living_legends.journal.tab.nearby"), button -> {
            tab = "NEARBY";
            sortMode = "DISTANCE";
            moreOpen = false;
            page = 0;
            requestPage();
        }, "NEARBY".equals(tab) ? JournalButtonStyle.ACTIVE_TAB : JournalButtonStyle.TAB));
        addRenderableWidget(new JournalButton(left + 132, y, 58, 16, Component.translatable("living_legends.journal.tab.settings"), button -> {
            tab = "SETTINGS";
            moreOpen = false;
            rebuild();
        }, "SETTINGS".equals(tab) ? JournalButtonStyle.ACTIVE_TAB : JournalButtonStyle.TAB));
    }

    private void addListControls(int left, int top) {
        searchField = new EditBox(
                font,
                left + 30,
                top + 52,
                104,
                16,
                Component.translatable("living_legends.journal.search")
        );
        searchField.setMaxLength(64);
        searchField.setValue(searchQuery());
        searchField.setBordered(false);
        addRenderableWidget(searchField);
        addRenderableWidget(new JournalButton(left + 140, top + 52, 28, 16, Component.translatable("living_legends.journal.button.search"), button -> {
            page = 0;
            listScroll = 0;
            requestPage();
        }, JournalButtonStyle.SEARCH));
        addRenderableWidget(new JournalButton(left + 30, top + 73, 48, 14, Component.literal(ellipsize(filterSummary(), 42)), button -> {
            typeFilter = next(TYPE_FILTERS, typeFilter);
            page = 0;
            listScroll = 0;
            requestPage();
        }, JournalButtonStyle.SMALL));
        addRenderableWidget(new JournalButton(left + 82, top + 73, 42, 14, Component.literal(ellipsize(dimensionSummary(), 36)), button -> {
            dimensionFilter = next(DIMENSION_FILTERS, dimensionFilter);
            page = 0;
            listScroll = 0;
            requestPage();
        }, JournalButtonStyle.SMALL));
        addRenderableWidget(new JournalButton(left + 128, top + 73, 48, 14, Component.literal(ellipsize(sortSummary(), 42)), button -> {
            sortMode = next(SORT_MODES, sortMode);
            page = 0;
            listScroll = 0;
            requestPage();
        }, JournalButtonStyle.SMALL));

        JournalButton previous = addRenderableWidget(new JournalButton(left + 34, top + 224, 22, 15, Component.literal("<"), button -> {
            if (page > 0) {
                page--;
                listScroll = 0;
                requestPage();
            }
        }, JournalButtonStyle.PAGE));
        previous.active = page > 0;
        JournalButton next = addRenderableWidget(new JournalButton(left + 154, top + 224, 22, 15, Component.literal(">"), button -> {
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
        int y = detailActionY(top, selected);
        JournalButton coordinates = addRenderableWidget(new JournalButton(x, y, 82, 17, Component.translatable("living_legends.journal.button.show_coordinates"), button -> showCoordinates(), JournalButtonStyle.NORMAL));
        coordinates.active = selected != null && showExactCoordinates;
        JournalButton more = addRenderableWidget(new JournalButton(x + 88, y, 54, 17, Component.translatable("living_legends.journal.button.more"), button -> {
            moreOpen = !moreOpen;
            rebuild();
        }, moreOpen ? JournalButtonStyle.ACTIVE_TAB : JournalButtonStyle.NORMAL));
        more.active = selected != null;
        if (moreOpen && selected != null) {
            int menuY = top + 108;
            JournalButton copy = addRenderableWidget(new JournalButton(x, menuY, 68, 15, Component.translatable("living_legends.journal.button.copy_coordinates"), button -> copyCoordinates(), JournalButtonStyle.SMALL));
            copy.active = showExactCoordinates;
            JournalButton rename = addRenderableWidget(new JournalButton(x + 74, menuY, 68, 15, Component.translatable("living_legends.journal.button.rename"), button -> {
                if (minecraft != null) {
                    minecraft.setScreen(new WorldJournalRenameScreen(this, selected));
                }
            }, JournalButtonStyle.SMALL));
            rename.active = canManage;
            menuY += 18;
            JournalButton restore = addRenderableWidget(new JournalButton(x, menuY, 68, 15, Component.translatable("living_legends.journal.button.restore_generated"), button -> {
                WorldJournalClient.send(actionPayload(WorldJournalC2SPayload.Action.RESTORE_GENERATED, selected.placeId(), ""));
            }, JournalButtonStyle.SMALL));
            restore.active = canManage && selected.manualName();
            JournalButton teleport = addRenderableWidget(new JournalButton(x + 74, menuY, 68, 15, Component.translatable("living_legends.journal.button.teleport"), button -> {
                WorldJournalClient.send(WorldJournalC2SPayload.action(WorldJournalC2SPayload.Action.TELEPORT, selected.placeId(), ""));
            }, JournalButtonStyle.SMALL));
            teleport.active = canTeleport;
            menuY += 18;
            JournalButton delete = addRenderableWidget(new JournalButton(x, menuY, 142, 15, Component.translatable("living_legends.journal.button.delete"), button -> {
                if (minecraft != null) {
                    minecraft.setScreen(new WorldJournalConfirmDeleteScreen(this, selected));
                }
            }, JournalButtonStyle.DANGER));
            delete.active = canManage;
        }
        JournalButton create = addRenderableWidget(new JournalButton(left + BOOK_WIDTH - 148, top + 224, 64, 16, Component.translatable("living_legends.journal.button.create_place"), button -> {
            if (minecraft != null) {
                minecraft.setScreen(new WorldJournalCreatePlaceScreen(this));
            }
        }, JournalButtonStyle.NORMAL));
        create.active = canManage;
        addRenderableWidget(new JournalButton(left + BOOK_WIDTH - 78, top + 224, 58, 16, Component.translatable("gui.done"), button -> onClose(), JournalButtonStyle.NORMAL));
    }

    private void addSettingsControls(int left, int top) {
        addRenderableWidget(new JournalButton(left + 226, top + 172, 132, 18, Component.translatable("living_legends.journal.button.open_title_overlay_settings"), button -> {
            if (minecraft != null) {
                minecraft.setScreen(new PlaceTitleOverlayConfigScreen(this));
            }
        }, JournalButtonStyle.NORMAL));
        addRenderableWidget(new JournalButton(left + BOOK_WIDTH - 78, top + 224, 58, 16, Component.translatable("gui.done"), button -> onClose(), JournalButtonStyle.NORMAL));
    }

    private void drawBook(GuiGraphics graphics, int left, int top) {
        graphics.blit(
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

    private void drawLeftPage(GuiGraphics graphics, int left, int top) {
        if ("SETTINGS".equals(tab)) {
            return;
        }

        int searchX = left + 30;
        int searchY = top + 52;
        graphics.fill(searchX - 1, searchY - 1, searchX + 105, searchY + 17, 0xCCB88E55);
        graphics.fill(searchX, searchY, searchX + 104, searchY + 16, 0xCCF9EDCB);

        int listX = left + 32;
        int listY = top + 100;
        if (entries.isEmpty()) {
            drawText(graphics, Component.translatable(lastDeletedPlaceId.isBlank()
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
                graphics.fill(listX - 4, y - 3, listX + PAGE_WIDTH - 18, y + LIST_ROW_HEIGHT - 4, SELECTED);
                graphics.fill(listX - 4, y - 3, listX + PAGE_WIDTH - 18, y - 2, 0x44B88E55);
            }
            graphics.fill(listX, y + 4, listX + 3, y + LIST_ROW_HEIGHT - 8, typeColor(entry.placeType()));
            drawText(graphics, Component.literal(ellipsize(displayName(entry), PAGE_WIDTH - 52)), listX + 8, y + 2, TEXT_DARK);
            String subtitle = Component.translatable(placeTypeKey(entry.placeType())).getString();
            if ("NEARBY".equals(tab) && entry.distanceToPlayer() >= 0.0) {
                subtitle = subtitle + " - " + distanceText(entry);
            }
            drawText(graphics, Component.literal(ellipsize(subtitle, PAGE_WIDTH - 52)), listX + 8, y + 14, TEXT_SOFT);
        }
        String pageText = Component.translatable("living_legends.journal.page").getString()
                + " " + (page + 1) + "/" + totalPages();
        drawCenteredText(graphics, Component.literal(pageText), left + 105, top + 227, TEXT_MUTED);
    }

    private void drawRightPage(GuiGraphics graphics, int left, int top) {
        int x = left + 232;
        int y = top + 62;
        if ("SETTINGS".equals(tab)) {
            drawText(graphics, Component.translatable("living_legends.journal.settings_title"), x, y, TEXT_DARK);
            graphics.fill(x, y + 15, x + 142, y + 16, SOFT_LINE);
            y += 28;
            for (String line : wrap(Component.translatable("living_legends.journal.settings_overlay").getString(), 142, 2)) {
                drawText(graphics, Component.literal(line), x, y, TEXT_MUTED);
                y += 12;
            }
            y += 8;
            for (String line : wrap(Component.translatable("living_legends.journal.settings_hint").getString(), 142, 3)) {
                drawText(graphics, Component.literal(line), x, y, TEXT_SOFT);
                y += 12;
            }
            return;
        }

        WorldJournalS2CPayload.Entry entry = selectedEntry();
        if (entry == null) {
            for (String line : wrap(Component.translatable("living_legends.journal.no_selected_place").getString(), 132, 3)) {
                drawText(graphics, Component.literal(line), x, y, TEXT_MUTED);
                y += 12;
            }
            return;
        }

        List<String> titleLines = wrap(displayName(entry), DETAIL_TITLE_WIDTH, DETAIL_TITLE_MAX_LINES);
        int titleColor = typeColor(entry.placeType());
        for (String line : titleLines) {
            drawText(graphics, Component.literal(line), x, y, titleColor);
            y += DETAIL_LINE_HEIGHT;
        }

        int separatorY = y + 5;
        graphics.fill(x, separatorY, x + DETAIL_WIDTH, separatorY + 1, LINE);
        graphics.fill(x + 18, separatorY + 3, x + 124, separatorY + 4, SOFT_LINE);
        y = separatorY + 11;

        drawText(graphics, Component.translatable(placeTypeKey(entry.placeType())), x, y, TEXT_MUTED);
        y += 18;
        drawDetail(graphics, x, y, "dimension", dimensionLabel(entry.dimension()));
        y += 14;
        if (showExactCoordinates) {
            drawDetail(graphics, x, y, "coordinates", coordinatesText(entry));
            y += 14;
        }
        if (entry.distanceToPlayer() >= 0.0) {
            drawDetail(graphics, x, y, "distance", distanceText(entry));
            y += 14;
        }

        int bodyY = y + DETAIL_BODY_TOP_GAP;
        int bodyBottom = detailActionY(top, entry) - DETAIL_BODY_BUTTON_GAP;
        int bodyLines = Math.min(DETAIL_BODY_MAX_LINES, fittingBodyLines(bodyY, bodyBottom));
        if (moreOpen) {
            drawMorePanelBackground(graphics, x - 4, top + 102);
        } else if (bodyLines > 0 && statusMessage != null && !statusMessage.isBlank()) {
            drawWrapped(graphics, statusMessage, x, bodyY, DETAIL_WIDTH, bodyLines, TEXT_SOFT);
        } else if (bodyLines > 0) {
            drawFlavor(graphics, entry, x, bodyY, bodyLines);
        }
    }

    private int detailActionY(int top, WorldJournalS2CPayload.Entry entry) {
        if (entry == null) {
            return top + DETAIL_ACTION_BUTTON_Y_OFFSET;
        }
        int bodyY = detailBodyY(top, entry);
        int preferredY = bodyY + DETAIL_BODY_MAX_LINES * DETAIL_LINE_HEIGHT + DETAIL_BODY_BUTTON_GAP;
        int maxY = top + DETAIL_BOTTOM_BUTTON_Y_OFFSET - DETAIL_ACTION_BUTTON_HEIGHT - DETAIL_BOTTOM_BUTTON_GAP;
        return Math.max(top + DETAIL_ACTION_BUTTON_Y_OFFSET, Math.min(maxY, preferredY));
    }

    private int detailBodyY(int top, WorldJournalS2CPayload.Entry entry) {
        int y = top + 62;
        y += wrap(displayName(entry), DETAIL_TITLE_WIDTH, DETAIL_TITLE_MAX_LINES).size() * DETAIL_LINE_HEIGHT;
        int separatorY = y + 5;
        y = separatorY + 11;
        y += 18;
        y += 14;
        if (showExactCoordinates) {
            y += 14;
        }
        if (entry.distanceToPlayer() >= 0.0) {
            y += 14;
        }
        return y + DETAIL_BODY_TOP_GAP;
    }

    private static int fittingBodyLines(int y, int bottom) {
        if (y + DETAIL_FONT_HEIGHT > bottom) {
            return 0;
        }
        return 1 + Math.max(0, bottom - y - DETAIL_FONT_HEIGHT) / DETAIL_LINE_HEIGHT;
    }

    private void drawFlavor(GuiGraphics graphics, WorldJournalS2CPayload.Entry entry, int x, int y, int maxLines) {
        for (String line : wrap(journalFlavor(entry), DETAIL_WIDTH, maxLines)) {
            drawText(graphics, Component.literal(line), x, y, TEXT_SOFT);
            y += DETAIL_LINE_HEIGHT;
        }
    }

    private String journalFlavor(WorldJournalS2CPayload.Entry entry) {
        String type = journalFlavorType(entry.placeType());
        String key = "living_legends.journal.flavor." + type;
        String variants = Component.translatable(key).getString();
        if (variants == null || variants.isBlank() || variants.equals(key)) {
            variants = Component.translatable("living_legends.journal.flavor.remembered").getString();
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

    private void drawMorePanelBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 150, y + 62, 0xFFE9D0A0);
        graphics.fill(x + 1, y + 1, x + 149, y + 61, 0xFFF7E8BE);
        graphics.fill(x + 4, y + 4, x + 146, y + 5, SOFT_LINE);
        graphics.fill(x + 4, y + 57, x + 146, y + 58, SOFT_LINE);
    }

    private void showCoordinates() {
        WorldJournalS2CPayload.Entry entry = selectedEntry();
        if (minecraft == null || entry == null) {
            return;
        }
        minecraft.gui.getChat().addMessage(Component.literal(title.getString()
                + ": " + displayName(entry) + " - " + coordinateString(entry)));
    }

    private void copyCoordinates() {
        WorldJournalS2CPayload.Entry entry = selectedEntry();
        if (minecraft == null || entry == null) {
            return;
        }
        minecraft.keyboardHandler.setClipboard(coordinateString(entry));
        minecraft.gui.getChat().addMessage(Component.translatable("living_legends.journal.coordinates_copied"));
    }

    private String coordinateString(WorldJournalS2CPayload.Entry entry) {
        return entry.dimension() + " " + coordinatesText(entry);
    }

    private void requestPage() {
        requestPage("");
    }

    private void requestPage(String focusedPlaceId) {
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

    private WorldJournalC2SPayload actionPayload(WorldJournalC2SPayload.Action action, String placeId, String text) {
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

    private void createCustomPlace(String name) {
        tab = "ALL_PLACES";
        page = 0;
        listScroll = 0;
        selectedPlaceId = "";
        typeFilter = "";
        dimensionFilter = "";
        sortMode = "NAME";
        sortDirection = "ASC";
        if (searchField != null) {
            searchField.setValue("");
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

    private void refresh() {
        WorldJournalClient.send(new WorldJournalC2SPayload(
                WorldJournalC2SPayload.Action.REFRESH,
                page,
                pageSize,
                tab,
                searchQuery(),
                typeFilter,
                dimensionFilter,
                sortMode,
                sortDirection,
                selectedPlaceId,
                ""
        ));
    }

    private String searchQuery() {
        return searchField == null ? "" : searchField.getValue();
    }

    private WorldJournalS2CPayload.Entry selectedEntry() {
        for (WorldJournalS2CPayload.Entry entry : entries) {
            if (entry.placeId().equals(selectedPlaceId)) {
                return entry;
            }
        }
        return entries.isEmpty() ? null : entries.get(0);
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

    private static String displayName(WorldJournalS2CPayload.Entry entry) {
        if (entry.manualName()) {
            return entry.manualNameText();
        }
        String resolved = WorldRemembersLivingLegendsNeoForgeClientNameResolver.resolve(entry.nameRecipe()).getString();
        if (resolved == null || resolved.isBlank() || resolved.startsWith("living_legends.name.")) {
            return entry.serverResolvedFallbackName().isBlank() ? entry.placeId() : entry.serverResolvedFallbackName();
        }
        return resolved;
    }

    private static String placeTypeKey(String type) {
        return "living_legends.place_type." + (type == null || type.isBlank() ? "custom" : type);
    }

    private String filterSummary() {
        return typeFilter == null || typeFilter.isBlank()
                ? Component.translatable("living_legends.journal.filter.all").getString()
                : shortType(typeFilter);
    }

    private String dimensionSummary() {
        return dimensionFilter == null || dimensionFilter.isBlank()
                ? Component.translatable("living_legends.journal.filter.all").getString()
                : dimensionLabel(dimensionFilter);
    }

    private String sortSummary() {
        return switch (sortMode) {
            case "DISTANCE" -> Component.translatable("living_legends.journal.sort.distance").getString();
            case "TYPE" -> Component.translatable("living_legends.journal.sort.type").getString();
            case "CREATED_TIME" -> Component.translatable("living_legends.journal.sort.created").getString();
            case "UPDATED_TIME" -> Component.translatable("living_legends.journal.sort.updated").getString();
            default -> Component.translatable("living_legends.journal.sort.name").getString();
        };
    }

    private static String shortType(String type) {
        if (type == null || type.isBlank()) {
            return "*";
        }
        return Component.translatable(placeTypeKey(type)).getString();
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
            return Component.translatable("living_legends.journal.filter.all").getString();
        }
        return switch (dimension) {
            case "minecraft:overworld" -> Component.translatable("living_legends.journal.dimension.overworld").getString();
            case "minecraft:the_nether" -> Component.translatable("living_legends.journal.dimension.nether").getString();
            case "minecraft:the_end" -> Component.translatable("living_legends.journal.dimension.end").getString();
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
        return Component.translatable(
                "living_legends.journal.distance.blocks",
                String.format(Locale.ROOT, "%.0f", entry.distanceToPlayer())
        ).getString();
    }

    private static String messageText(WorldJournalS2CPayload payload) {
        if (payload.messageKey() != null && !payload.messageKey().isBlank()) {
            String translated = Component.translatable(payload.messageKey()).getString();
            if (!translated.equals(payload.messageKey())) {
                return translated;
            }
        }
        return payload.messageText() == null ? "" : payload.messageText();
    }

    private void drawDetail(GuiGraphics graphics, int x, int y, String key, String value) {
        String label = Component.translatable("living_legends.journal.details." + key).getString() + ":";
        drawText(graphics, Component.literal(label), x, y, TEXT_MUTED);
        drawText(graphics, Component.literal(ellipsize(value, 74)), x + 66, y, TEXT_DARK);
    }

    private void drawWrapped(GuiGraphics graphics, String text, int x, int y, int maxWidth, int maxLines, int color) {
        for (String line : wrap(text, maxWidth, maxLines)) {
            drawText(graphics, Component.literal(line), x, y, color);
            y += DETAIL_LINE_HEIGHT;
        }
    }

    private void drawText(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, false);
    }

    private void drawCenteredText(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private String ellipsize(String text, int maxWidth) {
        if (text == null || font.width(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        String suffix = "...";
        int suffixWidth = font.width(suffix);
        String result = text;
        while (!result.isEmpty() && font.width(result) + suffixWidth > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + suffix;
    }

    private List<String> wrap(String text, int maxWidth, int maxLines) {
        String remaining = text == null ? "" : text.trim();
        List<String> lines = new ArrayList<>();
        while (!remaining.isBlank() && lines.size() < maxLines) {
            if (font.width(remaining) <= maxWidth) {
                lines.add(remaining);
                break;
            }
            int split = remaining.length();
            while (split > 1 && font.width(remaining.substring(0, split)) > maxWidth) {
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

    private static final class WorldJournalRenameScreen extends Screen {
        private final WorldJournalScreen parent;
        private final WorldJournalS2CPayload.Entry entry;
        private EditBox nameField;

        private WorldJournalRenameScreen(WorldJournalScreen parent, WorldJournalS2CPayload.Entry entry) {
            super(Component.translatable("living_legends.journal.rename_title"));
            this.parent = parent;
            this.entry = entry;
        }

        @Override
        protected void init() {
            int x = width / 2 - 110;
            int y = height / 2 - 26;
            nameField = new EditBox(font, x, y, 220, 20, Component.translatable("living_legends.journal.rename_title"));
            nameField.setMaxLength(64);
            nameField.setValue(entry.manualName()
                    ? entry.manualNameText()
                    : WorldRemembersLivingLegendsNeoForgeClientNameResolver.resolve(entry.nameRecipe()).getString());
            addRenderableWidget(nameField);
            setInitialFocus(nameField);
            addRenderableWidget(new JournalButton(x, y + 30, 104, 20, Component.translatable("living_legends.journal.button.confirm"), button -> confirm(), JournalButtonStyle.NORMAL));
            addRenderableWidget(new JournalButton(x + 116, y + 30, 104, 20, Component.translatable("living_legends.journal.button.cancel"), button -> onClose(), JournalButtonStyle.NORMAL));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            drawCrispDimBackground(graphics, width, height);
            graphics.fill(width / 2 - 132, height / 2 - 58, width / 2 + 132, height / 2 + 58, 0xFFE8D4A5);
            graphics.fill(width / 2 - 126, height / 2 - 52, width / 2 + 126, height / 2 + 52, 0xFFF7E9C3);
            super.render(graphics, mouseX, mouseY, delta);
            graphics.drawString(font, title, width / 2 - font.width(title) / 2, height / 2 - 46, 0xFF3D2815, false);
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        }

        @Override
        public void onClose() {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }

        private void confirm() {
            WorldJournalClient.send(parent.actionPayload(
                    WorldJournalC2SPayload.Action.RENAME,
                    entry.placeId(),
                    nameField == null ? "" : nameField.getValue()
            ));
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }
    }

    private static final class WorldJournalCreatePlaceScreen extends Screen {
        private final WorldJournalScreen parent;
        private EditBox nameField;

        private WorldJournalCreatePlaceScreen(WorldJournalScreen parent) {
            super(Component.translatable("living_legends.journal.create_title"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int x = width / 2 - 110;
            int y = height / 2 - 26;
            nameField = new EditBox(font, x, y, 220, 20, Component.translatable("living_legends.journal.create_title"));
            nameField.setMaxLength(64);
            addRenderableWidget(nameField);
            setInitialFocus(nameField);
            addRenderableWidget(new JournalButton(x, y + 30, 104, 20, Component.translatable("living_legends.journal.button.create_place"), button -> confirm(), JournalButtonStyle.NORMAL));
            addRenderableWidget(new JournalButton(x + 116, y + 30, 104, 20, Component.translatable("living_legends.journal.button.cancel"), button -> onClose(), JournalButtonStyle.NORMAL));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            drawCrispDimBackground(graphics, width, height);
            graphics.fill(width / 2 - 132, height / 2 - 58, width / 2 + 132, height / 2 + 58, 0xFFE8D4A5);
            graphics.fill(width / 2 - 126, height / 2 - 52, width / 2 + 126, height / 2 + 52, 0xFFF7E9C3);
            super.render(graphics, mouseX, mouseY, delta);
            graphics.drawString(font, title, width / 2 - font.width(title) / 2, height / 2 - 46, 0xFF3D2815, false);
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        }

        @Override
        public void onClose() {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }

        private void confirm() {
            parent.createCustomPlace(nameField == null ? "" : nameField.getValue());
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }
    }

    private static final class WorldJournalConfirmDeleteScreen extends Screen {
        private final WorldJournalScreen parent;
        private final WorldJournalS2CPayload.Entry entry;

        private WorldJournalConfirmDeleteScreen(WorldJournalScreen parent, WorldJournalS2CPayload.Entry entry) {
            super(Component.translatable("living_legends.journal.delete_confirm", WorldJournalScreen.displayName(entry)));
            this.parent = parent;
            this.entry = entry;
        }

        @Override
        protected void init() {
            int x = width / 2 - 108;
            int y = height / 2 + 12;
            addRenderableWidget(new JournalButton(x, y, 100, 20, Component.translatable("living_legends.journal.button.delete"), button -> {
                WorldJournalClient.send(parent.actionPayload(WorldJournalC2SPayload.Action.DELETE, entry.placeId(), ""));
                onClose();
            }, JournalButtonStyle.DANGER));
            addRenderableWidget(new JournalButton(x + 116, y, 100, 20, Component.translatable("living_legends.journal.button.cancel"), button -> onClose(), JournalButtonStyle.NORMAL));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            drawCrispDimBackground(graphics, width, height);
            graphics.fill(width / 2 - 142, height / 2 - 54, width / 2 + 142, height / 2 + 54, 0xFFE8D4A5);
            graphics.fill(width / 2 - 136, height / 2 - 48, width / 2 + 136, height / 2 + 48, 0xFFF7E9C3);
            super.render(graphics, mouseX, mouseY, delta);
            graphics.drawString(font, title, width / 2 - font.width(title) / 2, height / 2 - 24, 0xFF7A2E21, false);
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        }

        @Override
        public void onClose() {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }
    }

    private enum JournalButtonStyle {
        NORMAL,
        SMALL,
        SEARCH,
        PAGE,
        TAB,
        ACTIVE_TAB,
        DANGER
    }

    private static final class JournalButton extends Button {
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

        JournalButton(int x, int y, int width, int height, Component message, OnPress onPress, JournalButtonStyle style) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.style = style;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int border = style == JournalButtonStyle.DANGER ? BORDER_DANGER : BORDER;
            int fill = fillColor();
            int x = getX();
            int y = getY();
            int right = x + getWidth();
            int bottom = y + getHeight();
            graphics.fill(x, y, right, bottom, active ? border : 0xFFB59A73);
            graphics.fill(x + 1, y + 1, right - 1, bottom - 1, fill);
            if (style == JournalButtonStyle.ACTIVE_TAB) {
                graphics.fill(x + 2, bottom - 2, right - 2, bottom - 1, 0xFFA07133);
            } else if (isHovered() && active) {
                graphics.fill(x + 2, y + 2, right - 2, y + 3, 0x66FFFFFF);
            }
            if (style == JournalButtonStyle.SEARCH) {
                renderSearchIcon(graphics, x, y, getWidth(), getHeight(), active ? TEXT : TEXT_DISABLED);
                return;
            }
            String label = trimToWidth(getMessage().getString(), getWidth() - 8);
            int color = active ? TEXT : TEXT_DISABLED;
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                graphics.drawString(
                        client.font,
                        Component.literal(label),
                        x + (getWidth() - client.font.width(label)) / 2,
                        y + (getHeight() - 8) / 2,
                        color,
                        false
                );
            }
        }

        private void renderSearchIcon(GuiGraphics graphics, int x, int y, int width, int height, int color) {
            int iconX = x + (width - 10) / 2;
            int iconY = y + (height - 10) / 2;
            graphics.fill(iconX + 2, iconY, iconX + 6, iconY + 1, color);
            graphics.fill(iconX, iconY + 2, iconX + 1, iconY + 6, color);
            graphics.fill(iconX + 7, iconY + 2, iconX + 8, iconY + 6, color);
            graphics.fill(iconX + 2, iconY + 7, iconX + 6, iconY + 8, color);
            graphics.fill(iconX + 6, iconY + 6, iconX + 8, iconY + 8, color);
            graphics.fill(iconX + 8, iconY + 8, iconX + 10, iconY + 10, color);
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
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.font.width(text) <= maxWidth) {
                return text;
            }
            String suffix = "...";
            int suffixWidth = client.font.width(suffix);
            String result = text;
            while (!result.isEmpty() && client.font.width(result) + suffixWidth > maxWidth) {
                result = result.substring(0, result.length() - 1);
            }
            return result + suffix;
        }
    }
}
