package com.example

import com.example.fightmodel.*
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class FightModelTest {
    private fun stats(hp: Double = 100.0, atk: Double = 20.0, def: Double = 5.0, spd: Double = 10.0) =
        mapOf(Stat.MAX_HP to hp, Stat.ATTACK to atk, Stat.DEFENSE to def, Stat.SPEED to spd)

    @Test fun equipmentAndBuffsModifyStats() {
        val c = Combatant("A", stats(), listOf(Equipment("Sword", listOf(Modifier(Stat.ATTACK, flat = 10.0)))))
        assertEquals(30.0, c.stat(Stat.ATTACK), 0.001)
        c.addBuff(Buff("Rage", listOf(Modifier(Stat.ATTACK, percent = 0.5)), 2))
        assertEquals(45.0, c.stat(Stat.ATTACK), 0.001)
    }

    @Test fun equipmentRaisesStartingHp() {
        val c = Combatant("A", stats(), listOf(Equipment("Armor", listOf(Modifier(Stat.MAX_HP, flat = 50.0)))))
        assertEquals(150.0, c.hp, 0.001)
    }

    @Test fun skillDealsMoreDamageThanBasicAttack() {
        val skill = Skill("Slam", power = 2.0, cooldown = 3)
        val a = Combatant("A", stats(), skills = listOf(skill))
        val d = Combatant("D", stats())
        val e = FightModel(Random(1)).step(a, d).filterIsInstance<FightEvent.Attack>().first()
        assertEquals("Slam", e.skill)
        assertEquals(35.0, e.damage, 0.001)
        val e2 = FightModel(Random(1)).step(a, d).filterIsInstance<FightEvent.Attack>().first()
        assertNull(e2.skill)
    }

    @Test fun buffExpires() {
        val c = Combatant("A", stats())
        c.addBuff(Buff("B", listOf(Modifier(Stat.ATTACK, flat = 5.0)), 1))
        FightModel(Random(1)).step(c, Combatant("D", stats()))
        assertTrue(c.activeBuffs.isEmpty())
    }

    @Test fun duelIsDeterministicAndEquipmentWins() {
        val strong = Combatant("S", stats(), listOf(Equipment("Axe", listOf(Modifier(Stat.ATTACK, flat = 30.0)))))
        val weak = Combatant("W", stats())
        val r = FightModel(Random(42)).duel(strong, weak)
        assertEquals("S", r.winner?.name)
        assertTrue(r.events.any { it is FightEvent.Defeated })
    }
}
