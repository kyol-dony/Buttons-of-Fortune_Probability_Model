# Buttons of Fortune Probability Model

This project explores the “Buttons of Fortune” style LED guessing game. It provides:
- Exact combinatorial probability calculations (hypergeometric).
- Monte Carlo simulations for fixed strategies.
- Dynamic-programming “optimal” policy that decides when to reveal vs. claim.
- An outcome distribution (full net payout probabilities) under the optimal policy.
- Swing charts to visualize expected values and outcome distributions.

## Requirements
- JDK 17+
- Maven (for building/running)

## Run
From the project root:
```bash
mvn -q exec:java -Dexec.mainClass=ledprobabilitymodel.Main
```
This runs the simulations/DP, prints summary stats, and opens Swing charts.

## What the main run does
- Computes exact EVs by k, Monte Carlo EVs, and optimal-policy averages.
- Runs an optimal-policy Monte Carlo to estimate average net and per-k averages.
- Prints net occurrence counts and the full theoretical outcome distribution for the optimal policy.
- Plots bar charts for expected/average net by k and the optimal-policy net payout distribution.

## Key Components
- `simulation/ExactCalculator`: Closed-form hypergeometric expected value and net distributions for a given k.
- `simulation/MonteCarlo`: Fixed-k Monte Carlo simulators and net occurrence counters.
- `simulation/OptimalPolicy`: Dynamic program policy that chooses reveal vs. claim; includes the flat penalty if all goods were revealed.
- `simulation/OptimalPlayerSimulator`: Plays games following the optimal policy to estimate empirical averages and net counts.
- `simulation/OutcomeDistribution`: Exact probability map of final net payouts under the optimal policy (all reveal paths + claim distribution).
- `simulation/Plotter`: XChart-based bar charts for distributions and EV-by-k.
- `ledprobabilitymodel/Main`: Wires everything together for a full run/demo.

## Arduino / Trellis M4 firmware
- Firmware lives in `src/game/buttonsoffortune.ino` and builds with PlatformIO (VS Code extension or CLI).
- Board: `adafruit_trellis_m4`, Framework: Arduino, Libraries: `Adafruit NeoTrellis M4 Library` and `Adafruit NeoPixel` (declared in `platformio.ini` and auto-installed).
- Build from repo root: `pio run -e trellis_m4`
- Flash (USB connected): `pio run -e trellis_m4 -t upload`
- Optional serial monitor: `pio device monitor -e trellis_m4 -b 115200`
- If IntelliSense is stale, run “PlatformIO: Rebuild IntelliSense Index”.

## Variables (game terms)
- `N`: total LEDs on the board.
- `K`: number of good LEDs on the board (total goods).
- `k`: number of LEDs a player claims in a single claim action.
- `g`: number of good LEDs already revealed (can’t be claimed anymore).
- `N0`, `K0`: starting values of `N` and `K` used by the optimal policy/DP.
- `K_MAX`: maximum allowed claim size (cap on k).
- `REVEAL_PENALTY`: flat token penalty applied only if all good LEDs have been revealed before claiming.

## Adjusting Parameters
In `Main.java`, you can tweak:
- `N`, `K`, and `trials` for problem size and Monte Carlo iterations.
- Which k to plot in the quick distribution example.

The optimal-policy logic uses constants in `OptimalPolicy`:
- `N0`, `K0`, `K_MAX`, and `REVEAL_PENALTY` (penalty applied only if all goods were revealed).

## Notes
- Charts require a desktop environment (Swing). If you’re running headless, you can comment out the plotting calls in `Main`.
- If you see a “command not found: mvn” error, install Maven or run via your IDE’s “Run Main” action.
