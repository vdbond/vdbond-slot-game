# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A play-money-only, interactive command-line slot machine simulator in Java: fixed 3×3 grid, config-driven reels/paylines/paytable, `SecureRandom` spins, wild substitution, scatter payouts, and live session/RTP reporting, all wrapped in a REPL. No real money, accounts, or external services are involved anywhere — a session runs entirely in memory against a local JSON config file.

Stack: Java 25, Gradle 9.1.0, `tools.jackson.core:jackson-databind` as the only runtime dependency (via `JsonMapper`), Lombok compile-time only, JUnit 6, Checkstyle at zero tolerance.

## Do not run the build yourself

**Do not execute `./gradlew build`, `test`, `run`, or `installDist` — nor `./play.sh`, which wraps the last two — on the user's behalf.** This is a standing instruction from the user: write the code and describe how to verify it, and let the user run and verify builds, tests, and manual REPL sessions themselves.

## Commands (for reference / to tell the user, not to run)

- Build + test: `./gradlew build` (runs checkstyle and tests; `checkstyle.maxWarnings = 0`, so any checkstyle warning fails the build)
- Tests only: `./gradlew test`
- Single test class: `./gradlew test --tests "com.vdbond.slots.engine.PayoutEvaluatorTest"`
- Single test method: `./gradlew test --tests "com.vdbond.slots.session.RunningStatsTest.methodName"`
- Checkstyle only: `./gradlew checkstyleMain checkstyleTest`
- Run the REPL: `./play.sh` (`installDist` + the generated `build/install/slots/bin/slots` start script, so the game runs as a plain `java` process outside Gradle — nothing but the game prints and Ctrl-C/Ctrl-D reach the REPL). Args pass straight through: `./play.sh --balance 500`
- Run the REPL through Gradle instead: `./gradlew run -q` (`--args="--balance 500"` for launch args). The `run` task has `standardInput = System.in` wired so the prompt can read the console; `-q` drops Gradle's lifecycle logging and `org.gradle.console=plain` in `gradle.properties` stops the animated progress bar repainting over the prompt — do not remove either without replacing them
- Both launch paths run whatever `java` is on `PATH`/`JAVA_HOME`, not necessarily the Java 25 toolchain Gradle compiles with. An `UnsupportedClassVersionError` at startup means `JAVA_HOME` needs a JDK 25 (`export JAVA_HOME=$(/usr/libexec/java_home -v 25)` on macOS)

## Game rules the code has to honor

These are the mechanics the implementation encodes. Restructuring code around them is fine; changing them changes the game.

- The grid is a fixed 3×3 over three reels — **not** configurable.
- A reel strip is a loop: a spin picks a random stop index and shows that symbol plus the next two, wrapping circularly. The strips are where the odds live — how often a symbol appears on a strip is how often the player sees it, so there is no probability table anywhere.
- Paylines are arbitrary row-triples from config and pay only on an exact 3-of-a-kind. Each payline is evaluated independently, so a spin can win several at once and a wild is never "used up": one wild sitting where two lines cross completes both, standing in for a different symbol on each.
- Wild (`W1`) substitutes for anything except scatter, and has its own multiplier when three wilds line up.
- Scatter (`SCA`) is carved out of payline matching entirely — a payline containing a scatter never wins, and no wild may stand in for it. Three **or more** scatters anywhere in the grid pay a flat `multiplier × bet`, so a grid showing four pays the same as three rather than nothing.
- Flat-bet model: one bet per spin, and every winning payline pays `multiplier × bet` regardless of how many paylines are configured.
- A paytable multiplier of `0` is the configured way to say "this symbol never pays on its own"; it must yield no `LineWin` at all, not one worth nothing.
- The grid is reported exactly as it landed and is never redrawn with wilds swapped out: since one wild can pay as two different symbols at once, no single redrawn grid could show every win truthfully. Each win is explained on its own line instead — the rows its payline ran through, the symbols that landed there, and what they paid as.
- RTP is measured, not claimed. `RtpValidationTest` (in `com.vdbond.slots.simulation`) is a `@RepeatedTest(50)` of 1,000,000 rounds each, spun through a real `GameSession` opened with a bankroll of `defaultBet × rounds` so it can never go bust mid-run, asserting `report().returnToPlayer()` against the loaded config's own `targetRtp` within `0.02`. It is a self-consistency check against whatever config is on the classpath — never a hardcoded percentage — so retuning the paytable keeps it honest.

