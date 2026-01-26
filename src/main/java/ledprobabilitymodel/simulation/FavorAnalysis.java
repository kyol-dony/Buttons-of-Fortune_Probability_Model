package ledprobabilitymodel.simulation;

import java.util.Map;

// Analyze when the player is "in favor" of continuing to play - utilized for OptimalPolicy MDP
public class FavorAnalysis {

    /**
     * Calculate the probability of making a profit based on the net distribution.
     *
     * @param netDist map of net values to their probabilities
     * @return probability of making a profit (net > 0)
     */
    public static double profitProbability(Map<Integer, Double> netDist) {
        double p = 0.0;
        for (var e : netDist.entrySet()) {
            int net = e.getKey();
            double prob = e.getValue();
            if (net > 0) p += prob;
        }
        return p;
    }

    public static record BestResult(int bestK, double probability) {}

    /**
     * Find the k (number of claims) that maximizes profit probability for a given setup.
     *
     * @param N total LEDs
     * @param K good LEDs
     */
    public static BestResult bestProfit(int N, int K) {
        int upper = Math.min(ExactCalculator.K_MAX, N);
        int bestK = 1;
        double best = Double.NEGATIVE_INFINITY;

        for (int k = 1; k <= upper; k++) {
            Map<Integer, Double> netDist = ExactCalculator.netDistribution(N, K, k);
            double pProfit = profitProbability(netDist);
            if (pProfit > best) {
                best = pProfit;
                bestK = k;
            }
        }
        return new BestResult(bestK, best);
    }

    /**
     * Finds the earliest reveal-count t where the player can be "in favor"
     * assuming the player has revealed t LEDs and found g Good among them.
     *
     * We scan all reachable (t,g) states and report those that meet:
     *   - EV criterion: bestEV > 0
     *   - Profit criterion: bestProfitProb > 0.5
     *
     * You can change thresholds as you like.
     */
    public static void findSwitchPoints() {
        int N0 = OptimalPolicy.N0;
        int K0 = OptimalPolicy.K0;

        System.out.println("---- EV > 0 switch states ----");
        for (int t = 0; t <= N0; t++) {
            for (int g = 0; g <= K0; g++) {

                // Skip unreachable g (can't find more good than reveals)
                if (g > t) continue;

                int N = N0 - t;
                int K = K0 - g;
                if (K < 0) continue;

                // terminal: if K==0 auto-end; skip or treat separately
                if (K == 0 || N == 0) continue;

                BestResult br = bestProfit(N, K);
                if (br.probability() > 0.0) {
                    System.out.printf("State (t=%d,g=%d): N=%d,K=%d bestK=%d EV=%.6f%n",
                            t, g, N, K, br.bestK(), br.probability());
                }
            }
        }

        System.out.println("\n---- P(Net>0) > 0.5 switch states ----");
        for (int t = 0; t <= N0; t++) {
            for (int g = 0; g <= K0; g++) {

                if (g > t) continue;

                int N = N0 - t;
                int K = K0 - g;
                if (K < 0) continue;

                if (K == 0 || N == 0) continue;

                BestResult br = bestProfit(N, K);
                if (br.probability() > 0.5) {
                    System.out.printf("State (t=%d,g=%d): N=%d,K=%d bestK=%d P(profit)=%.6f%n",
                            t, g, N, K, br.bestK(), br.probability());
                }
            }
        }

    }

    
}
