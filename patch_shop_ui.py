import re

with open('app/src/main/java/com/example/game/GameScreens.kt', 'r') as f:
    content = f.read()

old_shop_ui = """        // Shop UI
        if (!state.isBattleActive && state.battleResult == null && state.selectedFighterId == null) {"""

new_shop_ui = """        // Shop UI
        if (!state.isBattleActive && state.battleResult == null) {"""

content = content.replace(old_shop_ui, new_shop_ui)

with open('app/src/main/java/com/example/game/GameScreens.kt', 'w') as f:
    f.write(content)
