package ledprobabilitymodel;

import java.util.Map;
import java.util.TreeMap;

import ledprobabilitymodel.simulation.ExactCalculator;
import ledprobabilitymodel.simulation.MonteCarlo;
import ledprobabilitymodel.simulation.OptimalPlayerSimulator;
import ledprobabilitymodel.simulation.OptimalPolicy;
import ledprobabilitymodel.simulation.OutcomeDistribution;
import ledprobabilitymodel.simulation.Plotter;

public class Main {

    public static void main(String[] args) {

        int N = 30;
        int K = 3;
        int trials = 1_000_000;

        var dist = ExactCalculator.netDistribution(N, K, 4); // Edit k here for different distributions 
        Map<Integer, Double> exactEV = ExactCalculator.expectedNetByK(N, K);
        Map<Integer, Double> mcEV = MonteCarlo.expectedNetByKMC(trials, N, K);
        Map<Integer, Integer> mcNet = MonteCarlo.simulatedNetTotalByKMC(trials, N, K);
        Map<Integer, Integer> mcNumNet1 = MonteCarlo.numNetOccured(N, K, 1, trials);
        Map<Integer, Integer> mcNumNet2 = MonteCarlo.numNetOccured(N, K, 2, trials);
        Map<Integer, Integer> mcNumNet3 = MonteCarlo.numNetOccured(N, K, 3, trials);
        Map<Integer, Integer> mcNumNet4 = MonteCarlo.numNetOccured(N, K, 4, trials);
        Map<Integer, Integer> mcNumNet5 = MonteCarlo.numNetOccured(N, K, 5, trials);

        // System.out.println("k\tExactEV\t\tMonteCarloEV");
        // System.out.println("----------------------------------------");

        // int bestK = 1;
        // double bestExact = Double.NEGATIVE_INFINITY;

        // for (int k : exactEV.keySet()) {
        //     double e = exactEV.get(k);
        //     double m = mcEV.get(k);

        //     System.out.printf("%d\t% .6f\t% .6f%n", k, e, m);

        //     if (e > bestExact) {
        //         bestExact = e;
        //         bestK = k;
        //     }
        // }

        // for (int k : mcNet.keySet()) {
        //     int net = mcNet.get(k);
        //     System.out.printf("Simulated Total Net for k=%d: %d%n", k, net);
        // }

        // Optimal simulation data
        OptimalPolicy policy = new OptimalPolicy();
        OptimalPlayerSimulator optimalSim = new OptimalPlayerSimulator(policy);
        var optimalResult = optimalSim.simulate(trials);

        System.out.printf("%nOptimal policy average net over %,d trials: %.6f%n",
                trials, optimalResult.overallAverageNet());
        System.out.println("Average net by claimed k when following optimal policy:");
        var sorted = new TreeMap<>(optimalResult.averageNetByK());
        for (var e : sorted.entrySet()) {
            System.out.printf("k=%d -> avg net = %.6f%n", e.getKey(), e.getValue());
        }
        var optimalNetCounts = new TreeMap<>(optimalSim.netOccurrences(trials));
        System.out.println("\nNet payout occurrence counts under optimal policy:");
        for (var e : optimalNetCounts.entrySet()) {
            System.out.printf("net=%d -> count=%d%n", e.getKey(), e.getValue());
        }

        // Visualize expected/average net by k
        Plotter.plotExpectedNetByK("Optimal policy average net by k", "Optimal policy", sorted);
        // Plotter.plotExpectedNetByK("Exact expected net by k (non-optimal)", "Exact EV", exactEV);
        // Plotter.plotExpectedNetByK("Monte Carlo expected net by k", "MC EV", mcEV);

        // System.out.println("\nBest k under k ≤ 5 constraint:");
        // System.out.printf("k = %d, ExactEV = %.6f%n", bestK, bestExact);
        // Plotter.plotDistribution ("Probability of Payouts for 4 Claimed", dist);
        // Plotter.netNumDistribution ("Number of Occurrences of Payouts for k=5 (N=30, K=3, k=5)", mcNumNet5);
        
        Map<Integer, Double> netProb = OutcomeDistribution.optimalNetDistribution(policy);

        System.out.println("\nFull outcome distribution under optimal policy (net -> probability):");
        var sortedOutcomes = new TreeMap<>(netProb);
        for (var e : sortedOutcomes.entrySet()) {
            System.out.printf("net=%d -> %.10f%n", e.getKey(), e.getValue());
        }

        Plotter.plotDistribution("Optimal Policy Net Payout Distribution (N=30, K=3)", netProb);

    }
}


// mvn -q exec:java -Dexec.mainClass=ledprobabilitymodel.Main to run the code `
