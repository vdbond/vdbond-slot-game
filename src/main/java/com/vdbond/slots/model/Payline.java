package com.vdbond.slots.model;

import java.util.List;

public record Payline(int index, Row firstReel, Row secondReel, Row thirdReel) {

    public Row rowOn(Reel reel) {
        return switch (reel) {
            case FIRST -> firstReel;
            case SECOND -> secondReel;
            case THIRD -> thirdReel;
        };
    }

    public List<Row> rows() {
        return List.of(firstReel, secondReel, thirdReel);
    }

    public int displayNumber() {
        return index + 1;
    }

}
