# Original physical game

The school project used an Adafruit NeoTrellis M4 with a 32-button grid. The reset and mode buttons occupy two positions, leaving the 30 playable tiles used in the default Java study.

The original sketch is preserved at [trellis-m4/buttonsoffortune.ino](trellis-m4/buttonsoffortune.ino). This toolkit refactor does not change the sketch's behavior and does not claim new hardware testing.

The board assigns three good tiles. A two-stage press reveals a tile, mode toggles highlights, reset reveals/restarts the board, and revealing all three good tiles schedules a reset. The Java model studies the abstract claim and payout rules. Payout accounting is not implemented by this sketch, and the toolkit does not communicate with the board.

## Build and upload

Requires PlatformIO and the physical board for upload. From the repository root:

```sh
pio run -e trellis_m4
pio run -e trellis_m4 -t upload
pio device monitor -e trellis_m4 -b 115200
```

The original PlatformIO environment and Adafruit libraries remain in platformio.ini. Hardware dependencies are independent of the Java/Maven analysis.

