#include "Adafruit_NeoTrellisM4.h"

Adafruit_NeoTrellisM4 trellis = Adafruit_NeoTrellisM4();

// --- Tile types ---
const uint8_t TILE_SPECIAL = 0;
const uint8_t TILE_GOOD    = 1;
const uint8_t TILE_BAD     = 2;

// --- Tile phases ---
const uint8_t PHASE_HIDDEN    = 0; // untouched
const uint8_t PHASE_BLINKING  = 1; // first press, blinking blue
const uint8_t PHASE_REVEALING = 2; // second press, waiting 1s before reveal
const uint8_t PHASE_REVEALED  = 3; // showing state color

// --- Special buttons ---
const int RESET_KEY = 0;  // top-left corner
const int MODE_KEY  = 7;  // top-right corner

// --- Timings ---
const unsigned long BLINK_INTERVAL   = 300; // ms
const unsigned long REVEAL_DELAY     = 1000; // ms (LED stays off before reveal)
const unsigned long RESET_TIMEOUT    = 8000; // ms to cancel reset blinking
const unsigned long AUTO_RESET_DELAY = 800;  // ms to pause before auto reset after 3 goods revealed

// --- Colors ---
uint32_t COLOR_RESET_SOLID;
uint32_t COLOR_RESET_BLINK;
uint32_t COLOR_MODE_SOLID;
uint32_t COLOR_MODE_BLINK;
const uint32_t COLOR_GOOD  = trellis.Color(0, 255, 0);
const uint32_t COLOR_BAD   = trellis.Color(255, 0, 0);
const uint32_t COLOR_BLUE  = trellis.Color(0, 0, 255);
const uint32_t COLOR_OFF   = 0;

// --- Game state containers ---
uint8_t       *tile_state;     // good / bad / special
uint8_t       *tile_phase;     // hidden / blinking / revealing / revealed
bool          *blink_visible;  // visibility flag for blinking entries
unsigned long *last_blink_time;
unsigned long *reveal_start_time;
bool          *highlighted;    // pressed while mode blinking
bool pixelsDirty = false;      // track pending LED changes

uint8_t resetStage = 0; // 0 solid, 1 blinking, 2 revealed waiting for reset
unsigned long resetStageStart = 0;
bool modeBlinking = false;
bool autoResetPending = false;
unsigned long autoResetTime = 0;
uint8_t revealedGoodCount = 0;

void assignTileStates();
void resetGame();
void revealTile(int key);
void handleGameKeyPress(int key);
void handleResetKeyPress();
void handleModeKeyPress();
void revealAllTiles();
bool isPlayableKey(int key);
void updateBlinking();
void updateRevealDelays();
void updateResetTimeout();
void checkAutoReset();
void paintKey(int key, uint32_t color);
void flushPixels();

void setup() {
  Serial.begin(115200);
  trellis.begin();
  trellis.setBrightness(80);

  Serial.println("Pseudo Minesweeper keypad");

  int n = trellis.num_keys();

  COLOR_RESET_SOLID = trellis.Color(255, 165, 0); // orange
  COLOR_RESET_BLINK = trellis.Color(255, 215, 0); // bright yellow
  COLOR_MODE_SOLID  = trellis.Color(0, 255, 255); // cyan
  COLOR_MODE_BLINK  = trellis.Color(0, 120, 255); // blue-ish

  tile_state        = new uint8_t[n];
  tile_phase        = new uint8_t[n];
  blink_visible     = new bool[n];
  last_blink_time   = new unsigned long[n];
  reveal_start_time = new unsigned long[n];
  highlighted       = new bool[n];

  for (int i = 0; i < n; i++) {
    tile_state[i] = TILE_SPECIAL;
    tile_phase[i] = PHASE_HIDDEN;
    blink_visible[i] = false;
    highlighted[i] = false;
    last_blink_time[i] = 0;
    reveal_start_time[i] = 0;
    paintKey(i, COLOR_OFF);
  }

  resetGame();
}

