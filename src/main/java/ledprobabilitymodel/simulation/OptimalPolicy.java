package ledprobabilitymodel.simulation;

import java.util.ArrayList;
import java.util.List;
import ledprobabilitymodel.game.Action;
import ledprobabilitymodel.game.Decision;
import ledprobabilitymodel.game.GameConfig;
import ledprobabilitymodel.game.GameState;

/** Backward induction maximizes expected net over reachable reveal-or-claim states. */
public final class OptimalPolicy {
  public static final int N0 = GameConfig.defaults().totalTiles();
  public static final int K0 = GameConfig.defaults().goodTiles();
  public static final int K_MAX = GameConfig.defaults().maxClaim();
  public static final int REVEAL_PENALTY = GameConfig.defaults().revealPenalty();

  private final GameConfig config;
  private final double[][] values;
  private final Decision[][] decisions;
  private final List<GameState> states;

  public OptimalPolicy() {
    this(GameConfig.defaults());
  }

  public OptimalPolicy(GameConfig config) {
    this.config = java.util.Objects.requireNonNull(config, "config");
    values = new double[config.totalTiles() + 1][config.goodTiles() + 1];
    decisions = new Decision[values.length][config.goodTiles() + 1];
    var reachable = new ArrayList<GameState>();
    for (int revealed = config.totalTiles(); revealed >= 0; revealed--) {
      int minGood = Math.max(0, revealed - (config.totalTiles() - config.goodTiles()));
      int maxGood = Math.min(revealed, config.goodTiles());
      for (int good = minGood; good <= maxGood; good++) {
        var state = new GameState(revealed, good);
        reachable.add(state);
        solve(state);
      }
    }
    reachable.sort(
        java.util.Comparator.comparingInt(GameState::revealed)
            .thenComparingInt(GameState::goodRevealed));
    states = List.copyOf(reachable);
  }

  private void solve(GameState state) {
    int revealed = state.revealed(), good = state.goodRevealed();
    if (config.terminal(state)) {
      values[revealed][good] = config.terminalNet(state);
      decisions[revealed][good] = new Decision(Action.CLAIM, 0);
      return;
    }
    int remaining = config.totalTiles() - revealed;
    int goodRemaining = config.goodTiles() - good;
    double best = Double.NEGATIVE_INFINITY;
    int bestClaim = 1;
    for (int size = 1; size <= Math.min(config.maxClaim(), remaining); size++) {
      double value = ExactCalculator.expectedNet(config, state, size);
      if (value > best) {
        best = value;
        bestClaim = size;
      }
    }
    double reveal = 0;
    if (goodRemaining > 0) {
      reveal += ((double) goodRemaining / remaining) * values[revealed + 1][good + 1];
    }
    if (remaining > goodRemaining) {
      reveal += ((double) (remaining - goodRemaining) / remaining) * values[revealed + 1][good];
    }
    // Strict comparison preserves smaller claims and chooses CLAIM on exact ties.
    decisions[revealed][good] =
        reveal > best ? new Decision(Action.REVEAL, 0) : new Decision(Action.CLAIM, bestClaim);
    values[revealed][good] = Math.max(best, reveal);
  }

  public GameConfig config() {
    return config;
  }

  public List<GameState> reachableStates() {
    return states;
  }

  public Decision decisionAt(GameState state) {
    config.validateState(state);
    return decisions[state.revealed()][state.goodRevealed()];
  }

  public double valueAt(GameState state) {
    config.validateState(state);
    return values[state.revealed()][state.goodRevealed()];
  }

  public double valueAt(int revealed, int goodRevealed) {
    return valueAt(new GameState(revealed, goodRevealed));
  }

  public Action actionAt(int revealed, int goodRevealed) {
    return decisionAt(new GameState(revealed, goodRevealed)).action();
  }

  public int kAt(int revealed, int goodRevealed) {
    var decision = decisionAt(new GameState(revealed, goodRevealed));
    return decision.action() == Action.REVEAL ? -1 : decision.claimSize();
  }

  public void printPolicy() {
    System.out.println("revealed,good_revealed,action,claim_size,expected_net");
    for (var state : states) {
      var decision = decisionAt(state);
      System.out.printf(
          java.util.Locale.ROOT,
          "%d,%d,%s,%d,%.12f%n",
          state.revealed(),
          state.goodRevealed(),
          config.terminal(state) ? "END" : decision.action(),
          decision.claimSize(),
          valueAt(state));
    }
  }
}
