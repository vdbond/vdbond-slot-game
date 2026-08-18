package com.vdbond.slots.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vdbond.slots.model.GameConfig;
import com.vdbond.slots.model.Payline;
import com.vdbond.slots.model.Reel;
import com.vdbond.slots.model.Row;
import com.vdbond.slots.model.Symbol;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfigLoaderTest {

    private static final String TEMPLATE = """
            {
              "defaultBalance": 10000,
              "defaultBet": 10.00,
              "targetRtp": 0.96,
              "reels": %s,
              "paylines": %s,
              "paytable": %s
            }
            """;

    private static final String REELS = "{ \"FIRST\": [\"L1\"], \"SECOND\": [\"L1\"], \"THIRD\": [\"L1\"] }";
    private static final String PAYLINES = "[[\"TOP\", \"TOP\", \"TOP\"]]";
    private static final String PAYTABLE =
            "{ \"L1\": 10, \"L2\": 15, \"L3\": 20, \"L4\": 50, \"H1\": 80, \"H2\": 500,"
                    + "\"H3\": 800, \"W1\": 2000, \"SCA\": 200 }";

    private static void assertMessageContains(String json, String expected) {
        ConfigLoadException failure = failureFrom(json);
        assertTrue(failure.getMessage().contains(expected),
                "expected the message to mention '%s' but it was: %s".formatted(expected, failure.getMessage()));
    }

    private static ConfigLoadException failureFrom(String json) {
        return assertThrows(ConfigLoadException.class,
                () -> ConfigLoader.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    @DisplayName("the shipped configuration loads into fully typed domain objects")
    void loadsEveryPartOfTheConfiguration() throws Exception {
        GameConfig config;
        try (InputStream json = ConfigLoaderTest.class.getResourceAsStream("/config/test-config.json")) {
            config = ConfigLoader.load(json);
        }

        assertEquals(0, config.defaultBalance().compareTo(new BigDecimal("10000")));
        assertEquals(0, config.defaultBet().compareTo(new BigDecimal("10")));
        assertEquals(0.96, config.targetRtp());

        for (Reel reel : Reel.values()) {
            assertFalse(config.stripFor(reel).isEmpty(), reel + " strip should have symbols");
        }
        assertEquals(List.of(Symbol.L3, Symbol.L3, Symbol.L2), config.stripFor(Reel.FIRST).subList(0, 3));

        assertEquals(5, config.paylines().size());
        Payline diagonal = config.paylines().get(3);
        assertEquals(new Payline(3, Row.TOP, Row.MIDDLE, Row.BOTTOM), diagonal);
        assertEquals(4, diagonal.displayNumber());

        assertEquals(0, new BigDecimal("200.0").compareTo(config.multiplierFor(Symbol.W1)));
        assertEquals(0, new BigDecimal("20.0").compareTo(config.multiplierFor(Symbol.SCA)));
    }

    @Test
    @DisplayName("a missing field is reported by name")
    void rejectsAMissingField() {
        String json = """
                {
                  "defaultBalance": 10000,
                  "targetRtp": 0.96,
                  "reels": %s,
                  "paylines": %s,
                  "paytable": %s
                }
                """.formatted(REELS, PAYLINES, PAYTABLE);

        assertMessageContains(json, "defaultBet");
    }

    @Test
    @DisplayName("text that is not JSON at all is reported as unreadable rather than crashing")
    void rejectsMalformedJson() {
        assertMessageContains("{ \"defaultBalance\": ", "JSON");
    }

    @Test
    @DisplayName("an unknown symbol code names the offender and lists the valid codes")
    void rejectsAnUnknownSymbolCode() {
        String json = TEMPLATE.formatted(
                "{ \"FIRST\": [\"L1\"], \"SECOND\": [\"XX\"], \"THIRD\": [\"L1\"] }", PAYLINES, PAYTABLE
        );

        ConfigLoadException failure = failureFrom(json);
        assertTrue(failure.getMessage().contains("XX"), failure.getMessage());
        assertTrue(failure.getMessage().contains("SECOND"), failure.getMessage());
    }

    @Test
    @DisplayName("a reel with no strip is called out by name")
    void rejectsAMissingReel() {
        String json = TEMPLATE.formatted("{ \"FIRST\": [\"L1\"], \"SECOND\": [\"L1\"] }", PAYLINES, PAYTABLE);

        assertMessageContains(json, "THIRD");
    }

    @Test
    @DisplayName("an empty reel strip is rejected")
    void rejectsAnEmptyReelStrip() {
        String json = TEMPLATE.formatted(
                "{ \"FIRST\": [], \"SECOND\": [\"L1\"], \"THIRD\": [\"L1\"] }", PAYLINES, PAYTABLE
        );

        assertMessageContains(json, "FIRST");
    }

    @Test
    @DisplayName("a payline that does not cover every reel is rejected")
    void rejectsAPaylineWithTheWrongNumberOfRows() {
        String json = TEMPLATE.formatted(REELS, "[[\"TOP\", \"TOP\"]]", PAYTABLE);

        assertMessageContains(json, "Payline 1");
    }

    @Test
    @DisplayName("an unknown row name lists the valid rows")
    void rejectsAnUnknownRowName() {
        String json = TEMPLATE.formatted(REELS, "[[\"TOP\", \"CENTRE\", \"TOP\"]]", PAYTABLE);

        ConfigLoadException failure = failureFrom(json);
        assertTrue(failure.getMessage().contains("CENTRE"), failure.getMessage());
        assertTrue(failure.getMessage().contains("MIDDLE"), failure.getMessage());
    }

    @Test
    @DisplayName("a paytable missing a symbol names what is missing")
    void rejectsAnIncompletePaytable() {
        String json = TEMPLATE.formatted(REELS, PAYLINES, "{ \"L1\": 10 }");

        assertMessageContains(json, "SCA");
    }

}
