package ledprobabilitymodel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import ledprobabilitymodel.game.*;
import ledprobabilitymodel.simulation.*;
import org.junit.jupiter.api.Test;

class AnalysisTest {
  @Test
  void defaultThreeTokenPenaltyMatchesUpdatedMasterFindings() {
    var policy = new OptimalPolicy();
    var distribution = OutcomeDistribution.optimalNetDistribution(policy);
    assertEquals(-0.19113300492610846, policy.valueAt(GameState.initial()), 1e-12);
    assertEquals(policy.valueAt(GameState.initial()), mean(distribution), 1e-12);
    assertEquals(0.1556650246305418, FavorAnalysis.profitProbability(distribution), 1e-12);
    assertTrue(distribution.containsKey(-3));
    assertDistribution(distribution);
  }

  @Test
  void originalOneTokenPenaltyFindingsRemainReproducible() {
    var c = new GameConfig(30, 3, 5, 1, List.of(0, 5, 10, 30));
    double[] expected = {-0.5, -1, -1.4963054187192115, -1.9852216748768474, -2.4630541871921183};
    for (int size = 1; size <= 5; size++) {
      assertEquals(
          expected[size - 1], ExactCalculator.expectedNet(c, GameState.initial(), size), 1e-12);
    }
    var policy = new OptimalPolicy(c);
    var dist = OutcomeDistribution.optimalNetDistribution(policy);
    assertDistributionEquals(
        Map.of(
            -1,
            0.7142857142857142,
            2,
            0.22167487684729073,
            5,
            0.061576354679802964,
            25,
            0.002463054187192118),
        dist);
    assertEquals(0.0985221674876847, policy.valueAt(GameState.initial()), 1e-12);
    assertEquals(policy.valueAt(GameState.initial()), mean(dist), 1e-12);
    assertDistribution(dist);
  }

