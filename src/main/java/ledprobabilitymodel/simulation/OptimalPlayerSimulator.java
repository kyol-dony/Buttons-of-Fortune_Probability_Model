package ledprobabilitymodel.simulation;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/** Compatibility adapter. Claim-size averages are conditional, not fixed-strategy estimates. */
public final class OptimalPlayerSimulator {
  private final OptimalPolicy policy;
  private final Random random;

  public OptimalPlayerSimulator(OptimalPolicy policy) {
    this(policy, new Random(2026));
  }

  public OptimalPlayerSimulator(OptimalPolicy policy, Random random) {
    this.policy = java.util.Objects.requireNonNull(policy, "policy");
    this.random = java.util.Objects.requireNonNull(random, "random");
  }

  public SimulationResult simulate(int trials) {
    var batch = Simulator.run(policy.config(), policy, 0, trials, random);
    return new SimulationResult(batch.stats().mean(), batch.averageNetByClaim());
  }

  public Map<Integer, Integer> netOccurrences(int trials) {
    var batch = Simulator.run(policy.config(), policy, 0, trials, random);
    Map<Integer, Integer> counts = new TreeMap<>();
    batch.stats().outcomes().forEach((net, count) -> counts.put(net, Math.toIntExact(count)));
    return counts;
  }

  public record TrialResult(int claimedK, int net) {}

  public record SimulationResult(double overallAverageNet, Map<Integer, Double> averageNetByK) {
    public SimulationResult {
      averageNetByK = Map.copyOf(averageNetByK);
    }
  }
}
