package com.example.game

import kotlin.math.abs
import kotlin.math.max

object CombatCalculator {
    /**
     * Chebyshev distance matching auto-chess grid standard (max(abs(dx), abs(dy)))
     */
    fun chebyshevDistance(f1: Fighter, f2: Fighter): Int {
        return max(abs(f1.x - f2.x), abs(f1.y - f2.y))
    }

    /**
     * Manhattan distance (fallback and backward-compatibility)
     */
    fun distance(f1: Fighter, f2: Fighter): Int {
        return abs(f1.x - f2.x) + abs(f1.y - f2.y)
    }

    data class AttackResult(
        val damage: Int,
        val isHit: Boolean,
        val isDodge: Boolean,
        val isCrit: Boolean,
        val isMagic: Boolean = false,
        val damageType: DamageType = DamageType.PHYSICAL,
        val shieldAbsorbed: Int = 0,
        val ccApplied: CCType = CCType.NONE,
        val logMessage: String
    )

    /**
     * Diminishing returns defense mitigation calculation:
     * mitigated = rawAmount * (100.0 / (100.0 + defense))
     */
    fun calculateMitigatedDamage(rawAmount: Float, defense: Float, damageType: DamageType): Float {
        if (damageType == DamageType.TRUE) return max(1.0f, rawAmount)
        val mitigation = 100.0f / (100.0f + max(0.0f, defense))
        return max(1.0f, rawAmount * mitigation)
    }

    /**
     * Performs standard auto-attack roll with accuracy, dodge, defense calculation,
     * critical strike, and shield absorption.
     * Accepts an optional [random] generator for deterministic testing.
     */
    fun calculateAttack(
        attacker: Fighter,
        target: Fighter,
        random: java.util.Random? = null
    ): AttackResult {
        fun nextRand(): Double = random?.nextDouble() ?: Math.random()

        // Accuracy check: Melee (range <= 1) always hits; ranged depends on accuracy
        val hitChance = if (attacker.attackRange <= 1) 1.0 else (attacker.accuracy / 100.0)
        if (nextRand() > hitChance) {
            return AttackResult(
                damage = 0,
                isHit = false,
                isDodge = false,
                isCrit = false,
                logMessage = "${attacker.name} missed ${target.name}!"
            )
        }

        // Dodge check (physical strikes only)
        if (nextRand() < target.dodgeChance) {
            return AttackResult(
                damage = 0,
                isHit = true,
                isDodge = true,
                isCrit = false,
                logMessage = "${target.name} dodged ${attacker.name}'s strike!"
            )
        }

        // Determine damage type & offensive/defensive stats
        val isMagic = attacker.magAttack > attacker.physAttack || attacker.fighterClass == FighterClass.ILLUMINS
        val damageType = if (isMagic) DamageType.MAGIC else DamageType.PHYSICAL
        val atkStat = if (isMagic) maxOf(attacker.magAttack, attacker.damage, attacker.attackPower)
                      else maxOf(attacker.physAttack, attacker.damage, attacker.attackPower)
        val defStat = if (isMagic) target.magDefense else maxOf(target.physDefense, target.defense)

        // Equipment bonuses
        val equipmentAttackBonus = attacker.equipment.count { it.type == EquipmentType.ATTACK } * 15
        val equipmentDefenseBonus = target.equipment.count { it.type == EquipmentType.DEFENSE } * 10
        val totalAtk = (atkStat + equipmentAttackBonus).toFloat()
        val totalDef = (defStat + equipmentDefenseBonus).toFloat()

        // Base linear armor formula: max(1, attack - defense / 2)
        var damage = maxOf(1, (totalAtk - (totalDef / 2)).toInt())

        // Star-level scaling bonus
        if (attacker.starLevel > 1) {
            val starMultiplier = if (attacker.starLevel == 3) 1.6 else 1.25
            damage = (damage * starMultiplier).toInt()
        }

        // Critical strike check
        var isCrit = false
        if (nextRand() < attacker.critChance) {
            damage = (damage * 1.5).toInt()
            isCrit = true
        }

        // Shield absorption
        var absorbed = 0
        var finalDamage = damage
        if (target.shield > 0.0f) {
            val shieldInt = target.shield.toInt()
            if (shieldInt >= finalDamage) {
                absorbed = finalDamage
                target.shield = maxOf(0.0f, target.shield - finalDamage)
                finalDamage = 0
            } else {
                absorbed = shieldInt
                finalDamage -= shieldInt
                target.shield = 0.0f
            }
        }

        // Generate focus on hit (0.5 focus per hit taken, 1.0 focus for attacker)
        target.focus = minOf(10.0f, target.focus + 0.5f)
        attacker.focus = minOf(10.0f, attacker.focus + 1.0f)
        attacker.attacksSinceSkill++

        val critText = if (isCrit) " CRITICALLY" else ""
        val magicText = if (isMagic) " (Magic)" else ""
        val shieldText = if (absorbed > 0) " (🛡️ $absorbed absorbed by shield)" else ""
        val log = "${attacker.name}$critText hits ${target.name}$magicText for $finalDamage dmg!$shieldText"

        return AttackResult(
            damage = maxOf(0, finalDamage),
            isHit = true,
            isDodge = false,
            isCrit = isCrit,
            isMagic = isMagic,
            damageType = damageType,
            shieldAbsorbed = absorbed,
            logMessage = log
        )
    }
}
