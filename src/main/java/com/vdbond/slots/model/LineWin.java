package com.vdbond.slots.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code landedSymbols} is what the player saw on this payline, wilds included; {@code resolvedSymbols} is what it
 * paid as. The two differ only where a wild stood in for {@code symbol}. Both are kept so a win can be shown line by
 * line — one wild can complete two paylines as two different symbols, so no single redrawn grid could show them all.
 */
public record LineWin(Payline payline, Symbol symbol, List<Symbol> landedSymbols, BigDecimal payout) {

    public List<Symbol> resolvedSymbols() {
        return landedSymbols.stream()
                .map(landed -> landed.isWild() ? symbol : landed)
                .toList();
    }

    public boolean involvedWild() {
        return landedSymbols.stream()
                .anyMatch(Symbol::isWild);
    }

}