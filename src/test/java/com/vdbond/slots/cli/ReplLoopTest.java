package com.vdbond.slots.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vdbond.slots.model.GameConfig;
import com.vdbond.slots.model.Payline;
import com.vdbond.slots.model.Reel;
import com.vdbond.slots.model.Row;
import com.vdbond.slots.model.Symbol;
import com.vdbond.slots.session.GameSession;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReplLoopTest {

    @Test
    @DisplayName("--exit reports the session and stops reading, leaving anything typed after it untouched")
    void stopsAtExitWithoutReadingWhatFollows() {
        GameSession session = session("100.00");

        String output = play(session, "--exit", "--deposit 50");
        
        assertTrue(output.contains("This session so far"), output);
        assertTrue(output.contains("Thanks for playing"), output);
        assertAmount("100.00", session.getBalance());
    }

    @Test
    @DisplayName("input running out ends the session the same way --exit does")
    void endsWhenTheInputRunsOut() {
        String output = play(session("100.00"), "--report");

        assertTrue(output.contains("Thanks for playing"), output);
    }

    @Test
    @DisplayName("a spin with no amount stakes the default bet and shows the grid")
    void spinsAtTheDefaultBet() {
        GameSession session = session("100.00");

        String output = play(session, "--spin", "--exit");

        assertTrue(output.contains("You staked 10.00 credits"), output);
        assertTrue(output.contains("Payline 1 — TOP, TOP, TOP"), output);
        assertEquals(1, session.getRounds());
        assertAmount("110.00", session.getBalance());
    }

    @Test
    @DisplayName("a spin with an amount stakes exactly that")
    void spinsAtTheAmountGiven() {
        GameSession session = session("100.00");

        String output = play(session, "--spin 20", "--exit");

        assertTrue(output.contains("You staked 20.00 credits"), output);
        assertAmount("120.00", session.getBalance());
    }

    @Test
    @DisplayName("a deposit is applied, confirmed, and kept out of the staked total")
    void appliesADeposit() {
        GameSession session = session("100.00");

        String output = play(session, "--deposit 50", "--exit");

        assertTrue(output.contains("Added 50.00 credits"), output);
        assertAmount("150.00", session.getBalance());
        assertAmount("0", session.getTotalWagered());
    }

    @Test
    @DisplayName("a bet the balance cannot cover is explained and play carries on")
    void explainsABetTheBalanceCannotCover() {
        GameSession session = session("5.00");

        String output = play(session, "--spin 100", "--report", "--exit");

        assertTrue(output.contains("--deposit"), output);
        assertFalse(output.contains("Exception"), output);
        assertEquals(0, session.getRounds());
        assertAmount("5.00", session.getBalance());
    }

    @Test
    @DisplayName("an unknown command is explained and play carries on")
    void explainsAnUnknownCommand() {
        String output = play(session("100.00"), "--fly", "--exit");

        assertTrue(output.contains("'--fly'"), output);
        assertTrue(output.contains("Thanks for playing"), output);
    }

    @Test
    @DisplayName("a blank line simply gets a fresh prompt")
    void ignoresABlankLine() {
        String output = play(session("100.00"), "", "   ", "--exit");

        assertFalse(output.contains("not something I know how to do"), output);
        assertTrue(output.contains("Thanks for playing"), output);
    }

    @Test
    @DisplayName("--paytable, --rules and --help explain themselves rather than printing bare figures")
    void explainsThePaytableRulesAndHelp() {
        String output = play(session("100.00"), "--paytable", "--rules", "--help", "--exit");

        assertTrue(output.contains("multiple of the stake"), output);
        assertTrue(output.contains("A payline is a fixed path across the three reels"), output);
        assertTrue(output.contains("--spin [amount]"), output);
    }

    private static String play(GameSession session, String... lines) {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(captured, true, StandardCharsets.UTF_8);
        OutputRenderer renderer = new OutputRenderer(out, session.getConfig());
        BufferedReader input = new BufferedReader(new StringReader(String.join("\n", lines) + "\n"));

        new ReplLoop(session, renderer, input).run();

        return captured.toString(StandardCharsets.UTF_8);
    }

    private static GameSession session(String startingBalance) {
        return new GameSession(everySpinWins(), new BigDecimal(startingBalance));
    }

    private static GameConfig everySpinWins() {
        Map<Reel, List<Symbol>> strips = new EnumMap<>(Reel.class);
        strips.put(Reel.FIRST, List.of(Symbol.L1));
        strips.put(Reel.SECOND, List.of(Symbol.L1));
        strips.put(Reel.THIRD, List.of(Symbol.L1));
        Map<Symbol, BigDecimal> paytable = new EnumMap<>(Symbol.class);
        paytable.put(Symbol.L1, new BigDecimal("2"));
        return new GameConfig(new BigDecimal("500.00"), new BigDecimal("10.00"), 0.96, strips,
                List.of(new Payline(0, Row.TOP, Row.TOP, Row.TOP)), paytable);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected %s but was %s".formatted(expected, actual.toPlainString()));
    }

}
