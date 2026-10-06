package com.example.game

import java.util.UUID
import com.example.R
import com.example.data.local.FighterEntity

const val MAX_COMBAT_LOG_HISTORY = 50

enum class AbilityEffectType {
    DAMAGE,
    HEAL,
    MULTI_ATTACK,
    SHIELD,
    AOE_BURST
}

enum class DamageType(val displayName: String) {
    PHYSICAL("Physical"),
    MAGIC("Magic"),
    TRUE("True")
}

enum class CCType(val displayName: String) {
    NONE("None"),
    STUN("Stun"),
    ROOT("Root"),
    DISARM("Disarm")
}

data class StatusEffect(
    val name: String,
    var duration: Float,
    val value: Float = 0.0f,
    val tickInterval: Float = 1.0f,
    var lastTick: Float = 0.0f
)

data class SpecialAbility(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val damage: Int = 0,
    val healAmount: Int = 0,
    val shieldAmount: Int = 0,
    val manaCost: Int = 100,
    val cooldownSeconds: Int = 0,
    val targetType: AbilityTargetType = AbilityTargetType.ENEMY_SINGLE,
    val cooldownTurns: Int = 3,
    val effectType: AbilityEffectType = AbilityEffectType.DAMAGE,
    val multiAttackHits: Int = 1,
    val staminaCost: Float = 10.0f,
    val focusRequired: Float = 0.0f,
    val attackTrigger: Int = 4,
    val damageType: DamageType = DamageType.PHYSICAL,
    val ccType: CCType = CCType.NONE,
    val ccDuration: Float = 0.0f
)

enum class AbilityTargetType {
    ENEMY_SINGLE,
    ENEMY_ALL,
    ENEMY_FURTHEST,
    ALLY_SINGLE,
    ALLY_ALL,
    SELF
}

enum class CommanderActiveType {
    SINGLE_TARGET_DMG,
    AOE_DMG,
    HEAL_LOWEST,
    SHIELD_ALL,
    STUN_RANDOM,
    BUFF_SPEED
}

data class CommanderActive(
    val type: CommanderActiveType,
    val value: Int,
    val cooldownTurns: Int = 8,
    val name: String,
    val description: String
)

enum class CommanderPassiveType {
    STAT_BOOST_PHYS_ATK,
    STAT_BOOST_MAG_ATK,
    STAT_BOOST_DEF,
    STAT_BOOST_HP,
    REGEN_PER_TURN,
    GOLD_PER_ROUND,
    EXP_PER_ROUND,
    DODGE_CHANCE_BOOST
}

data class CommanderPassive(
    val type: CommanderPassiveType,
    val value: Float,
    val description: String
)

enum class CommanderTacticalSkillType {
    SIGNATURE_ULTIMATE,
    TACTICAL_RALLY,
    PRECISION_STRIKE
}

data class CommanderTacticalSkill(
    val type: CommanderTacticalSkillType,
    val name: String,
    val icon: String,
    val energyCost: Int,
    val description: String,
    val tacticalBenefit: String
)

data class Commander(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val title: String = "",
    val abilityDescription: String = "",
    val hp: Int = 100,
    val maxHp: Int = 100,
    val imageRes: Int = 0,
    val preferredFaction: Faction = Faction.TULS,
    val preferredClass: FighterClass = FighterClass.KNUCKLE_POWER,
    val cost: Int = 2,
    val health: Int = hp,
    val maxHealth: Int = maxHp,
    val damage: Int = 15,
    val weapon: String = "",
    val specialAbility: SpecialAbility? = null,
    val activeAbility: CommanderActive? = null,
    val passiveAbility: CommanderPassive? = null
) {
    val isAlive: Boolean get() = hp > 0 || health > 0

    val effectiveAbility: SpecialAbility
        get() = specialAbility ?: SpecialAbility(
            name = name.substringBefore(" (").ifBlank { name },
            description = abilityDescription.ifBlank { "Deals commander combat damage." },
            damage = damage
        )

    fun getTacticalSkills(): List<CommanderTacticalSkill> {
        val ultimateName = activeAbility?.name ?: "Dominion Cataclysm"
        val ultimateDesc = activeAbility?.description ?: "Unleashes commander devastating tactical combat power."
        return listOf(
            CommanderTacticalSkill(
                type = CommanderTacticalSkillType.SIGNATURE_ULTIMATE,
                name = ultimateName,
                icon = "⚡",
                energyCost = 100,
                description = ultimateDesc,
                tacticalBenefit = "Signature Ultimate (Game-Changer)"
            ),
            CommanderTacticalSkill(
                type = CommanderTacticalSkillType.TACTICAL_RALLY,
                name = "Battle Rally",
                icon = "🛡️",
                energyCost = 50,
                description = "Instantly heals all living allies for +45 HP, cleanses crowd-control, and adds +15 Defense.",
                tacticalBenefit = "Saves Frontline & Cleanses CC"
            ),
            CommanderTacticalSkill(
                type = CommanderTacticalSkillType.PRECISION_STRIKE,
                name = "Precision Strike",
                icon = "🎯",
                energyCost = 50,
                description = "Strikes the deadliest living enemy for 70 True Damage and Disarms them for 2 combat ticks.",
                tacticalBenefit = "Neutralizes Enemy Carry"
            )
        )
    }
}

