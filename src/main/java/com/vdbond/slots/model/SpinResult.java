package com.vdbond.slots.model;

import java.math.BigDecimal;
import java.util.List;

public record SpinResult(
        Grid grid,
        List<LineWin> lineWins,
        BigDecimal scatterPayout,
        BigDecimal totalPayout
) {

    public boolean isWin() {
        return totalPayout.signum() > 0;
    }

}
