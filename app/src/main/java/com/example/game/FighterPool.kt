package com.example.game

import java.util.UUID

class FighterPool {
    val poolCounts = mutableMapOf<String, Int>()

    init {
        resetPool()
    }

    fun resetPool() {
        poolCounts.clear()
        fighterPool.forEach {
            val count = when (it.cost) {
                1 -> 45
                2 -> 30
                3 -> 25
                4 -> 15
                5 -> 10
                else -> 10
            }
            poolCounts[it.name] = count
        }
    }

    fun getTierWeights(playerLevel: Int): List<Double> {
        return when (playerLevel) {
            1 -> listOf(1.00, 0.00, 0.00, 0.00, 0.00)
            2 -> listOf(0.80, 0.20, 0.00, 0.00, 0.00)
            3 -> listOf(0.60, 0.30, 0.10, 0.00, 0.00)
            4 -> listOf(0.45, 0.35, 0.18, 0.02, 0.00)
            5 -> listOf(0.35, 0.35, 0.23, 0.07, 0.00)
            6 -> listOf(0.25, 0.35, 0.28, 0.11, 0.01)
            7 -> listOf(0.18, 0.30, 0.34, 0.15, 0.03)
            8 -> listOf(0.14, 0.22, 0.34, 0.24, 0.06)
            9 -> listOf(0.10, 0.15, 0.30, 0.33, 0.12)
            else -> listOf(0.05, 0.10, 0.25, 0.38, 0.22)
        }
    }

    fun getWeights(playerLevel: Int): Triple<Double, Double, Double> {
        // Kept for backward compatibility
        val w = getTierWeights(playerLevel)
        return Triple(w[0], w[1], w[2] + w[3] + w[4])
    }

    fun rollShop(playerLevel: Int): List<Fighter> {
        val weights = getTierWeights(playerLevel)
        val shop = mutableListOf<Fighter>()
        
        for (i in 1..5) {
            val rand = Math.random()
            var cumulative = 0.0
            var targetCost = 1
            for ((idx, w) in weights.withIndex()) {
                cumulative += w
                if (rand < cumulative) {
                    targetCost = idx + 1
                    break
                }
            }
            
            var availableTemplates = fighterPool.filter { it.cost == targetCost && poolCounts.getOrDefault(it.name, 0) > 0 }
            if (availableTemplates.isEmpty()) {
                availableTemplates = fighterPool.filter { poolCounts.getOrDefault(it.name, 0) > 0 }
            }
            if (availableTemplates.isEmpty()) {
                availableTemplates = fighterPool
            }
            
            val totalRemaining = availableTemplates.sumOf { maxOf(1, poolCounts.getOrDefault(it.name, 1)) }
            var r = (0 until totalRemaining).random()
            var selected: FighterTemplate? = null
            for (t in availableTemplates) {
                val count = maxOf(1, poolCounts.getOrDefault(t.name, 1))
                if (r < count) {
                    selected = t
                    break
                }
                r -= count
            }
            if (selected != null) {
                shop.add(instantiateFighter(selected, true, -1).copy(id = UUID.randomUUID().toString()))
            }
        }
        return shop
    }

    fun buyUnit(fighter: Fighter) {
        val currentCount = poolCounts.getOrDefault(fighter.name, 0)
        poolCounts[fighter.name] = maxOf(0, currentCount - 1)
    }

    fun sellUnit(fighter: Fighter) {
        val multiplier = when (fighter.starLevel) {
            3 -> 9
            2 -> 3
            else -> 1
        }
        val currentCount = poolCounts.getOrDefault(fighter.name, 0)
        poolCounts[fighter.name] = currentCount + multiplier
    }

    // Handles merging three 1-stars into a 2-star, and three 2-stars into a 3-star
    fun processMerges(fighters: List<Fighter>, currentInventory: List<Equipment>, currentEnhancements: List<AbilityEnhancement>, applyBuffs: (List<Fighter>) -> List<Fighter>): Triple<List<Fighter>, List<Equipment>, List<AbilityEnhancement>> {
        var currentFighters = fighters.toList()
        var newInventory = currentInventory.toList()
        var newEnhancements = currentEnhancements.toList()
        var merged = true
        
        while (merged) {
            merged = false
            val grouped = currentFighters.groupBy { it.name to it.starLevel }
            for ((key, group) in grouped) {
                if (group.size >= 3 && key.second < 3) {
                    val (name, star) = key
                    val toMerge = group.take(3)
                    currentFighters = currentFighters.filterNot { it in toMerge }
                    
                    val refundedEquipment = toMerge.flatMap { it.equipment }
                    newInventory = newInventory + refundedEquipment
                    
                    val refundedEnhancements = toMerge.flatMap { it.enhancements }
                    newEnhancements = newEnhancements + refundedEnhancements
                    
                    val boardUnit = toMerge.find { it.startY >= 0 }
                    val targetPos = boardUnit ?: toMerge.first()
                    
                    val template = fighterPool.find { it.name == name }!!
                    val newFighterBase = instantiateFighter(template, true, targetPos.startY, starLevel = star + 1)
                    val newFighter = newFighterBase.copy(
                        id = UUID.randomUUID().toString(),
                        x = targetPos.startX, startX = targetPos.startX,
                        y = targetPos.startY, startY = targetPos.startY
                    )
                    
                    val buffed = applyBuffs(listOf(newFighter)).first()
                    currentFighters = currentFighters + buffed
                    merged = true
                    break
                }
            }
        }
        return Triple(currentFighters, newInventory, newEnhancements)
    }
}