## Architecture

Package layout under `com.vdbond.slots`, each package a distinct layer:

- **`model`** — pure domain types, no logic beyond derived queries. `Symbol` (enum: `L1-L4` low, `H1-H3` high, `W1` wild, `SCA` scatter; enum constant name doubles as its JSON code) + `SymbolKind`. `Reel` (`FIRST/SECOND/THIRD`) and `Row` (`TOP/MIDDLE/BOTTOM`) are enums, never raw ints — grid coordinates are always looked up via exhaustive `switch`. `Grid` holds three `ReelWindow`s; `Payline` is a row-triple; `LineWin`/`SpinResult` carry results; `GameConfig` is the fully-parsed, immutable config.
- **`config`** — `ConfigDto` (raw Jackson-bound shape, `String`-keyed) → `ConfigLoader` (a Lombok `@UtilityClass`) converts it into the strongly-typed `GameConfig`, resolving symbol/reel/row codes and validating everything (missing fields, unknown codes, non-positive amounts) with player-friendly `ConfigLoadException` messages. `src/main/resources/config/default-config.json` and `src/test/resources/config/test-config.json` hold the same content but are **not kept in sync automatically** — a change to reels/paytable/RTP has to be made in both files by hand.
- **`engine`** — stateless evaluation. `ReelSpinner` is a small class holding a `RandomGenerator` (no-arg constructor defaults it to `SecureRandom`, so tests can pin stop positions); it picks a random stop index per reel strip and reads 3 consecutive symbols wrapping circularly. `PayoutEvaluator` is a Lombok `@UtilityClass`: it evaluates each payline independently and pays scatters separately from paylines. `Symbol.isReplaceable()` (true for everything except `SCATTER`) is what wilds check before substituting.
- **`session`** — `GameSession` is the stateful entry point: holds `GameConfig`, balance, and a `RunningStats` accumulator; `spin()`/`spin(bet)` debit, run the engine, credit, and update stats; `deposit()` is stats-neutral; `report()` produces a `SessionReport`. A bet or deposit of zero or less throws `IllegalArgumentException`, a bet the balance cannot cover throws `InsufficientBalanceException` pointing the player at `--deposit`. `SessionReport` derives `netResult()` and `returnToPlayer()`, the latter `Optional.empty()` until something has been staked rather than a misleading zero. `RunningStats` streams count/sum/sum-of-squares as `BigDecimal` for constant-memory mean/standard-deviation over arbitrarily many rounds.
- **`cli`** — the REPL layer. `Command` is a sealed interface with an explicit `permits`, and each variant (`Spin`, `Deposit`, `ShowPaytable`, `ShowRules`, `ShowReport`, `ShowHelp`, `Exit`) lives in its own file. `CommandParser` (a Lombok `@UtilityClass`) turns a line in `--flag value` form into a `Command`, matching case-insensitively with the `--` optional, and throws `CommandParseException` with a friendly message otherwise. `OutputRenderer` holds a `PrintStream` and the `GameConfig` and owns **all** player-facing text; `ReplLoop` reads lines, dispatches through an exhaustive `switch` over `Command`, and catches exactly three exceptions (`CommandParseException`, `InsufficientBalanceException`, `IllegalArgumentException`) to print them plainly. End of input ends the session exactly like `--exit`, report included. `Main` loads the config off the classpath, parses an optional `--balance` launch arg, prints the welcome + help, and enters the loop.

Data flow for a spin: `ReplLoop` → `CommandParser` parses input → `GameSession.spin(bet)` → `ReelSpinner.spin(config)` builds a `Grid` → `PayoutEvaluator.evaluate(...)` produces a `SpinResult` → `GameSession` updates balance/stats → `OutputRenderer` prints the win, line by line, naming the payline's rows and the symbols on them.

## Settled decisions — do not reopen

Each of these was deliberated and decided; revisit only if the user asks.

