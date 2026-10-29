package ledprobabilitymodel.game;

/** Information available to a player before deciding whether to reveal or claim. */
public record GameState(int revealed, int goodRevealed) {
  public static GameState initial() {
    return new GameState(0, 0);
  }
}