enum class Faction(val displayName: String) {
    REPTILIANS("Reptilians"),
    EBONS("Ebons"),
    P45("P45"),
    FEET_WORK("Feet Work"),
    ELEMOS("Elemos"),
    TULS("Tuls"),
    WIND_WALKER("Wind Walker"),
    NPC("NPC"),
    // Legacy support
    ORC("Orc"),
    DARK("Dark"),
    LIGHT("Light"),
    NATURE("Nature"),
    ELEMENTAL("Elemental");

    companion object {
        fun fromString(value: String): Faction {
            val normalized = value.trim().replace(" ", "_").replace("-", "_").uppercase()
            return entries.firstOrNull { 
                it.name.equals(normalized, ignoreCase = true) || 
                it.displayName.equals(value.trim(), ignoreCase = true) 
            } ?: REPTILIANS
        }
    }
}

enum class FighterClass(val displayName: String) {
    KNUCKLE_POWER("Knuckle Power"),
    HUNTERS("Hunters"),
    ILLUMINS("Illumins"),
    CONVICT("Convict"),
    NPC("NPC"),
    // Legacy support
    TANK("Tank"),
    ASSASSIN("Assassin"),
    MARKSMAN("Marksman"),
    MAGE("Mage");

    companion object {
        fun fromString(value: String): FighterClass {
            val normalized = value.trim().replace(" ", "_").replace("-", "_").uppercase()
            return entries.firstOrNull { 
                it.name.equals(normalized, ignoreCase = true) || 
                it.displayName.equals(value.trim(), ignoreCase = true) 
            } ?: KNUCKLE_POWER
        }
    }
}

enum class EquipmentType {
    ATTACK, DEFENSE, HP, DODGE
}

data class Equipment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: EquipmentType
)

data class AiPlayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val commander: Commander,
    var hp: Int = 100,
    var level: Int = 3,
    var exp: Int = 0,
    var gold: Int = 10,
    var winStreak: Int = 0,
    var loseStreak: Int = 0,
    var boardFighters: List<Fighter> = emptyList(),
    var benchFighters: List<Fighter> = emptyList(),
    var inventory: List<Equipment> = emptyList(),
    val preferredFaction: Faction = commander.preferredFaction,
    val preferredClass: FighterClass = commander.preferredClass,
    val strategyName: String = "Balanced"
) {
    val isAlive: Boolean get() = hp > 0
    val allFighters: List<Fighter> get() = boardFighters + benchFighters
}

data class LobbyEntry(
    val name: String,
    val imageRes: Int,
    val hp: Int,
    val level: Int,
    val gold: Int,
    val isCurrentOpponent: Boolean,
    val isAlive: Boolean,
    val strategy: String
)

