package com.vdbond.slots.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommandParserTest {

    @Test
    @DisplayName("--spin on its own names no stake")
    void parsesASpinWithoutAnAmount() {
        assertEquals(new Spin(Optional.empty()), CommandParser.parse("--spin"));
    }

    @Test
    @DisplayName("--spin with an amount keeps it exactly as typed, decimals included")
    void parsesASpinWithAnAmount() {
        assertEquals(new Spin(Optional.of(new BigDecimal("12.50"))), CommandParser.parse("--spin 12.50"));
    }

    @Test
    @DisplayName("--deposit takes its amount")
    void parsesADeposit() {
        assertEquals(new Deposit(new BigDecimal("50")), CommandParser.parse("--deposit 50"));
    }

    @Test
    @DisplayName("the commands that take no arg parse to their own variants")
    void parsesTheCommandsThatTakeNoArg() {
        assertEquals(new ShowPaytable(), CommandParser.parse("--paytable"));
        assertEquals(new ShowRules(), CommandParser.parse("--rules"));
        assertEquals(new ShowReport(), CommandParser.parse("--report"));
        assertEquals(new ShowHelp(), CommandParser.parse("--help"));
        assertEquals(new Exit(), CommandParser.parse("--exit"));
    }

    @Test
    @DisplayName("stray spacing and capitals are forgiven, and so are missing dashes")
    void forgivesSpacingCapitalsAndMissingDashes() {
        assertEquals(new ShowHelp(), CommandParser.parse("   --HELP  "));
        assertEquals(new Spin(Optional.of(new BigDecimal("25"))), CommandParser.parse("--spin    25"));
        assertEquals(new Exit(), CommandParser.parse("exit"));
    }

    @Test
    @DisplayName("a deposit with no amount says what to type instead")
    void refusesADepositWithNoAmount() {
        CommandParseException refusal =
                assertThrows(CommandParseException.class, () -> CommandParser.parse("--deposit"));

        assertTrue(refusal.getMessage().contains("--deposit 100"), refusal.getMessage());
    }

    @Test
    @DisplayName("an amount that is not a number is quoted back with an example")
    void refusesAnAmountThatIsNotANumber() {
        CommandParseException refusal =
                assertThrows(CommandParseException.class, () -> CommandParser.parse("--spin loads"));

        assertTrue(refusal.getMessage().contains("'loads'"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("--spin 25"), refusal.getMessage());
    }

    @Test
    @DisplayName("more than one amount is refused rather than quietly using the first")
    void refusesMoreThanOneAmount() {
        assertThrows(CommandParseException.class, () -> CommandParser.parse("--spin 10 20"));
        assertThrows(CommandParseException.class, () -> CommandParser.parse("--deposit 10 20"));
    }

    @Test
    @DisplayName("an amount typed after a command that takes none is refused")
    void refusesAnAmountOnACommandThatTakesNone() {
        CommandParseException refusal =
                assertThrows(CommandParseException.class, () -> CommandParser.parse("--report 10"));

        assertTrue(refusal.getMessage().contains("--report"), refusal.getMessage());
    }

    @Test
    @DisplayName("an unknown command is quoted back and points at --help")
    void refusesAnUnknownCommand() {
        CommandParseException refusal =
                assertThrows(CommandParseException.class, () -> CommandParser.parse("--fly"));

        assertTrue(refusal.getMessage().contains("'--fly'"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("--help"), refusal.getMessage());
    }

    @Test
    @DisplayName("an empty line asks for a command rather than failing silently")
    void refusesAnEmptyLine() {
        CommandParseException refusal =
                assertThrows(CommandParseException.class, () -> CommandParser.parse("   "));

        assertTrue(refusal.getMessage().contains("--help"), refusal.getMessage());
    }

}
