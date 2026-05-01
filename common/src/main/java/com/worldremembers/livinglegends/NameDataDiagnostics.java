package com.worldremembers.livinglegends;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NameDataDiagnostics {
    private NameDataDiagnostics() {
    }

    public static List<String> validate(NameDataPack nameData, Set<String> availableTranslationKeys) {
        NameDataPack data = nameData == null ? BuiltInNameData.defaultPack() : nameData;
        Set<String> translationKeys = availableTranslationKeys == null ? Set.of() : availableTranslationKeys;
        boolean checkTranslations = !translationKeys.isEmpty();
        List<String> warnings = new ArrayList<>();
        Set<String> patternIds = new HashSet<>();
        Set<String> tokenIds = new HashSet<>();

        for (NamePattern pattern : data.patterns()) {
            if (!patternIds.add(pattern.id())) {
                warnings.add("Duplicate name pattern id: " + pattern.id());
            }
            if (checkTranslations && !translationKeys.contains(pattern.translationKey())) {
                warnings.add("Missing translation key for name pattern " + pattern.id() + ": " + pattern.translationKey());
            }
            for (NamePatternSlot slot : pattern.slots()) {
                if (compatibleTokens(data, slot.requiredTag()).isEmpty()) {
                    warnings.add("No name tokens with tag '" + slot.requiredTag() + "' for pattern " + pattern.id());
                    continue;
                }
                if (compatibleTokensWithForm(data, slot.requiredTag(), slot.requiredForm()).isEmpty()) {
                    warnings.add("No name token with tag '" + slot.requiredTag()
                            + "' provides requested form " + slot.requiredForm().idString()
                            + " for pattern " + pattern.id());
                }
            }
        }

        for (NameToken token : data.tokens()) {
            if (!tokenIds.add(token.id())) {
                warnings.add("Duplicate name token id: " + token.id());
            }
            for (NameTokenForm form : NameTokenForm.values()) {
                String translationKey = token.localizedForms().get(form);
                if (translationKey == null || translationKey.isBlank()) {
                    warnings.add("Missing token form " + form.idString() + " for token " + token.id());
                    continue;
                }
                if (checkTranslations && !translationKeys.contains(translationKey)) {
                    warnings.add("Missing translation key for token " + token.id()
                            + " form " + form.idString() + ": " + translationKey);
                }
            }
        }

        return List.copyOf(warnings);
    }

    private static List<NameToken> compatibleTokens(NameDataPack nameData, String tag) {
        return nameData.tokens().stream()
                .filter(token -> token.hasTag(tag))
                .toList();
    }

    private static List<NameToken> compatibleTokensWithForm(NameDataPack nameData, String tag, NameTokenForm form) {
        return nameData.tokens().stream()
                .filter(token -> token.hasTag(tag))
                .filter(token -> token.localizedForms().containsKey(form == null ? NameTokenForm.BASE : form))
                .toList();
    }
}
