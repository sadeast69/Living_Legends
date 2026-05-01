package com.worldremembers.livinglegends;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class NameGenerationDiagnostics {
    private final boolean collectRejectionDetails;
    private int generated;
    private int rejectedTautology;
    private int rejectedMissingForm;
    private int rejectedRoleMismatch;
    private int rejectedCauseMismatch;
    private int rejectedTargetMismatch;
    private int duplicateAvoided;
    private NamePatternSource selectedPatternSource = NamePatternSource.SAFE_FALLBACK;
    private final List<String> rejectionDetails = new ArrayList<>();

    public NameGenerationDiagnostics() {
        this(true);
    }

    public NameGenerationDiagnostics(boolean collectRejectionDetails) {
        this.collectRejectionDetails = collectRejectionDetails;
    }

    public void generated() {
        generated++;
    }

    public void duplicateAvoided() {
        duplicateAvoided++;
    }

    public void rejectRoleMismatch(NamePattern pattern, NamePatternSlot slot) {
        rejectedRoleMismatch++;
        addDetail(pattern, null, slot, "role_mismatch", Set.of());
    }

    public void rejectMissingForm(NamePattern pattern, NamePatternSlot slot, NameToken token) {
        rejectedMissingForm++;
        addDetail(pattern, token, slot, "missing_form", Set.of());
    }

    public void rejectTautology(NamePattern pattern, NamePatternSlot slot, NameToken token, Set<String> forbiddenRoots) {
        rejectedTautology++;
        addDetail(pattern, token, slot, "semantic_root_conflict", forbiddenRoots);
    }

    public void rejectCauseMismatch(NamePattern pattern, NamePatternSlot slot, NameToken token) {
        rejectedCauseMismatch++;
        addDetail(pattern, token, slot, "cause_mismatch", Set.of());
    }

    public void rejectTargetMismatch(NamePattern pattern, NamePatternSlot slot, NameToken token) {
        rejectedTargetMismatch++;
        addDetail(pattern, token, slot, "target_mismatch", Set.of());
    }

    public void selectedPatternSource(NamePatternSource source) {
        selectedPatternSource = source == null ? NamePatternSource.SAFE_FALLBACK : source;
    }

    public int generatedCount() {
        return generated;
    }

    public int rejectedTautology() {
        return rejectedTautology;
    }

    public int rejectedMissingForm() {
        return rejectedMissingForm;
    }

    public int rejectedRoleMismatch() {
        return rejectedRoleMismatch;
    }

    public int rejectedCauseMismatch() {
        return rejectedCauseMismatch;
    }

    public int rejectedTargetMismatch() {
        return rejectedTargetMismatch;
    }

    public NamePatternSource selectedPatternSource() {
        return selectedPatternSource;
    }

    public int duplicateAvoidedCount() {
        return duplicateAvoided;
    }

    public List<String> rejectionDetails() {
        return List.copyOf(rejectionDetails);
    }

    public String summary() {
        return "generated=" + generated
                + " rejectedTautology=" + rejectedTautology
                + " rejectedMissingForm=" + rejectedMissingForm
                + " rejectedRoleMismatch=" + rejectedRoleMismatch
                + " rejectedCauseMismatch=" + rejectedCauseMismatch
                + " rejectedTargetMismatch=" + rejectedTargetMismatch
                + " duplicateAvoided=" + duplicateAvoided
                + " selectedPatternSource=" + selectedPatternSource.name();
    }

    private void addDetail(
            NamePattern pattern,
            NameToken token,
            NamePatternSlot slot,
            String reason,
            Set<String> forbiddenRoots
    ) {
        if (!collectRejectionDetails) {
            return;
        }
        if (rejectionDetails.size() >= 50) {
            return;
        }
        rejectionDetails.add("rejected patternKey="
                + (pattern == null ? "none" : pattern.translationKey())
                + " tokenKey="
                + (token == null ? "none" : token.id())
                + " reason=" + reason
                + " patternSemanticRoot="
                + (pattern == null ? "none" : pattern.semanticRoot())
                + " tokenSemanticRoot="
                + (token == null ? "none" : token.semanticRoot())
                + " forbiddenSemanticRoots="
                + (forbiddenRoots == null ? Set.of() : forbiddenRoots)
                + " slot="
                + (slot == null ? "none" : slot.slotId()));
    }
}
