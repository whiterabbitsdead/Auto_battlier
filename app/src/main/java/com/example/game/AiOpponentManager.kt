package com.example.game

import java.util.UUID

object AiOpponentManager {

    fun createInitialAiPlayers(
        playerCommander: Commander,
        poolManager: FighterPool
    ): List<AiPlayer> {
        // Take 3 distinct commanders from the available 20 commanders
        val otherCommanders = availableCommanders.filter { it.name != playerCommander.name }.shuffled().take(3)
        
        return otherCommanders.mapIndexed { index, commander ->
            val strategyName = when (commander.preferredClass) {
                FighterClass.KNUCKLE_POWER, FighterClass.TANK -> "Vanguard Brawlers"
                FighterClass.HUNTERS, FighterClass.MARKSMAN -> "Deadly Sharpshooters"
                FighterClass.ILLUMINS, FighterClass.MAGE -> "Mystic Conclave"
                FighterClass.CONVICT, FighterClass.ASSASSIN -> "Outlaw Syndicate"
                else -> "Balanced Army"
            }

            // Generate 3 initial cost-1 units favoring their preferred faction or class
            val favoredTemplates = fighterPool.filter { it.cost == 1 && (it.faction == commander.preferredFaction || it.cls == commander.preferredClass) }
            val fallbackTemplates = fighterPool.filter { it.cost == 1 }

            val initialFighters = (0..2).map { slot ->
                val template = if (favoredTemplates.isNotEmpty() && (slot < 2 || Math.random() < 0.7)) {
                    favoredTemplates.random()
                } else {
                    fallbackTemplates.random()
                }
                
                // Position on AI board side (Y: 0..3)
                val targetY = when (template.cls) {
                    FighterClass.KNUCKLE_POWER, FighterClass.TANK -> 3
                    FighterClass.CONVICT -> 2
                    FighterClass.HUNTERS, FighterClass.ASSASSIN -> 1
                    FighterClass.ILLUMINS, FighterClass.MAGE -> 0
                    else -> 1
                }
                val targetX = 1 + slot * 2
                
                instantiateFighter(template, isPlayer = false, yPos = targetY).copy(
                    id = UUID.randomUUID().toString(),
                    x = targetX,
                    startX = targetX,
                    y = targetY,
                    startY = targetY
                )
            }

            // Apply commander buffs
            val buffedFighters = applyCommanderBuffsToAi(initialFighters, commander)

            AiPlayer(
                id = UUID.randomUUID().toString(),
                name = commander.title.ifEmpty { commander.name.substringBefore(" (") },
                commander = commander,
                hp = 100,
                level = 3,
                exp = 0,
                gold = 10,
                winStreak = 0,
                loseStreak = 0,
                boardFighters = buffedFighters,
                benchFighters = emptyList(),
                preferredFaction = commander.preferredFaction,
                preferredClass = commander.preferredClass,
                strategyName = strategyName
            )
        }
    }

    fun applyCommanderBuffsToAi(fighters: List<Fighter>, commander: Commander): List<Fighter> {
        return fighters.map { f ->
            val newF = f.copy()
            when (commander.preferredClass) {
                FighterClass.KNUCKLE_POWER -> {
                    newF.physAttack += 8
                    newF.maxHp += 40
                    newF.hp += 40
                }
                FighterClass.HUNTERS -> {
                    newF.attackRange = maxOf(newF.attackRange, 2)
                    newF.critChance += 0.15
                    newF.accuracy += 15
                }
                FighterClass.ILLUMINS -> {
                    newF.magAttack += 15
                    newF.magDefense += 10
                }
                FighterClass.CONVICT -> {
                    newF.physDefense += 12
                    newF.dodgeChance += 0.10
                }
                else -> {
                    newF.physAttack += 5
                    newF.physDefense += 5
                }
            }
            // Synergy resonance if matching commander faction
            if (f.faction == commander.preferredFaction) {
                newF.physAttack += 5
                newF.magAttack += 5
                newF.maxHp += 30
                newF.hp += 30
            }
            newF
        }
    }

