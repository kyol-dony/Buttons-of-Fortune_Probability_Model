package ledprobabilitymodel.simulation;

import java.util.HashMap;
import java.util.Map;

import ledprobabilitymodel.game.Action;

/**
 * Computes the exact probability distribution over final net outcomes
 * when following the optimal policy (including reveal penalties).
 *
 * The probability of a final payout is the weighted sum over all reveal
 * paths dictated by the policy, combined with the hypergeometric payout
 * distribution of the chosen claim.
 */
public class OutcomeDistribution {

    /**
     * Probability distribution of net payouts starting from the initial state
     * and following the optimal policy.
     */
    public static Map<Integer, Double> optimalNetDistribution(OptimalPolicy policy) {
        return distFromState(0, 0, policy, new HashMap<>());
    }

    // Recursive, memoized distribution from state (t, g)
    private static Map<Integer, Double> distFromState(
            int t, int g, OptimalPolicy policy, Map<String, Map<Integer, Double>> memo) {

        String key = t + ":" + g;
        if (memo.containsKey(key)) return memo.get(key);

        // Terminal: all goods revealed or no LEDs left to reveal/claim
        boolean allGoodsRevealed = (g == OptimalPolicy.K0);
        if (allGoodsRevealed || t == OptimalPolicy.N0) {
            int penalty = allGoodsRevealed ? OptimalPolicy.REVEAL_PENALTY : 0;
            Map<Integer, Double> terminal = Map.of(-penalty, 1.0);
            memo.put(key, terminal);
            return terminal;
        }

        Action action = policy.actionAt(t, g);

        if (action == Action.CLAIM) {
            int k = policy.kAt(t, g);
            int penalty = allGoodsRevealed ? OptimalPolicy.REVEAL_PENALTY : 0;

            if (k <= 0) {
                Map<Integer, Double> terminal = Map.of(-penalty, 1.0);
                memo.put(key, terminal);
                return terminal;
            }

            int N = OptimalPolicy.N0 - t; // unrevealed LEDs
            int K = OptimalPolicy.K0 - g; // good LEDs remaining

            Map<Integer, Double> base = ExactCalculator.netDistribution(N, K, k);
            Map<Integer, Double> shifted = new HashMap<>();

            for (var e : base.entrySet()) {
                int net = e.getKey() - penalty;
                shifted.merge(net, e.getValue(), Double::sum);
            }

            memo.put(key, shifted);
            return shifted;
        }

        // action == REVEAL
        int N = OptimalPolicy.N0 - t; // unrevealed LEDs remaining
        int K = OptimalPolicy.K0 - g; // good LEDs remaining

        double pGood = (double) K / N;
        double pBad = 1.0 - pGood;

        Map<Integer, Double> goodDist = distFromState(t + 1, g + 1, policy, memo);
        Map<Integer, Double> badDist = distFromState(t + 1, g, policy, memo);

        Map<Integer, Double> combined = new HashMap<>();
        accumulateWeighted(combined, goodDist, pGood);
        accumulateWeighted(combined, badDist, pBad);

        memo.put(key, combined);
        return combined;
    }

    private static void accumulateWeighted(Map<Integer, Double> into,
                                           Map<Integer, Double> source,
                                           double weight) {
        if (weight == 0.0) return;
        for (var e : source.entrySet()) {
            into.merge(e.getKey(), e.getValue() * weight, Double::sum);
        }
    }
}

