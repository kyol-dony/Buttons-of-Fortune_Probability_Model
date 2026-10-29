# Buttons of Fortune
### A reproducible probability analysis toolkit

A Java case study comparing immediate claims with an adaptive reveal-or-claim strategy in a physical LED guessing game. It combines analytical probabilities, dynamic programming, and seeded Monte Carlo simulation.

The project began as a tool for summarizing findings for a school project. This version makes the assumptions, calculations, and results reproducible for readers reviewing the code.

## What the study finds

Under the original rules, the best immediate fixed claim has an expected net of **-0.50 tokens**. The expected-net-maximizing adaptive policy has an expected net of approximately **+0.098522 tokens**, while losing one token in **71.43%** of games.

A positive mean and a high probability of profit are different properties. The adaptive policy's occasional larger payouts offset frequent losses. These conclusions depend on free reveals and the game's exact payout and termination rules.

## Quick start

Requires **JDK 17 or later** and **Maven 3.6.3 or later**. No hardware, account, or graphical desktop is required.

```sh
mvn verify
mvn -q exec:java
```

The default study uses 100,000 trials per strategy and seed 2026:

```text
Strategy     Exact mean      MC mean      MC SE Exact P(profit)  Exact P(loss)
fixed-1       -0.500000    -0.494950   0.004765        0.100000       0.900000
fixed-2       -1.000000    -0.998750   0.006593        0.193103       0.806897
fixed-3       -1.496305    -1.501150   0.008028        0.279557       0.720443
fixed-4       -1.985222    -1.980800   0.009411        0.359606       0.640394
fixed-5       -2.463054    -2.466550   0.010518        0.064039       0.566502
adaptive       0.098522     0.092020   0.006767        0.285714       0.714286
```

The five-tile claim has a substantial probability of breaking even, so its profit and loss probabilities sum to less than one. MC SE is Monte Carlo standard error of the estimated mean, not the spread of one game's outcome.

## Game rules

- The original board has 32 buttons. Two are controls, leaving 30 playable tiles.
- Exactly three playable tiles are good, uniformly placed without replacement.
- A player may reveal a tile for free. A revealed tile is unavailable for a later claim.
- Revealing all good tiles ends the game with a loss of one token.
- Alternatively, the player claims one to five hidden tiles and ends the game.
- A claim costs one token per claimed tile.
- Gross payouts for zero, one, two, or three good tiles claimed are 0, 5, 10, and 30 tokens.

The Java model captures these abstract rules. It does not connect to or read the physical board.

## Run a study

```sh
# Help
mvn -q exec:java -Dexec.args="--help"

# Export the original case study to a new directory
mvn -q exec:java -Dexec.args="--output reports/original"

# Increase precision
mvn -q exec:java -Dexec.args="--trials 1000000 --seed 2026"

# A different finite game, with an explicit payout table
mvn -q exec:java -Dexec.args="--tiles 8 --good 2 --max-claim 3 --penalty 4 --payouts 0,4,19 --trials 100000 --seed 42 --output reports/custom"

# Optional desktop charts
mvn -q exec:java -Dexec.args="--charts"
```

Run `mvn compile` first after checking out or changing source if you skip `mvn verify`. Maven's exec goal does not compile automatically.

For an output path containing spaces, pass quotes inside the Maven argument string:

```sh
mvn -q exec:java -Dexec.args='--output "reports/my study"'
```

### Options and limits

| Option | Default | Accepted values |
| --- | --- | --- |
| `--tiles` | 30 | 1..200 |
| `--good` | 3 | 0..min(tiles, 20) |
| `--max-claim` | 5 | 1..min(tiles, 50) |
| `--penalty` | 1 | 0..1,000,000 |
| `--payouts` | 0,5,10,30 | Exactly good+1 nonnegative integers, each at most 1,000,000 |
| `--trials` | 100000 | 2..1,000,000 per strategy |
| `--seed` | 2026 | Signed 64-bit integer |
| `--output` | none | Report directory |
| `--charts` | off | Requires a graphical desktop |
| `--help`, `-h` | off | Print usage |

Changing the number of good tiles requires a matching payout table. Settings are never silently clamped. A zero-good board starts terminal and pays the negative terminal penalty for every strategy.

The state table has at most 201 x 21 cells. The board-size limit keeps direct double-precision binomial coefficients finite. The CLI also caps the conservative worst-case draw budget at 100 million: trials x (maxClaim x (maxClaim+1) / 2 + totalTiles). Reduce trials for larger custom games. At least two trials are required to estimate sample standard error.

## Reports

A report bundle contains:

| File | Contents |
| --- | --- |
| `summary.md` | Settings, methods, comparison table, interpretation, and limits |
| `strategies.csv` | Exact and simulated means, MC SE, profit/loss probabilities |
| `outcomes.csv` | Exact probabilities and sampled counts for every net outcome |
| `policy.csv` | Optimal decision and expected net at every valid state |

Every CSV row records the rules, seed, and trial count. Payout tables use `|` between values inside CSV cells. Decimal points stay consistent across system locales.

Existing report filenames are refused before writing. Unrelated files in the output directory remain untouched. An unexpected disk failure may leave a partially written bundle. Choose a new directory for each run.

[Checked-in default results](examples/default/summary.md) were generated with:

```sh
mvn -q exec:java -Dexec.args="--trials 100000 --seed 2026 --output examples/default"
```

To regenerate the checked-in example, write to a fresh directory and compare/copy the four reviewed outputs. The exporter intentionally refuses to overwrite the existing example.

## Methods and verification

[Methods](docs/methods.md) explains the probability model, Bellman recurrence, terminal rules, ties, and uncertainty.

The test suite checks:
- Default findings against the original model.
- Hypergeometric probabilities against exhaustive small-board counts.
- Adaptive values and distributions against an independent enumerated-board oracle.
- Input domains, terminal states, seed repeatability, and sample standard errors.
- CLI errors, headless runs, CSV consistency, locale handling, and output preservation.

`mvn verify` runs the tests. CI builds and tests on JDK 17 and JDK 25, checks formatting, and confirms a normal study leaves the checkout clean.

Java source uses google-java-format 1.37.0. Formatting needs a separate JDK 25 installation; building and running the toolkit still supports JDK 17.

```sh
# With Java 25 active
sh scripts/format.sh --check
sh scripts/format.sh --write
```

The formatter is checksum-verified and stored outside tracked source. It is a development tool, not a runtime dependency.

## Repository layout

```text
src/main/java/ledprobabilitymodel/
  game/         validated rules, state, and decisions
  math/         combinatorics and hypergeometric probabilities
  simulation/   analytical solver, simulation, and optional plots
  cli/          strict option parsing
  reporting/    study comparisons and report exports
src/test/java/  independent correctness and integration checks
hardware/      original Trellis M4 firmware and setup notes
examples/      reproducible default study
docs/          methods, compatibility notes, and approved design
```

The [compatibility notes](docs/compatibility.md) cover retained original Java calls and deliberate behavior changes. The [hardware notes](hardware/README.md) preserve the original board context.

## Limitations and licensing

This is a bounded, risk-neutral expected-net model. Reveals have no cost, elapsed time is ignored, tile locations are exchangeable, and the policy knows the rules. The toolkit does not model human behavior, betting bankrolls, machine learning, or real-world profitability.

No new software license has been selected for this repository. Public source visibility alone does not grant general reuse permission. Third-party dependencies retain their own licenses.

