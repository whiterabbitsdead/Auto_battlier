import re

with open('app/src/main/java/com/example/game/GameScreens.kt', 'r') as f:
    content = f.read()

# 1. Define ShopComponent
shop_component = """
@Composable
fun ShopComponent(
    state: GameState,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Shop Header & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Shop (Lvl ${state.playerLevel})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                Row {
                    val levelCost = CommanderLeveling.getCostToLevelUp(state.playerLevel, state.playerExp)
                    Button(
                        onClick = { viewModel.buyLevelUp() },
                        enabled = state.playerLevel < CommanderLeveling.MAX_LEVEL && state.playerGold >= levelCost && levelCost > 0,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(if (state.playerLevel < CommanderLeveling.MAX_LEVEL) "Lvl Up ($levelCost G)" else "Max Lvl", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = { viewModel.refreshShop() },
                        enabled = state.playerGold >= 1,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Refresh (1 G)", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = { viewModel.toggleShopLock() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (state.shopLocked) Color.Red else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(if (state.shopLocked) "Unlock" else "Lock", fontSize = 11.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Fighters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                state.shopFighters.forEach { shopFighter ->
                    val benchOccupied = state.playerFighters.count { it.startY == -1 }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .fillMaxHeight()
                            .clickable(enabled = state.playerGold >= shopFighter.cost && benchOccupied < 8) {
                                viewModel.buyFighter(shopFighter)
                            },
                        colors = CardDefaults.cardColors(containerColor = if (shopFighter.cost == 1) Color.LightGray else if (shopFighter.cost == 2) Color(0xFFADD8E6) else Color(0xFFFFD700))
                    ) {
                        Column(
                            modifier = Modifier.padding(2.dp).fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(shopFighter.name.take(6), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("${shopFighter.cost}G - ${shopFighter.fighterClass.name.take(3)}", fontSize = 10.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}
"""

if "fun ShopComponent" not in content:
    content = content + "\n" + shop_component

with open('app/src/main/java/com/example/game/GameScreens.kt', 'w') as f:
    f.write(content)
