# Slot Machine CLI Simulator

A 3×3 slot machine you play in the terminal. Play money only — nothing here costs or pays anything real.

Reel strips, paylines, wild substitution, scatter pays and a return-to-player figure that is measured
rather than asserted: the machinery a slot game actually runs on, config-driven end to end, and
explained on screen as you play.

## Running it

```
./play.sh                  # start a session
./play.sh --balance 500    # start with a different bankroll
```

`./gradlew run -q` works too.

## What it looks like

```
> --spin

You staked 10.00 credits, and the reels landed like this:

  TOP     🍋 L2    🔔 H2    🔔 H2
  MIDDLE  ⭐ W1    🍋 L2    🍋 L2
  BOTTOM  🍇 L3    🍋 L2    🍋 L2

  Payline 2 — MIDDLE, MIDDLE, MIDDLE -> ⭐ 🍋 🍋
      Three of 🍋 (L2) pays 2x your stake — 20.00 credits.
      The wild ⭐ (W1) on the MIDDLE row of the first reel stood in for 🍋 (L2), making it 🍋 🍋 🍋.
  Payline 4 — TOP, MIDDLE, BOTTOM -> 🍋 🍋 🍋
      Three of 🍋 (L2) pays 2x your stake — 20.00 credits.

You won 40.00 credits back, 30.00 more than you staked. Your balance is now 10030.00 credits.
```

That one wild pays twice, on two different lines, from a single cell — which is also why the grid is
printed exactly as it landed and never redrawn with the wild swapped out. Every win explains itself
instead.

`--report` tracks the session as it goes:

```
This session so far
  Rounds played      12
  Balance            9975.00 credits
  Total staked       120.00 credits
  Total won back     95.00 credits
  Net result         down 25.00 credits
  Return to player   79.17% — 79.17 credits back for every 100 staked
  Swing per spin     28.40 credits, or 2.84x the default 10.00-credit stake
```

Type `--rules` in the game for the full walkthrough, or `--paytable` for what each symbol pays.

## How the game works

- **The reels.** Each of the three reels is a long strip of symbols joined into a loop. A spin picks a
  random stopping point on each strip and shows the symbol there plus the two after it, wrapping round
  at the end. How often a symbol appears on a strip is how often you see it — the strips are where the
  odds live, not a table of probabilities somewhere.
- **The paylines.** Five fixed paths across the grid, one row per reel. Each is judged on its own, so a
  spin can win on several at once and every winner pays in full.
- **The wild (⭐).** Stands in for any symbol except the scatter. Because paylines are judged
  independently, a wild is never used up — one sitting where two lines cross completes both, and can
  stand in for a different symbol on each.
- **The scatter (🎰).** Ignores paylines entirely: three or more anywhere on the grid pay, wherever
  they land. It is the one symbol no wild may replace.
- **The money.** One flat stake covers the whole spin, and every payout is a multiple of it. All money
  is `BigDecimal` end to end — balance, bet, payout, and the running totals.

## Configuration

Every gameplay number lives in `src/main/resources/config/default-config.json`: starting balance,
default bet, target RTP, the three reel strips, the payline layout, and the paytable. Change the strips
or the multipliers and you have a different game, with no code touched — including deliberately silly
setups like an RTP well above 100%. `--paytable`, `--rules` and `--report` are all generated from that
file, so they can't describe a game other than the one being played.

## Is the RTP actually 96%?

It's tested rather than claimed. `RtpValidationTest` runs 50 repetitions of a million rounds each
through the real engine and the real `SecureRandom`, then asserts the measured return-to-player lands
within 0.02 of whatever `targetRtp` the config declares — a self-consistency check, so it stays honest
if the paytable is retuned.

## Built with

Java 25 and Gradle 9.1. Jackson 3 (`JsonMapper`) is the only runtime dependency; Lombok is compile-time
only and never on the runtime classpath; JUnit 6 for tests; Checkstyle (Google's style, four documented
deviations) at zero tolerance.