  @Test
  void analyticalInputsRejectImpossibleStatesAndClaims() {
    var c = GameConfig.defaults();
    assertThrows(IllegalArgumentException.class, () -> ExactCalculator.netDistribution(2, 3, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> ExactCalculator.netDistribution(c, new GameState(29, 0), 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> ExactCalculator.netDistribution(c, GameState.initial(), 6));
    var policy = new OptimalPolicy(c);
    assertThrows(IllegalArgumentException.class, () -> policy.valueAt(new GameState(0, 2)));
    assertThrows(IllegalArgumentException.class, () -> policy.actionAt(29, 0));
  }

  @Test
  void zeroGoodAndAllGoodGamesHaveConsistentTerminalRules() {
    var empty = new GameConfig(4, 0, 3, 7, List.of(0));
    var p = new OptimalPolicy(empty);
    assertEquals(-7, p.valueAt(GameState.initial()));
    assertEquals(Map.of(-7, 1.0), OutcomeDistribution.optimalNetDistribution(p));
    assertEquals(Map.of(-7, 1.0), ExactCalculator.netDistribution(empty, GameState.initial(), 1));
    var allGood = new GameConfig(3, 3, 3, 1, List.of(0, 5, 10, 30));
    assertEquals(27, new OptimalPolicy(allGood).valueAt(GameState.initial()));
    assertEquals(
        Map.of(27, 1.0), OutcomeDistribution.optimalNetDistribution(new OptimalPolicy(allGood)));
  }

  @Test
  void exactTiesPreferSmallestClaimAndClaimOverReveal() {
    var p = new OptimalPolicy(new GameConfig(2, 2, 2, 1, List.of(0, 2, 3)));
    assertEquals(new Decision(Action.CLAIM, 1), p.decisionAt(GameState.initial()));
  }

  @Test
  void smallBoardPoliciesAgreeWithIndependentEnumeration() {
    for (int total = 1; total <= 7; total++) {
      for (int good = 0; good <= Math.min(3, total); good++) {
        var payouts = new ArrayList<Integer>();
        for (int i = 0; i <= good; i++) payouts.add(i * i * 3);
        var c = new GameConfig(total, good, Math.min(total, 3), 2, payouts);
        var p = new OptimalPolicy(c);
        for (var state : p.reachableStates()) {
          assertEquals(
              oracle(c, state, new HashMap<>()), p.valueAt(state), 1e-10, state.toString());
        }
        var dist = OutcomeDistribution.optimalNetDistribution(p);
        assertDistribution(dist);
        assertEquals(p.valueAt(GameState.initial()), mean(dist), 1e-10);
        assertDistributionEquals(enumeratePolicy(c, p), dist);
      }
    }
  }

  @Test
  void profitProbabilityAndExpectedValueRemainSeparate() {
    var c = GameConfig.defaults();
    assertEquals(-0.5, FavorAnalysis.bestExpectedNet(c, GameState.initial()).expectedNet(), 1e-12);
    assertTrue(FavorAnalysis.bestProfit(c, GameState.initial()).probability() > 0);
    assertFalse(
        FavorAnalysis.positiveExpectedValueStates(new OptimalPolicy(c))
            .contains(GameState.initial()));
    for (var s : FavorAnalysis.positiveExpectedValueStates(new OptimalPolicy(c))) {
      assertTrue(FavorAnalysis.bestExpectedNet(c, s).expectedNet() > 0);
    }
  }

  private static void assertDistributionEquals(
      Map<Integer, Double> expected, Map<Integer, Double> actual) {
    assertEquals(expected.keySet(), actual.keySet());
    expected.forEach((net, probability) -> assertEquals(probability, actual.get(net), 1e-12));
  }

  private static double mean(Map<Integer, Double> dist) {
    return dist.entrySet().stream().mapToDouble(e -> e.getKey() * e.getValue()).sum();
  }

  private static void assertDistribution(Map<Integer, Double> dist) {
    assertEquals(1, dist.values().stream().mapToDouble(Double::doubleValue).sum(), 1e-11);
    assertTrue(dist.values().stream().allMatch(p -> p >= 0 && Double.isFinite(p)));
  }

  // Enumerates actual labeled boards, without calling production probability functions.
  private static double oracle(GameConfig c, GameState s, Map<GameState, Double> memo) {
    if (c.terminal(s)) return c.terminalNet(s);
    if (memo.containsKey(s)) return memo.get(s);
    int remaining = c.totalTiles() - s.revealed(), good = c.goodTiles() - s.goodRevealed();
    var boards = new ArrayList<Integer>();
    for (int mask = 0; mask < (1 << remaining); mask++)
      if (Integer.bitCount(mask) == good) boards.add(mask);
    double best = Double.NEGATIVE_INFINITY;
    for (int size = 1; size <= Math.min(c.maxClaim(), remaining); size++) {
      double sum = 0;
      for (int mask : boards)
        sum += c.payouts().get(Integer.bitCount(mask & ((1 << size) - 1))) - size;
      best = Math.max(best, sum / boards.size());
    }
    double reveal = 0;
    for (int mask : boards)
      reveal +=
          oracle(c, new GameState(s.revealed() + 1, s.goodRevealed() + (mask & 1)), memo)
              / boards.size();
    double result = Math.max(best, reveal);
    memo.put(s, result);
    return result;
  }

  private static Map<Integer, Double> enumeratePolicy(GameConfig c, OptimalPolicy p) {
    Map<Integer, Long> counts = new TreeMap<>();
    long boards = 0;
    for (int mask = 0; mask < (1 << c.totalTiles()); mask++) {
      if (Integer.bitCount(mask) != c.goodTiles()) continue;
      boards++;
      var s = GameState.initial();
      while (!c.terminal(s) && p.decisionAt(s).action() == Action.REVEAL) {
        s = new GameState(s.revealed() + 1, s.goodRevealed() + ((mask >> s.revealed()) & 1));
      }
      int net;
      if (c.terminal(s)) net = c.terminalNet(s);
      else {
        int size = p.decisionAt(s).claimSize();
        int good = Integer.bitCount((mask >> s.revealed()) & ((1 << size) - 1));
        net = c.payouts().get(good) - size;
      }
      counts.merge(net, 1L, Long::sum);
    }
    Map<Integer, Double> result = new TreeMap<>();
    for (var e : counts.entrySet()) result.put(e.getKey(), (double) e.getValue() / boards);
    return result;
  }
}
