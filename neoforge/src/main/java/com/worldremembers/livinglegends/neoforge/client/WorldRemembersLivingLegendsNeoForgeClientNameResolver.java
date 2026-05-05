package com.worldremembers.livinglegends.neoforge.client;

import com.worldremembers.livinglegends.BuiltInNameData;
import com.worldremembers.livinglegends.NameDataPack;
import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NameTextSafety;
import com.worldremembers.livinglegends.NameToken;
import com.worldremembers.livinglegends.NameTokenForm;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class WorldRemembersLivingLegendsNeoForgeClientNameResolver {
    private WorldRemembersLivingLegendsNeoForgeClientNameResolver() {
    }

    static Component resolve(NameRecipe recipe) {
        NameRecipe resolvedRecipe = recipe == null ? NameRecipe.empty() : recipe;
        return resolve(resolvedRecipe, BuiltInNameData.packForStyle(resolvedRecipe.styleId()));
    }

    private static Component resolve(NameRecipe recipe, NameDataPack nameData) {
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

        Component resolved = resolvedRecipe.patternKey().isBlank()
                ? Component.literal(resolvedRecipe.fallbackResolvedName())
                : Component.translatable(resolvedRecipe.patternKey(), tokenTexts.toArray());
        if (!NameTextSafety.looksBrokenOrTechnical(resolved.getString())) {
            return resolved;
        }
        String fallback = resolvedRecipe.fallbackResolvedName();
        return NameTextSafety.looksBrokenOrTechnical(fallback)
                ? Component.translatable(NameTextSafety.SAFE_FALLBACK_PATTERN_KEY)
                : Component.literal(fallback);
    }
}
