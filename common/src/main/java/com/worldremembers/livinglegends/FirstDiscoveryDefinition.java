package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record FirstDiscoveryDefinition(
        String discoveryId,
        DiscoveryTriggerType triggerType,
        String targetId,
        PlaceType placeType,
        double weight,
        Map<String, String> nameTokens,
        boolean useStructureBounds,
        int fallbackRadius
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public FirstDiscoveryDefinition(
            String discoveryId,
            DiscoveryTriggerType triggerType,
            String targetId,
            PlaceType placeType,
            double weight,
            Map<String, String> nameTokens
    ) {
        this(discoveryId, triggerType, targetId, placeType, weight, nameTokens, false, 32);
    }

    public FirstDiscoveryDefinition {
        discoveryId = WorldPos.requireId(discoveryId, "discoveryId");
        triggerType = DiscoveryTriggerType.fromId(
                Objects.requireNonNullElse(triggerType, DiscoveryTriggerType.CUSTOM).idString()
        );
        targetId = WorldPos.requireId(targetId, "targetId");
        placeType = PlaceType.fromId(Objects.requireNonNullElse(placeType, PlaceType.FIRST_DISCOVERY).idString());
        weight = Math.max(0.0, weight);
        nameTokens = normalizedNameTokens(nameTokens);
        fallbackRadius = Math.max(0, fallbackRadius);
    }

    public String discoveryIdString() {
        return discoveryId;
    }

    public String triggerTypeIdString() {
        return triggerType.idString();
    }

    public String targetIdString() {
        return targetId;
    }

    public Map<String, String> nameTokens() {
        return Collections.unmodifiableMap(nameTokens);
    }

    private static Map<String, String> normalizedNameTokens(Map<String, String> tokens) {
        Map<String, String> result = new LinkedHashMap<>();
        if (tokens == null) {
            return result;
        }

        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            String key = WorldPos.optionalId(entry.getKey());
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (!key.isBlank() && !value.isBlank()) {
                result.put(key, value);
            }
        }

        return result;
    }
}
