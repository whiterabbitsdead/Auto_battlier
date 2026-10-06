package com.example.game

import java.util.Random
import java.util.UUID

/**
 * CombatEngine simulates turn-based battles between two lists of fighters
 * based on core attributes: HP, Attack, Speed (initiative), Defense, Star Level,
 * and Special Abilities, capturing events into [CombatLog].
 *
 * Mechanics:
 * - Initiative: Fighters take turns in order of their [speed] stat (descending).
 * - Attack & Damage: Calculated based on [attack], target's [defense], and [starLevel].
 * - Upgraded Heroes: 2-star and 3-star fighters deal greater damage and have higher stats.
 * - Stat Balance: 5-gold heroes have stats close to 3-star 1-gold heroes.
 * - Special Abilities: Evaluated and cast when ready, appending rich combat events to [CombatLog].
 * - Survival: Fighters with HP <= 0 are knocked out and cannot act in subsequent turns.
 */
class CombatEngine(
    private val random: Random = Random(),
    private val targetingStrategy: TargetingStrategy = TargetingStrategy.FIRST_ALIVE,
    private val maxRounds: Int = 100,
    val combatLog: CombatLog = CombatLog()
) {

    enum class Team {
        TEAM_A,
        TEAM_B;

        val opponent: Team
            get() = if (this == TEAM_A) TEAM_B else TEAM_A
    }

    enum class BattleOutcome {
        TEAM_A_VICTORY,
        TEAM_B_VICTORY,
        DRAW,
        IN_PROGRESS
    }

    enum class TargetingStrategy {
        FIRST_ALIVE,
        LOWEST_HP,
        HIGHEST_ATTACK,
        HIGHEST_SPEED,
        RANDOM
    }

    /**
     * Represents a combatant participating in battle.
     * Core attributes: [hp], [attack], [speed], [defense], [starLevel], and optional [specialAbility].
     */
    data class Combatant(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        var hp: Int,
        val maxHp: Int = hp,
        val attack: Int,
        val speed: Int,
        val defense: Int = 0,
        val team: Team = Team.TEAM_A,
        val cost: Int = 1,
        var starLevel: Int = 1,
        var mana: Int = 0,
        val maxMana: Int = 100,
        val specialAbility: SpecialAbility? = null,
        var abilityCooldown: Int = 0
    ) {
        val isAlive: Boolean get() = hp > 0
        val isAbilityReady: Boolean
            get() = isAlive && specialAbility != null && (abilityCooldown <= 0 || mana >= (specialAbility?.manaCost ?: 50))

        /**
         * Applies incoming damage and returns the actual damage dealt.
         */
        fun takeDamage(amount: Int): Int {
            if (!isAlive || amount <= 0) return 0
            val effectiveDamage = minOf(hp, amount)
            hp = (hp - effectiveDamage).coerceAtLeast(0)
            return effectiveDamage
        }

        fun heal(amount: Int): Int {
            if (!isAlive || amount <= 0) return 0
            val effectiveHeal = minOf(maxHp - hp, amount)
            hp += effectiveHeal
            return effectiveHeal
        }

        fun copyCombatant(): Combatant = copy()
    }

    /**
     * Details of an individual attack action in combat.
     */
    data class CombatAction(
        val round: Int,
        val turnInRound: Int,
        val attackerId: String,
        val attackerName: String,
        val attackerTeam: Team,
        val attackerSpeed: Int,
        val targetId: String,
        val targetName: String,
        val targetTeam: Team,
        val damageDealt: Int,
        val targetRemainingHp: Int,
        val isFatal: Boolean,
        val logMessage: String,
        val isSpecialAbility: Boolean = false,
        val abilityName: String? = null
    )

    /**
     * Summary of actions in a completed round.
     */
    data class CombatRound(
        val roundNumber: Int,
        val actions: List<CombatAction>,
        val teamASurvivors: Int,
        val teamBSurvivors: Int
    )

    /**
     * Immutable snapshot of the combat state at any point during simulation.
     */
    data class CombatState(
        val round: Int = 1,
        val teamAFighters: List<Combatant>,
        val teamBFighters: List<Combatant>,
        val actionHistory: List<CombatAction> = emptyList(),
        val roundHistory: List<CombatRound> = emptyList(),
        val logs: List<String> = emptyList(),
        val outcome: BattleOutcome = BattleOutcome.IN_PROGRESS,
        val damageDealtByFighter: Map<String, Int> = emptyMap(),
        val damageTakenByFighter: Map<String, Int> = emptyMap()
    ) {
        val isGameOver: Boolean get() = outcome != BattleOutcome.IN_PROGRESS
        val survivingTeamA: List<Combatant> get() = teamAFighters.filter { it.isAlive }
        val survivingTeamB: List<Combatant> get() = teamBFighters.filter { it.isAlive }
        val totalTeamAHp: Int get() = survivingTeamA.sumOf { it.hp }
        val totalTeamBHp: Int get() = survivingTeamB.sumOf { it.hp }
    }

    /**
     * Final result of a completed combat simulation.
     */
    data class CombatResult(
        val outcome: BattleOutcome,
        val winnerTeam: Team?,
        val totalRounds: Int,
        val totalActions: Int,
        val teamAFighters: List<Combatant>,
        val teamBFighters: List<Combatant>,
        val survivingTeamA: List<Combatant>,
        val survivingTeamB: List<Combatant>,
        val logs: List<String>,
        val actionHistory: List<CombatAction>,
        val roundHistory: List<CombatRound>,
        val damageDealtByFighter: Map<String, Int>,
        val damageTakenByFighter: Map<String, Int>,
        val mvp: Combatant?
    ) {
        val isDraw: Boolean get() = outcome == BattleOutcome.DRAW
        val isTeamAVictory: Boolean get() = outcome == BattleOutcome.TEAM_A_VICTORY
        val isTeamBVictory: Boolean get() = outcome == BattleOutcome.TEAM_B_VICTORY
    }

    /**
     * Calculates damage dealt from [attacker] to [target] based on Attack and Defense attributes,
     * accounting for star-level upgrades (upgraded heroes deal more damage).
     */
    fun calculateDamage(attacker: Combatant, target: Combatant): Int {
        var rawAttack = attacker.attack.toFloat()

        // Upgraded heroes are stronger: star level attack bonus if not already scaled
        if (attacker.starLevel == 2) {
            rawAttack *= 1.25f
        } else if (attacker.starLevel >= 3) {
            rawAttack *= 1.6f
        }

        val defenseMitigation = (target.defense / 2.0f).coerceAtLeast(0.0f)
        return (rawAttack - defenseMitigation).toInt().coerceAtLeast(1)
    }

    /**
     * Calculates damage between application [Fighter]s based on attributes and star level.
     */
    fun calculateDamage(attacker: Fighter, target: Fighter): Int {
        val isMagic = attacker.magAttack > attacker.physAttack || attacker.fighterClass == FighterClass.ILLUMINS
        val atk = if (isMagic) maxOf(attacker.magAttack, attacker.damage, attacker.attackPower)
                  else maxOf(attacker.physAttack, attacker.damage, attacker.attackPower)
        val def = if (isMagic) target.magDefense else maxOf(target.physDefense, target.defense)
        var damage = maxOf(1, atk - (def / 2))
        if (attacker.starLevel == 2) {
            damage = (damage * 1.25).toInt()
        } else if (attacker.starLevel >= 3) {
            damage = (damage * 1.6).toInt()
        }
        return damage.coerceAtLeast(1)
    }

    /**
     * Initializes a fresh [CombatState] with deep copies of the fighters.
     */
    fun initCombat(teamA: List<Combatant>, teamB: List<Combatant>): CombatState {
        combatLog.startFight()
        val clonedA = teamA.map { it.copy(team = Team.TEAM_A) }
        val clonedB = teamB.map { it.copy(team = Team.TEAM_B) }

        val initialOutcome = when {
            clonedA.none { it.isAlive } && clonedB.none { it.isAlive } -> BattleOutcome.DRAW
            clonedA.none { it.isAlive } -> BattleOutcome.TEAM_B_VICTORY
            clonedB.none { it.isAlive } -> BattleOutcome.TEAM_A_VICTORY
            else -> BattleOutcome.IN_PROGRESS
        }

        val initialLogs = if (initialOutcome != BattleOutcome.IN_PROGRESS) {
            listOf("Combat ended before starting: Outcome is $initialOutcome")
        } else {
            listOf("⚔️ Combat initialized: Team A (${clonedA.size} fighters) vs Team B (${clonedB.size} fighters)")
        }

        initialLogs.forEach { combatLog.appendEvent(it) }

        return CombatState(
            round = 1,
            teamAFighters = clonedA,
            teamBFighters = clonedB,
            outcome = initialOutcome,
            logs = initialLogs
        )
    }

    /**
     * Executes a single turn attack between [attacker] and [target],
     * considering special abilities and appending events to [log].
     */
    fun processTurn(
        attacker: Combatant,
        target: Combatant,
        log: CombatLog = combatLog,
        round: Int = 1,
        turnInRound: Int = 1
    ): CombatAction {
        if (!attacker.isAlive || !target.isAlive) {
            val msg = "${attacker.name} cannot attack ${target.name} (one or both are defeated)."
            log.appendEvent(msg)
            return CombatAction(
                round = round,
                turnInRound = turnInRound,
                attackerId = attacker.id,
                attackerName = attacker.name,
                attackerTeam = attacker.team,
                attackerSpeed = attacker.speed,
                targetId = target.id,
                targetName = target.name,
                targetTeam = target.team,
                damageDealt = 0,
                targetRemainingHp = target.hp,
                isFatal = !target.isAlive,
                logMessage = msg
            )
        }

        val ability = attacker.specialAbility
        val useSpecial = attacker.isAbilityReady && ability != null
        val actualDamageDealt: Int
        val logMsg: String

        if (useSpecial) {
            // Trigger special ability
            attacker.mana = 0
            attacker.abilityCooldown = ability.cooldownTurns
            val starMult = when (attacker.starLevel) {
                3 -> 2.0
                2 -> 1.5
                else -> 1.0
            }
            val baseAbilityDmg = if (ability.damage > 0) ability.damage else (attacker.attack * 1.8).toInt()
            val abilityDmg = maxOf(10, (baseAbilityDmg * starMult).toInt() - (target.defense / 3))
            actualDamageDealt = target.takeDamage(abilityDmg)
            val isFatal = !target.isAlive

            logMsg = if (isFatal) {
                "💥 [Special Ability] ${attacker.name} ⭐${attacker.starLevel} casts [${ability.name}] dealing $actualDamageDealt DMG to ${target.name}! 💀 ${target.name} has been defeated!"
            } else {
                "✨ [Special Ability] ${attacker.name} ⭐${attacker.starLevel} casts [${ability.name}] dealing $actualDamageDealt DMG to ${target.name} (${target.hp}/${target.maxHp} HP remaining)."
            }
        } else {
            // Normal attack
            attacker.mana = minOf(attacker.maxMana, attacker.mana + 25)
            if (attacker.abilityCooldown > 0) attacker.abilityCooldown--

            val damage = calculateDamage(attacker, target)
            actualDamageDealt = target.takeDamage(damage)
            val isFatal = !target.isAlive

            logMsg = if (isFatal) {
                "💥 [R$round T$turnInRound] ${attacker.name} ⭐${attacker.starLevel} (Spd ${attacker.speed}, Atk ${attacker.attack}) strikes ${target.name} for $actualDamageDealt DMG! 💀 ${target.name} has been defeated!"
            } else {
                "⚔️ [R$round T$turnInRound] ${attacker.name} ⭐${attacker.starLevel} (Spd ${attacker.speed}, Atk ${attacker.attack}) attacks ${target.name} for $actualDamageDealt DMG (${target.hp}/${target.maxHp} HP remaining)."
            }
        }

        log.appendEvent(logMsg)

        return CombatAction(
            round = round,
            turnInRound = turnInRound,
            attackerId = attacker.id,
            attackerName = attacker.name,
            attackerTeam = attacker.team,
            attackerSpeed = attacker.speed,
            targetId = target.id,
            targetName = target.name,
            targetTeam = target.team,
            damageDealt = actualDamageDealt,
            targetRemainingHp = target.hp,
            isFatal = !target.isAlive,
            logMessage = logMsg,
            isSpecialAbility = useSpecial,
            abilityName = ability?.name
        )
    }

    /**
     * Executes a single turn attack between application [Fighter] models,
     * calculating damage, factoring in special abilities, and logging to [log].
     */
    fun processTurn(
        attacker: Fighter,
        target: Fighter,
        log: CombatLog = combatLog
    ): Int {
        if (!attacker.isAlive || !target.isAlive) return 0

        val ability = attacker.effectiveAbility
        val canCast = attacker.isAbilityReady

        val damageDealt: Int
        if (canCast) {
            attacker.mana = 0
            attacker.resetCooldown(ability.cooldownTurns)
            val starMult = if (attacker.starLevel == 3) 2.0 else if (attacker.starLevel == 2) 1.5 else 1.0
            val baseDmg = if (ability.damage > 0) ability.damage else (attacker.attackPower * 2)
            val abilityDmg = maxOf(10, ((baseDmg * starMult).toInt()) - (target.defense / 3))
            damageDealt = minOf(target.hp, abilityDmg)
            target.hp = (target.hp - damageDealt).coerceAtLeast(0)
            target.health = target.hp

            val logMsg = if (!target.isAlive) {
                "✨ [Special Ability] ${attacker.name} ⭐${attacker.starLevel} casts [${ability.name}] for $damageDealt DMG! 💀 ${target.name} has been defeated!"
            } else {
                "✨ [Special Ability] ${attacker.name} ⭐${attacker.starLevel} casts [${ability.name}] dealing $damageDealt DMG to ${target.name} (${target.hp}/${target.maxHp} HP remaining)!"
            }
            log.appendEvent(logMsg)
        } else {
            attacker.mana = minOf(attacker.maxMana, attacker.mana + 25)
            attacker.reduceCooldown(1)
            val damage = calculateDamage(attacker, target)
            damageDealt = minOf(target.hp, damage)
            target.hp = (target.hp - damageDealt).coerceAtLeast(0)
            target.health = target.hp

            val logMsg = if (!target.isAlive) {
                "⚔️ ${attacker.name} ⭐${attacker.starLevel} strikes ${target.name} for $damageDealt DMG! 💀 ${target.name} has been defeated!"
            } else {
                "⚔️ ${attacker.name} ⭐${attacker.starLevel} attacks ${target.name} for $damageDealt DMG (${target.hp}/${target.maxHp} HP remaining)."
            }
            log.appendEvent(logMsg)
        }
        return damageDealt
    }

    /**
     * Executes a single round of battle between two teams in [state],
     * updating initiative, processing attacks, and logging events to [log].
     */
    fun processTurn(state: CombatState, log: CombatLog = combatLog): CombatState {
        val nextState = simulateRound(state)
        val roundActions = nextState.roundHistory.lastOrNull()?.actions ?: emptyList()
        for (action in roundActions) {
            log.appendEvent(action.logMessage)
        }
        return nextState
    }

    fun processSingleTurn(attacker: Combatant, target: Combatant, log: CombatLog = combatLog): CombatAction =
        processTurn(attacker, target, log)

    fun processSingleTurn(attacker: Fighter, target: Fighter, log: CombatLog = combatLog): Int =
        processTurn(attacker, target, log)

    fun processSingleTurn(state: CombatState, log: CombatLog = combatLog): CombatState =
        processTurn(state, log)

    fun processTurn(teamA: List<Combatant>, teamB: List<Combatant>, log: CombatLog = combatLog): CombatState {
        val state = initCombat(teamA, teamB)
        return processTurn(state, log)
    }

    fun processTurn(state: BattleState, log: CombatLog = combatLog): BattleState {
        val engine = BattleEngine()
        val newState = engine.processTurn(state)
        newState.battleLogs.take(3).reversed().forEach { log.appendEvent(it) }
        return newState
    }

    /**
     * Simulates a single round of combat:
     * 1. Determines turn initiative by sorting all living fighters by [speed] descending.
     * 2. Each living fighter targets and attacks an opposing living fighter.
     * 3. Checks for victory/defeat conditions throughout the round.
     */
    fun simulateRound(state: CombatState): CombatState {
        if (state.isGameOver) return state

        val teamA = state.teamAFighters.map { it.copyCombatant() }.toMutableList()
        val teamB = state.teamBFighters.map { it.copyCombatant() }.toMutableList()
        val damageDealt = state.damageDealtByFighter.toMutableMap()
        val damageTaken = state.damageTakenByFighter.toMutableMap()
        val roundActions = mutableListOf<CombatAction>()
        val newLogs = mutableListOf<String>()

        val initiativeQueue = (teamA.filter { it.isAlive } + teamB.filter { it.isAlive })
            .sortedWith(
                compareByDescending<Combatant> { it.speed }
                    .thenByDescending { it.hp }
                    .thenBy { if (it.team == Team.TEAM_A) 0 else 1 }
                    .thenBy { it.id }
            )

        val startMsg = "--- Round ${state.round} Begins ---"
        newLogs.add(startMsg)
        combatLog.appendEvent(startMsg)

        var turnCounter = 0
        var roundOutcome: BattleOutcome = BattleOutcome.IN_PROGRESS

        for (fighter in initiativeQueue) {
            val currentActor = if (fighter.team == Team.TEAM_A) {
                teamA.find { it.id == fighter.id }
            } else {
                teamB.find { it.id == fighter.id }
            } ?: continue

            if (!currentActor.isAlive) continue

            val opponentPool = if (currentActor.team == Team.TEAM_A) teamB else teamA
            val aliveOpponents = opponentPool.filter { it.isAlive }

            if (aliveOpponents.isEmpty()) {
                roundOutcome = if (currentActor.team == Team.TEAM_A) {
                    BattleOutcome.TEAM_A_VICTORY
                } else {
                    BattleOutcome.TEAM_B_VICTORY
                }
                break
            }

            val target = selectTarget(aliveOpponents, targetingStrategy) ?: continue
            turnCounter++

            // Process turn action for actor and target
            val action = processTurn(currentActor, target, combatLog, state.round, turnCounter)
            newLogs.add(action.logMessage)
            roundActions.add(action)

            damageDealt[currentActor.id] = (damageDealt[currentActor.id] ?: 0) + action.damageDealt
            damageTaken[target.id] = (damageTaken[target.id] ?: 0) + action.damageDealt

            val remainingOpponents = opponentPool.count { it.isAlive }
            if (remainingOpponents == 0) {
                roundOutcome = if (currentActor.team == Team.TEAM_A) {
                    BattleOutcome.TEAM_A_VICTORY
                } else {
                    BattleOutcome.TEAM_B_VICTORY
                }
                break
            }
        }

        val aliveA = teamA.count { it.isAlive }
        val aliveB = teamB.count { it.isAlive }

        val finalOutcome = when {
            roundOutcome != BattleOutcome.IN_PROGRESS -> roundOutcome
            aliveA == 0 && aliveB == 0 -> BattleOutcome.DRAW
            aliveA == 0 -> BattleOutcome.TEAM_B_VICTORY
            aliveB == 0 -> BattleOutcome.TEAM_A_VICTORY
            state.round >= maxRounds -> {
                val hpA = teamA.filter { it.isAlive }.sumOf { it.hp }
                val hpB = teamB.filter { it.isAlive }.sumOf { it.hp }
                when {
                    hpA > hpB -> {
                        val m = "⏱️ Max rounds reached! Team A wins by higher total HP ($hpA vs $hpB)."
                        newLogs.add(m)
                        combatLog.appendEvent(m)
                        BattleOutcome.TEAM_A_VICTORY
                    }
                    hpB > hpA -> {
                        val m = "⏱️ Max rounds reached! Team B wins by higher total HP ($hpB vs $hpA)."
                        newLogs.add(m)
                        combatLog.appendEvent(m)
                        BattleOutcome.TEAM_B_VICTORY
                    }
                    else -> {
                        val m = "⏱️ Max rounds reached! Tie in total HP ($hpA vs $hpB). Draw!"
                        newLogs.add(m)
                        combatLog.appendEvent(m)
                        BattleOutcome.DRAW
                    }
                }
            }
            else -> BattleOutcome.IN_PROGRESS
        }

        if (finalOutcome != BattleOutcome.IN_PROGRESS) {
            val endMsg = "🏆 Battle Ended: $finalOutcome"
            newLogs.add(endMsg)
            combatLog.appendEvent(endMsg)
        }

        val completedRound = CombatRound(
            roundNumber = state.round,
            actions = roundActions,
            teamASurvivors = aliveA,
            teamBSurvivors = aliveB
        )

        return state.copy(
            round = state.round + 1,
            teamAFighters = teamA,
            teamBFighters = teamB,
            actionHistory = state.actionHistory + roundActions,
            roundHistory = state.roundHistory + completedRound,
            logs = state.logs + newLogs,
            outcome = finalOutcome,
            damageDealtByFighter = damageDealt,
            damageTakenByFighter = damageTaken
        )
    }

    private fun selectTarget(
        aliveOpponents: List<Combatant>,
        strategy: TargetingStrategy
    ): Combatant? {
        if (aliveOpponents.isEmpty()) return null
        return when (strategy) {
            TargetingStrategy.FIRST_ALIVE -> aliveOpponents.first()
            TargetingStrategy.LOWEST_HP -> aliveOpponents.minByOrNull { it.hp }
            TargetingStrategy.HIGHEST_ATTACK -> aliveOpponents.maxByOrNull { it.attack }
            TargetingStrategy.HIGHEST_SPEED -> aliveOpponents.maxByOrNull { it.speed }
            TargetingStrategy.RANDOM -> aliveOpponents[random.nextInt(aliveOpponents.size)]
        }
    }

    fun simulate(teamA: List<Combatant>, teamB: List<Combatant>): CombatResult {
        var state = initCombat(teamA, teamB)
        while (!state.isGameOver) {
            state = simulateRound(state)
        }

        val allFighters = state.teamAFighters + state.teamBFighters
        val mvp = allFighters.maxByOrNull { state.damageDealtByFighter[it.id] ?: 0 }

        val winnerTeam = when (state.outcome) {
            BattleOutcome.TEAM_A_VICTORY -> Team.TEAM_A
            BattleOutcome.TEAM_B_VICTORY -> Team.TEAM_B
            else -> null
        }

        return CombatResult(
            outcome = state.outcome,
            winnerTeam = winnerTeam,
            totalRounds = state.round - 1,
            totalActions = state.actionHistory.size,
            teamAFighters = state.teamAFighters,
            teamBFighters = state.teamBFighters,
            survivingTeamA = state.survivingTeamA,
            survivingTeamB = state.survivingTeamB,
            logs = state.logs,
            actionHistory = state.actionHistory,
            roundHistory = state.roundHistory,
            damageDealtByFighter = state.damageDealtByFighter,
            damageTakenByFighter = state.damageTakenByFighter,
            mvp = mvp
        )
    }

    fun simulateBattle(teamA: List<Fighter>, teamB: List<Fighter>): CombatResult {
        val combatantsA = teamA.map { it.toCombatant(Team.TEAM_A) }
        val combatantsB = teamB.map { it.toCombatant(Team.TEAM_B) }
        return simulate(combatantsA, combatantsB)
    }

    companion object {
        private val defaultEngine = CombatEngine()

        /**
         * Converts an application [Fighter] into a [Combatant] extracting all attributes.
         */
        fun Fighter.toCombatant(team: Team = Team.TEAM_A): Combatant {
            val atk = maxOf(physAttack, attackPower, damage, magAttack)
            val spd = attackSpeed
            val hpVal = if (hp > 0) hp else maxOf(1, health)
            val maxHpVal = maxOf(maxHp, hpVal)
            val defVal = maxOf(physDefense, defense, magDefense)

            return Combatant(
                id = id,
                name = name,
                hp = hpVal,
                maxHp = maxHpVal,
                attack = atk,
                speed = spd,
                defense = defVal,
                team = team,
                cost = cost,
                starLevel = starLevel,
                mana = mana,
                maxMana = maxMana,
                specialAbility = effectiveAbility,
                abilityCooldown = abilityCooldown
            )
        }

        /**
         * Creates a stock 1-star hero calibrated such that 5-gold heroes
         * have stats very close to 3-star 1-gold heroes.
         *
         * Stats balance:
         * 1-gold 1-star: ~140 HP, ~21 Attack, ~12 Defense
         * 1-gold 3-star (via upgrade): ~560 HP, ~84 Attack, ~26 Defense
         * 5-gold 1-star: ~650 HP, ~88 Attack, ~45 Defense
         */
        fun createStockHero(cost: Int, name: String = "Hero (Cost $cost)"): Combatant {
            return when (cost) {
                1 -> Combatant(name = name, hp = 140, attack = 21, speed = 10, defense = 12, cost = 1, starLevel = 1)
                2 -> Combatant(name = name, hp = 170, attack = 35, speed = 15, defense = 14, cost = 2, starLevel = 1)
                3 -> Combatant(name = name, hp = 240, attack = 55, speed = 12, defense = 20, cost = 3, starLevel = 1)
                4 -> Combatant(name = name, hp = 450, attack = 75, speed = 11, defense = 35, cost = 4, starLevel = 1)
                5 -> Combatant(name = name, hp = 650, attack = 88, speed = 12, defense = 45, cost = 5, starLevel = 1)
                else -> Combatant(name = name, hp = 140, attack = 21, speed = 10, defense = 12, cost = cost, starLevel = 1)
            }
        }

        /**
         * Upgrades a fighter to the target star level, multiplying stats.
         */
        fun upgradeCombatant(stock: Combatant, targetStar: Int = stock.starLevel + 1): Combatant {
            val multiplier = when (targetStar) {
                3 -> 4.0
                2 -> 2.0
                else -> 1.0
            }
            val baseDivisor = when (stock.starLevel) {
                3 -> 4.0
                2 -> 2.0
                else -> 1.0
            }
            val baseHp = (stock.maxHp / baseDivisor).toInt()
            val baseAtk = (stock.attack / baseDivisor).toInt()
            val newHp = (baseHp * multiplier).toInt()
            val newAtk = (baseAtk * multiplier).toInt()
            val newDef = (stock.defense * (if (targetStar == 3) 2.2 else if (targetStar == 2) 1.5 else 1.0)).toInt()

            return stock.copy(
                starLevel = targetStar,
                hp = newHp,
                maxHp = newHp,
                attack = newAtk,
                defense = newDef
            )
        }

        fun simulate(teamA: List<Combatant>, teamB: List<Combatant>): CombatResult =
            defaultEngine.simulate(teamA, teamB)

        fun simulateBattle(teamA: List<Fighter>, teamB: List<Fighter>): CombatResult =
            defaultEngine.simulateBattle(teamA, teamB)
    }
}