data class Fighter(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val faction: Faction = Faction.ORC,
    val fighterClass: FighterClass = FighterClass.TANK,
    val cost: Int = 1,
    var hp: Int = 100,
    var maxHp: Int = hp,
    var mana: Int = 0,
    var maxMana: Int = 100,
    var physAttack: Int = 10,
    var magAttack: Int = 0,
    var physDefense: Int = 5,
    var magDefense: Int = 0,
    var attackSpeed: Int = 10,
    var intelligence: Int = 10,
    var attackRange: Int = 1,
    var accuracy: Int = 90,
    var critChance: Double = 0.05,
    var dodgeChance: Double = 0.05,
    var x: Int = 0,
    var y: Int = 0,
    var startX: Int = x,
    var startY: Int = y,
    val isPlayer: Boolean = true,
    var starLevel: Int = 1,
    val equipment: List<Equipment> = emptyList(),
    var health: Int = hp,
    var attackPower: Int = physAttack,
    var defense: Int = physDefense,
    var damage: Int = physAttack,
    val specialAbility: SpecialAbility? = null,
    var abilityCooldown: Int = 0,
    var currentCooldown: Int = abilityCooldown,
    var maxAbilityCooldown: Int = 3,
    var stamina: Float = 20.0f,
    var maxStamina: Float = 20.0f,
    var focus: Float = 0.0f,
    var shield: Float = 0.0f,
    var stunDuration: Float = 0.0f,
    var rootDuration: Float = 0.0f,
    var disarmDuration: Float = 0.0f,
    var attacksSinceSkill: Int = 0,
    val enhancements: List<AbilityEnhancement> = emptyList()
) {
    val isAlive: Boolean get() = hp > 0 || health > 0
    val isStunned: Boolean get() = stunDuration > 0.0f
    val isRooted: Boolean get() = rootDuration > 0.0f || stunDuration > 0.0f
    val isDisarmed: Boolean get() = disarmDuration > 0.0f || stunDuration > 0.0f
    val canAct: Boolean get() = isAlive && !isStunned

    val effectiveAbility: SpecialAbility
        get() = specialAbility ?: getFighterSpecialAbility(name, fighterClass, damage)

    val isAbilityReady: Boolean
        get() {
            if (!canAct) return false
            val ability = effectiveAbility
            val hasResources = if (ability.manaCost > 0) mana >= ability.manaCost else (stamina >= ability.staminaCost && focus >= ability.focusRequired)
            val triggerMet = abilityCooldown <= 0 || attacksSinceSkill >= ability.attackTrigger
            return hasResources && triggerMet
        }

    fun reduceCooldown(amount: Int = 1) {
        abilityCooldown = maxOf(0, abilityCooldown - amount)
        currentCooldown = abilityCooldown
    }

    fun resetCooldown(turns: Int = effectiveAbility.cooldownTurns) {
        abilityCooldown = turns
        currentCooldown = turns
        maxAbilityCooldown = turns
        attacksSinceSkill = 0
    }

    fun tickTurnStatus() {
        if (stunDuration > 0.0f) stunDuration = maxOf(0.0f, stunDuration - 1.0f)
        if (rootDuration > 0.0f) rootDuration = maxOf(0.0f, rootDuration - 1.0f)
        if (disarmDuration > 0.0f) disarmDuration = maxOf(0.0f, disarmDuration - 1.0f)
        // Stamina passive recovery
        stamina = minOf(maxStamina, stamina + 3.0f)
    }

    init {
        if (currentCooldown != 0 && abilityCooldown == 0) {
            abilityCooldown = currentCooldown
        }
        currentCooldown = abilityCooldown
        maxAbilityCooldown = effectiveAbility.cooldownTurns
        // Synchronize health, damage, attackPower, and defense if custom values are provided
        if (health != 100 && hp == 100) {
            hp = health
            maxHp = health
        }
        if (damage != 10 && physAttack == 10) {
            physAttack = damage
            attackPower = damage
        } else if (attackPower != 10 && physAttack == 10) {
            physAttack = attackPower
            damage = attackPower
        }
        if (defense != 5 && physDefense == 5) {
            physDefense = defense
        }
        health = hp
        attackPower = physAttack
        damage = physAttack
        defense = physDefense
    }
}

fun Fighter.toEntity(): FighterEntity = FighterEntity(
    id = id,
    name = name,
    health = hp,
    maxHealth = maxHp,
    attackPower = physAttack,
    defense = physDefense,
    magicAttack = magAttack,
    magicDefense = magDefense,
    attackSpeed = attackSpeed,
    attackRange = attackRange,
    cost = cost,
    starLevel = starLevel,
    faction = faction.name,
    fighterClass = fighterClass.name,
    isPlayer = isPlayer,
    boardX = x,
    boardY = y
)

fun FighterEntity.toFighter(): Fighter = Fighter(
    id = id,
    name = name,
    faction = Faction.fromString(faction),
    fighterClass = FighterClass.fromString(fighterClass),
    cost = cost,
    hp = health,
    maxHp = maxHealth,
    mana = 0,
    maxMana = 100,
    physAttack = attackPower,
    magAttack = magicAttack,
    physDefense = defense,
    magDefense = magicDefense,
    attackSpeed = attackSpeed,
    intelligence = 10,
    attackRange = attackRange,
    accuracy = 90,
    critChance = 0.05,
    dodgeChance = 0.05,
    x = boardX,
    y = boardY,
    startX = boardX,
    startY = boardY,
    isPlayer = isPlayer,
    starLevel = starLevel,
    equipment = emptyList(),
    health = health,
    attackPower = attackPower,
    defense = defense
)

data class CommanderRoundResult(
    val round: Int,
    val fight: Int,
    val commanderName: String,
    val isWin: Boolean,
    val remainingHp: Int,
    val isPlayer: Boolean = false
)

