package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.NameDataPack;
import com.worldremembers.livinglegends.NameRecipe;
import net.minecraft.text.MutableText;

public final class NameResolver {
    private NameResolver() {
    }

    public static MutableText resolve(NameRecipe recipe) {
        return WorldRemembersLivingLegendsFabricNameResolver.resolve(recipe);
    }

    public static MutableText resolve(NameRecipe recipe, NameDataPack nameData) {
        return WorldRemembersLivingLegendsFabricNameResolver.resolve(recipe, nameData);
    }
}
