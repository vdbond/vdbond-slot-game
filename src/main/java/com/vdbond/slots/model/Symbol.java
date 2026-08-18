package com.vdbond.slots.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Symbol {

    L4("🍉", SymbolKind.LOW),
    L3("🍇", SymbolKind.LOW),
    L2("🍋", SymbolKind.LOW),
    L1("🍒", SymbolKind.LOW),
    H3("⑦⃣", SymbolKind.HIGH),
    H2("🔔", SymbolKind.HIGH),
    H1("💎", SymbolKind.HIGH),
    W1("⭐", SymbolKind.WILD),
    SCA("🎰", SymbolKind.SCATTER);

    private final String emoji;
    private final SymbolKind kind;

    public static Symbol fromCode(String code) {
        try {
            return Symbol.valueOf(code);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("'%s' is not a known symbol code".formatted(code));
        }
    }

    public boolean isScatter() {
        return kind == SymbolKind.SCATTER;
    }

    /**
     * The scatter is carved out of payline matching entirely, so a wild is never allowed to stand in for it.
     */
    public boolean isReplaceable() {
        return kind != SymbolKind.SCATTER;
    }

    public boolean isWild() {
        return kind == SymbolKind.WILD;
    }

}
