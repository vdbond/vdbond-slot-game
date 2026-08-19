package com.vdbond.slots.session;

import com.vdbond.slots.engine.PayoutEvaluator;
import com.vdbond.slots.engine.ReelSpinner;
import com.vdbond.slots.model.GameConfig;
import com.vdbond.slots.model.SpinResult;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;
import lombok.Getter;

@Getter
public class GameSession {

    private final GameConfig config;

    private final ReelSpinner spinner;

    private final RunningStats netResultPerSpin = new RunningStats();

    private BigDecimal balance;

    private long rounds;

    private BigDecimal totalWagered = BigDecimal.ZERO;

    private BigDecimal totalReturned = BigDecimal.ZERO;

    public GameSession(GameConfig config, BigDecimal startingBalance) {
        this.config = config;
        this.spinner = new ReelSpinner();
        this.balance = requirePositive(startingBalance, "A starting balance");
    }

    public SpinResult spin() {
        return spin(config.defaultBet());
    }

    public SpinResult spin(BigDecimal bet) {
        requirePositive(bet, "A bet");
        if (balance.compareTo(bet) < 0) {
            throw new InsufficientBalanceException(
                    ("Your balance is %s credits, which is not enough for a bet of %s. Bet less than that, or top "
                            + "your balance up with --deposit <amount>.")
                            .formatted(balance.toPlainString(), bet.toPlainString()));
        }
        balance = balance.subtract(bet);
        SpinResult result = PayoutEvaluator.evaluate(spinner.spin(config), config.paylines(), config.paytable(), bet);
        balance = balance.add(result.totalPayout());
        rounds++;
        totalWagered = totalWagered.add(bet);
        totalReturned = totalReturned.add(result.totalPayout());
        netResultPerSpin.record(result.totalPayout().subtract(bet));
        return result;
    }

    public void deposit(BigDecimal amount) {
        balance = balance.add(requirePositive(amount, "A deposit"));
    }

    public SessionReport report() {
        Optional<BigDecimal> standardDeviation = netResultPerSpin.standardDeviation();
        return new SessionReport(rounds, balance, totalWagered, totalReturned, standardDeviation,
                standardDeviation.map(spread -> spread.divide(config.defaultBet(), MathContext.DECIMAL64)));
    }

    private static BigDecimal requirePositive(BigDecimal amount, String what) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "%s must be more than zero credits, but was %s".formatted(what, amount.toPlainString()));
        }
        return amount;
    }

}
