package ledprobabilitymodel.math;

/** Double-precision binomial coefficients for the toolkit's bounded board sizes. */
public final class Combinatorics {
  private Combinatorics() {}

  public static double nCk(int total, int chosen) {
    if (total < 0 || total > 200)
      throw new IllegalArgumentException("total must be between 0 and 200");
    if (chosen < 0 || chosen > total) return 0;
    int count = Math.min(chosen, total - chosen);
    double result = 1;
    for (int i = 1; i <= count; i++) {
      result *= (double) (total - count + i) / i;
    }
    return result;
  }
}
