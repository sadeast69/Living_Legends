package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public record NameRecipe(
        String styleId,
        String patternKey,
        List<String> selectedTokenIds,
        List<NameTokenForm> requestedTokenForms,
        long seed,
        String fallbackResolvedName
) implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String LITERAL_TOKEN_PREFIX = "literal:";

    public NameRecipe {
        styleId = WorldPos.optionalId(styleId).isBlank() ? "vanilla_adventure" : WorldPos.optionalId(styleId);
        patternKey = patternKey == null ? "" : patternKey.trim();
        selectedTokenIds = Collections.unmodifiableList(normalizedTokenIds(selectedTokenIds));
        requestedTokenForms = Collections.unmodifiableList(normalizedForms(requestedTokenForms, selectedTokenIds.size()));
        fallbackResolvedName = fallbackResolvedName == null ? "" : fallbackResolvedName.trim();
    }

    public static NameRecipe empty() {
        return new NameRecipe("vanilla_adventure", "living_legends.name.pattern.unknown", List.of(), List.of(), 0L, "");
    }

    public static String literalToken(String value) {
        String sanitized = RuntimeNameFormatter.sanitize(value);
        return sanitized.isBlank() ? "" : LITERAL_TOKEN_PREFIX + sanitized;
    }

    public static boolean isLiteralToken(String tokenId) {
        return tokenId != null && tokenId.startsWith(LITERAL_TOKEN_PREFIX);
    }

    public static String literalTokenValue(String tokenId) {
        if (!isLiteralToken(tokenId)) {
            return "";
        }
        return RuntimeNameFormatter.sanitize(tokenId.substring(LITERAL_TOKEN_PREFIX.length()));
    }

    public NameRecipe withoutFallback() {
        if ("living_legends.name.pattern.legacy".equals(patternKey)) {
            return this;
        }
        return fallbackResolvedName.isBlank()
                ? this
                : new NameRecipe(styleId, patternKey, selectedTokenIds, requestedTokenForms, seed, "");
    }

    public String recipeSignature() {
        StringJoiner joiner = new StringJoiner("|");
        joiner.add(styleId).add(patternKey);
        for (String tokenId : selectedTokenIds) {
            joiner.add(tokenId);
        }
        for (NameTokenForm form : requestedTokenForms) {
            joiner.add(form.idString());
        }
        return joiner.toString();
    }

    private static List<String> normalizedTokenIds(List<String> selectedTokenIds) {
        List<String> result = new ArrayList<>();
        if (selectedTokenIds == null) {
            return result;
        }
        for (String tokenId : selectedTokenIds) {
            String normalized = WorldPos.optionalId(tokenId);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private static List<NameTokenForm> normalizedForms(List<NameTokenForm> requestedTokenForms, int tokenCount) {
        List<NameTokenForm> result = new ArrayList<>();
        if (requestedTokenForms != null) {
            for (NameTokenForm form : requestedTokenForms) {
                result.add(form == null ? NameTokenForm.BASE : form);
            }
        }
        while (result.size() < tokenCount) {
            result.add(NameTokenForm.BASE);
        }
        if (result.size() > tokenCount) {
            return new ArrayList<>(result.subList(0, tokenCount));
        }
        return result;
    }
}
