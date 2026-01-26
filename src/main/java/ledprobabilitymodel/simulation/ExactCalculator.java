package ledprobabilitymodel.simulation;

import ledprobabilitymodel.game.Payout;
import ledprobabilitymodel.math.Hypergeometric;

import java.util.*;

public class ExactCalculator {

    // Maximum number of LEDs the player is allowed to claim
    public static final int K_MAX = 5;

    public static Map<Integer, Double> netDistribution(int N, int K, int k) {
        if (k < 1 || k > K_MAX || k > N) {
            throw new IllegalArgumentException(
                    "k must satisfy 1 <= k <= min(" + K_MAX + ", N)"
            );
        }

        Map<Integer, Double> dist = new HashMap<>();

        int gMin = Hypergeometric.gMin(N, K, k); // safe check for k > N - K
        int gMax = Hypergeometric.gMax(K, k); // safe check for k > K

        for (int g = gMin; g <= gMax; g++) {
            double p = Hypergeometric.pmf(g, N, K, k); // probability of g good LEDs in k claimed
            int net = Payout.net(g, k); // net payout for g good LEDs claimed
            dist.put(net, dist.getOrDefault(net, 0.0) + p); // accumulate probability
        }

        return dist;
    }

    public static double expectedNet(int N, int K, int k) {
        Map<Integer, Double> dist = netDistribution(N, K, k); // get net distribution
        double ev = 0.0; 
        for (Map.Entry<Integer, Double> e : dist.entrySet()) { 
            ev += e.getKey() * e.getValue(); // EV = sum(net * probability)
        }
        return ev; 
    }

    /**
     * Exact expected net payout for all allowed k values (1..K_MAX).
     */
    public static Map<Integer, Double> expectedNetByK(int N, int K) { 
        Map<Integer, Double> evs = new LinkedHashMap<>();

        int upper = Math.min(K_MAX, N);
        for (int k = 1; k <= upper; k++) {
            evs.put(k, expectedNet(N, K, k)); // compute and store EV for each k
        }
        return evs;
    }

  
    // public static void printHistogram(Map<Integer, Double> dist) {
    //     var keys = new ArrayList<>(dist.keySet());
    //     Collections.sort(keys);

    //     for (int net : keys) {
    //         double p = dist.get(net);
    //         int bars = (int) Math.round(p * 100); // scale: 1 bar = 1%
    //         System.out.printf("%4d | %-100s %.6f%n", net, "#".repeat(bars), p);
    //     }
    // }

}
