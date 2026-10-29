# Buttons of Fortune: Reproducible Analysis Toolkit
Date: 2026-10-28
Status: Proposed scope for user review
Repository: kyol-dony/ledprobdistribution
Baseline commit: 752f09622fe825230ba53dbc3657c00801270d21

## Purpose
Turn a school project's one-off calculation tool into a readable Java portfolio project showing probability modeling, simulation, dynamic programming, validation, and reproducible reporting. Preserve the original physical game's origin and document the assumptions behind each result. The user chose the analysis-toolkit direction.

## Approaches considered
1. Focused cleanup: retain the current demo and output, tidy code and add tests. Small scope, but reproducing findings would still require source edits.
2. Analysis toolkit, selected: keep Java and provide a configurable command-line study, shared model rules, comparable strategy results, and saved reports. Best fit for the user's Statistics and CIS portfolio.
3. Interactive teaching demo: add a playable visual interface. More interface work and less immediate focus on the statistical analysis. Defer.

## User-visible result
A reviewer clones the repository and runs a documented Maven command without hardware or a graphical desktop. The default command prints a compact comparison of immediate fixed-size claims and the adaptive reveal-or-claim policy, then writes a reproducible report bundle when an output directory is requested. One command runs correctness tests.

The report includes settings, seed, trials, exact expected net, simulated expected net with Monte Carlo standard error, profit probability, loss probability, net-payout distributions, and the optimal policy for reachable states. CSV files support reuse in R, spreadsheets, or other charting tools. A Markdown summary explains the question, assumptions, methods, results, and limitations. Optional existing Swing charts remain available explicitly, rather than launching during every run.

## Scope and defaults
- Keep Java 17 compatibility, Maven, and the existing XChart dependency. Add a test dependency only for the verification suite.
- Retain repository identity and history. Work on a separate branch. Keep the original study recognizable, with Buttons of Fortune as the case study and an analysis-toolkit subtitle.
- Keep the original rules: uniformly randomized board, sampling without replacement, revealed tiles unavailable for claiming, free reveals, terminal loss when all good tiles are revealed, and a terminal claim whose cost is the number of claimed tiles.
- Defaults: 30 playable tiles, 3 good tiles, claim cap 5, payout table [0, 5, 10, 30] by good tiles claimed, exhausted-good penalty 1.
- Configurable board size, good-tile count, claim cap, terminal penalty, trial count, seed, and integer payout table. Changing good-tile count requires a matching table through that count. Reject invalid or impractically large settings with a clear message rather than silently truncating them.
- Default trials 100,000 and seed 2026. Document how to request one million trials. Preserve the original defaults as a named case study, while making its simulation reproducible.
- The implementation chooses conservative, documented computational limits based on the state-table memory and numeric range. It must reject unsupported inputs before allocating tables, and test those limits. This is a bounded finite-game study, not an unrestricted combinatorial engine.
- Preserve deterministic tie-breaking: smallest claim size among equal claim values; claim rather than reveal on an exact tie. Document floating-point comparison behavior.
- The exact method uses floating-point evaluation of an analytical distribution. Describe it accurately, rather than claiming rational arithmetic.

## Structure
A shared validated game configuration owns rules and payouts. Small domain types express a reachable state and a policy decision. Math utilities calculate probabilities. The analysis layer owns fixed-claim distributions and the backward dynamic program. The simulation layer owns seeded random trials. Reporting owns console, CSV, Markdown, and optional plotting. The command-line entry point parses inputs and composes those pieces.

Use descriptive names such as totalTiles, goodTiles, claimSize, and goodRevealed in public interfaces. Mathematical notation belongs in explanatory documentation. Remove duplicated configuration constants, unused computations, commented experiments, redundant branches, and misspelled methods. Preserve existing callable entry points with thin delegates where straightforward; document any necessary interface changes because the user has authorized repurposing.

