package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class EventCollector {
    private static final AntiFarmEngine ANTI_FARM_ENGINE = new AntiFarmEngine();

    private EventCollector() {
    }

    public interface EventStore {
        WorldMemoryStorageData data();

        void markDirty(WorldMemoryEvent event, ChunkMemoryStats changedChunkStats);
    }

    public static String debugAntiFarmState() {
        return ANTI_FARM_ENGINE.debugState(WorldRemembersLivingLegends.config());
    }

    public static boolean collect(WorldMemoryEvent event, EventStore store, Consumer<String> debugLogger) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(store, "store");

        LivingLegendsConfig config = WorldRemembersLivingLegends.config();
        if (!config.general.isEnabled()) {
            debug(config, debugLogger, "Ignored world memory event because general.enableMod is false type="
                    + event.eventType().idString()
                    + " pos=" + event.position().blockIdString());
            return false;
        }

        AntiFarmEngine.Decision antiFarmDecision = ANTI_FARM_ENGINE.evaluate(event, config);
        if (antiFarmDecision.rejected()) {
            if (config.debug.enabled && config.antiFarm.debugFarmRejections && debugLogger != null) {
                debugLogger.accept("Rejected farm-like kill event"
                        + " type=" + event.eventType().idString()
                        + " actor=" + event.actorIdString()
                        + " pos=" + event.position().blockIdString()
                        + " reason=\"" + antiFarmDecision.reason() + "\""
                        + " acceptedKillEvents=" + antiFarmDecision.acceptedKillEvents()
                        + " rejectedFarmLikeKillEvents=" + antiFarmDecision.rejectedFarmLikeKillEvents());
                if (antiFarmDecision.suppressionWindowCreated()) {
                    debugLogger.accept("Created anti-farm local suppression window"
                            + " key=" + antiFarmDecision.suppressionKey()
                            + " expiresAtEpochMillis=" + antiFarmDecision.suppressionExpiresAtEpochMillis());
                }
            } else {
                debug(config, debugLogger, "Ignored farm-like kill event type="
                        + event.eventType().idString()
                        + " actor=" + event.actorIdString()
                        + " pos=" + event.position().blockIdString());
            }
            logAntiFarmCounters(config, debugLogger, antiFarmDecision);
            return false;
        }

        WorldMemoryStorageData data = store.data();
        boolean changed = data.incrementEventCounter(event.eventType());
        changed |= data.recordFirstWorldEvent(event);
        changed |= data.recordFirstPlayerWorldEvent(event);

        ChunkMemoryStats changedChunkStats = data.updateChunkStats(event);
        changed |= changedChunkStats != null;

        if (event.eventType() == EventType.STRUCTURE_DISCOVERED) {
            debug(config, debugLogger, formatStructureDiscoveredLog(event));
        }

        List<WorldFirstDiscoveryRecord> newFirstDiscoveries = recordWorldFirstDiscoveries(event, data, config, debugLogger);
        for (WorldFirstDiscoveryRecord discovery : newFirstDiscoveries) {
            WorldMemoryEvent discoveryEvent = discovery.toDiscoveryEvent();
            changed |= data.incrementEventCounter(discoveryEvent.eventType());
            ChunkMemoryStats discoveryChunkStats = data.updateChunkStats(discoveryEvent);
            changed |= discoveryChunkStats != null;
            if (discoveryChunkStats != null) {
                changedChunkStats = discoveryChunkStats;
            }
            debug(config, debugLogger, formatFirstDiscoveryLog(discovery));
            debug(config, debugLogger, formatDebugEvent(discoveryEvent));
        }

        if (changed) {
            store.markDirty(event, changedChunkStats);
        }

        debug(config, debugLogger, formatDebugEvent(event));
        logAntiFarmCounters(config, debugLogger, antiFarmDecision);
        return changed;
    }

    public static void collect(WorldMemoryEvent event, Consumer<String> debugLogger) {
        Objects.requireNonNull(event, "event");

        debug(WorldRemembersLivingLegends.config(), debugLogger, formatDebugEvent(event));
    }

    public static void expireTemporaryWindows() {
        ANTI_FARM_ENGINE.expireTemporaryWindows(System.currentTimeMillis());
    }

    public static String formatDebugEvent(WorldMemoryEvent event) {
        return "Collected world memory event"
                + " id=" + event.eventIdString()
                + " type=" + event.eventType().idString()
                + " pos=" + event.position().blockIdString()
                + " actor=" + event.actorIdString()
                + " subject=" + event.subjectIdString()
                + " score=" + event.basicScore()
                + " note=\"" + event.note() + "\"";
    }

    public static String formatFirstDiscoveryLog(WorldFirstDiscoveryRecord discovery) {
        PlaceBounds bounds = discovery.effectiveBounds();
        return "FIRST_DISCOVERY recorded"
                + " key=" + discovery.discoveryIdString()
                + " placeType=" + discovery.placeType().name()
                + " bounds=" + (bounds == null ? "POINT_RADIUS" : bounds.shape().name())
                + " scope=world"
                + " triggerType=" + discovery.triggerTypeIdString()
                + " targetId=" + discovery.targetIdString()
                + " playerName=" + discovery.playerName()
                + " position=" + discovery.position().blockIdString();
    }

    public static String formatStructureDiscoveredLog(WorldMemoryEvent event) {
        PlaceBounds bounds = event.structureBounds();
        return "STRUCTURE_DISCOVERED"
                + " structureId=" + event.structureIdString()
                + " firstDiscoveryKey=" + event.firstDiscoveryKeyString()
                + " scope=world"
                + " triggerType=" + DiscoveryTriggerType.STRUCTURE_DISCOVERED.idString()
                + " targetId=" + event.subjectIdString()
                + " boundsFound=" + (bounds != null)
                + (bounds == null
                ? " fallbackRadius=" + noteValue(event.note(), "fallbackRadius")
                : " min=" + bounds.minX() + "," + bounds.minY() + "," + bounds.minZ()
                + " max=" + bounds.maxX() + "," + bounds.maxY() + "," + bounds.maxZ())
                + " playerName=" + noteValue(event.note(), "player_name")
                + " position=" + event.position().blockIdString();
    }

    private static List<WorldFirstDiscoveryRecord> recordWorldFirstDiscoveries(
            WorldMemoryEvent event,
            WorldMemoryStorageData data,
            LivingLegendsConfig config,
            Consumer<String> debugLogger
    ) {
        List<WorldFirstDiscoveryRecord> records = new ArrayList<>();
        for (FirstDiscoveryDefinition definition : FirstDiscoveryDefinitions.matchingDefinitions(event)) {
            WorldFirstDiscoveryRecord record = WorldFirstDiscoveryRecord.from(definition, event);
            if (data.recordWorldFirstDiscovery(record)) {
                records.add(record);
            } else {
                debug(config, debugLogger, "FIRST_DISCOVERY skipped"
                        + " key=" + record.discoveryIdString()
                        + " scope=world"
                        + " triggerType=" + record.triggerTypeIdString()
                        + " targetId=" + record.targetIdString()
                        + " reason=already_recorded");
            }
        }
        return records;
    }

    private static void debug(LivingLegendsConfig config, Consumer<String> debugLogger, String message) {
        if (config.debug.enabled && debugLogger != null) {
            debugLogger.accept(message);
        }
    }

    private static String noteValue(String note, String key) {
        if (note == null || note.isBlank() || key == null || key.isBlank()) {
            return "";
        }

        String prefix = key + "=";
        for (String part : note.split("\\s+")) {
            if (part.startsWith(prefix) && part.length() > prefix.length()) {
                return part.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private static void logAntiFarmCounters(
            LivingLegendsConfig config,
            Consumer<String> debugLogger,
            AntiFarmEngine.Decision antiFarmDecision
    ) {
        if (!antiFarmDecision.checked() || debugLogger == null) {
            return;
        }

        if (config.debug.enabled) {
            debugLogger.accept("Anti-farm counters"
                    + " acceptedKillEvents=" + antiFarmDecision.acceptedKillEvents()
                    + " rejectedFarmLikeKillEvents=" + antiFarmDecision.rejectedFarmLikeKillEvents());
        }
    }
}
