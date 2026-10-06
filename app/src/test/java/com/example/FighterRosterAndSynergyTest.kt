package com.example

import com.example.game.Faction
import com.example.game.FighterClass
import com.example.game.FighterPool
import com.example.game.SynergyCalculator
import com.example.game.fighterPool
import com.example.game.instantiateFighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FighterRosterAndSynergyTest {

    @Test
    fun testAll20FightersExistWithCorrectAttributes() {
        assertEquals("There should be exactly 20 fighters in the pool", 20, fighterPool.size)

        val expectedRoster = listOf(
            Triple("Brus Li", FighterClass.KNUCKLE_POWER to Faction.REPTILIANS, 2),
            Triple("Night Stalker", FighterClass.HUNTERS to Faction.EBONS, 3),
            Triple("Iron Apex", FighterClass.ILLUMINS to Faction.P45, 4),
            Triple("Crimson Streak", FighterClass.HUNTERS to Faction.FEET_WORK, 2),
            Triple("Tempest Empress", FighterClass.ILLUMINS to Faction.ELEMOS, 3),
            Triple("Steel Bastion", FighterClass.CONVICT to Faction.TULS, 1),
            Triple("Shadow Viper", FighterClass.HUNTERS to Faction.WIND_WALKER, 2),
            Triple("Mad Grin", FighterClass.CONVICT to Faction.P45, 1),
            Triple("Solar Aegis", FighterClass.ILLUMINS to Faction.TULS, 5),
            Triple("Swamp Titan", FighterClass.KNUCKLE_POWER to Faction.ELEMOS, 3),
            Triple("Brimstone Rider", FighterClass.CONVICT to Faction.EBONS, 4),
            Triple("Astral Sovereign", FighterClass.ILLUMINS to Faction.REPTILIANS, 4),
            Triple("Bio-Parasite", FighterClass.CONVICT to Faction.REPTILIANS, 3),
            Triple("Gale Marksman", FighterClass.HUNTERS to Faction.ELEMOS, 1),
            Triple("Zephyr Claw", FighterClass.KNUCKLE_POWER to Faction.WIND_WALKER, 2),
            Triple("Thunder Mauler", FighterClass.KNUCKLE_POWER to Faction.TULS, 4),
            Triple("Deadshot Cyber", FighterClass.HUNTERS to Faction.P45, 3),
            Triple("Umbral Seer", FighterClass.ILLUMINS to Faction.EBONS, 2),
            Triple("Alley Slugger", FighterClass.KNUCKLE_POWER to Faction.FEET_WORK, 1),
            Triple("Ghost Escapee", FighterClass.CONVICT to Faction.WIND_WALKER, 2)
        )

        for ((name, synergies, cost) in expectedRoster) {
            val (role, faction) = synergies
            val template = fighterPool.find { it.name == name }
            assertNotNull("Fighter $name must exist in fighterPool", template)
            assertEquals("$name should have role $role", role, template!!.cls)
            assertEquals("$name should have faction $faction", faction, template.faction)
            assertEquals("$name should cost $cost gold", cost, template.cost)
        }
    }

    @Test
    fun testRoleSynergiesCalculation() {
        val knuckleFighters = fighterPool
            .filter { it.cls == FighterClass.KNUCKLE_POWER }
            .take(4)
            .map { instantiateFighter(it, true, 4) }

        val active = SynergyCalculator.calculateSynergies(knuckleFighters)
        assertTrue(
            "Should activate Knuckle Power (4)",
            active.any { it.startsWith("Knuckle Power (4)") }
        )
    }

    @Test
    fun testFactionSynergiesCalculation() {
        val reptilianFighters = fighterPool
            .filter { it.faction == Faction.REPTILIANS }
            .take(3)
            .map { instantiateFighter(it, true, 4) }

        val active = SynergyCalculator.calculateSynergies(reptilianFighters)
        assertTrue(
            "Should activate Reptilians (3)",
            active.any { it.startsWith("Reptilians (3)") }
        )
    }

    @Test
    fun testFighterPoolShopGeneration() {
        val pool = FighterPool()
        val shopUnits = pool.rollShop(playerLevel = 5)
        assertEquals(5, shopUnits.size)
        for (unit in shopUnits) {
            assertTrue("Unit should be from the valid cost range 1..5", unit.cost in 1..5)
            assertTrue("Unit name should match one of the 20 roster fighters", fighterPool.any { it.name == unit.name })
        }
    }

    @Test
    fun testStarterCommandersAndUnlockingCatalog() {
        val starters = com.example.game.CommanderUnlockManager.STARTER_COMMANDER_NAMES
        assertEquals("Should have exactly 4 starter commanders", 4, starters.size)
        assertTrue(starters.contains("Brus Li (The Dragon Monk)"))
        assertTrue(starters.contains("Steel Bastion (The Unbreakable Felon)"))
        assertTrue(starters.contains("Gale Marksman (The Precision Fletcher)"))
        assertTrue(starters.contains("Umbral Seer (The Shadow Mystic)"))

        val catalog = com.example.game.CommanderUnlockManager.UNLOCK_CATALOG
        assertEquals("All 20 heroes should be represented in the unlock catalog", 20, catalog.size)

        // Verify locked heroes require credits and cash purchase option
        val nightStalker = catalog["Night Stalker (The Shadow Detective)"]
        assertNotNull(nightStalker)
        assertEquals(false, nightStalker!!.isStarter)
        assertTrue("Unlock credit cost should be positive", nightStalker.unlockCreditCost > 0)
        assertTrue("Demo cash price should be defined", nightStalker.cashPriceCents > 0)
    }

    @Test
    fun testStackedSynergySpecialSkillsResult() {
        val knuckleFighters = fighterPool
            .filter { it.cls == FighterClass.KNUCKLE_POWER }
            .take(4)
            .map { instantiateFighter(it, true, 4) }

        val activeSynergies = SynergyCalculator.calculateSynergies(knuckleFighters)
        val stackedSkills = SynergyCalculator.getStackedSynergySkills(activeSynergies)

        assertTrue(
            "Stacking Knuckle Power should unlock Dragon Wrath Burst special skill",
            stackedSkills.any { it.name == "Dragon Wrath Burst" }
        )
    }
}
