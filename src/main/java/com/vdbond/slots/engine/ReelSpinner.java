package com.vdbond.slots.engine;

import com.vdbond.slots.model.GameConfig;
import com.vdbond.slots.model.Grid;
import com.vdbond.slots.model.Reel;
import com.vdbond.slots.model.ReelWindow;
import com.vdbond.slots.model.Symbol;
import java.security.SecureRandom;
import java.util.List;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReelSpinner {

    private final RandomGenerator random;

    public ReelSpinner() {
        this(new SecureRandom());
    }

    public Grid spin(GameConfig config) {
        return new Grid(stop(config.stripFor(Reel.FIRST)),
                stop(config.stripFor(Reel.SECOND)),
                stop(config.stripFor(Reel.THIRD)));
    }

    private ReelWindow stop(List<Symbol> strip) {
        int stopPosition = random.nextInt(strip.size());
        return new ReelWindow(symbolAt(strip, stopPosition),
                symbolAt(strip, stopPosition + 1),
                symbolAt(strip, stopPosition + 2));
    }

    private Symbol symbolAt(List<Symbol> strip, int position) {
        return strip.get(position % strip.size());
    }

}