- **One file per `Command` variant**, matching how `Symbol`/`Reel`/`Row` are laid out, rather than nesting the records inside the sealed interface — hence the explicit `permits` clause.
- **The config file is duplicated, not shared.** The app loads the main-resources copy, the tests load the test-resources one, and nothing keeps them in step automatically.
- **No `displayGrid` on `SpinResult`.** Each `LineWin` carries `landedSymbols` (what the player saw) plus `resolvedSymbols` (what it paid as); that pair is what lets a win be explained line by line without redrawing the grid.
- **`RunningStats` uses a running count/sum/sum-of-squares — not Welford's algorithm — over `BigDecimal`, not `double`.** Welford exists to stop floating point losing the difference between two large sums, which exact decimal arithmetic never does, so the textbook variance formula is both simpler and more accurate here, with rounding entering once at the final division (`MathContext.DECIMAL64`). It reports sample rather than population spread (divide by `count - 1`), so `mean()` is empty with nothing recorded and `standardDeviation()` is empty below two values.
- **`GameSession` builds its own `ReelSpinner`** — no injected spinner and no rigged generator; tests fix a spin's outcome with one-symbol reel strips instead.
- **`SessionReport` carries no execution-time figure** — do not add timing to it.
- **Input is forgiving, output is canonical**: a command matches case-insensitively with the leading `--` optional (`--SPIN`, `--spin`, `spin` are one command), but everything printed stays in `--flag` form.

## Conventions specific to this repo

- **All money is `BigDecimal`** — balance, bet, payout, wagered, returned, paytable multipliers. `targetRtp` is the one deliberate exception, kept as a primitive `double` since it's never multiplied into money.
- **No raw positional ints for grid coordinates** — see `model` above. This is a standing rule, not just how the existing code happens to look.
- **No defensive copying** (`List.copyOf`, `Map.copyOf`, array clones) unless a concrete mutation risk is demonstrated — nothing here mutates a collection it was handed.
- **Comments/Javadoc only for genuinely non-obvious things** (a game rule the code doesn't convey, a subtle trade-off, a "why") — never a restatement of the code. Checkstyle's `MissingJavadocMethod`/`MissingJavadocType` are deliberately removed from `config/checkstyle/checkstyle.xml` to match this policy.
- **Every file ends with a trailing newline.**
- **Never pad an emoji with `%-Ns`** — emoji are 1–3 chars long (`7️⃣` is three) but two display cells wide, so padding by character count wrecks column alignment. Put the emoji first in a cell and pad only the ASCII after it, as `OutputRenderer.grid`/`paytable` do.
- **RTP is a fraction everywhere except the screen** — config, engine, `SessionReport` and tests all keep `0.96`; `OutputRenderer` is the only place it becomes `96.00%`.
- **`--paytable`, `--rules` and `--report` are generated from `GameConfig`** (payline list, multipliers, default bet, target RTP) so they cannot drift from the game actually being played; `PayoutEvaluator.SCATTERS_NEEDED` is public for that reason.
- **All player-facing CLI text is plain-language** — win explanations, errors, `--paytable`, `--rules`, and validation messages avoid jargon and explain mechanics, not just state them (see the friendly messages already in `ConfigLoader`, `GameSession`, and exception classes as the model to follow).
- **Payout tests draw the grid.** `ReelWindow` is constructed per reel (column) while the player sees rows, so each `PayoutEvaluatorTest` case carries a three-line comment drawing the grid as it appears on screen.
- **Dependency versions live only in `gradle.properties`** (`jacksonVersion`, `lombokVersion`, `junitVersion`, `checkstyleVersion`) and are referenced from `build.gradle` as `"$xVersion"` — never hardcode a version inline in the dependency block.
- **Lombok is compile-time only** (`compileOnly` + `annotationProcessor`, mirrored for tests) and never appears on the runtime classpath. Used for boilerplate (`@Getter`, `@RequiredArgsConstructor`, `@UtilityClass`) on plain classes and enums, with default JavaBean naming; `record`s are left as plain records, never Lombok-ified.
- **Checkstyle is Google's style** (`config/checkstyle/checkstyle.xml`) with four documented local deviations (120-char lines, 4-space indent, no blanket Javadoc requirement, no forced `default` on exhaustive enum switches) — re-apply these four if the file is ever regenerated from a newer `google_checks.xml`. All rules run at `severity=warning` but `maxWarnings=0` in `build.gradle` makes any warning a build failure.
