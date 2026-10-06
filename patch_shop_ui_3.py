import re

with open('app/src/main/java/com/example/game/GameScreens.kt', 'r') as f:
    content = f.read()

# Make the Shop UI taller and more obvious, maybe add a title

old_shop_ui = """        // Shop UI
        if (!state.isBattleActive && state.battleResult == null) {
            Row("""

new_shop_ui = """        // Shop UI
        if (!state.isBattleActive && state.battleResult == null) {
            Text("Shop", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            Row("""

content = content.replace(old_shop_ui, new_shop_ui)

with open('app/src/main/java/com/example/game/GameScreens.kt', 'w') as f:
    f.write(content)
