package ledprobabilitymodel.cli;

import java.nio.file.Path;
import java.util.*;
import ledprobabilitymodel.game.GameConfig;
import ledprobabilitymodel.simulation.Simulator;

/** Strict, dependency-free command-line parsing. */
public record Options(
    GameConfig config, int trials, long seed, Path output, boolean charts, boolean help) {
  private static final Set<String> VALUE_FLAGS =
      Set.of(
          "--tiles",
          "--good",
          "--max-claim",
          "--penalty",
          "--payouts",
          "--trials",
          "--seed",
          "--output");

  public static Options parse(String[] args) {
    Map<String, String> values = new HashMap<>();
    Set<String> seen = new HashSet<>();
    boolean charts = false, help = false;
    for (int i = 0; i < args.length; i++) {
      String flag = args[i].equals("-h") ? "--help" : args[i];
      if (!seen.add(flag)) throw new IllegalArgumentException("duplicate option: " + flag);
      if (flag.equals("--help")) {
        help = true;
      } else if (flag.equals("--charts")) {
        charts = true;
      } else if (VALUE_FLAGS.contains(flag)) {
        if (++i >= args.length || args[i].startsWith("--")) {
          throw new IllegalArgumentException("missing value for " + flag);
        }
        values.put(flag, args[i]);
      } else {
        throw new IllegalArgumentException("unknown option: " + flag);
      }
    }
    try {
      var defaults = GameConfig.defaults();
      List<Integer> payouts = defaults.payouts();
      if (values.containsKey("--payouts")) {
        payouts =
            Arrays.stream(values.get("--payouts").split(",", -1))
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();
      }
      var config =
          new GameConfig(
              integer(values, "--tiles", defaults.totalTiles()),
              integer(values, "--good", defaults.goodTiles()),
              integer(values, "--max-claim", defaults.maxClaim()),
              integer(values, "--penalty", defaults.revealPenalty()),
              payouts);
      int trials = integer(values, "--trials", 100_000);
      if (trials < 2 || trials > Simulator.MAX_TRIALS) {
        throw new IllegalArgumentException("trials must be between 2 and " + Simulator.MAX_TRIALS);
      }
      long seed = Long.parseLong(values.getOrDefault("--seed", "2026"));
      Path output = values.containsKey("--output") ? Path.of(values.get("--output")) : null;
      if (output != null && values.get("--output").isBlank()) {
        throw new IllegalArgumentException("output directory must not be blank");
      }
      return new Options(config, trials, seed, output, charts, help);
    } catch (NumberFormatException error) {
      throw new IllegalArgumentException(
          "numeric options require in-range integers; payouts use comma-separated integers");
    }
  }

  private static int integer(Map<String, String> values, String key, int fallback) {
    return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback)));
  }

  public static String usage() {
    return """
    Buttons of Fortune: Probability Analysis Toolkit
    Usage: mvn -q exec:java -Dexec.args="[options]"
      --tiles N        Playable tiles, 1..200 (default 30)
      --good N         Good tiles, 0..min(tiles,20) (default 3)
      --max-claim N    Claim cap, 1..min(tiles,50) (default 5)
      --penalty N      Loss after revealing all good tiles (default 1)
      --payouts LIST   Gross payouts for 0..good claimed (default 0,5,10,30)
      --trials N       Trials per strategy, 2..1000000 (default 100000)
      --seed N         Signed 64-bit random seed (default 2026)
      --output PATH    Write CSV and Markdown reports; refuse existing report files
      --charts         Open optional Swing charts; requires a graphical desktop
      --help, -h       Show this help
    Payouts and penalty: 0..1000000. Custom good count requires matching payouts.
    A study allows at most 100 million worst-case simulated tile draws.
    """;
  }
}
