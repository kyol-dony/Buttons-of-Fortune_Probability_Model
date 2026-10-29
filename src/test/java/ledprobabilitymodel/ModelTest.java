package ledprobabilitymodel;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import ledprobabilitymodel.game.GameConfig;
import ledprobabilitymodel.game.GameState;
import ledprobabilitymodel.math.Combinatorics;
import ledprobabilitymodel.math.Hypergeometric;
import org.junit.jupiter.api.Test;

class ModelTest {
  @Test
  void defaultRevealPenaltyFollowsLatestMasterRules() {
    assertEquals(3, GameConfig.defaults().revealPenalty());
    assertEquals(3, ledprobabilitymodel.simulation.OptimalPolicy.REVEAL_PENALTY);
    assertEquals(-3, GameConfig.defaults().terminalNet(new GameState(3, 3)));
  }

  @Test
  void defaultPayoutsAndCostsMatchOriginalGame() {
    var c = GameConfig.defaults();
    assertEquals(-5, c.net(0, 5));
    assertEquals(4, c.net(1, 1));
    assertEquals(8, c.net(2, 2));
    assertEquals(27, c.net(3, 3));
    assertThrows(IllegalArgumentException.class, () -> c.net(4, 4));
    assertThrows(IllegalArgumentException.class, () -> c.net(2, 1));
  }

  @Test
  void invalidAndOversizedConfigurationsFailBeforeAllocation() {
    for (int[] p :
        new int[][] {
          {0, 0, 1},
          {2, 3, 1},
          {201, 3, 5},
          {30, 21, 5},
          {30, -1, 5},
          {30, 3, 0},
          {30, 3, 31},
          {100, 3, 51}
        }) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new GameConfig(p[0], p[1], p[2], 1, List.of(0, 5, 10, 30)));
    }
    assertThrows(
        IllegalArgumentException.class, () -> new GameConfig(30, 3, 5, -1, List.of(0, 5, 10, 30)));
    assertThrows(IllegalArgumentException.class, () -> new GameConfig(30, 3, 5, 1, List.of(0, 5)));
    assertThrows(
        IllegalArgumentException.class, () -> new GameConfig(30, 3, 5, 1, List.of(0, -5, 10, 30)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new GameConfig(30, 3, 5, 1, List.of(0, 5, 10, 1_000_001)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new GameConfig(30, 3, 5, 1_000_001, List.of(0, 5, 10, 30)));
    assertDoesNotThrow(
        () -> new GameConfig(200, 20, 50, 0, java.util.Collections.nCopies(21, 1_000_000)));
  }

  @Test
  void rulesOwnAnImmutablePayoutTable() {
    var payouts = new ArrayList<>(List.of(0, 5));
    var c = new GameConfig(2, 1, 2, 1, payouts);
    payouts.set(1, 90);
    assertEquals(4, c.net(1, 1));
    assertThrows(UnsupportedOperationException.class, () -> c.payouts().set(0, 8));
  }

  @Test
  void stateValidationChecksBothGoodAndBadTileAvailability() {
    var c = GameConfig.defaults();
    assertDoesNotThrow(() -> c.validateState(new GameState(29, 2)));
    for (var state :
        List.of(
            new GameState(-1, 0),
            new GameState(31, 3),
            new GameState(0, 1),
            new GameState(29, 0),
            new GameState(3, -1))) {
      assertThrows(IllegalArgumentException.class, () -> c.validateState(state));
    }
  }

  @Test
  void hypergeometricMatchesEnumeratedSmallBoards() {
    for (int total = 1; total <= 8; total++) {
      for (int good = 0; good <= total; good++) {
        for (int draws = 0; draws <= total; draws++) {
          long[] hits = new long[good + 1];
          long boards = 0;
          for (int mask = 0; mask < (1 << total); mask++) {
            if (Integer.bitCount(mask) != good) continue;
            hits[Integer.bitCount(mask & ((1 << draws) - 1))]++;
            boards++;
          }
          double mass = 0;
          for (int found = 0; found <= good; found++) {
            double p = Hypergeometric.pmf(found, total, good, draws);
            assertEquals((double) hits[found] / boards, p, 1e-12);
            mass += p;
          }
          assertEquals(1.0, mass, 1e-12);
        }
      }
    }
  }

  @Test
  void probabilityDomainAndCombinationsAreExplicit() {
    assertEquals(10, Combinatorics.nCk(5, 2));
    assertEquals(0, Combinatorics.nCk(5, 6));
    assertThrows(IllegalArgumentException.class, () -> Combinatorics.nCk(-1, 0));
    assertEquals(0, Hypergeometric.pmf(-1, 30, 3, 5));
    assertThrows(IllegalArgumentException.class, () -> Hypergeometric.pmf(0, 2, 3, 1));
    assertThrows(IllegalArgumentException.class, () -> Hypergeometric.pmf(0, 2, 1, 3));
    assertEquals(1, Hypergeometric.pmf(0, 0, 0, 0));
    assertTrue(Double.isFinite(Hypergeometric.pmf(10, 200, 20, 50)));
  }
}
