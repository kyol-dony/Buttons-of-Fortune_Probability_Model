# Compatibility with the original project

The repository and its original game remain recognizable. The command-line runner now accepts options and runs without charts by default. Most previous Java entry points delegate to the shared validated model.

| Original API | Current behavior |
| --- | --- |
| `ExactCalculator.netDistribution(N,K,k)`, `expectedNet`, `expectedNetByK` | Retained for original payouts with 0..3 good tiles. Use GameConfig overloads for custom payouts. |
| `Payout.payout(good)`, `Payout.net(good,k)` | Retained for original rules. Invalid values now throw. |
| `OptimalPolicy()`, `valueAt(t,g)`, `actionAt(t,g)`, `kAt(t,g)`, `printPolicy()` | Retained. Invalid states now throw; printed policy excludes impossible states. The new constructor accepts GameConfig. |
| `OptimalPolicy.N0/K0/K_MAX/REVEAL_PENALTY` | Retained as aliases derived from GameConfig.defaults(). Configurable analyses use their instance rules. |
| `OutcomeDistribution.optimalNetDistribution(policy)` | Retained, honors the supplied policy's configuration. |
| `MonteCarlo.expectedNetMC`, `expectedNetByKMC`, `numNetOccured`, `simulatedNetTotalByKMC` | Retained with seed 2026 and validated trial counts. Legacy integer total conversions throw rather than overflow. |
| `MonteCarlo.simualtedTotalNet` | Deprecated misspelling retained as a delegate. Prefer simulatedTotalNet, which returns long. |
| `OptimalPlayerSimulator(policy, Random)` | Retained and honors the injected random source. |
| `OptimalPlayerSimulator.SimulationResult` | Retained. averageNetByK contains only observed claim sizes, including zero if a game terminates without a claim. These averages are conditional. |
| `Plotter` methods | Retained as optional graphical helpers. |
| `FavorAnalysis.findSwitchPoints()` | Corrected to report expected value and profit probability separately, with appropriate thresholds. |

New primary APIs are GameConfig, GameState, Simulator, SimulationStats, Study, and ReportWriter. Reports derive all simulation statistics for a strategy from one sample.

## Intentional changes

The default reveal penalty is 3 tokens, preserving the later master change in commit 616f7c7. The historical 1-token study is available through --penalty 1.

- Impossible configurations and states fail explicitly.
- Simulations require 2..1,000,000 trials.
- A zero-good board begins terminal, with net equal to the negative reveal penalty.
- Payout tables must cover every possible good count, with no silent default-zero values.
- The simulation uses urn sampling, changing the seeded random sequence from the original shuffle implementation.
- Result maps are immutable.
- Charts require an explicit --charts flag.
- Generated build files are no longer tracked.
- Unused calculations, commented experiments, duplicate branches, and misleading labels were removed.
- Firmware moved from src/game to hardware/trellis-m4 without behavior changes. platformio.ini points to its new location.

