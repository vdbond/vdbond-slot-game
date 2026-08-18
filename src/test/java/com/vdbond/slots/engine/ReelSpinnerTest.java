package com.vdbond.slots.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vdbond.slots.model.GameConfig;
import com.vdbond.slots.model.Grid;
import com.vdbond.slots.model.Reel;
import com.vdbond.slots.model.ReelWindow;
import com.vdbond.slots.model.Symbol;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReelSpinnerTest {

    private static final List<Symbol> STRIP = List.of(Symbol.L1, Symbol.L2, Symbol.L3, Symbol.H1, Symbol.W1);

    @Test
    @DisplayName("a reel shows the symbol it stopped on and the two below it")
    void showsThreeConsecutiveSymbolsFromTheStopPosition() {
        ReelSpinner spinner = new ReelSpinner(stoppingAt(0, 1, 2));

        Grid grid = spinner.spin(configWith(STRIP));

        assertEquals(new ReelWindow(Symbol.L1, Symbol.L2, Symbol.L3), grid.windowOn(Reel.FIRST));
        assertEquals(new ReelWindow(Symbol.L2, Symbol.L3, Symbol.H1), grid.windowOn(Reel.SECOND));
        assertEquals(new ReelWindow(Symbol.L3, Symbol.H1, Symbol.W1), grid.windowOn(Reel.THIRD));
    }

    @Test
    @DisplayName("a strip is a loop, so stopping near its end wraps back to the start")
    void wrapsAroundTheEndOfTheStrip() {
        ReelSpinner spinner = new ReelSpinner(stoppingAt(4, 3, 4));

        Grid grid = spinner.spin(configWith(STRIP));

        assertEquals(new ReelWindow(Symbol.W1, Symbol.L1, Symbol.L2), grid.windowOn(Reel.FIRST));
        assertEquals(new ReelWindow(Symbol.H1, Symbol.W1, Symbol.L1), grid.windowOn(Reel.SECOND));
        assertEquals(new ReelWindow(Symbol.W1, Symbol.L1, Symbol.L2), grid.windowOn(Reel.THIRD));
    }

    @Test
    @DisplayName("a strip shorter than the grid keeps wrapping instead of running out of symbols")
    void wrapsRepeatedlyOnAVeryShortStrip() {
        ReelSpinner spinner = new ReelSpinner(stoppingAt(1, 0, 1));

        Grid grid = spinner.spin(configWith(List.of(Symbol.L1, Symbol.H1)));

        assertEquals(new ReelWindow(Symbol.H1, Symbol.L1, Symbol.H1), grid.windowOn(Reel.FIRST));
        assertEquals(new ReelWindow(Symbol.L1, Symbol.H1, Symbol.L1), grid.windowOn(Reel.SECOND));
        assertEquals(new ReelWindow(Symbol.H1, Symbol.L1, Symbol.H1), grid.windowOn(Reel.THIRD));
    }

    private static GameConfig configWith(List<Symbol> strip) {
        Map<Reel, List<Symbol>> strips = new EnumMap<>(Reel.class);
        for (Reel reel : Reel.values()) {
            strips.put(reel, strip);
        }
        return new GameConfig(BigDecimal.TEN, BigDecimal.ONE, 0.96, strips, List.of(), Map.of());
    }

    private static RandomGenerator stoppingAt(int... stopPositions) {
        return new RandomGenerator() {

            private int call;

            @Override
            public long nextLong() {
                throw new UnsupportedOperationException("the spinner should only ask for a stop position");
            }

            @Override
            public int nextInt(int bound) {
                return stopPositions[call++] % bound;
            }
        };
    }

}
