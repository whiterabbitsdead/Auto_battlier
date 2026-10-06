package com.example

import com.example.game.CombatEngine
import com.example.game.CombatEngine.BattleOutcome
import com.example.game.CombatEngine.Combatant
import com.example.game.CombatEngine.Companion.toCombatant
import com.example.game.CombatEngine.TargetingStrategy
import com.example.game.CombatEngine.Team
import com.example.game.CombatLog
import com.example.game.Fighter
import com.example.game.FighterClass
import com.example.game.SpecialAbility
import com.example.game.getFighterSpecialAbility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class CombatEngineTest {

    @Test
    fun testSpeedDeterminesInitiativeOrder() {
        val engine = CombatEngine()

        // Fast fighter with high speed
        val fastFighter = Combatant(
            id = "fast-1",
            name = "Swift Rogue",
            hp = 100,
            attack = 20,
            speed = 50,
            team = Team.TEAM_A
        )

        // Slow fighter with low speed
        val slowFighter = Combatant(
            id = "slow-1",
            name = "Heavy Golem",
            hp = 100,
            attack = 20,
            speed = 10,
            team = Team.TEAM_B
        )

        val initialState = engine.initCombat(listOf(fastFighter), listOf(slowFighter))
        val nextState = engine.simulateRound(initialState)

        // The first action of the round should have been initiated by the faster fighter
        assertEquals(2, nextState.actionHistory.size)
        val firstAction = nextState.actionHistory[0]
        assertEquals("Swift Rogue", firstAction.attackerName)
        assertEquals(50, firstAction.attackerSpeed)

        val secondAction = nextState.actionHistory[1]
        assertEquals("Heavy Golem", secondAction.attackerName)
        assertEquals(10, secondAction.attackerSpeed)
    }

    @Test
    fun testAttackReducesTargetHpCorrectly() {
        val engine = CombatEngine()

        val attacker = Combatant(
            id = "att-1",
            name = "Striker",
            hp = 100,
            attack = 35,
            speed = 30,
            team = Team.TEAM_A
        )

        val target = Combatant(
            id = "tar-1",
            name = "Defender",
            hp = 100,
            attack = 10,
            speed = 10,
            defense = 10, // Mitigates defense / 2 = 5 damage
            team = Team.TEAM_B
        )

        // Expected damage = 35 - 5 = 30
        val damage = engine.calculateDamage(attacker, target)
        assertEquals(30, damage)

        val state = engine.initCombat(listOf(attacker), listOf(target))
        val round1 = engine.simulateRound(state)

        val defenderPostTurn = round1.teamBFighters.first()
        assertEquals(70, defenderPostTurn.hp) // 100 - 30 = 70
    }

    @Test
    fun testDefeatedFighterCannotActLaterInSameRound() {
        val engine = CombatEngine()

        // Faster fighter has enough attack to one-shot slower fighter
        val fastOneShotter = Combatant(
            id = "fast-killer",
            name = "Assassin",
            hp = 50,
            attack = 100,
            speed = 99,
            team = Team.TEAM_A
        )

        val slowVictim = Combatant(
            id = "slow-victim",
            name = "Slow Giant",
            hp = 60,
            attack = 50,
            speed = 10,
            team = Team.TEAM_B
        )

        val result = engine.simulate(listOf(fastOneShotter), listOf(slowVictim))

        // Battle should end in round 1
        assertEquals(1, result.totalRounds)
        assertEquals(1, result.totalActions) // Slow giant never got to attack because it was KO'd first
        assertEquals(BattleOutcome.TEAM_A_VICTORY, result.outcome)
        assertEquals(Team.TEAM_A, result.winnerTeam)

        // Assassin should be unscathed at 50 HP
        assertEquals(50, result.survivingTeamA.first().hp)
        assertTrue(result.survivingTeamB.isEmpty())
    }

    @Test
    fun testTeamBVictoryWhenTeamAIsEliminated() {
        val engine = CombatEngine()

        val weakTeamA = listOf(
            Combatant(name = "Weakling 1", hp = 20, attack = 5, speed = 10),
            Combatant(name = "Weakling 2", hp = 20, attack = 5, speed = 10)
        )

        val bossTeamB = listOf(
            Combatant(name = "Dragon Boss", hp = 500, attack = 50, speed = 40)
        )

        val result = engine.simulate(weakTeamA, bossTeamB)

        assertEquals(BattleOutcome.TEAM_B_VICTORY, result.outcome)
        assertEquals(Team.TEAM_B, result.winnerTeam)
        assertTrue(result.survivingTeamA.isEmpty())
        assertEquals(1, result.survivingTeamB.size)
        assertEquals("Dragon Boss", result.survivingTeamB.first().name)
    }

    @Test
    fun testTargetingStrategyLowestHp() {
        val engine = CombatEngine(targetingStrategy = TargetingStrategy.LOWEST_HP)

        val attacker = Combatant(name = "Sniper", hp = 100, attack = 20, speed = 50, team = Team.TEAM_A)

        val enemyTank = Combatant(id = "tank", name = "Tank", hp = 200, attack = 10, speed = 10, team = Team.TEAM_B)
        val enemyWounded = Combatant(id = "wounded", name = "Wounded Grunt", hp = 25, attack = 10, speed = 10, team = Team.TEAM_B)

        val state = engine.initCombat(listOf(attacker), listOf(enemyTank, enemyWounded))
        val round1 = engine.simulateRound(state)

        // The first action should target the wounded enemy because targeting is LOWEST_HP
        val firstAction = round1.actionHistory.first()
        assertEquals("Wounded Grunt", firstAction.targetName)
        assertEquals(5, firstAction.targetRemainingHp) // 25 - 20 = 5
    }

    @Test
    fun testTargetingStrategyHighestAttack() {
        val engine = CombatEngine(targetingStrategy = TargetingStrategy.HIGHEST_ATTACK)

        val attacker = Combatant(name = "Disrupter", hp = 100, attack = 15, speed = 50, team = Team.TEAM_A)

        val enemySupport = Combatant(name = "Support", hp = 80, attack = 5, speed = 10, team = Team.TEAM_B)
        val enemyNuker = Combatant(name = "Mage Nuker", hp = 80, attack = 60, speed = 10, team = Team.TEAM_B)

        val state = engine.initCombat(listOf(attacker), listOf(enemySupport, enemyNuker))
        val round1 = engine.simulateRound(state)

        val firstAction = round1.actionHistory.first()
        assertEquals("Mage Nuker", firstAction.targetName)
    }

    @Test
    fun testTargetingStrategyHighestSpeed() {
        val engine = CombatEngine(targetingStrategy = TargetingStrategy.HIGHEST_SPEED)

        val attacker = Combatant(name = "Speedster 1", hp = 100, attack = 15, speed = 100, team = Team.TEAM_A)

        val enemySlow = Combatant(name = "Slow Turtle", hp = 80, attack = 10, speed = 5, team = Team.TEAM_B)
        val enemyAgile = Combatant(name = "Agile Scout", hp = 80, attack = 10, speed = 60, team = Team.TEAM_B)

        val state = engine.initCombat(listOf(attacker), listOf(enemySlow, enemyAgile))
        val round1 = engine.simulateRound(state)

        val firstAction = round1.actionHistory.first()
        assertEquals("Agile Scout", firstAction.targetName)
    }

    @Test
    fun testSimulateBattleWithApplicationFighters() {
        // Test integration with the main game's Fighter class
        val playerFighters = listOf(
            Fighter(
                name = "Orc Warrior",
                hp = 120,
                physAttack = 25,
                attackSpeed = 20,
                physDefense = 10
            ),
            Fighter(
                name = "Elf Ranger",
                hp = 80,
                physAttack = 35,
                attackSpeed = 35,
                physDefense = 4
            )
        )

        val enemyFighters = listOf(
            Fighter(
                name = "Goblin Brute",
                hp = 100,
                physAttack = 20,
                attackSpeed = 15,
                physDefense = 6
            ),
            Fighter(
                name = "Dark Shaman",
                hp = 70,
                physAttack = 30,
                attackSpeed = 25,
                physDefense = 2
            )
        )

        val engine = CombatEngine()
        val result = engine.simulateBattle(playerFighters, enemyFighters)

        assertTrue(result.totalRounds > 0)
        assertTrue(result.actionHistory.isNotEmpty())
        assertTrue(result.outcome in listOf(BattleOutcome.TEAM_A_VICTORY, BattleOutcome.TEAM_B_VICTORY, BattleOutcome.DRAW))
        assertNotNull(result.mvp)
        assertTrue(result.damageDealtByFighter.isNotEmpty())
    }

    @Test
    fun testMaxRoundsDecidedByRemainingHp() {
        // High HP low attack fighters with maxRounds = 2
        val engine = CombatEngine(maxRounds = 2)

        val teamA = listOf(
            Combatant(name = "Tank A", hp = 1000, attack = 1, speed = 20)
        )
        val teamB = listOf(
            Combatant(name = "Tank B", hp = 500, attack = 1, speed = 10)
        )

        val result = engine.simulate(teamA, teamB)

        assertEquals(2, result.totalRounds)
        assertEquals(BattleOutcome.TEAM_A_VICTORY, result.outcome)
        assertTrue(result.logs.any { it.contains("Max rounds reached") })
    }

    @Test
    fun testEmptyTeamsProduceCorrectImmediateOutcomes() {
        val engine = CombatEngine()

        // Both empty
        val resultEmpty = engine.simulate(emptyList(), emptyList())
        assertEquals(BattleOutcome.DRAW, resultEmpty.outcome)

        // Team A empty
        val resultAEmpty = engine.simulate(emptyList(), listOf(Combatant(name = "Solo", hp = 50, attack = 10, speed = 10)))
        assertEquals(BattleOutcome.TEAM_B_VICTORY, resultAEmpty.outcome)

        // Team B empty
        val resultBEmpty = engine.simulate(listOf(Combatant(name = "Solo", hp = 50, attack = 10, speed = 10)), emptyList())
        assertEquals(BattleOutcome.TEAM_A_VICTORY, resultBEmpty.outcome)
    }

    @Test
    fun testToCombatantExtensionMapping() {
        val fighter = Fighter(
            name = "Shadow Blade",
            hp = 150,
            physAttack = 40,
            attackSpeed = 32,
            physDefense = 14
        )

        val combatant = fighter.toCombatant(Team.TEAM_B)

        assertEquals("Shadow Blade", combatant.name)
        assertEquals(150, combatant.hp)
        assertEquals(150, combatant.maxHp)
        assertEquals(40, combatant.attack)
        assertEquals(32, combatant.speed)
        assertEquals(14, combatant.defense)
        assertEquals(Team.TEAM_B, combatant.team)
        assertTrue(combatant.isAlive)
    }

    @Test
    fun testCombatLogRecordsAndRetrievesRecentHistory() {
        val log = CombatLog(maxCapacity = 20, defaultRecentLimit = 5)

        log.appendEvent("Event 1: Battle commenced")
        log.appendEvent("Event 2: Striker attacked Target")
        log.appendEvent("Event 3: Target defended")
        log.appendEvent("Event 4: Striker charged mana")
        log.appendEvent("Event 5: Striker cast special ability")
        log.appendEvent("Event 6: Target took heavy damage")
        log.appendEvent("Event 7: Target was defeated")

        assertEquals(7, log.size)

        // default limit is 5
        val defaultRecent = log.getRecentHistory()
        assertEquals(5, defaultRecent.size)
        assertEquals("Event 3: Target defended", defaultRecent[0])
        assertEquals("Event 7: Target was defeated", defaultRecent[4])

        // custom limit
        val recent3 = log.getRecentHistory(3)
        assertEquals(3, recent3.size)
        assertEquals("Event 5: Striker cast special ability", recent3[0])
        assertEquals("Event 7: Target was defeated", recent3[2])

        // All history
        val all = log.getAllHistory()
        assertEquals(7, all.size)
        assertEquals("Event 1: Battle commenced", all.first())
    }

    @Test
    fun testCombatLogFightLifecycle() {
        val log = CombatLog()
        val fight1Id = log.currentFightId
        log.appendEvent("Fight 1 Event 1")
        log.appendEvent("Fight 1 Event 2")
        assertEquals(2, log.getRecentHistory().size)

        // Start new fight
        log.startFight("fight-2")
        assertEquals("fight-2", log.currentFightId)
        assertTrue(log.getRecentHistory().isEmpty())

        log.appendEvent("Fight 2 Event 1")
        assertEquals(1, log.getRecentHistory().size)
        assertEquals("Fight 2 Event 1", log.getRecentHistory().first())

        // Overall history retains all events
        assertEquals(3, log.getAllHistory().size)

        // Clear current fight
        log.clearCurrentFight()
        assertTrue(log.getRecentHistory().isEmpty())
        assertEquals(3, log.getAllHistory().size)

        // Clear all
        log.clear()
        assertTrue(log.getAllHistory().isEmpty())
    }

    @Test
    fun testProcessTurnCalculatesDamageAndAppendsToCombatLog() {
        val log = CombatLog()
        val engine = CombatEngine(combatLog = log)

        val attacker = Combatant(
            id = "att-1",
            name = "Berserker",
            hp = 100,
            attack = 30,
            speed = 15,
            defense = 5,
            team = Team.TEAM_A
        )

        val target = Combatant(
            id = "tar-1",
            name = "Shieldbearer",
            hp = 80,
            attack = 10,
            speed = 8,
            defense = 10,
            team = Team.TEAM_B
        )

        val action = engine.processTurn(attacker, target, log)

        // Damage = 30 - 10 / 2 = 25
        assertEquals(25, action.damageDealt)
        assertEquals(55, target.hp)
        assertEquals(55, action.targetRemainingHp)
        assertFalse(action.isFatal)

        // Verify event appended to combat log
        val recent = log.getRecentHistory()
        assertEquals(1, recent.size)
        assertTrue(recent.first().contains("Berserker"))
        assertTrue(recent.first().contains("Shieldbearer"))
        assertTrue(recent.first().contains("25 DMG"))
    }

    @Test
    fun testProcessTurnSpecialAbilityActivation() {
        val log = CombatLog()
        val engine = CombatEngine(combatLog = log)

        val ability = SpecialAbility(
            name = "Dragon Strike",
            description = "Unleashes fierce burst",
            damage = 50,
            manaCost = 50,
            cooldownTurns = 3
        )

        val caster = Combatant(
            name = "Monk",
            hp = 120,
            attack = 20,
            speed = 20,
            defense = 10,
            mana = 100, // Ready to cast!
            specialAbility = ability
        )

        val target = Combatant(
            name = "Goblin",
            hp = 100,
            attack = 10,
            speed = 5,
            defense = 6
        )

        assertTrue(caster.isAbilityReady)

        val action = engine.processTurn(caster, target, log)

        assertTrue("Action should be a special ability", action.isSpecialAbility)
        assertEquals("Dragon Strike", action.abilityName)
        assertTrue("Damage should exceed normal attack", action.damageDealt >= 45)
        assertEquals(0, caster.mana) // Mana consumed
        assertEquals(3, caster.abilityCooldown) // Cooldown set

        val recent = log.getRecentHistory()
        assertTrue(recent.any { it.contains("Special Ability") && it.contains("Dragon Strike") })
    }

    @Test
    fun testUpgradedHeroesAreStrongerThanStockHeroes() {
        val engine = CombatEngine()

        val stockHero = CombatEngine.createStockHero(cost = 1, name = "Stock Bastion")
        val upgraded2Star = CombatEngine.upgradeCombatant(stockHero, targetStar = 2)
        val upgraded3Star = CombatEngine.upgradeCombatant(stockHero, targetStar = 3)

        // Verify stats progression
        assertTrue(upgraded2Star.hp > stockHero.hp)
        assertTrue(upgraded2Star.attack > stockHero.attack)
        assertTrue(upgraded3Star.hp > upgraded2Star.hp)
        assertTrue(upgraded3Star.attack > upgraded2Star.attack)

        // Verify damage calculation: upgraded hero deals more damage to same target
        val target = Combatant(name = "Target Dummy", hp = 500, attack = 10, speed = 10, defense = 10)

        val stockDamage = engine.calculateDamage(stockHero, target)
        val twoStarDamage = engine.calculateDamage(upgraded2Star, target)
        val threeStarDamage = engine.calculateDamage(upgraded3Star, target)

        assertTrue("2-star damage ($twoStarDamage) should exceed 1-star ($stockDamage)", twoStarDamage > stockDamage)
        assertTrue("3-star damage ($threeStarDamage) should exceed 2-star ($twoStarDamage)", threeStarDamage > twoStarDamage)
    }

    @Test
    fun testFiveGoldHeroStatsCloseToThreeStarOneGoldHero() {
        // 1-gold 1-star stock hero
        val stock1Gold = CombatEngine.createStockHero(cost = 1, name = "Stock 1-Gold")
        // Upgraded 3-star 1-gold hero
        val threeStar1Gold = CombatEngine.upgradeCombatant(stock1Gold, targetStar = 3)

        // 5-gold 1-star hero
        val fiveGoldHero = CombatEngine.createStockHero(cost = 5, name = "Cosmic 5-Gold")

        // HP Comparison: Both should be around 550 - 650 HP (within 25% margin)
        val hpDiffRatio = Math.abs(threeStar1Gold.hp - fiveGoldHero.hp).toDouble() / fiveGoldHero.hp
        assertTrue("HP difference ratio ($hpDiffRatio) should be within 25%", hpDiffRatio < 0.25)

        // Attack Comparison: Both should be around 80 - 90 Attack (within 20% margin)
        val atkDiffRatio = Math.abs(threeStar1Gold.attack - fiveGoldHero.attack).toDouble() / fiveGoldHero.attack
        assertTrue("Attack difference ratio ($atkDiffRatio) should be within 20%", atkDiffRatio < 0.20)

        // Both are formidable late-game tier powerhouses
        assertTrue("3-star 1-gold HP should be >= 500", threeStar1Gold.hp >= 500)
        assertTrue("5-gold HP should be >= 500", fiveGoldHero.hp >= 500)
        assertTrue("3-star 1-gold Attack should be >= 70", threeStar1Gold.attack >= 70)
        assertTrue("5-gold Attack should be >= 70", fiveGoldHero.attack >= 70)
    }

    @Test
    fun testProcessTurnWithFullStateAppendsAllRoundActionsToCombatLog() {
        val log = CombatLog()
        val engine = CombatEngine(combatLog = log)

        val teamA = listOf(
            Combatant(name = "Striker A", hp = 60, attack = 20, speed = 25, team = Team.TEAM_A)
        )
        val teamB = listOf(
            Combatant(name = "Defender B", hp = 60, attack = 15, speed = 10, team = Team.TEAM_B)
        )

        val initialState = engine.initCombat(teamA, teamB)
        val nextState = engine.processTurn(initialState, log)

        assertEquals(2, nextState.round)
        val recentLogs = log.getRecentHistory(count = 10)
        assertTrue(recentLogs.isNotEmpty())
        assertTrue(recentLogs.any { it.contains("Striker A") })
        assertTrue(recentLogs.any { it.contains("Defender B") })
    }
}
