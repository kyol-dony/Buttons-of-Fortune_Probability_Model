package ledprobabilitymodel;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.PrintStream;
import ledprobabilitymodel.cli.Options;
import ledprobabilitymodel.reporting.ReportWriter;
import ledprobabilitymodel.reporting.Study;
import ledprobabilitymodel.simulation.Plotter;

/** Command-line entry point; normal analyses run without a graphical desktop. */
public final class Main {
  private Main() {}

  public static void main(String[] args) {
    int status = run(args, System.out, System.err);
    if (status != 0) System.exit(status);
  }

  /** Returns 0 on success, 2 for invalid arguments, and 1 for output failures. */
  public static int run(String[] args, PrintStream output, PrintStream error) {
    try {
      var options = Options.parse(args);
      if (options.help()) {
        output.print(Options.usage());
        return 0;
      }
      if (options.charts() && GraphicsEnvironment.isHeadless()) {
        throw new IllegalArgumentException(
            "--charts requires a graphical desktop; omit it for headless analysis");
      }
      var study = Study.run(options.config(), options.trials(), options.seed());
      if (options.output() != null) ReportWriter.write(study, options.output());
      output.print(ReportWriter.console(study));
      if (options.output() != null) output.println("Reports: " + options.output().toAbsolutePath());
      if (options.charts()) {
        var adaptive = study.results().get(study.results().size() - 1);
        Plotter.plotDistribution("Adaptive net-payout distribution", adaptive.exact());
        var expected = new java.util.TreeMap<Integer, Double>();
        study.results().stream()
            .filter(r -> r.claimSize() > 0)
            .forEach(r -> expected.put(r.claimSize(), r.exactMean()));
        Plotter.plotExpectedNetByK("Immediate fixed claims", "Exact expected net", expected);
      }
      return 0;
    } catch (IllegalArgumentException exception) {
      error.println("Error: " + exception.getMessage());
      error.println("Use --help for usage.");
      return 2;
    } catch (IOException exception) {
      error.println("Could not write reports: " + exception.getMessage());
      return 1;
    }
  }
}
