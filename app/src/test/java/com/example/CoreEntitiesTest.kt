package com.example

import com.example.game.AbilityTargetType
import com.example.game.BattleResult
import com.example.game.BattleState
import com.example.game.Commander
import com.example.game.Faction
import com.example.game.Fighter
import com.example.game.FighterClass
import com.example.game.SpecialAbility
import com.example.game.instantiateFighter
import com.example.game.fighterPool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreEntitiesTest {

    @Test
    fun testCommanderAttributes() {
        val customAbility = SpecialAbility(
            name = "Dragon Kick",
            description = "Deals devastating physical damage and stuns foe.",
            damage = 45,
            manaCost = 50,
            targetType = AbilityTargetType.ENEMY_SINGLE
        )

        val commander = Commander(
            name = "Brus Li",
            title = "The Dragon Monk",
            abilityDescription = "Dragon Monk martial aura",
            hp = 120,
            maxHp = 120,
            cost = 3,
            preferredFaction = Faction.REPTILIANS,
            preferredClass = FighterClass.KNUCKLE_POWER,
            damage = 25,
            specialAbility = customAbility
        )

        assertEquals("Brus Li", commander.name)
        assertEquals("The Dragon Monk", commander.title)
        assertEquals(120, commander.health)
        assertEquals(120, commander.maxHealth)
        assertEquals(25, commander.damage)
        assertTrue(commander.isAlive)
        assertNotNull(commander.specialAbility)
        assertEquals("Dragon Kick", commander.specialAbility?.name)
        assertEquals(45, commander.specialAbility?.damage)
        assertEquals(50, commander.specialAbility?.manaCost)
        assertEquals(AbilityTargetType.ENEMY_SINGLE, commander.specialAbility?.targetType)
    }

    @Test
    fun testCommanderDefaultAbilityGeneration() {
        val commander = Commander(
            name = "Steel Bastion",
            abilityDescription = "Unbreakable Bastion: Shields nearby allies",
            hp = 150,
            damage = 18
        )

        assertEquals(150, commander.health)
        assertEquals(18, commander.damage)
        assertNotNull(commander.effectiveAbility)
        assertEquals("Steel Bastion", commander.effectiveAbility.name)
        assertEquals(18, commander.effectiveAbility.damage)
    }

    @Test
    fun testFighterAttributesAndSpecialAbility() {
        val ability = SpecialAbility(
            name = "Piercing Arrow",
            description = "Fires a wind-infused arrow that pierces armor.",
            damage = 60,
            manaCost = 70,
            targetType = AbilityTargetType.ENEMY_FURTHEST
        )

        val fighter = Fighter(
            name = "Gale Marksman",
            faction = Faction.ELEMOS,
            fighterClass = FighterClass.HUNTERS,
            cost = 1,
            health = 110,
            damage = 28,
            defense = 12,
            specialAbility = ability
        )

        assertEquals("Gale Marksman", fighter.name)
        assertEquals(110, fighter.health)
        assertEquals(110, fighter.hp)
        assertEquals(28, fighter.damage)
        assertEquals(28, fighter.attackPower)
        assertEquals(28, fighter.physAttack)
        assertEquals(12, fighter.defense)
        assertTrue(fighter.isAlive)
        assertNotNull(fighter.specialAbility)
        assertEquals("Piercing Arrow", fighter.specialAbility?.name)
        assertEquals(60, fighter.specialAbility?.damage)
    }

    @Test
    fun testInstantiateFighterPopulatesSpecialAbilities() {
        val template = fighterPool.first { it.name == "Brus Li" }
        val fighter = instantiateFighter(template, isPlayer = true, yPos = 4)

        assertNotNull(fighter.specialAbility)
        assertTrue(fighter.specialAbility!!.name.contains("Dragon Strike"))
        assertTrue(fighter.specialAbility!!.damage > 0)
        assertEquals(fighter.health, fighter.hp)
        assertEquals(fighter.damage, fighter.physAttack)
    }

    @Test
    fun testBattleStateCoreEntity() {
        val playerCmd = Commander(name = "Player Commander", hp = 100, damage = 20)
        val opponentCmd = Commander(name = "Enemy Commander", hp = 80, damage = 18)

        val playerFighter1 = Fighter(name = "Hero 1", health = 100, damage = 25)
        val playerFighter2 = Fighter(name = "Hero 2", health = 80, damage = 20)
        val enemyFighter1 = Fighter(name = "Monster 1", health = 90, damage = 15)

        val battleState = BattleState(
            round = 2,
            fight = 1,
            playerCommander = playerCmd,
            opponentCommander = opponentCmd,
            playerFighters = listOf(playerFighter1, playerFighter2),
            opponentFighters = listOf(enemyFighter1),
            isBattleActive = true,
            activeSynergies = listOf("Knuckle Power (2)", "Tuls (2)")
        )

        assertEquals(2, battleState.round)
        assertEquals(1, battleState.fight)
        assertTrue(battleState.isBattleActive)
        assertEquals(2, battleState.livingPlayerFighters.size)
        assertEquals(1, battleState.livingOpponentFighters.size)

        assertEquals(180, battleState.totalPlayerHealth)
        assertEquals(45, battleState.totalPlayerDamage)
        assertEquals(90, battleState.totalOpponentHealth)
        assertEquals(15, battleState.totalOpponentDamage)
        assertFalse(battleState.isGameOver)

        // Simulate combat outcome
        val completedState = battleState.copy(
            isBattleActive = false,
            battleResult = BattleResult.PLAYER_WIN
        )
        assertTrue(completedState.isGameOver)
    }
}
