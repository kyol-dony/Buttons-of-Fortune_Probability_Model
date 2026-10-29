package ledprobabilitymodel.reporting;

import java.util.*;
import ledprobabilitymodel.game.GameConfig;
import ledprobabilitymodel.game.GameState;
import ledprobabilitymodel.simulation.*;

/** One study holds analytical and simulated results for comparable strategies. */
public record Study(
    GameConfig config, int trials, long seed, OptimalPolicy policy, List<Result> results) {
  public static final long MAX_DRAWS = 100_000_000L;

  public Study {
    results = List.copyOf(results);
  }

  public record Result(
      String strategy, int claimSize, Map<Integer, Double> exact, SimulationStats simulated) {
    public Result {
      exact = Collections.unmodifiableMap(new TreeMap<>(exact));
    }

    public double exactMean() {
      return exact.entrySet().stream().mapToDouble(e -> e.getKey() * e.getValue()).sum();
    }

    public double exactProbability(boolean profit) {
      return exact.entrySet().stream()
          .filter(e -> profit ? e.getKey() > 0 : e.getKey() < 0)
          .mapToDouble(Map.Entry::getValue)
          .sum();
    }

    public double simulatedProbability(boolean profit) {
      long count =
          simulated.outcomes().entrySet().stream()
              .filter(e -> profit ? e.getKey() > 0 : e.getKey() < 0)
              .mapToLong(Map.Entry::getValue)
              .sum();
      return (double) count / simulated.trials();
    }
  }

  public static Study run(GameConfig config, int trials, long seed) {
    if (trials < 2 || trials > Simulator.MAX_TRIALS) {
      throw new IllegalArgumentException("trials must be between 2 and " + Simulator.MAX_TRIALS);
    }
    long fixedDraws = (long) config.maxClaim() * (config.maxClaim() + 1) / 2;
    if ((fixedDraws + config.totalTiles()) * trials > MAX_DRAWS) {
      throw new IllegalArgumentException(
          "study exceeds 100 million worst-case tile draws; reduce trials, tiles, or max-claim");
    }
    var policy = new OptimalPolicy(config);
    var results = new ArrayList<Result>();
    for (int size = 1; size <= config.maxClaim(); size++) {
      results.add(
          new Result(
              "fixed-" + size,
              size,
              ExactCalculator.netDistribution(config, GameState.initial(), size),
              Simulator.fixed(config, size, trials, seed)));
    }
    results.add(
        new Result(
            "adaptive",
            0,
            OutcomeDistribution.optimalNetDistribution(policy),
            Simulator.adaptive(policy, trials, seed)));
    return new Study(config, trials, seed, policy, results);
  }
}
