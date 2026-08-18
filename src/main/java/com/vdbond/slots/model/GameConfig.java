package com.vdbond.slots.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record GameConfig(
        BigDecimal defaultBalance,
        BigDecimal defaultBet,
        double targetRtp,
        Map<Reel, List<Symbol>> reelStrips,
        List<Payline> paylines,
        Map<Symbol, BigDecimal> paytable
) {

    public List<Symbol> stripFor(Reel reel) {
        return reelStrips.get(reel);
    }

    public BigDecimal multiplierFor(Symbol symbol) {
        return paytable.getOrDefault(symbol, BigDecimal.ZERO);
    }

}
