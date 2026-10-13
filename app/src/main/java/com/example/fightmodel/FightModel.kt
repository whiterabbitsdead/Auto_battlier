package com.example.fightmodel

/**
 * Portable, dependency-free turn-based fight model.
 * Copy this single package into any Kotlin/JVM project. It has no Android or game imports.
 *
 * Concepts:
 *  - [Stat] base stats; [Modifier]s (from equipment or buffs) change them.
 *  - [Skill] special attacks with cost, cooldown, power multiplier, damage type, optional buff/debuff.
 *  - [FightModel.duel] / [FightModel.step] run a deterministic fight (seedable [Random]).
 */
import kotlin.random.Random

enum class Stat { MAX_HP, ATTACK, DEFENSE, SPEED, CRIT_CHANCE, DODGE_CHANCE }

enum class DamageType { PHYSICAL, MAGICAL, TRUE }

/** Flat bonus applied first, then percent bonus (0.10 = +10%). */
data class Modifier(val stat: Stat, val flat: Double = 0.0, val percent: Double = 0.0)

/** A source of modifiers that never expires while worn. */
data class Equipment(val name: String, val modifiers: List<Modifier>)

/** Timed modifiers. [duration] is counted in the owner's turns. */
data class Buff(val name: String, val modifiers: List<Modifier>, val duration: Int)

enum class SkillTarget { ENEMY, SELF }

data class Skill(
    val name: String,
    val power: Double = 1.5,
    val damageType: DamageType = DamageType.PHYSICAL,
    val cooldown: Int = 3,
    val target: SkillTarget = SkillTarget.ENEMY,
    val buff: Buff? = null,
    /** Fraction of damage dealt returned as healing. */
    val lifesteal: Double = 0.0,
    val cannotDodge: Boolean = true
)

class Combatant(
    val name: String,
    val baseStats: Map<Stat, Double>,
    val equipment: List<Equipment> = emptyList(),
    val skills: List<Skill> = emptyList()
) {
    private val buffs = mutableListOf<Buff>()
    private val cooldowns = mutableMapOf<Skill, Int>()
    val activeBuffs: List<Buff> get() = buffs.toList()

    val maxHp: Double get() = stat(Stat.MAX_HP)
    var hp: Double = 0.0
        private set
    val isAlive: Boolean get() = hp > 0.0

    init { hp = maxHp }

    /** Final stat = (base + sum flat) * (1 + sum percent) from equipment and active buffs. */
    fun stat(stat: Stat): Double {
        val mods = equipment.flatMap { it.modifiers } + buffs.flatMap { it.modifiers }
        val relevant = mods.filter { it.stat == stat }
        val value = ((baseStats[stat] ?: 0.0) + relevant.sumOf { it.flat }) * (1.0 + relevant.sumOf { it.percent })
        return when (stat) {
            Stat.CRIT_CHANCE, Stat.DODGE_CHANCE -> value.coerceIn(0.0, 1.0)
            else -> maxOf(0.0, value)
        }
    }

    fun addBuff(buff: Buff) {
        buffs.removeAll { it.name == buff.name } // refresh instead of stacking
        buffs.add(buff)
    }

    fun heal(amount: Double) { hp = minOf(maxHp, hp + amount) }

    fun takeDamage(amount: Double) { hp = maxOf(0.0, hp - amount) }

    fun readySkill(): Skill? = skills.firstOrNull { (cooldowns[it] ?: 0) <= 0 }

    internal fun startCooldown(skill: Skill) { cooldowns[skill] = skill.cooldown }

    /** Ticks cooldowns and buff durations; call once at the end of this combatant's turn. */
    internal fun endTurn() {
        cooldowns.keys.toList().forEach { cooldowns[it] = maxOf(0, (cooldowns[it] ?: 0) - 1) }
        val remaining = buffs.map { it.copy(duration = it.duration - 1) }.filter { it.duration > 0 }
        buffs.clear(); buffs.addAll(remaining)
    }
}

sealed class FightEvent {
    data class Attack(val attacker: String, val target: String, val skill: String?, val damage: Double,
                      val crit: Boolean, val dodged: Boolean) : FightEvent()
    data class BuffApplied(val target: String, val buff: String) : FightEvent()
    data class Defeated(val name: String) : FightEvent()
}

data class FightResult(val winner: Combatant?, val rounds: Int, val events: List<FightEvent>)

class FightModel(private val random: Random = Random.Default, private val critMultiplier: Double = 2.0) {

    /** Skills are used whenever off cooldown, otherwise a basic attack. Returns events produced. */
    fun step(attacker: Combatant, defender: Combatant): List<FightEvent> {
        val events = mutableListOf<FightEvent>()
        val skill = attacker.readySkill()
        if (skill != null) attacker.startCooldown(skill)
        val power = skill?.power ?: 1.0
        val type = skill?.damageType ?: DamageType.PHYSICAL
        val target = if (skill?.target == SkillTarget.SELF) attacker else defender

        if (skill != null && skill.target == SkillTarget.SELF) {
            skill.buff?.let { attacker.addBuff(it); events += FightEvent.BuffApplied(attacker.name, it.name) }
            events += FightEvent.Attack(attacker.name, attacker.name, skill.name, 0.0, false, false)
        } else {
            val dodged = !(skill?.cannotDodge ?: false) && random.nextDouble() < defender.stat(Stat.DODGE_CHANCE)
            var damage = 0.0
            var crit = false
            if (!dodged) {
                crit = random.nextDouble() < attacker.stat(Stat.CRIT_CHANCE)
                val raw = attacker.stat(Stat.ATTACK) * power * (if (crit) critMultiplier else 1.0)
                damage = if (type == DamageType.TRUE) raw else maxOf(1.0, raw - defender.stat(Stat.DEFENSE))
                defender.takeDamage(damage)
                if (skill != null && skill.lifesteal > 0) attacker.heal(damage * skill.lifesteal)
                skill?.buff?.let { defender.addBuff(it); events += FightEvent.BuffApplied(defender.name, it.name) }
            }
            events += FightEvent.Attack(attacker.name, target.name, skill?.name, damage, crit, dodged)
        }
        if (!defender.isAlive) events += FightEvent.Defeated(defender.name)
        attacker.endTurn()
        return events
    }

    /** Runs a duel; the faster combatant (by final SPEED) acts first each round. */
    fun duel(a: Combatant, b: Combatant, maxRounds: Int = 100): FightResult {
        val events = mutableListOf<FightEvent>()
        val (first, second) = if (a.stat(Stat.SPEED) >= b.stat(Stat.SPEED)) a to b else b to a
        var round = 0
        while (round < maxRounds && a.isAlive && b.isAlive) {
            round++
            events += step(first, second)
            if (second.isAlive) events += step(second, first)
        }
        val winner = when {
            a.isAlive && !b.isAlive -> a
            b.isAlive && !a.isAlive -> b
            else -> null
        }
        return FightResult(winner, round, events)
    }
}
