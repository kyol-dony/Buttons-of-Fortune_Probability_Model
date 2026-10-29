# Probability Analysis Toolkit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Deliver a reproducible, tested Java analysis toolkit for the original Buttons of Fortune case study.
**Architecture:** Validated immutable rules and state feed analytical calculations, a backward policy solver, and seeded simulation. A thin CLI composes these and exports consistent reports.
**Tech Stack:** Java 17, Maven, XChart 3.8.8, JUnit Jupiter 5.10.1.
**Spec:** docs/superpowers/specs/2026-10-28-analysis-toolkit-design.md

## Global Constraints
- Java 17 compatibility, Maven, existing XChart dependency; no new runtime dependency.
- Default board 30, good 3, claim cap 5, payouts [0,5,10,30], terminal penalty 1.
- Default 100,000 trials, seed 2026. CLI headless unless charts explicitly requested.
- Preserve free reveals, sampling without replacement, and original terminal rules.
- Strict comparisons select smaller claim sizes and claim on exact reveal ties.
- Separate branch and draft PR; preserve hardware behavior and upstream history.

## Review Focus
- All-good and zero-good boards terminate consistently; pin in core and policy tests.
- Extreme or inconsistent configuration rejects before allocation; pin upper bounds and payout coverage.
- Output paths containing spaces and pre-existing files preserve unrelated data; pin report smoke tests.
- Nondefault locale cannot corrupt CSV decimals; pin reporting under German locale.
- Unknown/repeated/missing CLI flags fail clearly; pin parser tests and subprocess exit behavior.

## Task 1: Validated game model and probability math
**Files:** game/GameConfig.java, game/GameState.java, game/Decision.java; math/Hypergeometric.java, math/Combinatorics.java, game/Payout.java; pom.xml; src/test/java/ledprobabilitymodel/ModelTest.java.
**Interfaces:** GameConfig(int totalTiles,int goodTiles,int maxClaim,int revealPenalty,List<Integer> payouts), defaults(), validateState(GameState), net(int good,int claimSize); GameState(int revealed,int goodRevealed). Decision(Action action,int claimSize). Hypergeometric.pmf(int good,int total,int populationGood,int draws).
- [x] Add JUnit and stable Maven compiler/surefire versions.
- [x] Write tests for default payouts, invalid boards, bounded configuration, payout immutability, reachable states, PMF support/normalization and exact small-board enumeration.
- [x] Run tests RED, implement model/math and compatible default delegates, run GREEN.
- [x] Commit validated model and proof.

## Task 2: Analytical distributions and policy
**Files:** simulation/ExactCalculator.java, OptimalPolicy.java, OutcomeDistribution.java, FavorAnalysis.java; tests/AnalysisTest.java.
**Interfaces:** ExactCalculator.netDistribution(GameConfig,GameState,int) and expectedNet overloads; OptimalPolicy(GameConfig), config(), decisionAt(GameState), valueAt(GameState), reachableStates(); OutcomeDistribution.optimalNetDistribution(OptimalPolicy).
- [x] Write RED tests for default EVs/distribution, invalid state, terminal behavior, exact ties, exhaustive small-board adaptive values/distributions, and correct profit-vs-EV reporting.
- [x] Implement shared validated configuration, reachable-only backward recursion and forward distribution propagation.
- [x] Run GREEN; commit analytical engine.

## Task 3: Reproducible simulation
**Files:** simulation/SimulationStats.java, Simulator.java; MonteCarlo.java and OptimalPlayerSimulator.java compatibility wrappers; tests/SimulationTest.java.
**Interfaces:** Simulator.fixed(GameConfig,int claimSize,int trials,long seed), adaptive(OptimalPolicy,int trials,long seed) -> SimulationStats(long trials,double mean,double standardError,Map<Integer,Long> outcomes).
- [x] Write RED tests for fixed-seed repeatability, histogram-derived mean/SE, invalid trial counts, deterministic boards and exact-vs-simulated agreement within six standard errors.
- [x] Implement one shared without-replacement draw engine, single-pass outcome aggregation and legacy delegates.
- [x] Run GREEN; commit seeded simulation.

## Task 4: CLI and reporting
**Files:** Main.java, cli/Options.java, reporting/Study.java, ReportWriter.java, simulation/Plotter.java; tests/CliReportTest.java.
**Interfaces:** Options.parse(String[]); Study.run(GameConfig,int trials,long seed); ReportWriter.console(Study), write(Study,Path); Main.run(String[],PrintStream,PrintStream) -> int.
- [x] Write RED tests for help/errors/defaults/custom flags, headless execution, locale-safe report bundle, settings in every CSV, output path spaces, no unrelated overwrite, sample totals.
- [x] Implement --tiles --good --max-claim --penalty --payouts --trials --seed --output --charts --help with explicit duplicate/unknown flag rejection.
- [x] Export summary.md, strategies.csv, outcomes.csv and policy.csv with complete configuration metadata and correct denominators.
- [x] Run GREEN and headless smoke; commit application/reporting.

## Task 5: Presentation, examples and clean verification
**Files:** README.md, docs/methods.md, docs/compatibility.md, hardware/, platformio.ini, .gitignore, .github/workflows/verify.yml, examples/default/*.
- [x] Move unchanged firmware to hardware/trellis-m4, update PlatformIO source path, document no hardware retesting.
- [x] Ignore build/reports and remove tracked upstream target tree.
- [x] Document methods, interpretation, uncertainty, bounded inputs, defaults, examples, compatibility and licensing status.
- [x] Generate examples with documented command; add CI Maven verification and whitespace verification.
- [x] Run Maven verify, CLI subprocess errors, alternate configs and clean-tree checks.
- [ ] Commit; dispatch final independent review; fix material findings with regression proof.
- [ ] Publish changes to existing feature branch using native GitHub APIs with expected-head guard and open draft PR.

