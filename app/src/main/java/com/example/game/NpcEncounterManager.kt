package com.example.game

import java.util.UUID

object NpcEncounterManager {
    
    fun getEncounterForRound(round: Int, fight: Int): Triple<Commander, List<Fighter>, String> {
        return when {
            round == 2 && fight == 3 -> {
                val boss = Commander(
                    name = "Iron Behemoth",
                    title = "Mechanical Terror",
                    abilityDescription = "Siege Protocol: Deals massive AOE damage and stuns.",
                    hp = 400,
                    maxHp = 400,
                    damage = 40
                )
                val minions = (0..3).map { y ->
                    val f = instantiateFighter(
                        FighterTemplate("Iron Husk", Faction.NPC, FighterClass.NPC, 1, 150, 0, 20, 0, 15, 10, 5, 0, 1, 100),
                        false, y
                    )
                    f.copy(name = "Sentry Bot ${y+1}")
                }
                Triple(boss, minions, "Encounter: Iron Behemoth and its Sentry Bots!")
            }
            round == 3 && fight == 3 -> {
                val boss = Commander(
                    name = "Void Archon",
                    title = "Cosmic Nightmare",
                    abilityDescription = "Void Rift: Drains mana and deals magic damage over time.",
                    hp = 500,
                    maxHp = 500,
                    damage = 50
                )
                val minions = (0..4).map { y ->
                    val f = instantiateFighter(
                        FighterTemplate("Void Creeper", Faction.NPC, FighterClass.NPC, 1, 120, 40, 20, 30, 10, 15, 16, 20, 2, 95),
                        false, y
                    )
                    f.copy(name = "Void Spawn ${y+1}")
                }
                Triple(boss, minions, "Encounter: Void Archon and the Void Spawns!")
            }
            else -> {
                // Default generic NPC encounter if called elsewhere
                val boss = Commander(name = "Ancient Guardian", hp = 300, maxHp = 300)
                val minions = (0..2).map { generateNpcMonster(it, round) }
                Triple(boss, minions, "Encounter: Ancient Guardian")
            }
        }
    }

    fun generateLoot(round: Int, fight: Int): Pair<List<Equipment>, List<AbilityEnhancement>> {
        val equipment = mutableListOf<Equipment>()
        val enhancements = mutableListOf<AbilityEnhancement>()

        when {
            round == 2 && fight == 3 -> {
                equipment.add(Equipment(name = "Heavy Plating", type = EquipmentType.DEFENSE))
                enhancements.add(AbilityEnhancement(
                    name = "Reinforced Chassis",
                    description = "+100 HP and +15 Defense",
                    hpBonus = 100,
                    defenseBonus = 15
                ))
            }
            round == 3 && fight == 3 -> {
                equipment.add(Equipment(name = "Void Essence", type = EquipmentType.ATTACK))
                enhancements.add(AbilityEnhancement(
                    name = "Cosmic Focus",
                    description = "+30 Attack and +10% Critical Chance",
                    attackBonus = 30,
                    critBonus = 0.10
                ))
                enhancements.add(AbilityEnhancement(
                    name = "Ethereal Speed",
                    description = "+5 Attack Speed and +10% Dodge",
                    speedBonus = 5,
                    dodgeBonus = 0.10
                ))
            }
        }
        return Pair(equipment, enhancements)
    }
}
