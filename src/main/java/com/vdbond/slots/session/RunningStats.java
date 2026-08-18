package com.vdbond.slots.session;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;
import lombok.Getter;

public class RunningStats {

    private static final MathContext PRECISION = MathContext.DECIMAL64;

    @Getter
    private long count;

    private BigDecimal sum = BigDecimal.ZERO;

    private BigDecimal sumOfSquares = BigDecimal.ZERO;

    public void record(BigDecimal value) {
        count++;
        sum = sum.add(value);
        sumOfSquares = sumOfSquares.add(value.multiply(value));
    }

    public Optional<BigDecimal> mean() {
        return count == 0 ? Optional.empty() : Optional.of(sum.divide(BigDecimal.valueOf(count), PRECISION));
    }

    /**
     * How far a single value typically strays from the average one. The values recorded are a sample of everything the
     * game could have paid rather than the whole of it, so the squared deviations are divided by one less than the
     * count; a single value has nothing to vary against and so has no spread to report.
     */
    public Optional<BigDecimal> standardDeviation() {
        if (count < 2) {
            return Optional.empty();
        }
        BigDecimal rounds = BigDecimal.valueOf(count);
        BigDecimal squaredDeviations = rounds.multiply(sumOfSquares).subtract(sum.multiply(sum));
        BigDecimal variance = squaredDeviations.divide(rounds.multiply(rounds.subtract(BigDecimal.ONE)), PRECISION);
        return Optional.of(variance.sqrt(PRECISION));
    }

}
