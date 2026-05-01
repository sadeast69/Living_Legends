package com.worldremembers.livinglegends;

import com.worldremembers.livinglegends.config.LivingLegendsConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AntiFarmEngine {
    private static final long TEN_MINUTES_MILLIS = 10L * 60L * 1000L;
    private static final int MAX_RECENT_WINDOWS = 4096;
    private static final String UNKNOWN_ENTITY_TYPE = "unknown";

    private final Map<String, KillWindow> killWindowsByArea = new LinkedHashMap<>();
    private final Map<String, SuppressionWindow> suppressionsByArea = new LinkedHashMap<>();
    private long acceptedKillEvents;
    private long rejectedFarmLikeKillEvents;

    public synchronized Decision evaluate(WorldMemoryEvent event, LivingLegendsConfig config) {
        if (event == null || config == null || !isCheckedKillEvent(event.eventType())) {
            return Decision.notChecked(acceptedKillEvents, rejectedFarmLikeKillEvents);
        }

        long now = event.createdAtEpochMillis() > 0L ? event.createdAtEpochMillis() : System.currentTimeMillis();
        LivingLegendsConfig.AntiFarm antiFarm = config.antiFarm;
        if (!antiFarm.enabled) {
            acceptedKillEvents++;
            return Decision.accepted("anti-farm disabled", acceptedKillEvents, rejectedFarmLikeKillEvents);
        }

        KillSample sample = KillSample.from(event, now, antiFarm.samePositionRadiusBlocks);
        pruneSuppressions(now);
        SuppressionWindow activeSuppression = suppressionsByArea.get(sample.suppressionKey());
        if (activeSuppression != null && activeSuppression.activeAt(now)) {
            rejectedFarmLikeKillEvents++;
            trimRecentWindows();
            return Decision.rejected(
                    "local suppression active key=" + sample.suppressionKey()
                            + " originalReason=\"" + activeSuppression.reason() + "\""
                            + " expiresInSeconds=" + Math.max(0L, (activeSuppression.expiresAtEpochMillis() - now) / 1000L),
                    false,
                    sample.suppressionKey(),
                    activeSuppression.expiresAtEpochMillis(),
                    acceptedKillEvents,
                    rejectedFarmLikeKillEvents
            );
        }

        KillWindow window = killWindowsByArea.computeIfAbsent(sample.areaKey(), key -> new KillWindow());
        window.prune(now - TEN_MINUTES_MILLIS);

        List<KillSample> candidateSamples = window.with(sample);
        String rejectionReason = rejectionReason(candidateSamples, sample, antiFarm);
        if (!rejectionReason.isBlank()) {
            rejectedFarmLikeKillEvents++;
            long suppressionExpiresAt = now + antiFarm.localSuppressionWindowSeconds * 1000L;
            suppressionsByArea.put(sample.suppressionKey(), new SuppressionWindow(suppressionExpiresAt, rejectionReason));
            trimRecentWindows();
            return Decision.rejected(
                    rejectionReason,
                    true,
                    sample.suppressionKey(),
                    suppressionExpiresAt,
                    acceptedKillEvents,
                    rejectedFarmLikeKillEvents
            );
        }

        window.add(sample);
        acceptedKillEvents++;
        trimRecentWindows();
        return Decision.accepted("accepted", acceptedKillEvents, rejectedFarmLikeKillEvents);
    }

    public synchronized boolean shouldIgnore(WorldMemoryEvent event, LivingLegendsConfig config) {
        return evaluate(event, config).rejected();
    }

    public synchronized void expireTemporaryWindows(long nowEpochMillis) {
        long now = nowEpochMillis > 0L ? nowEpochMillis : System.currentTimeMillis();
        pruneSuppressions(now);
        long oldestAllowedTimestampMillis = now - TEN_MINUTES_MILLIS;
        killWindowsByArea.values().forEach(window -> window.prune(oldestAllowedTimestampMillis));
        killWindowsByArea.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public synchronized long acceptedKillEvents() {
        return acceptedKillEvents;
    }

    public synchronized long rejectedFarmLikeKillEvents() {
        return rejectedFarmLikeKillEvents;
    }

    public synchronized int activeKillWindowsCount() {
        expireTemporaryWindows(System.currentTimeMillis());
        return killWindowsByArea.size();
    }

    public synchronized int activeSuppressionWindowsCount() {
        expireTemporaryWindows(System.currentTimeMillis());
        return suppressionsByArea.size();
    }

    public synchronized String debugState(LivingLegendsConfig config) {
        expireTemporaryWindows(System.currentTimeMillis());
        LivingLegendsConfig.AntiFarm antiFarm = config == null ? LivingLegendsConfig.defaults().antiFarm : config.antiFarm;
        return "World Remembers anti-farm"
                + " acceptedKillEvents=" + acceptedKillEvents
                + " rejectedFarmLikeKillEvents=" + rejectedFarmLikeKillEvents
                + " activeKillWindows=" + killWindowsByArea.size()
                + " activeSuppressionWindows=" + suppressionsByArea.size()
                + " enabled=" + antiFarm.enabled
                + " strictMode=" + antiFarm.strictMode
                + " smallAreaRadius=" + antiFarm.samePositionRadiusBlocks
                + " maxKillsInSmallAreaPerTenMinutes=" + antiFarm.maxKillsPerSmallAreaInTenMinutes
                + " maxAntiFarmWindowAgeTicks=12000"
                + " maxTrackedKillWindows=" + MAX_RECENT_WINDOWS;
    }

    public static boolean isCheckedKillEvent(EventType eventType) {
        return eventType == EventType.PLAYER_KILLED_HOSTILE_MOB
                || eventType == EventType.PLAYER_KILLED_PASSIVE_MOB
                || eventType == EventType.PLAYER_KILLED_NEUTRAL_MOB;
    }

    private String rejectionReason(
            List<KillSample> samples,
            KillSample currentSample,
            LivingLegendsConfig.AntiFarm antiFarm
    ) {
        int sampleCount = samples.size();
        if (sampleCount > antiFarm.maxKillsPerSmallAreaInTenMinutes) {
            return "too many kills in a small radius within 10 minutes"
                    + " count=" + sampleCount
                    + " limit=" + antiFarm.maxKillsPerSmallAreaInTenMinutes;
        }

        int minimumSamples = antiFarm.minimumSamplesForVarianceChecks;
        if (sampleCount < minimumSamples) {
            return "";
        }

        double playerSpread = spread(samples, true);
        if (!Double.isNaN(playerSpread) && playerSpread < antiFarm.minimumPlayerPositionSpreadBlocks) {
            return "player position variance is too low"
                    + " spread=" + playerSpread
                    + " minimum=" + antiFarm.minimumPlayerPositionSpreadBlocks;
        }

        double mobSpread = spread(samples, false);
        if (mobSpread < antiFarm.minimumMobDeathPositionSpreadBlocks) {
            return "mob death position variance is too low"
                    + " spread=" + mobSpread
                    + " minimum=" + antiFarm.minimumMobDeathPositionSpreadBlocks;
        }

        int sameEntityTypeKills = sameEntityTypeKillCount(samples, currentSample.entityType());
        if (sameEntityTypeKills >= antiFarm.sameEntityTypeKillThreshold) {
            return "same entity type killed repeatedly in the same small area"
                    + " entityType=" + currentSample.entityType()
                    + " count=" + sameEntityTypeKills
                    + " threshold=" + antiFarm.sameEntityTypeKillThreshold;
        }

        if (antiFarm.strictMode && looksLikeKillbox(samples, currentSample, antiFarm, playerSpread, mobSpread)) {
            return "strictMode killbox pattern"
                    + " entityType=" + currentSample.entityType()
                    + " count=" + sampleCount
                    + " playerSpread=" + playerSpread
                    + " mobSpread=" + mobSpread;
        }

        return "";
    }

    private boolean looksLikeKillbox(
            List<KillSample> samples,
            KillSample currentSample,
            LivingLegendsConfig.AntiFarm antiFarm,
            double playerSpread,
            double mobSpread
    ) {
        if (samples.size() < antiFarm.strictModeMinimumKills) {
            return false;
        }

        int sameEntityTypeKills = sameEntityTypeKillCount(samples, currentSample.entityType());
        double sameTypeRatio = (double) sameEntityTypeKills / (double) samples.size();
        double resolvedPlayerSpread = Double.isNaN(playerSpread) ? 0.0 : playerSpread;
        return sameTypeRatio >= antiFarm.strictModeSameEntityTypeRatio
                && resolvedPlayerSpread <= antiFarm.strictModeMaximumPlayerSpreadBlocks
                && mobSpread <= antiFarm.strictModeMaximumMobSpreadBlocks;
    }

    private int sameEntityTypeKillCount(List<KillSample> samples, String entityType) {
        int count = 0;
        for (KillSample sample : samples) {
            if (sample.entityType().equals(entityType)) {
                count++;
            }
        }
        return count;
    }

    private double spread(List<KillSample> samples, boolean playerPosition) {
        int count = 0;
        double sumX = 0.0;
        double sumY = 0.0;
        double sumZ = 0.0;

        for (KillSample sample : samples) {
            Position position = playerPosition ? sample.playerPosition() : sample.mobDeathPosition();
            if (position == null) {
                continue;
            }

            count++;
            sumX += position.x();
            sumY += position.y();
            sumZ += position.z();
        }

        if (count == 0) {
            return Double.NaN;
        }

        double meanX = sumX / count;
        double meanY = sumY / count;
        double meanZ = sumZ / count;
        double squaredDistanceSum = 0.0;
        for (KillSample sample : samples) {
            Position position = playerPosition ? sample.playerPosition() : sample.mobDeathPosition();
            if (position == null) {
                continue;
            }

            double deltaX = position.x() - meanX;
            double deltaY = position.y() - meanY;
            double deltaZ = position.z() - meanZ;
            squaredDistanceSum += deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        }

        return Math.sqrt(squaredDistanceSum / count);
    }

    private void trimRecentWindows() {
        while (killWindowsByArea.size() > MAX_RECENT_WINDOWS) {
            String firstKey = killWindowsByArea.keySet().iterator().next();
            killWindowsByArea.remove(firstKey);
        }

        while (suppressionsByArea.size() > MAX_RECENT_WINDOWS) {
            String firstKey = suppressionsByArea.keySet().iterator().next();
            suppressionsByArea.remove(firstKey);
        }
    }

    private void pruneSuppressions(long now) {
        suppressionsByArea.entrySet().removeIf(entry -> !entry.getValue().activeAt(now));
    }

    public record Decision(
            boolean checked,
            boolean rejected,
            String reason,
            boolean suppressionWindowCreated,
            String suppressionKey,
            long suppressionExpiresAtEpochMillis,
            long acceptedKillEvents,
            long rejectedFarmLikeKillEvents
    ) {
        private static Decision notChecked(long acceptedKillEvents, long rejectedFarmLikeKillEvents) {
            return new Decision(false, false, "", false, "", 0L, acceptedKillEvents, rejectedFarmLikeKillEvents);
        }

        private static Decision accepted(
                String reason,
                long acceptedKillEvents,
                long rejectedFarmLikeKillEvents
        ) {
            return new Decision(true, false, reason, false, "", 0L, acceptedKillEvents, rejectedFarmLikeKillEvents);
        }

        private static Decision rejected(
                String reason,
                boolean suppressionWindowCreated,
                String suppressionKey,
                long suppressionExpiresAtEpochMillis,
                long acceptedKillEvents,
                long rejectedFarmLikeKillEvents
        ) {
            return new Decision(
                    true,
                    true,
                    reason,
                    suppressionWindowCreated,
                    suppressionKey,
                    suppressionExpiresAtEpochMillis,
                    acceptedKillEvents,
                    rejectedFarmLikeKillEvents
            );
        }
    }

    private static final class KillWindow {
        private final Deque<KillSample> samples = new ArrayDeque<>();

        private void prune(long oldestAllowedTimestampMillis) {
            while (!samples.isEmpty() && samples.peekFirst().createdAtEpochMillis() < oldestAllowedTimestampMillis) {
                samples.removeFirst();
            }
        }

        private void add(KillSample sample) {
            samples.addLast(sample);
        }

        private boolean isEmpty() {
            return samples.isEmpty();
        }

        private List<KillSample> with(KillSample sample) {
            List<KillSample> result = new ArrayList<>(samples);
            result.add(sample);
            return result;
        }
    }

    private record KillSample(
            long createdAtEpochMillis,
            String areaKey,
            String suppressionKey,
            String entityType,
            Position playerPosition,
            Position mobDeathPosition
    ) {
        private static KillSample from(WorldMemoryEvent event, long now, int radiusBlocks) {
            Position mobDeathPosition = Position.from(event.position());
            Position playerPosition = playerPosition(event);
            String entityType = noteValue(event.note(), "target_type");
            if (entityType.isBlank()) {
                entityType = noteValue(event.note(), "entity_type");
            }
            if (entityType.isBlank() && event.subjectIdString().contains(":")) {
                entityType = event.subjectIdString();
            }
            if (entityType.isBlank()) {
                entityType = UNKNOWN_ENTITY_TYPE;
            }

            return new KillSample(
                    now,
                    areaKey(event.position(), entityType, radiusBlocks),
                    areaKey(event.position(), entityType, radiusBlocks),
                    entityType,
                    playerPosition,
                    mobDeathPosition
            );
        }

        private static String areaKey(WorldPos position, String entityType, int radiusBlocks) {
            int radius = Math.max(1, radiusBlocks);
            return position.dimensionId()
                    + "@" + areaCenter(position.x(), radius)
                    + "," + areaCenter(position.y(), radius)
                    + "," + areaCenter(position.z(), radius)
                    + ":" + entityType;
        }

        private static int areaCenter(int coordinate, int radius) {
            return Math.floorDiv(coordinate, radius) * radius + radius / 2;
        }

        private static Position playerPosition(WorldMemoryEvent event) {
            String value = noteValue(event.note(), "player_pos");
            if (value.isBlank()) {
                return null;
            }

            String[] parts = value.split(",", -1);
            if (parts.length != 3) {
                return null;
            }

            try {
                return new Position(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                );
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        private static String noteValue(String note, String key) {
            if (note == null || note.isBlank() || key == null || key.isBlank()) {
                return "";
            }

            String prefix = key + "=";
            String[] tokens = note.split("\\s+");
            for (String token : tokens) {
                if (token.startsWith(prefix)) {
                    return token.substring(prefix.length()).trim();
                }
            }

            return "";
        }
    }

    private record Position(int x, int y, int z) {
        private static Position from(WorldPos position) {
            return new Position(position.x(), position.y(), position.z());
        }
    }

    private record SuppressionWindow(long expiresAtEpochMillis, String reason) {
        private boolean activeAt(long now) {
            return now < expiresAtEpochMillis;
        }
    }
}
