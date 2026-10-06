package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.game.BattleResult
import com.example.game.BattleState
import com.example.game.BattleVisualizationScreen
import com.example.game.Commander
import com.example.game.Faction
import com.example.game.Fighter
import com.example.game.FighterClass
import com.example.game.SpecialAbility
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class BattleVisualizationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBattleVisualizationScreenDisplaysFightersHealthBarsAndLogs() {
        val playerFighter = Fighter(
            id = "player_hero_1",
            name = "Iron Vanguard",
            hp = 120,
            health = 120,
            maxHp = 120,
            mana = 45,
            physAttack = 35,
            fighterClass = FighterClass.KNUCKLE_POWER,
            faction = Faction.TULS,
            isPlayer = true,
            specialAbility = SpecialAbility(name = "Meteor Punch", description = "Explosive punch", damage = 60)
        )

        val enemyFighter = Fighter(
            id = "enemy_boss_1",
            name = "Shadow Assassin",
            hp = 75,
            health = 75,
            maxHp = 100,
            mana = 100,
            physAttack = 40,
            fighterClass = FighterClass.HUNTERS,
            faction = Faction.P45,
            isPlayer = false,
            specialAbility = SpecialAbility(name = "Shadow Strike", description = "Pierces armor", damage = 50)
        )

        val battleState = BattleState(
            round = 2,
            fight = 1,
            turn = 3,
            playerCommander = Commander(name = "Commander Alpha", hp = 100, health = 100),
            opponentCommander = Commander(name = "Commander Omega", hp = 85, health = 85),
            playerFighters = listOf(playerFighter),
            opponentFighters = listOf(enemyFighter),
            isBattleActive = true,
            battleLogs = listOf(
                "✨ Iron Vanguard casts Meteor Punch for 60 DMG!",
                "Shadow Assassin hits Iron Vanguard for 25 dmg!"
            )
        )

        var stepTurnInvoked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                BattleVisualizationScreen(
                    battleState = battleState,
                    onStepTurn = { stepTurnInvoked = true }
                )
            }
        }

        // Verify root screen is displayed
        composeTestRule.onNodeWithTag("battle_visualization_screen").assertIsDisplayed()

        // Verify player and enemy fighters are rendered
        composeTestRule.onNodeWithText("Iron Vanguard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shadow Assassin").assertIsDisplayed()

        // Verify health bars / text for both fighters
        composeTestRule.onNodeWithText("120/120").assertExists()
        composeTestRule.onNodeWithText("75/100").assertExists()

        // Verify recent combat action logs
        composeTestRule.onNodeWithTag("combat_actions_log_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("✨ Iron Vanguard casts Meteor Punch for 60 DMG!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shadow Assassin hits Iron Vanguard for 25 dmg!").assertIsDisplayed()

        // Verify interactive step turn button
        composeTestRule.onNodeWithTag("step_turn_button").performClick()
        assertTrue("Step turn callback should be invoked", stepTurnInvoked)
    }

    @Test
    fun testBattleVisualizationScreenDisplaysVictoryState() {
        val playerFighter = Fighter(
            name = "Champion",
            hp = 100,
            health = 100,
            isPlayer = true
        )

        val battleState = BattleState(
            round = 1,
            fight = 2,
            turn = 5,
            playerFighters = listOf(playerFighter),
            opponentFighters = emptyList(),
            isBattleActive = false,
            battleResult = BattleResult.PLAYER_WIN,
            battleLogs = listOf("Victory! All opponent units defeated.")
        )

        var nextFightInvoked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                BattleVisualizationScreen(
                    battleState = battleState,
                    onNextFight = { nextFightInvoked = true }
                )
            }
        }

        // Verify Victory status
        composeTestRule.onNodeWithText("🏆 VICTORY").assertIsDisplayed()

        // Verify Next Fight button
        composeTestRule.onNodeWithTag("next_fight_button").performClick()
        assertTrue("Next fight callback should be invoked", nextFightInvoked)
    }
}
