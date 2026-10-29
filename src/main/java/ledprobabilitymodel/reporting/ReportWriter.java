package ledprobabilitymodel.reporting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Renders all views from the same study; decimal formatting is locale independent. */
public final class ReportWriter {
  private static final String SETTINGS = "tiles,good,max_claim,penalty,payouts,trials,seed,";
  private static final List<String> FILENAMES =
      List.of("summary.md", "strategies.csv", "outcomes.csv", "policy.csv");

  private ReportWriter() {}

  public static String console(Study study) {
    var config = study.config();
    var text = new StringBuilder("Buttons of Fortune | Probability Analysis Toolkit\n");
    text.append(
        String.format(
            Locale.ROOT,
            "Tiles=%d Good=%d Max claim=%d Penalty=%d Payouts=%s Trials=%d Seed=%d%n",
            config.totalTiles(),
            config.goodTiles(),
            config.maxClaim(),
            config.revealPenalty(),
            config.payouts(),
            study.trials(),
            study.seed()));
    text.append(
        String.format(
            Locale.ROOT,
            "%-10s %12s %12s %10s %15s %14s%n",
            "Strategy",
            "Exact mean",
            "MC mean",
            "MC SE",
            "Exact P(profit)",
            "Exact P(loss)"));
    for (var result : study.results()) {
      text.append(
          String.format(
              Locale.ROOT,
              "%-10s %12.6f %12.6f %10.6f %15.6f %14.6f%n",
              result.strategy(),
              result.exactMean(),
              result.simulated().mean(),
              result.simulated().standardError(),
              result.exactProbability(true),
              result.exactProbability(false)));
    }
    text.append("MC SE measures simulation error. It does not describe the risk of one game.\n");
    text.append(
        "Adaptive results include free reveals and a loss when all good tiles are revealed.\n");
    return text.toString();
  }

