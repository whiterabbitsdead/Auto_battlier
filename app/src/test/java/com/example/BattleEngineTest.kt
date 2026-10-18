package com.example

import com.example.game.AbilityEffectType
import com.example.game.AbilityTargetType
import com.example.game.BattleEngine
import com.example.game.BattleResult
import com.example.game.BattleState
import com.example.game.Commander
import com.example.game.CombatCalculator
import com.example.game.DirectionalCombatEvent
import com.example.game.Equipment
import com.example.game.EquipmentType
import com.example.game.FacingDirection
import com.example.game.Faction
import com.example.game.Fighter
import com.example.game.FighterClass
import com.example.game.HitSector
import com.example.game.ShieldType
import com.example.game.SpecialAbility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class BattleEngineTest {

    @Test
    fun testCalculateDamageBasedOnPhysicalStats() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5 // Won't crit, won't miss, won't dodge
        }
        val engine = BattleEngine(deterministicRandom)

        // Attacker: physAttack = 50, critChance = 0.05
        val attacker = Fighter(
            name = "Iron Brawler",
            fighterClass = FighterClass.KNUCKLE_POWER,
            physAttack = 50,
            damage = 50,
            attackRange = 1,
            critChance = 0.05
        )

        // Target: physDefense = 20
        // Expected formula: max(1, 50 - 20/2) = 50 - 10 = 40
        val target = Fighter(
            name = "Training Dummy",
            physDefense = 20,
            defense = 20,
            dodgeChance = 0.0
        )

        val damage = engine.calculateDamage(attacker, target)
        assertEquals(40, damage)
    }

    @Test
    fun testCalculateDamageBasedOnMagicalStats() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val engine = BattleEngine(deterministicRandom)

        // Mage attacker: magAttack = 80 > physAttack = 10
        val mage = Fighter(
            name = "Astral Sage",
            fighterClass = FighterClass.ILLUMINS,
            magAttack = 80,
            physAttack = 10,
            damage = 10,
            attackRange = 2,
            accuracy = 100
        )

        // Target with high magDefense = 40
        // Expected formula: max(1, 80 - 40/2) = 80 - 20 = 60
        val target = Fighter(
            name = "Runic Golem",
            magDefense = 40,
            physDefense = 80,
            dodgeChance = 0.0
        )

        val damage = engine.calculateDamage(mage, target)
        assertEquals(60, damage)
    }

    @Test
    fun testEquipmentModifiersAffectDamage() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val engine = BattleEngine(deterministicRandom)

        val sword = Equipment(name = "Broadsword", type = EquipmentType.ATTACK)
        val shield = Equipment(name = "Tower Shield", type = EquipmentType.DEFENSE)

        val armedAttacker = Fighter(
            name = "Swordsman",
            physAttack = 30,
            damage = 30,
            attackRange = 1,
            equipment = listOf(sword) // +15 attack
        )

        val shieldedTarget = Fighter(
            name = "Guardian",
            physDefense = 20,
            defense = 20,
            equipment = listOf(shield) // +10 defense
        )

        // Attack = 30 + 15 = 45
        // Defense = 20 + 10 = 30
        // Net damage = 45 - (30 / 2) = 45 - 15 = 30
        val damage = engine.calculateDamage(armedAttacker, shieldedTarget)
        assertEquals(30, damage)
    }

    @Test
    fun testProcessTurnAdvancesTurnAndReducesHealth() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val engine = BattleEngine(deterministicRandom)

        val playerFighter = Fighter(
            name = "Hero Boxer",
            hp = 100,
            health = 100,
            physAttack = 40,
            damage = 40,
            attackSpeed = 15,
            attackRange = 1,
            x = 3,
            y = 4,
            startX = 3,
            startY = 4,
            isPlayer = true
        )

        val enemyFighter = Fighter(
            name = "Goblin Grunt",
            hp = 50,
            health = 50,
            physAttack = 10,
            damage = 10,
            physDefense = 0,
            defense = 0,
            attackSpeed = 5,
            attackRange = 1,
            x = 3,
            y = 4, // Adjacent / same tile for immediate melee
            startX = 3,
            startY = 4,
            isPlayer = false
        )

        val initialState = BattleState(
            round = 1,
            turn = 0,
            playerFighters = listOf(playerFighter),
            opponentFighters = listOf(enemyFighter),
            isBattleActive = true
        )

        val updatedState = engine.processTurn(initialState)

        // Turn should have incremented
        assertEquals(1, updatedState.turn)
        assertTrue("Damage dealt map should record player attack", updatedState.roundDamageDealt.isNotEmpty())

        // Opponent took damage (40 damage against 50 hp -> 10 hp remaining)
        val updatedEnemy = updatedState.opponentFighters.first()
        assertEquals(10, updatedEnemy.health)
        assertEquals(10, updatedEnemy.hp)
        assertTrue(updatedState.isBattleActive)
    }

    @Test
    fun testProcessTurnDefeatsEnemyAndUpdatesBattleResult() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val engine = BattleEngine(deterministicRandom)

        val playerFighter = Fighter(
            name = "Hero Boxer",
            hp = 100,
            health = 100,
            physAttack = 60,
            damage = 60,
            attackSpeed = 20,
            attackRange = 1,
            x = 2,
            y = 2,
            startX = 2,
            startY = 2,
            isPlayer = true
        )

        val frailEnemy = Fighter(
            name = "Frail Imp",
            hp = 20,
            health = 20,
            physDefense = 0,
            defense = 0,
            attackSpeed = 1,
            x = 2,
            y = 2,
            isPlayer = false
        )

        val initialState = BattleState(
            round = 1,
            turn = 0,
            playerFighters = listOf(playerFighter),
            opponentFighters = listOf(frailEnemy),
            isBattleActive = true
        )

        val finalState = engine.processTurn(initialState)

        assertEquals(1, finalState.turn)
        val enemy = finalState.opponentFighters.first()
        assertEquals(0, enemy.health)
        assertFalse(enemy.isAlive)
        assertFalse(finalState.isBattleActive)
        assertEquals(BattleResult.PLAYER_WIN, finalState.battleResult)
        assertTrue(finalState.battleLogs.any { it.contains("Victory") || it.contains("defeated") })
    }

    @Test
    fun testSpecialAbilityTriggerInTurnProcessing() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val engine = BattleEngine(deterministicRandom)

        val dragonKickAbility = SpecialAbility(
            name = "Dragon Meteor Kick",
            description = "Unleashes an explosive kick",
            damage = 75,
            manaCost = 50,
            targetType = AbilityTargetType.ENEMY_SINGLE
        )

        val martialArtist = Fighter(
            name = "Grandmaster",
            mana = 100, // Full mana to cast immediately
            maxMana = 100,
            hp = 150,
            health = 150,
            damage = 25,
            attackSpeed = 25,
            attackRange = 1,
            x = 1,
            y = 1,
            startX = 1,
            startY = 1,
            isPlayer = true,
            specialAbility = dragonKickAbility
        )

        val bossEnemy = Fighter(
            name = "Demon Lord",
            hp = 200,
            health = 200,
            attackSpeed = 5,
            attackRange = 1,
            x = 1,
            y = 1,
            isPlayer = false
        )

        val initialState = BattleState(
            round = 3,
            turn = 0,
            playerFighters = listOf(martialArtist),
            opponentFighters = listOf(bossEnemy),
            isBattleActive = true
        )

        val stateAfterTurn = engine.processTurn(initialState)

        // Caster spent full mana on special ability cast (reset to 0, then gained 10 from boss counter-attack)
        val casterAfterTurn = stateAfterTurn.playerFighters.first()
        assertEquals(10, casterAfterTurn.mana)

        // Boss should have received special ability damage (75 damage)
        val enemyAfterTurn = stateAfterTurn.opponentFighters.first()
        assertEquals(125, enemyAfterTurn.health)

        // Telemetry should track active special abilities and logs
        assertTrue(stateAfterTurn.activeSpecialAbilities.any { it.name == "Dragon Meteor Kick" })
        assertTrue(stateAfterTurn.battleLogs.any { it.contains("Dragon Meteor Kick") })
    }

    @Test
    fun testSimulateBattleToCompletionResolvesToEndState() {
        val player = Fighter(
            name = "Gladiator",
            hp = 120,
            health = 120,
            physAttack = 35,
            damage = 35,
            attackSpeed = 15,
            attackRange = 1,
            x = 1,
            y = 1,
            startX = 1,
            startY = 1,
            isPlayer = true
        )

        val opponent = Fighter(
            name = "Beast",
            hp = 70,
            health = 70,
            physAttack = 15,
            damage = 15,
            attackSpeed = 8,
            attackRange = 1,
            x = 1,
            y = 1,
            isPlayer = false
        )

        val engine = BattleEngine()
        val battleState = engine.initBattle(
            playerCommander = Commander(name = "Warlord", hp = 100),
            opponentCommander = Commander(name = "Shadow Lord", hp = 100),
            playerFighters = listOf(player),
            opponentFighters = listOf(opponent)
        )

        val finalState = engine.simulateBattleToCompletion(battleState, maxTurns = 50)

        assertFalse(finalState.isBattleActive)
        assertNotNull(finalState.battleResult)
        assertTrue(finalState.turn > 0)
        assertTrue(finalState.isGameOver)
    }

    @Test
    fun testFighterCooldownGatingAndTurnDecrement() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val engine = BattleEngine(deterministicRandom)

        val nukeAbility = SpecialAbility(
            name = "Mega Punch",
            description = "Deals massive burst damage",
            damage = 80,
            manaCost = 50,
            cooldownTurns = 3,
            targetType = AbilityTargetType.ENEMY_SINGLE
        )

        // Starts with ability on cooldown (abilityCooldown = 2)
        val fighter = Fighter(
            name = "Brawler",
            mana = 100,
            maxMana = 100,
            hp = 200,
            health = 200,
            damage = 10,
            physAttack = 10,
            attackSpeed = 20,
            attackRange = 1,
            x = 1,
            y = 1,
            startX = 1,
            startY = 1,
            isPlayer = true,
            specialAbility = nukeAbility,
            abilityCooldown = 2
        )

        val targetDummy = Fighter(
            name = "Target Dummy",
            hp = 300,
            health = 300,
            physDefense = 0,
            defense = 0,
            attackSpeed = 5,
            x = 1,
            y = 1,
            isPlayer = false
        )

        val initialState = BattleState(
            round = 1,
            turn = 0,
            playerFighters = listOf(fighter),
            opponentFighters = listOf(targetDummy),
            isBattleActive = true
        )

        // Turn 1: Cooldown should decrement from 2 to 1; ability is still locked!
        val stateTurn1 = engine.processTurn(initialState)
        val actorTurn1 = stateTurn1.playerFighters.first()
        assertEquals("Cooldown should decrement to 1 on Turn 1", 1, actorTurn1.abilityCooldown)
        assertFalse("Special ability should not have triggered while on cooldown",
            stateTurn1.activeSpecialAbilities.any { it.name == "Mega Punch" })

        // Turn 2: Cooldown decrements from 1 to 0; now ready to cast!
        val stateTurn2 = engine.processTurn(stateTurn1)
        val actorTurn2 = stateTurn2.playerFighters.first()
        // Special ability triggered, resetting cooldown to cooldownTurns (3)
        assertEquals(3, actorTurn2.abilityCooldown)
        assertTrue("Mega Punch should trigger once cooldown reaches 0",
            stateTurn2.activeSpecialAbilities.any { it.name == "Mega Punch" })
        assertTrue("Battle log should record Mega Punch cast",
            stateTurn2.battleLogs.any { it.contains("Mega Punch") })
    }

    @Test
    fun testHealingSpecialAbilityRestoresHealth() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val engine = BattleEngine(deterministicRandom)

        val holyLight = SpecialAbility(
            name = "Holy Light",
            description = "Restores 50 health to the caster",
            healAmount = 50,
            damage = 0,
            manaCost = 50,
            cooldownTurns = 3,
            effectType = AbilityEffectType.HEAL,
            targetType = AbilityTargetType.SELF
        )

        // Fighter damaged down to 30/100 HP, ready to cast
        val paladin = Fighter(
            name = "Paladin",
            mana = 100,
            maxMana = 100,
            hp = 30,
            maxHp = 100,
            health = 30,
            attackSpeed = 20,
            attackRange = 1,
            x = 1,
            y = 1,
            startX = 1,
            startY = 1,
            isPlayer = true,
            specialAbility = holyLight,
            abilityCooldown = 0
        )

        val enemy = Fighter(
            name = "Goblin",
            hp = 150,
            health = 150,
            physAttack = 5,
            attackSpeed = 5,
            attackRange = 1,
            x = 1,
            y = 1,
            isPlayer = false
        )

        val initialState = BattleState(
            round = 1,
            turn = 0,
            playerFighters = listOf(paladin),
            opponentFighters = listOf(enemy),
            isBattleActive = true
        )

        val stateAfterTurn = engine.processTurn(initialState)
        val paladinAfter = stateAfterTurn.playerFighters.first()

        // Paladin healed +50 HP from 30 -> 80 (then took 5 dmg from goblin attack -> 75 HP)
        assertTrue("Paladin should have healed from initial 30 HP", paladinAfter.health > 50)
        assertTrue("Battle logs should reflect healing",
            stateAfterTurn.battleLogs.any { it.contains("Holy Light") && it.contains("healing self") })
        assertEquals("Cooldown should be set after casting", 3, paladinAfter.abilityCooldown)
    }

    @Test
    fun testMultiAttackSpecialAbilityStrikesMultipleTimes() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val engine = BattleEngine(deterministicRandom)

        val flurryStrike = SpecialAbility(
            name = "Triple Flurry",
            description = "Strikes 3 times in rapid succession",
            damage = 60,
            manaCost = 50,
            cooldownTurns = 2,
            effectType = AbilityEffectType.MULTI_ATTACK,
            multiAttackHits = 3,
            targetType = AbilityTargetType.ENEMY_SINGLE
        )

        val monk = Fighter(
            name = "Wind Monk",
            mana = 100,
            maxMana = 100,
            hp = 150,
            health = 150,
            damage = 20,
            attackSpeed = 20,
            attackRange = 1,
            x = 1,
            y = 1,
            startX = 1,
            startY = 1,
            isPlayer = true,
            specialAbility = flurryStrike,
            abilityCooldown = 0
        )

        val boss = Fighter(
            name = "Ogre Chieftain",
            hp = 200,
            health = 200,
            physDefense = 0,
            defense = 0,
            attackSpeed = 5,
            attackRange = 1,
            x = 1,
            y = 1,
            isPlayer = false
        )

        val initialState = BattleState(
            round = 1,
            turn = 0,
            playerFighters = listOf(monk),
            opponentFighters = listOf(boss),
            isBattleActive = true
        )

        val stateAfterTurn = engine.processTurn(initialState)
        val bossAfter = stateAfterTurn.opponentFighters.first()

        // Total damage dealt: 60 (3 strikes of 20 DMG each)
        assertEquals("Boss should have taken full multi-attack damage (200 - 60 = 140)", 140, bossAfter.health)
        assertTrue("Logs should describe the 3-strike combo",
            stateAfterTurn.battleLogs.any { it.contains("Triple Flurry") && it.contains("combo") })
        assertTrue("Logs should record individual combo strikes",
            stateAfterTurn.battleLogs.any { it.contains("Strike 1/3") || it.contains("Strike 2/3") || it.contains("Strike 3/3") })
        assertEquals(2, stateAfterTurn.playerFighters.first().abilityCooldown)
    }

    @Test
    fun testAllyHealingRestoresWoundedTeammate() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val engine = BattleEngine(deterministicRandom)

        val clericBlessing = SpecialAbility(
            name = "Astral Mend",
            description = "Heals the most wounded ally for 45 HP",
            healAmount = 45,
            manaCost = 50,
            cooldownTurns = 3,
            effectType = AbilityEffectType.HEAL,
            targetType = AbilityTargetType.ALLY_SINGLE
        )

        val cleric = Fighter(
            name = "Cleric",
            mana = 100,
            hp = 100,
            health = 100,
            attackSpeed = 25,
            attackRange = 3,
            x = 0,
            y = 1,
            startX = 0,
            startY = 1,
            isPlayer = true,
            specialAbility = clericBlessing,
            abilityCooldown = 0
        )

        val woundedWarrior = Fighter(
            name = "Wounded Warrior",
            hp = 20,
            maxHp = 100,
            health = 20,
            attackSpeed = 10,
            attackRange = 1,
            x = 1,
            y = 1,
            startX = 1,
            startY = 1,
            isPlayer = true
        )

        val enemy = Fighter(
            name = "Orc",
            hp = 100,
            health = 100,
            attackSpeed = 5,
            attackRange = 1,
            x = 2,
            y = 1,
            isPlayer = false
        )

        val initialState = BattleState(
            round = 1,
            turn = 0,
            playerFighters = listOf(cleric, woundedWarrior),
            opponentFighters = listOf(enemy),
            isBattleActive = true
        )

        val stateAfterTurn = engine.processTurn(initialState)
        val warriorAfter = stateAfterTurn.playerFighters.find { it.name == "Wounded Warrior" }!!

        // Wounded warrior healed by 45 (from 20 -> 65 HP, before enemy attack)
        assertTrue("Wounded Warrior should have received healing", warriorAfter.health > 20)
        assertTrue("Log should mention healing Wounded Warrior",
            stateAfterTurn.battleLogs.any { it.contains("Astral Mend") && it.contains("Wounded Warrior") })
    }

    @Test
    fun testDirectionalIncidentAngleAndHitSectorClassification() {
        val target = Fighter(name = "Target", x = 3, y = 3, facing = FacingDirection.NORTH)

        // Frontal: directly North (0°)
        val frontAttacker = Fighter(name = "Front", x = 3, y = 2)
        val frontAngle = CombatCalculator.calculateIncidentAngle(frontAttacker, target)
        assertEquals(0.0, frontAngle, 0.01)
        assertEquals(HitSector.FRONTAL, HitSector.fromIncidentAngle(frontAngle))

        // Flanking: directly East (90°)
        val flankAttacker = Fighter(name = "Flank", x = 4, y = 3)
        val flankAngle = CombatCalculator.calculateIncidentAngle(flankAttacker, target)
        assertEquals(90.0, flankAngle, 0.01)
        assertEquals(HitSector.FLANKING, HitSector.fromIncidentAngle(flankAngle))

        // Rear: directly South (180°)
        val rearAttacker = Fighter(name = "Rear", x = 3, y = 4)
        val rearAngle = CombatCalculator.calculateIncidentAngle(rearAttacker, target)
        assertEquals(180.0, rearAngle, 0.01)
        assertEquals(HitSector.REAR, HitSector.fromIncidentAngle(rearAngle))

        // Sector boundaries check
        assertEquals(HitSector.FRONTAL, HitSector.fromIncidentAngle(59.9))
        assertEquals(HitSector.FLANKING, HitSector.fromIncidentAngle(60.1))
        assertEquals(HitSector.FLANKING, HitSector.fromIncidentAngle(119.9))
        assertEquals(HitSector.REAR, HitSector.fromIncidentAngle(120.1))
    }

    @Test
    fun testBackstabDamageWith80PercentArmorPenetrationAndGuaranteedCrit() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5 // Default would not crit (critChance is 0.0)
        }
        val target = Fighter(
            name = "Iron Golem",
            hp = 200,
            health = 200,
            physDefense = 50,
            x = 3,
            y = 3,
            facing = FacingDirection.NORTH
        )
        val attacker = Fighter(
            name = "Shadow Rogue",
            physAttack = 50,
            damage = 50,
            critChance = 0.0,
            x = 3,
            y = 4, // Directly behind target (Rear Arc)
            facing = FacingDirection.NORTH
        )

        val result = CombatCalculator.calculateAttack(attacker, target, deterministicRandom)

        assertEquals(HitSector.REAR, result.hitSector)
        assertTrue("Rear strike should guarantee critical hit", result.isCrit)
        assertEquals(DirectionalCombatEvent.CRITICAL_BACKSTAB, result.directionalEvent)

        // Effective Defense with 80% penetration = 50 * 0.2 = 10
        // Base damage = 50 - 10 / 2 = 45
        // Guaranteed 2.0x crit multiplier = 45 * 2.0 = 90
        assertEquals(90, result.damage)
    }

    @Test
    fun testFlankingAttackArmorBypassAndGlancingFlankEvent() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.9 // Prevent random crit
        }
        val target = Fighter(
            name = "Armored Sentinel",
            hp = 100,
            health = 100,
            physDefense = 50,
            x = 3,
            y = 3,
            facing = FacingDirection.NORTH
        )
        val flankAttacker = Fighter(
            name = "Skirmisher",
            physAttack = 50,
            damage = 50,
            critChance = 0.0,
            x = 4,
            y = 3, // Flanking side (90°)
            facing = FacingDirection.WEST
        )

        val result = CombatCalculator.calculateAttack(flankAttacker, target, deterministicRandom)

        assertEquals(HitSector.FLANKING, result.hitSector)
        assertEquals(DirectionalCombatEvent.GLANCING_FLANK, result.directionalEvent)

        // Effective Defense with 40% bypass = 50 * 0.6 = 30
        // Base damage = 50 - 30 / 2 = 35 (compared to 25 for frontal defense)
        assertEquals(35, result.damage)
    }

    @Test
    fun testDirectionalShieldDeflectionAndRearShieldBypass() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val target = Fighter(
            name = "Shield Tank",
            hp = 100,
            health = 100,
            physDefense = 0,
            shield = 40.0f,
            shieldType = ShieldType.TOWER, // 180° front coverage
            x = 3,
            y = 3,
            facing = FacingDirection.NORTH
        )

        // Frontal attack hits tower shield
        val frontAttacker = Fighter(
            name = "Front Attacker",
            physAttack = 30,
            damage = 30,
            critChance = 0.0,
            x = 3,
            y = 2
        )
        val frontResult = CombatCalculator.calculateAttack(frontAttacker, target, deterministicRandom)
        assertEquals(DirectionalCombatEvent.SHIELD_ARC_DEFLECTED, frontResult.directionalEvent)
        assertEquals(30, frontResult.shieldAbsorbed)
        assertEquals(0, frontResult.damage)
        assertEquals(10.0f, target.shield, 0.01f)

        // Rear attack completely bypasses the remaining shield
        val rearAttacker = Fighter(
            name = "Rear Assassin",
            physAttack = 20,
            damage = 20,
            critChance = 0.0,
            x = 3,
            y = 4
        )
        val rearResult = CombatCalculator.calculateAttack(rearAttacker, target, deterministicRandom)
        assertEquals(HitSector.REAR, rearResult.hitSector)
        assertEquals(DirectionalCombatEvent.CRITICAL_BACKSTAB, rearResult.directionalEvent)
        assertEquals(0, rearResult.shieldAbsorbed)
        // Shield untouched by rear attack
        assertEquals(10.0f, target.shield, 0.01f)
        // Damage dealt directly to health (20 * 2.0 crit = 40)
        assertEquals(40, rearResult.damage)
    }

    @Test
    fun testPostureDepletionAndGuardBreakExposure() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val target = Fighter(
            name = "Guard Defender",
            hp = 100,
            health = 100,
            physDefense = 40,
            posture = 100.0f,
            isGuardBroken = false,
            x = 3,
            y = 3,
            facing = FacingDirection.NORTH
        )

        // Deplete all posture to break guard
        target.depletePosture(100.0f)
        assertEquals(0.0f, target.posture, 0.01f)
        assertTrue("Target guard should be broken", target.isGuardBroken)

        // Attack against guard-broken target
        val attacker = Fighter(
            name = "Attacker",
            physAttack = 40,
            damage = 40,
            critChance = 0.0,
            x = 3,
            y = 2 // Frontal
        )
        val result = CombatCalculator.calculateAttack(attacker, target, deterministicRandom)
        assertEquals(DirectionalCombatEvent.GUARD_BROKEN_EXPOSURE, result.directionalEvent)

        // Guard broken grants 50% defense bypass: 40 * 0.5 = 20 effective def
        // Damage = 40 - 20 / 2 = 30 (instead of 40 - 20 = 20)
        assertEquals(30, result.damage)
    }

    @Test
    fun testPhalanxInterlockingFormationDefense() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        // Center Defender and Right Flank Ally forming an interlocking shield wall
        val target = Fighter(
            id = "target-1",
            name = "Center Defender",
            hp = 100,
            health = 100,
            physDefense = 40,
            shieldType = ShieldType.STANDARD,
            x = 3,
            y = 3,
            facing = FacingDirection.NORTH
        )
        val allyRight = Fighter(
            id = "ally-1",
            name = "Right Wing Ally",
            hp = 100,
            health = 100,
            physDefense = 40,
            shieldType = ShieldType.TOWER,
            x = 4,
            y = 3, // Adjacent on the right
            facing = FacingDirection.NORTH // Facing same forward direction
        )

        // Attacker attempts to flank target from the right (x = 5)
        val flankAttacker = Fighter(
            name = "Flank Raider",
            physAttack = 50,
            damage = 50,
            critChance = 0.0,
            x = 5,
            y = 3
        )

        val result = CombatCalculator.calculateAttack(
            flankAttacker,
            target,
            deterministicRandom,
            targetAllies = listOf(target, allyRight)
        )

        // Phalanx converts flanking attack into an interlocked frontal defense
        assertTrue("Target should be protected by phalanx wall", result.isPhalanxProtected)
        assertEquals(HitSector.FRONTAL, result.hitSector)
        assertEquals(DirectionalCombatEvent.PHALANX_INTERLOCKED_DEFENSE, result.directionalEvent)
        // Full defense applies without flanking bypass: 50 - 40 / 2 = 30
        assertEquals(30, result.damage)
    }

    @Test
    fun testTurningInertiaPreventsInstant180Spin() {
        // Heavy fighter with 90° turning limit per action
        val heavyFighter = Fighter(
            name = "Heavy Ironclad",
            x = 3,
            y = 3,
            facing = FacingDirection.NORTH,
            turnSpeedDegrees = 90.0
        )

        // Target directly behind to the SOUTH (180° rotation requested)
        heavyFighter.turnTowards(3, 5)

        // Should NOT face SOUTH immediately; should turn 90° towards EAST or WEST
        assertTrue("Heavy fighter should not turn 180° instantly", heavyFighter.facing != FacingDirection.SOUTH)
        assertTrue(
            "Heavy fighter should rotate 90° to EAST or WEST",
            heavyFighter.facing == FacingDirection.EAST || heavyFighter.facing == FacingDirection.WEST
        )

        // A second turn allows completing the turn to SOUTH
        heavyFighter.turnTowards(3, 5)
        assertEquals(FacingDirection.SOUTH, heavyFighter.facing)
    }

    @Test
    fun testMovementUpdatesFacingToDirectionOfTravel() {
        val fighter = Fighter(name = "Scout", x = 2, y = 4, facing = FacingDirection.NORTH)
        val engine = BattleEngine()

        // Move North: (2, 4) -> (2, 3)
        val targetNorth = Fighter(name = "TargetN", x = 2, y = 1)
        engine.moveTowards(fighter, targetNorth)
        assertEquals(2, fighter.x)
        assertEquals(3, fighter.y)
        assertEquals(FacingDirection.NORTH, fighter.facing)

        // Move East: (2, 3) -> (3, 3)
        val targetEast = Fighter(name = "TargetE", x = 5, y = 3)
        engine.moveTowards(fighter, targetEast)
        assertEquals(3, fighter.x)
        assertEquals(3, fighter.y)
        assertEquals(FacingDirection.EAST, fighter.facing)

        // Move South: (3, 3) -> (3, 4)
        val targetSouth = Fighter(name = "TargetS", x = 3, y = 6)
        engine.moveTowards(fighter, targetSouth)
        assertEquals(3, fighter.x)
        assertEquals(4, fighter.y)
        assertEquals(FacingDirection.SOUTH, fighter.facing)
    }

    @Test
    fun testTauntAsPositionalAnchor() {
        val engine = BattleEngine()
        val tank = Fighter(
            id = "tank-1",
            name = "Paladin Tank",
            fighterClass = FighterClass.TANK,
            x = 3,
            y = 3,
            facing = FacingDirection.NORTH,
            isPlayer = true
        )
        val enemy = Fighter(
            id = "enemy-1",
            name = "Enemy Assassin",
            x = 3,
            y = 4,
            facing = FacingDirection.SOUTH,
            isPlayer = false
        )

        val battleState = BattleState(
            round = 1,
            turn = 0,
            playerFighters = listOf(tank),
            opponentFighters = listOf(enemy),
            isBattleActive = true
        )

        val nextState = engine.processTurn(battleState)
        val enemyAfter = nextState.opponentFighters.first()

        // Tank should taunt adjacent enemy, forcing enemy to target and face tank
        assertEquals("Enemy should be taunted by tank", tank.id, enemyAfter.tauntedById)
        assertEquals("Enemy should face tank to the North", FacingDirection.NORTH, enemyAfter.facing)
    }

    @Test
    fun testActiveDeflectionCounterMitigatesDamageAndReflectsPosture() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val target = Fighter(
            name = "Shieldmaster",
            hp = 100,
            physDefense = 30,
            deflectionChance = 1.0f,
            dodgeChance = 0.0,
            x = 3,
            y = 3,
            facing = FacingDirection.NORTH
        )
        val attacker = Fighter(
            name = "Aggressor",
            physAttack = 40,
            damage = 40,
            posture = 100.0f,
            critChance = 0.0,
            x = 3,
            y = 2, // Frontal strike
            facing = FacingDirection.SOUTH
        )

        val result = CombatCalculator.calculateAttack(attacker, target, deterministicRandom)

        assertEquals(DirectionalCombatEvent.DEFLECTION_COUNTER, result.directionalEvent)
        assertTrue("Attack should be deflected", result.isDeflected)
        // Deflection mitigates 75% damage: (40 - 15) * 0.25 = 6
        assertEquals(6, result.damage)
        // Reflected posture: 30 * 0.4 + 15 = 27 posture loss on attacker
        assertEquals(73.0f, attacker.posture, 0.1f)
    }

    @Test
    fun testPoiseMitigatesPostureDamageAndStaggersAtThreshold() {
        val normalFighter = Fighter(
            name = "Normal Fighter",
            posture = 100.0f,
            maxPosture = 100.0f,
            poise = 100.0f
        )
        val heavyJuggernaut = Fighter(
            name = "Heavy Juggernaut",
            posture = 100.0f,
            maxPosture = 100.0f,
            poise = 200.0f
        )

        // Deplete 60 posture
        normalFighter.depletePosture(60.0f)
        heavyJuggernaut.depletePosture(60.0f)

        // Normal loses 60/1.0 = 60 posture (remaining 40)
        assertEquals(40.0f, normalFighter.posture, 0.01f)
        assertFalse("At 40%, normal fighter should not yet be staggered", normalFighter.isStaggered)

        // Heavy loses 60/2.0 = 30 posture (remaining 70)
        assertEquals(70.0f, heavyJuggernaut.posture, 0.01f)
        assertFalse(heavyJuggernaut.isStaggered)

        // Deplete 10 more from normal fighter: drops to 30 posture (<= 35%) -> Stagger!
        normalFighter.depletePosture(10.0f)
        assertEquals(30.0f, normalFighter.posture, 0.01f)
        assertTrue("Normal fighter should enter staggered state at <= 35% posture", normalFighter.isStaggered)
        assertFalse("Staggered fighter is not guard broken", normalFighter.isGuardBroken)
    }

    @Test
    fun testStaggerVulnerabilityIncreasesDamage() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5 // Non-crit roll
        }
        val target = Fighter(
            name = "Staggered Defender",
            hp = 100,
            physDefense = 40,
            posture = 30.0f,
            isStaggered = true,
            x = 3,
            y = 3,
            facing = FacingDirection.NORTH
        )
        val attacker = Fighter(
            name = "Striker",
            physAttack = 50,
            damage = 50,
            critChance = 0.0,
            x = 3,
            y = 2, // Frontal
            facing = FacingDirection.SOUTH
        )

        val result = CombatCalculator.calculateAttack(attacker, target, deterministicRandom)

        assertEquals(DirectionalCombatEvent.POSTURE_STAGGERED, result.directionalEvent)
        // Stagger grants 30% defense bypass: 40 * 0.7 = 28 effective def
        // Damage = 50 - 28/2 = 36 (instead of 50 - 20 = 30)
        assertEquals(36, result.damage)
    }

    @Test
    fun testDeathblowExecutionOnGuardBrokenTarget() {
        val deterministicRandom = object : Random() {
            override fun nextDouble(): Double = 0.5
        }
        val target = Fighter(
            name = "Exposed Defender",
            hp = 100,
            maxHp = 100,
            physDefense = 40,
            posture = 0.0f,
            isGuardBroken = true,
            dodgeChance = 0.0,
            x = 3,
            y = 3,
            facing = FacingDirection.NORTH
        )
        val attacker = Fighter(
            name = "Executioner",
            physAttack = 40,
            damage = 40,
            critChance = 1.0, // Guaranteed deathblow execution
            x = 3,
            y = 2,
            facing = FacingDirection.SOUTH
        )

        val result = CombatCalculator.calculateAttack(attacker, target, deterministicRandom)

        assertEquals(DirectionalCombatEvent.DEATHBLOW_EXECUTION, result.directionalEvent)
        assertTrue("Result should be marked as deathblow", result.isDeathblow)
        // Effective def: 40 * 0.2 = 8 -> base dmg: 40 - 4 = 36
        // Crit multiplier 2.5x: 36 * 2.5 = 90
        // +10% target max HP execute: 90 + 10 = 100
        assertEquals(100, result.damage)
    }

    @Test
    fun testDynamicPostureRecoveryWhenUndisturbedVsPressured() {
        val undisturbed = Fighter(
            name = "Undisturbed",
            posture = 20.0f,
            maxPosture = 100.0f,
            isStaggered = true,
            tookDamageThisTurn = false
        )
        val pressured = Fighter(
            name = "Pressured",
            posture = 20.0f,
            maxPosture = 100.0f,
            isStaggered = true,
            tookDamageThisTurn = true
        )

        // Turn tick
        undisturbed.tickTurnStatus()
        pressured.tickTurnStatus()

        // Undisturbed recovers 25.0f posture: 20 + 25 = 45 (> 35% -> stagger cleared)
        assertEquals(45.0f, undisturbed.posture, 0.01f)
        assertFalse("Undisturbed fighter recovers out of stagger", undisturbed.isStaggered)

        // Pressured recovers only 12.0f posture: 20 + 12 = 32 (<= 35% -> remains staggered)
        assertEquals(32.0f, pressured.posture, 0.01f)
        assertTrue("Pressured fighter remains staggered", pressured.isStaggered)
        assertFalse("tookDamageThisTurn should reset after tick", pressured.tookDamageThisTurn)
    }
}
