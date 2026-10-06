package com.example.game

object SynergyCalculator {
    // Calculates which synergies are active based on the unique units on the board.
    // Returns a list of string descriptions for active synergies.
    fun calculateSynergies(boardFighters: List<Fighter>): List<String> {
        val uniqueFighters = boardFighters.distinctBy { it.name }
        
        val factionCounts = uniqueFighters.groupingBy { it.faction }.eachCount()
        val classCounts = uniqueFighters.groupingBy { it.fighterClass }.eachCount()
        
        val activeSynergies = mutableListOf<String>()
        
        // --- Role Synergies ---
        val knuckleCount = classCounts.getOrDefault(FighterClass.KNUCKLE_POWER, 0)
        if (knuckleCount >= 4) activeSynergies.add("Knuckle Power (4): +35 Atk, +40% AtkSpd, +150 HP")
        else if (knuckleCount >= 2) activeSynergies.add("Knuckle Power (2): +15 Atk, +20% AtkSpd")

        val huntersCount = classCounts.getOrDefault(FighterClass.HUNTERS, 0)
        if (huntersCount >= 4) activeSynergies.add("Hunters (4): +2 Rng, +30% Crit, +25 Atk")
        else if (huntersCount >= 2) activeSynergies.add("Hunters (2): +1 Rng, +15% Crit")

        val illuminsCount = classCounts.getOrDefault(FighterClass.ILLUMINS, 0)
        if (illuminsCount >= 4) activeSynergies.add("Illumins (4): +60 Mag Atk, +30 Mag Def, +100 HP")
        else if (illuminsCount >= 2) activeSynergies.add("Illumins (2): +25 Mag Atk, +15 Mag Def")

        val convictCount = classCounts.getOrDefault(FighterClass.CONVICT, 0)
        if (convictCount >= 4) activeSynergies.add("Convict (4): +35 Phys Def, +30% Dodge, +200 HP")
        else if (convictCount >= 2) activeSynergies.add("Convict (2): +15 Phys Def, +15% Dodge")

        // --- Faction Synergies ---
        val reptilianCount = factionCounts.getOrDefault(Faction.REPTILIANS, 0)
        if (reptilianCount >= 3) activeSynergies.add("Reptilians (3): +350 Max HP, +25 Def")
        else if (reptilianCount >= 2) activeSynergies.add("Reptilians (2): +150 Max HP, +10 Def")

        val ebonsCount = factionCounts.getOrDefault(Faction.EBONS, 0)
        if (ebonsCount >= 3) activeSynergies.add("Ebons (3): +40% Crit Chance, +20 Acc")
        else if (ebonsCount >= 2) activeSynergies.add("Ebons (2): +20% Crit Chance, +10 Acc")

        val p45Count = factionCounts.getOrDefault(Faction.P45, 0)
        if (p45Count >= 3) activeSynergies.add("P45 (3): +45 Phys Atk, +25 Mag Atk")
        else if (p45Count >= 2) activeSynergies.add("P45 (2): +20 Phys Atk, +10 Mag Atk")

        val feetWorkCount = factionCounts.getOrDefault(Faction.FEET_WORK, 0)
        if (feetWorkCount >= 2) activeSynergies.add("Feet Work (2): +25% Dodge, +15 AtkSpd")

        val elemosCount = factionCounts.getOrDefault(Faction.ELEMOS, 0)
        if (elemosCount >= 3) activeSynergies.add("Elemos (3): +55 Mag Atk, +25 AtkSpd")
        else if (elemosCount >= 2) activeSynergies.add("Elemos (2): +25 Mag Atk, +10 AtkSpd")

        val tulsCount = factionCounts.getOrDefault(Faction.TULS, 0)
        if (tulsCount >= 3) activeSynergies.add("Tuls (3): +50 Phys Def, +35 Mag Def")
        else if (tulsCount >= 2) activeSynergies.add("Tuls (2): +20 Phys Def, +15 Mag Def")

        val windWalkerCount = factionCounts.getOrDefault(Faction.WIND_WALKER, 0)
        if (windWalkerCount >= 3) activeSynergies.add("Wind Walker (3): +35% Dodge, +2 Rng")
        else if (windWalkerCount >= 2) activeSynergies.add("Wind Walker (2): +20% Dodge, +1 Rng")

        // Legacy Synergies
        if (factionCounts.getOrDefault(Faction.DARK, 0) >= 2) activeSynergies.add("Dark (2): +10% Crit Chance")
        if (factionCounts.getOrDefault(Faction.LIGHT, 0) >= 2) activeSynergies.add("Light (2): +15 Phys Def")
        if (factionCounts.getOrDefault(Faction.NATURE, 0) >= 2) activeSynergies.add("Nature (2): +100 Max HP")
        if (factionCounts.getOrDefault(Faction.ELEMENTAL, 0) >= 2) activeSynergies.add("Elemental (2): +15 Mag Atk")
        if (factionCounts.getOrDefault(Faction.ORC, 0) >= 1) activeSynergies.add("Orc (1): +10 Phys Atk")
        
        if (classCounts.getOrDefault(FighterClass.TANK, 0) >= 2) activeSynergies.add("Tank (2): +20 Max HP, +5 Def")
        if (classCounts.getOrDefault(FighterClass.ASSASSIN, 0) >= 2) activeSynergies.add("Assassin (2): +15% Dodge")
        if (classCounts.getOrDefault(FighterClass.MARKSMAN, 0) >= 2) activeSynergies.add("Marksman (2): +1 Rng, +10 Acc")
        if (classCounts.getOrDefault(FighterClass.MAGE, 0) >= 2) activeSynergies.add("Mage (2): +20 Mag Atk")
        
        return activeSynergies
    }

