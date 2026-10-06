import re

with open('app/src/main/java/com/example/game/GameScreens.kt', 'r') as f:
    content = f.read()

# Modify the sell UI to overlay on top, or just keep both visible.
# Currently the Sell UI is visible when selectedFighterId != null.
# Shop is now visible always between battles.

old_sell_ui = """        if (!state.isBattleActive && state.battleResult == null && state.selectedFighterId != null) {
            val selectedFighter = state.playerFighters.find { it.id == state.selectedFighterId }"""

new_sell_ui = """        if (!state.isBattleActive && state.battleResult == null && state.selectedFighterId != null) {
            val selectedFighter = state.playerFighters.find { it.id == state.selectedFighterId }"""

# They are in a Column so they should just stack vertically. I just need to make sure the Shop is explicitly rendering correctly.
