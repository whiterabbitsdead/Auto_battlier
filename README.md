# Auto Battlier

Android (Kotlin / Jetpack Compose) auto-battler.

## Mock Battle mode
From the first screen tap **Mock Battle**. Pick any number of each fighter for the
Left and Right armies (any size, e.g. 1 Chief Kong vs 3 Mike Tysons), create your own
fighters (HP/ATK/DEF/SPD/range), then **Fight!** for a play-by-play or **Odds** to run
100 battles and see win rates. Engine: `game/MockBattle.kt`, UI: `game/MockBattleScreen.kt`.

## Build / test
`./gradlew assembleDebug` and `./gradlew testDebugUnitTest --tests com.example.MockBattleTest`
