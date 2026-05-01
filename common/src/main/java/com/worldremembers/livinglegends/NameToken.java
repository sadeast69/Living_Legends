package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record NameToken(
        String id,
        Set<String> tags,
        double weight,
        Map<NameTokenForm, String> localizedForms,
        String semanticRoot,
        NameCauseConstraints causeConstraints
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public NameToken(
            String id,
            Set<String> tags,
            double weight,
            Map<NameTokenForm, String> localizedForms
    ) {
        this(id, tags, weight, localizedForms, NameSemanticRoots.inferTokenRoot(id), NameCauseConstraints.none());
    }

    public NameToken(
            String id,
            Set<String> tags,
            double weight,
            Map<NameTokenForm, String> localizedForms,
            String semanticRoot
    ) {
        this(id, tags, weight, localizedForms, semanticRoot, NameCauseConstraints.none());
    }

    public NameToken {
        id = WorldPos.requireId(id, "id");
        tags = Collections.unmodifiableSet(normalizedTags(tags));
        weight = Math.max(0.0, weight);
        localizedForms = Collections.unmodifiableMap(normalizedForms(id, localizedForms));
        semanticRoot = NameSemanticRoots.normalize(
                semanticRoot == null || semanticRoot.isBlank()
                        ? NameSemanticRoots.inferTokenRoot(id)
                        : semanticRoot
        );
        causeConstraints = causeConstraints == null ? NameCauseConstraints.none() : causeConstraints;
    }

    public boolean hasTag(String tag) {
        String normalized = WorldPos.optionalId(tag);
        return !normalized.isBlank() && tags.contains(normalized);
    }

    public boolean supportsForm(NameTokenForm form) {
        NameTokenForm resolvedForm = form == null ? NameTokenForm.BASE : form;
        return localizedForms.containsKey(resolvedForm) || localizedForms.containsKey(NameTokenForm.BASE);
    }

    public boolean supportsCause(PlaceCause cause) {
        return causeConstraints.matchesToken(cause);
    }

    public boolean supportsCause(NameContext context) {
        return causeConstraints.matchesToken(context);
    }

    public String translationKey(NameTokenForm form) {
        NameTokenForm resolvedForm = form == null ? NameTokenForm.BASE : form;
        String key = localizedForms.get(resolvedForm);
        if (key == null || key.isBlank()) {
            key = localizedForms.get(NameTokenForm.BASE);
        }
        return key == null || key.isBlank()
                ? "living_legends.name.token." + id + "." + NameTokenForm.BASE.idString()
                : key;
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

    private static Map<NameTokenForm, String> normalizedForms(
            String id,
            Map<NameTokenForm, String> localizedForms
    ) {
        Map<NameTokenForm, String> result = new EnumMap<>(NameTokenForm.class);
        if (localizedForms != null) {
            for (Map.Entry<NameTokenForm, String> entry : localizedForms.entrySet()) {
                NameTokenForm form = entry.getKey() == null ? NameTokenForm.BASE : entry.getKey();
                String key = entry.getValue() == null ? "" : entry.getValue().trim();
                if (!key.isBlank()) {
                    result.put(form, key);
                }
            }
        }
        result.putIfAbsent(NameTokenForm.BASE, "living_legends.name.token." + id + "." + NameTokenForm.BASE.idString());
        return result;
    }
}
