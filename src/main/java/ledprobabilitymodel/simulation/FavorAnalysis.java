package ledprobabilitymodel.simulation;

import java.util.List;
import java.util.Map;
import ledprobabilitymodel.game.GameConfig;
import ledprobabilitymodel.game.GameState;

/** Keeps probability of profit separate from expected net payoff. */
public final class FavorAnalysis {
  private FavorAnalysis() {}

  public record BestResult(int bestK, double probability) {}

  public record ExpectedValueResult(int bestK, double expectedNet) {}

  public static double profitProbability(Map<Integer, Double> distribution) {
    return distribution.entrySet().stream()
        .filter(entry -> entry.getKey() > 0)
        .mapToDouble(Map.Entry::getValue)
        .sum();
  }

  public static BestResult bestProfit(GameConfig config, GameState state) {
    config.validateState(state);
    if (config.terminal(state)) return new BestResult(0, config.terminalNet(state) > 0 ? 1 : 0);
    int bestSize = 1;
    double bestProbability = -1;
    int remaining = config.totalTiles() - state.revealed();
    for (int size = 1; size <= Math.min(config.maxClaim(), remaining); size++) {
      double probability = profitProbability(ExactCalculator.netDistribution(config, state, size));
      if (probability > bestProbability) {
        bestProbability = probability;
        bestSize = size;
      }
    }
    return new BestResult(bestSize, bestProbability);
  }

  public static ExpectedValueResult bestExpectedNet(GameConfig config, GameState state) {
    config.validateState(state);
    if (config.terminal(state)) return new ExpectedValueResult(0, config.terminalNet(state));
    int bestSize = 1;
    double best = Double.NEGATIVE_INFINITY;
    for (int size = 1;
        size <= Math.min(config.maxClaim(), config.totalTiles() - state.revealed());
        size++) {
      double value = ExactCalculator.expectedNet(config, state, size);
      if (value > best) {
        best = value;
        bestSize = size;
      }
    }
    return new ExpectedValueResult(bestSize, best);
  }

  public static List<GameState> positiveExpectedValueStates(OptimalPolicy policy) {
    return policy.reachableStates().stream()
        .filter(state -> !policy.config().terminal(state))
        .filter(state -> bestExpectedNet(policy.config(), state).expectedNet() > 0)
        .toList();
  }

  public static BestResult bestProfit(int total, int good) {
    return bestProfit(ExactCalculator.originalRules(total, good), GameState.initial());
  }

  public static void findSwitchPoints() {
    var policy = new OptimalPolicy();
    System.out.println("Immediate claim states with expected net > 0:");
    for (var state : positiveExpectedValueStates(policy)) {
      var result = bestExpectedNet(policy.config(), state);
      System.out.printf(
          java.util.Locale.ROOT,
          "revealed=%d good_revealed=%d claim=%d EV=%.6f%n",
          state.revealed(),
          state.goodRevealed(),
          result.bestK(),
          result.expectedNet());
    }
    System.out.println("Immediate claim states with P(profit) > 0.5:");
    for (var state : policy.reachableStates()) {
      if (policy.config().terminal(state)) continue;
      var result = bestProfit(policy.config(), state);
      if (result.probability() > 0.5) {
        System.out.printf(
            java.util.Locale.ROOT,
            "revealed=%d good_revealed=%d claim=%d P(profit)=%.6f%n",
            state.revealed(),
            state.goodRevealed(),
            result.bestK(),
            result.probability());
      }
    }
  }
}
