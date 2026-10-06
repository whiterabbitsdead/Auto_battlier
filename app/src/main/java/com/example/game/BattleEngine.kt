package com.example.game

import java.util.Random
import java.util.UUID
import kotlin.math.abs

/**
 * BattleEngine processes turns in the combat arena, calculates damage based
 * on fighter stats and special abilities, and produces updated immutable [BattleState] instances.
 */
class BattleEngine(
    private val random: Random = Random()
) {

    data class DamageCalculation(
        val damage: Int,
        val isHit: Boolean,
        val isDodge: Boolean,
        val isCrit: Boolean,
        val isMagic: Boolean,
        val logMessage: String
    )

    /**
     * Calculates base damage dealt by [attacker] to [target] based on their physical
     * and magical offensive and defensive stats, equipment, and critical strike chance.
     */
    fun calculateDamage(attacker: Fighter, target: Fighter): Int {
        return calculateDetailedDamage(attacker, target).damage
    }

    /**
     * Detailed combat calculation taking into account accuracy, dodge, physical vs magic stats,
     * equipment bonuses, star levels, critical strike rolls, and shield absorption.
     * Unified with [CombatCalculator].
     */
    fun calculateDetailedDamage(attacker: Fighter, target: Fighter): DamageCalculation {
        val result = CombatCalculator.calculateAttack(attacker, target, random)
        return DamageCalculation(
            damage = result.damage,
            isHit = result.isHit,
            isDodge = result.isDodge,
            isCrit = result.isCrit,
            isMagic = result.isMagic,
            logMessage = result.logMessage
        )
    }

    /**
     * Executes a single turn round, activating living fighters in order of attack speed,
     * applying movement, attacks, special abilities, and updating the [BattleState].
     */
    fun processTurn(currentState: BattleState): BattleState {
        // If battle is already over, return as-is
        if (currentState.isGameOver) {
            return currentState
        }

        val playerBoardUnits = currentState.playerFighters.filter { it.startY >= 0 }
        val playerHasLiving = playerBoardUnits.any { it.isAlive }
        val opponentHasLiving = currentState.opponentFighters.any { it.isAlive }

        // Termination checks before executing actions
        if (!playerHasLiving && !opponentHasLiving) {
            return currentState.copy(
                isBattleActive = false,
                battleResult = BattleResult.DRAW,
                battleLogs = listOf("Battle ended in a draw!") + currentState.battleLogs
            )
        }
        if (!playerHasLiving) {
            return currentState.copy(
                isBattleActive = false,
                battleResult = BattleResult.AI_WIN,
                battleLogs = listOf("Defeat! Opponent won the battle.") + currentState.battleLogs
            )
        }
        if (!opponentHasLiving) {
            return currentState.copy(
                isBattleActive = false,
                battleResult = BattleResult.PLAYER_WIN,
                battleLogs = listOf("Victory! All opponent units defeated.") + currentState.battleLogs
            )
        }

        // Clone units to guarantee state immutability
        val updatedPlayerFighters = currentState.playerFighters.map { it.copy() }.toMutableList()
        val updatedOpponentFighters = currentState.opponentFighters.map { it.copy() }.toMutableList()

        val damageDealtMap = currentState.roundDamageDealt.toMutableMap()
        val damageTakenMap = currentState.roundDamageTaken.toMutableMap()
        val newTurnLogs = mutableListOf<String>()
        val triggeredAbilities = currentState.activeSpecialAbilities.toMutableList()
        
        var playerCooldown = currentState.playerCommanderActiveCooldown
        var aiCooldown = currentState.aiCommanderActiveCooldown

        // Reduce commander cooldowns each turn
        if (playerCooldown > 0) playerCooldown--
        if (aiCooldown > 0) aiCooldown--

        // Commander Active Ability Logic
        currentState.playerCommander?.activeAbility?.let { active ->
            if (playerCooldown <= 0 && playerHasLiving) {
                executeCommanderActive(active, updatedPlayerFighters, updatedOpponentFighters, newTurnLogs)
                playerCooldown = active.cooldownTurns
            }
        }
        
        currentState.opponentCommander?.activeAbility?.let { active ->
            if (aiCooldown <= 0 && opponentHasLiving) {
                executeCommanderActive(active, updatedOpponentFighters, updatedPlayerFighters, newTurnLogs)
                aiCooldown = active.cooldownTurns
            }
        }

        // Turn preparation: Cooldown and status effect ticks for living fighters
        for (fighter in updatedPlayerFighters) {
            if (fighter.isAlive && fighter.startY >= 0) {
                fighter.reduceCooldown(1)
                fighter.tickTurnStatus()
            }
        }
        for (fighter in updatedOpponentFighters) {
            if (fighter.isAlive) {
                fighter.reduceCooldown(1)
                fighter.tickTurnStatus()
            }
        }

        // Turn initiative determined by attack speed descending
        val activeLivingFighters = (updatedPlayerFighters.filter { it.isAlive && it.startY >= 0 } +
                updatedOpponentFighters.filter { it.isAlive })
            .sortedByDescending { it.attackSpeed }

        // Selected Commander Tactical Battle Aura & Direct Command Logs
        val pCommander = currentState.playerCommander
        val opCommander = currentState.opponentCommander

        // Turn 1 Commander opening command shout
        if (currentState.turn == 0) {
            if (pCommander != null) {
                newTurnLogs.add("📢 [Commander ${pCommander.name}] issues orders: 'Charge! Stand your ground!'")
            }
            if (opCommander != null) {
                newTurnLogs.add("🔥 [Commander ${opCommander.name}] taunts: 'Show them no mercy!'")
            }
        }

        // Periodic Commander direct tactical support / interventions every 3 turns
        if (currentState.turn > 0 && currentState.turn % 3 == 0) {
            if (pCommander != null && playerHasLiving) {
                val playerFrontline = updatedPlayerFighters.filter { it.isAlive && it.startY >= 0 }
                val lowestHpAlly = playerFrontline.minByOrNull { it.health.toFloat() / it.maxHp }
                if (lowestHpAlly != null) {
                    val boostHeal = 20
                    lowestHpAlly.health = minOf(lowestHpAlly.maxHp, lowestHpAlly.health + boostHeal)
                    lowestHpAlly.hp = lowestHpAlly.health
                    newTurnLogs.add("🛡️ [Commander ${pCommander.name} Support] buffs ${lowestHpAlly.name} (+${boostHeal} HP rally)!")
                }
            }
            if (opCommander != null && opponentHasLiving) {
                val oppFrontline = updatedOpponentFighters.filter { it.isAlive }
                val targetPlayer = updatedPlayerFighters.filter { it.isAlive && it.startY >= 0 }.randomOrNull()
                if (targetPlayer != null) {
                    val directDmg = 12
                    applyDamage(targetPlayer, directDmg)
                    recordDamage(opCommander.name, targetPlayer.id, directDmg, damageDealtMap, damageTakenMap)
                    newTurnLogs.add("⚡ [Commander ${opCommander.name} Strike] fires tactical bombardment at ${targetPlayer.name} for ${directDmg} DMG!")
                    if (!targetPlayer.isAlive) {
                        newTurnLogs.add("💀 ${targetPlayer.name} was eliminated by Commander ${opCommander.name}'s barrage!")
                    }
                }
            }
        }

        for (fighter in activeLivingFighters) {
            // Re-verify if unit is still alive and capable of action
            if (!fighter.isAlive) continue

            val isPlayer = fighter.isPlayer
            val allies = if (isPlayer) updatedPlayerFighters else updatedOpponentFighters
            val enemies = if (isPlayer) updatedOpponentFighters else updatedPlayerFighters

            val currentActor = allies.find { it.id == fighter.id } ?: continue
            if (!currentActor.canAct || (isPlayer && currentActor.startY < 0)) continue

            val aliveEnemies = enemies.filter { it.isAlive && (!it.isPlayer || it.startY >= 0) }
            if (aliveEnemies.isEmpty()) break

            // Target the closest living enemy
            val target = aliveEnemies.minByOrNull { distance(currentActor, it) } ?: continue
            val dist = distance(currentActor, target)

            val ability = currentActor.effectiveAbility
            val canCastSpecial = currentActor.isAbilityReady
            val isSupportAbility = ability.targetType in listOf(AbilityTargetType.ALLY_SINGLE, AbilityTargetType.ALLY_ALL, AbilityTargetType.SELF) &&
                    (ability.effectType == AbilityEffectType.HEAL || ability.healAmount > 0 || ability.shieldAmount > 0)

            if (canCastSpecial && (isSupportAbility || dist <= currentActor.attackRange)) {
                // Execute Special Ability
                currentActor.mana = 0
                currentActor.resetCooldown(ability.cooldownTurns)
                triggeredAbilities.add(ability)

                when {
                    // Unique Effect: Healing (Single Ally, All Allies, or Self)
                    ability.effectType == AbilityEffectType.HEAL || ability.healAmount > 0 -> {
                        when (ability.targetType) {
                            AbilityTargetType.SELF -> {
                                val healVal = maxOf(20, ability.healAmount)
                                val prevHp = currentActor.health
                                currentActor.health = minOf(currentActor.maxHp, currentActor.health + healVal)
                                currentActor.hp = currentActor.health
                                val actualHealed = currentActor.health - prevHp
                                val shield = maxOf(0, ability.shieldAmount)
                                if (shield > 0) {
                                    currentActor.physDefense += (shield / 4)
                                }
                                newTurnLogs.add("💚 ${currentActor.name} activates [${ability.name}] healing self for $actualHealed HP (now ${currentActor.health}/${currentActor.maxHp})!")
                                if (ability.damage > 0 && target.isAlive) {
                                    applyDamage(target, ability.damage)
                                    recordDamage(currentActor.id, target.id, ability.damage, damageDealtMap, damageTakenMap)
                                    newTurnLogs.add("💥 [${ability.name}] strike deals ${ability.damage} DMG to ${target.name}!")
                                    if (!target.isAlive) newTurnLogs.add("💀 ${target.name} is defeated!")
                                }
                            }
                            AbilityTargetType.ALLY_ALL -> {
                                val livingAllies = allies.filter { it.isAlive && (!it.isPlayer || it.startY >= 0) }
                                val healVal = maxOf(15, ability.healAmount)
                                livingAllies.forEach { ally ->
                                    val prevHp = ally.health
                                    ally.health = minOf(ally.maxHp, ally.health + healVal)
                                    ally.hp = ally.health
                                }
                                newTurnLogs.add("💚 ${currentActor.name} casts [${ability.name}] healing all ${livingAllies.size} allies for $healVal HP!")
                                if (ability.damage > 0 && target.isAlive) {
                                    applyDamage(target, ability.damage)
                                    recordDamage(currentActor.id, target.id, ability.damage, damageDealtMap, damageTakenMap)
                                    newTurnLogs.add("✨ [${ability.name}] smites ${target.name} for ${ability.damage} DMG!")
                                    if (!target.isAlive) newTurnLogs.add("💀 ${target.name} is defeated!")
                                }
                            }
                            else -> {
                                // Default ALLY_SINGLE: heal lowest-health percentage ally
                                val livingAllies = allies.filter { it.isAlive && (!it.isPlayer || it.startY >= 0) }
                                val targetAlly = livingAllies.minByOrNull { it.health.toFloat() / maxOf(1, it.maxHp) } ?: currentActor
                                val healVal = maxOf(20, ability.healAmount)
                                val prevHp = targetAlly.health
                                targetAlly.health = minOf(targetAlly.maxHp, targetAlly.health + healVal)
                                targetAlly.hp = targetAlly.health
                                val actualHealed = targetAlly.health - prevHp
                                newTurnLogs.add("💚 ${currentActor.name} casts [${ability.name}] restoring $actualHealed HP to ${targetAlly.name} (now ${targetAlly.health}/${targetAlly.maxHp})!")
                                if (ability.damage > 0 && target.isAlive) {
                                    applyDamage(target, ability.damage)
                                    recordDamage(currentActor.id, target.id, ability.damage, damageDealtMap, damageTakenMap)
                                    newTurnLogs.add("✨ ${currentActor.name}'s [${ability.name}] smites ${target.name} for ${ability.damage} DMG!")
                                    if (!target.isAlive) newTurnLogs.add("💀 ${target.name} is defeated!")
                                }
                            }
                        }
                    }

                    // Unique Effect: Multi-Attacks (Multiple rapid strikes in one activation)
                    ability.effectType == AbilityEffectType.MULTI_ATTACK || ability.multiAttackHits > 1 -> {
                        val hits = maxOf(2, ability.multiAttackHits)
                        val totalDmg = maxOf(currentActor.damage * 2, ability.damage)
                        val hitDamage = maxOf(5, totalDmg / hits)
                        newTurnLogs.add("⚡ ${currentActor.name} unleashes [${ability.name}] ($hits-strike combo)!")
                        for (hitIndex in 1..hits) {
                            val livingEnemies = enemies.filter { it.isAlive && (!it.isPlayer || it.startY >= 0) }
                            if (livingEnemies.isEmpty()) break
                            val currentVictim = if (target.isAlive) target else (livingEnemies.minByOrNull { distance(currentActor, it) } ?: break)
                            applyDamage(currentVictim, hitDamage)
                            recordDamage(currentActor.id, currentVictim.id, hitDamage, damageDealtMap, damageTakenMap)
                            newTurnLogs.add("💥 Strike $hitIndex/$hits: ${currentActor.name} hits ${currentVictim.name} for $hitDamage DMG!")
                            if (!currentVictim.isAlive) {
                                newTurnLogs.add("💀 ${currentVictim.name} is defeated!")
                            }
                        }
                    }

                    // AOE Burst
                    ability.targetType == AbilityTargetType.ENEMY_ALL -> {
                        val aoeDamage = maxOf(15, ability.damage)
                        newTurnLogs.add("✨ ${currentActor.name} unleashes [${ability.name}] on all enemies for $aoeDamage DMG!")
                        aliveEnemies.forEach { enemy ->
                            applyDamage(enemy, aoeDamage)
                            recordDamage(currentActor.id, enemy.id, aoeDamage, damageDealtMap, damageTakenMap)
                            if (!enemy.isAlive) {
                                newTurnLogs.add("💀 ${enemy.name} is defeated!")
                            }
                        }
                    }

                    // Furthest Snipe
                    ability.targetType == AbilityTargetType.ENEMY_FURTHEST -> {
                        val furthest = aliveEnemies.maxByOrNull { distance(currentActor, it) } ?: target
                        val abilityDmg = maxOf(20, ability.damage)
                        applyDamage(furthest, abilityDmg)
                        recordDamage(currentActor.id, furthest.id, abilityDmg, damageDealtMap, damageTakenMap)
                        newTurnLogs.add("🏹 ${currentActor.name} casts [${ability.name}] sniping ${furthest.name} for $abilityDmg DMG!")
                        if (!furthest.isAlive) {
                            newTurnLogs.add("💀 ${furthest.name} is defeated!")
                        }
                    }

                    // Shielding
                    ability.targetType == AbilityTargetType.SELF || ability.effectType == AbilityEffectType.SHIELD -> {
                        val shield = maxOf(40, ability.shieldAmount)
                        currentActor.health = minOf(currentActor.maxHp, currentActor.health + (shield / 2))
                        currentActor.hp = currentActor.health
                        newTurnLogs.add("🛡️ ${currentActor.name} activates [${ability.name}] gaining +$shield defense & barrier!")
                    }

                    // Default / ENEMY_SINGLE Burst
                    else -> {
                        val abilityDmg = maxOf(currentActor.damage * 2, ability.damage)
                        applyDamage(target, abilityDmg)
                        recordDamage(currentActor.id, target.id, abilityDmg, damageDealtMap, damageTakenMap)
                        newTurnLogs.add("💥 ${currentActor.name} unleashes [${ability.name}] on ${target.name} for $abilityDmg DMG!")
                        if (!target.isAlive) {
                            newTurnLogs.add("💀 ${target.name} is defeated!")
                        }
                    }
                }
            } else if (dist <= currentActor.attackRange) {
                // Standard Attack
                    val calc = calculateDetailedDamage(currentActor, target)
                    newTurnLogs.add(calc.logMessage)

                    if (calc.isHit && !calc.isDodge) {
                        var finalDamage = calc.damage

                        // Stacked Synergy Bonuses for Player Units
                        if (isPlayer) {
                            if (currentActor.fighterClass == FighterClass.KNUCKLE_POWER &&
                                currentState.activeSynergies.any { it.startsWith("Knuckle Power") }
                            ) {
                                finalDamage += 15
                            }
                            if (currentActor.faction == Faction.REPTILIANS &&
                                currentState.activeSynergies.any { it.startsWith("Reptilians") }
                            ) {
                                finalDamage += 10
                            }
                        }

                        applyDamage(target, finalDamage)
                        recordDamage(currentActor.id, target.id, finalDamage, damageDealtMap, damageTakenMap)

                        // Gain combat mana
                        currentActor.mana = minOf(100, currentActor.mana + 15)
                        target.mana = minOf(100, target.mana + 10)

                        // Retaliation thorns (Tuls Faction synergy)
                        if (target.faction == Faction.TULS && target.isPlayer &&
                            currentState.activeSynergies.any { it.startsWith("Tuls") }
                        ) {
                            val thorns = maxOf(5, (finalDamage * 0.25).toInt())
                            applyDamage(currentActor, thorns)
                            recordDamage(target.id, currentActor.id, thorns, damageDealtMap, damageTakenMap)
                            newTurnLogs.add("🛡️ ${target.name} reflects $thorns thorns damage to ${currentActor.name}!")
                        }

                        if (!target.isAlive) {
                            newTurnLogs.add("💀 ${target.name} is defeated!")
                        }
                    } else if (calc.isDodge) {
                        // Dodge counter reflex (Feet Work)
                        if (target.faction == Faction.FEET_WORK && target.isPlayer &&
                            currentState.activeSynergies.any { it.startsWith("Feet Work") }
                        ) {
                            val counterDmg = 25
                            applyDamage(currentActor, counterDmg)
                            recordDamage(target.id, currentActor.id, counterDmg, damageDealtMap, damageTakenMap)
                            newTurnLogs.add("⚡ [Counter-Flash] ${target.name} dodged and countered for $counterDmg DMG!")
                            if (!currentActor.isAlive) {
                                newTurnLogs.add("💀 ${currentActor.name} is defeated!")
                            }
                        }
                    }
            } else {
                // Out of range: step 1 grid tile towards the target
                moveTowards(currentActor, target, updatedPlayerFighters + updatedOpponentFighters)
            }
        }

        // Evaluate battle outcome after turn actions
        val remainingPlayer = updatedPlayerFighters.any { it.isAlive && it.startY >= 0 }
        val remainingOpponent = updatedOpponentFighters.any { it.isAlive }

        val isStillActive: Boolean
        val result: BattleResult?

        if (!remainingPlayer && !remainingOpponent) {
            isStillActive = false
            result = BattleResult.DRAW
            newTurnLogs.add("Battle ended in a draw!")
        } else if (!remainingPlayer) {
            isStillActive = false
            result = BattleResult.AI_WIN
            newTurnLogs.add("Defeat! All player fighters fallen.")
        } else if (!remainingOpponent) {
            isStillActive = false
            result = BattleResult.PLAYER_WIN
            newTurnLogs.add("Victory! All enemy fighters defeated.")
        } else {
            isStillActive = true
            result = null
        }

        val combinedLogs = (newTurnLogs.reversed() + currentState.battleLogs).take(MAX_COMBAT_LOG_HISTORY)

        return currentState.copy(
            turn = currentState.turn + 1,
            playerFighters = updatedPlayerFighters,
            opponentFighters = updatedOpponentFighters,
            isBattleActive = isStillActive,
            battleResult = result,
            battleLogs = combinedLogs,
            activeSpecialAbilities = triggeredAbilities.takeLast(10),
            roundDamageDealt = damageDealtMap,
            roundDamageTaken = damageTakenMap,
            playerCommanderActiveCooldown = playerCooldown,
            aiCommanderActiveCooldown = aiCooldown
        )
    }

    private fun executeCommanderActive(
        active: CommanderActive,
        allies: List<Fighter>,
        enemies: List<Fighter>,
        logs: MutableList<String>
    ) {
        logs.add(0, "✨ Commander casts ${active.name}!")
        when (active.type) {
            CommanderActiveType.SINGLE_TARGET_DMG -> {
                val target = enemies.filter { it.isAlive }.minByOrNull { it.hp }
                if (target != null) {
                    target.hp = maxOf(0, target.hp - active.value)
                    logs.add(0, "💥 ${active.name} strikes ${target.name} for ${active.value} damage!")
                }
            }
            CommanderActiveType.AOE_DMG -> {
                enemies.filter { it.isAlive }.forEach {
                    it.hp = maxOf(0, it.hp - active.value)
                }
                logs.add(0, "🔥 ${active.name} blasts all enemies for ${active.value} damage!")
            }
            CommanderActiveType.HEAL_LOWEST -> {
                val target = allies.filter { it.isAlive }.minByOrNull { it.hp }
                if (target != null) {
                    target.hp = minOf(target.maxHp, target.hp + active.value)
                    logs.add(0, "💚 ${active.name} restores ${active.value} HP to ${target.name}!")
                }
            }
            CommanderActiveType.SHIELD_ALL -> {
                allies.filter { it.isAlive }.forEach {
                    it.shield += active.value.toFloat()
                }
                logs.add(0, "🛡️ ${active.name} shields all allies for ${active.value}!")
            }
            CommanderActiveType.STUN_RANDOM -> {
                val target = enemies.filter { it.isAlive }.randomOrNull()
                if (target != null) {
                    target.stunDuration += 1.0f
                    logs.add(0, "🌀 ${active.name} stuns ${target.name}!")
                }
            }
            CommanderActiveType.BUFF_SPEED -> {
                allies.filter { it.isAlive }.forEach {
                    it.attackSpeed += active.value
                }
                logs.add(0, "⚡ ${active.name} increases all allies' speed by ${active.value}!")
            }
        }
    }

    /**
     * Convenience method to simulate an entire battle turn by turn until completion
     * or until reaching [maxTurns].
     */
    fun simulateBattleToCompletion(initialState: BattleState, maxTurns: Int = 100): BattleState {
        var state = if (!initialState.isBattleActive && initialState.battleResult == null) {
            initialState.copy(isBattleActive = true)
        } else {
            initialState
        }

        var turns = 0
        while (state.isBattleActive && turns < maxTurns) {
            state = processTurn(state)
            turns++
        }

        // Fallback resolution if maxTurns exceeded without a victor
        if (state.isBattleActive) {
            val playerHp = state.totalPlayerHealth
            val enemyHp = state.totalOpponentHealth
            val result = if (playerHp > enemyHp) BattleResult.PLAYER_WIN else BattleResult.AI_WIN
            state = state.copy(
                isBattleActive = false,
                battleResult = result,
                battleLogs = listOf("Turn limit reached! Decided by remaining total HP.") + state.battleLogs
            )
        }

        return state
    }

    /**
     * Initializes a fresh [BattleState] ready for simulation.
     */
    fun initBattle(
        playerCommander: Commander?,
        opponentCommander: Commander?,
        playerFighters: List<Fighter>,
        opponentFighters: List<Fighter>,
        round: Int = 1,
        fight: Int = 1,
        activeSynergies: List<String> = emptyList()
    ): BattleState {
        return BattleState(
            round = round,
            fight = fight,
            turn = 0,
            playerCommander = playerCommander,
            opponentCommander = opponentCommander,
            playerFighters = playerFighters.map { it.copy() },
            opponentFighters = opponentFighters.map { it.copy() },
            isBattleActive = true,
            battleResult = null,
            battleLogs = listOf("Battle Started - Round $round Fight $fight"),
            activeSynergies = activeSynergies
        )
    }

    private fun applyDamage(fighter: Fighter, amount: Int) {
        fighter.health = maxOf(0, fighter.health - amount)
        fighter.hp = fighter.health
    }

    private fun recordDamage(
        dealerId: String,
        receiverId: String,
        amount: Int,
        dealtMap: MutableMap<String, Int>,
        takenMap: MutableMap<String, Int>
    ) {
        dealtMap[dealerId] = (dealtMap[dealerId] ?: 0) + amount
        takenMap[receiverId] = (takenMap[receiverId] ?: 0) + amount
    }

    private fun distance(f1: Fighter, f2: Fighter): Int {
        return abs(f1.x - f2.x) + abs(f1.y - f2.y)
    }

    private fun moveTowards(actor: Fighter, target: Fighter, allUnits: List<Fighter>) {
        val dx = target.x - actor.x
        val dy = target.y - actor.y

        val stepX = if (dx > 0) 1 else if (dx < 0) -1 else 0
        val stepY = if (dy > 0) 1 else if (dy < 0) -1 else 0

        val oldX = actor.x
        val oldY = actor.y

        if (abs(dx) >= abs(dy) && stepX != 0) {
            val candidateX = (actor.x + stepX).coerceIn(0, 7)
            val occupied = allUnits.any { it.isAlive && it.id != actor.id && it.x == candidateX && it.y == actor.y }
            if (!occupied) {
                actor.x = candidateX
                return
            }
        }

        if (stepY != 0) {
            val candidateY = (actor.y + stepY).coerceIn(0, 7)
            val occupied = allUnits.any { it.isAlive && it.id != actor.id && it.x == actor.x && it.y == candidateY }
            if (!occupied) {
                actor.y = candidateY
                return
            }
        }

        // Try alternative axis if primary was blocked
        if (stepX != 0) {
            val candidateX = (actor.x + stepX).coerceIn(0, 7)
            val occupied = allUnits.any { it.isAlive && it.id != actor.id && it.x == candidateX && it.y == actor.y }
            if (!occupied) {
                actor.x = candidateX
            }
        }
    }

    companion object {
        private val defaultEngine = BattleEngine()

        fun calculateDamage(attacker: Fighter, target: Fighter): Int =
            defaultEngine.calculateDamage(attacker, target)

        fun calculateDetailedDamage(attacker: Fighter, target: Fighter): DamageCalculation =
            defaultEngine.calculateDetailedDamage(attacker, target)

        fun processTurn(currentState: BattleState): BattleState =
            defaultEngine.processTurn(currentState)

        fun simulateBattleToCompletion(initialState: BattleState, maxTurns: Int = 100): BattleState =
            defaultEngine.simulateBattleToCompletion(initialState, maxTurns)
    }
}
