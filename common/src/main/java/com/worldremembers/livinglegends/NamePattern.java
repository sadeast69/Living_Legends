package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record NamePattern(
        String id,
        String translationKey,
        Set<PlaceType> supportedPlaceTypes,
        Set<DeathSiteEnvironment> supportedEnvironments,
        double weight,
        List<NamePatternSlot> slots,
        Set<String> tags,
        String semanticRoot,
        NameCauseConstraints causeConstraints,
        Set<String> forbiddenRoles
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public NamePattern(
            String id,
            String translationKey,
            Set<PlaceType> supportedPlaceTypes,
            Set<DeathSiteEnvironment> supportedEnvironments,
            double weight,
            List<NamePatternSlot> slots,
            Set<String> tags
    ) {
        this(
                id,
                translationKey,
                supportedPlaceTypes,
                supportedEnvironments,
                weight,
                slots,
                tags,
                NameSemanticRoots.inferPatternRoot(id),
                NameCauseConstraints.none(),
                Set.of()
        );
    }

    public NamePattern(
            String id,
            String translationKey,
            Set<PlaceType> supportedPlaceTypes,
            Set<DeathSiteEnvironment> supportedEnvironments,
            double weight,
            List<NamePatternSlot> slots,
            Set<String> tags,
            String semanticRoot
    ) {
        this(
                id,
                translationKey,
                supportedPlaceTypes,
                supportedEnvironments,
                weight,
                slots,
                tags,
                semanticRoot,
                NameCauseConstraints.none(),
                Set.of()
        );
    }

    public NamePattern {
        id = WorldPos.requireId(id, "id");
        translationKey = Objects.requireNonNullElse(translationKey, "").trim();
        if (translationKey.isBlank()) {
            translationKey = "living_legends.name.pattern." + id;
        }
        supportedPlaceTypes = Collections.unmodifiableSet(normalizedPlaceTypes(supportedPlaceTypes));
        supportedEnvironments = Collections.unmodifiableSet(normalizedEnvironments(supportedEnvironments));
        weight = Math.max(0.0, weight);
        slots = Collections.unmodifiableList(new ArrayList<>(slots == null ? List.of() : slots));
        tags = Collections.unmodifiableSet(normalizedTags(tags));
        semanticRoot = NameSemanticRoots.normalize(
                semanticRoot == null || semanticRoot.isBlank()
                        ? NameSemanticRoots.inferPatternRoot(id)
                        : semanticRoot
        );
        causeConstraints = causeConstraints == null ? NameCauseConstraints.none() : causeConstraints;
        forbiddenRoles = Collections.unmodifiableSet(normalizedTags(forbiddenRoles));
    }

    public boolean supports(PlaceType placeType, DeathSiteEnvironment environment) {
        PlaceType resolvedType = placeType == null ? PlaceType.UNKNOWN : placeType;
        DeathSiteEnvironment resolvedEnvironment = environment == null ? DeathSiteEnvironment.UNKNOWN : environment;
        return (supportedPlaceTypes.isEmpty() || supportedPlaceTypes.contains(resolvedType))
                && (supportedEnvironments.isEmpty() || supportedEnvironments.contains(resolvedEnvironment));
    }

    public boolean supports(PlaceType placeType, DeathSiteEnvironment environment, PlaceCause cause) {
        return supports(placeType, environment) && causeConstraints.matchesPattern(cause);
    }

    public NamePatternSource sourceFor(DeathSiteEnvironment environment) {
        if (causeConstraints.hasExactCauseRequirement()) {
            return NamePatternSource.EXACT_CAUSE;
        }
        if (causeConstraints.hasDominantTargetRequirement()) {
            return NamePatternSource.DOMINANT_TARGET;
        }
        if (causeConstraints.hasCauseTypeRequirement()) {
            return NamePatternSource.CAUSE_TYPE;
        }
        DeathSiteEnvironment resolvedEnvironment = environment == null ? DeathSiteEnvironment.UNKNOWN : environment;
        if (!supportedEnvironments.isEmpty() && supportedEnvironments.contains(resolvedEnvironment)) {
            return NamePatternSource.PLACE_TYPE_ENVIRONMENT;
        }
        return NamePatternSource.PLACE_TYPE_GENERIC;
    }

    private static Set<PlaceType> normalizedPlaceTypes(Set<PlaceType> placeTypes) {
        Set<PlaceType> result = new LinkedHashSet<>();
        if (placeTypes == null) {
            return result;
        }
        for (PlaceType placeType : placeTypes) {
            result.add(placeType == null ? PlaceType.UNKNOWN : placeType);
        }
        return result;
    }

    private static Set<DeathSiteEnvironment> normalizedEnvironments(Set<DeathSiteEnvironment> environments) {
        Set<DeathSiteEnvironment> result = new LinkedHashSet<>();
        if (environments == null) {
            return result;
        }
        for (DeathSiteEnvironment environment : environments) {
            result.add(environment == null ? DeathSiteEnvironment.UNKNOWN : environment);
        }
        return result;
    }

    private static Set<String> normalizedTags(Set<String> tags) {
        Set<String> result = new LinkedHashSet<>();
        if (tags == null) {
            return result;
        }
        for (String tag : tags) {
            String normalized = WorldPos.optionalId(tag);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }
}
