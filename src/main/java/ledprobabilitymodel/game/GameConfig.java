package ledprobabilitymodel.game;

import java.util.List;

/** Immutable rules for a bounded finite game. Payouts index the number of good tiles claimed. */
public record GameConfig(
    int totalTiles, int goodTiles, int maxClaim, int revealPenalty, List<Integer> payouts) {
  public static final int MAX_TILES = 200;
  public static final int MAX_GOOD = 20;
  public static final int MAX_CLAIM = 50;
  public static final int MAX_PAYOUT = 1_000_000;

  public GameConfig {
    if (totalTiles < 1 || totalTiles > MAX_TILES) {
      throw new IllegalArgumentException("tiles must be between 1 and " + MAX_TILES);
    }
    if (goodTiles < 0 || goodTiles > Math.min(totalTiles, MAX_GOOD)) {
      throw new IllegalArgumentException("good must be between 0 and min(tiles, " + MAX_GOOD + ")");
    }
    if (maxClaim < 1 || maxClaim > Math.min(totalTiles, MAX_CLAIM)) {
      throw new IllegalArgumentException(
          "max-claim must be between 1 and min(tiles, " + MAX_CLAIM + ")");
    }
    if (revealPenalty < 0 || revealPenalty > MAX_PAYOUT) {
      throw new IllegalArgumentException("penalty must be between 0 and " + MAX_PAYOUT);
    }
    if (payouts == null
        || payouts.size() != goodTiles + 1
        || payouts.stream().anyMatch(p -> p == null || p < 0 || p > MAX_PAYOUT)) {
      throw new IllegalArgumentException(
          "payouts must contain good+1 integers between 0 and " + MAX_PAYOUT);
    }
    payouts = List.copyOf(payouts);
  }

  public static GameConfig defaults() {
    return new GameConfig(30, 3, 5, 1, List.of(0, 5, 10, 30));
  }

  /** A reachable state cannot contain more revealed good or bad tiles than exist. */
  public void validateState(GameState state) {
    if (state == null
        || state.revealed() < 0
        || state.revealed() > totalTiles
        || state.goodRevealed() < 0
        || state.goodRevealed() > goodTiles
        || state.goodRevealed() > state.revealed()
        || state.revealed() - state.goodRevealed() > totalTiles - goodTiles) {
      throw new IllegalArgumentException("unreachable game state: " + state);
    }
  }

  public boolean terminal(GameState state) {
    validateState(state);
    return state.goodRevealed() == goodTiles || state.revealed() == totalTiles;
  }

  public int terminalNet(GameState state) {
    if (!terminal(state)) throw new IllegalArgumentException("state is not terminal");
    return state.goodRevealed() == goodTiles ? -revealPenalty : 0;
  }

  public void validateClaim(GameState state, int claimSize) {
    validateState(state);
    if (claimSize < 1 || claimSize > Math.min(maxClaim, totalTiles - state.revealed())) {
      throw new IllegalArgumentException(
          "claim size must be between 1 and min(max-claim, remaining tiles)");
    }
  }

  public int net(int goodClaimed, int claimSize) {
    if (claimSize < 1
        || claimSize > maxClaim
        || goodClaimed < 0
        || goodClaimed > Math.min(goodTiles, claimSize)) {
      throw new IllegalArgumentException("invalid claimed good count or claim size");
    }
    return payouts.get(goodClaimed) - claimSize;
  }
}
