package com.vdbond.slots.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RunningStatsTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.000001");

    @Test
    @DisplayName("nothing recorded yet means no count, no average and no spread")
    void reportsNothingBeforeAnyValueIsRecorded() {
        RunningStats stats = new RunningStats();

        assertEquals(0, stats.getCount());
        assertTrue(stats.mean().isEmpty());
        assertTrue(stats.standardDeviation().isEmpty());
    }

    @Test
    @DisplayName("a single value is its own average and has nothing to vary against")
    void reportsNoSpreadForASingleValue() {
        RunningStats stats = recorded(amounts("-10.00"));

        assertEquals(1, stats.getCount());
        assertAmount("-10.00", stats.mean().orElseThrow());
        assertTrue(stats.standardDeviation().isEmpty());
    }

    @Test
    @DisplayName("values that cancel out average to zero while still reporting their spread")
    void separatesTheAverageFromTheSpread() {
        RunningStats stats = recorded(amounts("-10.00", "0.00", "10.00"));

        assertAmount("0", stats.mean().orElseThrow());
        assertAmount("10", stats.standardDeviation().orElseThrow());
    }

    @Test
    @DisplayName("fractional amounts keep their exact decimal value rather than drifting")
    void keepsFractionalAmountsExact() {
        RunningStats stats = recorded(amounts("0.10", "0.20", "0.30"));

        assertAmount("0.20", stats.mean().orElseThrow());
        assertAmount("0.10", stats.standardDeviation().orElseThrow());
    }

    @Test
    @DisplayName("the average and the spread match a brute-force calculation over the same values")
    void matchesABruteForceCalculation() {
        List<BigDecimal> values = amounts("-10.00", "-10.00", "40.00", "-10.00", "190.00", "-10.00");

        RunningStats stats = recorded(values);

        assertCloseTo(bruteForceMean(values), stats.mean().orElseThrow());
        assertCloseTo(bruteForceStandardDeviation(values), stats.standardDeviation().orElseThrow());
    }

    @Test
    @DisplayName("a long run of spins stays as accurate as a brute-force calculation over all of them")
    void staysAccurateAcrossManyValues() {
        Random random = new Random(42);
        List<BigDecimal> values = Stream.generate(() -> BigDecimal.valueOf(random.nextInt(-1000, 5000)))
                .limit(20_000)
                .toList();

        RunningStats stats = recorded(values);

        assertEquals(20_000, stats.getCount());
        assertCloseTo(bruteForceMean(values), stats.mean().orElseThrow());
        assertCloseTo(bruteForceStandardDeviation(values), stats.standardDeviation().orElseThrow());
    }

    private static RunningStats recorded(List<BigDecimal> values) {
        RunningStats stats = new RunningStats();
        values.forEach(stats::record);
        return stats;
    }

    private static List<BigDecimal> amounts(String... values) {
        return Arrays.stream(values)
                .map(BigDecimal::new)
                .toList();
    }

    private static BigDecimal bruteForceMean(List<BigDecimal> values) {
        BigDecimal sum = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL64);
    }

    private static BigDecimal bruteForceStandardDeviation(List<BigDecimal> values) {
        BigDecimal mean = bruteForceMean(values);
        BigDecimal squaredDeviations = values.stream()
                .map(value -> value.subtract(mean))
                .map(deviation -> deviation.multiply(deviation))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variance = squaredDeviations.divide(BigDecimal.valueOf(values.size() - 1L), MathContext.DECIMAL64);
        return variance.sqrt(MathContext.DECIMAL64);
    }

    private static void assertCloseTo(BigDecimal expected, BigDecimal actual) {
        assertTrue(expected.subtract(actual).abs().compareTo(TOLERANCE) < 0,
                "expected %s but was %s".formatted(expected.toPlainString(), actual.toPlainString()));
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected %s but was %s".formatted(expected, actual.toPlainString()));
    }

}
