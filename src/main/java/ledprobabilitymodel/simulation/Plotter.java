package ledprobabilitymodel.simulation;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.SwingWrapper;

/** Optional desktop plots. The CLI invokes these only with --charts. */
public final class Plotter {
  private Plotter() {}

  public static void plotDistribution(String title, Map<Integer, Double> distribution) {
    show(title, "Net payout", "Probability", "P(net)", distribution);
  }

  public static void plotExpectedNetByK(
      String title, String seriesName, Map<Integer, Double> values) {
    show(title, "Claim size", "Expected net", seriesName, values);
  }

  public static void netNumDistribution(String title, Map<Integer, Integer> counts) {
    show(title, "Net payout", "Occurrences", "Count", counts);
  }

  private static void show(
      String title,
      String xLabel,
      String yLabel,
      String series,
      Map<Integer, ? extends Number> values) {
    var sorted = new TreeMap<>(values);
    CategoryChart chart =
        new CategoryChartBuilder()
            .width(900)
            .height(500)
            .title(title)
            .xAxisTitle(xLabel)
            .yAxisTitle(yLabel)
            .build();
    chart.addSeries(series, new ArrayList<>(sorted.keySet()), new ArrayList<>(sorted.values()));
    new SwingWrapper<>(chart).displayChart();
  }
}
