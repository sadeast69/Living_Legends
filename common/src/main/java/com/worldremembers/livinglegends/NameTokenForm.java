package com.worldremembers.livinglegends;

import java.io.Serializable;
import java.util.Locale;

public enum NameTokenForm implements Serializable {
    BASE("base"),
    GENITIVE("genitive"),
    PLURAL("plural"),
    ADJECTIVE_MASC("adjective_masc"),
    ADJECTIVE_FEM("adjective_fem"),
    ADJECTIVE_NEUT("adjective_neut"),
    ADJECTIVE_PLURAL("adjective_plural");

    private final String id;

    NameTokenForm(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String idString() {
        return id;
    }

    public static NameTokenForm fromId(String id) {
        String normalized = id == null || id.isBlank()
                ? BASE.id
                : id.trim().toLowerCase(Locale.ROOT);
        for (NameTokenForm form : values()) {
            if (form.id.equals(normalized)) {
                return form;
            }
        }
        return BASE;
    }
}
