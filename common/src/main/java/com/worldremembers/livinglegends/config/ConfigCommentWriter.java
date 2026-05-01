package com.worldremembers.livinglegends.config;

import java.util.LinkedHashMap;
import java.util.Map;

final class ConfigCommentWriter {
    private static final Map<String, String> COMMENTS = comments();

    private ConfigCommentWriter() {
    }

    static String write(LivingLegendsConfig config) {
        StringBuilder builder = new StringBuilder();
        builder.append("// World Remembers: Living Legends config.\n");
        writeValue(builder, config.toMap(), "", 0);
        builder.append('\n');
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder builder, Object value, String path, int indent) {
        if (value instanceof Map<?, ?> map) {
            builder.append('{');
            if (!map.isEmpty()) {
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) {
                        builder.append(',');
                    }
                    String key = String.valueOf(entry.getKey());
                    String childPath = path.isBlank() ? key : path + "." + key;
                    builder.append('\n');
                    writeComment(builder, childPath, indent + 2);
                    indent(builder, indent + 2);
                    writeString(builder, key);
                    builder.append(": ");
                    writeValue(builder, entry.getValue(), childPath, indent + 2);
                    first = false;
                }
                builder.append('\n');
                indent(builder, indent);
            }
            builder.append('}');
        } else if (value instanceof Iterable<?> iterable) {
            builder.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                builder.append('\n');
                indent(builder, indent + 2);
                writeValue(builder, item, path, indent + 2);
                first = false;
            }
            if (!first) {
                builder.append('\n');
                indent(builder, indent);
            }
            builder.append(']');
        } else if (value instanceof String string) {
            writeString(builder, string);
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value == null) {
            builder.append("null");
        } else {
            writeString(builder, String.valueOf(value));
        }
    }

    private static void writeComment(StringBuilder builder, String path, int indent) {
        String comment = commentFor(path);
        if (comment.isBlank()) {
            return;
        }
        for (String line : comment.split("\\R")) {
            indent(builder, indent);
            builder.append("// ").append(line).append('\n');
        }
    }

    private static String commentFor(String path) {
        String exact = COMMENTS.get(path);
        if (exact != null) {
            return exact;
        }
        if (path.startsWith("placeTypes.autoGeneration.")) {
            String type = path.substring("placeTypes.autoGeneration.".length());
            String typeComment = placeTypeComment(type);
            if (!typeComment.isBlank()) {
                return typeComment;
            }
        }
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0) {
            String wildcard = path.substring(0, lastDot) + ".*";
            String comment = COMMENTS.get(wildcard);
            if (comment != null) {
                return comment;
            }
        }
        return "Config value: " + path + ".";
    }

    private static void writeString(StringBuilder builder, String value) {
        builder.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }
        builder.append('"');
    }

    private static void indent(StringBuilder builder, int indent) {
        builder.append(" ".repeat(Math.max(0, indent)));
    }

    private static String placeTypeComment(String type) {
        return switch (type) {
            case "DEATH_SITE" -> "Places created from repeated player deaths. Environment decides surface/cave/water/mountain naming.";
            case "BATTLEFIELD" -> "Places created from combat with hostile or neutral mobs.";
            case "SLAUGHTER_FIELD" -> "Places created from repeated passive mob kills. Anti-farm checks still apply.";
            case "PVP_ARENA" -> "Places created from repeated PvP deaths/kills. Mostly useful on servers.";
            case "MINING_SITE" -> "Places created from valuable block mining, such as diamond ore or ancient debris.";
            case "PORTAL_LANDMARK" -> "Places created from frequent portal or dimension travel.";
            case "GENERAL_LANDMARK" -> "Fallback landmarks created from repeated visits or mixed low-specificity activity.";
            case "FIRST_DISCOVERY" -> "World-first discoveries, such as first Nether entry, first diamond, first stronghold.";
            case "BOSS_SITE" -> "Places created from boss or boss-like mob kills.";
            case "PET_MEMORIAL" -> "Places created from pet deaths. Rare/sentimental place type.";
            case "NAMED_MOB_MEMORIAL" -> "Places created from deaths of custom-named mobs.";
            case "RAID_SITE" -> "Places created from raid victories or raid-related events.";
            case "DIMENSION_THRESHOLD" -> "Places created from major dimension transition milestones.";
            case "SETTLEMENT" -> "Places created from base-like activity such as building and respawn points.";
            case "CUSTOM" -> "Manual/custom places created by commands or external integrations.";
            default -> "";
        };
    }

    private static Map<String, String> comments() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("general", "Basic mod settings.");
        values.put("general.enabled", "Turns the mod on or off. If false, new events are ignored.");
        values.put("general.enableMod", "Legacy alias for enabled. Keep it the same as enabled.");
        values.put("general.configVersion", "Config schema version. Leave this alone unless migrating by hand.");
        values.put("general.locale", "Preferred language hint for future config/UI text.");
        values.put("general.autosaveIntervalSeconds", "How often Minecraft may autosave world data, in seconds. Values below 30 reset to the safe default 300.");

        values.put("generation", "Automatic place generation settings. These affect new generation, not existing places.");
        values.put("generation.scanRadiusChunks", "Legacy radius value. The mod stays event-driven and does not scan the world.");
        values.put("generation.minEventsForNamedPlace", "Minimum event count used by older generation logic.");
        values.put("generation.maxGeneratedPlacesPerChunk", "Maximum generated places per chunk candidate.");
        values.put("generation.mergeDistanceBlocks", "Legacy merge distance in blocks.");
        values.put("generation.defaultHorizontalRadiusBlocks", "Default horizontal radius for generated places, in blocks. Must be positive.");
        values.put("generation.defaultVerticalRadiusBlocks", "Default vertical radius for generated places, in blocks. Must be positive.");
        values.put("generation.placeMaxMergeDistanceBlocks", "Maximum distance for merging normal area places, in blocks.");
        values.put("generation.placeMaxClusterRadiusBlocks", "Maximum radius for normal area place clusters, in blocks.");
        values.put("generation.placeMaxCenterShiftOnUpdateBlocks", "Maximum center movement on one update, in blocks.");
        values.put("generation.deathSiteMaxMergeDistanceBlocks", "Maximum distance for merging death sites, in blocks.");
        values.put("generation.deathSiteMaxClusterRadiusBlocks", "Maximum radius for death-site clusters, in blocks.");
        values.put("generation.deathSiteMaxCenterShiftOnUpdateBlocks", "Maximum death-site center movement on one update, in blocks.");
        values.put("generation.verticalSeparationMinYGap", "Minimum Y gap for separate vertical places, in blocks.");
        values.put("generation.allowMixedDeathSiteEnvironments", "If true, death sites may mix cave/surface/etc. False keeps them separate.");
        values.put("generation.allowUnknownDeathSiteEnvironmentFallback", "Allows UNKNOWN death-site environment to merge with known environments.");
        values.put("generation.generalLandmarkMaxMergeDistanceBlocks", "Maximum merge distance for general landmarks, in blocks.");
        values.put("generation.generalLandmarkMaxClusterRadiusBlocks", "Maximum cluster radius for general landmarks, in blocks.");
        values.put("generation.generalLandmarkMaxCenterShiftOnUpdateBlocks", "Maximum center movement for general landmarks, in blocks.");
        values.put("generation.portalLandmarkMaxMergeDistanceBlocks", "Maximum merge distance for portal landmarks, in blocks.");
        values.put("generation.portalLandmarkMaxClusterRadiusBlocks", "Maximum radius for portal landmarks, in blocks.");
        values.put("generation.portalLandmarkMaxCenterShiftOnUpdateBlocks", "Maximum center movement for portal landmarks, in blocks.");
        values.put("generation.pointPlaceMaxMergeDistanceBlocks", "Maximum merge distance for point-like places, in blocks.");
        values.put("generation.pointPlaceMaxClusterRadiusBlocks", "Maximum radius for point-like places, in blocks.");
        values.put("generation.pointPlaceMaxCenterShiftOnUpdateBlocks", "Maximum center movement for point-like places, in blocks.");
        values.put("generation.deletedPlaceSuppressionEnabled", "If true, deleted places leave markers that prevent auto-recreation.");
        values.put("generation.deletedPlaceSuppressionDays", "How long deleted markers prevent auto-recreation, in Minecraft days. -1 forever, 0 disables.");
        values.put("generation.spacing", "Extra spacing rules for sparse place types. Existing places are not renamed or removed.");
        values.put("generation.spacing.enabled", "If true, applies per-place-type spacing before creating new places.");
        values.put("generation.spacing.sameTypeMinDistanceBlocks", "Minimum distance from another place of the same type before a new place can be created. Values below 0 are clamped.");
        values.put("generation.spacing.sameTypeMinDistanceBlocks.GENERAL_LANDMARK", "GENERAL_LANDMARK is intentionally sparse because it represents casual travel/activity. Increase if generic places are too dense; decrease for a denser map.");
        values.put("generation.spacing.anyPlaceMinDistanceBlocks", "Minimum distance from any existing NamedPlace before this place type can be created. Only configured types are affected. Values below 0 are clamped.");
        values.put("generation.spacing.anyPlaceMinDistanceBlocks.GENERAL_LANDMARK", "Prevents generic landmarks from cluttering around more specific named places.");
        values.put("generation.spacing.mergeDistanceBlocks", "Distance where a nearby candidate may update an existing place instead of creating a new one. It is clamped to the same-type spacing distance.");
        values.put("generation.spacing.mergeDistanceBlocks.GENERAL_LANDMARK", "GENERAL_LANDMARK candidates beyond this distance are suppressed instead of moving an existing landmark across the map.");

        values.put("thresholds", "Legacy rarity and minimum thresholds.");
        values.put("thresholds.*", "Threshold value used for rarity or legacy generation checks.");
        values.put("requiredCounts", "Raw event counts required before a place can become a candidate.");
        values.put("requiredCounts.hostileKillsForBattlefield", "Minimum accepted combat events in the local cluster before a battlefield can generate. Anti-farm still filters repetitive farm-like kills.");
        values.put("requiredCounts.*", "Required raw count. Higher values make this place type rarer. Values below 1 are clamped.");
        values.put("scoreThresholds", "Score thresholds required before a place can become a candidate.");
        values.put("scoreThresholds.battlefield", "Controls how much local combat is required before a battlefield is named. Lower values make combat sites appear more often. Anti-farm still filters repetitive farm-like kills.");
        values.put("scoreThresholds.*", "Required score. Higher values make this place type rarer. Negative or non-finite values are clamped to 0.");

        values.put("display", "Player-facing display settings.");
        values.put("display.discoveryMessages", "Shows discovery messages when places are found.");
        values.put("display.showCoordinates", "Shows coordinates in player-facing place messages.");
        values.put("display.showRarity", "Shows place rarity in player-facing messages.");
        values.put("display.nearbyPlaceRadiusBlocks", "Radius used for nearby place checks, in blocks.");
        values.put("display.maxPlacesInList", "Maximum places shown per /places list page.");

        values.put("performance", "Performance limits. The mod remains event-driven and does not scan the world.");
        values.put("performance.maxScoreEvaluationsPerTick", "Maximum dirty chunks scored per server tick.");
        values.put("performance.candidateGenerationCooldownTicks", "Cooldown before the same chunk can create candidate logs again, in ticks. 0 disables this cooldown.");
        values.put("performance.maxClusterRadiusChunks", "Maximum cluster radius for neighboring candidate chunks, in chunks.");
        values.put("performance.visitSampleIntervalTicks", "How often player visits are sampled, in ticks. Values below 20 reset to the safe default 400.");
        values.put("performance.displayCheckIntervalTicks", "How often display checks run, in ticks. Values below 20 reset to the safe default 100.");
        values.put("performance.structureDiscoveryCheckIntervalTicks", "How often structure discovery checks run for a player, in ticks. Values below 20 reset to the safe default 200.");

        values.put("eventCollection", "Event collection settings. These affect which events are counted before generation.");
        values.put("eventCollection.countCreativeModeEvents", "If false, future creative-mode event hooks may be ignored for automatic generation.");

        values.put("antiFarm", "Anti-farm protection for repeated mob kills.");
        values.put("antiFarm.*", "Anti-farm tuning. Higher limits are more permissive.");

        values.put("naming", "Naming settings for newly generated places. Existing places keep saved recipes.");
        values.put("naming.defaultStyle", "Default naming style for newly generated places. Unknown styles fall back to vanilla_adventure.");
        values.put("naming.enabledStyles", "Styles available for random or weighted style selection. Unknown styles are ignored/fixed safely.");
        values.put("naming.styleSelectionMode", "How naming style is selected: DEFAULT_ONLY, RANDOM_ENABLED, WEIGHTED, or PER_PLACE_TYPE.");
        values.put("naming.styleWeights", "Weights used when styleSelectionMode is WEIGHTED.");
        values.put("naming.styleWeights.*", "Style weight. 0 means this style will not be selected by weighted mode. Negative values are clamped to 0.");
        values.put("naming.*", "Naming option. Existing saved NameRecipe data is not regenerated automatically.");

        values.put("commands", "Command permission settings.");
        values.put("commands.requiredOpLevel", "OP level required for admin commands. Valid range: 0 to 4.");

        values.put("placeTypes", "Controls automatic generation of each place type.");
        values.put("placeTypes.displayExistingWhenDisabled", "If true, existing disabled-type places are still shown to players.");
        values.put("placeTypes.allowManualCreateWhenDisabled", "If true, OPs can manually create disabled place types.");
        values.put("placeTypes.autoGeneration", "These toggles control automatic generation only. They do not delete existing places.");

        values.put("biomeThemes", "Biome theme mapping for generic landmarks. Useful for modded biomes later.");
        values.put("biomeThemes.enabled", "If true, biome metadata is used when naming general landmarks.");
        values.put("biomeThemes.useBiomeForGeneralLandmarks", "If true, GENERAL_LANDMARK names may use the biome at the place center.");
        values.put("biomeThemes.unknownBiomeFallbackGroup", "Fallback biome group when a biome is missing or unmapped.");
        values.put("biomeThemes.mappings", "Maps biome ids to simple groups like forest, desert, plains, nether, or end.");
        values.put("biomeThemes.mappings.*", "Biome id to biome group. Add modded biome ids here for custom packs.");

        values.put("notifications", "Chat notifications shown when a new place is automatically created.");
        values.put("notifications.placeCreated", "Lore-style chat messages for newly created places. This does not affect generation.");
        values.put("notifications.placeCreated.enabled", "Turns new-place chat messages on or off.");
        values.put("notifications.placeCreated.sendToAllPlayers", "If true, all online players receive new-place messages.");
        values.put("notifications.placeCreated.sendToNearbyPlayersOnly", "If true, only nearby players in the same dimension receive messages.");
        values.put("notifications.placeCreated.nearbyRadiusBlocks", "Nearby notification radius, in blocks. 0 means only exact-position checks where applicable.");
        values.put("notifications.placeCreated.includeCoordinates", "If true, messages include a small coordinate hint.");
        values.put("notifications.placeCreated.includeDimension", "If true, messages include the dimension id.");
        values.put("notifications.placeCreated.includePlaceType", "If true, messages include the place type.");
        values.put("notifications.placeCreated.notifyManualCreate", "If true, manual /places create can show notifications in future command paths.");
        values.put("notifications.placeCreated.maxNotificationsPerMinute", "Maximum new-place messages per minute. 0 disables rate limiting.");
        values.put("notifications.placeCreated.perType", "Per-place-type notification toggles. Disabling a type only hides messages.");
        values.put("notifications.placeCreated.perType.*", "If false, this place type will not send place-created chat messages.");

        values.put("titleOverlay", "Server-side title overlay checks. Only saved NamedPlace objects can trigger titles.");
        values.put("titleOverlay.enabled", "Turns NamedPlace title overlay packets on or off.");
        values.put("titleOverlay.checkIntervalTicks", "How often the server checks whether players entered a named place. Values below 1 reset to 10; values above 200 are clamped.");
        values.put("titleOverlay.minTitleRadius", "Small places use at least this radius for title entry checks. Must be positive.");
        values.put("titleOverlay.exitPaddingBlocks", "Extra radius used only for leaving a place. Prevents title border flicker. Values below 0 are clamped.");
        values.put("titleOverlay.globalCooldownTicks", "Minimum time between any two title overlays for one player.");
        values.put("titleOverlay.samePlaceCooldownTicks", "Minimum time before the same place can show again for one player.");
        values.put("titleOverlay.generalLandmarkCooldownTicks", "Longer repeat cooldown for GENERAL_LANDMARK titles because they are background travel content.");
        values.put("titleOverlay.teleportDelayTicks", "Delay after teleport or dimension change before entry titles can appear.");
        values.put("titleOverlay.showOnEnter", "If true, entering a saved NamedPlace can show a title.");
        values.put("titleOverlay.showOnPlaceCreated", "If true, a newly created saved NamedPlace can show a title to players inside it.");
        values.put("titleOverlay.showGeneralLandmarks", "If true, GENERAL_LANDMARK places may show titles.");
        values.put("titleOverlay.generalLandmarkOnlyIfNoHigherPriority", "If true, GENERAL_LANDMARK titles are suppressed when a more specific place also contains the player.");
        values.put("titleOverlay.verticalToleranceBlocks", "Y-distance tolerance for horizontal title checks. Cave/mining/portal-like places still use 3D distance.");

        values.put("decay", "Future decay settings for old importance.");
        values.put("decay.*", "Decay setting. Existing places are kept unless future decay logic says otherwise.");
        values.put("candidateDecay", "Candidate decay affects only activity that has not yet become a NamedPlace. Existing named places are never reduced, renamed, or removed by candidate decay.");
        values.put("candidateDecay.enabled", "If true, stale unpromoted candidate activity can fade after the grace period.");
        values.put("candidateDecay.intervalTicks", "How often stale candidate activity loses score, in ticks. Values below 20 reset to the safe default 24000. 24000 ticks = 1 Minecraft day.");
        values.put("candidateDecay.gracePeriodTicks", "How long fresh unpromoted activity is protected before decay can start. Values below 0 are clamped.");
        values.put("candidateDecay.baseDecayPerInterval", "Base score removed each interval before applying the place-type multiplier. Values below 0 are clamped.");
        values.put("candidateDecay.minCandidateScore", "Lowest score candidate decay may leave behind.");
        values.put("candidateDecay.pruneBelowScore", "If true, very small candidate scores are clamped to the minimum candidate score.");
        values.put("candidateDecay.pruneThreshold", "Candidate scores at or below this value are treated as empty when pruning is enabled.");
        values.put("candidateDecay.debugLogging", "If true, decay passes print one compact summary. Keep false on normal servers.");
        values.put("candidateDecay.typeMultipliers", "Per-place-type decay multipliers for unpromoted candidates only. Set a multiplier to 0 to disable decay for that type.");
        values.put("candidateDecay.typeMultipliers.*", "Decay multiplier for this place type. Existing NamedPlace objects are protected regardless of this value.");
        values.put("journal", "World Journal item and GUI settings.");
        values.put("journal.enabled", "If true, players can open the World Journal item GUI.");
        values.put("journal.visibilityMode", "ALL_KNOWN shows all active places. VISITED_BY_PLAYER shows only places discovered by that player.");
        values.put("journal.managementMode", "Controls who may rename/delete/restore names through the journal: SINGLEPLAYER_OWNER_AND_OP, OP_ONLY, ALL_PLAYERS, or DISABLED.");
        values.put("journal.pageSize", "Upper limit for places returned per journal page. Values outside 5 to 100 reset to the safe default 20. The book UI currently caps visible pages at 4 entries for readability.");
        values.put("journal.showExactCoordinates", "If true, journal details can show exact saved place coordinates.");
        values.put("journal.allowTeleportForOps", "If true, OPs and cheat-enabled players can teleport from the journal.");
        values.put("journal.maxManualNameLength", "Maximum length for names entered through the journal rename button. Values outside 8 to 128 reset to the safe default 64.");
        values.put("permissions", "Legacy permission settings used by older commands.");
        values.put("permissions.*", "Permission setting. OP levels are from 0 to 4.");
        values.put("debug", "Debug settings for development. Keep disabled on public servers.");
        values.put("debug.enabled", "Enables debug logs for development. Keep false on public servers.");
        values.put("debug.namingVerbose", "Logs detailed naming candidate rejection reasons. Useful for debugging name generation, but can spam logs. Keep disabled on normal servers.");
        values.put("debug.useTestingThresholds", "Uses very low thresholds for testing. Keep false for normal worlds.");
        values.put("debug.*", "Debug logging option.");
        return values;
    }
}
