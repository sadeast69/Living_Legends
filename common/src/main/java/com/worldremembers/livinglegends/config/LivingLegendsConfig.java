package com.worldremembers.livinglegends.config;

import com.worldremembers.livinglegends.NameStyle;
import com.worldremembers.livinglegends.NameStyleSelectionMode;
import com.worldremembers.livinglegends.PlaceType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LivingLegendsConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int CURRENT_CONFIG_VERSION = 3;

    public General general = new General();
    public Generation generation = new Generation();
    public Thresholds thresholds = new Thresholds();
    public RequiredCounts requiredCounts = new RequiredCounts();
    public ScoreThresholds scoreThresholds = new ScoreThresholds();
    public Display display = new Display();
    public Performance performance = new Performance();
    public EventCollection eventCollection = new EventCollection();
    public AntiFarm antiFarm = new AntiFarm();
    public Naming naming = new Naming();
    public Commands commands = new Commands();
    public PlaceTypes placeTypes = new PlaceTypes();
    public BiomeThemes biomeThemes = new BiomeThemes();
    public Notifications notifications = new Notifications();
    public TitleOverlay titleOverlay = new TitleOverlay();
    public Decay decay = new Decay();
    public CandidateDecay candidateDecay = new CandidateDecay();
    public Journal journal = new Journal();
    public Permissions permissions = new Permissions();
    public Debug debug = new Debug();

    private transient List<String> validationWarnings = new ArrayList<>();
    private transient ValidationSummary validationSummary = ValidationSummary.pass(List.of());
    private static final ThreadLocal<List<String>> LOAD_WARNINGS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<ValidationStats> LOAD_VALIDATION = ThreadLocal.withInitial(ValidationStats::new);

    public static LivingLegendsConfig defaults() {
        return new LivingLegendsConfig().normalize();
    }

    public static LivingLegendsConfig fromMap(Map<String, Object> values) {
        LivingLegendsConfig config = defaults();
        Map<String, Object> root = values == null ? Collections.emptyMap() : values;
        LOAD_WARNINGS.set(new ArrayList<>());
        LOAD_VALIDATION.set(new ValidationStats());
        int sourceConfigVersion = integer(section(root, "general"), "configVersion", 1);

        config.general.apply(section(root, "general"));
        config.generation.apply(section(root, "generation"));
        config.thresholds.apply(section(root, "thresholds"));
        config.requiredCounts.apply(section(root, "requiredCounts"));
        config.scoreThresholds.apply(section(root, "scoreThresholds"));
        config.display.apply(section(root, "display"));
        config.performance.apply(section(root, "performance"));
        config.eventCollection.apply(section(root, "eventCollection"));
        config.antiFarm.apply(section(root, "antiFarm"));
        config.naming.apply(section(root, "naming"));
        config.commands.apply(section(root, "commands"));
        config.placeTypes.apply(section(root, "placeTypes"));
        config.biomeThemes.apply(section(root, "biomeThemes"));
        config.notifications.apply(section(root, "notifications"));
        config.titleOverlay.apply(section(root, "titleOverlay"));
        config.decay.apply(section(root, "decay"));
        config.candidateDecay.apply(section(root, "candidateDecay"));
        config.journal.apply(section(root, "journal"));
        config.permissions.apply(section(root, "permissions"));
        config.debug.apply(section(root, "debug"));
        config.migrateDefaults(sourceConfigVersion);
        config.normalize();
        config.captureValidationSummary();
        LOAD_WARNINGS.remove();
        LOAD_VALIDATION.remove();
        return config;
    }

    private void migrateDefaults(int sourceConfigVersion) {
        if (sourceConfigVersion >= CURRENT_CONFIG_VERSION) {
            return;
        }

        if (sourceConfigVersion < 2) {
            if (requiredCounts != null && requiredCounts.hostileKillsForBattlefield == 45) {
                requiredCounts.hostileKillsForBattlefield = 8;
            }
            if (scoreThresholds != null && Math.abs(scoreThresholds.battlefield - 300.0) < 0.000_001) {
                scoreThresholds.battlefield = 56.0;
            }
        }

        if (sourceConfigVersion < 3) {
            if (candidateDecay != null && candidateDecay.intervalTicks == 1L) {
                candidateDecay.intervalTicks = 24000L;
                warnFixed("Migrated candidateDecay.intervalTicks from the old unsafe clamp value 1 to safe default 24000.");
            }
            if (journal != null && journal.pageSize == 100) {
                journal.pageSize = 20;
                warnFixed("Migrated journal.pageSize from the old unsafe clamp value 100 to safe default 20.");
            }
        }

        if (general != null && general.configVersion < CURRENT_CONFIG_VERSION) {
            general.configVersion = CURRENT_CONFIG_VERSION;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("general", general.toMap());
        values.put("generation", generation.toMap());
        values.put("thresholds", thresholds.toMap());
        values.put("requiredCounts", requiredCounts.toMap());
        values.put("scoreThresholds", scoreThresholds.toMap());
        values.put("display", display.toMap());
        values.put("performance", performance.toMap());
        values.put("eventCollection", eventCollection.toMap());
        values.put("antiFarm", antiFarm.toMap());
        values.put("naming", naming.toMap());
        values.put("commands", commands.toMap());
        values.put("placeTypes", placeTypes.toMap());
        values.put("biomeThemes", biomeThemes.toMap());
        values.put("notifications", notifications.toMap());
        values.put("titleOverlay", titleOverlay.toMap());
        values.put("decay", decay.toMap());
        values.put("candidateDecay", candidateDecay.toMap());
        values.put("journal", journal.toMap());
        values.put("permissions", permissions.toMap());
        values.put("debug", debug.toMap());
        return values;
    }

    public LivingLegendsConfig normalize() {
        if (general == null) {
            general = new General();
        }
        if (generation == null) {
            generation = new Generation();
        }
        if (thresholds == null) {
            thresholds = new Thresholds();
        }
        if (requiredCounts == null) {
            requiredCounts = new RequiredCounts();
        }
        if (scoreThresholds == null) {
            scoreThresholds = new ScoreThresholds();
        }
        if (display == null) {
            display = new Display();
        }
        if (performance == null) {
            performance = new Performance();
        }
        if (eventCollection == null) {
            eventCollection = new EventCollection();
        }
        if (antiFarm == null) {
            antiFarm = new AntiFarm();
        }
        if (naming == null) {
            naming = new Naming();
        }
        if (commands == null) {
            commands = new Commands();
        }
        if (placeTypes == null) {
            placeTypes = new PlaceTypes();
        }
        if (biomeThemes == null) {
            biomeThemes = new BiomeThemes();
        }
        if (notifications == null) {
            notifications = new Notifications();
        }
        if (titleOverlay == null) {
            titleOverlay = new TitleOverlay();
        }
        if (decay == null) {
            decay = new Decay();
        }
        if (candidateDecay == null) {
            candidateDecay = new CandidateDecay();
        }
        if (journal == null) {
            journal = new Journal();
        }
        if (permissions == null) {
            permissions = new Permissions();
        }
        if (debug == null) {
            debug = new Debug();
        }

        general.normalize();
        generation.normalize();
        thresholds.normalize();
        requiredCounts.normalize();
        scoreThresholds.normalize();
        display.normalize();
        performance.normalize();
        eventCollection.normalize();
        antiFarm.normalize();
        naming.normalize();
        commands.normalize();
        placeTypes.normalize();
        biomeThemes.normalize();
        notifications.normalize();
        titleOverlay.normalize();
        decay.normalize();
        candidateDecay.normalize();
        journal.normalize();
        permissions.normalize();
        debug.normalize();
        return this;
    }

    public ValidationSummary validateAndFix() {
        LOAD_WARNINGS.set(new ArrayList<>());
        LOAD_VALIDATION.set(new ValidationStats());
        normalize();
        captureValidationSummary();
        LOAD_WARNINGS.remove();
        LOAD_VALIDATION.remove();
        return validationSummary;
    }

    private void captureValidationSummary() {
        List<String> warnings = List.copyOf(LOAD_WARNINGS.get());
        validationWarnings = warnings;
        validationSummary = LOAD_VALIDATION.get().summary(warnings);
    }

    public List<String> validationWarnings() {
        return validationWarnings == null ? List.of() : List.copyOf(validationWarnings);
    }

    public ValidationSummary validationSummary() {
        return validationSummary == null ? ValidationSummary.pass(validationWarnings()) : validationSummary;
    }

    public record ValidationSummary(
            int warnings,
            int fixedValues,
            int invalidEnums,
            int unknownPlaceTypes,
            int clampedValues,
            List<String> messages
    ) {
        private static ValidationSummary pass(List<String> messages) {
            return new ValidationSummary(
                    messages == null ? 0 : messages.size(),
                    0,
                    0,
                    0,
                    0,
                    messages == null ? List.of() : List.copyOf(messages)
            );
        }

        public String result() {
            return warnings == 0 ? "PASS" : "WARN";
        }

        public String compact() {
            return "result=" + result()
                    + " fixed=" + fixedValues
                    + " warnings=" + warnings
                    + " invalidEnums=" + invalidEnums
                    + " unknownPlaceTypes=" + unknownPlaceTypes
                    + " clamped=" + clampedValues;
        }

        public List<String> firstWarnings(int max) {
            if (messages == null || messages.isEmpty() || max <= 0) {
                return List.of();
            }
            return List.copyOf(messages.subList(0, Math.min(max, messages.size())));
        }
    }

    private static final class ValidationStats {
        private int fixedValues;
        private int invalidEnums;
        private int unknownPlaceTypes;
        private int clampedValues;

        private ValidationSummary summary(List<String> warnings) {
            return new ValidationSummary(
                    warnings == null ? 0 : warnings.size(),
                    fixedValues,
                    invalidEnums,
                    unknownPlaceTypes,
                    clampedValues,
                    warnings == null ? List.of() : List.copyOf(warnings)
            );
        }
    }

    public static final class General implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean enabled = true;
        public boolean enableMod = true;
        public int configVersion = CURRENT_CONFIG_VERSION;
        public String locale = "en_us";
        public int autosaveIntervalSeconds = 300;

        public boolean isEnabled() {
            return enabled && enableMod;
        }

        private void normalize() {
            configVersion = atLeast("general.configVersion", configVersion, 1);
            locale = nonBlank("general.locale", locale, "en_us");
            autosaveIntervalSeconds = defaultIfBelow("general.autosaveIntervalSeconds", autosaveIntervalSeconds, 30, 300);
        }

        private void apply(Map<String, Object> values) {
            enabled = bool(values, "enabled", enabled);
            enableMod = bool(values, "enableMod", enabled);
            configVersion = integer(values, "configVersion", configVersion);
            locale = string(values, "locale", locale);
            autosaveIntervalSeconds = integer(values, "autosaveIntervalSeconds", autosaveIntervalSeconds);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("enabled", enabled);
            values.put("enableMod", enableMod);
            values.put("configVersion", configVersion);
            values.put("locale", locale);
            values.put("autosaveIntervalSeconds", autosaveIntervalSeconds);
            return values;
        }
    }

    public static final class Generation implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean enabled = true;
        public int scanRadiusChunks = 8;
        public int minEventsForNamedPlace = 4;
        public int maxGeneratedPlacesPerChunk = 1;
        public int mergeDistanceBlocks = 96;
        public int defaultHorizontalRadiusBlocks = 64;
        public int defaultVerticalRadiusBlocks = 48;
        public int placeMaxMergeDistanceBlocks = 96;
        public int placeMaxClusterRadiusBlocks = 64;
        public int placeMaxCenterShiftOnUpdateBlocks = 24;
        public int deathSiteMaxMergeDistanceBlocks = 48;
        public int deathSiteMaxClusterRadiusBlocks = 40;
        public int deathSiteMaxCenterShiftOnUpdateBlocks = 12;
        public int verticalSeparationMinYGap = 32;
        public boolean allowMixedDeathSiteEnvironments = false;
        public boolean allowUnknownDeathSiteEnvironmentFallback = false;
        public int generalLandmarkMaxMergeDistanceBlocks = 48;
        public int generalLandmarkMaxClusterRadiusBlocks = 48;
        public int generalLandmarkMaxCenterShiftOnUpdateBlocks = 12;
        public int portalLandmarkMaxMergeDistanceBlocks = 48;
        public int portalLandmarkMaxClusterRadiusBlocks = 24;
        public int portalLandmarkMaxCenterShiftOnUpdateBlocks = 12;
        public int pointPlaceMaxMergeDistanceBlocks = 24;
        public int pointPlaceMaxClusterRadiusBlocks = 16;
        public int pointPlaceMaxCenterShiftOnUpdateBlocks = 8;
        public boolean deletedPlaceSuppressionEnabled = true;
        public int deletedPlaceSuppressionDays = -1;
        public Spacing spacing = new Spacing();

        private void normalize() {
            if (spacing == null) {
                spacing = new Spacing();
            }
            scanRadiusChunks = atLeast("generation.scanRadiusChunks", scanRadiusChunks, 1);
            minEventsForNamedPlace = atLeast("generation.minEventsForNamedPlace", minEventsForNamedPlace, 1);
            maxGeneratedPlacesPerChunk = atLeast("generation.maxGeneratedPlacesPerChunk", maxGeneratedPlacesPerChunk, 1);
            mergeDistanceBlocks = atLeast("generation.mergeDistanceBlocks", mergeDistanceBlocks, 8);
            defaultHorizontalRadiusBlocks = atLeast("generation.defaultHorizontalRadiusBlocks", defaultHorizontalRadiusBlocks, 8);
            defaultVerticalRadiusBlocks = atLeast("generation.defaultVerticalRadiusBlocks", defaultVerticalRadiusBlocks, 8);
            placeMaxMergeDistanceBlocks = atLeast("generation.placeMaxMergeDistanceBlocks", placeMaxMergeDistanceBlocks, 8);
            placeMaxClusterRadiusBlocks = atLeast("generation.placeMaxClusterRadiusBlocks", placeMaxClusterRadiusBlocks, 8);
            placeMaxCenterShiftOnUpdateBlocks = atLeast("generation.placeMaxCenterShiftOnUpdateBlocks", placeMaxCenterShiftOnUpdateBlocks, 0);
            deathSiteMaxMergeDistanceBlocks = atLeast("generation.deathSiteMaxMergeDistanceBlocks", deathSiteMaxMergeDistanceBlocks, 8);
            deathSiteMaxClusterRadiusBlocks = atLeast("generation.deathSiteMaxClusterRadiusBlocks", deathSiteMaxClusterRadiusBlocks, 8);
            deathSiteMaxCenterShiftOnUpdateBlocks = atLeast("generation.deathSiteMaxCenterShiftOnUpdateBlocks", deathSiteMaxCenterShiftOnUpdateBlocks, 0);
            verticalSeparationMinYGap = atLeast("generation.verticalSeparationMinYGap", verticalSeparationMinYGap, 0);
            generalLandmarkMaxMergeDistanceBlocks = atLeast("generation.generalLandmarkMaxMergeDistanceBlocks", generalLandmarkMaxMergeDistanceBlocks, 8);
            generalLandmarkMaxClusterRadiusBlocks = atLeast("generation.generalLandmarkMaxClusterRadiusBlocks", generalLandmarkMaxClusterRadiusBlocks, 8);
            generalLandmarkMaxCenterShiftOnUpdateBlocks = atLeast("generation.generalLandmarkMaxCenterShiftOnUpdateBlocks", generalLandmarkMaxCenterShiftOnUpdateBlocks, 0);
            portalLandmarkMaxMergeDistanceBlocks = atLeast("generation.portalLandmarkMaxMergeDistanceBlocks", portalLandmarkMaxMergeDistanceBlocks, 8);
            portalLandmarkMaxClusterRadiusBlocks = atLeast("generation.portalLandmarkMaxClusterRadiusBlocks", portalLandmarkMaxClusterRadiusBlocks, 8);
            portalLandmarkMaxCenterShiftOnUpdateBlocks = atLeast("generation.portalLandmarkMaxCenterShiftOnUpdateBlocks", portalLandmarkMaxCenterShiftOnUpdateBlocks, 0);
            pointPlaceMaxMergeDistanceBlocks = atLeast("generation.pointPlaceMaxMergeDistanceBlocks", pointPlaceMaxMergeDistanceBlocks, 8);
            pointPlaceMaxClusterRadiusBlocks = atLeast("generation.pointPlaceMaxClusterRadiusBlocks", pointPlaceMaxClusterRadiusBlocks, 8);
            pointPlaceMaxCenterShiftOnUpdateBlocks = atLeast("generation.pointPlaceMaxCenterShiftOnUpdateBlocks", pointPlaceMaxCenterShiftOnUpdateBlocks, 0);
            deletedPlaceSuppressionDays = atLeast("generation.deletedPlaceSuppressionDays", deletedPlaceSuppressionDays, -1);
            spacing.normalize();
        }

        private void apply(Map<String, Object> values) {
            enabled = bool(values, "enabled", enabled);
            scanRadiusChunks = integer(values, "scanRadiusChunks", scanRadiusChunks);
            minEventsForNamedPlace = integer(values, "minEventsForNamedPlace", minEventsForNamedPlace);
            maxGeneratedPlacesPerChunk = integer(values, "maxGeneratedPlacesPerChunk", maxGeneratedPlacesPerChunk);
            mergeDistanceBlocks = integer(values, "mergeDistanceBlocks", mergeDistanceBlocks);
            defaultHorizontalRadiusBlocks = integer(values, "defaultHorizontalRadiusBlocks", defaultHorizontalRadiusBlocks);
            defaultVerticalRadiusBlocks = integer(values, "defaultVerticalRadiusBlocks", defaultVerticalRadiusBlocks);
            placeMaxMergeDistanceBlocks = integer(values, "placeMaxMergeDistanceBlocks", placeMaxMergeDistanceBlocks);
            placeMaxClusterRadiusBlocks = integer(values, "placeMaxClusterRadiusBlocks", placeMaxClusterRadiusBlocks);
            placeMaxCenterShiftOnUpdateBlocks = integer(values, "placeMaxCenterShiftOnUpdateBlocks", placeMaxCenterShiftOnUpdateBlocks);
            deathSiteMaxMergeDistanceBlocks = integer(values, "deathSiteMaxMergeDistanceBlocks", deathSiteMaxMergeDistanceBlocks);
            deathSiteMaxClusterRadiusBlocks = integer(values, "deathSiteMaxClusterRadiusBlocks", deathSiteMaxClusterRadiusBlocks);
            deathSiteMaxCenterShiftOnUpdateBlocks = integer(values, "deathSiteMaxCenterShiftOnUpdateBlocks", deathSiteMaxCenterShiftOnUpdateBlocks);
            verticalSeparationMinYGap = integer(values, "verticalSeparationMinYGap", verticalSeparationMinYGap);
            allowMixedDeathSiteEnvironments = bool(values, "allowMixedDeathSiteEnvironments", allowMixedDeathSiteEnvironments);
            allowUnknownDeathSiteEnvironmentFallback = bool(values, "allowUnknownDeathSiteEnvironmentFallback", allowUnknownDeathSiteEnvironmentFallback);
            generalLandmarkMaxMergeDistanceBlocks = integer(values, "generalLandmarkMaxMergeDistanceBlocks", generalLandmarkMaxMergeDistanceBlocks);
            generalLandmarkMaxClusterRadiusBlocks = integer(values, "generalLandmarkMaxClusterRadiusBlocks", generalLandmarkMaxClusterRadiusBlocks);
            generalLandmarkMaxCenterShiftOnUpdateBlocks = integer(values, "generalLandmarkMaxCenterShiftOnUpdateBlocks", generalLandmarkMaxCenterShiftOnUpdateBlocks);
            portalLandmarkMaxMergeDistanceBlocks = integer(values, "portalLandmarkMaxMergeDistanceBlocks", portalLandmarkMaxMergeDistanceBlocks);
            portalLandmarkMaxClusterRadiusBlocks = integer(values, "portalLandmarkMaxClusterRadiusBlocks", portalLandmarkMaxClusterRadiusBlocks);
            portalLandmarkMaxCenterShiftOnUpdateBlocks = integer(values, "portalLandmarkMaxCenterShiftOnUpdateBlocks", portalLandmarkMaxCenterShiftOnUpdateBlocks);
            pointPlaceMaxMergeDistanceBlocks = integer(values, "pointPlaceMaxMergeDistanceBlocks", pointPlaceMaxMergeDistanceBlocks);
            pointPlaceMaxClusterRadiusBlocks = integer(values, "pointPlaceMaxClusterRadiusBlocks", pointPlaceMaxClusterRadiusBlocks);
            pointPlaceMaxCenterShiftOnUpdateBlocks = integer(values, "pointPlaceMaxCenterShiftOnUpdateBlocks", pointPlaceMaxCenterShiftOnUpdateBlocks);
            deletedPlaceSuppressionEnabled = bool(values, "deletedPlaceSuppressionEnabled", deletedPlaceSuppressionEnabled);
            deletedPlaceSuppressionDays = integer(values, "deletedPlaceSuppressionDays", deletedPlaceSuppressionDays);
            spacing.apply(section(values, "spacing"));
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("enabled", enabled);
            values.put("scanRadiusChunks", scanRadiusChunks);
            values.put("minEventsForNamedPlace", minEventsForNamedPlace);
            values.put("maxGeneratedPlacesPerChunk", maxGeneratedPlacesPerChunk);
            values.put("mergeDistanceBlocks", mergeDistanceBlocks);
            values.put("defaultHorizontalRadiusBlocks", defaultHorizontalRadiusBlocks);
            values.put("defaultVerticalRadiusBlocks", defaultVerticalRadiusBlocks);
            values.put("placeMaxMergeDistanceBlocks", placeMaxMergeDistanceBlocks);
            values.put("placeMaxClusterRadiusBlocks", placeMaxClusterRadiusBlocks);
            values.put("placeMaxCenterShiftOnUpdateBlocks", placeMaxCenterShiftOnUpdateBlocks);
            values.put("deathSiteMaxMergeDistanceBlocks", deathSiteMaxMergeDistanceBlocks);
            values.put("deathSiteMaxClusterRadiusBlocks", deathSiteMaxClusterRadiusBlocks);
            values.put("deathSiteMaxCenterShiftOnUpdateBlocks", deathSiteMaxCenterShiftOnUpdateBlocks);
            values.put("verticalSeparationMinYGap", verticalSeparationMinYGap);
            values.put("allowMixedDeathSiteEnvironments", allowMixedDeathSiteEnvironments);
            values.put("allowUnknownDeathSiteEnvironmentFallback", allowUnknownDeathSiteEnvironmentFallback);
            values.put("generalLandmarkMaxMergeDistanceBlocks", generalLandmarkMaxMergeDistanceBlocks);
            values.put("generalLandmarkMaxClusterRadiusBlocks", generalLandmarkMaxClusterRadiusBlocks);
            values.put("generalLandmarkMaxCenterShiftOnUpdateBlocks", generalLandmarkMaxCenterShiftOnUpdateBlocks);
            values.put("portalLandmarkMaxMergeDistanceBlocks", portalLandmarkMaxMergeDistanceBlocks);
            values.put("portalLandmarkMaxClusterRadiusBlocks", portalLandmarkMaxClusterRadiusBlocks);
            values.put("portalLandmarkMaxCenterShiftOnUpdateBlocks", portalLandmarkMaxCenterShiftOnUpdateBlocks);
            values.put("pointPlaceMaxMergeDistanceBlocks", pointPlaceMaxMergeDistanceBlocks);
            values.put("pointPlaceMaxClusterRadiusBlocks", pointPlaceMaxClusterRadiusBlocks);
            values.put("pointPlaceMaxCenterShiftOnUpdateBlocks", pointPlaceMaxCenterShiftOnUpdateBlocks);
            values.put("deletedPlaceSuppressionEnabled", deletedPlaceSuppressionEnabled);
            values.put("deletedPlaceSuppressionDays", deletedPlaceSuppressionDays);
            values.put("spacing", spacing.toMap());
            return values;
        }

        public static final class Spacing implements Serializable {
            private static final long serialVersionUID = 1L;

            public boolean enabled = true;
            public Map<String, Integer> sameTypeMinDistanceBlocks = defaultSameTypeMinDistanceBlocks();
            public Map<String, Integer> anyPlaceMinDistanceBlocks = defaultAnyPlaceMinDistanceBlocks();
            public Map<String, Integer> mergeDistanceBlocks = defaultMergeDistanceBlocks();

            private void normalize() {
                sameTypeMinDistanceBlocks = normalizePlaceTypeDistances(
                        sameTypeMinDistanceBlocks,
                        defaultSameTypeMinDistanceBlocks(),
                        "generation.spacing.sameTypeMinDistanceBlocks"
                );
                anyPlaceMinDistanceBlocks = normalizePlaceTypeDistances(
                        anyPlaceMinDistanceBlocks,
                        defaultAnyPlaceMinDistanceBlocks(),
                        "generation.spacing.anyPlaceMinDistanceBlocks"
                );
                mergeDistanceBlocks = normalizePlaceTypeDistances(
                        mergeDistanceBlocks,
                        defaultMergeDistanceBlocks(),
                        "generation.spacing.mergeDistanceBlocks"
                );
                for (String typeKey : new ArrayList<>(mergeDistanceBlocks.keySet())) {
                    int sameTypeDistance = sameTypeMinDistanceBlocks.getOrDefault(typeKey, 0);
                    int mergeDistance = mergeDistanceBlocks.getOrDefault(typeKey, 0);
                    if (mergeDistance > sameTypeDistance) {
                        mergeDistanceBlocks.put(typeKey, sameTypeDistance);
                        warnClamped(
                                "generation.spacing.mergeDistanceBlocks." + typeKey,
                                mergeDistance,
                                sameTypeDistance
                        );
                    }
                }
            }

            private void apply(Map<String, Object> values) {
                enabled = bool(values, "enabled", enabled);
                sameTypeMinDistanceBlocks = parsePlaceTypeDistanceSection(
                        values,
                        "sameTypeMinDistanceBlocks",
                        sameTypeMinDistanceBlocks,
                        "generation.spacing.sameTypeMinDistanceBlocks"
                );
                anyPlaceMinDistanceBlocks = parsePlaceTypeDistanceSection(
                        values,
                        "anyPlaceMinDistanceBlocks",
                        anyPlaceMinDistanceBlocks,
                        "generation.spacing.anyPlaceMinDistanceBlocks"
                );
                mergeDistanceBlocks = parsePlaceTypeDistanceSection(
                        values,
                        "mergeDistanceBlocks",
                        mergeDistanceBlocks,
                        "generation.spacing.mergeDistanceBlocks"
                );
            }

            private Map<String, Object> toMap() {
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("enabled", enabled);
                values.put("sameTypeMinDistanceBlocks", new LinkedHashMap<>(sameTypeMinDistanceBlocks));
                values.put("anyPlaceMinDistanceBlocks", new LinkedHashMap<>(anyPlaceMinDistanceBlocks));
                values.put("mergeDistanceBlocks", new LinkedHashMap<>(mergeDistanceBlocks));
                return values;
            }

            public int sameTypeMinDistanceBlocks(PlaceType placeType) {
                return distanceFor(sameTypeMinDistanceBlocks, placeType);
            }

            public int anyPlaceMinDistanceBlocks(PlaceType placeType) {
                return distanceFor(anyPlaceMinDistanceBlocks, placeType);
            }

            public int mergeDistanceBlocks(PlaceType placeType) {
                return distanceFor(mergeDistanceBlocks, placeType);
            }

            private static int distanceFor(Map<String, Integer> values, PlaceType placeType) {
                PlaceType resolved = placeType == null ? PlaceType.UNKNOWN : placeType;
                return Math.max(0, values == null ? 0 : values.getOrDefault(resolved.name(), 0));
            }

            private static Map<String, Integer> defaultSameTypeMinDistanceBlocks() {
                Map<String, Integer> values = new LinkedHashMap<>();
                values.put(PlaceType.GENERAL_LANDMARK.name(), 256);
                return values;
            }

            private static Map<String, Integer> defaultAnyPlaceMinDistanceBlocks() {
                Map<String, Integer> values = new LinkedHashMap<>();
                values.put(PlaceType.GENERAL_LANDMARK.name(), 96);
                return values;
            }

            private static Map<String, Integer> defaultMergeDistanceBlocks() {
                Map<String, Integer> values = new LinkedHashMap<>();
                values.put(PlaceType.GENERAL_LANDMARK.name(), 64);
                return values;
            }

            private static Map<String, Integer> parsePlaceTypeDistanceSection(
                    Map<String, Object> values,
                    String key,
                    Map<String, Integer> fallback,
                    String configPath
            ) {
                Object raw = values.get(key);
                if (raw == null) {
                    return fallback == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fallback);
                }
                if (!(raw instanceof Map<?, ?> map)) {
                    warnFixed("Config section '" + configPath + "' must be an object; using defaults.");
                    return fallback == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fallback);
                }

                Map<String, Integer> result = fallback == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fallback);
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String typeKey)) {
                        warnFixed("Config section '" + configPath + "' has a non-string key; ignored.");
                        continue;
                    }
                    PlaceType type = configuredPlaceType(typeKey, configPath);
                    if (type == PlaceType.UNKNOWN) {
                        continue;
                    }
                    if (!(entry.getValue() instanceof Number number) || !finiteNumber(number)) {
                        warnFixed("Config value '" + configPath + "." + typeKey + "' must be a finite number; using default.");
                        continue;
                    }
                    result.put(type.name(), atLeast(configPath + "." + type.name(), number.intValue(), 0));
                }
                return result;
            }

            private static Map<String, Integer> normalizePlaceTypeDistances(
                    Map<String, Integer> values,
                    Map<String, Integer> defaults,
                    String configPath
            ) {
                Map<String, Integer> result = new LinkedHashMap<>(defaults == null ? Map.of() : defaults);
                if (values != null) {
                    for (Map.Entry<String, Integer> entry : values.entrySet()) {
                        PlaceType type = configuredPlaceType(entry.getKey(), configPath);
                        if (type == PlaceType.UNKNOWN) {
                            continue;
                        }
                        int value = entry.getValue() == null ? result.getOrDefault(type.name(), 0) : entry.getValue();
                        result.put(type.name(), atLeast(configPath + "." + type.name(), value, 0));
                    }
                }
                return result;
            }
        }
    }

    public static final class Thresholds implements Serializable {
        private static final long serialVersionUID = 1L;

        public double namedPlaceScore = 25.0;
        public double rarePlaceScore = 75.0;
        public double legendaryPlaceScore = 150.0;
        public double mythicPlaceScore = 300.0;
        public int minimumUniqueVisitors = 1;
        public int minimumEvents = 3;

        private void normalize() {
            namedPlaceScore = atLeast("thresholds.namedPlaceScore", namedPlaceScore, 0.0);
            rarePlaceScore = atLeast("thresholds.rarePlaceScore", rarePlaceScore, namedPlaceScore);
            legendaryPlaceScore = atLeast("thresholds.legendaryPlaceScore", legendaryPlaceScore, rarePlaceScore);
            mythicPlaceScore = atLeast("thresholds.mythicPlaceScore", mythicPlaceScore, legendaryPlaceScore);
            minimumUniqueVisitors = atLeast("thresholds.minimumUniqueVisitors", minimumUniqueVisitors, 1);
            minimumEvents = atLeast("thresholds.minimumEvents", minimumEvents, 1);
        }

        private void apply(Map<String, Object> values) {
            namedPlaceScore = decimal(values, "namedPlaceScore", namedPlaceScore);
            rarePlaceScore = decimal(values, "rarePlaceScore", rarePlaceScore);
            legendaryPlaceScore = decimal(values, "legendaryPlaceScore", legendaryPlaceScore);
            mythicPlaceScore = decimal(values, "mythicPlaceScore", mythicPlaceScore);
            minimumUniqueVisitors = integer(values, "minimumUniqueVisitors", minimumUniqueVisitors);
            minimumEvents = integer(values, "minimumEvents", minimumEvents);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("namedPlaceScore", namedPlaceScore);
            values.put("rarePlaceScore", rarePlaceScore);
            values.put("legendaryPlaceScore", legendaryPlaceScore);
            values.put("mythicPlaceScore", mythicPlaceScore);
            values.put("minimumUniqueVisitors", minimumUniqueVisitors);
            values.put("minimumEvents", minimumEvents);
            return values;
        }
    }

    public static final class RequiredCounts implements Serializable {
        private static final long serialVersionUID = 1L;

        public int playerDeathsForDeathSite = 5;
        public int hostileKillsForBattlefield = 8;
        public int passiveKillsForSlaughterField = 30;
        public int pvpDeathsForPvpArena = 5;
        public int valuableBlocksForMiningSite = 5;
        public int portalUsesForLandmark = 16;
        public int visitsForGeneralLandmark = 80;
        public int blocksPlacedForSettlementCandidate = 250;
        public int respawnPointsForSettlementCandidate = 3;
        public int bossKillsForBossSite = 1;
        public int petDeathsForPetMemorial = 1;
        public int namedMobDeathsForMemorial = 1;
        public int raidWinsForRaidSite = 1;
        public int firstDimensionEntryForDiscovery = 1;
        public int firstMajorDiscovery = 1;

        private void normalize() {
            playerDeathsForDeathSite = atLeast("requiredCounts.playerDeathsForDeathSite", playerDeathsForDeathSite, 1);
            hostileKillsForBattlefield = atLeast("requiredCounts.hostileKillsForBattlefield", hostileKillsForBattlefield, 1);
            passiveKillsForSlaughterField = atLeast("requiredCounts.passiveKillsForSlaughterField", passiveKillsForSlaughterField, 1);
            pvpDeathsForPvpArena = atLeast("requiredCounts.pvpDeathsForPvpArena", pvpDeathsForPvpArena, 1);
            valuableBlocksForMiningSite = atLeast("requiredCounts.valuableBlocksForMiningSite", valuableBlocksForMiningSite, 1);
            portalUsesForLandmark = atLeast("requiredCounts.portalUsesForLandmark", portalUsesForLandmark, 1);
            visitsForGeneralLandmark = atLeast("requiredCounts.visitsForGeneralLandmark", visitsForGeneralLandmark, 1);
            blocksPlacedForSettlementCandidate = atLeast("requiredCounts.blocksPlacedForSettlementCandidate", blocksPlacedForSettlementCandidate, 1);
            respawnPointsForSettlementCandidate = atLeast("requiredCounts.respawnPointsForSettlementCandidate", respawnPointsForSettlementCandidate, 1);
            bossKillsForBossSite = atLeast("requiredCounts.bossKillsForBossSite", bossKillsForBossSite, 1);
            petDeathsForPetMemorial = atLeast("requiredCounts.petDeathsForPetMemorial", petDeathsForPetMemorial, 1);
            namedMobDeathsForMemorial = atLeast("requiredCounts.namedMobDeathsForMemorial", namedMobDeathsForMemorial, 1);
            raidWinsForRaidSite = atLeast("requiredCounts.raidWinsForRaidSite", raidWinsForRaidSite, 1);
            firstDimensionEntryForDiscovery = atLeast("requiredCounts.firstDimensionEntryForDiscovery", firstDimensionEntryForDiscovery, 1);
            firstMajorDiscovery = atLeast("requiredCounts.firstMajorDiscovery", firstMajorDiscovery, 1);
        }

        private void apply(Map<String, Object> values) {
            playerDeathsForDeathSite = integer(values, "playerDeathsForDeathSite", playerDeathsForDeathSite);
            hostileKillsForBattlefield = integer(values, "hostileKillsForBattlefield", hostileKillsForBattlefield);
            passiveKillsForSlaughterField = integer(values, "passiveKillsForSlaughterField", passiveKillsForSlaughterField);
            pvpDeathsForPvpArena = integer(values, "pvpDeathsForPvpArena", pvpDeathsForPvpArena);
            valuableBlocksForMiningSite = integer(values, "valuableBlocksForMiningSite", valuableBlocksForMiningSite);
            portalUsesForLandmark = integer(values, "portalUsesForLandmark", portalUsesForLandmark);
            visitsForGeneralLandmark = integer(values, "visitsForGeneralLandmark", visitsForGeneralLandmark);
            blocksPlacedForSettlementCandidate = integer(values, "blocksPlacedForSettlementCandidate", blocksPlacedForSettlementCandidate);
            respawnPointsForSettlementCandidate = integer(values, "respawnPointsForSettlementCandidate", respawnPointsForSettlementCandidate);
            bossKillsForBossSite = integer(values, "bossKillsForBossSite", bossKillsForBossSite);
            petDeathsForPetMemorial = integer(values, "petDeathsForPetMemorial", petDeathsForPetMemorial);
            namedMobDeathsForMemorial = integer(values, "namedMobDeathsForMemorial", namedMobDeathsForMemorial);
            raidWinsForRaidSite = integer(values, "raidWinsForRaidSite", raidWinsForRaidSite);
            firstDimensionEntryForDiscovery = integer(values, "firstDimensionEntryForDiscovery", firstDimensionEntryForDiscovery);
            firstMajorDiscovery = integer(values, "firstMajorDiscovery", firstMajorDiscovery);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("playerDeathsForDeathSite", playerDeathsForDeathSite);
            values.put("hostileKillsForBattlefield", hostileKillsForBattlefield);
            values.put("passiveKillsForSlaughterField", passiveKillsForSlaughterField);
            values.put("pvpDeathsForPvpArena", pvpDeathsForPvpArena);
            values.put("valuableBlocksForMiningSite", valuableBlocksForMiningSite);
            values.put("portalUsesForLandmark", portalUsesForLandmark);
            values.put("visitsForGeneralLandmark", visitsForGeneralLandmark);
            values.put("blocksPlacedForSettlementCandidate", blocksPlacedForSettlementCandidate);
            values.put("respawnPointsForSettlementCandidate", respawnPointsForSettlementCandidate);
            values.put("bossKillsForBossSite", bossKillsForBossSite);
            values.put("petDeathsForPetMemorial", petDeathsForPetMemorial);
            values.put("namedMobDeathsForMemorial", namedMobDeathsForMemorial);
            values.put("raidWinsForRaidSite", raidWinsForRaidSite);
            values.put("firstDimensionEntryForDiscovery", firstDimensionEntryForDiscovery);
            values.put("firstMajorDiscovery", firstMajorDiscovery);
            return values;
        }
    }

    public static final class ScoreThresholds implements Serializable {
        private static final long serialVersionUID = 1L;

        public double deathSite = 100.0;
        public double battlefield = 56.0;
        public double slaughterField = 180.0;
        public double pvpArena = 100.0;
        public double miningSite = 100.0;
        public double portalLandmark = 140.0;
        public double generalLandmark = 240.0;
        public double settlement = 250.0;
        public double firstDiscovery = 20.0;
        public double bossSite = 30.0;
        public double petMemorial = 20.0;
        public double namedMobMemorial = 20.0;
        public double raidSite = 40.0;
        public double dimensionThreshold = 30.0;

        private void normalize() {
            deathSite = atLeast("scoreThresholds.deathSite", deathSite, 0.0);
            battlefield = atLeast("scoreThresholds.battlefield", battlefield, 0.0);
            slaughterField = atLeast("scoreThresholds.slaughterField", slaughterField, 0.0);
            pvpArena = atLeast("scoreThresholds.pvpArena", pvpArena, 0.0);
            miningSite = atLeast("scoreThresholds.miningSite", miningSite, 0.0);
            portalLandmark = atLeast("scoreThresholds.portalLandmark", portalLandmark, 0.0);
            generalLandmark = atLeast("scoreThresholds.generalLandmark", generalLandmark, 0.0);
            settlement = atLeast("scoreThresholds.settlement", settlement, 0.0);
            firstDiscovery = atLeast("scoreThresholds.firstDiscovery", firstDiscovery, 0.0);
            bossSite = atLeast("scoreThresholds.bossSite", bossSite, 0.0);
            petMemorial = atLeast("scoreThresholds.petMemorial", petMemorial, 0.0);
            namedMobMemorial = atLeast("scoreThresholds.namedMobMemorial", namedMobMemorial, 0.0);
            raidSite = atLeast("scoreThresholds.raidSite", raidSite, 0.0);
            dimensionThreshold = atLeast("scoreThresholds.dimensionThreshold", dimensionThreshold, 0.0);
        }

        private void apply(Map<String, Object> values) {
            deathSite = decimal(values, "deathSite", deathSite);
            battlefield = decimal(values, "battlefield", battlefield);
            slaughterField = decimal(values, "slaughterField", slaughterField);
            pvpArena = decimal(values, "pvpArena", pvpArena);
            miningSite = decimal(values, "miningSite", miningSite);
            portalLandmark = decimal(values, "portalLandmark", portalLandmark);
            generalLandmark = decimal(values, "generalLandmark", generalLandmark);
            settlement = decimal(values, "settlement", settlement);
            firstDiscovery = decimal(values, "firstDiscovery", firstDiscovery);
            bossSite = decimal(values, "bossSite", bossSite);
            petMemorial = decimal(values, "petMemorial", petMemorial);
            namedMobMemorial = decimal(values, "namedMobMemorial", namedMobMemorial);
            raidSite = decimal(values, "raidSite", raidSite);
            dimensionThreshold = decimal(values, "dimensionThreshold", dimensionThreshold);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("deathSite", deathSite);
            values.put("battlefield", battlefield);
            values.put("slaughterField", slaughterField);
            values.put("pvpArena", pvpArena);
            values.put("miningSite", miningSite);
            values.put("portalLandmark", portalLandmark);
            values.put("generalLandmark", generalLandmark);
            values.put("settlement", settlement);
            values.put("firstDiscovery", firstDiscovery);
            values.put("bossSite", bossSite);
            values.put("petMemorial", petMemorial);
            values.put("namedMobMemorial", namedMobMemorial);
            values.put("raidSite", raidSite);
            values.put("dimensionThreshold", dimensionThreshold);
            return values;
        }
    }

    public static final class Display implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean discoveryMessages = true;
        public boolean showCoordinates = false;
        public boolean showRarity = true;
        public int nearbyPlaceRadiusBlocks = 128;
        public int maxPlacesInList = 20;

        private void normalize() {
            nearbyPlaceRadiusBlocks = atLeast("display.nearbyPlaceRadiusBlocks", nearbyPlaceRadiusBlocks, 16);
            maxPlacesInList = atLeast("display.maxPlacesInList", maxPlacesInList, 1);
        }

        private void apply(Map<String, Object> values) {
            discoveryMessages = bool(values, "discoveryMessages", discoveryMessages);
            showCoordinates = bool(values, "showCoordinates", showCoordinates);
            showRarity = bool(values, "showRarity", showRarity);
            nearbyPlaceRadiusBlocks = integer(values, "nearbyPlaceRadiusBlocks", nearbyPlaceRadiusBlocks);
            maxPlacesInList = integer(values, "maxPlacesInList", maxPlacesInList);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("discoveryMessages", discoveryMessages);
            values.put("showCoordinates", showCoordinates);
            values.put("showRarity", showRarity);
            values.put("nearbyPlaceRadiusBlocks", nearbyPlaceRadiusBlocks);
            values.put("maxPlacesInList", maxPlacesInList);
            return values;
        }
    }

    public static final class Performance implements Serializable {
        private static final long serialVersionUID = 1L;

        public int maxScoreEvaluationsPerTick = 4;
        public int candidateGenerationCooldownTicks = 1200;
        public int maxClusterRadiusChunks = 2;
        public int visitSampleIntervalTicks = 400;
        public int displayCheckIntervalTicks = 100;
        public int structureDiscoveryCheckIntervalTicks = 200;

        private void normalize() {
            maxScoreEvaluationsPerTick = clamp("performance.maxScoreEvaluationsPerTick", maxScoreEvaluationsPerTick, 1, 128);
            candidateGenerationCooldownTicks = atLeast("performance.candidateGenerationCooldownTicks", candidateGenerationCooldownTicks, 0);
            maxClusterRadiusChunks = clamp("performance.maxClusterRadiusChunks", maxClusterRadiusChunks, 1, 64);
            visitSampleIntervalTicks = defaultIfBelow("performance.visitSampleIntervalTicks", visitSampleIntervalTicks, 20, 400);
            displayCheckIntervalTicks = defaultIfBelow("performance.displayCheckIntervalTicks", displayCheckIntervalTicks, 20, 100);
            structureDiscoveryCheckIntervalTicks = defaultIfBelow("performance.structureDiscoveryCheckIntervalTicks", structureDiscoveryCheckIntervalTicks, 20, 200);
        }

        private void apply(Map<String, Object> values) {
            maxScoreEvaluationsPerTick = integer(values, "maxScoreEvaluationsPerTick", maxScoreEvaluationsPerTick);
            candidateGenerationCooldownTicks = integer(values, "candidateGenerationCooldownTicks", candidateGenerationCooldownTicks);
            maxClusterRadiusChunks = integer(values, "maxClusterRadiusChunks", maxClusterRadiusChunks);
            visitSampleIntervalTicks = integer(values, "visitSampleIntervalTicks", visitSampleIntervalTicks);
            displayCheckIntervalTicks = integer(values, "displayCheckIntervalTicks", displayCheckIntervalTicks);
            structureDiscoveryCheckIntervalTicks = integer(values, "structureDiscoveryCheckIntervalTicks", structureDiscoveryCheckIntervalTicks);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("maxScoreEvaluationsPerTick", maxScoreEvaluationsPerTick);
            values.put("candidateGenerationCooldownTicks", candidateGenerationCooldownTicks);
            values.put("maxClusterRadiusChunks", maxClusterRadiusChunks);
            values.put("visitSampleIntervalTicks", visitSampleIntervalTicks);
            values.put("displayCheckIntervalTicks", displayCheckIntervalTicks);
            values.put("structureDiscoveryCheckIntervalTicks", structureDiscoveryCheckIntervalTicks);
            return values;
        }
    }

    public static final class EventCollection implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean countCreativeModeEvents = true;

        private void normalize() {
            // Currently no range validation is needed.
        }

        private void apply(Map<String, Object> values) {
            countCreativeModeEvents = bool(values, "countCreativeModeEvents", countCreativeModeEvents);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("countCreativeModeEvents", countCreativeModeEvents);
            return values;
        }
    }

    public static final class AntiFarm implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean enabled = true;
        public int repeatEventCooldownSeconds = 120;
        public int samePositionRadiusBlocks = 8;
        public int maxEventsPerPlayerPerMinute = 12;
        public int maxKillsPerSmallAreaInTenMinutes = 40;
        public int minimumSamplesForVarianceChecks = 8;
        public double minimumPlayerPositionSpreadBlocks = 1.5;
        public double minimumMobDeathPositionSpreadBlocks = 1.5;
        public int sameEntityTypeKillThreshold = 16;
        public boolean strictMode = false;
        public int localSuppressionWindowSeconds = 600;
        public int strictModeMinimumKills = 10;
        public double strictModeSameEntityTypeRatio = 0.8;
        public double strictModeMaximumPlayerSpreadBlocks = 2.5;
        public double strictModeMaximumMobSpreadBlocks = 4.0;
        public boolean debugFarmRejections = false;
        public double repeatedEventScoreMultiplier = 0.25;

        private void normalize() {
            repeatEventCooldownSeconds = atLeast("antiFarm.repeatEventCooldownSeconds", repeatEventCooldownSeconds, 0);
            samePositionRadiusBlocks = atLeast("antiFarm.samePositionRadiusBlocks", samePositionRadiusBlocks, 0);
            maxEventsPerPlayerPerMinute = atLeast("antiFarm.maxEventsPerPlayerPerMinute", maxEventsPerPlayerPerMinute, 1);
            maxKillsPerSmallAreaInTenMinutes = atLeast("antiFarm.maxKillsPerSmallAreaInTenMinutes", maxKillsPerSmallAreaInTenMinutes, 1);
            minimumSamplesForVarianceChecks = atLeast("antiFarm.minimumSamplesForVarianceChecks", minimumSamplesForVarianceChecks, 2);
            minimumPlayerPositionSpreadBlocks = atLeast("antiFarm.minimumPlayerPositionSpreadBlocks", minimumPlayerPositionSpreadBlocks, 0.0);
            minimumMobDeathPositionSpreadBlocks = atLeast("antiFarm.minimumMobDeathPositionSpreadBlocks", minimumMobDeathPositionSpreadBlocks, 0.0);
            sameEntityTypeKillThreshold = atLeast("antiFarm.sameEntityTypeKillThreshold", sameEntityTypeKillThreshold, 2);
            localSuppressionWindowSeconds = clamp("antiFarm.localSuppressionWindowSeconds", localSuppressionWindowSeconds, 300, 600);
            strictModeMinimumKills = atLeast("antiFarm.strictModeMinimumKills", strictModeMinimumKills, 2);
            strictModeSameEntityTypeRatio = clamp("antiFarm.strictModeSameEntityTypeRatio", strictModeSameEntityTypeRatio, 0.0, 1.0);
            strictModeMaximumPlayerSpreadBlocks = atLeast("antiFarm.strictModeMaximumPlayerSpreadBlocks", strictModeMaximumPlayerSpreadBlocks, 0.0);
            strictModeMaximumMobSpreadBlocks = atLeast("antiFarm.strictModeMaximumMobSpreadBlocks", strictModeMaximumMobSpreadBlocks, 0.0);
            repeatedEventScoreMultiplier = clamp("antiFarm.repeatedEventScoreMultiplier", repeatedEventScoreMultiplier, 0.0, 1.0);
        }

        private void apply(Map<String, Object> values) {
            enabled = bool(values, "enabled", enabled);
            repeatEventCooldownSeconds = integer(values, "repeatEventCooldownSeconds", repeatEventCooldownSeconds);
            samePositionRadiusBlocks = integer(values, "samePositionRadiusBlocks", samePositionRadiusBlocks);
            maxEventsPerPlayerPerMinute = integer(values, "maxEventsPerPlayerPerMinute", maxEventsPerPlayerPerMinute);
            maxKillsPerSmallAreaInTenMinutes = integer(values, "maxKillsPerSmallAreaInTenMinutes", maxKillsPerSmallAreaInTenMinutes);
            minimumSamplesForVarianceChecks = integer(values, "minimumSamplesForVarianceChecks", minimumSamplesForVarianceChecks);
            minimumPlayerPositionSpreadBlocks = decimal(values, "minimumPlayerPositionSpreadBlocks", minimumPlayerPositionSpreadBlocks);
            minimumMobDeathPositionSpreadBlocks = decimal(values, "minimumMobDeathPositionSpreadBlocks", minimumMobDeathPositionSpreadBlocks);
            sameEntityTypeKillThreshold = integer(values, "sameEntityTypeKillThreshold", sameEntityTypeKillThreshold);
            strictMode = bool(values, "strictMode", strictMode);
            localSuppressionWindowSeconds = integer(values, "localSuppressionWindowSeconds", localSuppressionWindowSeconds);
            strictModeMinimumKills = integer(values, "strictModeMinimumKills", strictModeMinimumKills);
            strictModeSameEntityTypeRatio = decimal(values, "strictModeSameEntityTypeRatio", strictModeSameEntityTypeRatio);
            strictModeMaximumPlayerSpreadBlocks = decimal(values, "strictModeMaximumPlayerSpreadBlocks", strictModeMaximumPlayerSpreadBlocks);
            strictModeMaximumMobSpreadBlocks = decimal(values, "strictModeMaximumMobSpreadBlocks", strictModeMaximumMobSpreadBlocks);
            debugFarmRejections = bool(values, "debugFarmRejections", debugFarmRejections);
            repeatedEventScoreMultiplier = decimal(values, "repeatedEventScoreMultiplier", repeatedEventScoreMultiplier);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("enabled", enabled);
            values.put("repeatEventCooldownSeconds", repeatEventCooldownSeconds);
            values.put("samePositionRadiusBlocks", samePositionRadiusBlocks);
            values.put("maxEventsPerPlayerPerMinute", maxEventsPerPlayerPerMinute);
            values.put("maxKillsPerSmallAreaInTenMinutes", maxKillsPerSmallAreaInTenMinutes);
            values.put("minimumSamplesForVarianceChecks", minimumSamplesForVarianceChecks);
            values.put("minimumPlayerPositionSpreadBlocks", minimumPlayerPositionSpreadBlocks);
            values.put("minimumMobDeathPositionSpreadBlocks", minimumMobDeathPositionSpreadBlocks);
            values.put("sameEntityTypeKillThreshold", sameEntityTypeKillThreshold);
            values.put("strictMode", strictMode);
            values.put("localSuppressionWindowSeconds", localSuppressionWindowSeconds);
            values.put("strictModeMinimumKills", strictModeMinimumKills);
            values.put("strictModeSameEntityTypeRatio", strictModeSameEntityTypeRatio);
            values.put("strictModeMaximumPlayerSpreadBlocks", strictModeMaximumPlayerSpreadBlocks);
            values.put("strictModeMaximumMobSpreadBlocks", strictModeMaximumMobSpreadBlocks);
            values.put("debugFarmRejections", debugFarmRejections);
            values.put("repeatedEventScoreMultiplier", repeatedEventScoreMultiplier);
            return values;
        }
    }

    public static final class Naming implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean enabled = true;
        public boolean enableNamePacks = true;
        public String defaultStyle = NameStyle.VANILLA_ADVENTURE.idString();
        @Deprecated
        public String defaultNameStyle = NameStyle.VANILLA_ADVENTURE.idString();
        public List<String> enabledStyles = new ArrayList<>(List.of(NameStyle.VANILLA_ADVENTURE.idString()));
        public String styleSelectionMode = NameStyleSelectionMode.DEFAULT_ONLY.idString();
        public Map<String, Integer> styleWeights = defaultStyleWeights();
        public boolean allowMixedStyleTokens = false;
        public boolean existingPlacesKeepOriginalStyle = true;
        public boolean allowAutoRenameOnCauseShift = false;
        public boolean allowPerPlaceTypeStyleOverrides = true;
        public Map<String, String> placeTypeStyleOverrides = new LinkedHashMap<>();
        public String fallbackName = "Unnamed Place";
        public int maxNameLength = 48;
        public int duplicateNameAvoidanceRadiusBlocks = 192;
        public boolean allowPlayerNamesInGeneratedNames = false;

        private void normalize() {
            defaultStyle = normalizeStyle("naming.defaultStyle", defaultStyle);
            defaultNameStyle = defaultStyle;
            enabledStyles = normalizeEnabledStyles(enabledStyles, defaultStyle);
            styleSelectionMode = normalizeStyleSelectionMode(styleSelectionMode);
            styleWeights = normalizeStyleWeights(styleWeights);
            placeTypeStyleOverrides = normalizePlaceTypeStyleOverrides(placeTypeStyleOverrides);
            fallbackName = nonBlank("naming.fallbackName", fallbackName, "Unnamed Place");
            maxNameLength = clamp("naming.maxNameLength", maxNameLength, 8, 128);
            duplicateNameAvoidanceRadiusBlocks = atLeast("naming.duplicateNameAvoidanceRadiusBlocks", duplicateNameAvoidanceRadiusBlocks, 0);
        }

        private void apply(Map<String, Object> values) {
            enabled = bool(values, "enabled", enabled);
            enableNamePacks = bool(values, "enableNamePacks", enableNamePacks);
            String legacyDefaultStyle = string(values, "defaultNameStyle", defaultStyle);
            defaultStyle = string(values, "defaultStyle", legacyDefaultStyle);
            enabledStyles = stringList(values, "enabledStyles", enabledStyles);
            styleSelectionMode = string(values, "styleSelectionMode", styleSelectionMode);
            styleWeights = integerMap(values, "styleWeights", styleWeights);
            allowMixedStyleTokens = bool(values, "allowMixedStyleTokens", allowMixedStyleTokens);
            existingPlacesKeepOriginalStyle = bool(values, "existingPlacesKeepOriginalStyle", existingPlacesKeepOriginalStyle);
            allowAutoRenameOnCauseShift = bool(values, "allowAutoRenameOnCauseShift", allowAutoRenameOnCauseShift);
            allowPerPlaceTypeStyleOverrides = bool(values, "allowPerPlaceTypeStyleOverrides", allowPerPlaceTypeStyleOverrides);
            placeTypeStyleOverrides = stringMap(values, "placeTypeStyleOverrides", placeTypeStyleOverrides);
            fallbackName = string(values, "fallbackName", fallbackName);
            maxNameLength = integer(values, "maxNameLength", maxNameLength);
            duplicateNameAvoidanceRadiusBlocks = integer(values, "duplicateNameAvoidanceRadiusBlocks", duplicateNameAvoidanceRadiusBlocks);
            allowPlayerNamesInGeneratedNames = bool(values, "allowPlayerNamesInGeneratedNames", allowPlayerNamesInGeneratedNames);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("enabled", enabled);
            values.put("enableNamePacks", enableNamePacks);
            values.put("defaultStyle", defaultStyle);
            values.put("enabledStyles", enabledStyles);
            values.put("styleSelectionMode", styleSelectionMode);
            values.put("styleWeights", styleWeights);
            values.put("allowMixedStyleTokens", allowMixedStyleTokens);
            values.put("existingPlacesKeepOriginalStyle", existingPlacesKeepOriginalStyle);
            values.put("allowAutoRenameOnCauseShift", allowAutoRenameOnCauseShift);
            values.put("allowPerPlaceTypeStyleOverrides", allowPerPlaceTypeStyleOverrides);
            values.put("placeTypeStyleOverrides", placeTypeStyleOverrides);
            values.put("fallbackName", fallbackName);
            values.put("maxNameLength", maxNameLength);
            values.put("duplicateNameAvoidanceRadiusBlocks", duplicateNameAvoidanceRadiusBlocks);
            values.put("allowPlayerNamesInGeneratedNames", allowPlayerNamesInGeneratedNames);
            return values;
        }

        public List<String> normalizedEnabledStyles() {
            return normalizeEnabledStyles(enabledStyles, defaultStyle);
        }

        private static String normalizeStyle(String styleId) {
            return normalizeStyle("naming.style", styleId);
        }

        private static String normalizeStyle(String path, String styleId) {
            NameStyle style = NameStyle.fromId(styleId);
            String normalized = switch (style) {
                case VANILLA_ADVENTURE, DARK_FANTASY, COZY_SURVIVAL, EPIC_MYTHOLOGY, NEUTRAL_SERVER, FUNNY_COMMUNITY ->
                        style.idString();
                default -> NameStyle.VANILLA_ADVENTURE.idString();
            };
            if (!normalized.equalsIgnoreCase(styleId == null ? "" : styleId.trim())) {
                warnInvalidEnum("Unknown naming style '" + styleId + "' in " + path + "; using '" + normalized + "'.");
            }
            return normalized;
        }

        private static String normalizeStyleSelectionMode(String modeId) {
            NameStyleSelectionMode mode = NameStyleSelectionMode.fromId(modeId);
            if (!mode.idString().equalsIgnoreCase(modeId == null ? "" : modeId.trim())) {
                warnInvalidEnum("Unknown naming.styleSelectionMode '" + modeId + "'; using " + mode.idString() + ".");
            }
            return mode.idString();
        }

        private static List<String> normalizeEnabledStyles(List<String> styles, String defaultStyle) {
            List<String> result = new ArrayList<>();
            for (String style : styles == null ? List.<String>of() : styles) {
                String normalized = normalizeStyle("naming.enabledStyles", style);
                if (!result.contains(normalized)) {
                    result.add(normalized);
                }
            }
            String normalizedDefault = normalizeStyle("naming.defaultStyle", defaultStyle);
            if (result.isEmpty()) {
                warnFixed("Config value 'naming.enabledStyles' had no valid styles; using '" + normalizedDefault + "'.");
                result.add(normalizedDefault);
            }
            return result;
        }

        private static Map<String, Integer> defaultStyleWeights() {
            Map<String, Integer> values = new LinkedHashMap<>();
            values.put(NameStyle.VANILLA_ADVENTURE.idString(), 100);
            values.put(NameStyle.DARK_FANTASY.idString(), 0);
            values.put(NameStyle.COZY_SURVIVAL.idString(), 0);
            values.put(NameStyle.EPIC_MYTHOLOGY.idString(), 0);
            values.put(NameStyle.NEUTRAL_SERVER.idString(), 0);
            values.put(NameStyle.FUNNY_COMMUNITY.idString(), 0);
            return values;
        }

        private static Map<String, Integer> normalizeStyleWeights(Map<String, Integer> weights) {
            Map<String, Integer> result = defaultStyleWeights();
            if (weights == null) {
                return result;
            }
            for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                String style = normalizeStyle("naming.styleWeights", entry.getKey());
                int value = entry.getValue() == null ? result.getOrDefault(style, 0) : entry.getValue();
                result.put(style, atLeast("naming.styleWeights." + style, value, 0));
            }
            return result;
        }

        private static Map<String, String> normalizePlaceTypeStyleOverrides(Map<String, String> overrides) {
            Map<String, String> result = new LinkedHashMap<>();
            if (overrides == null) {
                return result;
            }
            for (Map.Entry<String, String> entry : overrides.entrySet()) {
                PlaceType placeType = configuredPlaceType(entry.getKey(), "naming.placeTypeStyleOverrides");
                if (placeType == PlaceType.UNKNOWN || placeType == PlaceType.CUSTOM) {
                    continue;
                }
                result.put(placeType.idString(), normalizeStyle("naming.placeTypeStyleOverrides." + placeType.name(), entry.getValue()));
            }
            return result;
        }
    }

    public static final class Commands implements Serializable {
        private static final long serialVersionUID = 1L;

        public int requiredOpLevel = 2;

        private void normalize() {
            requiredOpLevel = clamp("commands.requiredOpLevel", requiredOpLevel, 0, 4);
        }

        private void apply(Map<String, Object> values) {
            requiredOpLevel = integer(values, "requiredOpLevel", requiredOpLevel);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("requiredOpLevel", requiredOpLevel);
            return values;
        }
    }

    public static final class PlaceTypes implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean displayExistingWhenDisabled = true;
        public boolean allowManualCreateWhenDisabled = true;
        public Map<String, Boolean> autoGeneration = defaultAutoGeneration();

        private void normalize() {
            autoGeneration = normalizeAutoGeneration(autoGeneration);
        }

        private void apply(Map<String, Object> values) {
            displayExistingWhenDisabled = bool(values, "displayExistingWhenDisabled", displayExistingWhenDisabled);
            allowManualCreateWhenDisabled = bool(values, "allowManualCreateWhenDisabled", allowManualCreateWhenDisabled);
            Object rawAutoGeneration = values.get("autoGeneration");
            if (rawAutoGeneration instanceof Map<?, ?> map) {
                Map<String, Boolean> result = new LinkedHashMap<>(autoGeneration);
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        warnFixed("Config section 'placeTypes.autoGeneration' has a non-string key; ignored.");
                        continue;
                    }
                    PlaceType placeType = configuredPlaceType(key, "placeTypes.autoGeneration");
                    if (placeType == PlaceType.UNKNOWN) {
                        continue;
                    }
                    if (!(entry.getValue() instanceof Boolean enabled)) {
                        warnFixed("Config value 'placeTypes.autoGeneration." + key + "' must be true or false; using default.");
                        continue;
                    }
                    result.put(placeType.name(), enabled);
                }
                autoGeneration = result;
            } else if (rawAutoGeneration != null) {
                warnFixed("Config section 'placeTypes.autoGeneration' must be an object; using defaults.");
            }
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("displayExistingWhenDisabled", displayExistingWhenDisabled);
            values.put("allowManualCreateWhenDisabled", allowManualCreateWhenDisabled);
            values.put("autoGeneration", new LinkedHashMap<>(autoGeneration));
            return values;
        }

        public boolean autoGenerationEnabled(PlaceType placeType) {
            PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
            return autoGeneration.getOrDefault(resolvedType.name(), Boolean.TRUE);
        }

        public boolean shouldDisplayExisting(PlaceType placeType) {
            return displayExistingWhenDisabled || autoGenerationEnabled(placeType);
        }

        private static Map<String, Boolean> normalizeAutoGeneration(Map<String, Boolean> values) {
            Map<String, Boolean> result = defaultAutoGeneration();
            if (values != null) {
                for (Map.Entry<String, Boolean> entry : values.entrySet()) {
                    PlaceType type = configuredPlaceType(entry.getKey(), "placeTypes.autoGeneration");
                    if (type == PlaceType.UNKNOWN) {
                        continue;
                    }
                    result.put(type.name(), entry.getValue() == null ? Boolean.TRUE : entry.getValue());
                }
            }
            return result;
        }

        private static Map<String, Boolean> defaultAutoGeneration() {
            Map<String, Boolean> values = new LinkedHashMap<>();
            values.put(PlaceType.DEATH_SITE.name(), true);
            values.put(PlaceType.BATTLEFIELD.name(), true);
            values.put(PlaceType.SLAUGHTER_FIELD.name(), true);
            values.put(PlaceType.PVP_ARENA.name(), true);
            values.put(PlaceType.MINING_SITE.name(), true);
            values.put(PlaceType.PORTAL_LANDMARK.name(), true);
            values.put(PlaceType.GENERAL_LANDMARK.name(), true);
            values.put(PlaceType.FIRST_DISCOVERY.name(), true);
            values.put(PlaceType.BOSS_SITE.name(), true);
            values.put(PlaceType.PET_MEMORIAL.name(), true);
            values.put(PlaceType.NAMED_MOB_MEMORIAL.name(), true);
            values.put(PlaceType.RAID_SITE.name(), true);
            values.put(PlaceType.DIMENSION_THRESHOLD.name(), true);
            values.put(PlaceType.SETTLEMENT.name(), true);
            values.put(PlaceType.CUSTOM.name(), true);
            return values;
        }
    }

    public static final class BiomeThemes implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean enabled = true;
        public boolean useBiomeForGeneralLandmarks = true;
        public String unknownBiomeFallbackGroup = "unknown";
        public Map<String, String> mappings = defaultMappings();

        private void normalize() {
            unknownBiomeFallbackGroup = nonBlank("biomeThemes.unknownBiomeFallbackGroup", unknownBiomeFallbackGroup, "unknown").trim().toLowerCase();
            mappings = normalizeMappings(mappings);
        }

        private void apply(Map<String, Object> values) {
            enabled = bool(values, "enabled", enabled);
            useBiomeForGeneralLandmarks = bool(values, "useBiomeForGeneralLandmarks", useBiomeForGeneralLandmarks);
            unknownBiomeFallbackGroup = string(values, "unknownBiomeFallbackGroup", unknownBiomeFallbackGroup);
            mappings = stringMap(values, "mappings", mappings);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("enabled", enabled);
            values.put("useBiomeForGeneralLandmarks", useBiomeForGeneralLandmarks);
            values.put("unknownBiomeFallbackGroup", unknownBiomeFallbackGroup);
            values.put("mappings", new LinkedHashMap<>(mappings));
            return values;
        }

        private static Map<String, String> normalizeMappings(Map<String, String> values) {
            Map<String, String> result = defaultMappings();
            if (values != null) {
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    String key = nonBlank(entry.getKey(), "").trim().toLowerCase();
                    String value = nonBlank(entry.getValue(), "").trim().toLowerCase();
                    if (!key.isBlank() && !value.isBlank()) {
                        result.put(key, value);
                    }
                }
            }
            return result;
        }

        private static Map<String, String> defaultMappings() {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("minecraft:plains", "plains");
            values.put("minecraft:sunflower_plains", "plains");
            values.put("minecraft:forest", "forest");
            values.put("minecraft:flower_forest", "forest");
            values.put("minecraft:birch_forest", "birch_forest");
            values.put("minecraft:old_growth_birch_forest", "birch_forest");
            values.put("minecraft:dark_forest", "dark_forest");
            values.put("minecraft:taiga", "taiga");
            values.put("minecraft:old_growth_pine_taiga", "taiga");
            values.put("minecraft:old_growth_spruce_taiga", "taiga");
            values.put("minecraft:snowy_plains", "snowy");
            values.put("minecraft:ice_spikes", "snowy");
            values.put("minecraft:snowy_taiga", "snowy");
            values.put("minecraft:desert", "desert");
            values.put("minecraft:savanna", "savanna");
            values.put("minecraft:savanna_plateau", "savanna");
            values.put("minecraft:jungle", "jungle");
            values.put("minecraft:sparse_jungle", "jungle");
            values.put("minecraft:bamboo_jungle", "jungle");
            values.put("minecraft:swamp", "swamp");
            values.put("minecraft:mangrove_swamp", "mangrove_swamp");
            values.put("minecraft:stony_peaks", "mountain");
            values.put("minecraft:jagged_peaks", "mountain");
            values.put("minecraft:frozen_peaks", "mountain");
            values.put("minecraft:meadow", "meadow");
            values.put("minecraft:cherry_grove", "cherry_grove");
            values.put("minecraft:river", "river");
            values.put("minecraft:frozen_river", "river");
            values.put("minecraft:beach", "beach");
            values.put("minecraft:snowy_beach", "beach");
            values.put("minecraft:ocean", "ocean");
            values.put("minecraft:deep_ocean", "ocean");
            values.put("minecraft:warm_ocean", "ocean");
            values.put("minecraft:lukewarm_ocean", "ocean");
            values.put("minecraft:cold_ocean", "ocean");
            values.put("minecraft:frozen_ocean", "ocean");
            values.put("minecraft:badlands", "badlands");
            values.put("minecraft:eroded_badlands", "badlands");
            values.put("minecraft:wooded_badlands", "badlands");
            values.put("minecraft:deep_dark", "cave_or_underground");
            values.put("minecraft:dripstone_caves", "cave_or_underground");
            values.put("minecraft:lush_caves", "cave_or_underground");
            values.put("minecraft:nether_wastes", "nether");
            values.put("minecraft:crimson_forest", "nether");
            values.put("minecraft:warped_forest", "nether");
            values.put("minecraft:soul_sand_valley", "nether");
            values.put("minecraft:basalt_deltas", "nether");
            values.put("minecraft:the_end", "end");
            values.put("minecraft:end_highlands", "end");
            values.put("minecraft:end_midlands", "end");
            values.put("minecraft:small_end_islands", "end");
            values.put("minecraft:end_barrens", "end");
            return values;
        }
    }

    public static final class Notifications implements Serializable {
        private static final long serialVersionUID = 1L;

        public PlaceCreated placeCreated = new PlaceCreated();

        private void normalize() {
            if (placeCreated == null) {
                placeCreated = new PlaceCreated();
            }
            placeCreated.normalize();
        }

        private void apply(Map<String, Object> values) {
            placeCreated.apply(section(values, "placeCreated"));
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("placeCreated", placeCreated.toMap());
            return values;
        }
    }

    public static final class PlaceCreated implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean enabled = true;
        public boolean sendToAllPlayers = true;
        public boolean sendToNearbyPlayersOnly = false;
        public int nearbyRadiusBlocks = 256;
        public boolean includeCoordinates = true;
        public boolean includeDimension = false;
        public boolean includePlaceType = false;
        public boolean notifyManualCreate = false;
        public int maxNotificationsPerMinute = 5;
        public Map<String, Boolean> perType = defaultPerType();

        private void normalize() {
            nearbyRadiusBlocks = atLeast("notifications.placeCreated.nearbyRadiusBlocks", nearbyRadiusBlocks, 0);
            maxNotificationsPerMinute = atLeast("notifications.placeCreated.maxNotificationsPerMinute", maxNotificationsPerMinute, 0);
            perType = normalizePerType(perType);
        }

        private void apply(Map<String, Object> values) {
            enabled = bool(values, "enabled", enabled);
            sendToAllPlayers = bool(values, "sendToAllPlayers", sendToAllPlayers);
            sendToNearbyPlayersOnly = bool(values, "sendToNearbyPlayersOnly", sendToNearbyPlayersOnly);
            nearbyRadiusBlocks = integer(values, "nearbyRadiusBlocks", nearbyRadiusBlocks);
            includeCoordinates = bool(values, "includeCoordinates", includeCoordinates);
            includeDimension = bool(values, "includeDimension", includeDimension);
            includePlaceType = bool(values, "includePlaceType", includePlaceType);
            notifyManualCreate = bool(values, "notifyManualCreate", notifyManualCreate);
            maxNotificationsPerMinute = integer(values, "maxNotificationsPerMinute", maxNotificationsPerMinute);
            Object rawPerType = values.get("perType");
            if (rawPerType instanceof Map<?, ?> map) {
                Map<String, Boolean> result = new LinkedHashMap<>(perType);
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        warnFixed("Config section 'notifications.placeCreated.perType' has a non-string key; ignored.");
                        continue;
                    }
                    PlaceType placeType = configuredPlaceType(key, "notifications.placeCreated.perType");
                    if (placeType == PlaceType.UNKNOWN) {
                        continue;
                    }
                    if (!(entry.getValue() instanceof Boolean enabledValue)) {
                        warnFixed("Config value 'notifications.placeCreated.perType." + key + "' must be true or false; using default.");
                        continue;
                    }
                    result.put(placeType.name(), enabledValue);
                }
                perType = result;
            } else if (rawPerType != null) {
                warnFixed("Config section 'notifications.placeCreated.perType' must be an object; using defaults.");
            }
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("enabled", enabled);
            values.put("sendToAllPlayers", sendToAllPlayers);
            values.put("sendToNearbyPlayersOnly", sendToNearbyPlayersOnly);
            values.put("nearbyRadiusBlocks", nearbyRadiusBlocks);
            values.put("includeCoordinates", includeCoordinates);
            values.put("includeDimension", includeDimension);
            values.put("includePlaceType", includePlaceType);
            values.put("notifyManualCreate", notifyManualCreate);
            values.put("maxNotificationsPerMinute", maxNotificationsPerMinute);
            values.put("perType", new LinkedHashMap<>(perType));
            return values;
        }

        public boolean enabledFor(PlaceType placeType) {
            PlaceType resolved = placeType == null ? PlaceType.UNKNOWN : placeType;
            return perType.getOrDefault(resolved.name(), Boolean.TRUE);
        }

        private static Map<String, Boolean> normalizePerType(Map<String, Boolean> values) {
            Map<String, Boolean> result = defaultPerType();
            if (values != null) {
                for (Map.Entry<String, Boolean> entry : values.entrySet()) {
                    PlaceType type = configuredPlaceType(entry.getKey(), "notifications.placeCreated.perType");
                    if (type == PlaceType.UNKNOWN) {
                        continue;
                    }
                    result.put(type.name(), entry.getValue() == null ? Boolean.TRUE : entry.getValue());
                }
            }
            return result;
        }

        private static Map<String, Boolean> defaultPerType() {
            Map<String, Boolean> values = new LinkedHashMap<>();
            values.put(PlaceType.DEATH_SITE.name(), true);
            values.put(PlaceType.BATTLEFIELD.name(), true);
            values.put(PlaceType.SLAUGHTER_FIELD.name(), true);
            values.put(PlaceType.PVP_ARENA.name(), true);
            values.put(PlaceType.MINING_SITE.name(), true);
            values.put(PlaceType.PORTAL_LANDMARK.name(), true);
            values.put(PlaceType.GENERAL_LANDMARK.name(), true);
            values.put(PlaceType.FIRST_DISCOVERY.name(), true);
            values.put(PlaceType.BOSS_SITE.name(), true);
            values.put(PlaceType.PET_MEMORIAL.name(), true);
            values.put(PlaceType.NAMED_MOB_MEMORIAL.name(), true);
            values.put(PlaceType.RAID_SITE.name(), true);
            values.put(PlaceType.DIMENSION_THRESHOLD.name(), true);
            values.put(PlaceType.SETTLEMENT.name(), true);
            values.put(PlaceType.CUSTOM.name(), false);
            return values;
        }
    }

    public static final class TitleOverlay implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean enabled = true;
        public int checkIntervalTicks = 10;
        public int minTitleRadius = 24;
        public int exitPaddingBlocks = 8;
        public long globalCooldownTicks = 100L;
        public long samePlaceCooldownTicks = 12000L;
        public long generalLandmarkCooldownTicks = 24000L;
        public long teleportDelayTicks = 40L;
        public boolean showOnEnter = true;
        public boolean showOnPlaceCreated = true;
        public boolean showGeneralLandmarks = true;
        public boolean generalLandmarkOnlyIfNoHigherPriority = true;
        public int verticalToleranceBlocks = 48;

        private void normalize() {
            checkIntervalTicks = clamp("titleOverlay.checkIntervalTicks", defaultIfBelow("titleOverlay.checkIntervalTicks", checkIntervalTicks, 1, 10), 1, 200);
            minTitleRadius = atLeast("titleOverlay.minTitleRadius", minTitleRadius, 1);
            exitPaddingBlocks = atLeast("titleOverlay.exitPaddingBlocks", exitPaddingBlocks, 0);
            globalCooldownTicks = atLeast("titleOverlay.globalCooldownTicks", globalCooldownTicks, 0L);
            samePlaceCooldownTicks = atLeast("titleOverlay.samePlaceCooldownTicks", samePlaceCooldownTicks, 0L);
            generalLandmarkCooldownTicks = atLeast("titleOverlay.generalLandmarkCooldownTicks", generalLandmarkCooldownTicks, 0L);
            teleportDelayTicks = atLeast("titleOverlay.teleportDelayTicks", teleportDelayTicks, 0L);
            verticalToleranceBlocks = atLeast("titleOverlay.verticalToleranceBlocks", verticalToleranceBlocks, 0);
        }

        private void apply(Map<String, Object> values) {
            enabled = bool(values, "enabled", enabled);
            checkIntervalTicks = integer(values, "checkIntervalTicks", checkIntervalTicks);
            minTitleRadius = integer(values, "minTitleRadius", minTitleRadius);
            exitPaddingBlocks = integer(values, "exitPaddingBlocks", exitPaddingBlocks);
            globalCooldownTicks = longValue(values, "globalCooldownTicks", globalCooldownTicks);
            samePlaceCooldownTicks = longValue(values, "samePlaceCooldownTicks", samePlaceCooldownTicks);
            generalLandmarkCooldownTicks = longValue(values, "generalLandmarkCooldownTicks", generalLandmarkCooldownTicks);
            teleportDelayTicks = longValue(values, "teleportDelayTicks", teleportDelayTicks);
            showOnEnter = bool(values, "showOnEnter", showOnEnter);
            showOnPlaceCreated = bool(values, "showOnPlaceCreated", showOnPlaceCreated);
            showGeneralLandmarks = bool(values, "showGeneralLandmarks", showGeneralLandmarks);
            generalLandmarkOnlyIfNoHigherPriority = bool(values, "generalLandmarkOnlyIfNoHigherPriority", generalLandmarkOnlyIfNoHigherPriority);
            verticalToleranceBlocks = integer(values, "verticalToleranceBlocks", verticalToleranceBlocks);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("enabled", enabled);
            values.put("checkIntervalTicks", checkIntervalTicks);
            values.put("minTitleRadius", minTitleRadius);
            values.put("exitPaddingBlocks", exitPaddingBlocks);
            values.put("globalCooldownTicks", globalCooldownTicks);
            values.put("samePlaceCooldownTicks", samePlaceCooldownTicks);
            values.put("generalLandmarkCooldownTicks", generalLandmarkCooldownTicks);
            values.put("teleportDelayTicks", teleportDelayTicks);
            values.put("showOnEnter", showOnEnter);
            values.put("showOnPlaceCreated", showOnPlaceCreated);
            values.put("showGeneralLandmarks", showGeneralLandmarks);
            values.put("generalLandmarkOnlyIfNoHigherPriority", generalLandmarkOnlyIfNoHigherPriority);
            values.put("verticalToleranceBlocks", verticalToleranceBlocks);
            return values;
        }
    }

    public static final class Decay implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean enabled = false;
        public int inactiveDaysBeforeDecay = 30;
        public double dailyScoreRetention = 0.995;
        public double minimumScoreAfterDecay = 10.0;

        private void normalize() {
            inactiveDaysBeforeDecay = atLeast("decay.inactiveDaysBeforeDecay", inactiveDaysBeforeDecay, 1);
            dailyScoreRetention = clamp("decay.dailyScoreRetention", dailyScoreRetention, 0.0, 1.0);
            minimumScoreAfterDecay = atLeast("decay.minimumScoreAfterDecay", minimumScoreAfterDecay, 0.0);
        }

        private void apply(Map<String, Object> values) {
            enabled = bool(values, "enabled", enabled);
            inactiveDaysBeforeDecay = integer(values, "inactiveDaysBeforeDecay", inactiveDaysBeforeDecay);
            dailyScoreRetention = decimal(values, "dailyScoreRetention", dailyScoreRetention);
            minimumScoreAfterDecay = decimal(values, "minimumScoreAfterDecay", minimumScoreAfterDecay);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("enabled", enabled);
            values.put("inactiveDaysBeforeDecay", inactiveDaysBeforeDecay);
            values.put("dailyScoreRetention", dailyScoreRetention);
            values.put("minimumScoreAfterDecay", minimumScoreAfterDecay);
            return values;
        }
    }

    public static final class CandidateDecay implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean enabled = true;
        public long intervalTicks = 24000L;
        public long gracePeriodTicks = 1728000L;
        public double baseDecayPerInterval = 1.0;
        public double minCandidateScore = 0.0;
        public boolean pruneBelowScore = true;
        public double pruneThreshold = 0.1;
        public boolean debugLogging = false;
        public Map<String, Double> typeMultipliers = defaultTypeMultipliers();

        public double typeMultiplier(PlaceType placeType) {
            PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
            return typeMultipliers.getOrDefault(resolvedType.name(), 0.0);
        }

        private void normalize() {
            intervalTicks = defaultIfBelow("candidateDecay.intervalTicks", intervalTicks, 20L, 24000L);
            gracePeriodTicks = atLeast("candidateDecay.gracePeriodTicks", gracePeriodTicks, 0L);
            baseDecayPerInterval = atLeast("candidateDecay.baseDecayPerInterval", baseDecayPerInterval, 0.0);
            minCandidateScore = atLeast("candidateDecay.minCandidateScore", minCandidateScore, 0.0);
            pruneThreshold = atLeast("candidateDecay.pruneThreshold", pruneThreshold, 0.0);
            if (pruneThreshold < minCandidateScore) {
                warn("Config value 'candidateDecay.pruneThreshold' is lower than minCandidateScore; pruning may be effectively disabled.");
            }
            typeMultipliers = normalizeTypeMultipliers(typeMultipliers);
        }

        private void apply(Map<String, Object> values) {
            enabled = bool(values, "enabled", enabled);
            intervalTicks = longValue(values, "intervalTicks", intervalTicks);
            gracePeriodTicks = longValue(values, "gracePeriodTicks", gracePeriodTicks);
            baseDecayPerInterval = decimal(values, "baseDecayPerInterval", baseDecayPerInterval);
            minCandidateScore = decimal(values, "minCandidateScore", minCandidateScore);
            pruneBelowScore = bool(values, "pruneBelowScore", pruneBelowScore);
            pruneThreshold = decimal(values, "pruneThreshold", pruneThreshold);
            debugLogging = bool(values, "debugLogging", debugLogging);
            Object rawMultipliers = values.get("typeMultipliers");
            if (rawMultipliers instanceof Map<?, ?> map) {
                Map<String, Double> result = new LinkedHashMap<>(typeMultipliers);
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        warnFixed("Config section 'candidateDecay.typeMultipliers' has a non-string key; ignored.");
                        continue;
                    }
                    PlaceType type = configuredPlaceType(key, "candidateDecay.typeMultipliers");
                    if (type == PlaceType.UNKNOWN) {
                        continue;
                    }
                    if (!(entry.getValue() instanceof Number number) || !finiteNumber(number)) {
                        warnFixed("Config value 'candidateDecay.typeMultipliers." + key + "' must be a finite number; using default.");
                        continue;
                    }
                    result.put(type.name(), atLeast("candidateDecay.typeMultipliers." + type.name(), number.doubleValue(), 0.0));
                }
                typeMultipliers = result;
            } else if (rawMultipliers != null) {
                warnFixed("Config section 'candidateDecay.typeMultipliers' must be an object; using defaults.");
            }
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("enabled", enabled);
            values.put("intervalTicks", intervalTicks);
            values.put("gracePeriodTicks", gracePeriodTicks);
            values.put("baseDecayPerInterval", baseDecayPerInterval);
            values.put("minCandidateScore", minCandidateScore);
            values.put("pruneBelowScore", pruneBelowScore);
            values.put("pruneThreshold", pruneThreshold);
            values.put("debugLogging", debugLogging);
            values.put("typeMultipliers", new LinkedHashMap<>(typeMultipliers));
            return values;
        }

        private static Map<String, Double> normalizeTypeMultipliers(Map<String, Double> values) {
            Map<String, Double> result = defaultTypeMultipliers();
            if (values != null) {
                for (Map.Entry<String, Double> entry : values.entrySet()) {
                    PlaceType type = configuredPlaceType(entry.getKey(), "candidateDecay.typeMultipliers");
                    if (type == PlaceType.UNKNOWN) {
                        continue;
                    }
                    double value = entry.getValue() == null ? result.getOrDefault(type.name(), 0.0) : entry.getValue();
                    result.put(type.name(), atLeast("candidateDecay.typeMultipliers." + type.name(), value, 0.0));
                }
            }
            return result;
        }

        private static Map<String, Double> defaultTypeMultipliers() {
            Map<String, Double> values = new LinkedHashMap<>();
            values.put(PlaceType.DEATH_SITE.name(), 1.0);
            values.put(PlaceType.BATTLEFIELD.name(), 1.0);
            values.put(PlaceType.SLAUGHTER_FIELD.name(), 1.0);
            values.put(PlaceType.PVP_ARENA.name(), 0.8);
            values.put(PlaceType.MINING_SITE.name(), 1.0);
            values.put(PlaceType.PORTAL_LANDMARK.name(), 0.5);
            values.put(PlaceType.GENERAL_LANDMARK.name(), 1.2);
            values.put(PlaceType.SETTLEMENT.name(), 0.3);
            values.put(PlaceType.FIRST_DISCOVERY.name(), 0.0);
            values.put(PlaceType.BOSS_SITE.name(), 0.0);
            values.put(PlaceType.PET_MEMORIAL.name(), 0.0);
            values.put(PlaceType.NAMED_MOB_MEMORIAL.name(), 0.0);
            values.put(PlaceType.RAID_SITE.name(), 0.4);
            values.put(PlaceType.DIMENSION_THRESHOLD.name(), 0.5);
            values.put(PlaceType.CUSTOM.name(), 0.0);
            return values;
        }
    }

    public static final class Journal implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean enabled = true;
        public String visibilityMode = "ALL_KNOWN";
        public String managementMode = "SINGLEPLAYER_OWNER_AND_OP";
        public int pageSize = 20;
        public boolean showExactCoordinates = true;
        public boolean allowTeleportForOps = true;
        public int maxManualNameLength = 64;

        public VisibilityMode visibilityMode() {
            return VisibilityMode.fromId(visibilityMode);
        }

        public ManagementMode managementMode() {
            return ManagementMode.fromId(managementMode);
        }

        private void normalize() {
            visibilityMode = visibilityMode().name();
            managementMode = managementMode().name();
            pageSize = defaultIfOutside("journal.pageSize", pageSize, 5, 100, 20);
            maxManualNameLength = defaultIfOutside("journal.maxManualNameLength", maxManualNameLength, 8, 128, 64);
        }

        private void apply(Map<String, Object> values) {
            enabled = bool(values, "enabled", enabled);
            visibilityMode = string(values, "visibilityMode", visibilityMode);
            managementMode = string(values, "managementMode", managementMode);
            pageSize = integer(values, "pageSize", pageSize);
            showExactCoordinates = bool(values, "showExactCoordinates", showExactCoordinates);
            allowTeleportForOps = bool(values, "allowTeleportForOps", allowTeleportForOps);
            maxManualNameLength = integer(values, "maxManualNameLength", maxManualNameLength);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("enabled", enabled);
            values.put("visibilityMode", visibilityMode);
            values.put("managementMode", managementMode);
            values.put("pageSize", pageSize);
            values.put("showExactCoordinates", showExactCoordinates);
            values.put("allowTeleportForOps", allowTeleportForOps);
            values.put("maxManualNameLength", maxManualNameLength);
            return values;
        }

        public enum VisibilityMode {
            ALL_KNOWN,
            VISITED_BY_PLAYER;

            private static VisibilityMode fromId(String id) {
                if (id != null) {
                    for (VisibilityMode mode : values()) {
                        if (mode.name().equalsIgnoreCase(id.trim())) {
                            return mode;
                        }
                    }
                }
                warnInvalidEnum("Unknown journal.visibilityMode '" + id + "'; using ALL_KNOWN.");
                return ALL_KNOWN;
            }
        }

        public enum ManagementMode {
            SINGLEPLAYER_OWNER_AND_OP,
            OP_ONLY,
            ALL_PLAYERS,
            DISABLED;

            private static ManagementMode fromId(String id) {
                if (id != null) {
                    for (ManagementMode mode : values()) {
                        if (mode.name().equalsIgnoreCase(id.trim())) {
                            return mode;
                        }
                    }
                }
                warnInvalidEnum("Unknown journal.managementMode '" + id + "'; using SINGLEPLAYER_OWNER_AND_OP.");
                return SINGLEPLAYER_OWNER_AND_OP;
            }
        }
    }

    public static final class Permissions implements Serializable {
        private static final long serialVersionUID = 1L;

        public int reloadPermissionLevel = 2;
        public int exportPermissionLevel = 2;
        public boolean allowClientPlaceQueries = true;

        private void normalize() {
            reloadPermissionLevel = clamp("permissions.reloadPermissionLevel", reloadPermissionLevel, 0, 4);
            exportPermissionLevel = clamp("permissions.exportPermissionLevel", exportPermissionLevel, 0, 4);
        }

        private void apply(Map<String, Object> values) {
            reloadPermissionLevel = integer(values, "reloadPermissionLevel", reloadPermissionLevel);
            exportPermissionLevel = integer(values, "exportPermissionLevel", exportPermissionLevel);
            allowClientPlaceQueries = bool(values, "allowClientPlaceQueries", allowClientPlaceQueries);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("reloadPermissionLevel", reloadPermissionLevel);
            values.put("exportPermissionLevel", exportPermissionLevel);
            values.put("allowClientPlaceQueries", allowClientPlaceQueries);
            return values;
        }
    }

    public static final class Debug implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean enabled = false;
        public boolean logConfigLoad = true;
        public boolean logGeneratedNames = false;
        public boolean logScoring = false;
        public boolean namingVerbose = false;
        public boolean writeDebugExports = false;
        public boolean useTestingThresholds = false;

        private void normalize() {
        }

        private void apply(Map<String, Object> values) {
            enabled = bool(values, "enabled", enabled);
            logConfigLoad = bool(values, "logConfigLoad", logConfigLoad);
            logGeneratedNames = bool(values, "logGeneratedNames", logGeneratedNames);
            logScoring = bool(values, "logScoring", logScoring);
            namingVerbose = bool(values, "namingVerbose", namingVerbose);
            writeDebugExports = bool(values, "writeDebugExports", writeDebugExports);
            useTestingThresholds = bool(values, "useTestingThresholds", useTestingThresholds);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("enabled", enabled);
            values.put("logConfigLoad", logConfigLoad);
            values.put("logGeneratedNames", logGeneratedNames);
            values.put("logScoring", logScoring);
            values.put("namingVerbose", namingVerbose);
            values.put("writeDebugExports", writeDebugExports);
            values.put("useTestingThresholds", useTestingThresholds);
            return values;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> values, String key) {
        Object value = values.get(key);

        if (value == null) {
            return Collections.emptyMap();
        }

        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }

        warnFixed("Config section '" + key + "' must be an object; using defaults.");
        return Collections.emptyMap();
    }

    private static boolean bool(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key);

        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }

        warnFixed("Config value '" + key + "' must be a boolean; using default " + fallback + ".");
        return fallback;
    }

    private static int integer(Map<String, Object> values, String key, int fallback) {
        Object value = values.get(key);

        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            if (!finiteNumber(number)) {
                warnFixed("Config value '" + key + "' must be a finite integer; using default " + fallback + ".");
                return fallback;
            }
            return number.intValue();
        }

        warnFixed("Config value '" + key + "' must be an integer; using default " + fallback + ".");
        return fallback;
    }

    private static long longValue(Map<String, Object> values, String key, long fallback) {
        Object value = values.get(key);

        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            if (!finiteNumber(number)) {
                warnFixed("Config value '" + key + "' must be a finite integer; using default " + fallback + ".");
                return fallback;
            }
            return number.longValue();
        }

        warnFixed("Config value '" + key + "' must be an integer; using default " + fallback + ".");
        return fallback;
    }

    private static double decimal(Map<String, Object> values, String key, double fallback) {
        Object value = values.get(key);

        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            double result = number.doubleValue();
            if (Double.isFinite(result)) {
                return result;
            }
            warnFixed("Config value '" + key + "' must be a finite number; using default " + fallback + ".");
            return fallback;
        }

        warnFixed("Config value '" + key + "' must be a number; using default " + fallback + ".");
        return fallback;
    }

    private static String string(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key);

        if (value == null) {
            return fallback;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }

        warnFixed("Config value '" + key + "' must be a string; using default '" + fallback + "'.");
        return fallback;
    }

    private static List<String> stringList(Map<String, Object> values, String key, List<String> fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback == null ? List.of() : new ArrayList<>(fallback);
        }
        if (!(value instanceof List<?> list)) {
            warnFixed("Config value '" + key + "' must be an array of strings; using defaults.");
            return fallback == null ? List.of() : new ArrayList<>(fallback);
        }

        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String stringItem)) {
                warnFixed("Config value '" + key + "' must contain only strings; invalid item ignored.");
                continue;
            }
            result.add(stringItem);
        }
        return result;
    }

    private static Map<String, Integer> integerMap(Map<String, Object> values, String key, Map<String, Integer> fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fallback);
        }
        if (!(value instanceof Map<?, ?> map)) {
            warnFixed("Config value '" + key + "' must be an object with integer values; using defaults.");
            return fallback == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fallback);
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String stringKey) || !(entry.getValue() instanceof Number number) || !finiteNumber(number)) {
                warnFixed("Config value '" + key + "' must contain string keys and finite integer values; invalid entry ignored.");
                continue;
            }
            result.put(stringKey, number.intValue());
        }
        return result;
    }

    private static Map<String, String> stringMap(Map<String, Object> values, String key, Map<String, String> fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fallback);
        }
        if (!(value instanceof Map<?, ?> map)) {
            warnFixed("Config value '" + key + "' must be an object with string values; using defaults.");
            return fallback == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fallback);
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String stringKey) || !(entry.getValue() instanceof String stringValue)) {
                warnFixed("Config value '" + key + "' must contain string keys and string values; invalid entry ignored.");
                continue;
            }
            result.put(stringKey, stringValue);
        }
        return result;
    }

    private static void warn(String message) {
        LOAD_WARNINGS.get().add(message);
    }

    private static void warnFixed(String message) {
        LOAD_VALIDATION.get().fixedValues++;
        warn(message);
    }

    private static void warnClamped(String path, Object value, Object fixed) {
        ValidationStats stats = LOAD_VALIDATION.get();
        stats.fixedValues++;
        stats.clampedValues++;
        warn("Config value '" + path + "' was " + value + "; clamped to " + fixed + ".");
    }

    private static void warnDefaulted(String path, Object value, Object fixed, String reason) {
        ValidationStats stats = LOAD_VALIDATION.get();
        stats.fixedValues++;
        warn("Config value '" + path + "' was " + value + " but " + reason + "; using safe default " + fixed + ".");
    }

    private static void warnInvalidEnum(String message) {
        ValidationStats stats = LOAD_VALIDATION.get();
        stats.fixedValues++;
        stats.invalidEnums++;
        warn(message);
    }

    private static void warnUnknownPlaceType(String message) {
        ValidationStats stats = LOAD_VALIDATION.get();
        stats.fixedValues++;
        stats.unknownPlaceTypes++;
        warn(message);
    }

    private static boolean finiteNumber(Number number) {
        if (number instanceof Double || number instanceof Float) {
            return Double.isFinite(number.doubleValue());
        }
        return number != null;
    }

    private static String nonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private static String nonBlank(String path, String value, String fallback) {
        String fixed = nonBlank(value, fallback);
        if (!fixed.equals(value)) {
            warnFixed("Config value '" + path + "' was blank; using '" + fixed + "'.");
        }
        return fixed;
    }

    private static int atLeast(int value, int minimum) {
        return Math.max(value, minimum);
    }

    private static int atLeast(String path, int value, int minimum) {
        int fixed = Math.max(value, minimum);
        if (fixed != value) {
            warnClamped(path, value, fixed);
        }
        return fixed;
    }

    private static int defaultIfBelow(String path, int value, int minimum, int fallback) {
        if (value < minimum) {
            warnDefaulted(path, value, fallback, "must be at least " + minimum);
            return fallback;
        }
        return value;
    }

    private static int defaultIfOutside(String path, int value, int minimum, int maximum, int fallback) {
        if (value < minimum || value > maximum) {
            warnDefaulted(path, value, fallback, "must be between " + minimum + " and " + maximum);
            return fallback;
        }
        return value;
    }

    private static long atLeast(long value, long minimum) {
        return Math.max(value, minimum);
    }

    private static long atLeast(String path, long value, long minimum) {
        long fixed = Math.max(value, minimum);
        if (fixed != value) {
            warnClamped(path, value, fixed);
        }
        return fixed;
    }

    private static long defaultIfBelow(String path, long value, long minimum, long fallback) {
        if (value < minimum) {
            warnDefaulted(path, value, fallback, "must be at least " + minimum);
            return fallback;
        }
        return value;
    }

    private static double atLeast(double value, double minimum) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return minimum;
        }

        return Math.max(value, minimum);
    }

    private static double atLeast(String path, double value, double minimum) {
        double fixed = Double.isFinite(value) ? Math.max(value, minimum) : minimum;
        if (Double.compare(fixed, value) != 0) {
            warnClamped(path, value, fixed);
        }
        return fixed;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static int clamp(String path, int value, int minimum, int maximum) {
        int fixed = Math.max(minimum, Math.min(maximum, value));
        if (fixed != value) {
            warnClamped(path, value, fixed);
        }
        return fixed;
    }

    private static long clamp(String path, long value, long minimum, long maximum) {
        long fixed = Math.max(minimum, Math.min(maximum, value));
        if (fixed != value) {
            warnClamped(path, value, fixed);
        }
        return fixed;
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return minimum;
        }

        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(String path, double value, double minimum, double maximum) {
        double fixed = Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : minimum;
        if (Double.compare(fixed, value) != 0) {
            warnClamped(path, value, fixed);
        }
        return fixed;
    }

    private static PlaceType configuredPlaceType(String key, String configPath) {
        PlaceType type = PlaceType.fromId(key);
        if (type == PlaceType.CUSTOM && !matchesEnumId(key, PlaceType.CUSTOM.name(), PlaceType.CUSTOM.idString())) {
            warnUnknownPlaceType("Unknown place type in " + configPath + ": '" + key + "'. It was ignored.");
            return PlaceType.UNKNOWN;
        }
        if (type == PlaceType.UNKNOWN && !matchesEnumId(key, PlaceType.UNKNOWN.name(), PlaceType.UNKNOWN.idString())) {
            warnUnknownPlaceType("Unknown place type in " + configPath + ": '" + key + "'. It was ignored.");
            return PlaceType.UNKNOWN;
        }
        return type;
    }

    private static boolean matchesEnumId(String value, String enumName, String id) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.equalsIgnoreCase(enumName) || trimmed.equalsIgnoreCase(id);
    }
}