    // Applies the calculated synergies to the units. Should be called right before battle begins.
    fun applySynergyBuffs(fighters: List<Fighter>): List<Fighter> {
        val activeSynergies = calculateSynergies(fighters)
        return fighters.map { f ->
            var newCrit = f.critChance
            var newPDef = f.physDefense
            var newMaxHp = f.maxHp
            var newMAtk = f.magAttack
            var newPAtk = f.physAttack
            var newDodge = f.dodgeChance
            var newRng = f.attackRange
            var newAcc = f.accuracy
            var newMDef = f.magDefense
            var newSpd = f.attackSpeed

            // --- Role Buffs ---
            if (f.fighterClass == FighterClass.KNUCKLE_POWER) {
                if (activeSynergies.any { it.startsWith("Knuckle Power (4)") }) {
                    newPAtk += 35; newSpd += 4; newMaxHp += 150
                } else if (activeSynergies.any { it.startsWith("Knuckle Power (2)") }) {
                    newPAtk += 15; newSpd += 2
                }
            }

            if (f.fighterClass == FighterClass.HUNTERS) {
                if (activeSynergies.any { it.startsWith("Hunters (4)") }) {
                    newRng += 2; newCrit += 0.30; newPAtk += 25
                } else if (activeSynergies.any { it.startsWith("Hunters (2)") }) {
                    newRng += 1; newCrit += 0.15
                }
            }

            if (f.fighterClass == FighterClass.ILLUMINS) {
                if (activeSynergies.any { it.startsWith("Illumins (4)") }) {
                    newMAtk += 60; newMDef += 30; newMaxHp += 100
                } else if (activeSynergies.any { it.startsWith("Illumins (2)") }) {
                    newMAtk += 25; newMDef += 15
                }
            }

            if (f.fighterClass == FighterClass.CONVICT) {
                if (activeSynergies.any { it.startsWith("Convict (4)") }) {
                    newPDef += 35; newDodge += 0.30; newMaxHp += 200
                } else if (activeSynergies.any { it.startsWith("Convict (2)") }) {
                    newPDef += 15; newDodge += 0.15
                }
            }

            // --- Faction Buffs ---
            if (f.faction == Faction.REPTILIANS) {
                if (activeSynergies.any { it.startsWith("Reptilians (3)") }) {
                    newMaxHp += 350; newPDef += 25
                } else if (activeSynergies.any { it.startsWith("Reptilians (2)") }) {
                    newMaxHp += 150; newPDef += 10
                }
            }

            if (f.faction == Faction.EBONS) {
                if (activeSynergies.any { it.startsWith("Ebons (3)") }) {
                    newCrit += 0.40; newAcc += 20
                } else if (activeSynergies.any { it.startsWith("Ebons (2)") }) {
                    newCrit += 0.20; newAcc += 10
                }
            }

            if (f.faction == Faction.P45) {
                if (activeSynergies.any { it.startsWith("P45 (3)") }) {
                    newPAtk += 45; newMAtk += 25
                } else if (activeSynergies.any { it.startsWith("P45 (2)") }) {
                    newPAtk += 20; newMAtk += 10
                }
            }

            if (f.faction == Faction.FEET_WORK) {
                if (activeSynergies.any { it.startsWith("Feet Work (2)") }) {
                    newDodge += 0.25; newSpd += 3
                }
            }

            if (f.faction == Faction.ELEMOS) {
                if (activeSynergies.any { it.startsWith("Elemos (3)") }) {
                    newMAtk += 55; newSpd += 3
                } else if (activeSynergies.any { it.startsWith("Elemos (2)") }) {
                    newMAtk += 25; newSpd += 1
                }
            }

            if (f.faction == Faction.TULS) {
                if (activeSynergies.any { it.startsWith("Tuls (3)") }) {
                    newPDef += 50; newMDef += 35
                } else if (activeSynergies.any { it.startsWith("Tuls (2)") }) {
                    newPDef += 20; newMDef += 15
                }
            }

            if (f.faction == Faction.WIND_WALKER) {
                if (activeSynergies.any { it.startsWith("Wind Walker (3)") }) {
                    newDodge += 0.35; newRng += 2
                } else if (activeSynergies.any { it.startsWith("Wind Walker (2)") }) {
                    newDodge += 0.20; newRng += 1
                }
            }

            // Legacy Buffs
            if (activeSynergies.contains("Dark (2): +10% Crit Chance") && f.faction == Faction.DARK) newCrit += 0.10
            if (activeSynergies.contains("Light (2): +15 Phys Def") && f.faction == Faction.LIGHT) newPDef += 15
            if (activeSynergies.contains("Nature (2): +100 Max HP") && f.faction == Faction.NATURE) newMaxHp += 100
            if (activeSynergies.contains("Elemental (2): +15 Mag Atk") && f.faction == Faction.ELEMENTAL) newMAtk += 15
            if (activeSynergies.contains("Orc (1): +10 Phys Atk") && f.faction == Faction.ORC) newPAtk += 10

            if (activeSynergies.contains("Tank (2): +20 Max HP, +5 Def") && f.fighterClass == FighterClass.TANK) {
                newMaxHp += 20; newPDef += 5; newMDef += 5
            }
            if (activeSynergies.contains("Assassin (2): +15% Dodge") && f.fighterClass == FighterClass.ASSASSIN) newDodge += 0.15
            if (activeSynergies.contains("Marksman (2): +1 Rng, +10 Acc") && f.fighterClass == FighterClass.MARKSMAN) {
                newRng += 1; newAcc += 10
            }
            if (activeSynergies.contains("Mage (2): +20 Mag Atk") && f.fighterClass == FighterClass.MAGE) newMAtk += 20

            // If maxHp was boosted, heal the current hp by the same flat amount just for safety
            val hpDiff = newMaxHp - f.maxHp
            
            f.copy(
                critChance = newCrit,
                physDefense = newPDef,
                maxHp = newMaxHp,
                hp = f.hp + hpDiff,
                magAttack = newMAtk,
                physAttack = newPAtk,
                dodgeChance = newDodge,
                attackRange = newRng,
                accuracy = newAcc,
                magDefense = newMDef,
                attackSpeed = newSpd
            )
        }
    }

