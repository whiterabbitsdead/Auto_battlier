package com.example

import com.example.game.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class MockBattleTest {
    private val footman = FighterTemplate("Footman")
    private val tank = FighterTemplate("Brute", hp = 100000, attack = 100, defense = 50)

    @Test fun largeArmiesResolve() {
        val r = MockBattleSimulator.simulate(
            MockArmy("A", listOf(ArmyEntry(footman, 200))),
            MockArmy("B", listOf(ArmyEntry(footman, 5))), Random(1), recordLog = false)
        assertEquals(MockWinner.LEFT, r.winner)
    }

    @Test fun emptySideLoses() {
        val r = MockBattleSimulator.simulate(MockArmy("A", emptyList()), MockArmy("B", listOf(ArmyEntry(footman, 1))))
        assertEquals(MockWinner.RIGHT, r.winner)
    }

    @Test fun oddsSumToRuns() {
        val o = MockBattleSimulator.simulateOdds(
            MockArmy("A", listOf(ArmyEntry(tank, 1))), MockArmy("B", listOf(ArmyEntry(footman, 3))), 10, 7L)
        assertEquals(10, o.leftWins + o.rightWins + o.draws)
    }
}