    /**
     * AI takes its turn: collects income, levels up if prudent, rolls shop, buys synergy/duplicate units,
     * merges 3-stars, positions board, and equips items.
     */
    fun simulateAiTurn(
        ai: AiPlayer,
        round: Int,
        poolManager: FighterPool
    ): AiPlayer {
        if (!ai.isAlive) return ai

        var currentGold = ai.gold
        var currentLevel = ai.level
        var currentExp = ai.exp
        var fighters = (ai.boardFighters + ai.benchFighters).toMutableList()
        var inventory = ai.inventory.toMutableList()

        // 1. Income & Interest
        val baseGold = 5
        val interest = minOf(5, currentGold / 10)
        val streakBonus = when {
            ai.winStreak >= 5 || ai.loseStreak >= 5 -> 3
            ai.winStreak >= 3 || ai.loseStreak >= 3 -> 2
            ai.winStreak >= 2 || ai.loseStreak >= 2 -> 1
            else -> 0
        }
        currentGold += baseGold + interest + streakBonus

        // Passive EXP
        val (lvlAfterExp, expAfterExp) = CommanderLeveling.addPassiveExp(currentLevel, currentExp, 2)
        currentLevel = lvlAfterExp
        currentExp = expAfterExp

        // 2. Level Up Decision
        val costToLevel = CommanderLeveling.getCostToLevelUp(currentLevel, currentExp)
        if (currentLevel < CommanderLeveling.MAX_LEVEL && costToLevel > 0) {
            val shouldLevel = when {
                // Keep interest economy if possible, or aggressively level at key rounds
                currentGold - costToLevel >= 10 -> true
                round >= 2 && currentLevel < 4 && currentGold >= costToLevel -> true
                round >= 4 && currentLevel < 5 && currentGold >= costToLevel -> true
                round >= 6 && currentLevel < 6 && currentGold >= costToLevel -> true
                round >= 8 && currentLevel < 7 && currentGold >= costToLevel -> true
                else -> false
            }

            if (shouldLevel) {
                currentGold -= costToLevel
                currentLevel++
                currentExp = 0
            }
        }

        // 3. Shop & Buying Phase
        // AI rolls 1 shop (and potentially 1 reroll if rich > 20 gold)
        val rolls = if (currentGold > 20) 2 else 1
        for (r in 0 until rolls) {
            if (r > 0) {
                if (currentGold >= 2) {
                    currentGold -= 2
                } else {
                    break
                }
            }

            val shop = poolManager.rollShop(currentLevel)
            
            // Score cards for AI
            val scored = shop.map { card ->
                var score = 0
                // Check if card helps 3-merge
                val sameCopies = fighters.count { it.name == card.name && it.starLevel == 1 }
                if (sameCopies == 2) {
                    score += 150 // Immediate 2-star upgrade!
                } else if (sameCopies == 1) {
                    score += 50 // Halfway to 2-star
                }

                val twoStarCopies = fighters.count { it.name == card.name && it.starLevel == 2 }
                if (twoStarCopies == 2) {
                    score += 200 // Immediate 3-star upgrade!
                }

                if (card.faction == ai.preferredFaction) score += 35
                if (card.fighterClass == ai.preferredClass) score += 30
                score += card.cost * 10

                card to score
            }.sortedByDescending { it.second }

            for ((card, score) in scored) {
                if (score >= 30 && currentGold >= card.cost && fighters.size < 16) {
                    currentGold -= card.cost
                    poolManager.buyUnit(card)
                    
                    val newFighter = card.copy(
                        id = UUID.randomUUID().toString(),
                        isPlayer = false,
                        x = 0, y = -1, startX = 0, startY = -1
                    )
                    val buffed = applyCommanderBuffsToAi(listOf(newFighter), ai.commander).first()
                    fighters.add(buffed)
                }
            }
        }

        // 4. Process Merges
        val (newFighters, newInventory, _) = poolManager.processMerges(fighters, inventory, emptyList()) {
            applyCommanderBuffsToAi(it, ai.commander)
        }
        fighters = newFighters.toMutableList()
        inventory = newInventory.toMutableList()

        // 5. Board Placement Strategy
        // Sort to find best fighters for board: 3★ > 2★ > synergy matches > 1★
        val rankedFighters = fighters.sortedWith(
            compareByDescending<Fighter> { it.starLevel }
                .thenByDescending { if (it.faction == ai.preferredFaction || it.fighterClass == ai.preferredClass) 1 else 0 }
                .thenByDescending { it.cost }
                .thenByDescending { it.physAttack + it.magAttack + it.hp / 10 }
        )

        val boardUnits = rankedFighters.take(currentLevel)
        val benchUnits = rankedFighters.drop(currentLevel)

        // Assign board coordinates (Y: 0..3, X: 0..7)
        val finalBoard = mutableListOf<Fighter>()
        val frontliners = boardUnits.filter { it.fighterClass == FighterClass.KNUCKLE_POWER || it.fighterClass == FighterClass.TANK }
        val midliners = boardUnits.filter { it.fighterClass == FighterClass.CONVICT }
        val flankers = boardUnits.filter { it.fighterClass == FighterClass.HUNTERS || it.fighterClass == FighterClass.ASSASSIN }
        val backline = boardUnits.filter { it !in frontliners && it !in midliners && it !in flankers }

        var tankX = 1
        for (tank in frontliners) {
            finalBoard.add(tank.copy(
                x = tankX.coerceIn(0, 7), startX = tankX.coerceIn(0, 7),
                y = 3, startY = 3
            ))
            tankX += 2
        }

        var convictX = 0
        for (convict in midliners) {
            finalBoard.add(convict.copy(
                x = convictX.coerceIn(0, 7), startX = convictX.coerceIn(0, 7),
                y = 2, startY = 2
            ))
            convictX += 2
        }

        var assassinX = 0
        for (assassin in flankers) {
            val posX = if (assassinX % 2 == 0) (0 + assassinX / 2).coerceIn(0, 7) else (7 - assassinX / 2).coerceIn(0, 7)
            finalBoard.add(assassin.copy(
                x = posX, startX = posX,
                y = 1, startY = 1
            ))
            assassinX++
        }

        var backlineX = 1
        for (unit in backline) {
            finalBoard.add(unit.copy(
                x = backlineX.coerceIn(0, 7), startX = backlineX.coerceIn(0, 7),
                y = 0, startY = 0
            ))
            backlineX += 2
        }

        // Remaining bench units
        var finalBench = benchUnits.map { it.copy(x = 0, startX = 0, y = -1, startY = -1) }.toMutableList()

        // Sell bench overflow if more than 8
        if (finalBench.size > 8) {
            val toSell = finalBench.drop(8)
            finalBench = finalBench.take(8).toMutableList()
            for (sold in toSell) {
                poolManager.sellUnit(sold)
                currentGold += sold.cost * (if (sold.starLevel == 2) 3 else 1)
            }
        }

        // 6. Equip items to best board units
        if (inventory.isNotEmpty() && finalBoard.isNotEmpty()) {
            for (item in inventory.toList()) {
                val recipient = finalBoard.firstOrNull { it.equipment.size < 3 }
                if (recipient != null) {
                    val idx = finalBoard.indexOfFirst { it.id == recipient.id }
                    if (idx != -1) {
                        var pAtk = recipient.physAttack
                        var pDef = recipient.physDefense
                        var mHp = recipient.maxHp
                        var hp = recipient.hp
                        var dodge = recipient.dodgeChance
                        when (item.type) {
                            EquipmentType.ATTACK -> pAtk += 10
                            EquipmentType.DEFENSE -> pDef += 10
                            EquipmentType.HP -> { mHp += 50; hp += 50 }
                            EquipmentType.DODGE -> dodge += 0.10
                        }
                        finalBoard[idx] = recipient.copy(
                            equipment = recipient.equipment + item,
                            physAttack = pAtk,
                            physDefense = pDef,
                            maxHp = mHp,
                            hp = hp,
                            dodgeChance = dodge
                        )
                        inventory.remove(item)
                    }
                }
            }
        }

        return ai.copy(
            gold = currentGold,
            level = currentLevel,
            exp = currentExp,
            boardFighters = finalBoard,
            benchFighters = finalBench,
            inventory = inventory
        )
    }