  public static void write(Study study, Path output) throws IOException {
    Files.createDirectories(output);
    for (var filename : FILENAMES) {
      if (Files.exists(output.resolve(filename), LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException(
            "report already exists: "
                + output.resolve(filename)
                + "; choose a new output directory");
      }
    }
    var documents = List.of(summary(study), strategies(study), outcomes(study), policy(study));
    for (int i = 0; i < FILENAMES.size(); i++) {
      Files.writeString(
          output.resolve(FILENAMES.get(i)),
          documents.get(i),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE_NEW);
    }
  }

  private static String settings(Study study) {
    var c = study.config();
    String payouts = String.join("|", c.payouts().stream().map(Object::toString).toList());
    return c.totalTiles()
        + ","
        + c.goodTiles()
        + ","
        + c.maxClaim()
        + ","
        + c.revealPenalty()
        + ","
        + payouts
        + ","
        + study.trials()
        + ","
        + study.seed()
        + ",";
  }

  private static String strategies(Study study) {
    var text =
        new StringBuilder(
            SETTINGS
                + "strategy,claim_size,exact_mean,simulated_mean,mc_se,"
                + "exact_profit,exact_loss,simulated_profit,simulated_loss\n");
    for (var r : study.results()) {
      text.append(settings(study))
          .append(r.strategy())
          .append(',')
          .append(r.claimSize())
          .append(',')
          .append(r.exactMean())
          .append(',')
          .append(r.simulated().mean())
          .append(',')
          .append(r.simulated().standardError())
          .append(',')
          .append(r.exactProbability(true))
          .append(',')
          .append(r.exactProbability(false))
          .append(',')
          .append(r.simulatedProbability(true))
          .append(',')
          .append(r.simulatedProbability(false))
          .append('\n');
    }
    return text.toString();
  }

  private static String outcomes(Study study) {
    var text =
        new StringBuilder(
            SETTINGS + "strategy,net,exact_probability,simulated_count,simulated_probability\n");
    for (var r : study.results()) {
      var nets = new TreeSet<>(r.exact().keySet());
      nets.addAll(r.simulated().outcomes().keySet());
      for (int net : nets) {
        long count = r.simulated().outcomes().getOrDefault(net, 0L);
        text.append(settings(study))
            .append(r.strategy())
            .append(',')
            .append(net)
            .append(',')
            .append(r.exact().getOrDefault(net, 0.0))
            .append(',')
            .append(count)
            .append(',')
            .append((double) count / r.simulated().trials())
            .append('\n');
      }
    }
    return text.toString();
  }

  private static String policy(Study study) {
    var text =
        new StringBuilder(SETTINGS + "revealed,good_revealed,action,claim_size,expected_net\n");
    for (var state : study.policy().reachableStates()) {
      var decision = study.policy().decisionAt(state);
      text.append(settings(study))
          .append(state.revealed())
          .append(',')
          .append(state.goodRevealed())
          .append(',')
          .append(study.config().terminal(state) ? "END" : decision.action())
          .append(',')
          .append(decision.claimSize())
          .append(',')
          .append(study.policy().valueAt(state))
          .append('\n');
    }
    return text.toString();
  }

  private static String summary(Study study) {
    var text = new StringBuilder("# Buttons of Fortune: Reproducible Study\n\n");
    text.append(
        "Question: how do immediate fixed claims compare with an expected-net-maximizing"
            + " reveal-or-claim policy?\n\n");
    text.append("## Settings\n\n")
        .append("- Playable tiles: ")
        .append(study.config().totalTiles())
        .append("\n- Good tiles: ")
        .append(study.config().goodTiles())
        .append("\n- Maximum claim: ")
        .append(study.config().maxClaim())
        .append("\n- Terminal reveal penalty: ")
        .append(study.config().revealPenalty())
        .append("\n- Gross payouts by good tiles claimed: ")
        .append(study.config().payouts())
        .append("\n- Trials per strategy: ")
        .append(study.trials())
        .append("\n- Seed: ")
        .append(study.seed())
        .append("\n\n");
    text.append(
        "## Results\n\n"
            + "| Strategy | Exact mean | Simulated mean | MC standard error | Exact P(profit) |"
            + " Exact P(loss) |\n");
    text.append("| --- | ---: | ---: | ---: | ---: | ---: |\n");
    for (var r : study.results()) {
      text.append(
          String.format(
              Locale.ROOT,
              "| %s | %.9f | %.9f | %.9f | %.9f | %.9f |%n",
              r.strategy(),
              r.exactMean(),
              r.simulated().mean(),
              r.simulated().standardError(),
              r.exactProbability(true),
              r.exactProbability(false)));
    }
    text.append("\n## Methods and interpretation\n\n")
        .append("Fixed claims use hypergeometric probabilities for sampling without replacement. ")
        .append(
            "Backward induction chooses the best expected net among claims and another free reveal."
                + " ")
        .append(
            "Analytical values use double precision. Seeded Monte Carlo independently samples the"
                + " same game rules. ")
        .append(
            "Each strategy starts a fresh java.util.Random instance with the recorded seed.\n\n")
        .append(
            "MC standard error is sqrt(sample variance / trials), computed from the same outcome"
                + " histogram as the mean. ")
        .append(
            "Profit means net > 0; loss means net < 0. Probability of profit and expected net are"
                + " different objectives. ")
        .append("A positive mean does not imply a high chance of profit in a single game.\n\n")
        .append(
            "Revealed tiles cannot be claimed, reveals cost nothing, and revealing every good tile"
                + " ends the game with the penalty. ")
        .append(
            "These assumptions govern the findings. No hardware is required. The toolkit does not"
                + " estimate real-player behavior.\n\n")
        .append(
            "See strategies.csv for simulated profit/loss probabilities, outcomes.csv for full"
                + " distributions, ")
        .append(
            "and policy.csv for decisions at every valid state, including states not visited by the"
                + " optimal policy.\n");
    return text.toString();
  }
}