void loop() {
  trellis.tick();

  while (trellis.available()) {
    keypadEvent e = trellis.read();
    if (e.bit.EVENT == KEY_JUST_PRESSED) {
      int key = e.bit.KEY;

      if (key == RESET_KEY) {
        handleResetKeyPress();
      } else if (key == MODE_KEY) {
        handleModeKeyPress();
      } else {
        handleGameKeyPress(key);
      }
    }
  }

  updateBlinking();
  updateRevealDelays();
  updateResetTimeout();
  checkAutoReset();
  flushPixels();
  delay(5);
}

void resetGame() {
  int n = trellis.num_keys();
  revealedGoodCount = 0;
  autoResetPending = false;

  for (int i = 0; i < n; i++) {
    tile_phase[i] = PHASE_HIDDEN;
    blink_visible[i] = false;
    highlighted[i] = false;
    last_blink_time[i] = 0;
    reveal_start_time[i] = 0;
    if (i != RESET_KEY && i != MODE_KEY) {
      paintKey(i, COLOR_OFF);
    }
  }

  assignTileStates();

  // Reset button to solid
  resetStage = 0;
  resetStageStart = 0;
  paintKey(RESET_KEY, COLOR_RESET_SOLID);

  // Mode button solid (not blinking)
  modeBlinking = false;
  blink_visible[MODE_KEY] = true;
  paintKey(MODE_KEY, COLOR_MODE_SOLID);

  flushPixels();
}

void assignTileStates() {
  int n = trellis.num_keys();

  for (int i = 0; i < n; i++) {
    if (i == RESET_KEY || i == MODE_KEY) {
      tile_state[i] = TILE_SPECIAL;
    } else {
      tile_state[i] = TILE_BAD;
    }
  }

  // Assign three unique GOOD tiles
  int assigned = 0;
  while (assigned < 3) {
    int idx = random(n);
    if (!isPlayableKey(idx)) {
      continue;
    }
    if (tile_state[idx] != TILE_GOOD) {
      tile_state[idx] = TILE_GOOD;
      assigned++;
    }
  }
}

bool isPlayableKey(int key) {
  return key != RESET_KEY && key != MODE_KEY;
}

void handleGameKeyPress(int key) {
  if (!isPlayableKey(key)) {
    return;
  }

  // If mode button is blinking, use highlight-only behavior
  if (modeBlinking) {
    if (tile_phase[key] == PHASE_REVEALED || tile_phase[key] == PHASE_REVEALING) {
      return;
    }
    // Toggle highlight on/off while in highlight mode
    highlighted[key] = !highlighted[key];

    if (highlighted[key]) {
      paintKey(key, COLOR_BLUE);
    } else {
      if (tile_phase[key] == PHASE_BLINKING) {
        // Restore blinking visibility if the tile was mid-blink when unhighlighted
        paintKey(key, blink_visible[key] ? COLOR_BLUE : COLOR_OFF);
      } else {
        paintKey(key, COLOR_OFF);
      }
    }
    return;
  }

  if (tile_phase[key] == PHASE_HIDDEN) {
    // First press -> start blinking blue
    tile_phase[key] = PHASE_BLINKING;
    blink_visible[key] = true;
    last_blink_time[key] = millis();
    paintKey(key, COLOR_BLUE);
    highlighted[key] = false;
  } else if (tile_phase[key] == PHASE_BLINKING) {
    // Second press -> turn off for REVEAL_DELAY and then show actual color
    tile_phase[key] = PHASE_REVEALING;
    reveal_start_time[key] = millis();
    paintKey(key, COLOR_OFF);
  }
}

void handleResetKeyPress() {
  if (resetStage == 0) {
    resetStage = 1;
    resetStageStart = millis();
    blink_visible[RESET_KEY] = true;
    last_blink_time[RESET_KEY] = millis();
    paintKey(RESET_KEY, COLOR_RESET_BLINK);
  } else if (resetStage == 1) {
    resetStage = 2;
    paintKey(RESET_KEY, COLOR_RESET_SOLID);
    revealAllTiles();
  } else if (resetStage == 2) {
    resetGame();
  }
}

