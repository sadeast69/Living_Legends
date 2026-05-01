package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record NamePatternSlot(
        String slotId,
        String requiredTag,
        NameTokenForm requiredForm,
        Set<String> forbiddenSemanticRoots
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public NamePatternSlot(String slotId, String requiredTag, NameTokenForm requiredForm) {
        this(slotId, requiredTag, requiredForm, Set.of());
    }

    public NamePatternSlot {
        slotId = WorldPos.optionalId(slotId).isBlank() ? "token" : WorldPos.optionalId(slotId);
        requiredTag = WorldPos.optionalId(requiredTag).isBlank() ? "generic" : WorldPos.optionalId(requiredTag);
        requiredForm = requiredForm == null ? NameTokenForm.BASE : requiredForm;
        forbiddenSemanticRoots = Collections.unmodifiableSet(normalizedRoots(forbiddenSemanticRoots));
    }

    private static Set<String> normalizedRoots(Set<String> roots) {
        Set<String> result = new LinkedHashSet<>();
        if (roots == null) {
            return result;
        }
        for (String root : roots) {
            String normalized = NameSemanticRoots.normalize(root);
            if (!NameSemanticRoots.UNKNOWN.equals(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }
}
