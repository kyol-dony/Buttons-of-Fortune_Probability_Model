package ledprobabilitymodel.simulation;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Statistics derived from one histogram, with an unbiased sample-variance estimate. */
public final class SimulationStats {
  private final long trials;
  private final double mean;
  private final double standardError;
  private final Map<Integer, Long> outcomes;

  private SimulationStats(
      long trials, double mean, double standardError, Map<Integer, Long> outcomes) {
    this.trials = trials;
    this.mean = mean;
    this.standardError = standardError;
    this.outcomes = Collections.unmodifiableMap(new TreeMap<>(outcomes));
  }

  public static SimulationStats fromOutcomes(Map<Integer, Long> outcomes) {
    if (outcomes == null
        || outcomes.isEmpty()
        || outcomes.entrySet().stream()
            .anyMatch(e -> e.getKey() == null || e.getValue() == null || e.getValue() <= 0)) {
      throw new IllegalArgumentException("outcome counts must be positive");
    }
    long count = 0, total = 0;
    for (var entry : outcomes.entrySet()) {
      count = Math.addExact(count, entry.getValue());
      total = Math.addExact(total, Math.multiplyExact((long) entry.getKey(), entry.getValue()));
    }
    if (count < 2)
      throw new IllegalArgumentException(
          "at least two trials are needed to estimate standard error");
    double mean = (double) total / count;
    double squaredDeviations = 0;
    for (var entry : outcomes.entrySet()) {
      double deviation = entry.getKey() - mean;
      squaredDeviations += entry.getValue() * deviation * deviation;
    }
    double standardError = Math.sqrt(squaredDeviations / (count - 1) / count);
    return new SimulationStats(count, mean, standardError, outcomes);
  }

  public long trials() {
    return trials;
  }

  public double mean() {
    return mean;
  }

  public double standardError() {
    return standardError;
  }

  public Map<Integer, Long> outcomes() {
    return outcomes;
  }
}