void handleModeKeyPress() {
  if (!modeBlinking) {
    modeBlinking = true;
    blink_visible[MODE_KEY] = true;
    last_blink_time[MODE_KEY] = millis();
    paintKey(MODE_KEY, COLOR_MODE_BLINK);
  } else {
    modeBlinking = false;
    paintKey(MODE_KEY, COLOR_MODE_SOLID);
  }
}

void revealTile(int key) {
  if (!isPlayableKey(key)) {
    return;
  }

  tile_phase[key] = PHASE_REVEALED;
  highlighted[key] = false;

  if (tile_state[key] == TILE_GOOD) {
    paintKey(key, COLOR_GOOD);
    if (revealedGoodCount < 255) {
      revealedGoodCount++;
    }
  } else {
    paintKey(key, COLOR_BAD);
  }

  if (revealedGoodCount >= 3 && !autoResetPending) {
    autoResetPending = true;
    autoResetTime = millis() + AUTO_RESET_DELAY;
  }
}

void revealAllTiles() {
  int n = trellis.num_keys();
  for (int i = 0; i < n; i++) {
    if (!isPlayableKey(i)) {
      continue;
    }
    tile_phase[i] = PHASE_REVEALED;
    highlighted[i] = false;
    if (tile_state[i] == TILE_GOOD) {
      paintKey(i, COLOR_GOOD);
    } else {
      paintKey(i, COLOR_BAD);
    }
  }
}

void updateBlinking() {
  unsigned long now = millis();
  int n = trellis.num_keys();

  for (int i = 0; i < n; i++) {
    bool shouldBlink = false;
    uint32_t onColor = COLOR_OFF;

    if (tile_phase[i] == PHASE_BLINKING) {
      shouldBlink = true;
      onColor = COLOR_BLUE;
    } else if (i == RESET_KEY && resetStage == 1) {
      shouldBlink = true;
      onColor = COLOR_RESET_BLINK;
    } else if (i == MODE_KEY && modeBlinking) {
      shouldBlink = true;
      onColor = COLOR_MODE_BLINK;
    }

    if (shouldBlink) {
      if (now - last_blink_time[i] >= BLINK_INTERVAL) {
        last_blink_time[i] = now;
        blink_visible[i] = !blink_visible[i];
      }
      paintKey(i, blink_visible[i] ? onColor : COLOR_OFF);
    }
  }
}

void updateRevealDelays() {
  unsigned long now = millis();
  int n = trellis.num_keys();
  for (int i = 0; i < n; i++) {
    if (tile_phase[i] == PHASE_REVEALING) {
      if (now - reveal_start_time[i] >= REVEAL_DELAY) {
        revealTile(i);
      }
    }
  }
}

void updateResetTimeout() {
  if (resetStage == 1 && (millis() - resetStageStart) >= RESET_TIMEOUT) {
    resetStage = 0;
    paintKey(RESET_KEY, COLOR_RESET_SOLID);
  }
}

void checkAutoReset() {
  if (autoResetPending && millis() >= autoResetTime) {
    resetGame();
  }
}

void paintKey(int key, uint32_t color) {
  trellis.setPixelColor(key, color);
  pixelsDirty = true;
}

void flushPixels() {
  if (pixelsDirty) {
    trellis.show();
    pixelsDirty = false;
  }
}

// Input a value 0 to 255 to get a color value.
// The colours are a transition r - g - b - back to r.
uint32_t Wheel(byte WheelPos) {
  WheelPos = 255 - WheelPos;
  if (WheelPos < 85) {
    return trellis.Color(255 - WheelPos * 3, 0, WheelPos * 3);
  }
  if (WheelPos < 170) {
    WheelPos -= 85;
    return trellis.Color(0, WheelPos * 3, 255 - WheelPos * 3);
  }
  WheelPos -= 170;
  return trellis.Color(WheelPos * 3, 255 - WheelPos * 3, 0);
}

