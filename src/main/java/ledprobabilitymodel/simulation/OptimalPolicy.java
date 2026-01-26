package ledprobabilitymodel.simulation;

import ledprobabilitymodel.game.Action;

/*
 * The OptimalPolicy class computes the best possible strategy for the player
 * by solving the game as a Markov Decision Process
 * and storing the optimal action and claim size at every possible game state.
 */
public class OptimalPolicy {

    // Problem parameters
    public static final int N0 = 30;
    public static final int K0 = 3;
    
    // Maximum allowable k value
    public static final int K_MAX = 5;

    // Value table for expected net payouts
    private final double[][] V = new double[N0 + 1][K0 + 1];

    // Policy tables for actions and best k values
    private final Action[][] bestAction = new Action[N0 + 1][K0 + 1];
    private final int[][] bestK = new int[N0 + 1][K0 + 1];

    public OptimalPolicy() {
        compute();
}
    // Dynamic programming computation of optimal policy
    private void compute() {
        // Fill from t = N0 down to 0 because REVEAL moves you to t+1
        for (int t = N0; t >= 0; t--) {
            for (int g = K0; g >= 0; g--) {

                // Terminal states:
                // - all good found => auto-end with net 0 (per the rules)
                // - no LEDs left to reveal/claim => net 0
                if (g == K0 || t == N0) {
                    V[t][g] = 0.0;
                    bestAction[t][g] = Action.CLAIM;
                    bestK[t][g] = 0;
                    continue;
                }

                int N = N0 - t; // unrevealed LEDs remaining
                int K = K0 - g; // good LEDs remaining

                // ---- Option 1: CLAIM now ----
                int upperK = Math.min(K_MAX, N);

                double bestClaimEV = Double.NEGATIVE_INFINITY;
                int argBestK = 1;

                for (int k = 1; k <= upperK; k++) {
                    // Exact expected net if we claim k from (N,K)
                    double ev = ExactCalculator.expectedNet(N, K, k);
                    if (ev > bestClaimEV) {
                        bestClaimEV = ev;
                        argBestK = k;
                    }
                }

                // ---- Option 2: REVEAL one more LED ----
                // Next revealed is good with probability K/N
                double pGood = (double) K / N;
                double pBad = 1.0 - pGood;

                // Expected value if we reveal, then follow the optimal policy
                double revealEV =
                        pGood * V[t + 1][g + 1] +
                        pBad  * V[t + 1][g];

                // ---- Choose best option ----
                if (revealEV > bestClaimEV) {
                    V[t][g] = revealEV;
                    bestAction[t][g] = Action.REVEAL;
                    bestK[t][g] = -1;
                } else {
                    V[t][g] = bestClaimEV;
                    bestAction[t][g] = Action.CLAIM;
                    bestK[t][g] = argBestK;
                }
            }
        }
    }
    // Accessors
    public double valueAt (int t, int g) {
        return V[t][g];
    }
    // Get the best action at state (t,g)
    public Action actionAt (int t, int g) {
        return bestAction[t][g];
    }
    // Get the best k to claim at state (t,g) if action is CLAIM
    public int kAt (int t, int g) {
        return bestK[t][g];
    }
    
    // Print the optimal policy table
    public void printPolicy() {
        System.out.println("Optimal Policy Table (t,g): Action (k if CLAIM)");
        for (int t = 0; t <= N0; t++) {
            for (int g = 0; g <= K0; g++) {
                Action action = bestAction[t][g];
                if (action == Action.CLAIM) {
                    int k = bestK[t][g];
                    System.out.printf("(%2d,%2d): CLAIM (%d)  ", t, g, k);
                } else {
                    System.out.printf("(%2d,%2d): REVEAL     ", t, g);
                }
            }
            System.out.println();
        }
    }
}
