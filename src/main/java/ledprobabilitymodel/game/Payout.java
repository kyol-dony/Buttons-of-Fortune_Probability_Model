package ledprobabilitymodel.game;

/** Original-game payout helpers retained for existing callers. */
public final class Payout {
  private Payout() {}

  public static int payout(int good) {
    if (good < 0 || good > 3) throw new IllegalArgumentException("good must be between 0 and 3");
    return GameConfig.defaults().payouts().get(good);
  }

  public static int net(int good, int claimSize) {
    return GameConfig.defaults().net(good, claimSize);
  }
}
