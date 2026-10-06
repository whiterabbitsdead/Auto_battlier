import re

with open('app/src/main/java/com/example/game/GameViewModel.kt', 'r') as f:
    content = f.read()

# Add poolManager
if 'private val poolManager' not in content:
    content = content.replace("class GameViewModel : ViewModel() {", "class GameViewModel : ViewModel() {\n    private val poolManager = FighterPool()\n")

# Remove initGlobalPool
content = re.sub(r'\n    private fun initGlobalPool\(\): Map<String, Int> \{.*?    \}\n', '\n', content, flags=re.DOTALL)

# Remove processMerges
content = re.sub(r'\n    private fun processMerges\(.*?    \}\n', '\n', content, flags=re.DOTALL)

# Remove rollShop
content = re.sub(r'\n    private fun rollShop\(.*?    \}\n', '\n', content, flags=re.DOTALL)

# Replace in startBattle / resetGame / selectCommander / endBattle
content = content.replace('rollShop(3, initialPool)', 'poolManager.rollShop(3)')
content = content.replace('rollShop(newLevel, state.globalPool)', 'poolManager.rollShop(newLevel)')
content = content.replace('rollShop(it.playerLevel, it.globalPool)', 'poolManager.rollShop(it.playerLevel)')

content = content.replace('val initialPool = initGlobalPool()', 'poolManager.resetPool()')
content = content.replace('globalPool = initialPool,', '')

content = content.replace('processMerges(playerFighters, commander, emptyList())', 'poolManager.processMerges(playerFighters, emptyList()) { applyCommanderBuffs(it, commander) }')
content = content.replace('processMerges(state.playerFighters + buffed, state.playerCommander, state.playerInventory)', 'poolManager.processMerges(state.playerFighters + buffed, state.playerInventory) { applyCommanderBuffs(it, state.playerCommander) }')

# In resetGame
if 'poolManager.resetPool()' not in content.split('fun resetGame() {')[1]:
    content = content.replace('fun resetGame() {', 'fun resetGame() {\n        poolManager.resetPool()')

# Buying
buy_pool_regex = r'                val updatedPool = state\.globalPool\.toMutableMap\(\)\n                updatedPool\[fighter\.name\] = maxOf\(0, updatedPool\.getOrDefault\(fighter\.name, 0\) - 1\)\n                \n                _uiState\.update \{ updateSynergies\(it\.copy\(\n                    playerGold = it\.playerGold - fighter\.cost,\n                    playerFighters = newFighters,\n                    playerInventory = newInventory,\n                    globalPool = updatedPool,\n                    shopFighters = it\.shopFighters\.filter \{ sf -> sf\.id != fighter\.id \}\n                \)\) \}'
new_buy = """                poolManager.buyUnit(fighter)
                _uiState.update { updateSynergies(it.copy(
                    playerGold = it.playerGold - fighter.cost,
                    playerFighters = newFighters,
                    playerInventory = newInventory,
                    shopFighters = it.shopFighters.filter { sf -> sf.id != fighter.id }
                )) }"""
content = re.sub(buy_pool_regex, new_buy, content)

# Selling
sell_pool_regex = r'        val updatedPool = state\.globalPool\.toMutableMap\(\)\n        updatedPool\[fighterToSell\.name\] = updatedPool\.getOrDefault\(fighterToSell\.name, 0\) \+ multiplier\n        \n        val newFighters = state\.playerFighters\.filterNot \{ it\.id == selectedId \}\n        val newInventory = state\.playerInventory \+ fighterToSell\.equipment\n        \n        _uiState\.update \{ updateSynergies\(it\.copy\(\n            playerFighters = newFighters,\n            playerGold = it\.playerGold \+ refund,\n            playerInventory = newInventory,\n            globalPool = updatedPool,\n            selectedFighterId = null\n        \)\) \}'
new_sell = """        poolManager.sellUnit(fighterToSell)
        
        val newFighters = state.playerFighters.filterNot { it.id == selectedId }
        val newInventory = state.playerInventory + fighterToSell.equipment
        
        _uiState.update { updateSynergies(it.copy(
            playerFighters = newFighters,
            playerGold = it.playerGold + refund,
            playerInventory = newInventory,
            selectedFighterId = null
        )) }"""
content = re.sub(sell_pool_regex, new_sell, content)

with open('app/src/main/java/com/example/game/GameViewModel.kt', 'w') as f:
    f.write(content)

