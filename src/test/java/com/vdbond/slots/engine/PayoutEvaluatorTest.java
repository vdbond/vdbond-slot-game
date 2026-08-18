package com.vdbond.slots.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vdbond.slots.model.Grid;
import com.vdbond.slots.model.LineWin;
import com.vdbond.slots.model.Payline;
import com.vdbond.slots.model.ReelWindow;
import com.vdbond.slots.model.Row;
import com.vdbond.slots.model.SpinResult;
import com.vdbond.slots.model.Symbol;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PayoutEvaluatorTest {

    private static final List<Payline> PAYLINES = List.of(
            new Payline(0, Row.TOP, Row.TOP, Row.TOP),
            new Payline(1, Row.MIDDLE, Row.MIDDLE, Row.MIDDLE),
            new Payline(2, Row.BOTTOM, Row.BOTTOM, Row.BOTTOM),
            new Payline(3, Row.TOP, Row.MIDDLE, Row.BOTTOM),
            new Payline(4, Row.BOTTOM, Row.MIDDLE, Row.TOP)
    );

    private static final Map<Symbol, BigDecimal> PAYTABLE = paytable(Map.of(
            Symbol.L1, "1",
            Symbol.L2, "1.5",
            Symbol.L3, "2.0",
            Symbol.L4, "5.0",
            Symbol.H1, "8.0",
            Symbol.H2, "50.0",
            Symbol.H3, "80.0",
            Symbol.W1, "200.0",
            Symbol.SCA, "20.0")
    );

    private static final BigDecimal BET = new BigDecimal("10.00");

    @Test
    @DisplayName("three matching symbols on a payline pay that symbol's multiplier times the bet")
    void paysPlainThreeOfAKind() {
        // H3 H3 H3
        // L1 L2 L3
        // L2 L3 L1
        Grid grid = new Grid(
                new ReelWindow(Symbol.H3, Symbol.L1, Symbol.L2),
                new ReelWindow(Symbol.H3, Symbol.L2, Symbol.L3),
                new ReelWindow(Symbol.H3, Symbol.L3, Symbol.L1)
        );

        SpinResult result = evaluate(grid);

        assertEquals(1, result.lineWins().size());
        LineWin win = result.lineWins().getFirst();
        assertEquals(1, win.payline().displayNumber());
        assertEquals(Symbol.H3, win.symbol());
        assertAmount("800.0", win.payout());
        assertAmount("800.0", result.totalPayout());
        assertEquals(List.of(Row.TOP, Row.TOP, Row.TOP), win.payline().rows());
        assertEquals(List.of(Symbol.H3, Symbol.H3, Symbol.H3), win.landedSymbols());
        assertEquals(List.of(Symbol.H3, Symbol.H3, Symbol.H3), win.resolvedSymbols());
        assertFalse(win.involvedWild());
        assertEquals(grid, result.grid());
    }

    @Test
    @DisplayName("a wild completes a match and pays at the rate of the symbol it stood in for")
    void paysAWildCompletedMatch() {
        // H1 W1 H1
        // L1 L2 L3
        // L2 L3 L4
        Grid grid = new Grid(
                new ReelWindow(Symbol.H1, Symbol.L1, Symbol.L2),
                new ReelWindow(Symbol.W1, Symbol.L2, Symbol.L3),
                new ReelWindow(Symbol.H1, Symbol.L3, Symbol.L4)
        );

        SpinResult result = evaluate(grid);

        assertEquals(1, result.lineWins().size());
        LineWin win = result.lineWins().getFirst();
        assertEquals(Symbol.H1, win.symbol());
        assertAmount("80.0", result.totalPayout());
        assertEquals(List.of(Symbol.H1, Symbol.W1, Symbol.H1), win.landedSymbols());
        assertEquals(List.of(Symbol.H1, Symbol.H1, Symbol.H1), win.resolvedSymbols());
        assertTrue(win.involvedWild());
        assertEquals(grid, result.grid());
    }

    @Test
    @DisplayName("three wilds on a payline pay the wild's own rate and resolve to themselves")
    void paysAnAllWildLineAtTheWildRate() {
        // L1 L2 L3
        // W1 W1 W1
        // L2 L3 L4
        Grid grid = new Grid(
                new ReelWindow(Symbol.L1, Symbol.W1, Symbol.L2),
                new ReelWindow(Symbol.L2, Symbol.W1, Symbol.L3),
                new ReelWindow(Symbol.L3, Symbol.W1, Symbol.L4)
        );

        SpinResult result = evaluate(grid);

        assertEquals(1, result.lineWins().size());
        assertEquals(Symbol.W1, result.lineWins().getFirst().symbol());
        assertAmount("2000.0", result.totalPayout());
        assertEquals(List.of(Symbol.W1, Symbol.W1, Symbol.W1), result.lineWins().getFirst().landedSymbols());
        assertEquals(List.of(Symbol.W1, Symbol.W1, Symbol.W1), result.lineWins().getFirst().resolvedSymbols());
        assertEquals(grid, result.grid());
    }

    @Test
    @DisplayName("a payline of different symbols pays nothing")
    void paysNothingForAMismatchedLine() {
        // L1 L2 H2
        // L2 L3 L4
        // L3 L4 H1
        Grid grid = new Grid(
                new ReelWindow(Symbol.L1, Symbol.L2, Symbol.L3),
                new ReelWindow(Symbol.L2, Symbol.L3, Symbol.L4),
                new ReelWindow(Symbol.H2, Symbol.L4, Symbol.H1)
        );

        SpinResult result = evaluate(grid);

        assertTrue(result.lineWins().isEmpty());
        assertAmount("0", result.totalPayout());
        assertFalse(result.isWin());
        assertEquals(grid, result.grid());
    }

    @Test
    @DisplayName("three scatters pay wherever they land, even scattered across unrelated positions")
    void paysScattersFromAnywhereInTheGrid() {
        // SCA L3  H1
        // L1  L4  SCA
        // L2  SCA H2
        Grid grid = new Grid(
                new ReelWindow(Symbol.SCA, Symbol.L1, Symbol.L2),
                new ReelWindow(Symbol.L3, Symbol.L4, Symbol.SCA),
                new ReelWindow(Symbol.H1, Symbol.SCA, Symbol.H2)
        );

        SpinResult result = evaluate(grid);

        assertTrue(result.lineWins().isEmpty());
        assertAmount("200.0", result.scatterPayout());
        assertAmount("200.0", result.totalPayout());
        assertEquals(grid, result.grid());
    }

    @Test
    @DisplayName("scatters filling a payline pay only as scatters, never as that payline")
    void doesNotPayScattersTwiceWhenTheyFillAPayline() {
        // SCA SCA SCA
        // L1  L3  H1
        // L2  L4  H2
        Grid grid = new Grid(
                new ReelWindow(Symbol.SCA, Symbol.L1, Symbol.L2),
                new ReelWindow(Symbol.SCA, Symbol.L3, Symbol.L4),
                new ReelWindow(Symbol.SCA, Symbol.H1, Symbol.H2)
        );

        SpinResult result = evaluate(grid);

        assertTrue(result.lineWins().isEmpty());
        assertAmount("200.0", result.scatterPayout());
        assertAmount("200.0", result.totalPayout());
        assertEquals(grid, result.grid());
    }

    @Test
    @DisplayName("a wild cannot stand in for a scatter, so two scatters and a wild pay nothing")
    void refusesToSubstituteAWildForAScatter() {
        // SCA W1  SCA
        // L1  L3  H1
        // L2  L4  H2
        Grid grid = new Grid(
                new ReelWindow(Symbol.SCA, Symbol.L1, Symbol.L2),
                new ReelWindow(Symbol.W1, Symbol.L3, Symbol.L4),
                new ReelWindow(Symbol.SCA, Symbol.H1, Symbol.H2)
        );

        SpinResult result = evaluate(grid);

        assertTrue(result.lineWins().isEmpty());
        assertAmount("0", result.totalPayout());
        assertEquals(grid, result.grid());
    }

    @Test
    @DisplayName("more scatters than the three needed still pay the single scatter award")
    void paysOnceWhenMoreThanThreeScattersLand() {
        // SCA L3  H1
        // SCA L4  SCA
        // L2  SCA H2
        Grid grid = new Grid(
                new ReelWindow(Symbol.SCA, Symbol.SCA, Symbol.L2),
                new ReelWindow(Symbol.L3, Symbol.L4, Symbol.SCA),
                new ReelWindow(Symbol.H1, Symbol.SCA, Symbol.H2)
        );

        SpinResult result = evaluate(grid);

        assertAmount("200.0", result.scatterPayout());
        assertAmount("200.0", result.totalPayout());
        assertEquals(grid, result.grid());
    }

    @Test
    @DisplayName("every payline that matches pays, and the payouts add up")
    void addsUpSimultaneousLineWins() {
        // H2 H2 H2
        // L1 L1 L1
        // L4 L4 L4
        Grid grid = new Grid(
                new ReelWindow(Symbol.H2, Symbol.L1, Symbol.L4),
                new ReelWindow(Symbol.H2, Symbol.L1, Symbol.L4),
                new ReelWindow(Symbol.H2, Symbol.L1, Symbol.L4)
        );

        SpinResult result = evaluate(grid);

        assertEquals(List.of(1, 2, 3), result.lineWins().stream().map(win -> win.payline().displayNumber()).toList());
        assertEquals(Set.of(Symbol.H2, Symbol.L1, Symbol.L4),
                result.lineWins().stream().map(LineWin::symbol).collect(Collectors.toSet()));
        assertAmount("560.0", result.totalPayout());
        assertEquals(grid, result.grid());
    }

    @Test
    @DisplayName("wilds on two lines at once each resolve to their own line's symbol")
    void resolvesEachWildAgainstItsOwnLine() {
        // H1 W1 H1
        // L1 W1 L1
        // L2 L3 L4
        //
        // the top line pays as H1 and the middle line as L1, so no single
        // redrawn grid could show both wilds resolved at once
        Grid grid = new Grid(
                new ReelWindow(Symbol.H1, Symbol.L1, Symbol.L2),
                new ReelWindow(Symbol.W1, Symbol.W1, Symbol.L3),
                new ReelWindow(Symbol.H1, Symbol.L1, Symbol.L4)
        );

        SpinResult result = evaluate(grid);

        assertAmount("90.0", result.totalPayout());
        assertEquals(grid, result.grid());
        LineWin topLine = result.lineWins().getFirst();
        LineWin middleLine = result.lineWins().get(1);
        assertEquals(List.of(Symbol.H1, Symbol.W1, Symbol.H1), topLine.landedSymbols());
        assertEquals(List.of(Symbol.H1, Symbol.H1, Symbol.H1), topLine.resolvedSymbols());
        assertEquals(List.of(Symbol.L1, Symbol.W1, Symbol.L1), middleLine.landedSymbols());
        assertEquals(List.of(Symbol.L1, Symbol.L1, Symbol.L1), middleLine.resolvedSymbols());
    }

    @Test
    @DisplayName("one wild completing two lines pays and resolves on both of them")
    void resolvesASharedWildOnEveryLineItCompletes() {
        // W1 H2 H2
        // L3 L1 L4
        // L2 L4 L1
        //
        // the one wild completes the top line as H2 and the top-left
        // diagonal as L1, and is resolved separately on each
        Grid grid = new Grid(
                new ReelWindow(Symbol.W1, Symbol.L3, Symbol.L2),
                new ReelWindow(Symbol.H2, Symbol.L1, Symbol.L4),
                new ReelWindow(Symbol.H2, Symbol.L4, Symbol.L1)
        );

        SpinResult result = evaluate(grid);

        assertAmount("510.0", result.totalPayout());
        LineWin topLine = result.lineWins().getFirst();
        assertEquals(1, topLine.payline().displayNumber());
        assertEquals(List.of(Row.TOP, Row.TOP, Row.TOP), topLine.payline().rows());
        assertEquals(List.of(Symbol.W1, Symbol.H2, Symbol.H2), topLine.landedSymbols());
        assertEquals(List.of(Symbol.H2, Symbol.H2, Symbol.H2), topLine.resolvedSymbols());

        LineWin diagonal = result.lineWins().get(1);
        assertEquals(4, diagonal.payline().displayNumber());
        assertEquals(List.of(Row.TOP, Row.MIDDLE, Row.BOTTOM), diagonal.payline().rows());
        assertEquals(List.of(Symbol.W1, Symbol.L1, Symbol.L1), diagonal.landedSymbols());
        assertEquals(List.of(Symbol.L1, Symbol.L1, Symbol.L1), diagonal.resolvedSymbols());
    }

    @Test
    @DisplayName("a wild that took no part in a win is named by no win at all")
    void ignoresAWildThatCompletedNothing() {
        // H3 H3 H3
        // L1 L2 L3
        // W1 L3 L4
        Grid grid = new Grid(
                new ReelWindow(Symbol.H3, Symbol.L1, Symbol.W1),
                new ReelWindow(Symbol.H3, Symbol.L2, Symbol.L3),
                new ReelWindow(Symbol.H3, Symbol.L3, Symbol.L4)
        );

        SpinResult result = evaluate(grid);

        assertAmount("800.0", result.totalPayout());
        assertEquals(grid, result.grid());
        assertEquals(1, result.lineWins().size());
        assertFalse(result.lineWins().getFirst().involvedWild());
    }

    @Test
    @DisplayName("a symbol configured to pay nothing produces no win at all, not a zero-value one")
    void reportsNoWinForASymbolWorthNothing() {
        // L1 L1 L1
        // L2 L3 L4
        // L3 L4 H1
        Grid grid = new Grid(
                new ReelWindow(Symbol.L1, Symbol.L2, Symbol.L3),
                new ReelWindow(Symbol.L1, Symbol.L3, Symbol.L4),
                new ReelWindow(Symbol.L1, Symbol.L4, Symbol.H1)
        );

        SpinResult result = PayoutEvaluator.evaluate(grid, PAYLINES, paytableWith(Symbol.L1, "0"), BET);

        assertTrue(result.lineWins().isEmpty());
        assertFalse(result.isWin());
    }

    private static Map<Symbol, BigDecimal> paytable(Map<Symbol, String> multipliers) {
        Map<Symbol, BigDecimal> paytable = new EnumMap<>(Symbol.class);
        multipliers.forEach((symbol, multiplier) -> paytable.put(symbol, new BigDecimal(multiplier)));
        return paytable;
    }

    private static Map<Symbol, BigDecimal> paytableWith(Symbol symbol, String multiplier) {
        Map<Symbol, BigDecimal> adjusted = new EnumMap<>(PAYTABLE);
        adjusted.put(symbol, new BigDecimal(multiplier));
        return adjusted;
    }

    private static SpinResult evaluate(Grid grid) {
        return PayoutEvaluator.evaluate(grid, PAYLINES, PAYTABLE, BET);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected %s but was %s".formatted(expected, actual.toPlainString()));
    }

}
