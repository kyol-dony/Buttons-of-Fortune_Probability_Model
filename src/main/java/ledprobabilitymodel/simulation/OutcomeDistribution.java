package ledprobabilitymodel.simulation;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import ledprobabilitymodel.game.Action;

/** Propagates initial probability mass through the solved policy's reachable states. */
public final class OutcomeDistribution {
  private OutcomeDistribution() {}

  public static Map<Integer, Double> optimalNetDistribution(OptimalPolicy policy) {
    var config = policy.config();
    double[][] mass = new double[config.totalTiles() + 1][config.goodTiles() + 1];
    mass[0][0] = 1;
    Map<Integer, Double> outcomes = new TreeMap<>();
    for (var state : policy.reachableStates()) {
      double probability = mass[state.revealed()][state.goodRevealed()];
      if (probability == 0) continue;
      if (config.terminal(state)) {
        outcomes.merge(config.terminalNet(state), probability, Double::sum);
        continue;
      }
      var decision = policy.decisionAt(state);
      if (decision.action() == Action.CLAIM) {
        for (var entry :
            ExactCalculator.netDistribution(config, state, decision.claimSize()).entrySet()) {
          outcomes.merge(entry.getKey(), probability * entry.getValue(), Double::sum);
        }
      } else {
        int remaining = config.totalTiles() - state.revealed();
        int goodRemaining = config.goodTiles() - state.goodRevealed();
        if (goodRemaining > 0) {
          mass[state.revealed() + 1][state.goodRevealed() + 1] +=
              probability * goodRemaining / remaining;
        }
        if (remaining > goodRemaining) {
          mass[state.revealed() + 1][state.goodRevealed()] +=
              probability * (remaining - goodRemaining) / remaining;
        }
      }
    }
    return Collections.unmodifiableMap(outcomes);
  }
}
