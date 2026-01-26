package ledprobabilitymodel.simulation;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.SwingWrapper;

import java.util.*;

// Provides plotting functions for visualizing distributions
public class Plotter {
    public static void plotDistribution (String title, Map<Integer, Double> dist){

         // Sort outcomes (nets) so bars appear left-to-right
        List<Integer> x = new ArrayList<>(dist.keySet());
        Collections.sort(x);

        List<Double> y = new ArrayList<>();
        for (int net : x) y.add(dist.get(net));
        
        /* Creates chart that visualizes\information gathered via
           ExactCalculator package  */
        CategoryChart chart = new CategoryChartBuilder()
            .width(900).height(500)
            .title(title)
            .xAxisTitle("Net payout")
            .yAxisTitle("Probability")
            .build();
        
        chart.addSeries("P(Net)", x, y);

        new SwingWrapper<>(chart).displayChart();
    }
    
    // Plot for MonteCarlo results
    public static void netNumDistribution (String title, Map<Integer, Integer> dist){

         // Sort outcomes (nets) so bars appear left-to-right
        List<Integer> x = new ArrayList<>(dist.keySet());
        Collections.sort(x);

        List<Integer> y = new ArrayList<>();
        for (int net : x) y.add(dist.get(net));
        
        /* Creates chart that visualizes\information gathered via
           MonteCarlo package  */
        CategoryChart chart = new CategoryChartBuilder()
            .width(900).height(500)
            .title(title)
            .xAxisTitle("Net payout")
            .yAxisTitle("Number of Occurrences")
            .build();
        
        chart.addSeries("Num(Net)", x, y);

        new SwingWrapper<>(chart).displayChart();
    }
}

/* To call 
 * Plotter.plotDistribution ("Probability of Payouts (N=30, K=3, k=5)", dist);
 * Plotter.netNumDistribution ("Number of Occurrences of Payouts (N=30, K=3, k=5)", mcNumNet5);
*/