data class DamageEffect(
    val id: String = UUID.randomUUID().toString(),
    val x: Int,
    val y: Int,
    val damage: Int,
    val isCrit: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class GameState(
    val currentScreen: Screen = Screen.COMMANDER_SELECTION,
    val round: Int = 1,
    val fight: Int = 1,
    val playerCommander: Commander? = null,
    val aiCommander: Commander? = null,
    val aiPlayers: List<AiPlayer> = emptyList(),
    val currentOpponentAiId: String? = null,
    val playerFighters: List<Fighter> = emptyList(),
    val aiFighters: List<Fighter> = emptyList(),
    val battleLogs: List<String> = emptyList(),
    val isBattleActive: Boolean = false,
    val battleResult: BattleResult? = null,
    val playerLevel: Int = 3,
    val playerExp: Int = 0,
    val playerGold: Int = 0,
    val selectedFighterId: String? = null,
    val shopFighters: List<Fighter> = emptyList(),
    val playerInventory: List<Equipment> = emptyList(),
    val selectedEquipmentId: String? = null,
    val globalPool: Map<String, Int> = emptyMap(),
    val activeSynergies: List<String> = emptyList(),
    val shopLocked: Boolean = false,
    val winStreak: Int = 0,
    val loseStreak: Int = 0,
    val preBattleFighters: List<Fighter> = emptyList(),
    val prepTimer: Int = 30,
    val playerPlacement: Int = 1,
    val unlockedCommanderNames: Set<String> = CommanderUnlockManager.STARTER_COMMANDER_NAMES,
    val battleCredits: Int = 150,
    val showUnlockDialogFor: Commander? = null,
    val showDemoCashDialogFor: Commander? = null,
    val unlockFeedbackMessage: String? = null,
    val activeSynergySkills: List<StackedSynergySkill> = emptyList(),
    val roundHistory: List<CommanderRoundResult> = emptyList(),
    val playerEnhancements: List<AbilityEnhancement> = emptyList(),
    val selectedEnhancementId: String? = null,
    val isAdmin: Boolean = false,
    val potentialStatBonus: String? = null,
    val playerCommanderActiveCooldown: Int = 0,
    val aiCommanderActiveCooldown: Int = 0,
    val playerCommanderEnergy: Int = 0,
    val aiCommanderEnergy: Int = 0,
    val lastTacticalSkillCast: String? = null,
    val damageEffects: List<DamageEffect> = emptyList()
) {
    val battleState: BattleState get() = toBattleState()
}

data class BattleState(
    val id: String = UUID.randomUUID().toString(),
    val round: Int = 1,
    val fight: Int = 1,
    val turn: Int = 0,
    val playerCommander: Commander? = null,
    val opponentCommander: Commander? = null,
    val playerFighters: List<Fighter> = emptyList(),
    val opponentFighters: List<Fighter> = emptyList(),
    val isBattleActive: Boolean = false,
    val battleResult: BattleResult? = null,
    val battleLogs: List<String> = emptyList(),
    val activeSpecialAbilities: List<SpecialAbility> = emptyList(),
    val activeSynergies: List<String> = emptyList(),
    val roundDamageDealt: Map<String, Int> = emptyMap(),
    val roundDamageTaken: Map<String, Int> = emptyMap(),
    val roundHistory: List<CommanderRoundResult> = emptyList(),
    val playerCommanderActiveCooldown: Int = 0,
    val aiCommanderActiveCooldown: Int = 0,
    val playerCommanderEnergy: Int = 0,
    val aiCommanderEnergy: Int = 0,
    val lastTacticalSkillCast: String? = null
) {
    val isGameOver: Boolean
        get() = (playerCommander != null && !playerCommander.isAlive) ||
                (opponentCommander != null && !opponentCommander.isAlive) ||
                (battleResult != null)

    val livingPlayerFighters: List<Fighter>
        get() = playerFighters.filter { it.isAlive }

    val livingOpponentFighters: List<Fighter>
        get() = opponentFighters.filter { it.isAlive }

    val totalPlayerHealth: Int
        get() = livingPlayerFighters.sumOf { it.health }

    val totalOpponentHealth: Int
        get() = livingOpponentFighters.sumOf { it.health }

    val totalPlayerDamage: Int
        get() = livingPlayerFighters.sumOf { it.damage }

    val totalOpponentDamage: Int
        get() = livingOpponentFighters.sumOf { it.damage }
}

fun GameState.toBattleState(): BattleState = BattleState(
    round = round,
    fight = fight,
    playerCommander = playerCommander,
    opponentCommander = aiCommander,
    playerFighters = playerFighters,
    opponentFighters = aiFighters,
    isBattleActive = isBattleActive,
    battleResult = battleResult,
    battleLogs = battleLogs,
    activeSynergies = activeSynergies,
    roundHistory = roundHistory,
    playerCommanderActiveCooldown = playerCommanderActiveCooldown,
    aiCommanderActiveCooldown = aiCommanderActiveCooldown,
    playerCommanderEnergy = playerCommanderEnergy,
    aiCommanderEnergy = aiCommanderEnergy,
    lastTacticalSkillCast = lastTacticalSkillCast
)

data class StackedSynergySkill(
    val id: String,
    val name: String,
    val synergyTag: String,
    val icon: String,
    val description: String,
    val triggerDescription: String
)

data class CommanderUnlockInfo(
    val commanderName: String,
    val isStarter: Boolean,
    val unlockCreditCost: Int,
    val gameplayMilestone: String,
    val cashPriceDisplay: String = "$0.99 [Demo Purchase]",
    val cashPriceCents: Int = 99
) {
    val unlockRequirement: String get() = gameplayMilestone
    val cashPrice: String get() = String.format(java.util.Locale.US, "%.2f", cashPriceCents / 100.0)
}

object CommanderUnlockManager {
    val STARTER_COMMANDER_NAMES = setOf(
        "Ironfist Magnus",
        "Archmage Elara",
        "Shadow Kael"
    )

    val UNLOCK_CATALOG: Map<String, CommanderUnlockInfo> = mapOf(
        "Ironfist Magnus" to CommanderUnlockInfo("Ironfist Magnus", isStarter = true, 0, "Available at Start (Free)", "$0.00", 0),
        "Archmage Elara" to CommanderUnlockInfo("Archmage Elara", isStarter = true, 0, "Available at Start (Free)", "$0.00", 0),
        "Shadow Kael" to CommanderUnlockInfo("Shadow Kael", isStarter = true, 0, "Available at Start (Free)", "$0.00", 0)
    )
}

enum class Screen {
    COMMANDER_SELECTION,
    BATTLE_BOARD,
    BATTLE_VISUALIZATION,
    GAME_OVER
}

enum class BattleResult {
    PLAYER_WIN,
    AI_WIN,
    DRAW
}

val availableCommanders = listOf(
    Commander(
        name = "Ironfist Magnus",
        title = "The Tuls Vanguard",
        abilityDescription = "Passive: Iron Bulwark (+25 Def to all allies). Active: Shield Slam (AOE DMG + Stun).",
        imageRes = R.drawable.img_grog,
        weapon = "War Hammer",
        preferredFaction = Faction.TULS,
        preferredClass = FighterClass.KNUCKLE_POWER,
        activeAbility = CommanderActive(
            type = CommanderActiveType.AOE_DMG,
            value = 40,
            cooldownTurns = 6,
            name = "Shield Slam",
            description = "Deals 40 AOE damage and stuns enemies for 1 turn."
        ),
        passiveAbility = CommanderPassive(
            type = CommanderPassiveType.STAT_BOOST_DEF,
            value = 25f,
            description = "+25 Physical & Magic Defense to all allies."
        )
    ),
    Commander(
        name = "Archmage Elara",
        title = "The Elemos Sage",
        abilityDescription = "Passive: Arcane Brilliance (+40 Mag Atk to all allies). Active: Arcane Nova (Massive AOE DMG).",
        imageRes = R.drawable.img_ignis,
        weapon = "Arcane Staff",
        preferredFaction = Faction.ELEMOS,
        preferredClass = FighterClass.ILLUMINS,
        activeAbility = CommanderActive(
            type = CommanderActiveType.AOE_DMG,
            value = 60,
            cooldownTurns = 8,
            name = "Arcane Nova",
            description = "Unleashes a massive magical explosion dealing 60 damage to all enemies."
        ),
        passiveAbility = CommanderPassive(
            type = CommanderPassiveType.STAT_BOOST_MAG_ATK,
            value = 40f,
            description = "+40 Magic Attack to all allies."
        )
    ),
    Commander(
        name = "Shadow Kael",
        title = "The Ebon Assassin",
        abilityDescription = "Passive: Shadow Step (+15% Dodge to all allies). Active: Executioner Strike (High Single Target DMG).",
        imageRes = R.drawable.img_valeria,
        weapon = "Dual Daggers",
        preferredFaction = Faction.EBONS,
        preferredClass = FighterClass.HUNTERS,
        activeAbility = CommanderActive(
            type = CommanderActiveType.SINGLE_TARGET_DMG,
            value = 120,
            cooldownTurns = 5,
            name = "Executioner Strike",
            description = "Strikes the weakest enemy for 120 damage."
        ),
        passiveAbility = CommanderPassive(
            type = CommanderPassiveType.DODGE_CHANCE_BOOST,
            value = 0.15f,
            description = "+15% Dodge chance to all allies."
        )
    )
)

data class FighterTemplate(val name: String, val faction: Faction, val cls: FighterClass, val cost: Int, val hp: Int, val mana: Int, val pAtk: Int, val mAtk: Int, val pDef: Int, val mDef: Int, val spd: Int, val intl: Int, val rng: Int, val acc: Int)
    
val fighterPool = listOf(
    // 1 Gold
    FighterTemplate("Steel Bastion", Faction.TULS, FighterClass.CONVICT, 1, hp = 180, mana = 0, pAtk = 18, mAtk = 0, pDef = 22, mDef = 14, spd = 6, intl = 0, rng = 1, acc = 100),
    FighterTemplate("Mad Grin", Faction.P45, FighterClass.CONVICT, 1, hp = 130, mana = 10, pAtk = 24, mAtk = 0, pDef = 8, mDef = 6, spd = 13, intl = 5, rng = 1, acc = 85),
    FighterTemplate("Gale Marksman", Faction.ELEMOS, FighterClass.HUNTERS, 1, hp = 95, mana = 10, pAtk = 22, mAtk = 5, pDef = 6, mDef = 6, spd = 10, intl = 5, rng = 4, acc = 90),
    FighterTemplate("Alley Slugger", Faction.FEET_WORK, FighterClass.KNUCKLE_POWER, 1, hp = 150, mana = 0, pAtk = 20, mAtk = 0, pDef = 12, mDef = 8, spd = 11, intl = 0, rng = 1, acc = 95),
    FighterTemplate("Ironclad Warden", Faction.TULS, FighterClass.CONVICT, 1, hp = 200, mana = 0, pAtk = 15, mAtk = 0, pDef = 25, mDef = 16, spd = 5, intl = 0, rng = 1, acc = 100),

    // 2 Gold
    FighterTemplate("Brus Li", Faction.REPTILIANS, FighterClass.KNUCKLE_POWER, 2, hp = 220, mana = 20, pAtk = 35, mAtk = 0, pDef = 18, mDef = 12, spd = 16, intl = 10, rng = 1, acc = 95),
    FighterTemplate("Crimson Streak", Faction.FEET_WORK, FighterClass.HUNTERS, 2, hp = 140, mana = 20, pAtk = 38, mAtk = 0, pDef = 10, mDef = 10, spd = 18, intl = 10, rng = 3, acc = 90),
    FighterTemplate("Shadow Viper", Faction.WIND_WALKER, FighterClass.HUNTERS, 2, hp = 150, mana = 30, pAtk = 40, mAtk = 0, pDef = 12, mDef = 10, spd = 17, intl = 10, rng = 2, acc = 90),
    FighterTemplate("Zephyr Claw", Faction.WIND_WALKER, FighterClass.KNUCKLE_POWER, 2, hp = 190, mana = 20, pAtk = 36, mAtk = 0, pDef = 14, mDef = 12, spd = 16, intl = 10, rng = 1, acc = 95),
    FighterTemplate("Umbral Seer", Faction.EBONS, FighterClass.ILLUMINS, 2, hp = 130, mana = 80, pAtk = 10, mAtk = 42, pDef = 8, mDef = 18, spd = 9, intl = 20, rng = 3, acc = 90),
    FighterTemplate("Ghost Escapee", Faction.WIND_WALKER, FighterClass.CONVICT, 2, hp = 170, mana = 20, pAtk = 32, mAtk = 0, pDef = 12, mDef = 10, spd = 15, intl = 8, rng = 1, acc = 90),
    FighterTemplate("Venomous Stalker", Faction.REPTILIANS, FighterClass.HUNTERS, 2, hp = 160, mana = 30, pAtk = 42, mAtk = 0, pDef = 10, mDef = 10, spd = 18, intl = 10, rng = 2, acc = 90),
    FighterTemplate("Aether Weaver", Faction.ELEMOS, FighterClass.ILLUMINS, 2, hp = 140, mana = 80, pAtk = 12, mAtk = 38, pDef = 10, mDef = 20, spd = 10, intl = 20, rng = 3, acc = 90),

    // 3 Gold
    FighterTemplate("Night Stalker", Faction.EBONS, FighterClass.HUNTERS, 3, hp = 200, mana = 40, pAtk = 60, mAtk = 0, pDef = 15, mDef = 15, spd = 15, intl = 12, rng = 3, acc = 95),
    FighterTemplate("Tempest Empress", Faction.ELEMOS, FighterClass.ILLUMINS, 3, hp = 170, mana = 120, pAtk = 15, mAtk = 70, pDef = 12, mDef = 30, spd = 9, intl = 25, rng = 4, acc = 95),
    FighterTemplate("Swamp Titan", Faction.ELEMOS, FighterClass.KNUCKLE_POWER, 3, hp = 380, mana = 40, pAtk = 45, mAtk = 20, pDef = 30, mDef = 20, spd = 7, intl = 10, rng = 1, acc = 90),
    FighterTemplate("Bio-Parasite", Faction.REPTILIANS, FighterClass.CONVICT, 3, hp = 260, mana = 30, pAtk = 50, mAtk = 15, pDef = 20, mDef = 18, spd = 14, intl = 10, rng = 1, acc = 95),
    FighterTemplate("Deadshot Cyber", Faction.P45, FighterClass.HUNTERS, 3, hp = 190, mana = 30, pAtk = 58, mAtk = 10, pDef = 14, mDef = 14, spd = 13, intl = 15, rng = 4, acc = 100),
    FighterTemplate("Cyber Oni", Faction.P45, FighterClass.KNUCKLE_POWER, 3, hp = 240, mana = 40, pAtk = 55, mAtk = 0, pDef = 22, mDef = 14, spd = 15, intl = 10, rng = 1, acc = 95),
    FighterTemplate("Lunar Sentinel", Faction.WIND_WALKER, FighterClass.HUNTERS, 3, hp = 180, mana = 30, pAtk = 52, mAtk = 0, pDef = 15, mDef = 15, spd = 14, intl = 12, rng = 5, acc = 95),

    // 4 Gold
    FighterTemplate("Iron Apex", Faction.P45, FighterClass.ILLUMINS, 4, hp = 450, mana = 100, pAtk = 30, mAtk = 80, pDef = 35, mDef = 35, spd = 10, intl = 30, rng = 3, acc = 95),
    FighterTemplate("Brimstone Rider", Faction.EBONS, FighterClass.CONVICT, 4, hp = 500, mana = 60, pAtk = 75, mAtk = 30, pDef = 35, mDef = 25, spd = 14, intl = 15, rng = 1, acc = 95),
    FighterTemplate("Astral Sovereign", Faction.REPTILIANS, FighterClass.ILLUMINS, 4, hp = 420, mana = 120, pAtk = 35, mAtk = 85, pDef = 25, mDef = 35, spd = 11, intl = 32, rng = 4, acc = 95),
    FighterTemplate("Thunder Mauler", Faction.TULS, FighterClass.KNUCKLE_POWER, 4, hp = 520, mana = 50, pAtk = 70, mAtk = 25, pDef = 45, mDef = 30, spd = 9, intl = 15, rng = 1, acc = 95),
    FighterTemplate("Void Reaver", Faction.EBONS, FighterClass.CONVICT, 4, hp = 480, mana = 60, pAtk = 80, mAtk = 0, pDef = 30, mDef = 20, spd = 16, intl = 10, rng = 1, acc = 95),

    // 5 Gold
    FighterTemplate("Solar Aegis", Faction.TULS, FighterClass.ILLUMINS, 5, hp = 650, mana = 150, pAtk = 50, mAtk = 95, pDef = 55, mDef = 50, spd = 12, intl = 35, rng = 2, acc = 100),
    FighterTemplate("Magma Behemoth", Faction.ELEMOS, FighterClass.KNUCKLE_POWER, 5, hp = 800, mana = 40, pAtk = 65, mAtk = 20, pDef = 60, mDef = 40, spd = 8, intl = 10, rng = 1, acc = 90),
    FighterTemplate("Eternal Chronos", Faction.WIND_WALKER, FighterClass.ILLUMINS, 5, hp = 550, mana = 150, pAtk = 40, mAtk = 75, pDef = 40, mDef = 45, spd = 14, intl = 30, rng = 4, acc = 95)
)

fun getFighterSpecialAbility(name: String, fighterClass: FighterClass, damage: Int): SpecialAbility {
    val cleanName = name.substringBefore(" (").ifBlank { name }
    
    // Unique abilities by name first
    when (cleanName) {
        "Ironclad Warden" -> return SpecialAbility(
            name = "Warden's Bastion",
            description = "Creates a massive energy shield and heals for 15% Max HP.",
            shieldAmount = 100,
            healAmount = 40,
            manaCost = 40,
            cooldownTurns = 3,
            effectType = AbilityEffectType.SHIELD,
            targetType = AbilityTargetType.SELF
        )
        "Venomous Stalker" -> return SpecialAbility(
            name = "Toxic Ambush",
            description = "Strikes with poisoned blades, dealing 250% damage and applying a bleed effect.",
            damage = (damage * 2.5).toInt(),
            manaCost = 50,
            cooldownTurns = 2,
            effectType = AbilityEffectType.DAMAGE,
            targetType = AbilityTargetType.ENEMY_SINGLE
        )
        "Eternal Chronos" -> return SpecialAbility(
            name = "Time Warp",
            description = "Warps time to heal all allies for 120% Intelligence and smite the furthest enemy.",
            damage = (damage * 1.5).toInt(),
            healAmount = (damage * 1.2).toInt(),
            manaCost = 100,
            cooldownTurns = 4,
            effectType = AbilityEffectType.HEAL,
            targetType = AbilityTargetType.ALLY_SINGLE // Logic for AOE heal might need engine change, sticking to existing types
        )
    }

    return when (fighterClass) {
        FighterClass.KNUCKLE_POWER -> SpecialAbility(
            name = "$cleanName Dragon Strike Flurry",
            description = "Unleashes a rapid 3-strike flurry combo dealing heavy physical burst.",
            damage = (damage * 2.2).toInt().coerceAtLeast(20),
            manaCost = 50,
            cooldownTurns = 2,
            effectType = AbilityEffectType.MULTI_ATTACK,
            multiAttackHits = 3,
            targetType = AbilityTargetType.ENEMY_SINGLE
        )
        FighterClass.HUNTERS -> SpecialAbility(
            name = "$cleanName Twin Piercing Shot",
            description = "Fires a rapid 2-shot volley sniping through enemy lines.",
            damage = (damage * 2.0).toInt().coerceAtLeast(20),
            manaCost = 60,
            cooldownTurns = 3,
            effectType = AbilityEffectType.MULTI_ATTACK,
            multiAttackHits = 2,
            targetType = AbilityTargetType.ENEMY_FURTHEST
        )
        FighterClass.ILLUMINS -> SpecialAbility(
            name = "$cleanName Astral Restoration",
            description = "Channels celestial radiance, healing the lowest-health ally and smiting nearby enemies.",
            damage = (damage * 1.4).toInt().coerceAtLeast(15),
            healAmount = (damage * 1.8).toInt().coerceAtLeast(35),
            manaCost = 70,
            cooldownTurns = 3,
            effectType = AbilityEffectType.HEAL,
            targetType = AbilityTargetType.ALLY_SINGLE
        )
        FighterClass.CONVICT -> SpecialAbility(
            name = "$cleanName Iron Will",
            description = "Roars with convict tenacity, restoring health, gaining shield and dealing retaliation burst.",
            damage = (damage * 1.2).toInt().coerceAtLeast(10),
            healAmount = (damage * 1.0).toInt().coerceAtLeast(25),
            shieldAmount = 60,
            manaCost = 40,
            cooldownTurns = 3,
            effectType = AbilityEffectType.HEAL,
            targetType = AbilityTargetType.SELF
        )
        else -> SpecialAbility(
            name = "$cleanName Dual Strike",
            description = "Executes a focused 2-hit assault.",
            damage = (damage * 1.5).toInt().coerceAtLeast(10),
            manaCost = 50,
            cooldownTurns = 2,
            effectType = AbilityEffectType.MULTI_ATTACK,
            multiAttackHits = 2,
            targetType = AbilityTargetType.ENEMY_SINGLE
        )
    }
}

fun instantiateFighter(template: FighterTemplate, isPlayer: Boolean, yPos: Int, starLevel: Int = 1): Fighter {
    val multiplier = if (starLevel == 3) 4 else if (starLevel == 2) 2 else 1
    val baseDamage = (template.pAtk + template.mAtk) * multiplier
    val ability = getFighterSpecialAbility(template.name, template.cls, baseDamage)
    return Fighter(
        name = template.name, faction = template.faction, fighterClass = template.cls, cost = template.cost,
        hp = template.hp * multiplier, maxHp = template.hp * multiplier, mana = template.mana, maxMana = template.mana,
        physAttack = template.pAtk * multiplier, magAttack = template.mAtk * multiplier, physDefense = template.pDef, magDefense = template.mDef,
        attackSpeed = template.spd, intelligence = template.intl, attackRange = template.rng, accuracy = template.acc,
        critChance = 0.05, dodgeChance = 0.05, x = (0..7).random(), y = yPos, startX = 0, startY = 0, isPlayer = isPlayer,
        starLevel = starLevel,
        specialAbility = ability
    ).apply {
        startX = x
        startY = y
    }
}

fun generateFighterByCost(cost: Int, isPlayer: Boolean, yPos: Int): Fighter {
    val pool = fighterPool.filter { it.cost == cost }.ifEmpty { fighterPool }
    return instantiateFighter(pool.random(), isPlayer, yPos)
}

data class AbilityEnhancement(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val hpBonus: Int = 0,
    val attackBonus: Int = 0,
    val defenseBonus: Int = 0,
    val speedBonus: Int = 0,
    val accuracyBonus: Int = 0,
    val critBonus: Double = 0.0,
    val dodgeBonus: Double = 0.0,
    val passiveSkill: String? = null,
    val cost: Int = 0
)

fun Fighter.applyEnhancement(enhancement: AbilityEnhancement): Fighter {
    return this.copy(
        maxHp = maxHp + enhancement.hpBonus,
        hp = hp + enhancement.hpBonus,
        health = health + enhancement.hpBonus,
        physAttack = physAttack + enhancement.attackBonus,
        attackPower = attackPower + enhancement.attackBonus,
        damage = damage + enhancement.attackBonus,
        physDefense = physDefense + enhancement.defenseBonus,
        defense = defense + enhancement.defenseBonus,
        attackSpeed = attackSpeed + enhancement.speedBonus,
        accuracy = accuracy + enhancement.accuracyBonus,
        critChance = critChance + enhancement.critBonus,
        dodgeChance = dodgeChance + enhancement.dodgeBonus,
        enhancements = enhancements + enhancement
    )
}

fun generateNpcMonster(yPos: Int, round: Int = 1): Fighter {
    val templates = listOf(
        FighterTemplate("Void Creeper", Faction.NPC, FighterClass.NPC, 1, 100, 40, 15, 25, 5, 10, 18, 15, 2, 95),
        FighterTemplate("Iron Husk", Faction.NPC, FighterClass.NPC, 1, 250, 0, 30, 0, 35, 20, 6, 0, 1, 100),
        FighterTemplate("Spectral Wisp", Faction.NPC, FighterClass.NPC, 1, 80, 100, 10, 40, 5, 25, 12, 25, 3, 90),
        FighterTemplate("Gorgon Guard", Faction.NPC, FighterClass.NPC, 1, 180, 60, 25, 10, 20, 15, 9, 10, 1, 95)
    )
    
    val template = templates.random()
    val multiplier = when {
        round >= 4 -> 4
        round >= 3 -> 2
        else -> 1
    }
    
    return instantiateFighter(template, false, yPos).copy(
        hp = template.hp * multiplier,
        maxHp = template.hp * multiplier,
        physAttack = template.pAtk * multiplier,
        magAttack = template.mAtk * multiplier,
        critChance = 0.05,
        dodgeChance = 0.05
    )
}
