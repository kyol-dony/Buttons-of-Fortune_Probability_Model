package ledprobabilitymodel.simulation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import ledprobabilitymodel.game.GameConfig;
import ledprobabilitymodel.game.GameState;
import ledprobabilitymodel.math.Hypergeometric;

/** Analytical net-payout distributions, evaluated in double precision. */
public final class ExactCalculator {
  /** Legacy default cap; configurable calculations use GameConfig.maxClaim(). */
  public static final int K_MAX = GameConfig.defaults().maxClaim();

  private ExactCalculator() {}

  public static Map<Integer, Double> netDistribution(
      GameConfig config, GameState state, int claimSize) {
    config.validateClaim(state, claimSize);
    if (config.terminal(state)) return Map.of(config.terminalNet(state), 1.0);
    int remaining = config.totalTiles() - state.revealed();
    int goodRemaining = config.goodTiles() - state.goodRevealed();
    Map<Integer, Double> distribution = new TreeMap<>();
    for (int good = Hypergeometric.gMin(remaining, goodRemaining, claimSize);
        good <= Hypergeometric.gMax(goodRemaining, claimSize);
        good++) {
      double probability = Hypergeometric.pmf(good, remaining, goodRemaining, claimSize);
      distribution.merge(config.net(good, claimSize), probability, Double::sum);
    }
    return Collections.unmodifiableMap(distribution);
  }

  public static double expectedNet(GameConfig config, GameState state, int claimSize) {
    return netDistribution(config, state, claimSize).entrySet().stream()
        .mapToDouble(entry -> entry.getKey() * entry.getValue())
        .sum();
  }

  public static Map<Integer, Double> expectedNetByK(GameConfig config) {
    Map<Integer, Double> values = new LinkedHashMap<>();
    for (int size = 1; size <= config.maxClaim(); size++) {
      values.put(size, expectedNet(config, GameState.initial(), size));
    }
    return Collections.unmodifiableMap(values);
  }

  public static Map<Integer, Double> netDistribution(int total, int good, int claimSize) {
    return netDistribution(originalRules(total, good), GameState.initial(), claimSize);
  }

  public static double expectedNet(int total, int good, int claimSize) {
    return expectedNet(originalRules(total, good), GameState.initial(), claimSize);
  }

  public static Map<Integer, Double> expectedNetByK(int total, int good) {
    return expectedNetByK(originalRules(total, good));
  }

  static GameConfig originalRules(int total, int good) {
    var defaults = GameConfig.defaults();
    if (good < 0 || good > defaults.goodTiles()) {
      throw new IllegalArgumentException(
          "legacy calls support 0..3 good tiles; use GameConfig for custom payouts");
    }
    return new GameConfig(
        total,
        good,
        Math.min(defaults.maxClaim(), total),
        defaults.revealPenalty(),
        defaults.payouts().subList(0, good + 1));
  }
}
