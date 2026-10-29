package ledprobabilitymodel.math;

/** Probabilities for uniformly sampled tiles without replacement. */
public final class Hypergeometric {
  private Hypergeometric() {}

  public static double pmf(int goodDrawn, int totalTiles, int goodTiles, int draws) {
    if (totalTiles < 0
        || totalTiles > 200
        || goodTiles < 0
        || goodTiles > totalTiles
        || draws < 0
        || draws > totalTiles) {
      throw new IllegalArgumentException("invalid hypergeometric population or draw count");
    }
    if (goodDrawn < gMin(totalTiles, goodTiles, draws) || goodDrawn > gMax(goodTiles, draws))
      return 0;
    return Combinatorics.nCk(goodTiles, goodDrawn)
        * Combinatorics.nCk(totalTiles - goodTiles, draws - goodDrawn)
        / Combinatorics.nCk(totalTiles, draws);
  }

  public static int gMin(int totalTiles, int goodTiles, int draws) {
    return Math.max(0, draws - (totalTiles - goodTiles));
  }

  public static int gMax(int goodTiles, int draws) {
    return Math.min(goodTiles, draws);
  }
}
