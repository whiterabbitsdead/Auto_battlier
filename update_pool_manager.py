with open('app/src/main/java/com/example/game/FighterPool.kt', 'r') as f:
    content = f.read()

old_process = """fun processMerges(fighters: List<Fighter>, currentInventory: List<Equipment>): Pair<List<Fighter>, List<Equipment>> {"""
new_process = """fun processMerges(fighters: List<Fighter>, currentInventory: List<Equipment>, applyBuffs: (List<Fighter>) -> List<Fighter>): Pair<List<Fighter>, List<Equipment>> {"""
content = content.replace(old_process, new_process)

old_add = """                    val newFighter = newFighterBase.copy(
                        id = UUID.randomUUID().toString(),
                        x = targetPos.startX, startX = targetPos.startX,
                        y = targetPos.startY, startY = targetPos.startY
                    )
                    
                    currentFighters = currentFighters + newFighter"""

new_add = """                    val newFighter = newFighterBase.copy(
                        id = UUID.randomUUID().toString(),
                        x = targetPos.startX, startX = targetPos.startX,
                        y = targetPos.startY, startY = targetPos.startY
                    )
                    
                    val buffed = applyBuffs(listOf(newFighter)).first()
                    currentFighters = currentFighters + buffed"""
content = content.replace(old_add, new_add)

with open('app/src/main/java/com/example/game/FighterPool.kt', 'w') as f:
    f.write(content)
