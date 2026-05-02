package com.worldremembers.livinglegends.neoforge;

import com.worldremembers.livinglegends.BuiltInNameData;
import com.worldremembers.livinglegends.NameDataPack;
import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NameToken;
import com.worldremembers.livinglegends.NameTokenForm;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class WorldRemembersLivingLegendsNeoForgeNameResolver {
    private WorldRemembersLivingLegendsNeoForgeNameResolver() {
    }

    static Component resolve(NameRecipe recipe) {
        NameRecipe resolvedRecipe = recipe == null ? NameRecipe.empty() : recipe;
        return resolve(resolvedRecipe, BuiltInNameData.packForStyle(resolvedRecipe.styleId()));
    }

    static Component resolve(NameRecipe recipe, NameDataPack nameData) {
        NameRecipe resolvedRecipe = recipe == null ? NameRecipe.empty() : recipe;
        NameDataPack resolvedData = nameData == null ? BuiltInNameData.defaultPack() : nameData;
        Map<String, NameToken> tokens = resolvedData.tokenMap();
        List<Component> tokenTexts = new ArrayList<>();
        for (int index = 0; index < resolvedRecipe.selectedTokenIds().size(); index++) {
            String tokenId = resolvedRecipe.selectedTokenIds().get(index);
            if (NameRecipe.isLiteralToken(tokenId)) {
                tokenTexts.add(Component.literal(NameRecipe.literalTokenValue(tokenId)));
                continue;
            }
            NameTokenForm form = index < resolvedRecipe.requestedTokenForms().size()
                    ? resolvedRecipe.requestedTokenForms().get(index)
                    : NameTokenForm.BASE;
            NameToken token = tokens.get(tokenId);
            tokenTexts.add(token == null
                    ? Component.literal(tokenId.replace('_', ' '))
                    : Component.translatable(token.translationKey(form)));
        }

        if (resolvedRecipe.patternKey().isBlank()) {
            return Component.literal(resolvedRecipe.fallbackResolvedName());
        }
        return Component.translatable(resolvedRecipe.patternKey(), tokenTexts.toArray());
    }

    static String resolveToString(NameRecipe recipe) {
        NameRecipe resolvedRecipe = recipe == null ? NameRecipe.empty() : recipe;
        String fallback = resolvedRecipe.fallbackResolvedName();
        if (!fallback.isBlank()) {
            return fallback;
        }
        String resolved = resolve(resolvedRecipe).getString();
        return resolved == null ? "" : resolved;
    }
}
