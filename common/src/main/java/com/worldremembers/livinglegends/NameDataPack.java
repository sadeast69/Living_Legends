package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record NameDataPack(
        String styleId,
        List<NamePattern> patterns,
        List<NameToken> tokens
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public NameDataPack {
        styleId = WorldPos.optionalId(styleId).isBlank() ? "vanilla_adventure" : WorldPos.optionalId(styleId);
        patterns = Collections.unmodifiableList(new ArrayList<>(patterns == null ? List.of() : patterns));
        tokens = Collections.unmodifiableList(new ArrayList<>(tokens == null ? List.of() : tokens));
    }

    public Map<String, NameToken> tokenMap() {
        Map<String, NameToken> result = new LinkedHashMap<>();
        for (NameToken token : tokens) {
            result.putIfAbsent(token.id(), token);
        }
        return Collections.unmodifiableMap(result);
    }
}
