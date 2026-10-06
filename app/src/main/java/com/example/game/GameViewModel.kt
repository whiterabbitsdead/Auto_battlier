package com.example.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil

class GameViewModel : ViewModel() {
    private val poolManager = FighterPool()

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    private fun startPrepTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(prepTimer = 30) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.prepTimer > 0 && !_uiState.value.isBattleActive && _uiState.value.battleResult == null) {
                delay(1000)
                val currentTimer = _uiState.value.prepTimer
                if (currentTimer <= 1) {
                    _uiState.update { it.copy(prepTimer = 0) }
                    startBattle()
                    break
                } else {
                    _uiState.update { it.copy(prepTimer = currentTimer - 1) }
                }
            }
        }
    }

    private fun applyCommanderBuffs(fighters: List<Fighter>, commander: Commander?): List<Fighter> {
        if (commander == null) return fighters
        return fighters.map { f ->
            var newF = f.copy()
            
            // Apply formal Passive Ability if exists
            commander.passiveAbility?.let { passive ->
                when (passive.type) {
                    CommanderPassiveType.STAT_BOOST_PHYS_ATK -> newF.physAttack += passive.value.toInt()
                    CommanderPassiveType.STAT_BOOST_MAG_ATK -> newF.magAttack += passive.value.toInt()
                    CommanderPassiveType.STAT_BOOST_DEF -> {
                        newF.physDefense += passive.value.toInt()
                        newF.magDefense += passive.value.toInt()
                    }
                    CommanderPassiveType.STAT_BOOST_HP -> {
                        newF.maxHp += passive.value.toInt()
                        newF.hp += passive.value.toInt()
                    }
                    CommanderPassiveType.DODGE_CHANCE_BOOST -> {
                        // Apply only to preferred faction if it matches, otherwise all?
                        // Let's say it applies to all for now as a baseline commander passive
                        newF.dodgeChance += passive.value.toDouble()
                    }
                    else -> {}
                }
            }

            // Legacy/Class-based generic buffs
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
            if (f.faction == commander.preferredFaction) {
                newF.physAttack += 5
                newF.magAttack += 5
                newF.maxHp += 30
                newF.hp += 30
            }
            newF
        }
    }

    fun openUnlockDialog(commander: Commander) {
        _uiState.update { it.copy(showUnlockDialogFor = commander, unlockFeedbackMessage = null) }
    }

    fun dismissUnlockDialog() {
        _uiState.update { it.copy(showUnlockDialogFor = null, unlockFeedbackMessage = null) }
    }

    fun openDemoCashDialog(commander: Commander) {
        _uiState.update { it.copy(showDemoCashDialogFor = commander) }
    }

    fun dismissDemoCashDialog() {
        _uiState.update { it.copy(showDemoCashDialogFor = null) }
    }

    fun unlockWithGameplayCredits(commander: Commander) {
        val state = _uiState.value
        val info = CommanderUnlockManager.UNLOCK_CATALOG[commander.name] ?: return
        if (state.battleCredits >= info.unlockCreditCost) {
            val updatedUnlocked = state.unlockedCommanderNames + commander.name
            _uiState.update { it.copy(
                battleCredits = it.battleCredits - info.unlockCreditCost,
                unlockedCommanderNames = updatedUnlocked,
                showUnlockDialogFor = null,
                unlockFeedbackMessage = "Unlocked ${commander.name} via Gameplay Credits!"
            ) }
        } else {
            val needed = info.unlockCreditCost - state.battleCredits
            _uiState.update { it.copy(
                unlockFeedbackMessage = "Need $needed more Battle Credits to unlock via gameplay!"
            ) }
        }
    }

    fun unlockWithCashPurchase(commander: Commander) {
        val state = _uiState.value
        val updatedUnlocked = state.unlockedCommanderNames + commander.name
        _uiState.update { it.copy(
            unlockedCommanderNames = updatedUnlocked,
            showUnlockDialogFor = null,
            showDemoCashDialogFor = null,
            unlockFeedbackMessage = "[Demo Purchase Verified] Successfully unlocked ${commander.name}!"
        ) }
    }

    fun addDemoCredits(amount: Int = 100) {
        _uiState.update { it.copy(
            battleCredits = it.battleCredits + amount,
            unlockFeedbackMessage = "Added $amount Demo Battle Credits!"
        ) }
    }

    fun selectCommander(commander: Commander) {
        val state = _uiState.value
        if (!state.unlockedCommanderNames.contains(commander.name)) {
            // Locked commander: open unlock dialog
            openUnlockDialog(commander)
            return
        }

        poolManager.resetPool()
        var playerFighters = (0..2).map { generateFighterByCost(1, isPlayer = true, yPos = (4..7).random()) }
        playerFighters = applyCommanderBuffs(playerFighters, commander)
        val mergeResult = poolManager.processMerges(playerFighters, emptyList(), emptyList()) { applyCommanderBuffs(it, commander) }
        playerFighters = mergeResult.first
        
        val initialAis = AiOpponentManager.createInitialAiPlayers(commander, poolManager)
        val firstOpponent = initialAis.first()
        val opponentFighters = firstOpponent.boardFighters.map {
            it.copy(isPlayer = false, hp = it.maxHp, x = it.startX, y = it.startY)
        }

        val active = SynergyCalculator.calculateSynergies(playerFighters)
        val activeSkills = SynergyCalculator.getStackedSynergySkills(active)
        
        _uiState.update { s ->
            s.copy(
                currentScreen = Screen.BATTLE_BOARD,
                playerCommander = commander,
                aiCommander = firstOpponent.commander.copy(hp = firstOpponent.hp),
                aiPlayers = initialAis,
                currentOpponentAiId = firstOpponent.id,
                playerFighters = playerFighters,
                aiFighters = opponentFighters,
                playerGold = 10,
                shopFighters = poolManager.rollShop(3),
                activeSynergies = active,
                activeSynergySkills = activeSkills,
                showUnlockDialogFor = null
            )
        }
        startPrepTimer()
    }

    private fun endBattle(result: BattleResult, state: GameState) {
        val isBossRound = (state.round in 2..4 && state.fight == 3)
        val isWin = result == BattleResult.PLAYER_WIN
        val gainedExp = if (isBossRound) 10 else 5
        
        val newWinStreak = if (isWin) state.winStreak + 1 else 0
        val newLoseStreak = if (!isWin) state.loseStreak + 1 else 0
        
        val streakGold = when {
            newWinStreak >= 5 -> 3
            newWinStreak >= 3 -> 2
            newWinStreak >= 2 -> 1
            newLoseStreak >= 5 -> 3
            newLoseStreak >= 3 -> 2
            newLoseStreak >= 2 -> 1
            else -> 0
        }

        // Economy: Basic + Victory + Interest + Streaks
        val baseGold = 5
        val victoryGold = if (isWin) 1 else 0
        val interest = minOf(4, (state.playerGold / 10) * 2)
        val gainedGold = baseGold + victoryGold + interest + streakGold

        val (newLevel, newExp) = CommanderLeveling.addPassiveExp(state.playerLevel, state.playerExp, gainedExp)
        val newFighters = state.preBattleFighters.map { it.copy() } // Restore to pre-combat stats

        // Commander Damage
        val survivingUnits = if (isWin) state.playerFighters.filter { it.isAlive && it.startY >= 0 } else state.aiFighters.filter { it.isAlive }
        val survivingStars = survivingUnits.sumOf { it.starLevel }
        val baseDamage = when (state.round) {
            1 -> 3
            2 -> 5
            3 -> 7
            else -> 11
        }
        val totalDamage = baseDamage + survivingStars

        var pCommander = state.playerCommander
        var aCommander = state.aiCommander
        val updatedAiPlayers = state.aiPlayers.map { it.copy() }.toMutableList()
        val lobbyLogs = mutableListOf<String>()

        var resultText = ""
        val currentOpponent = updatedAiPlayers.find { it.id == state.currentOpponentAiId }

        if (isWin) {
            if (currentOpponent != null) {
                val newHp = maxOf(0, currentOpponent.hp - totalDamage)
                val idx = updatedAiPlayers.indexOfFirst { it.id == currentOpponent.id }
                val elim = if (newHp == 0) " 💀 ${currentOpponent.name} was ELIMINATED!" else ""
                updatedAiPlayers[idx] = currentOpponent.copy(
                    hp = newHp,
                    loseStreak = currentOpponent.loseStreak + 1,
                    winStreak = 0
                )
                aCommander = aCommander?.copy(hp = newHp)
                resultText = "Player Defeated ${currentOpponent.name}! Opponent took $totalDamage DMG.$elim"
            } else {
                aCommander = aCommander?.copy(hp = maxOf(0, aCommander.hp - totalDamage))
                resultText = "Player Victory! Boss took $totalDamage DMG."
            }
        } else {
            pCommander = pCommander?.copy(hp = maxOf(0, pCommander.hp - totalDamage))
            if (currentOpponent != null) {
                val idx = updatedAiPlayers.indexOfFirst { it.id == currentOpponent.id }
                updatedAiPlayers[idx] = currentOpponent.copy(
                    winStreak = currentOpponent.winStreak + 1,
                    loseStreak = 0,
                    gold = currentOpponent.gold + 1
                )
                resultText = "${currentOpponent.name} Defeated Player! Player took $totalDamage DMG."
            } else {
                resultText = "Defeated by Boss! Player took $totalDamage DMG."
            }
        }

        // Simulate battle between other alive AI opponents in the lobby
        val otherAliveAis = updatedAiPlayers.filter { it.isAlive && it.id != state.currentOpponentAiId }
        if (otherAliveAis.size >= 2) {
            val (ai1, ai2, simLog) = AiOpponentManager.simulateAiVsAiBattle(otherAliveAis[0], otherAliveAis[1], state.round)
            val idx1 = updatedAiPlayers.indexOfFirst { it.id == ai1.id }
            val idx2 = updatedAiPlayers.indexOfFirst { it.id == ai2.id }
            if (idx1 != -1) updatedAiPlayers[idx1] = ai1
            if (idx2 != -1) updatedAiPlayers[idx2] = ai2
            if (simLog.isNotEmpty()) lobbyLogs.add(simLog)
        }

        val streakText = if (streakGold > 0) " + $streakGold from Streaks" else ""
        val earnedCredits = if (isWin) 35 else 15
        val lootText = "Gained $gainedExp XP, $gainedGold Gold (inc. $interest Interest$streakText), +$earnedCredits Battle Credits."
        
        val nextShop = if (state.shopLocked) state.shopFighters else poolManager.rollShop(newLevel)

        // Record round history for player and commander matchup
        val pName = pCommander?.name?.substringBefore(" (") ?: "Player"
        val oppName = currentOpponent?.name ?: aCommander?.name ?: "Boss"
        val newRoundResults = listOf(
            CommanderRoundResult(
                round = state.round,
                fight = state.fight,
                commanderName = pName,
                isWin = isWin,
                remainingHp = pCommander?.hp ?: 0,
                isPlayer = true
            ),
            CommanderRoundResult(
                round = state.round,
                fight = state.fight,
                commanderName = oppName,
                isWin = !isWin,
                remainingHp = currentOpponent?.hp ?: aCommander?.hp ?: 0,
                isPlayer = false
            )
        )

        _uiState.update { it.copy(
            isBattleActive = false, 
            battleResult = result, 
            battleLogs = (listOf(lootText, resultText) + lobbyLogs + it.battleLogs).take(MAX_COMBAT_LOG_HISTORY),
            playerExp = newExp,
            playerLevel = newLevel,
            playerGold = it.playerGold + gainedGold,
            battleCredits = it.battleCredits + earnedCredits,
            playerFighters = newFighters,
            shopFighters = nextShop,
            shopLocked = false, // unlock after roll
            playerCommander = pCommander,
            aiCommander = aCommander,
            aiPlayers = updatedAiPlayers,
            winStreak = newWinStreak,
            loseStreak = newLoseStreak,
            roundHistory = it.roundHistory + newRoundResults
        ) }
    }

    fun startBattle() {
        if (_uiState.value.isBattleActive) return
        timerJob?.cancel()
        
        // Auto-fill board
        var currentFighters = _uiState.value.playerFighters.map { it.copy() }
        val playerLevel = _uiState.value.playerLevel
        val boardCount = currentFighters.count { it.startY >= 0 }
        
        if (boardCount < playerLevel) {
            val benchFighters = currentFighters.filter { it.startY == -1 }.sortedByDescending { it.starLevel * 10 + it.cost }
            var added = 0
            val needed = playerLevel - boardCount
            
            val emptySpots = mutableListOf<Pair<Int, Int>>()
            for (y in 4..7) {
                for (x in 0..7) {
                    if (currentFighters.none { it.startX == x && it.startY == y }) {
                        emptySpots.add(Pair(x, y))
                    }
                }
            }
            
            val newFighters = currentFighters.toMutableList()
            for (fighter in benchFighters) {
                if (added >= needed || emptySpots.isEmpty()) break
                val spot = emptySpots.removeAt(0)
                val idx = newFighters.indexOfFirst { it.id == fighter.id }
                if (idx != -1) {
                    newFighters[idx] = newFighters[idx].copy(x = spot.first, y = spot.second, startX = spot.first, startY = spot.second)
                    added++
                }
            }
            currentFighters = newFighters
        }

        // Auto-equip items
        var currentInventory = _uiState.value.playerInventory.toMutableList()
        if (currentInventory.isNotEmpty()) {
            val sortedFighters = currentFighters.sortedByDescending { it.starLevel * 10 + it.cost }
            val newFighters = currentFighters.toMutableList()
            
            for (fighter in sortedFighters) {
                if (currentInventory.isEmpty()) break
                
                var f = fighter
                val idx = newFighters.indexOfFirst { it.id == fighter.id }
                
                while (f.equipment.size < 3 && currentInventory.isNotEmpty()) {
                    val eq = currentInventory.removeAt(0)
                    var newPAtk = f.physAttack
                    var newPDef = f.physDefense
                    var newMaxHp = f.maxHp
                    var newHp = f.hp
                    var newDodge = f.dodgeChance
                    when (eq.type) {
                        EquipmentType.ATTACK -> newPAtk += 10
                        EquipmentType.DEFENSE -> newPDef += 10
                        EquipmentType.HP -> { newMaxHp += 50; newHp += 50 }
                        EquipmentType.DODGE -> newDodge += 0.10
                    }
                    f = f.copy(
                        equipment = f.equipment + eq,
                        physAttack = newPAtk,
                        physDefense = newPDef,
                        maxHp = newMaxHp,
                        hp = newHp,
                        dodgeChance = newDodge
                    )
                }
                if (idx != -1) newFighters[idx] = f
            }
            currentFighters = newFighters
        }

        val preBattle = currentFighters.map { it.copy() }
        val buffed = SynergyCalculator.applySynergyBuffs(preBattle)

        _uiState.update { it.copy(
            isBattleActive = true, 
            battleLogs = listOf("Battle Started!"), 
            battleResult = null,
            preBattleFighters = preBattle,
            playerFighters = buffed,
            playerInventory = currentInventory,
            playerCommanderEnergy = 25,
            aiCommanderEnergy = 15,
            lastTacticalSkillCast = null
        ) }
        
        viewModelScope.launch {
            while (_uiState.value.isBattleActive) {
                delay(800)
                simulateTick()
            }
        }
    }

    fun stepBattleTurn() {
        if (!_uiState.value.isBattleActive && _uiState.value.battleResult == null) {
            startBattle()
        } else if (_uiState.value.isBattleActive) {
            simulateTick()
        }
    }

    fun toggleAdmin() {
        _uiState.update { it.copy(isAdmin = !it.isAdmin) }
    }

    fun navigateToBattleVisualization() {
        _uiState.update { it.copy(currentScreen = Screen.BATTLE_VISUALIZATION) }
    }

    fun navigateToBattleBoard() {
        _uiState.update { it.copy(currentScreen = Screen.BATTLE_BOARD) }
    }

    fun simulateTick() {
        val state = _uiState.value
        val playerBoardFighters = state.playerFighters.filter { it.startY >= 0 }
        val allFighters = (playerBoardFighters + state.aiFighters).filter { it.isAlive }
        
        if (playerBoardFighters.none { it.isAlive }) {
            endBattle(BattleResult.AI_WIN, state)
            return
        }
        if (state.aiFighters.none { it.isAlive }) {
            endBattle(BattleResult.PLAYER_WIN, state)
            return
        }

        val orderedFighters = allFighters.sortedByDescending { it.attackSpeed }
        val newPlayerFighters = state.playerFighters.map { it.copy() }.toMutableList()
        val newAiFighters = state.aiFighters.map { it.copy() }.toMutableList()
        var newLogs = state.battleLogs
        val newDamageEffects = state.damageEffects.filter { System.currentTimeMillis() - it.timestamp < 1500 }.toMutableList()

        // Tick cooldowns at start of simulation tick
        newPlayerFighters.forEach { if (it.isAlive && it.startY >= 0) it.reduceCooldown(1) }
        newAiFighters.forEach { if (it.isAlive) it.reduceCooldown(1) }

        // Resource Bar: Tactical Energy accumulation
        var pEnergy = minOf(100, state.playerCommanderEnergy + 10)
        var aEnergy = minOf(100, state.aiCommanderEnergy + 8)

        // AI Commander Tactical Decision Making (Activates when Energy fills up!)
        if (aEnergy >= 100 && newAiFighters.any { it.isAlive }) {
            val aCmd = state.aiCommander
            val ultName = aCmd?.activeAbility?.name ?: "Dominion Cataclysm"
            val ultDmg = aCmd?.activeAbility?.value ?: 50
            newPlayerFighters.filter { it.isAlive && it.startY >= 0 }.forEach { target ->
                target.hp -= ultDmg
                target.health = target.hp
                newDamageEffects.add(DamageEffect(x = target.x, y = target.y, damage = ultDmg))
            }
            newLogs = listOf("🔥 [ENEMY COMMANDER ULTIMATE] ${aCmd?.name ?: "Opponent"} unleashes [$ultName] for $ultDmg AOE DMG!") + newLogs
            aEnergy -= 100
        } else if (aEnergy >= 50 && newAiFighters.filter { it.isAlive }.any { it.hp < it.maxHp * 0.40f }) {
            // AI Emergency Tactical Rally
            newAiFighters.filter { it.isAlive }.forEach { ally ->
                ally.hp = minOf(ally.maxHp, ally.hp + 35)
                ally.health = ally.hp
                newDamageEffects.add(DamageEffect(x = ally.x, y = ally.y, damage = -35))
            }
            newLogs = listOf("🛡️ [ENEMY TACTICAL RALLY] ${state.aiCommander?.name ?: "Opponent"} rallies army for +35 HP!") + newLogs
            aEnergy -= 50
        }

        for (fighter in orderedFighters) {
            val isPlayer = fighter.isPlayer
            val allies = if (isPlayer) newPlayerFighters else newAiFighters
            val enemies = if (isPlayer) newAiFighters else newPlayerFighters
            
            val currentFighter = allies.find { it.id == fighter.id } ?: continue
            if (!currentFighter.isAlive || currentFighter.startY < 0) continue
            
            val aliveEnemies = enemies.filter { it.isAlive && it.startY >= 0 }
            if (aliveEnemies.isEmpty()) break

            val target = aliveEnemies.minByOrNull { CombatCalculator.distance(currentFighter, it) } ?: continue
            val dist = CombatCalculator.distance(currentFighter, target)

            val ability = currentFighter.effectiveAbility
            val canCastSpecial = currentFighter.abilityCooldown <= 0 && (currentFighter.mana >= ability.manaCost || currentFighter.mana >= 100)
            val isSupportAbility = ability.targetType in listOf(AbilityTargetType.ALLY_SINGLE, AbilityTargetType.ALLY_ALL, AbilityTargetType.SELF) &&
                    (ability.effectType == AbilityEffectType.HEAL || ability.healAmount > 0 || ability.shieldAmount > 0)

            if (canCastSpecial && (isSupportAbility || dist <= currentFighter.attackRange)) {
                currentFighter.mana = 0
                currentFighter.resetCooldown(ability.cooldownTurns)

                when {
                    ability.effectType == AbilityEffectType.HEAL || ability.healAmount > 0 -> {
                        val healVal = maxOf(25, ability.healAmount)
                        val livingAllies = allies.filter { it.isAlive && it.startY >= 0 }
                        val healTarget = if (ability.targetType == AbilityTargetType.SELF) currentFighter else (livingAllies.minByOrNull { it.hp.toFloat() / maxOf(1, it.maxHp) } ?: currentFighter)
                        val prevHp = healTarget.hp
                        healTarget.hp = minOf(healTarget.maxHp, healTarget.hp + healVal)
                        healTarget.health = healTarget.hp
                        val actual = healTarget.hp - prevHp
                        newLogs = listOf("💚 ${currentFighter.name} casts [${ability.name}] healing ${healTarget.name} for +$actual HP!") + newLogs
                        newDamageEffects.add(DamageEffect(x = healTarget.x, y = healTarget.y, damage = -actual))
                        if (ability.damage > 0 && target.isAlive) {
                            target.hp -= ability.damage
                            newLogs = listOf("✨ [${ability.name}] smites ${target.name} for ${ability.damage} DMG!") + newLogs
                            newDamageEffects.add(DamageEffect(x = target.x, y = target.y, damage = ability.damage))
                            if (!target.isAlive) newLogs = listOf("💀 ${target.name} is defeated!") + newLogs
                        }
                    }
                    ability.effectType == AbilityEffectType.MULTI_ATTACK || ability.multiAttackHits > 1 -> {
                        val hits = maxOf(2, ability.multiAttackHits)
                        val hitDmg = maxOf(5, (ability.damage.takeIf { it > 0 } ?: (currentFighter.damage * 2)) / hits)
                        newLogs = listOf("⚡ ${currentFighter.name} unleashes [${ability.name}] ($hits-strike combo)!") + newLogs
                        for (i in 1..hits) {
                            val activeTarget = if (target.isAlive) target else (enemies.firstOrNull { it.isAlive && it.startY >= 0 } ?: break)
                            activeTarget.hp -= hitDmg
                            activeTarget.health = activeTarget.hp
                            newLogs = listOf("💥 Combo $i/$hits: ${currentFighter.name} strikes ${activeTarget.name} for $hitDmg DMG!") + newLogs
                            newDamageEffects.add(DamageEffect(x = activeTarget.x, y = activeTarget.y, damage = hitDmg))
                            if (!activeTarget.isAlive) {
                                newLogs = listOf("💀 ${activeTarget.name} is defeated!") + newLogs
                            }
                        }
                    }
                    else -> {
                        val ultDmg = maxOf(ability.damage, currentFighter.magAttack * 2 + 50)
                        target.hp -= ultDmg
                        target.health = target.hp
                        newLogs = listOf("✨ ${currentFighter.name} unleashes [${ability.name}]! Deals $ultDmg DMG to ${target.name}!") + newLogs
                        newDamageEffects.add(DamageEffect(x = target.x, y = target.y, damage = ultDmg, isCrit = true))
                        
                        // Stacked Synergy Skill: Illumins (4) Supernova Cataclysm
                        if (state.activeSynergies.any { it.startsWith("Illumins (4)") } && currentFighter.isPlayer) {
                            val novaDmg = 50
                            enemies.filter { it.isAlive }.forEach { 
                                it.hp -= novaDmg; it.health = it.hp
                                newDamageEffects.add(DamageEffect(x = it.x, y = it.y, damage = novaDmg))
                            }
                            newLogs = listOf("✨ [Illumins (4) Supernova] Cosmic cataclysm strikes all enemies for $novaDmg DMG!") + newLogs
                        }
                        if (!target.isAlive) {
                            newLogs = listOf("💀 ${target.name} is defeated!") + newLogs
                        }
                    }
                }
            } else if (dist <= currentFighter.attackRange) {
                val result = CombatCalculator.calculateAttack(currentFighter, target)
                newLogs = listOf(result.logMessage) + newLogs
                
                if (result.isHit && !result.isDodge) {
                    var finalDmg = result.damage

                    // Stacked Synergy Skill: Knuckle Power (4) Dragon Wrath Burst
                    if (currentFighter.fighterClass == FighterClass.KNUCKLE_POWER && state.activeSynergies.any { it.startsWith("Knuckle Power (4)") } && currentFighter.isPlayer) {
                        finalDmg += 35
                        newLogs = listOf("🐉 [Dragon Wrath Burst] ${currentFighter.name}'s punch detonates for +35 AOE DMG!") + newLogs
                    } else if (currentFighter.fighterClass == FighterClass.KNUCKLE_POWER && state.activeSynergies.any { it.startsWith("Knuckle Power (2)") } && currentFighter.isPlayer) {
                        if (Math.random() < 0.40) {
                            finalDmg += 20
                            newLogs = listOf("💥 [Shockwave Strike] ${currentFighter.name} unleashes sonic shockwave (+20 DMG)!") + newLogs
                        }
                    }

                    // Stacked Synergy Skill: Reptilians (3) Acidic Toxic Spores
                    if (currentFighter.faction == Faction.REPTILIANS && state.activeSynergies.any { it.startsWith("Reptilians (3)") } && currentFighter.isPlayer) {
                        finalDmg += 25
                        newLogs = listOf("🧪 [Acidic Toxic Spores] ${target.name} takes 25 venom True DMG!") + newLogs
                    }

                    // Stacked Synergy Skill: Elemos (2 or 3) Static Current
                    if (currentFighter.faction == Faction.ELEMOS && currentFighter.isPlayer) {
                        val secondEnemy = enemies.firstOrNull { it.isAlive && it.id != target.id }
                        if (secondEnemy != null) {
                            val sparkDmg = if (state.activeSynergies.any { it.startsWith("Elemos (3)") }) 35 else 20
                            secondEnemy.hp -= sparkDmg
                            newLogs = listOf("⚡ [Static Current] Spark zaps ${secondEnemy.name} for $sparkDmg DMG!") + newLogs
                            newDamageEffects.add(DamageEffect(x = secondEnemy.x, y = secondEnemy.y, damage = sparkDmg))
                        }
                    }

                    target.hp -= finalDmg
                    newDamageEffects.add(DamageEffect(x = target.x, y = target.y, damage = finalDmg, isCrit = result.isCrit))
                    currentFighter.mana = minOf(100, currentFighter.mana + 15)
                    target.mana = minOf(100, target.mana + 10)

                    // Stacked Synergy Skill: Tuls (3) Spiked Bastion Retribution
                    if (target.faction == Faction.TULS && state.activeSynergies.any { it.startsWith("Tuls (3)") } && target.isPlayer) {
                        val thornsDmg = maxOf(8, (finalDmg * 0.30).toInt())
                        currentFighter.hp -= thornsDmg
                        newLogs = listOf("🛡️ [Spiked Bastion Retribution] ${target.name} reflects $thornsDmg thorns DMG to ${currentFighter.name}!") + newLogs
                        newDamageEffects.add(DamageEffect(x = currentFighter.x, y = currentFighter.y, damage = thornsDmg))
                    }
                    
                    if (!target.isAlive) {
                        newLogs = listOf("💀 ${target.name} is defeated!") + newLogs
                    }
                } else if (result.isDodge) {
                    // Stacked Synergy Skill: Feet Work (2) Mach Counter-Strike
                    if (target.faction == Faction.FEET_WORK && state.activeSynergies.any { it.startsWith("Feet Work (2)") } && target.isPlayer) {
                        currentFighter.hp -= 40
                        newLogs = listOf("⚡ [Mach Counter-Strike] ${target.name} dodged and counter-kicked ${currentFighter.name} for 40 DMG!") + newLogs
                        newDamageEffects.add(DamageEffect(x = currentFighter.x, y = currentFighter.y, damage = 40))
                        if (!currentFighter.isAlive) {
                            newLogs = listOf("💀 ${currentFighter.name} is defeated!") + newLogs
                        }
                    }
                }
            } else {
                val dx = target.x - currentFighter.x
                val dy = target.y - currentFighter.y
                if (abs(dx) > abs(dy)) {
                    currentFighter.x += if (dx > 0) 1 else -1
                } else {
                    currentFighter.y += if (dy > 0) 1 else -1
                }
                
                if ((allies + enemies).filter { it.isAlive }.count { it.x == currentFighter.x && it.y == currentFighter.y } > 1) {
                    if (abs(dx) > abs(dy)) {
                        currentFighter.x -= if (dx > 0) 1 else -1
                    } else {
                        currentFighter.y -= if (dy > 0) 1 else -1
                    }
                }
            }
        }

        _uiState.update { it.copy(
            playerFighters = newPlayerFighters,
            aiFighters = newAiFighters,
            battleLogs = newLogs.take(MAX_COMBAT_LOG_HISTORY),
            damageEffects = newDamageEffects,
            playerCommanderEnergy = pEnergy,
            aiCommanderEnergy = aEnergy
        ) }
    }

    /**
     * Active Tactical Decision-Making: Player activates a commander skill once the resource bar has enough energy!
     */
    fun activateCommanderSkill(type: CommanderTacticalSkillType) {
        val state = _uiState.value
        if (!state.isBattleActive || state.battleResult != null) return

        when (type) {
            CommanderTacticalSkillType.SIGNATURE_ULTIMATE -> {
                if (state.playerCommanderEnergy < 100) return
                val cmd = state.playerCommander ?: return
                val ultName = cmd.activeAbility?.name ?: "Vanguard Shockwave"

                val updatedEnemies = state.aiFighters.map { it.copy() }.toMutableList()
                val updatedAllies = state.playerFighters.map { it.copy() }.toMutableList()
                val newDamageEffects = state.damageEffects.toMutableList()

                // Execute commander-specific tactical ultimate
                when {
                    cmd.name.contains("Magnus") || cmd.preferredFaction == Faction.TULS -> {
                        // Ironfist Magnus: Shield Slam & Vanguard Bulwark
                        // 60 AOE Physical DMG to all enemies + Stun, +70 Shield & +20 DEF to allies
                        updatedEnemies.filter { it.isAlive }.forEach { e ->
                            e.hp -= 60; e.health = e.hp
                            e.stunDuration = 1.0f
                            newDamageEffects.add(DamageEffect(x = e.x, y = e.y, damage = 60))
                        }
                        updatedAllies.filter { it.isAlive && it.startY >= 0 }.forEach { a ->
                            a.shield += 70f
                            a.physDefense += 20
                            a.defense = a.physDefense
                            newDamageEffects.add(DamageEffect(x = a.x, y = a.y, damage = -70))
                        }
                    }
                    cmd.name.contains("Elara") || cmd.preferredFaction == Faction.ELEMOS -> {
                        // Archmage Elara: Arcane Cataclysm & Mana Surge
                        // 80 AOE Magic DMG to all enemies, restores 100 Mana to all allies
                        updatedEnemies.filter { it.isAlive }.forEach { e ->
                            e.hp -= 80; e.health = e.hp
                            newDamageEffects.add(DamageEffect(x = e.x, y = e.y, damage = 80))
                        }
                        updatedAllies.filter { it.isAlive && it.startY >= 0 }.forEach { a ->
                            a.mana = 100
                            newDamageEffects.add(DamageEffect(x = a.x, y = a.y, damage = -30))
                        }
                    }
                    cmd.name.contains("Kael") || cmd.preferredFaction == Faction.EBONS -> {
                        // Shadow Kael: Executioner Strike & Smoke Veil
                        // Executes lowest HP enemy for 160 True DMG, gives all allies +50% Dodge for 3 ticks
                        val lowestEnemy = updatedEnemies.filter { it.isAlive }.minByOrNull { it.hp }
                        if (lowestEnemy != null) {
                            lowestEnemy.hp -= 160; lowestEnemy.health = lowestEnemy.hp
                            newDamageEffects.add(DamageEffect(x = lowestEnemy.x, y = lowestEnemy.y, damage = 160, isCrit = true))
                        }
                        updatedAllies.filter { it.isAlive && it.startY >= 0 }.forEach { a ->
                            a.dodgeChance = minOf(0.75, a.dodgeChance + 0.50)
                            a.critChance = minOf(0.75, a.critChance + 0.25)
                        }
                    }
                    else -> {
                        // Generic Ultimate: 55 AOE True DMG + +45 HP heal to allies
                        updatedEnemies.filter { it.isAlive }.forEach { e ->
                            e.hp -= 55; e.health = e.hp
                            newDamageEffects.add(DamageEffect(x = e.x, y = e.y, damage = 55))
                        }
                        updatedAllies.filter { it.isAlive && it.startY >= 0 }.forEach { a ->
                            a.hp = minOf(a.maxHp, a.hp + 45); a.health = a.hp
                            a.mana = minOf(100, a.mana + 50)
                            newDamageEffects.add(DamageEffect(x = a.x, y = a.y, damage = -45))
                        }
                    }
                }

                val logMsg = "👑 [COMMANDER ULTIMATE] ${cmd.name} unleashed [$ultName]!"
                _uiState.update { it.copy(
                    playerCommanderEnergy = it.playerCommanderEnergy - 100,
                    lastTacticalSkillCast = ultName,
                    playerFighters = updatedAllies,
                    aiFighters = updatedEnemies,
                    battleLogs = (listOf(logMsg) + it.battleLogs).take(MAX_COMBAT_LOG_HISTORY),
                    damageEffects = newDamageEffects
                ) }
            }
            CommanderTacticalSkillType.TACTICAL_RALLY -> {
                if (state.playerCommanderEnergy < 50) return
                val updatedAllies = state.playerFighters.map { it.copy() }.toMutableList()
                val newDamageEffects = state.damageEffects.toMutableList()

                updatedAllies.filter { it.isAlive && it.startY >= 0 }.forEach { a ->
                    val heal = 45
                    a.hp = minOf(a.maxHp, a.hp + heal)
                    a.health = a.hp
                    a.physDefense += 15; a.defense = a.physDefense
                    a.stunDuration = 0f
                    a.rootDuration = 0f
                    a.disarmDuration = 0f
                    newDamageEffects.add(DamageEffect(x = a.x, y = a.y, damage = -heal))
                }

                val logMsg = "🛡️ [TACTICAL RALLY] Commander rallies army: +45 HP, CC cleansed, +15 DEF!"
                _uiState.update { it.copy(
                    playerCommanderEnergy = it.playerCommanderEnergy - 50,
                    lastTacticalSkillCast = "Battle Rally",
                    playerFighters = updatedAllies,
                    battleLogs = (listOf(logMsg) + it.battleLogs).take(MAX_COMBAT_LOG_HISTORY),
                    damageEffects = newDamageEffects
                ) }
            }
            CommanderTacticalSkillType.PRECISION_STRIKE -> {
                if (state.playerCommanderEnergy < 50) return
                val updatedEnemies = state.aiFighters.map { it.copy() }.toMutableList()
                val newDamageEffects = state.damageEffects.toMutableList()

                // Strike highest damage living enemy
                val target = updatedEnemies.filter { it.isAlive }.maxByOrNull { it.physAttack + it.magAttack + it.damage }
                if (target != null) {
                    val dmg = 70
                    target.hp -= dmg; target.health = target.hp
                    target.disarmDuration = 2f
                    newDamageEffects.add(DamageEffect(x = target.x, y = target.y, damage = dmg, isCrit = true))
                    val logMsg = "🎯 [PRECISION STRIKE] Commander snipes ${target.name} for 70 True DMG & Disarms for 2 turns!"
                    _uiState.update { it.copy(
                        playerCommanderEnergy = it.playerCommanderEnergy - 50,
                        lastTacticalSkillCast = "Precision Strike",
                        aiFighters = updatedEnemies,
                        battleLogs = (listOf(logMsg) + it.battleLogs).take(MAX_COMBAT_LOG_HISTORY),
                        damageEffects = newDamageEffects
                    ) }
                }
            }
        }
    }

    fun addDemoCommanderEnergy(amount: Int = 50) {
        _uiState.update { it.copy(playerCommanderEnergy = minOf(100, it.playerCommanderEnergy + amount)) }
    }

    fun nextFight() {
        val state = _uiState.value
        val aliveAis = state.aiPlayers.filter { it.isAlive }

        // Check if player or all AIs are eliminated
        if ((state.playerCommander?.hp ?: 100) <= 0) {
            val placement = 1 + aliveAis.size
            _uiState.update { it.copy(currentScreen = Screen.GAME_OVER, playerPlacement = placement) }
            return
        }

        if (aliveAis.isEmpty()) {
            _uiState.update { it.copy(currentScreen = Screen.GAME_OVER, playerPlacement = 1) }
            return
        }

        var nextRound = state.round
        var nextFight = state.fight + 1
        
        val maxFights = if (nextRound == 1) 4 else 6
        if (nextFight > maxFights) {
            nextRound++
            nextFight = 1
        }
        
        if (nextRound > 6) {
            val placement = if (aliveAis.isEmpty()) 1 else 1 + aliveAis.count { it.hp > (state.playerCommander?.hp ?: 0) }
            _uiState.update { it.copy(currentScreen = Screen.GAME_OVER, playerPlacement = placement) }
            return
        }

        // Each alive AI takes its strategic turn: leveling up, shopping, merging, board positioning!
        val evolvedAis = state.aiPlayers.map { ai ->
            if (ai.isAlive) {
                AiOpponentManager.simulateAiTurn(ai, nextRound, poolManager)
            } else {
                ai
            }
        }

        val isBossRound = (nextRound in 2..4 && nextFight == 3)
        val (nextOpponentCommander, opponentFighters, opponentIdOrLog) = if (isBossRound) {
            NpcEncounterManager.getEncounterForRound(nextRound, nextFight)
        } else {
            val remainingLiving = evolvedAis.filter { it.isAlive }
            val chosenAi = remainingLiving.random()
            val fighters = chosenAi.boardFighters.map {
                it.copy(isPlayer = false, hp = it.maxHp, x = it.startX, y = it.startY)
            }
            Triple(
                chosenAi.commander.copy(hp = chosenAi.hp),
                fighters,
                chosenAi.id ?: ""
            )
        }
        
        val opponentId = if (isBossRound) null else opponentIdOrLog
        val encounterLog = if (isBossRound) opponentIdOrLog else ""
        
        val isCurrentBossRound = (state.round in 2..4 && state.fight == 3)
        val justWon = state.battleResult == BattleResult.PLAYER_WIN
        var newInventory = state.playerInventory
        var newEnhancements = state.playerEnhancements
        var dropLogs = mutableListOf<String>()
        if (encounterLog.isNotEmpty() && isBossRound) dropLogs.add(encounterLog)
        
        if (justWon && (state.round == 1 || isCurrentBossRound)) {
            if (isCurrentBossRound) {
                val (lootEquipment, lootEnhancements) = NpcEncounterManager.generateLoot(state.round, state.fight)
                newInventory = newInventory + lootEquipment
                newEnhancements = newEnhancements + lootEnhancements
                lootEquipment.forEach { dropLogs.add("Loot Found! Added ${it.name} to inventory!") }
                lootEnhancements.forEach { dropLogs.add("Bonus Found! Added ${it.name} enhancement!") }
            } else if (state.round == 1) {
                val dropType = listOf(EquipmentType.ATTACK, EquipmentType.DEFENSE, EquipmentType.DODGE, EquipmentType.HP).random()
                val equipment = when (dropType) {
                    EquipmentType.ATTACK -> Equipment(name = "Broadsword", type = EquipmentType.ATTACK)
                    EquipmentType.DEFENSE -> Equipment(name = "Chainmail", type = EquipmentType.DEFENSE)
                    EquipmentType.DODGE -> Equipment(name = "Elven Boots", type = EquipmentType.DODGE)
                    EquipmentType.HP -> Equipment(name = "Vitality Gem", type = EquipmentType.HP)
                }
                newInventory = newInventory + equipment
                dropLogs.add("Loot Found! Added ${equipment.name} to inventory!")
            }
        }

        val restoredPlayerFighters = state.playerFighters.map { 
            val swampBonus = if (state.playerCommander?.name?.contains("Swamp Titan") == true) 25 else 0
            val newMaxHp = it.maxHp + swampBonus

            it.copy(
                hp = newMaxHp, 
                maxHp = newMaxHp,
                x = it.startX, 
                y = it.startY
            ) 
        }

        _uiState.update { it.copy(
            round = nextRound,
            fight = nextFight,
            playerFighters = restoredPlayerFighters,
            aiFighters = opponentFighters,
            aiCommander = nextOpponentCommander,
            aiPlayers = evolvedAis,
            currentOpponentAiId = opponentId,
            playerInventory = newInventory,
            playerEnhancements = newEnhancements,
            battleResult = null,
            battleLogs = dropLogs + it.battleLogs.take(5)
        ) }
        startPrepTimer()
    }

    fun resetGame() {
        poolManager.resetPool()
        timerJob?.cancel()
        _uiState.update { GameState() }
    }

    private fun updateSynergies(state: GameState): GameState {
        val boardFighters = state.playerFighters.filter { it.startY >= 0 }
        val active = SynergyCalculator.calculateSynergies(boardFighters)
        val activeSkills = SynergyCalculator.getStackedSynergySkills(active)
        
        // Calculate potential bonus if a fighter is selected
        var potentialBonus: String? = null
        val selectedId = state.selectedFighterId
        if (selectedId != null) {
            val selected = state.playerFighters.find { it.id == selectedId }
            if (selected != null) {
                val faction = selected.faction
                val cls = selected.fighterClass
                val factionMatch = boardFighters.count { it.faction == faction && it.name != selected.name }
                val classMatch = boardFighters.count { it.fighterClass == cls && it.name != selected.name }
                
                potentialBonus = buildString {
                    if (factionMatch > 0) append("${faction.displayName} Link: +Stats ")
                    if (classMatch > 0) append("${cls.displayName} Link: +Buffs")
                    if (isEmpty()) append("No active links")
                }
            }
        }
        
        return state.copy(
            activeSynergies = active, 
            activeSynergySkills = activeSkills,
            potentialStatBonus = potentialBonus
        )
    }

    fun selectFighter(id: String) {
        if (_uiState.value.isBattleActive || _uiState.value.battleResult != null) return
        _uiState.update { it.copy(selectedFighterId = if (it.selectedFighterId == id) null else id) }
    }

    fun selectEnhancement(id: String?) {
        _uiState.update { it.copy(selectedEnhancementId = id) }
    }

    fun applyEnhancementToFighter(fighterId: String) {
        _uiState.update { state ->
            val enhId = state.selectedEnhancementId ?: return@update state
            val enhancement = state.playerEnhancements.find { it.id == enhId } ?: return@update state
            val fighterIdx = state.playerFighters.indexOfFirst { it.id == fighterId }
            if (fighterIdx == -1) return@update state
            
            val updatedFighters = state.playerFighters.toMutableList()
            updatedFighters[fighterIdx] = updatedFighters[fighterIdx].applyEnhancement(enhancement)
            
            state.copy(
                playerFighters = updatedFighters,
                playerEnhancements = state.playerEnhancements.filter { it.id != enhId },
                selectedEnhancementId = null
            )
        }
    }

    fun moveSelectedFighterTo(x: Int, y: Int) {
        if (_uiState.value.isBattleActive || _uiState.value.battleResult != null) return
        val state = _uiState.value
        val selectedId = state.selectedFighterId ?: return
        
        val selectedFighter = state.playerFighters.find { it.id == selectedId } ?: return
        val occupant = state.playerFighters.find { it.startX == x && it.startY == y }
        
        // Prevent moving from bench to board if capacity is full, unless swapping with a board unit
        if (selectedFighter.startY == -1 && y >= 0 && occupant == null) {
            val boardCount = state.playerFighters.count { it.startY >= 0 }
            if (boardCount >= state.playerLevel) return
        }
        
        val newFighters = state.playerFighters.map {
            if (it.id == selectedId) it.copy(x = x, y = y, startX = x, startY = y)
            else if (occupant != null && it.id == occupant.id) it.copy(x = selectedFighter.startX, y = selectedFighter.startY, startX = selectedFighter.startX, startY = selectedFighter.startY)
            else it
        }
        
        _uiState.update { updateSynergies(it.copy(playerFighters = newFighters, selectedFighterId = null)) }
    }

    fun buyLevelUp() {
        val state = _uiState.value
        if (state.playerLevel >= CommanderLeveling.MAX_LEVEL || state.isBattleActive) return
        
        val cost = CommanderLeveling.getCostToLevelUp(state.playerLevel, state.playerExp)
        
        if (state.playerGold >= cost && cost > 0) {
            _uiState.update { it.copy(
                playerGold = it.playerGold - cost,
                playerLevel = it.playerLevel + 1,
                playerExp = 0 // Excess exp is discarded when manually buying a level
            ) }
        }
    }

    fun buyFighter(fighter: Fighter) {
        val state = _uiState.value
        if (state.isBattleActive || state.battleResult != null) return
        
        val benchOccupied = state.playerFighters.filter { it.startY == -1 }.map { it.startX }
        if (benchOccupied.size >= 8) return // Bench is full
        
        if (state.playerGold >= fighter.cost) {
            var placedX = -1
            for (x in 0..7) {
                if (!benchOccupied.contains(x)) {
                    placedX = x
                    break
                }
            }
            if (placedX != -1) {
                val newFighter = fighter.copy(id = UUID.randomUUID().toString(), x = placedX, y = -1, startX = placedX, startY = -1)
                val buffed = applyCommanderBuffs(listOf(newFighter), state.playerCommander).first()
                
                val (newFighters, newInventory, newEnhancements) = poolManager.processMerges(state.playerFighters + buffed, state.playerInventory, state.playerEnhancements) { applyCommanderBuffs(it, state.playerCommander) }
                
                poolManager.buyUnit(fighter)
                _uiState.update { updateSynergies(it.copy(
                    playerGold = it.playerGold - fighter.cost,
                    playerFighters = newFighters,
                    playerInventory = newInventory,
                    playerEnhancements = newEnhancements,
                    shopFighters = it.shopFighters.filter { sf -> sf.id != fighter.id }
                )) }
            }
        }
    }

    fun sellSelectedFighter() {
        val state = _uiState.value
        if (state.isBattleActive || state.battleResult != null) return
        val selectedId = state.selectedFighterId ?: return
        val fighterToSell = state.playerFighters.find { it.id == selectedId } ?: return
        
        val multiplier = when (fighterToSell.starLevel) {
            3 -> 9
            2 -> 3
            else -> 1
        }
        val refund = fighterToSell.cost * multiplier
        
        poolManager.sellUnit(fighterToSell)
        
        val newFighters = state.playerFighters.filterNot { it.id == selectedId }
        val newInventory = state.playerInventory + fighterToSell.equipment
        
        _uiState.update { updateSynergies(it.copy(
            playerFighters = newFighters,
            playerGold = it.playerGold + refund,
            playerInventory = newInventory,
            selectedFighterId = null
        )) }
    }

    fun refreshShop(cost: Int = 2) {
        val state = _uiState.value
        if (state.isBattleActive || state.battleResult != null || state.playerGold < cost) return
        _uiState.update { it.copy(
            playerGold = it.playerGold - cost,
            shopFighters = poolManager.rollShop(it.playerLevel)
        ) }
    }
    
    fun toggleShopLock() {
        val state = _uiState.value
        if (state.isBattleActive || state.battleResult != null) return
        _uiState.update { it.copy(shopLocked = !it.shopLocked) }
    }

    fun selectEquipment(id: String) {
        if (_uiState.value.isBattleActive || _uiState.value.battleResult != null) return
        _uiState.update { it.copy(selectedEquipmentId = if (it.selectedEquipmentId == id) null else id) }
    }

    fun equipToFighter(fighterId: String) {
        val state = _uiState.value
        if (state.isBattleActive || state.battleResult != null) return
        val eqId = state.selectedEquipmentId ?: return
        val equipment = state.playerInventory.find { it.id == eqId } ?: return
        
        val newFighters = state.playerFighters.map { f ->
            if (f.id == fighterId && f.equipment.size < 3) {
                var newPAtk = f.physAttack
                var newPDef = f.physDefense
                var newMaxHp = f.maxHp
                var newHp = f.hp
                var newDodge = f.dodgeChance
                when (equipment.type) {
                    EquipmentType.ATTACK -> newPAtk += 10
                    EquipmentType.DEFENSE -> newPDef += 10
                    EquipmentType.HP -> { newMaxHp += 50; newHp += 50 }
                    EquipmentType.DODGE -> newDodge += 0.10
                }
                f.copy(
                    equipment = f.equipment + equipment,
                    physAttack = newPAtk,
                    physDefense = newPDef,
                    maxHp = newMaxHp,
                    hp = newHp,
                    dodgeChance = newDodge
                )
            } else f
        }
        
        // Only remove if it was actually equipped
        val fighter = newFighters.find { it.id == fighterId }
        val newInventory = if (fighter != null && fighter.equipment.any { it.id == eqId }) {
            state.playerInventory.filterNot { it.id == eqId }
        } else {
            state.playerInventory
        }
        
        _uiState.update { it.copy(
            playerFighters = newFighters,
            playerInventory = newInventory,
            selectedEquipmentId = null
        ) }
    }
}
