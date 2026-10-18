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
     * Distance matching auto-chess grid standard (Chebyshev metric)
     */
    fun distance(f1: Fighter, f2: Fighter): Int {
        return chebyshevDistance(f1, f2)
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
        val logMessage: String,
        val hitSector: HitSector = HitSector.FRONTAL,
        val incidentAngle: Double = 0.0,
        val directionalEvent: DirectionalCombatEvent = DirectionalCombatEvent.FRONTAL_STRIKE,
        val isPhalanxProtected: Boolean = false,
        val postureDamageDealt: Float = 0.0f,
        val isDeflected: Boolean = false,
        val isDeathblow: Boolean = false
    )

    /**
     * Calculates the incident strike angle in degrees [0.0, 180.0] relative to target's facing.
     * 0° indicates a direct frontal attack; 180° indicates an attack from directly behind (backstab).
     */
    fun calculateIncidentAngle(attacker: Fighter, target: Fighter): Double {
        val dx = attacker.x - target.x
        val dy = attacker.y - target.y
        if (dx == 0 && dy == 0) return 0.0

        val facingVecX = target.facing.dx.toDouble()
        val facingVecY = target.facing.dy.toDouble()
        val incomingVecX = dx.toDouble()
        val incomingVecY = dy.toDouble()

        val dot = facingVecX * incomingVecX + facingVecY * incomingVecY
        val magFacing = Math.sqrt(facingVecX * facingVecX + facingVecY * facingVecY)
        val magIncoming = Math.sqrt(incomingVecX * incomingVecX + incomingVecY * incomingVecY)
        if (magFacing == 0.0 || magIncoming == 0.0) return 0.0

        val cosTheta = (dot / (magFacing * magIncoming)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(Math.acos(cosTheta))
    }

    /**
     * Checks if adjacent allies facing the same forward direction form an interlocking phalanx shield wall
     * that covers the target's flank on the attacker's side.
     */
    fun isProtectedByPhalanx(attacker: Fighter, target: Fighter, allies: List<Fighter>): Boolean {
        if (allies.isEmpty()) return false
        val phalanxAllies = allies.filter { ally ->
            ally.isAlive && ally.id != target.id &&
            !ally.isGuardBroken && !ally.isStaggered &&
            chebyshevDistance(ally, target) == 1 &&
            target.facing.angleDifference(ally.facing) <= 45.0 &&
            (ally.shieldType != ShieldType.NONE || ally.fighterClass == FighterClass.TANK || ally.physDefense >= 15)
        }
        val attackLateral = -target.facing.dy * (attacker.x - target.x) + target.facing.dx * (attacker.y - target.y)
        return phalanxAllies.any { ally ->
            val allyLateral = -target.facing.dy * (ally.x - target.x) + target.facing.dx * (ally.y - target.y)
            (attackLateral > 0 && allyLateral > 0) || (attackLateral < 0 && allyLateral < 0)
        }
    }

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
     * Performs standard auto-attack roll with accuracy, dodge, directional hit angle classification,
     * facing-dependent defense bypass, critical strike, and directional shield absorption.
     * Accepts an optional [random] generator for deterministic testing.
     */
    fun calculateAttack(
        attacker: Fighter,
        target: Fighter,
        random: java.util.Random? = null,
        targetAllies: List<Fighter> = emptyList()
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

        // Calculate incident angle and classify hit sector
        val incidentAngle = calculateIncidentAngle(attacker, target)
        var hitSector = HitSector.fromIncidentAngle(incidentAngle)
        val phalanxCovered = if (hitSector == HitSector.FLANKING) {
            isProtectedByPhalanx(attacker, target, targetAllies)
        } else false

        if (phalanxCovered) {
            hitSector = HitSector.FRONTAL
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

        // Directional armor penetration, posture checks, and active deflection
        val isFrontal = hitSector == HitSector.FRONTAL
        val canAttemptDeflection = isFrontal && !target.isGuardBroken && !target.isStaggered && !target.isStunned && target.canAct
        val isDeflected = canAttemptDeflection && (nextRand() < target.deflectionChance)

        val effectiveDef: Float
        var directionalEvent: DirectionalCombatEvent
        var critBonusChance = 0.0
        var critMultiplier = 1.5
        var forceCrit = false
        var isDeathblow = false

        if (isDeflected) {
            effectiveDef = totalDef
            directionalEvent = DirectionalCombatEvent.DEFLECTION_COUNTER
            // Reflected posture damage onto the attacker
            val reflectedPosture = (target.physDefense * 0.4f + 15.0f).coerceIn(10.0f, 40.0f)
            attacker.depletePosture(reflectedPosture)
        } else {
            val executeRoll = target.isGuardBroken && (nextRand() < attacker.critChance)
            if (executeRoll) {
                isDeathblow = true
                forceCrit = true
                critMultiplier = 2.5
            }

            val directionalDefFactor = when (hitSector) {
                HitSector.REAR -> 0.2f
                HitSector.FLANKING -> 0.6f
                else -> 1.0f
            }
            if (hitSector == HitSector.REAR) {
                forceCrit = true
                critMultiplier = maxOf(critMultiplier, 2.0)
            }
            if (hitSector == HitSector.FLANKING) {
                critBonusChance = maxOf(critBonusChance, 0.20)
            }

            val postureDefFactor = when {
                isDeathblow -> 0.2f
                target.isGuardBroken -> 0.5f
                target.isStaggered -> {
                    critBonusChance = maxOf(critBonusChance, 0.20)
                    0.7f
                }
                else -> 1.0f
            }

            effectiveDef = totalDef * minOf(directionalDefFactor, postureDefFactor)

            directionalEvent = when {
                isDeathblow -> DirectionalCombatEvent.DEATHBLOW_EXECUTION
                hitSector == HitSector.REAR -> DirectionalCombatEvent.CRITICAL_BACKSTAB
                target.isGuardBroken -> DirectionalCombatEvent.GUARD_BROKEN_EXPOSURE
                target.isStaggered -> DirectionalCombatEvent.POSTURE_STAGGERED
                hitSector == HitSector.FLANKING -> DirectionalCombatEvent.GLANCING_FLANK
                phalanxCovered -> DirectionalCombatEvent.PHALANX_INTERLOCKED_DEFENSE
                else -> DirectionalCombatEvent.FRONTAL_STRIKE
            }
        }

        // Base linear armor formula: max(1, attack - effectiveDefense / 2)
        var damage = maxOf(1, (totalAtk - (effectiveDef / 2)).toInt())

        // Star-level scaling bonus
        if (attacker.starLevel > 1) {
            val starMultiplier = if (attacker.starLevel == 3) 1.6 else 1.25
            damage = (damage * starMultiplier).toInt()
        }

        // Critical strike check
        var isCrit = forceCrit
        if (!isCrit && !target.isGuardBroken && nextRand() < (attacker.critChance + critBonusChance)) {
            isCrit = true
        }
        if (isCrit) {
            damage = (damage * critMultiplier).toInt()
        }

        // Deathblow execution deals additional 10% target max HP damage
        if (isDeathblow) {
            damage += (target.maxHp * 0.10f).toInt()
        }

        // Active deflection mitigates 75% of incoming damage
        if (isDeflected) {
            damage = maxOf(1, (damage * 0.25f).toInt())
        }

        // Shield absorption & posture depletion
        var absorbed = 0
        var finalDamage = damage

        // Rear attacks completely bypass directional and barrier shields; staggered/guard-broken targets cannot guard
        val shieldCanDeflect = !isDeflected && !target.isStaggered && hitSector != HitSector.REAR && !target.isGuardBroken &&
                (phalanxCovered || target.canBlockDirection(incidentAngle))

        if (shieldCanDeflect && target.shield > 0.0f) {
            val shieldInt = target.shield.toInt()
            if (shieldInt >= finalDamage) {
                absorbed = finalDamage
                target.shield = maxOf(0.0f, target.shield - finalDamage)
                finalDamage = 0
                directionalEvent = DirectionalCombatEvent.SHIELD_ARC_DEFLECTED
            } else {
                absorbed = shieldInt
                finalDamage -= shieldInt
                target.shield = 0.0f
                if (directionalEvent == DirectionalCombatEvent.FRONTAL_STRIKE) {
                    directionalEvent = DirectionalCombatEvent.FRONTAL_BLOCKED
                }
            }
            // Sustained frontal impact depletes block posture
            target.depletePosture(absorbed.toFloat() * 0.5f + 5.0f)
        } else if (shieldCanDeflect && hitSector == HitSector.FRONTAL) {
            // Frontal strike absorbed by guard posture
            target.depletePosture(12.0f)
            if (phalanxCovered) {
                directionalEvent = DirectionalCombatEvent.PHALANX_INTERLOCKED_DEFENSE
            } else if (target.shieldType != ShieldType.NONE && directionalEvent == DirectionalCombatEvent.FRONTAL_STRIKE) {
                directionalEvent = DirectionalCombatEvent.FRONTAL_BLOCKED
            }
        }

        // Apply general attack posture damage and mark damage taken
        var postureDamageDealt = 0.0f
        target.tookDamageThisTurn = true
        if (isDeflected) {
            target.depletePosture(2.0f)
            postureDamageDealt = 2.0f
        } else {
            val impactMultiplier = if (attacker.fighterClass == FighterClass.KNUCKLE_POWER || attacker.fighterClass == FighterClass.TANK) 1.5f else 1.0f
            val basePostureDmg = (maxOf(attacker.physAttack, attacker.damage, attacker.attackPower) * 0.35f + 8.0f) * impactMultiplier
            target.depletePosture(basePostureDmg)
            postureDamageDealt = basePostureDmg
        }

        // Generate focus on hit (0.5 focus per hit taken, 1.0 focus for attacker)
        target.focus = minOf(10.0f, target.focus + 0.5f)
        attacker.focus = minOf(10.0f, attacker.focus + 1.0f)
        attacker.attacksSinceSkill++

        val critText = if (isCrit) " CRITICALLY" else ""
        val magicText = if (isMagic) " (Magic)" else ""
        val shieldText = if (absorbed > 0) " (🛡️ $absorbed absorbed by shield)" else ""
        val sectorTag = when (directionalEvent) {
            DirectionalCombatEvent.CRITICAL_BACKSTAB -> " [🗡️ BACKSTAB]"
            DirectionalCombatEvent.GLANCING_FLANK -> " [⚔️ FLANK]"
            DirectionalCombatEvent.PHALANX_INTERLOCKED_DEFENSE -> " [🛡️ PHALANX]"
            DirectionalCombatEvent.SHIELD_ARC_DEFLECTED -> " [🛡️ DEFLECTED]"
            DirectionalCombatEvent.FRONTAL_BLOCKED -> " [🛡️ BLOCKED]"
            DirectionalCombatEvent.GUARD_BROKEN_EXPOSURE -> " [⚠️ GUARD BROKEN]"
            DirectionalCombatEvent.POSTURE_STAGGERED -> " [⚡ STAGGERED]"
            DirectionalCombatEvent.DEFLECTION_COUNTER -> " [⚔️ DEFLECTED COUNTER]"
            DirectionalCombatEvent.DEATHBLOW_EXECUTION -> " [⚡ DEATHBLOW EXECUTION]"
            DirectionalCombatEvent.FRONTAL_STRIKE -> ""
        }
        val log = "${attacker.name}$critText hits ${target.name}$magicText for $finalDamage dmg!$shieldText$sectorTag"

        return AttackResult(
            damage = maxOf(0, finalDamage),
            isHit = true,
            isDodge = false,
            isCrit = isCrit,
            isMagic = isMagic,
            damageType = damageType,
            shieldAbsorbed = absorbed,
            logMessage = log,
            hitSector = hitSector,
            incidentAngle = incidentAngle,
            directionalEvent = directionalEvent,
            isPhalanxProtected = phalanxCovered,
            postureDamageDealt = postureDamageDealt,
            isDeflected = isDeflected,
            isDeathblow = isDeathblow
        )
    }
}
