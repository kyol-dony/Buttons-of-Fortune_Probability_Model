package ledprobabilitymodel.simulation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Original fixed-claim entry points, now reproducible with seed 2026. */
public final class MonteCarlo {
  public static final int K_MAX = ExactCalculator.K_MAX;
  private static final long LEGACY_SEED = 2026;

  private MonteCarlo() {}

  public static Map<Integer, Integer> numNetOccured(
      int total, int good, int claimSize, int trials) {
    var stats =
        Simulator.fixed(ExactCalculator.originalRules(total, good), claimSize, trials, LEGACY_SEED);
    Map<Integer, Integer> counts = new TreeMap<>();
    stats.outcomes().forEach((net, count) -> counts.put(net, Math.toIntExact(count)));
    return counts;
  }

  /**
   * @deprecated Use simulatedTotalNet; spelling retained for source compatibility.
   */
  @Deprecated
  public static double simualtedTotalNet(int trials, int total, int good, int claimSize) {
    return simulatedTotalNet(trials, total, good, claimSize);
  }

  public static long simulatedTotalNet(int trials, int total, int good, int claimSize) {
    var stats =
        Simulator.fixed(ExactCalculator.originalRules(total, good), claimSize, trials, LEGACY_SEED);
    return stats.outcomes().entrySet().stream()
        .mapToLong(e -> (long) e.getKey() * e.getValue())
        .sum();
  }

  public static double expectedNetMC(int trials, int total, int good, int claimSize) {
    return Simulator.fixed(
            ExactCalculator.originalRules(total, good), claimSize, trials, LEGACY_SEED)
        .mean();
  }

  public static Map<Integer, Double> expectedNetByKMC(int trials, int total, int good) {
    var config = ExactCalculator.originalRules(total, good);
    Map<Integer, Double> values = new LinkedHashMap<>();
    for (int size = 1; size <= config.maxClaim(); size++) {
      values.put(size, Simulator.fixed(config, size, trials, LEGACY_SEED).mean());
    }
    return values;
  }

  public static Map<Integer, Integer> simulatedNetTotalByKMC(int trials, int total, int good) {
    Map<Integer, Integer> totals = new LinkedHashMap<>();
    var config = ExactCalculator.originalRules(total, good);
    for (int size = 1; size <= config.maxClaim(); size++) {
      totals.put(size, Math.toIntExact(simulatedTotalNet(trials, total, good, size)));
    }
    return totals;
  }
}
