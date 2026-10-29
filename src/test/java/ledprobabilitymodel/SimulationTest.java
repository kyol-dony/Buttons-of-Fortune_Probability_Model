package ledprobabilitymodel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import ledprobabilitymodel.game.*;
import ledprobabilitymodel.simulation.*;
import org.junit.jupiter.api.Test;

class SimulationTest {
  @Test
  void fixedSeedRepeatsEveryOutcomeCount() {
    var c = GameConfig.defaults();
    assertEquals(
        Simulator.fixed(c, 3, 20_000, 2026).outcomes(),
        Simulator.fixed(c, 3, 20_000, 2026).outcomes());
    var p = new OptimalPolicy(c);
    assertEquals(
        Simulator.adaptive(p, 20_000, 2026).outcomes(),
        Simulator.adaptive(p, 20_000, 2026).outcomes());
  }

  @Test
  void summaryComesFromSameHistogramAndUsesSampleStandardError() {
    var stats = SimulationStats.fromOutcomes(Map.of(-1, 3L, 3, 1L));
    assertEquals(4, stats.trials());
    assertEquals(0, stats.mean(), 1e-12);
    assertEquals(1, stats.standardError(), 1e-12);
    assertThrows(UnsupportedOperationException.class, () -> stats.outcomes().put(0, 1L));
    assertThrows(IllegalArgumentException.class, () -> SimulationStats.fromOutcomes(Map.of(0, 1L)));
    assertThrows(
        IllegalArgumentException.class, () -> SimulationStats.fromOutcomes(Map.of(0, -1L)));
    var big = SimulationStats.fromOutcomes(Map.of(1_000_000, 1_000_000L));
    assertEquals(1_000_000, big.mean());
    assertEquals(0, big.standardError());
  }

  @Test
  void invalidTrialAndClaimCountsFailClearly() {
    for (int count : new int[] {-1, 0, 1, 1_000_001}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> Simulator.fixed(GameConfig.defaults(), 1, count, 0));
    }
    assertThrows(
        IllegalArgumentException.class, () -> Simulator.fixed(GameConfig.defaults(), 0, 10, 0));
    assertThrows(IllegalArgumentException.class, () -> MonteCarlo.expectedNetMC(0, 30, 3, 1));
  }

  @Test
  void seededSimulationsAgreeWithAnalyticalValuesWithinSixStandardErrors() {
    var c = GameConfig.defaults();
    for (int size = 1; size <= 5; size++) {
      var stats = Simulator.fixed(c, size, 100_000, 2026);
      assertEquals(100_000, stats.outcomes().values().stream().mapToLong(Long::longValue).sum());
      assertEquals(
          ExactCalculator.expectedNet(c, GameState.initial(), size),
          stats.mean(),
          6 * stats.standardError());
    }
    var p = new OptimalPolicy(c);
    var stats = Simulator.adaptive(p, 100_000, 2026);
    assertEquals(p.valueAt(GameState.initial()), stats.mean(), 6 * stats.standardError());
  }

  @Test
  void deterministicGamesAndCustomRulesMatchExactOutputs() {
    var allGood = new GameConfig(3, 3, 3, 1, List.of(0, 5, 10, 30));
    assertEquals(
        Map.of(27, 100L), Simulator.adaptive(new OptimalPolicy(allGood), 100, 1).outcomes());
    var zeroGood = new GameConfig(4, 0, 3, 7, List.of(0));
    assertEquals(
        Map.of(-7, 100L), Simulator.adaptive(new OptimalPolicy(zeroGood), 100, 1).outcomes());
    assertEquals(Map.of(-7, 100L), Simulator.fixed(zeroGood, 2, 100, 1).outcomes());
    var custom = new GameConfig(8, 2, 3, 4, List.of(0, 4, 19));
    var p = new OptimalPolicy(custom);
    var stats = Simulator.adaptive(p, 100_000, 42);
    assertEquals(p.valueAt(GameState.initial()), stats.mean(), 6 * stats.standardError());
  }

  @Test
  void legacySimulatorHonorsItsInjectedRandomSource() {
    var p = new OptimalPolicy(new GameConfig(4, 1, 2, 3, List.of(0, 9)));
    var a = new OptimalPlayerSimulator(p, new Random(9)).simulate(1_000);
    var b = new OptimalPlayerSimulator(p, new Random(9)).simulate(1_000);
    assertEquals(a, b);
    assertTrue(Double.isFinite(a.overallAverageNet()));
  }
}