    /**
     * Determines the Special Skills unlocked when synergies are stacked.
     */
    fun getStackedSynergySkills(activeSynergies: List<String>): List<StackedSynergySkill> {
        val skills = mutableListOf<StackedSynergySkill>()

        // Role Stacked Skills
        if (activeSynergies.any { it.startsWith("Knuckle Power (4)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "kp_4",
                    name = "Dragon Wrath Burst",
                    synergyTag = "Knuckle Power (4)",
                    icon = "🐉",
                    description = "Attacks detonate on contact dealing 110 AOE physical damage to adjacent targets with +40% attack speed!",
                    triggerDescription = "On Basic Attack"
                )
            )
        } else if (activeSynergies.any { it.startsWith("Knuckle Power (2)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "kp_2",
                    name = "Shockwave Strike",
                    synergyTag = "Knuckle Power (2)",
                    icon = "💥",
                    description = "Every 3rd punch unleashes a sonic shockwave dealing +35 bonus physical damage.",
                    triggerDescription = "Every 3rd Attack"
                )
            )
        }

        if (activeSynergies.any { it.startsWith("Hunters (4)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "hunt_4",
                    name = "Phantom Sniper Barrage",
                    synergyTag = "Hunters (4)",
                    icon = "🏹",
                    description = "Every 4 seconds, fires an armor-piercing long-range shot dealing 140 True Damage to the furthest enemies!",
                    triggerDescription = "Every 4s in Combat"
                )
            )
        } else if (activeSynergies.any { it.startsWith("Hunters (2)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "hunt_2",
                    name = "Hunter's Mark",
                    synergyTag = "Hunters (2)",
                    icon = "🎯",
                    description = "Attacks mark enemies with Vulnerability (+25% extra damage taken) and gain +15% Crit chance.",
                    triggerDescription = "On Attack"
                )
            )
        }

        if (activeSynergies.any { it.startsWith("Illumins (4)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "ill_4",
                    name = "Supernova Cataclysm",
                    synergyTag = "Illumins (4)",
                    icon = "✨",
                    description = "Casting an ultimate or dropping below 40% HP detonates a nova dealing 180 Magic Damage to all enemies and granting a 120 HP magic barrier!",
                    triggerDescription = "On Ultimate / Low HP"
                )
            )
        } else if (activeSynergies.any { it.startsWith("Illumins (2)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "ill_2",
                    name = "Radiant Surge",
                    synergyTag = "Illumins (2)",
                    icon = "🌟",
                    description = "Basic attacks restore 15 bonus Mana and grant +25 Magic Attack.",
                    triggerDescription = "On Attack"
                )
            )
        }

        if (activeSynergies.any { it.startsWith("Convict (4)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "conv_4",
                    name = "Riot Breakout Rampage",
                    synergyTag = "Convict (4)",
                    icon = "⛓️",
                    description = "Gain CC immunity, +30% Dodge, and every 4th attack inflicts a 1.5s Stun on target!",
                    triggerDescription = "Passive & Every 4th Hit"
                )
            )
        } else if (activeSynergies.any { it.startsWith("Convict (2)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "conv_2",
                    name = "Prison Hardening",
                    synergyTag = "Convict (2)",
                    icon = "🛡️",
                    description = "Taking damage below 50% HP grants an instant 120 HP shield and +15 Armor for 4s.",
                    triggerDescription = "When HP < 50%"
                )
            )
        }

        // Faction Stacked Skills
        if (activeSynergies.any { it.startsWith("Reptilians (3)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "rep_3",
                    name = "Acidic Toxic Spores",
                    synergyTag = "Reptilians (3)",
                    icon = "🧪",
                    description = "Attacks spray toxic venom dealing 35 True Damage per tick for 3 seconds and shredding 25% defense.",
                    triggerDescription = "On Attack"
                )
            )
        } else if (activeSynergies.any { it.startsWith("Reptilians (2)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "rep_2",
                    name = "Cold-Blood Regeneration",
                    synergyTag = "Reptilians (2)",
                    icon = "🦎",
                    description = "Reptilians regenerate 25 HP each combat tick.",
                    triggerDescription = "Every Combat Tick"
                )
            )
        }

        if (activeSynergies.any { it.startsWith("Ebons (3)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "eb_3",
                    name = "Eclipse Ambush",
                    synergyTag = "Ebons (3)",
                    icon = "👥",
                    description = "Teleports behind target with +100% Critical Damage and blinds target for 2s.",
                    triggerDescription = "Combat Start"
                )
            )
        } else if (activeSynergies.any { it.startsWith("Ebons (2)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "eb_2",
                    name = "Shadow Cloak",
                    synergyTag = "Ebons (2)",
                    icon = "🌑",
                    description = "+20% Crit chance; first attack deals guaranteed critical strike.",
                    triggerDescription = "First Attack"
                )
            )
        }

        if (activeSynergies.any { it.startsWith("P45 (3)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "p45_3",
                    name = "Orbital Railgun Salvo",
                    synergyTag = "P45 (3)",
                    icon = "🚀",
                    description = "Every 5 seconds, an orbital railgun salvo rains down dealing 130 True Damage to random enemies!",
                    triggerDescription = "Every 5s in Combat"
                )
            )
        } else if (activeSynergies.any { it.startsWith("P45 (2)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "p45_2",
                    name = "Overclock Matrix",
                    synergyTag = "P45 (2)",
                    icon = "⚙️",
                    description = "Overclocks combat processors granting +20 Physical and +10 Magic Attack.",
                    triggerDescription = "Passive Aura"
                )
            )
        }

        if (activeSynergies.any { it.startsWith("Feet Work (2)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "feet_2",
                    name = "Mach Counter-Strike",
                    synergyTag = "Feet Work (2)",
                    icon = "⚡",
                    description = "+25% Dodge; successfully dodging an attack instantly retaliates with a 65 Physical Damage kick!",
                    triggerDescription = "On Dodge"
                )
            )
        }

        if (activeSynergies.any { it.startsWith("Elemos (3)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "elem_3",
                    name = "Tempest Vortex",
                    synergyTag = "Elemos (3)",
                    icon = "🌪️",
                    description = "Discharges a lightning vortex bouncing across enemies for 120 Magic Damage and slows enemy attack speed by 35%!",
                    triggerDescription = "On Attack"
                )
            )
        } else if (activeSynergies.any { it.startsWith("Elemos (2)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "elem_2",
                    name = "Static Current",
                    synergyTag = "Elemos (2)",
                    icon = "⚡",
                    description = "Attacks bounce a static spark to a secondary enemy for 30 Magic Damage.",
                    triggerDescription = "On Attack"
                )
            )
        }

        if (activeSynergies.any { it.startsWith("Tuls (3)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "tuls_3",
                    name = "Spiked Bastion Retribution",
                    synergyTag = "Tuls (3)",
                    icon = "🛡️",
                    description = "Reflects 30% of all incoming damage back to the attacker as physical thorns damage!",
                    triggerDescription = "When Attacked"
                )
            )
        } else if (activeSynergies.any { it.startsWith("Tuls (2)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "tuls_2",
                    name = "Iron Fortress",
                    synergyTag = "Tuls (2)",
                    icon = "🏰",
                    description = "Fortifies defenses with +20 Physical Defense and +15 Magic Defense.",
                    triggerDescription = "Passive Defense"
                )
            )
        }

        if (activeSynergies.any { it.startsWith("Wind Walker (3)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "ww_3",
                    name = "Cyclone Twister",
                    synergyTag = "Wind Walker (3)",
                    icon = "🌪️",
                    description = "Every 4 seconds, summons a localized whirlwind lifting and disabling an enemy for 1.5s.",
                    triggerDescription = "Every 4s in Combat"
                )
            )
        } else if (activeSynergies.any { it.startsWith("Wind Walker (2)") }) {
            skills.add(
                StackedSynergySkill(
                    id = "ww_2",
                    name = "Gale Stride",
                    synergyTag = "Wind Walker (2)",
                    icon = "💨",
                    description = "+20% Dodge and +1 Attack Range.",
                    triggerDescription = "Passive Movement"
                )
            )
        }

        return skills
    }
}
