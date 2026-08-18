package com.vdbond.slots.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vdbond.slots.config.ConfigLoader;
import com.vdbond.slots.model.GameConfig;
import com.vdbond.slots.session.GameSession;
import com.vdbond.slots.session.SessionReport;
import java.io.InputStream;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

class RtpValidationTest {

    private static final int ROUNDS = 1_000_000;

    private static final double ALLOWED_DRIFT = 0.02;

    private static GameConfig config;

    @BeforeAll
    static void loadConfiguration() throws Exception {
        try (InputStream json = RtpValidationTest.class.getResourceAsStream("/config/test-config.json")) {
            config = ConfigLoader.load(json);
        }
    }

    @RepeatedTest(50)
    @DisplayName("the reel strips and the paytable together pay back the return to player the configuration declares")
    void paysBackTheDeclaredReturnToPlayer() {
        GameSession session = new GameSession(config, config.defaultBet().multiply(BigDecimal.valueOf(ROUNDS)));

        for (int round = 0; round < ROUNDS; round++) {
            session.spin();
        }

        SessionReport report = session.report();
        BigDecimal measured = report.returnToPlayer().orElseThrow();

        assertEquals(config.targetRtp(), measured.doubleValue(), ALLOWED_DRIFT,
                ("%d spins staked %s credits and paid back %s, a return to player of %s against the %s the "
                        + "configuration declares")
                        .formatted(ROUNDS, report.totalWagered().toPlainString(),
                                report.totalReturned().toPlainString(), measured, config.targetRtp()));
    }

}
