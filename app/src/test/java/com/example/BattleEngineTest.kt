package com.example

import com.example.game.AbilityEffectType
import com.example.game.AbilityTargetType
import com.example.game.BattleEngine
import com.example.game.BattleResult
import com.example.game.BattleState
import com.example.game.Commander
import com.example.game.Equipment
import com.example.game.EquipmentType
import com.example.game.Faction
import com.example.game.Fighter
import com.example.game.FighterClass
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
}