Keep comparisons distinct:
- A fixed claim starts from the original untouched board.
- An adaptive policy chooses whether to reveal or claim based on observed state.
- Averages grouped by the adaptive policy's chosen claim size are conditional results, not a fair direct ranking of fixed strategies.
- Expected value and probability of profit are separate statistics and may favor different strategies.

## Correctness fixes
- Validate impossible boards, unsupported payouts, invalid state coordinates, nonpositive trial counts, and invalid claim sizes.
- Exclude unreachable states from policy computation and reporting.
- Correct FavorAnalysis, which currently labels profit probability as EV and applies an incorrect threshold in its EV section.
- Aggregate simulation outcomes once per strategy run, deriving means, counts, and variance from the same sample.
- Use long counters/totals or wider accumulation where needed and check arithmetic bounds. Avoid integer overflow.
- Inject the random source, use a documented seed, and record it in outputs.
- Do not silently preserve defective edge behavior. Keep the default game's verified results stable.

## Baseline checks already run
Compiled the existing core classes with the available JDK, without chart dependencies.
- Fixed-claim expected nets for claim sizes 1..5:
  -0.5, -1.0, -1.4963054187192115, -1.9852216748768474, -2.4630541871921183.
- Initial adaptive policy expected net: 0.098522167488 approximately.
- Exact adaptive distribution:
  net -1: 0.7142857142857142
  net 2: 0.22167487684729073
  net 5: 0.061576354679802964
  net 25: 0.002463054187192118
- Distribution mass is 1 within floating-point tolerance. Its mean agrees with the policy value.
- Seed 2026, 100,000 existing adaptive trials: mean 0.0893.
- Confirmed defects: impossible board (2 total, 3 good) yields an empty distribution; zero trials yields NaN.
These checks establish useful defaults, not proof of every existing input.

## Verification and acceptance
1. Unit tests cover payouts, combinations/probabilities, valid domains, impossible states, terminal rules, and tie-breaking.
2. Exhaustive enumeration of small boards independently checks fixed-claim probabilities and adaptive policy values/distributions.
3. All supported analytical distributions are normalized and nonnegative within tolerance, and their means agree with analytical values.
4. Default case matches the baseline expected values and distribution within tolerance.
5. Seeded simulation repeats exactly for identical settings. A separate statistically justified check compares simulation to analytical results using its standard error.
6. A command-line smoke test exercises defaults, a custom configuration, help, invalid inputs, and headless report export.
7. Generated CSV and Markdown totals agree with the console and analytical distributions. Every exported artifact records its configuration.
8. Maven verification runs in CI on a clean checkout using Java 17. Formatting and compilation must pass.
9. Build output is removed from tracking and ignored. A clean checkout stays clean after building, testing, and report generation.

## Documentation and repository presentation
Write a README with the study's question, game rules, quick start, sample output, methods, interpretation, limitations, and repository structure. Add an explanation of the hypergeometric model and Bellman recurrence, plus the default findings. Include representative generated CSV/Markdown results as clearly labeled examples, regenerated through the documented command.

Keep the Arduino firmware as historical project context in a clearly named hardware area and preserve its PlatformIO path/build instructions if moved. Do not change hardware behavior, claim new hardware testing, or imply the Java model automatically reads hardware. Document the two special buttons versus the 30 playable tiles.

Do not choose a new software license for the user. Note the current licensing status accurately if needed.

## Out of scope
Web app, playable game UI, native app rewrite, cloud hosting, machine learning, Kelly betting, general-purpose reinforcement learning, new hardware functions, repository rename, release publication, and claims of user adoption or new performance benchmarks.

## Delivery
A reviewed implementation branch and draft pull request with a concise before/after explanation, validation results, sample reports, and any remaining limits. Default branch changes only through a later merge action.

## Implementation choices left to the builder
Exact package names, CLI flag spellings, report filenames, test framework version, numeric implementation details, conservative resource limits, and formatting configuration, provided they satisfy this scope and avoid unnecessary dependencies.
