package ledprobabilitymodel.simulation;

import ledprobabilitymodel.game.Payout;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

// Provides Monte Carlo simulations for net payout distributions and expected values with pure random claiming strategy
public class MonteCarlo {

    private static final Random rng = new Random();

    public static final int K_MAX = 5;

    // Single trial net payout simulation
    private static int oneTrialNet(int N, int K, int k) {
        boolean[] leds = new boolean[N];

        for (int i = 0; i < K; i++) leds[i] = true;

        for (int i = N - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            boolean tmp = leds[i];
            leds[i] = leds[j];
            leds[j] = tmp;
        }
        int good = 0;
        for (int i = 0; i < k; i++) {
            if (leds[i]) good++;
        }
        return Payout.net(good, k);
    }

    // Number of times each net payout occurred in trials
    public static Map<Integer, Integer> numNetOccured(int N, int K, int k, int trials) {
        int net = 0;
        Map<Integer, Integer> netCount = new LinkedHashMap<>();
        for (int i = 0; i < trials; i++) {
            net = oneTrialNet(N, K, k);
            if (net < 0) {
                netCount.put(net, netCount.getOrDefault(net, 0) + 1);
            } else {
                netCount.put(net, netCount.getOrDefault(net, 0) + 1);
            }
        }
        return netCount;
    }
    

    /**
     * Simulated total net for a specific k value.
     */
    public static double simualtedTotalNet(int trials, int N, int K, int k) {
        long total = 0;
        for (int t = 0; t < trials; t++) {
            total += oneTrialNet(N, K, k);
        }
        return total;
    }

    /**
     * Monte Carlo EV for a specific k value.
     */
    public static double expectedNetMC(int trials, int N, int K, int k) { 
        if (k < 1 || k > K_MAX || k > N) {
            throw new IllegalArgumentException(
                    "k must satisfy 1 <= k <= min(" + K_MAX + ", N)"
            );
        }
        long total = 0;
        for (int t = 0; t < trials; t++) {
            total += oneTrialNet(N, K, k);
        }
        return (double) total / trials;
    }

    /**
     * Monte Carlo EV for all allowed k values (1..K_MAX).
     */
    public static Map<Integer, Double> expectedNetByKMC(int trials, int N, int K) {
        Map<Integer, Double> evs = new LinkedHashMap<>();

        int upper = Math.min(K_MAX, N);
        for (int k = 1; k <= upper; k++) {
            evs.put(k, expectedNetMC(trials, N, K, k));
        }
        return evs;
    }

    /**
     * Simulated total net for all allowed k values (1..K_MAX).
     */
    public static Map<Integer, Integer> simulatedNetTotalByKMC(int trials, int N, int K) { 
        Map<Integer, Integer> netMap = new LinkedHashMap<>();
        int upper = Math.min(K_MAX, N);

        for (int k = 1; k <= upper; k++) {
            int totalNet = 0;
            for (int t = 0; t < trials; t++) {
                totalNet += oneTrialNet(N, K, k);
                
            }
            netMap.put(k, totalNet);
        }
        return netMap;
    }
    
}
