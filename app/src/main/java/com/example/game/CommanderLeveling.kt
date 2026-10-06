package com.example.game

import kotlin.math.ceil

object CommanderLeveling {
    const val MAX_LEVEL = 10
    const val EXP_PER_GOLD = 3

    // Calculates how much EXP is needed to progress from the current level to the next.
    // We use a simple curve for now: Level 3 takes 30 EXP, Level 4 takes 40 EXP, etc.
    fun getRequiredExpForNextLevel(currentLevel: Int): Int {
        if (currentLevel >= MAX_LEVEL) return 0
        return currentLevel * 10
    }

    // Calculates the exact gold cost to instantly buy the remaining EXP needed for the next level.
    fun getCostToLevelUp(currentLevel: Int, currentExp: Int): Int {
        if (currentLevel >= MAX_LEVEL) return 0
        val expNeeded = getRequiredExpForNextLevel(currentLevel) - currentExp
        return ceil(expNeeded.toDouble() / EXP_PER_GOLD).toInt()
    }

    // Handles adding passive EXP (e.g. after a battle) and rolling over into level ups.
    // Returns a Pair of (NewLevel, NewExp)
    fun addPassiveExp(currentLevel: Int, currentExp: Int, gainedExp: Int): Pair<Int, Int> {
        var newLevel = currentLevel
        var newExp = currentExp + gainedExp

        while (newLevel < MAX_LEVEL) {
            val required = getRequiredExpForNextLevel(newLevel)
            if (newExp >= required) {
                newExp -= required
                newLevel++
            } else {
                break
            }
        }
        
        // Cap exp if max level is reached
        if (newLevel >= MAX_LEVEL) {
            newExp = 0
        }

        return Pair(newLevel, newExp)
    }
}
