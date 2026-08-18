package com.vdbond.slots.model;

import java.util.Arrays;
import java.util.List;

public record Grid(ReelWindow firstReel, ReelWindow secondReel, ReelWindow thirdReel) {

    public ReelWindow windowOn(Reel reel) {
        return switch (reel) {
            case FIRST -> firstReel;
            case SECOND -> secondReel;
            case THIRD -> thirdReel;
        };
    }

    public Symbol at(Reel reel, Row row) {
        return windowOn(reel).symbolAt(row);
    }

    public List<Symbol> symbolsOn(Payline payline) {
        return Arrays.stream(Reel.values())
                .map(reel -> at(reel, payline.rowOn(reel)))
                .toList();
    }

}
