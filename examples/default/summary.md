# Buttons of Fortune: Reproducible Study

Question: how do immediate fixed claims compare with an expected-net-maximizing reveal-or-claim policy?

## Settings

- Playable tiles: 30
- Good tiles: 3
- Maximum claim: 5
- Terminal reveal penalty: 1
- Gross payouts by good tiles claimed: [0, 5, 10, 30]
- Trials per strategy: 100000
- Seed: 2026

## Results

| Strategy | Exact mean | Simulated mean | MC standard error | Exact P(profit) | Exact P(loss) |
| --- | ---: | ---: | ---: | ---: | ---: |
| fixed-1 | -0.500000000 | -0.494950000 | 0.004764659 | 0.100000000 | 0.900000000 |
| fixed-2 | -1.000000000 | -0.998750000 | 0.006593020 | 0.193103448 | 0.806896552 |
| fixed-3 | -1.496305419 | -1.501150000 | 0.008027928 | 0.279556650 | 0.720443350 |
| fixed-4 | -1.985221675 | -1.980800000 | 0.009411387 | 0.359605911 | 0.640394089 |
| fixed-5 | -2.463054187 | -2.466550000 | 0.010518076 | 0.064039409 | 0.566502463 |
| adaptive | 0.098522167 | 0.092020000 | 0.006767258 | 0.285714286 | 0.714285714 |

## Methods and interpretation

Fixed claims use hypergeometric probabilities for sampling without replacement. Backward induction chooses the best expected net among claims and another free reveal. Analytical values use double precision. Seeded Monte Carlo independently samples the same game rules. Each strategy starts a fresh java.util.Random instance with the recorded seed.

MC standard error is sqrt(sample variance / trials), computed from the same outcome histogram as the mean. Profit means net > 0; loss means net < 0. Probability of profit and expected net are different objectives. A positive mean does not imply a high chance of profit in a single game.

Revealed tiles cannot be claimed, reveals cost nothing, and revealing every good tile ends the game with the penalty. These assumptions govern the findings. No hardware is required. The toolkit does not estimate real-player behavior.

See strategies.csv for simulated profit/loss probabilities, outcomes.csv for full distributions, and policy.csv for decisions at every valid state, including states not visited by the optimal policy.
