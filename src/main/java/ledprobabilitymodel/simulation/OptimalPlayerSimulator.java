package ledprobabilitymodel.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ledprobabilitymodel.game.Action;
import ledprobabilitymodel.game.Payout;

/**
 * Runs Monte Carlo simulations where the player follows the precomputed
 * optimal policy (revealing or claiming) to estimate the true average net. 
 */
public class OptimalPlayerSimulator {

    private final OptimalPolicy policy;
    private final Random rng;

    public OptimalPlayerSimulator(OptimalPolicy policy) {
        this(policy, new Random());
    }

    public OptimalPlayerSimulator(OptimalPolicy policy, Random rng) {
        this.policy = policy;
        this.rng = rng;
    }

    /**
     * Play one game following the optimal policy.
     *
     * @return trial result containing the claimed k and the resulting net
     */
    private TrialResult playOne() {
        boolean[] leds = new boolean[OptimalPolicy.N0];
        for (int i = 0; i < OptimalPolicy.K0; i++) {
            leds[i] = true;
        }

        // Shuffle LEDs
        for (int i = leds.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            boolean tmp = leds[i];
            leds[i] = leds[j];
            leds[j] = tmp;
        }

        int revealed = 0;
        int goodFound = 0;

        while (true) {
            Action action = policy.actionAt(revealed, goodFound);

            if (action == Action.CLAIM) {
                int k = policy.kAt(revealed, goodFound);
                if (k <= 0) {
                    return new TrialResult(0, 0);
                }

                int goodClaimed = 0;
                int remaining = leds.length - revealed;
                int actualK = Math.min(k, remaining);

                for (int i = 0; i < actualK; i++) {
                    if (leds[revealed + i]) goodClaimed++;
                }

                return new TrialResult(actualK, Payout.net(goodClaimed, actualK));
            }

            // Reveal one LED
            if (revealed >= leds.length) {
                return new TrialResult(0, 0);
            }

            boolean isGood = leds[revealed];
            revealed++;
            if (isGood) goodFound++;
        }
    }

    /**
     * Estimate the average net when following the optimal policy,
     * broken down by the k value chosen at the claim step.
     */
    public SimulationResult simulate(int trials) {
        Map<Integer, Long> totalsByK = new HashMap<>();
        Map<Integer, Integer> countsByK = new HashMap<>();
        for (int k = 1; k <= OptimalPolicy.K_MAX; k++) {
            totalsByK.put(k, 0L);
            countsByK.put(k, 0);
        }

        long totalNet = 0;

        for (int t = 0; t < trials; t++) {
            TrialResult r = playOne();

            totalsByK.merge(r.claimedK(), (long) r.net(), Long::sum);
            countsByK.merge(r.claimedK(), 1, Integer::sum);
            totalNet += r.net();
        }

        Map<Integer, Double> avgByK = new HashMap<>();
        for (int k = 1; k <= OptimalPolicy.K_MAX; k++) {
            long tot = totalsByK.getOrDefault(k, 0L);
            int count = countsByK.getOrDefault(k, 0);
            double avg = (count == 0) ? 0.0 : (double) tot / count;
            avgByK.put(k, avg);
        }

        double overallAvg = (double) totalNet / trials;
        return new SimulationResult(overallAvg, avgByK);
    }

    public record TrialResult(int claimedK, int net) {}

    public record SimulationResult(double overallAverageNet, Map<Integer, Double> averageNetByK) {}
}
