import re

with open('app/src/main/java/com/example/game/GameScreens.kt', 'r') as f:
    content = f.read()

# 1. Remove old top buttons
top_buttons_regex = r'            val levelCost = CommanderLeveling\.getCostToLevelUp\(state\.playerLevel, state\.playerExp\)\n            \n            Row \{\n                Button\(\n                    onClick = \{ viewModel\.buyLevelUp\(\) \},\n                    enabled = !state\.isBattleActive && state\.playerLevel < CommanderLeveling\.MAX_LEVEL && state\.playerGold >= levelCost && state\.battleResult == null && levelCost > 0\n                \) \{\n                    Text\(if \(state\.playerLevel < CommanderLeveling\.MAX_LEVEL\) "Lvl Up \(\$levelCost G\)" else "Max Lvl", fontSize = 12\.sp\)\n                \}\n                Spacer\(modifier = Modifier\.width\(4\.dp\)\)\n                Button\(\n                    onClick = \{ viewModel\.refreshShop\(\) \},\n                    enabled = !state\.isBattleActive && state\.playerGold >= 1 && state\.battleResult == null\n                \) \{\n                    Text\("Refresh \(1 G\)", fontSize = 12\.sp\)\n                \}\n                Spacer\(modifier = Modifier\.width\(4\.dp\)\)\n                Button\(\n                    onClick = \{ viewModel\.toggleShopLock\(\) \},\n                    enabled = !state\.isBattleActive && state\.battleResult == null,\n                    colors = ButtonDefaults\.buttonColors\(containerColor = if \(state\.shopLocked\) Color\.Red else MaterialTheme\.colorScheme\.primary\)\n                \) \{\n                    Text\(if \(state\.shopLocked\) "Unlock" else "Lock", fontSize = 12\.sp\)\n                \}\n            \}'

content = re.sub(top_buttons_regex, '', content, flags=re.DOTALL)

# 2. Remove old shop UI and insert ShopComponent call
old_shop_ui_regex = r'        // Shop UI\n        if \(!state\.isBattleActive && state\.battleResult == null\) \{\n            Text\("Shop", fontWeight = FontWeight\.Bold, modifier = Modifier\.padding\(start = 8\.dp\)\)\n            Row\(\n                modifier = Modifier\n                    \.fillMaxWidth\(\)\n                    \.height\(60\.dp\),\n                horizontalArrangement = Arrangement\.SpaceEvenly\n            \) \{\n                state\.shopFighters\.forEach \{ shopFighter ->\n                    val benchOccupied = state\.playerFighters\.count \{ it\.startY == -1 \}\n                    Card\(\n                        modifier = Modifier\n                            \.weight\(1f\)\n                            \.padding\(2\.dp\)\n                            \.fillMaxHeight\(\)\n                            \.clickable\(enabled = state\.playerGold >= shopFighter\.cost && benchOccupied < 8\) \{\n                                viewModel\.buyFighter\(shopFighter\)\n                            \},\n                        colors = CardDefaults\.cardColors\(containerColor = if \(shopFighter\.cost == 1\) Color\.LightGray else if \(shopFighter\.cost == 2\) Color\(0xFFADD8E6\) else Color\(0xFFFFD700\)\)\n                    \) \{\n                        Column\(\n                            modifier = Modifier\.padding\(2\.dp\)\.fillMaxSize\(\),\n                            horizontalAlignment = Alignment\.CenterHorizontally,\n                            verticalArrangement = Arrangement\.Center\n                        \) \{\n                            Text\(shopFighter\.name\.take\(6\), fontSize = 10\.sp, fontWeight = FontWeight\.Bold, color = Color\.Black\)\n                            Text\("\$\{shopFighter\.cost\}G - \$\{shopFighter\.fighterClass\.name\.take\(3\)\}", fontSize = 9\.sp, color = Color\.DarkGray\)\n                        \}\n                    \}\n                \}\n            \}\n            Spacer\(modifier = Modifier\.height\(4\.dp\)\)\n        \}'

shop_component_call = """        // Unified Shop UI
        if (!state.isBattleActive && state.battleResult == null) {
            ShopComponent(state = state, viewModel = viewModel)
        }"""

content = re.sub(old_shop_ui_regex, shop_component_call, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/game/GameScreens.kt', 'w') as f:
    f.write(content)

