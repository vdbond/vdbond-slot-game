package com.vdbond.slots.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vdbond.slots.model.GameConfig;
import com.vdbond.slots.model.Payline;
import com.vdbond.slots.model.Reel;
import com.vdbond.slots.model.Row;
import com.vdbond.slots.model.SpinResult;
import com.vdbond.slots.model.Symbol;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameSessionTest {

    private static final BigDecimal BET = new BigDecimal("10.00");

    @Test
    @DisplayName("a session given no balance of its own opens with the configured one")
    void opensWithTheConfiguredBalance() {
        assertAmount("500.00", new GameSession(everySpinWins()).getBalance());
    }

    @Test
    @DisplayName("a winning spin takes the bet, pays the win back, and counts both")
    void creditsAWinningSpin() {
        GameSession session = session(everySpinWins(), "100.00");

        SpinResult result = session.spin(BET);

        assertAmount("20.00", result.totalPayout());
        assertAmount("110.00", session.getBalance());
        assertEquals(1, session.getRounds());
        assertAmount("10.00", session.getTotalWagered());
        assertAmount("20.00", session.getTotalReturned());
    }

    @Test
    @DisplayName("a losing spin takes the bet and pays nothing back")
    void debitsALosingSpin() {
        GameSession session = session(noSpinEverWins(), "100.00");

        SpinResult result = session.spin(BET);

        assertFalse(result.isWin());
        assertAmount("90.00", session.getBalance());
        assertEquals(1, session.getRounds());
        assertAmount("10.00", session.getTotalWagered());
        assertAmount("0", session.getTotalReturned());
    }

    @Test
    @DisplayName("a spin with no amount named stakes the configured default bet")
    void spinsAtTheDefaultBetWhenNoAmountIsGiven() {
        GameSession session = session(noSpinEverWins(), "100.00");

        session.spin();

        assertAmount("90.00", session.getBalance());
        assertAmount("10.00", session.getTotalWagered());
    }

    @Test
    @DisplayName("a bet larger than the balance is refused, and refusing it changes nothing")
    void refusesABetLargerThanTheBalance() {
        GameSession session = session(everySpinWins(), "5.00");

        InsufficientBalanceException refusal =
                assertThrows(InsufficientBalanceException.class, () -> session.spin(BET));

        assertTrue(refusal.getMessage().contains("--deposit"), refusal.getMessage());
        assertAmount("5.00", session.getBalance());
        assertEquals(0, session.getRounds());
        assertAmount("0", session.getTotalWagered());
    }

    @Test
    @DisplayName("a bet the balance covers to the last credit is allowed")
    void allowsABetThatSpendsTheWholeBalance() {
        GameSession session = session(noSpinEverWins(), "10.00");

        session.spin(BET);

        assertAmount("0", session.getBalance());
        assertEquals(1, session.getRounds());
    }

    @Test
    @DisplayName("a bet of zero or less is refused")
    void refusesANonPositiveBet() {
        GameSession session = session(everySpinWins(), "100.00");

        assertThrows(IllegalArgumentException.class, () -> session.spin(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> session.spin(new BigDecimal("-5.00")));
        assertAmount("100.00", session.getBalance());
        assertEquals(0, session.getRounds());
    }

    @Test
    @DisplayName("a deposit raises the balance without counting as money staked or won")
    void keepsDepositsOutOfTheStakedAndReturnedTotals() {
        GameSession session = session(everySpinWins(), "100.00");
        session.spin(BET);

        session.deposit(new BigDecimal("50.00"));

        assertAmount("160.00", session.getBalance());
        assertEquals(1, session.getRounds());
        assertAmount("10.00", session.getTotalWagered());
        assertAmount("20.00", session.getTotalReturned());
        assertAmount("2", session.report().returnToPlayer().orElseThrow());
    }

    @Test
    @DisplayName("a deposit of zero or less is refused")
    void refusesANonPositiveDeposit() {
        GameSession session = session(everySpinWins(), "100.00");

        assertThrows(IllegalArgumentException.class, () -> session.deposit(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> session.deposit(new BigDecimal("-5.00")));
        assertAmount("100.00", session.getBalance());
    }

    @Test
    @DisplayName("the report counts every round and divides what came back by what was staked")
    void reportsRoundsTotalsAndReturnToPlayer() {
        GameSession session = session(everySpinWins(), "100.00");
        session.spin(BET);
        session.spin(BET);
        session.spin(BET);

        SessionReport report = session.report();

        assertEquals(3, report.rounds());
        assertAmount("30.00", report.totalWagered());
        assertAmount("60.00", report.totalReturned());
        assertAmount("30.00", report.netResult());
        assertAmount("130.00", report.balance());
        assertAmount("2", report.returnToPlayer().orElseThrow());
    }

    @Test
    @DisplayName("a session that has won nothing back reports a return to player of zero")
    void reportsAZeroReturnToPlayerWhenNothingHasPaid() {
        GameSession session = session(noSpinEverWins(), "100.00");
        session.spin(BET);
        session.spin(BET);

        SessionReport report = session.report();

        assertAmount("20.00", report.totalWagered());
        assertAmount("0", report.totalReturned());
        assertAmount("-20.00", report.netResult());
        assertAmount("0", report.returnToPlayer().orElseThrow());
    }

    @Test
    @DisplayName("the report has no return to player and no spread before the first spin")
    void reportsNothingMeasuredBeforeTheFirstSpin() {
        SessionReport report = session(everySpinWins(), "100.00").report();

        assertEquals(0, report.rounds());
        assertAmount("100.00", report.balance());
        assertAmount("0", report.netResult());
        assertTrue(report.returnToPlayer().isEmpty());
        assertTrue(report.standardDeviation().isEmpty());
        assertTrue(report.standardDeviationInBets().isEmpty());
    }

    @Test
    @DisplayName("spins that all end the same way have no spread between them")
    void reportsNoSpreadWhenEverySpinEndsTheSame() {
        GameSession session = session(everySpinWins(), "100.00");
        session.spin(BET);
        session.spin(BET);
        session.spin(BET);

        SessionReport report = session.report();

        assertAmount("0", report.standardDeviation().orElseThrow());
        assertAmount("0", report.standardDeviationInBets().orElseThrow());
    }

    @Test
    @DisplayName("the report measures the spread of net results in credits and in default bets")
    void reportsTheSpreadOfNetResultsBothWays() {
        GameSession session = session(everySpinWins(), "100.00");
        session.spin(new BigDecimal("10.00"));
        session.spin(new BigDecimal("20.00"));
        session.spin(new BigDecimal("30.00"));

        SessionReport report = session.report();

        assertAmount("60.00", report.netResult());
        assertAmount("10", report.standardDeviation().orElseThrow());
        assertAmount("1", report.standardDeviationInBets().orElseThrow());
    }

    private static GameSession session(GameConfig game, String startingBalance) {
        return new GameSession(game, new BigDecimal(startingBalance));
    }

    private static GameConfig everySpinWins() {
        return singleSymbolReels(Symbol.L1, Symbol.L1, Symbol.L1);
    }

    private static GameConfig noSpinEverWins() {
        return singleSymbolReels(Symbol.L1, Symbol.L2, Symbol.L3);
    }
    
    private static GameConfig singleSymbolReels(Symbol firstReel, Symbol secondReel, Symbol thirdReel) {
        Map<Reel, List<Symbol>> strips = new EnumMap<>(Reel.class);
        strips.put(Reel.FIRST, List.of(firstReel));
        strips.put(Reel.SECOND, List.of(secondReel));
        strips.put(Reel.THIRD, List.of(thirdReel));
        return new GameConfig(new BigDecimal("500.00"), BET, 0.96, strips,
                List.of(new Payline(0, Row.TOP, Row.TOP, Row.TOP)), paytable());
    }

    private static Map<Symbol, BigDecimal> paytable() {
        Map<Symbol, BigDecimal> paytable = new EnumMap<>(Symbol.class);
        paytable.put(Symbol.L1, new BigDecimal("2"));
        paytable.put(Symbol.L2, new BigDecimal("3"));
        paytable.put(Symbol.L3, new BigDecimal("5"));
        return paytable;
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected %s but was %s".formatted(expected, actual.toPlainString()));
    }

}
