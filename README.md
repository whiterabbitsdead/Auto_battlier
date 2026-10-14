# Auto Battlier

Auto Battlier is an Android auto-battler built with Kotlin and Jetpack Compose. It combines a
round-based, commander-driven strategy game with a standalone **Mock Battle** simulator that lets
players pit armies of any size against each other, using their own custom-designed fighters.

## Overview

| Area | Description |
|------|-------------|
| Main game | Pick a commander, build a board of fighters, and watch automated rounds play out. Includes factions, classes, star levels, equipment, synergies, special abilities and commander active skills. |
| Mock Battle | A sandbox for "what-if" fights (for example, one Chief Kong versus three Mike Tysons) with unlimited army sizes and player-created fighters. |
| Persistence | Fighters are stored locally with Room (`data/local`). |

## Architecture

- **UI** – Jetpack Compose screens (`game/GameScreens.kt`, `BattleVisualizationScreen.kt`, `MockBattleScreen.kt`), hosted by `MainActivity` and driven by `GameViewModel` state flows.
- **Domain models** – `game/Models.kt` defines `Fighter`, `Commander`, `BattleState`, factions, classes and abilities as immutable-style data classes.
- **Combat** – `BattleEngine` / `CombatEngine` run the main game's turns; `CombatCalculator` is the shared attack resolver (accuracy, dodge, mitigation, crits, shields, equipment and star scaling).
- **Mock Battle** – `game/MockBattle.kt` is a self-contained simulator that reuses `CombatCalculator` without touching the main game flow.

## Mock Battle: how it works

1. **Fighter templates.** A `FighterTemplate` describes a fighter: HP, attack, defense, speed, range, accuracy, crit and dodge chance, and physical or magic damage. Presets (Chief Kong, Mike Tyson, Footman, Archer, Mage, Knight) are included, and players can create their own in the UI.
2. **Armies of any size.** Each side is a list of `ArmyEntry(template, count)`. Every unit is instantiated as an individual `Fighter`, so 1 versus 3, or 500 versus 500, is handled the same way.
3. **Deployment.** Armies are placed in formation on opposite sides of a grid that widens with the larger army's size.
4. **Round loop.** Each round, living units act in descending speed order. A unit targets its nearest enemy (Chebyshev distance); if the target is within the unit's range it attacks via `CombatCalculator.calculateAttack`, otherwise it steps one tile toward it.
5. **Termination.** The battle ends when one side has no survivors, or after 500 rounds (declared a draw). The result reports the winner, rounds fought, survivors and a play-by-play log.
6. **Odds.** `simulateOdds` repeats the fight many times (the UI uses 100) and reports win/loss/draw counts, which gives a realistic read on randomised combat rather than a single lucky outcome.

Limitations: mock fighters use basic attacks and movement; special abilities, commanders and synergies from the main game are not yet applied.

## Build and test

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew testDebugUnitTest --tests com.example.MockBattleTest
```

Requires the Android SDK and network access to resolve Gradle plugins on first build.
