package com.example.game

import java.util.Random
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Player-designed fighter template for mock battles. Any number of copies
 * can be fielded per side (e.g. 1 Chief Kong vs 3 Mike Tysons).
 */
data class FighterTemplate(
    val name: String,
    val hp: Int = 100,
    val attack: Int = 15,
    val defense: Int = 5,
    val speed: Int = 10,
    val range: Int = 1,
    val accuracy: Int = 90,
    val critChance: Double = 0.05,
    val dodgeChance: Double = 0.05,
    val isMagic: Boolean = false
) {
    fun toFighter(isPlayer: Boolean, label: String = name): Fighter = Fighter(
        name = label,
        fighterClass = if (isMagic) FighterClass.MAGE else if (range > 1) FighterClass.MARKSMAN else FighterClass.TANK,
        hp = hp.coerceAtLeast(1),
        physAttack = if (isMagic) 0 else attack,
        magAttack = if (isMagic) attack else 0,
        physDefense = defense,
        magDefense = defense,
        attackSpeed = speed,
        attackRange = range.coerceAtLeast(1),
        accuracy = accuracy.coerceIn(0, 100),
        critChance = critChance.coerceIn(0.0, 1.0),
        dodgeChance = dodgeChance.coerceIn(0.0, 0.95),
        isPlayer = isPlayer
    )
}

/** One side of a mock battle: templates with a head count each. Unlimited size. */
data class ArmyEntry(val template: FighterTemplate, val count: Int)

data class MockArmy(val name: String, val entries: List<ArmyEntry>) {
    val totalUnits: Int get() = entries.sumOf { max(0, it.count) }
}

enum class MockWinner { LEFT, RIGHT, DRAW }

data class MockBattleResult(
    val winner: MockWinner,
    val rounds: Int,
    val leftSurvivors: Int,
    val rightSurvivors: Int,
    val log: List<String>
)

/** Aggregated result of many repeated battles, for realistic odds. */
data class MockBattleOdds(val runs: Int, val leftWins: Int, val rightWins: Int, val draws: Int) {
    val leftWinRate: Double get() = if (runs == 0) 0.0 else leftWins.toDouble() / runs
}

/**
 * Self-contained mock battle simulator. Armies of any size are placed on a
 * grid that grows with unit count; fighters act by speed, step toward the
 * nearest enemy and attack in range using [CombatCalculator].
 */
object MockBattleSimulator {
    const val MAX_ROUNDS = 500
    private const val MAX_LOG_LINES = 2000

    private fun deploy(army: MockArmy, isPlayer: Boolean, columns: Int, side: Int): List<Fighter> {
        val result = mutableListOf<Fighter>()
        val multi = army.entries.count { it.count > 0 } > 1
        var index = 0
        for (entry in army.entries) {
            for (n in 1..max(0, entry.count)) {
                val label = if (entry.count > 1) "${entry.template.name} #$n" else entry.template.name
                val f = entry.template.toFighter(isPlayer, label)
                val col = index / columns
                val row = index % columns
                val gx = if (isPlayer) side - 1 - col * 1 else side + 4 + col
                f.x = gx.coerceAtLeast(-1000)
                f.y = row
                f.startX = f.x
                f.startY = f.y
                result.add(f)
                index++
            }
        }
        if (multi) result.sortBy { -it.attackSpeed }
        return result
    }

    fun simulate(
        left: MockArmy,
        right: MockArmy,
        random: Random = Random(),
        recordLog: Boolean = true
    ): MockBattleResult {
        val columns = max(1, ceil(sqrt(max(left.totalUnits, right.totalUnits).toDouble())).toInt())
        val side = 0
        val a = deploy(left, true, columns, side + 100)
        val b = deploy(right, false, columns, side + 100)
        val log = mutableListOf<String>()
        fun say(s: String) { if (recordLog && log.size < MAX_LOG_LINES) log.add(s) }

        if (a.isEmpty() || b.isEmpty()) {
            val w = when {
                a.isEmpty() && b.isEmpty() -> MockWinner.DRAW
                a.isEmpty() -> MockWinner.RIGHT
                else -> MockWinner.LEFT
            }
            return MockBattleResult(w, 0, a.size, b.size, log)
        }
        say("⚔️ ${left.name} (${a.size}) vs ${right.name} (${b.size})")

        var round = 0
        while (round < MAX_ROUNDS && a.any { it.isAlive } && b.any { it.isAlive }) {
            round++
            val order = (a + b).filter { it.isAlive }.sortedByDescending { it.attackSpeed }
            for (unit in order) {
                if (!unit.isAlive) continue
                val enemies = (if (unit.isPlayer) b else a).filter { it.isAlive }
                if (enemies.isEmpty()) break
                val target = enemies.minBy { CombatCalculator.chebyshevDistance(unit, it) }
                if (CombatCalculator.chebyshevDistance(unit, target) <= unit.attackRange) {
                    val r = CombatCalculator.calculateAttack(unit, target, random)
                    if (r.isHit && !r.isDodge) {
                        target.hp -= r.damage
                        target.health = target.hp
                    }
                    say("R$round: ${r.logMessage}" + if (!target.isAlive) " 💀 ${target.name} falls!" else "")
                } else {
                    unit.x += Integer.signum(target.x - unit.x)
                    unit.y += Integer.signum(target.y - unit.y)
                }
            }
        }
        val la = a.count { it.isAlive }
        val lb = b.count { it.isAlive }
        val winner = when {
            la > 0 && lb == 0 -> MockWinner.LEFT
            lb > 0 && la == 0 -> MockWinner.RIGHT
            else -> MockWinner.DRAW
        }
        say(when (winner) {
            MockWinner.LEFT -> "🏆 ${left.name} wins with $la survivors after $round rounds!"
            MockWinner.RIGHT -> "🏆 ${right.name} wins with $lb survivors after $round rounds!"
            MockWinner.DRAW -> "🤝 Draw after $round rounds."
        })
        return MockBattleResult(winner, round, la, lb, log)
    }

    /** Runs many battles to estimate win odds, since single fights are random. */
    fun simulateOdds(left: MockArmy, right: MockArmy, runs: Int = 100, seed: Long? = null): MockBattleOdds {
        val rnd = if (seed != null) Random(seed) else Random()
        var l = 0; var r = 0; var d = 0
        repeat(runs) {
            when (simulate(left, right, rnd, recordLog = false).winner) {
                MockWinner.LEFT -> l++
                MockWinner.RIGHT -> r++
                MockWinner.DRAW -> d++
            }
        }
        return MockBattleOdds(runs, l, r, d)
    }
}

/** Ready-made fun presets. Players can add their own on the mock battle screen. */
val presetTemplates: List<FighterTemplate> = listOf(
    FighterTemplate("Chief Kong", hp = 600, attack = 45, defense = 25, speed = 8, critChance = 0.10, dodgeChance = 0.02),
    FighterTemplate("Mike Tyson", hp = 220, attack = 40, defense = 12, speed = 16, critChance = 0.25, dodgeChance = 0.20),
    FighterTemplate("Footman", hp = 100, attack = 14, defense = 8, speed = 10),
    FighterTemplate("Archer", hp = 70, attack = 16, defense = 3, speed = 12, range = 4, accuracy = 85),
    FighterTemplate("Mage", hp = 60, attack = 25, defense = 2, speed = 9, range = 3, isMagic = true),
    FighterTemplate("Knight", hp = 180, attack = 22, defense = 18, speed = 11)
)
