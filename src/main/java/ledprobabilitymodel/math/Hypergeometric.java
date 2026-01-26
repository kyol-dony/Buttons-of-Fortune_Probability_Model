package ledprobabilitymodel.math;

// Provides hypergeometric distribution functions
public class Hypergeometric {
    public static double pmf(int g, int N, int K, int k) { // hypergeometric probability mass function
        return Combinatorics.nCk(K, g)
                * Combinatorics.nCk(N - K, k - g)
                / Combinatorics.nCk(N, k);
    }
    public static int gMin(int N, int K, int k) { // ensures that smallest number for k isn't always 0
        return Math.max(0, k - (N - K));
    }
    public static int gMax(int K, int k) {
        return Math.min(K, k);
    }
}

// g = good LEDs inside claimed set
// N = total num LEDs
// K = num good LEDs in overall pool
// N - K = num bad LEDs in overall pool
// k = num LEDs the player claims
