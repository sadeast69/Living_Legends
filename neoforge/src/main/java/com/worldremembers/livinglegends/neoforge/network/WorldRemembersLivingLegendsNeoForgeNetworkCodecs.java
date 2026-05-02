package com.worldremembers.livinglegends.neoforge.network;

import com.worldremembers.livinglegends.NameRecipe;
import com.worldremembers.livinglegends.NameTokenForm;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

final class WorldRemembersLivingLegendsNeoForgeNetworkCodecs {
    static final int MAX_STRING_LIST_SIZE = 64;
    static final int MAX_JOURNAL_ENTRY_COUNT = 128;

    private WorldRemembersLivingLegendsNeoForgeNetworkCodecs() {
    }

    static String readString(RegistryFriendlyByteBuf buf) {
        return buf.readUtf();
    }

    static void writeString(RegistryFriendlyByteBuf buf, String value) {
        buf.writeUtf(value == null ? "" : value);
    }

    static void writeNameRecipe(RegistryFriendlyByteBuf buf, NameRecipe recipe) {
        NameRecipe resolved = recipe == null ? NameRecipe.empty() : recipe;
        writeString(buf, resolved.styleId());
        writeString(buf, resolved.patternKey());
        writeStringList(buf, resolved.selectedTokenIds());
        List<String> forms = new ArrayList<>();
        for (NameTokenForm form : resolved.requestedTokenForms()) {
            forms.add((form == null ? NameTokenForm.BASE : form).idString());
        }
        writeStringList(buf, forms);
        buf.writeLong(resolved.seed());
        writeString(buf, resolved.fallbackResolvedName());
    }

    static NameRecipe readNameRecipe(RegistryFriendlyByteBuf buf) {
        String styleId = readString(buf);
        String patternKey = readString(buf);
        List<String> tokenIds = readStringList(buf);
        List<String> formIds = readStringList(buf);
        List<NameTokenForm> forms = new ArrayList<>();
        for (String formId : formIds) {
            forms.add(NameTokenForm.fromId(formId));
        }
        long seed = buf.readLong();
        String fallback = readString(buf);
        return new NameRecipe(styleId, patternKey, tokenIds, forms, seed, fallback);
    }

    static void writeStringList(RegistryFriendlyByteBuf buf, List<String> values) {
        List<String> resolved = values == null ? List.of() : values;
        buf.writeVarInt(resolved.size());
        for (String value : resolved) {
            writeString(buf, value);
        }
    }

    static List<String> readStringList(RegistryFriendlyByteBuf buf) {
        int size = Math.max(0, Math.min(MAX_STRING_LIST_SIZE, buf.readVarInt()));
        List<String> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            result.add(readString(buf));
        }
        return result;
    }
}
