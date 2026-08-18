package com.vdbond.slots.cli;

import com.vdbond.slots.engine.PayoutEvaluator;
import com.vdbond.slots.model.GameConfig;
import com.vdbond.slots.model.Grid;
import com.vdbond.slots.model.LineWin;
import com.vdbond.slots.model.Payline;
import com.vdbond.slots.model.Reel;
import com.vdbond.slots.model.Row;
import com.vdbond.slots.model.SpinResult;
import com.vdbond.slots.model.Symbol;
import com.vdbond.slots.model.SymbolKind;
import com.vdbond.slots.session.SessionReport;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OutputRenderer {

    private static final String INDENT = "      ";

    private static final int MONEY_SCALE = 2;

    private static final int RATE_SCALE = 2;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final PrintStream out;

    private final GameConfig config;

    public void prompt() {
        out.print("> ");
        out.flush();
    }

    public void welcome(BigDecimal balance) {
        out.println();
        out.println("Three reels, three rows, and play money only — nothing here costs or pays anything real.");
        out.printf(
                "You are starting with %s credits, and a spin stakes %s credits unless you name another amount.%n",
                credits(balance),
                credits(config.defaultBet())
        );
    }

    public void help() {
        out.println();
        out.println("What you can type:");
        out.println(usage("--spin [amount]", "Spin the reels. With no amount, you stake the default %s credits."
                .formatted(credits(config.defaultBet()))));
        out.println(usage("--deposit <amount>", "Top your balance up. Deposits never count as money staked or won."));
        out.println(usage("--paytable", "What each symbol pays when three of them line up."));
        out.println(usage("--rules", "The full walkthrough: reels, paylines, the wild, the scatter, and RTP."));
        out.println(usage("--report", "How this session is going so far."));
        out.println(usage("--help", "This list."));
        out.println(usage("--exit", "Show the session report one last time, then quit."));
        out.println();
        out.println("Amounts are plain numbers and may have decimals, for example: --spin 2.50");
    }

    public void spin(BigDecimal bet, SpinResult result, BigDecimal balance) {
        out.println();
        out.printf("You staked %s credits, and the reels landed like this:%n", credits(bet));
        out.println();
        grid(result.grid());
        out.println();
        if (result.isWin()) {
            result.lineWins()
                    .forEach(win -> lineWin(win, result.grid()));
            if (result.scatterPayout().signum() > 0) {
                scatterWin(result);
            }
            out.println();
        } else {
            out.printf(
                    "Nothing lined up this time — no payline held three matching symbols, and fewer than %d%n",
                    PayoutEvaluator.SCATTERS_NEEDED
            );
            out.println("scatters landed on the grid.");
            out.println();
        }
        out.println(outcome(bet, result.totalPayout(), balance));
    }

    public void deposit(BigDecimal amount, BigDecimal balance) {
        out.println();
        out.printf("Added %s credits, so your balance is now %s credits.%n", credits(amount), credits(balance));
        out.println("A deposit is not a stake, so it leaves your return-to-player figure exactly where it was.");
    }

    public void paytable() {
        out.println();
        out.printf(
                "Every payout is a multiple of the stake you put on that one spin. At the default %s-credit%n",
                credits(config.defaultBet())
        );
        out.printf(
                "stake, a symbol paying 5x returns %s credits; at half the stake it would return half as much.%n",
                credits(config.defaultBet().multiply(new BigDecimal("5")))
        );
        out.println();
        out.printf("     %-5s %-9s %s%n", "Code", "Tier", "Three of them on a payline pay");
        Arrays.stream(Symbol.values())
                .forEach(symbol -> out.printf(
                        "  %s %-5s %-9s %s%n", symbol.getEmoji(), symbol.name(), tier(symbol), pays(symbol)
                ));
        out.println();
        out.printf("  %s %s%n", Symbol.W1.getEmoji(), SymbolKind.WILD.getDescription());
        out.printf(
                "  %s %s, so three of them anywhere on the grid pay wherever they land.%n",
                Symbol.SCA.getEmoji(), SymbolKind.SCATTER.getDescription()
        );
    }

    public void rules() {
        out.println();
        out.print("""
                How this game works
                
                The screen
                  Three reels spin, and each one stops showing three symbols, so you always see a 3x3 grid.
                  A reel is a long strip of symbols joined end to end into a loop. A spin picks a random
                  stopping point on each strip and shows you the symbol there plus the two after it, wrapping
                  round to the start of the strip if it runs off the end. How often a symbol appears on a
                  strip is how often you see it, so the strips are where this game's odds really live.
                
                Paylines
                  A payline is a fixed path across the three reels — one row on each. Only symbols sitting on
                  a payline can win it, and a payline pays only when all three of its cells hold the same
                  symbol. Paylines are judged one at a time and independently of each other, so a single spin
                  can win on several at once and every winner pays in full.
                """);
        out.println("  This game has %d paylines, and all of them are live on every spin, covered by the one"
                .formatted(config.paylines().size()));
        out.println("  stake you put on that spin:");
        config.paylines().forEach(payline -> out.println("    Payline %d — %s"
                .formatted(payline.displayNumber(), rowNames(payline))));
        out.println();
        out.println("The wild %s (%s)".formatted(Symbol.W1.getEmoji(), Symbol.W1.name()));
        out.print("""
                  A wild stands in for whatever symbol a payline needs to complete it — anything except the
                  scatter. Because every payline is judged on its own, a wild is never used up: one wild
                  sitting where two paylines cross completes both of them, standing in for a different symbol
                  on each if that is what the two lines need.
                """);
        out.println("  Three wilds on a payline pay at the wild's own rate: %s.".formatted(pays(Symbol.W1)));
        out.print("""
                  The grid is always shown exactly as it landed, wilds included, and each win is explained on
                  its own line instead — with one wild able to pay as two different symbols at once, no single
                  redrawn grid could show every win truthfully.
                """);
        out.println();
        out.println("The scatter %s (%s)".formatted(Symbol.SCA.getEmoji(), Symbol.SCA.name()));
        out.println("  The scatter ignores paylines completely. Land %d or more of them anywhere on the grid,"
                .formatted(PayoutEvaluator.SCATTERS_NEEDED));
        out.println("  on any rows and any reels, and they pay %s.".formatted(pays(Symbol.SCA)));
        out.print("""
                  It is the one symbol whose position does not matter, and the one symbol no wild may stand in
                  for. A scatter landing on a payline never wins that line.
                
                Return to player
                  Return to player, or RTP, is the share of everything staked that a game pays back as winnings
                  over a very long run.
                """);
        out.printf(
                "  This one is tuned to %s%%, so across millions of spins it pays back about %s credits for%n",
                percent(BigDecimal.valueOf(config.targetRtp())), percent(BigDecimal.valueOf(config.targetRtp()))
        );
        out.println("  every 100 staked and keeps the rest as the house edge.");
        out.print("""
                  It promises nothing about any one session: short runs swing far above and far below it, which
                  is exactly what --report measures for the session you are playing.
                """);
    }

    public void report(SessionReport report) {
        out.println();
        out.println("This session so far");
        out.println(figure("Rounds played", Long.toString(report.rounds())));
        out.println(figure("Balance", "%s credits".formatted(credits(report.balance()))));
        out.println(figure("Total staked", "%s credits".formatted(credits(report.totalWagered()))));
        out.println(figure("Total won back", "%s credits".formatted(credits(report.totalReturned()))));
        out.println(figure("Net result", netResult(report)));
        out.println(figure("Return to player", returnToPlayer(report)));
        out.println(figure("Swing per spin", swingPerSpin(report)));
        out.println();
        out.println("  Return to player is how much has come back to you as wins, as a share of everything you");
        out.println("  have staked. This game is tuned to return %s%% over a very long run, and a session as"
                .formatted(percent(BigDecimal.valueOf(config.targetRtp()))));
        out.println("  short as yours can land far either side of that.");
        out.println("  Swing per spin is how far a typical single spin lands from your average one: the bigger");
        out.println("  it is, the rougher the ride between the wins.");
    }

    public void goodbye(SessionReport report) {
        report(report);
        out.println();
        out.println("Thanks for playing. No real money was staked, won, or lost.");
    }

    public void error(String message) {
        out.println();
        out.println(message);
    }

    private void grid(Grid grid) {
        Arrays.stream(Row.values()).forEach(row -> out.println(("  %-8s%s".formatted(row, Arrays.stream(Reel.values())
                .map(reel -> "%s %-6s".formatted(grid.at(reel, row).getEmoji(), grid.at(reel, row).name()))
                .collect(Collectors.joining()))).stripTrailing()));
    }

    private void lineWin(LineWin win, Grid grid) {
        out.println("  Payline %d — %s -> %s".formatted(
                win.payline().displayNumber(), rowNames(win.payline()), emojis(win.landedSymbols())));
        out.println("%sThree of %s pays %s — %s credits."
                .formatted(INDENT, name(win.symbol()), pays(win.symbol()), credits(win.payout())));
        if (win.symbol().isWild()) {
            out.println("%sAll three cells were wilds, so the line paid at the wild's own rate.".formatted(INDENT));
        } else if (win.involvedWild()) {
            out.println("%sThe wild %s on %s stood in for %s, making it %s.".formatted(
                    INDENT, name(Symbol.W1), wildPositions(win, grid), name(win.symbol()),
                    emojis(win.resolvedSymbols())));
        }
    }

    private void scatterWin(SpinResult result) {
        out.println("  Scatters — %d %s landed on the grid, on no payline in particular."
                .formatted(scatterCount(result.grid()), name(Symbol.SCA)));
        out.println("%sPays %s — %s credits.".formatted(INDENT, pays(Symbol.SCA), credits(result.scatterPayout())));
    }

    private String wildPositions(LineWin win, Grid grid) {
        return Arrays.stream(Reel.values())
                .filter(reel -> grid.at(reel, win.payline().rowOn(reel)).isWild())
                .map(reel -> "the %s row of the %s reel".formatted(win.payline().rowOn(reel), reelName(reel)))
                .collect(Collectors.joining(" and "));
    }

    private long scatterCount(Grid grid) {
        return Arrays.stream(Reel.values())
                .flatMap(reel -> Arrays.stream(Row.values()).map(row -> grid.at(reel, row)))
                .filter(Symbol::isScatter)
                .count();
    }

    private String outcome(BigDecimal bet, BigDecimal payout, BigDecimal balance) {
        BigDecimal net = payout.subtract(bet);
        String ending = "Your balance is now %s credits.".formatted(credits(balance));
        if (payout.signum() == 0) {
            return "That leaves your %s stake behind. %s".formatted(credits(bet), ending);
        }
        return switch (net.signum()) {
            case 1 -> "You won %s credits back, %s more than you staked. %s"
                    .formatted(credits(payout), credits(net), ending);
            case -1 -> "You won %s credits back, %s short of your stake. %s"
                    .formatted(credits(payout), credits(net.negate()), ending);
            default -> "You won your %s stake back exactly, so the spin cost you nothing. %s"
                    .formatted(credits(bet), ending);
        };
    }

    private String netResult(SessionReport report) {
        BigDecimal net = report.netResult();
        return switch (net.signum()) {
            case 1 -> "up %s credits".formatted(credits(net));
            case -1 -> "down %s credits".formatted(credits(net.negate()));
            default -> "even — you have won back exactly what you staked";
        };
    }

    private String returnToPlayer(SessionReport report) {
        return report.returnToPlayer()
                .map(rate -> "%s%% — %s credits back for every 100 staked".formatted(percent(rate), percent(rate)))
                .orElse("nothing staked yet, so there is nothing to measure");
    }

    private String swingPerSpin(SessionReport report) {
        return report.standardDeviation()
                .flatMap(spread -> report.standardDeviationInBets()
                        .map(inBets -> "%s credits, or %sx the default %s-credit stake"
                                .formatted(credits(spread), rate(inBets), credits(config.defaultBet()))))
                .orElse("it takes two spins before there is any spread to measure");
    }

    private String pays(Symbol symbol) {
        BigDecimal multiplier = config.multiplierFor(symbol);
        return multiplier.signum() == 0
                ? "nothing on their own"
                : "%sx your stake".formatted(multiplier.stripTrailingZeros().toPlainString());
    }

    private String tier(Symbol symbol) {
        return switch (symbol.getKind()) {
            case LOW -> "low";
            case HIGH -> "high";
            case WILD -> "wild";
            case SCATTER -> "scatter";
        };
    }

    private String reelName(Reel reel) {
        return switch (reel) {
            case FIRST -> "first";
            case SECOND -> "second";
            case THIRD -> "third";
        };
    }

    private String name(Symbol symbol) {
        return "%s (%s)".formatted(symbol.getEmoji(), symbol.name());
    }

    private String emojis(List<Symbol> symbols) {
        return symbols.stream().map(Symbol::getEmoji).collect(Collectors.joining(" "));
    }

    private String rowNames(Payline payline) {
        return payline.rows().stream().map(Row::name).collect(Collectors.joining(", "));
    }

    private String usage(String command, String description) {
        return "  %-20s %s".formatted(command, description);
    }

    private String figure(String label, String value) {
        return "  %-18s %s".formatted(label, value);
    }

    private String credits(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    private String rate(BigDecimal multiple) {
        return multiple.setScale(RATE_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    private String percent(BigDecimal fraction) {
        return fraction.multiply(HUNDRED)
                .setScale(RATE_SCALE, RoundingMode.HALF_UP)
                .toPlainString();
    }

}
