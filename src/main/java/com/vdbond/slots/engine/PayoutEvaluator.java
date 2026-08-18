package com.vdbond.slots.engine;

import com.vdbond.slots.model.Grid;
import com.vdbond.slots.model.LineWin;
import com.vdbond.slots.model.Payline;
import com.vdbond.slots.model.Reel;
import com.vdbond.slots.model.Row;
import com.vdbond.slots.model.SpinResult;
import com.vdbond.slots.model.Symbol;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PayoutEvaluator {

    public static final int SCATTERS_NEEDED = 3;

    public SpinResult evaluate(Grid grid, List<Payline> paylines, Map<Symbol, BigDecimal> paytable, BigDecimal bet) {
        List<LineWin> lineWins = paylines.stream()
                .map(payline -> lineWin(grid, payline, paytable, bet))
                .flatMap(Optional::stream)
                .toList();
        BigDecimal scatterPayout = scatterPayout(grid, paytable, bet);
        BigDecimal totalPayout = lineWins.stream()
                .map(LineWin::payout)
                .reduce(scatterPayout, BigDecimal::add);
        return new SpinResult(grid, lineWins, scatterPayout, totalPayout);
    }

    private Optional<LineWin> lineWin(Grid grid, Payline payline, Map<Symbol, BigDecimal> paytable, BigDecimal bet) {
        List<Symbol> landedSymbols = grid.symbolsOn(payline);
        return matchedSymbol(landedSymbols)
                .map(symbol -> new LineWin(payline, symbol, landedSymbols, payout(symbol, paytable, bet)))
                .filter(win -> win.payout().signum() > 0);
    }

    /**
     * A payline wins only when its three cells are one and the same symbol, with wilds free to stand in for it. So the
     * line is decided by the symbols that are not wild: none left means all three were wild and the line pays at the
     * wild's own rate, one distinct symbol means the wilds completed it, and more than one means no match. A scatter is
     * the exception a wild may not stand in for — it pays only for landing anywhere in the grid — so a line holding one
     * never wins.
     */
    private Optional<Symbol> matchedSymbol(List<Symbol> lineSymbols) {
        Set<Symbol> distinct = lineSymbols.stream()
                .filter(symbol -> !symbol.isWild())
                .collect(Collectors.toSet());
        if (distinct.isEmpty()) {
            return Optional.of(lineSymbols.getFirst());
        }
        if (distinct.size() > 1) {
            return Optional.empty();
        }
        return distinct.stream()
                .findFirst()
                .filter(Symbol::isReplaceable);
    }

    private BigDecimal scatterPayout(Grid grid, Map<Symbol, BigDecimal> paytable, BigDecimal bet) {
        long scatters = Arrays.stream(Reel.values())
                .flatMap(reel -> Arrays.stream(Row.values()).map(row -> grid.at(reel, row)))
                .filter(Symbol::isScatter)
                .count();
        return scatters >= SCATTERS_NEEDED ? payout(Symbol.SCA, paytable, bet) : BigDecimal.ZERO;
    }

    private BigDecimal payout(Symbol symbol, Map<Symbol, BigDecimal> paytable, BigDecimal bet) {
        return bet.multiply(paytable.getOrDefault(symbol, BigDecimal.ZERO));
    }

}
