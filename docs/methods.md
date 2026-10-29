# Probability model and validation

## State and assumptions

A state records revealed tiles t and good tiles revealed g. Given an initial board N with K good tiles, the hidden pool contains n = N - t tiles and m = K - g good tiles.

A state is valid only when:
- 0 <= t <= N.
- 0 <= g <= min(t, K).
- t - g <= N - K.

The board is uniformly randomized. Revealing a tile conveys only its good/bad type. Positions carry no additional information, so a uniformly random hidden claim has the same distribution as any chosen hidden positions.

Reveals cost zero and remove tiles from the claim pool. Once all good tiles have been revealed, the game ends with net -penalty. Claiming instead pays grossPayout[goodClaimed] minus claimSize. The terminal reveal penalty is not added to a claim which happens to contain all remaining good tiles.

## Fixed claims: hypergeometric distribution

For a claim of size c from n hidden tiles containing m good tiles:

```math
P(X=x) = choose(m,x) choose(n-m,c-x) / choose(n,c)
E[net] = sum_x (grossPayout[x] - c) P(X=x)
```

The valid support is max(0, c-(n-m)) through min(m,c). Impossible success counts have probability zero. Invalid populations or draws fail explicitly.

The method is analytical, evaluated with double-precision binomial coefficients. It is not arbitrary-precision rational arithmetic. Supported boards are capped at 200 tiles, good count 20, and claim size 50. These limits keep the coefficients finite and the table small.

## Adaptive policy: backward induction

At a nonterminal state, compute the expected net for every allowed claim. Let C(t,g) be the largest of these values.

The value of another reveal is:

```math
R(t,g) = (m/n) V(t+1,g+1) + ((n-m)/n) V(t+1,g)
V(t,g) = max(C(t,g), R(t,g))
```

Terminal states have V = -penalty when all good tiles have been revealed. Population-valid exhaustion also implies all good tiles were revealed.

The solver fills states backward by revealed count. It evaluates only population-valid states and only possible reveal branches, avoiding invalid successor states and zero-weight invalid arithmetic.

Strict double comparisons select the first, smallest claim size among exact claim ties. The solver claims on an exact claim-versus-reveal tie. No epsilon is used to call near-equal values equal. Platform-level floating-point rounding may affect nearly tied custom decisions even when expected values agree within tolerance.

`policy.csv` includes every population-valid state, including states the optimal policy never visits from the initial board. A terminal state is labeled END with claim size zero.

## Complete adaptive outcome distribution

Starting with mass one at (0,0), propagate probability forward:
- Terminal states contribute their penalty outcome.
- Claim states contribute their weighted hypergeometric net distribution.
- Reveal states split mass according to the remaining good/bad proportions.

This calculation uses the selected policy, not the Monte Carlo samples. Its expected net must agree with the initial Bellman value.

## Monte Carlo

An urn starts with the configured good and bad counts. Each draw uses `Random.nextInt(remaining)` to choose a uniformly random remaining tile. The chosen type is removed. This samples without replacement without allocating and shuffling a labeled board.

Fixed and adaptive strategies each start a fresh `java.util.Random` with the recorded seed. Their estimates are reproducible, but may be correlated because they share the seed. The reported MC SE describes each strategy's mean individually. It is not a standard error for the difference between two strategies.

One run per strategy builds a long-count histogram. The histogram determines the mean, sample variance, MC SE, and profit/loss frequencies:

```math
mean = sum_y count[y] y / trials
sampleVariance = sum_y count[y] (y - mean)^2 / (trials - 1)
MC_SE = sqrt(sampleVariance / trials)
```

At least two trials are required. The sample variance uses the trials-1 denominator. A deterministic outcome has zero standard error. This estimate may be noisy when rare large payouts have not appeared in a small run. Exact distributions remain the authority for model-based risk.

Changing from the old shuffled-board implementation to urn sampling changes the random sequence, so an old mean at the same seed need not recur. The probability law and default exact findings are preserved.

## Interpretation

For the original game:
- The best fixed claim is one tile, with mean -0.5.
- The adaptive mean is about +0.098522167488.
- Adaptive net outcomes are -1, 2, 5, and 25.
- P(loss) = 5/7, about 71.4286%.
- P(profit) = 2/7, about 28.5714%.

A mean-maximizing policy need not maximize the chance of profit. The fixed four-tile strategy has a higher chance of profit than the adaptive policy, yet has negative expected net. The five-tile strategy can break even, so P(profit) + P(loss) need not equal one.

Averages grouped by the claim size chosen by an adaptive policy condition on the state which led to that choice. They must not be ranked as if they were immediate fixed-size strategies. Main reports compare complete strategies from the same initial board.

## Independent checks

Tests enumerate labeled small boards directly, count their outcomes without using the production hypergeometric calculation, and use an independent enumerated-board recurrence to check policy values. Following the selected policy across every small board checks the final outcome distribution.

Other checks cover probability mass, nonnegative probabilities, distribution means versus policy values, original default findings, deterministic edge cases, strict ties, invalid settings, and fixed-seed reproducibility.

A seeded 100,000-trial check compares each simulated mean with its analytical value using six MC standard errors. This is a deterministic regression check with a statistical tolerance, not a mathematical proof or a performance benchmark.

The current finite-game model does not include reveal costs, utility curvature, a bankroll, time discounting, learning unknown rules, or hardware noise.