    /**
     * Simulates a battle between two AI players and returns updated states plus a combat summary.
     */
    fun simulateAiVsAiBattle(
        ai1: AiPlayer,
        ai2: AiPlayer,
        round: Int
    ): Triple<AiPlayer, AiPlayer, String> {
        if (!ai1.isAlive && !ai2.isAlive) {
            return Triple(ai1, ai2, "")
        }
        if (!ai1.isAlive) {
            return Triple(ai1, ai2, "")
        }
        if (!ai2.isAlive) {
            return Triple(ai1, ai2, "")
        }

        val power1 = calculateArmyPower(ai1.boardFighters) * (0.9 + Math.random() * 0.2)
        val power2 = calculateArmyPower(ai2.boardFighters) * (0.9 + Math.random() * 0.2)

        val baseDamage = when (round) {
            1 -> 3
            2 -> 5
            3 -> 7
            else -> 10
        }

        return if (power1 >= power2) {
            val survivingStars = maxOf(1, ai1.boardFighters.sumOf { it.starLevel } / 2)
            val totalDamage = baseDamage + survivingStars
            val newHp2 = maxOf(0, ai2.hp - totalDamage)
            val updatedAi1 = ai1.copy(winStreak = ai1.winStreak + 1, loseStreak = 0, gold = ai1.gold + 1)
            val updatedAi2 = ai2.copy(hp = newHp2, loseStreak = ai2.loseStreak + 1, winStreak = 0)
            val elimText = if (newHp2 == 0) " 💀 ${ai2.name} was ELIMINATED!" else ""
            val log = "⚔️ Matchup: ${ai1.name} defeated ${ai2.name} (-$totalDamage HP)!$elimText"
            Triple(updatedAi1, updatedAi2, log)
        } else {
            val survivingStars = maxOf(1, ai2.boardFighters.sumOf { it.starLevel } / 2)
            val totalDamage = baseDamage + survivingStars
            val newHp1 = maxOf(0, ai1.hp - totalDamage)
            val updatedAi2 = ai2.copy(winStreak = ai2.winStreak + 1, loseStreak = 0, gold = ai2.gold + 1)
            val updatedAi1 = ai1.copy(hp = newHp1, loseStreak = ai1.loseStreak + 1, winStreak = 0)
            val elimText = if (newHp1 == 0) " 💀 ${ai1.name} was ELIMINATED!" else ""
            val log = "⚔️ Matchup: ${ai2.name} defeated ${ai1.name} (-$totalDamage HP)!$elimText"
            Triple(updatedAi1, updatedAi2, log)
        }
    }

    private fun calculateArmyPower(fighters: List<Fighter>): Double {
        if (fighters.isEmpty()) return 10.0
        return fighters.sumOf { f ->
            (f.hp / 10.0) + (f.physAttack * 1.5) + (f.magAttack * 1.5) + f.physDefense + f.magDefense + (f.starLevel * 30.0)
        }
    }
}
