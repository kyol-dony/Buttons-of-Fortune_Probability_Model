package ledprobabilitymodel.simulation;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import ledprobabilitymodel.game.Action;
import ledprobabilitymodel.game.GameConfig;
import ledprobabilitymodel.game.GameState;

/** Seeded urn sampling without replacement; no physical board or shuffle is required. */
public final class Simulator {
  public static final int MAX_TRIALS = 1_000_000;

  private Simulator() {}

  public static SimulationStats fixed(GameConfig config, int claimSize, int trials, long seed) {
    config.validateClaim(GameState.initial(), claimSize);
    return run(config, null, claimSize, trials, new Random(seed)).stats();
  }

  public static SimulationStats adaptive(OptimalPolicy policy, int trials, long seed) {
    return run(policy.config(), policy, 0, trials, new Random(seed)).stats();
  }

  record Trial(int claimed, int net) {}

  record Batch(SimulationStats stats, Map<Integer, Double> averageNetByClaim) {}

  static Batch run(
      GameConfig config, OptimalPolicy policy, int fixedClaim, int trials, Random random) {
    if (trials < 2 || trials > MAX_TRIALS) {
      throw new IllegalArgumentException("trials must be between 2 and " + MAX_TRIALS);
    }
    java.util.Objects.requireNonNull(random, "random");
    Map<Integer, Long> outcomes = new TreeMap<>(),
        totals = new TreeMap<>(),
        counts = new TreeMap<>();
    for (int i = 0; i < trials; i++) {
      var trial = play(config, policy, fixedClaim, random);
      outcomes.merge(trial.net(), 1L, Long::sum);
      totals.merge(trial.claimed(), (long) trial.net(), Long::sum);
      counts.merge(trial.claimed(), 1L, Long::sum);
    }
    Map<Integer, Double> means = new TreeMap<>();
    counts.forEach((claim, count) -> means.put(claim, (double) totals.get(claim) / count));
    return new Batch(SimulationStats.fromOutcomes(outcomes), Map.copyOf(means));
  }

  private static Trial play(
      GameConfig config, OptimalPolicy policy, int fixedClaim, Random random) {
    int remaining = config.totalTiles(), goodRemaining = config.goodTiles();
    while (goodRemaining > 0 && remaining > 0) {
      var state =
          new GameState(config.totalTiles() - remaining, config.goodTiles() - goodRemaining);
      int claim = fixedClaim;
      if (policy != null) {
        var decision = policy.decisionAt(state);
        if (decision.action() == Action.REVEAL) {
          if (random.nextInt(remaining) < goodRemaining) goodRemaining--;
          remaining--;
          continue;
        }
        claim = decision.claimSize();
      }
      int goodClaimed = 0;
      for (int i = 0; i < claim; i++) {
        if (random.nextInt(remaining) < goodRemaining) {
          goodRemaining--;
          goodClaimed++;
        }
        remaining--;
      }
      return new Trial(claim, config.net(goodClaimed, claim));
    }
    var terminal =
        new GameState(config.totalTiles() - remaining, config.goodTiles() - goodRemaining);
    return new Trial(0, config.terminalNet(terminal));
  }
}
