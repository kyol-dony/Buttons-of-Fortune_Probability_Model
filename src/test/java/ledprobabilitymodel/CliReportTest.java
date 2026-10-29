package ledprobabilitymodel;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import ledprobabilitymodel.cli.Options;
import ledprobabilitymodel.game.GameConfig;
import ledprobabilitymodel.reporting.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliReportTest {
  @TempDir Path temp;

  private record Result(int exit, String out, String error) {}

  private Result run(String... args) {
    var out = new ByteArrayOutputStream();
    var error = new ByteArrayOutputStream();
    int exit =
        Main.run(
            args,
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(error, true, StandardCharsets.UTF_8));
    return new Result(
        exit, out.toString(StandardCharsets.UTF_8), error.toString(StandardCharsets.UTF_8));
  }

  @Test
  void helpIsFastAndDefaultsAreDocumented() {
    var r = run("--help");
    assertEquals(0, r.exit());
    assertTrue(r.out().contains("--trials"));
    assertTrue(r.out().contains("100000"));
    assertTrue(r.out().contains("2026"));
    assertTrue(r.error().isEmpty());
    var options = Options.parse(new String[] {});
    assertEquals(GameConfig.defaults(), options.config());
    assertEquals(100_000, options.trials());
    assertFalse(options.charts());
  }

  @Test
  void unknownDuplicateMissingAndInvalidOptionsReturnUsageError() {
    for (String[] args :
        new String[][] {
          {"--unknown"},
          {"--trials"},
          {"--trials", "x"},
          {"--trials", "0"},
          {"--seed", "1", "--seed", "2"},
          {"--charts", "--charts"},
          {"--payouts", "0,,5"},
          {"--good", "2"},
          {"--tiles", "1000000000"},
          {"--max-claim", "50", "--tiles", "200", "--trials", "1000000"},
          {"--trials", "2147483648"},
          {"--penalty", "-1"}
        }) {
      var r = run(args);
      assertEquals(2, r.exit(), Arrays.toString(args));
      assertFalse(r.error().isBlank());
    }
  }

  @Test
  void defaultCommandRunsHeadlesslyAndReportsBothExactAndSimulatedStatistics() {
    var r = run("--trials", "1000");
    assertEquals(0, r.exit());
    assertTrue(r.error().isEmpty());
    assertTrue(r.out().contains("fixed-1"));
    assertTrue(r.out().contains("adaptive"));
    assertTrue(r.out().contains("MC SE"));
    assertTrue(r.out().contains("Exact P(loss)"));
    assertFalse(r.out().contains("NaN"));
  }

  @Test
  void customConfigurationExportsConsistentLocaleIndependentReports() throws Exception {
    var output = temp.resolve("analysis with spaces");
    Locale original = Locale.getDefault();
    Result r;
    try {
      Locale.setDefault(Locale.GERMANY);
      r =
          run(
              "--tiles",
              "8",
              "--good",
              "2",
              "--max-claim",
              "3",
              "--penalty",
              "4",
              "--payouts",
              "0,4,19",
              "--trials",
              "2000",
              "--seed",
              "42",
              "--output",
              output.toString());
    } finally {
      Locale.setDefault(original);
    }
    assertEquals(0, r.exit(), r.error());
    for (String file : List.of("summary.md", "strategies.csv", "outcomes.csv", "policy.csv")) {
      assertTrue(Files.isRegularFile(output.resolve(file)));
    }
    var lines = Files.readAllLines(output.resolve("strategies.csv"));
    var header = List.of(lines.get(0).split(","));
    assertEquals(5, lines.size());
    for (var line : lines.subList(1, lines.size())) {
      var row = line.split(",", -1);
      assertEquals(header.size(), row.length);
      assertEquals("8", row[header.indexOf("tiles")]);
      assertEquals("0|4|19", row[header.indexOf("payouts")]);
      assertEquals("42", row[header.indexOf("seed")]);
      double mean = Double.parseDouble(row[header.indexOf("exact_mean")]);
      assertTrue(Double.isFinite(mean));
    }
    var outcomes = Files.readAllLines(output.resolve("outcomes.csv"));
    var oh = List.of(outcomes.get(0).split(","));
    Map<String, Long> counts = new HashMap<>();
    Map<String, Double> mass = new HashMap<>(), means = new HashMap<>();
    for (var line : outcomes.subList(1, outcomes.size())) {
      var row = line.split(",");
      var strategy = row[oh.indexOf("strategy")];
      counts.merge(strategy, Long.parseLong(row[oh.indexOf("simulated_count")]), Long::sum);
      double probability = Double.parseDouble(row[oh.indexOf("exact_probability")]);
      mass.merge(strategy, probability, Double::sum);
      means.merge(strategy, probability * Integer.parseInt(row[oh.indexOf("net")]), Double::sum);
    }
    assertEquals(4, counts.size());
    counts.values().forEach(n -> assertEquals(2000, n.longValue()));
    mass.values().forEach(p -> assertEquals(1, p, 1e-11));
    for (var line : lines.subList(1, lines.size())) {
      var row = line.split(",");
      assertEquals(
          Double.parseDouble(row[header.indexOf("exact_mean")]),
          means.get(row[header.indexOf("strategy")]),
          1e-11);
    }
    var summary = Files.readString(output.resolve("summary.md"));
    assertTrue(summary.contains("MC standard error"));
    assertTrue(summary.contains("0, 4, 19"));
    assertTrue(summary.contains("42"));
    assertTrue(
        Files.readString(output.resolve("policy.csv"))
            .startsWith("tiles,good,max_claim,penalty,payouts,trials,seed,"));
  }

  @Test
  void existingFilesAreNeverOverwrittenAndUnrelatedFilesRemain() throws Exception {
    var study = Study.run(GameConfig.defaults(), 100, 2026);
    Files.writeString(temp.resolve("notes.txt"), "keep");
    ReportWriter.write(study, temp);
    assertEquals("keep", Files.readString(temp.resolve("notes.txt")));
    var original = Files.readString(temp.resolve("summary.md"));
    assertThrows(IOException.class, () -> ReportWriter.write(study, temp));
    assertEquals(original, Files.readString(temp.resolve("summary.md")));
    var other = temp.resolve("other");
    Files.createDirectories(other);
    Files.writeString(other.resolve("policy.csv"), "keep");
    assertThrows(IOException.class, () -> ReportWriter.write(study, other));
    assertFalse(Files.exists(other.resolve("summary.md")));
    assertEquals("keep", Files.readString(other.resolve("policy.csv")));
    var result = run("--trials", "100", "--output", other.toString());
    assertEquals(1, result.exit());
  }

  @Test
  void zeroGoodCustomCaseHasOnlyTerminalOutcomes() throws Exception {
    var r =
        run(
            "--tiles",
            "4",
            "--good",
            "0",
            "--max-claim",
            "3",
            "--payouts",
            "0",
            "--penalty",
            "7",
            "--trials",
            "50");
    assertEquals(0, r.exit(), r.error());
    assertTrue(r.out().contains("-7.000000"));
  }
}
