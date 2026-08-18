package com.vdbond.slots.session;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

public record SessionReport(
        long rounds,
        BigDecimal balance,
        BigDecimal totalWagered,
        BigDecimal totalReturned,
        Optional<BigDecimal> standardDeviation,
        Optional<BigDecimal> standardDeviationInBets
) {

    public BigDecimal netResult() {
        return totalReturned.subtract(totalWagered);
    }
    
    public Optional<BigDecimal> returnToPlayer() {
        return totalWagered.signum() == 0
                ? Optional.empty()
                : Optional.of(totalReturned.divide(totalWagered, MathContext.DECIMAL64));
    }

}
