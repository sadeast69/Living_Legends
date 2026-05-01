package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.BuiltInNameData;
import com.worldremembers.livinglegends.NameDataPack;
import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NameToken;
import com.worldremembers.livinglegends.NameTokenForm;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class WorldRemembersLivingLegendsFabricNameResolver {
    private WorldRemembersLivingLegendsFabricNameResolver() {
    }

    static MutableText resolve(NameRecipe recipe) {
        NameRecipe resolvedRecipe = recipe == null ? NameRecipe.empty() : recipe;
        return resolve(resolvedRecipe, BuiltInNameData.packForStyle(resolvedRecipe.styleId()));
    }

    static MutableText resolve(NameRecipe recipe, NameDataPack nameData) {
        NameRecipe resolvedRecipe = recipe == null ? NameRecipe.empty() : recipe;
        NameDataPack resolvedData = nameData == null ? BuiltInNameData.defaultPack() : nameData;
        Map<String, NameToken> tokens = resolvedData.tokenMap();
        List<Text> tokenTexts = new ArrayList<>();
        for (int index = 0; index < resolvedRecipe.selectedTokenIds().size(); index++) {
            String tokenId = resolvedRecipe.selectedTokenIds().get(index);
            if (NameRecipe.isLiteralToken(tokenId)) {
                tokenTexts.add(Text.literal(NameRecipe.literalTokenValue(tokenId)));
                continue;
            }
            NameTokenForm form = index < resolvedRecipe.requestedTokenForms().size()
                    ? resolvedRecipe.requestedTokenForms().get(index)
                    : NameTokenForm.BASE;
            NameToken token = tokens.get(tokenId);
            tokenTexts.add(token == null
                    ? Text.literal(tokenId.replace('_', ' '))
                    : Text.translatable(token.translationKey(form)));
        }

        if (resolvedRecipe.patternKey().isBlank()) {
            return Text.literal(resolvedRecipe.fallbackResolvedName());
        }
        return Text.translatable(resolvedRecipe.patternKey(), tokenTexts.toArray());
    }

    static String resolveToString(NameRecipe recipe) {
        return resolve(recipe).getString();
    }
}
