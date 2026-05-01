package com.worldremembers.livinglegends;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FirstDiscoveryDefinitions {
    private static final List<FirstDiscoveryDefinition> VANILLA_ALPHA = List.of(
            definition(
                    "world:first_nether_entry",
                    DiscoveryTriggerType.DIMENSION_ENTRY,
                    "minecraft:the_nether",
                    28.0,
                    Map.of("dimension", "Nether")
            ),
            definition(
                    "world:first_end_entry",
                    DiscoveryTriggerType.DIMENSION_ENTRY,
                    "minecraft:the_end",
                    28.0,
                    Map.of("dimension", "End")
            ),
            definition(
                    "world:first_diamond_ore_mined",
                    DiscoveryTriggerType.BLOCK_MINED,
                    "minecraft:diamond_ore",
                    28.0,
                    Map.of("block", "Diamond Ore")
            ),
            definition(
                    "world:first_diamond_ore_mined",
                    DiscoveryTriggerType.BLOCK_MINED,
                    "minecraft:deepslate_diamond_ore",
                    28.0,
                    Map.of("block", "Diamond Ore")
            ),
            definition(
                    "world:first_ancient_debris_mined",
                    DiscoveryTriggerType.BLOCK_MINED,
                    "minecraft:ancient_debris",
                    30.0,
                    Map.of("block", "Ancient Debris")
            ),
            definition(
                    "world:first_ender_dragon_killed",
                    DiscoveryTriggerType.ENTITY_KILLED,
                    "minecraft:ender_dragon",
                    40.0,
                    Map.of("boss", "Ender Dragon")
            ),
            definition(
                    "world:first_wither_killed",
                    DiscoveryTriggerType.ENTITY_KILLED,
                    "minecraft:wither",
                    35.0,
                    Map.of("boss", "Wither")
            ),
            definition(
                    "world:first_warden_killed",
                    DiscoveryTriggerType.ENTITY_KILLED,
                    "minecraft:warden",
                    35.0,
                    Map.of("boss", "Warden")
            ),
            definition(
                    "world:first_trial_spawner_discovery",
                    DiscoveryTriggerType.BLOCK_MINED,
                    "minecraft:trial_spawner",
                    24.0,
                    Map.of("block", "Trial Spawner")
            ),
            definition(
                    "world:first_vault_discovery",
                    DiscoveryTriggerType.BLOCK_MINED,
                    "minecraft:vault",
                    24.0,
                    Map.of("block", "Vault")
            ),
            definition(
                    "world:first_stronghold_found",
                    DiscoveryTriggerType.STRUCTURE_DISCOVERED,
                    "minecraft:stronghold",
                    35.0,
                    Map.of("structure", "Stronghold"),
                    true,
                    96
            )
    );

    private FirstDiscoveryDefinitions() {
    }

    public static List<FirstDiscoveryDefinition> vanillaAlphaDefinitions() {
        return VANILLA_ALPHA;
    }

    public static List<FirstDiscoveryDefinition> structureDiscoveryDefinitions() {
        return mergedDefinitions().stream()
                .filter(definition -> definition.triggerType() == DiscoveryTriggerType.STRUCTURE_DISCOVERED)
                .toList();
    }

    public static List<FirstDiscoveryDefinition> matchingDefinitions(WorldMemoryEvent event) {
        if (event == null) {
            return List.of();
        }

        DiscoveryTriggerType triggerType = triggerTypeFor(event);
        if (triggerType == DiscoveryTriggerType.CUSTOM) {
            return List.of();
        }

        String targetId = targetIdFor(event, triggerType);
        if (targetId.isBlank()) {
            return List.of();
        }

        return mergedDefinitions().stream()
                .filter(definition -> definition.triggerType() == triggerType)
                .filter(definition -> definition.targetIdString().equals(targetId))
                .toList();
    }

    private static List<FirstDiscoveryDefinition> mergedDefinitions() {
        List<FirstDiscoveryDefinition> result = new ArrayList<>(VANILLA_ALPHA);
        Set<String> keys = new LinkedHashSet<>();
        for (FirstDiscoveryDefinition definition : VANILLA_ALPHA) {
            keys.add(definition.triggerType().idString() + "|" + definition.targetIdString());
        }
        for (FirstDiscoveryDefinition definition : WorldRemembersCompatRegistries.compatFirstDiscoveryDefinitions()) {
            String key = definition.triggerType().idString() + "|" + definition.targetIdString();
            if (keys.add(key)) {
                result.add(definition);
            }
        }
        return List.copyOf(result);
    }

    public static DiscoveryTriggerType triggerTypeFor(WorldMemoryEvent event) {
        if (event == null) {
            return DiscoveryTriggerType.CUSTOM;
        }

        return switch (event.eventType()) {
            case PLAYER_ENTERED_DIMENSION, PLAYER_FIRST_ENTERED_DIMENSION -> DiscoveryTriggerType.DIMENSION_ENTRY;
            case VALUABLE_BLOCK_MINED -> DiscoveryTriggerType.BLOCK_MINED;
            case BOSS_KILLED, BOSS_KILL -> DiscoveryTriggerType.ENTITY_KILLED;
            case STRUCTURE_DISCOVERED -> DiscoveryTriggerType.STRUCTURE_DISCOVERED;
            default -> DiscoveryTriggerType.CUSTOM;
        };
    }

    public static String targetIdFor(WorldMemoryEvent event, DiscoveryTriggerType triggerType) {
        if (event == null || triggerType == null) {
            return "";
        }

        return switch (triggerType) {
            case DIMENSION_ENTRY -> firstNonBlank(
                    event.subjectIdString(),
                    noteValue(event.note(), "to"),
                    noteValue(event.note(), "dimension")
            );
            case BLOCK_MINED -> firstNonBlank(
                    event.subjectIdString(),
                    noteValue(event.note(), "block")
            );
            case ENTITY_KILLED -> firstNonBlank(
                    event.subjectIdString(),
                    noteValue(event.note(), "boss_type"),
                    noteValue(event.note(), "target_type")
            );
            case STRUCTURE_DISCOVERED -> firstNonBlank(
                    event.structureIdString(),
                    event.subjectIdString(),
                    noteValue(event.note(), "structureId"),
                    noteValue(event.note(), "structure")
            );
            default -> "";
        };
    }

    private static FirstDiscoveryDefinition definition(
            String discoveryId,
            DiscoveryTriggerType triggerType,
            String targetId,
            double weight,
            Map<String, String> nameTokens
    ) {
        return definition(discoveryId, triggerType, targetId, weight, nameTokens, false, 32);
    }

    private static FirstDiscoveryDefinition definition(
            String discoveryId,
            DiscoveryTriggerType triggerType,
            String targetId,
            double weight,
            Map<String, String> nameTokens,
            boolean useStructureBounds,
            int fallbackRadius
    ) {
        return new FirstDiscoveryDefinition(
                discoveryId,
                triggerType,
                targetId,
                PlaceType.FIRST_DISCOVERY,
                weight,
                new LinkedHashMap<>(nameTokens),
                useStructureBounds,
                fallbackRadius
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            String normalized = WorldPos.optionalId(value);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }

        return "";
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
}
