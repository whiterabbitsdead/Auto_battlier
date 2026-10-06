package com.example.game

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.inset
import com.example.R

enum class FighterFilterTab {
    ALL,
    PLAYER,
    OPPONENT
}

/**
 * Connected ViewModel entry point for the Battle Visualization screen.
 */
@Composable
fun BattleVisualizationScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val battleState = state.battleState

    BattleVisualizationScreen(
        battleState = battleState,
        isAdmin = state.isAdmin,
        onStepTurn = { viewModel.stepBattleTurn() },
        onNextFight = { viewModel.nextFight() },
        onBack = { viewModel.navigateToBattleBoard() },
        onActivateSkill = { viewModel.activateCommanderSkill(it) },
        onAddDemoEnergy = if (state.isAdmin) { { viewModel.addDemoCommanderEnergy(50) } } else null,
        modifier = modifier
    )
}

/**
 * Pure state Composable that visualizes the current state of a battle:
 * displays player and enemy fighters, their health/mana bars, combat stats,
 * and a formatted log of recent combat actions.
 */
@Composable
fun BattleVisualizationScreen(
    battleState: BattleState,
    isAdmin: Boolean = false,
    onStepTurn: (() -> Unit)? = null,
    onNextFight: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onActivateSkill: ((CommanderTacticalSkillType) -> Unit)? = null,
    onAddDemoEnergy: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFighterTab by remember { mutableStateOf(FighterFilterTab.ALL) }
    var selectedLogFilter by remember { mutableStateOf(LogFilterType.ALL) }
    var showTacticalGrid by remember { mutableStateOf(false) }
    var showCombatHealthScreen by remember { mutableStateOf(false) }

    if (showCombatHealthScreen) {
        val teamA = remember(battleState.playerFighters) {
            val fighters = battleState.playerFighters.filter { it.startY >= 0 }
            if (fighters.isNotEmpty()) {
                fighters.map { CombatEngine.run { it.toCombatant(CombatEngine.Team.TEAM_A) } }
            } else {
                null
            }
        }
        val teamB = remember(battleState.opponentFighters) {
            val fighters = battleState.opponentFighters
            if (fighters.isNotEmpty()) {
                fighters.map { CombatEngine.run { it.toCombatant(CombatEngine.Team.TEAM_B) } }
            } else {
                null
            }
        }
        CombatHealthStatusScreen(
            initialTeamA = teamA,
            initialTeamB = teamB,
            onBack = { showCombatHealthScreen = false },
            modifier = modifier
        )
        return
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF1E293B)  // Slate 800
        )
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("battle_visualization_screen"),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // 1. Header & Navigation Bar
            BattleHeaderSection(
                battleState = battleState,
                onBack = onBack,
                onStepTurn = onStepTurn,
                onNextFight = onNextFight
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Commander Matchup & Team Army Health Meter
            ArmyHealthOverviewCard(battleState = battleState)

            // 2.1 Commander Tactical Decision Console (Active skill activation with resource bar)
            battleState.playerCommander?.let { cmd ->
                Spacer(modifier = Modifier.height(6.dp))
                CommanderTacticalConsole(
                    commander = cmd,
                    energy = battleState.playerCommanderEnergy,
                    isBattleActive = battleState.isBattleActive,
                    onActivateSkill = { onActivateSkill?.invoke(it) },
                    onAddDemoEnergy = onAddDemoEnergy,
                    lastSkillCast = battleState.lastTacticalSkillCast
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Tab Filter & Tactical Grid Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedFighterTab == FighterFilterTab.ALL,
                        onClick = { selectedFighterTab = FighterFilterTab.ALL },
                        label = { Text("All (${battleState.playerFighters.size + battleState.opponentFighters.size})", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_all_fighters"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = selectedFighterTab == FighterFilterTab.PLAYER,
                        onClick = { selectedFighterTab = FighterFilterTab.PLAYER },
                        label = { Text("Player (${battleState.livingPlayerFighters.size})", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_player_fighters"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF065F46),
                            selectedLabelColor = Color(0xFFD1FAE5)
                        )
                    )
                    FilterChip(
                        selected = selectedFighterTab == FighterFilterTab.OPPONENT,
                        onClick = { selectedFighterTab = FighterFilterTab.OPPONENT },
                        label = { Text("Enemy (${battleState.livingOpponentFighters.size})", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_enemy_fighters"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF991B1B),
                            selectedLabelColor = Color(0xFFFEE2E2)
                        )
                    )
                }

                if (isAdmin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { showCombatHealthScreen = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("open_combat_health_screen")
                        ) {
                            Text("Health View 🩺", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { showTacticalGrid = !showTacticalGrid },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("toggle_tactical_grid")
                        ) {
                            Text(if (showTacticalGrid) "Hide Grid" else "Grid 🗺️", fontSize = 10.sp)
                        }
                    }
                }
            }

            // Optional: Tactical Grid Formation View
            if (showTacticalGrid) {
                Spacer(modifier = Modifier.height(6.dp))
                TacticalFormationGrid(
                    playerFighters = battleState.playerFighters,
                    opponentFighters = battleState.opponentFighters
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Fighters List (Player & Enemy)
            val displayedFighters = when (selectedFighterTab) {
                FighterFilterTab.ALL -> (battleState.playerFighters + battleState.opponentFighters)
                    .sortedWith(compareByDescending<Fighter> { it.isAlive }.thenByDescending { it.isPlayer })
                FighterFilterTab.PLAYER -> battleState.playerFighters
                FighterFilterTab.OPPONENT -> battleState.opponentFighters
            }

            Text(
                text = "COMBATANTS IN BATTLE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fighters_horizontal_list"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                if (displayedFighters.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text(
                                text = "No fighters deployed in this category.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(displayedFighters, key = { it.id }) { fighter ->
                        FighterCombatCard(fighter = fighter)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Commander Win/Loss History Visual Summary (Recharts-style interactive analytics)
            CommanderWinLossAnalyticsCard(roundHistory = battleState.roundHistory)

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Recent Combat Actions Log (Gated for Admin)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isAdmin) {
                    CombatLogSection(
                        logs = battleState.battleLogs,
                        selectedFilter = selectedLogFilter,
                        onFilterSelected = { selectedLogFilter = it },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Public Graphics/Cinematic View
                    Box(modifier = Modifier.fillMaxSize()) {
                        CinematicBattleView(
                            playerFighters = battleState.playerFighters,
                            opponentFighters = battleState.opponentFighters,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Real-time Combat Log Overlay
                        CombatLogOverlay(
                            logs = battleState.battleLogs.take(5),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .fillMaxWidth(0.6f)
                        )

                        // Tactical Skill Activation Banner Overlay
                        battleState.lastTacticalSkillCast?.let { castName ->
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 10.dp),
                                color = Color(0xFFB45309).copy(alpha = 0.9f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFD700))
                            ) {
                                Text(
                                    text = "👑 TACTICAL ACTIVATION: $castName",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD700),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-style Visual Summary Chart component showing Win/Loss history per Commander after combat rounds.
 */
@Composable
fun CommanderWinLossAnalyticsCard(
    roundHistory: List<CommanderRoundResult>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("commander_win_loss_analytics_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 COMMANDER WIN/LOSS HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = "${roundHistory.size} Records",
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (roundHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Complete combat rounds to track Commander Win/Loss analytics.",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                // Group results by Commander to compute win rate and total bouts
                val groupedByCommander = remember(roundHistory) {
                    roundHistory.groupBy { it.commanderName }
                }

                // Summary Bar Breakdown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groupedByCommander.forEach { (commanderName, results) ->
                        val wins = results.count { it.isWin }
                        val losses = results.size - wins
                        val winPct = if (results.isNotEmpty()) (wins.toFloat() / results.size) else 0f
                        val isPlayer = results.firstOrNull()?.isPlayer == true

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = (if (isPlayer) "👑 " else "🤖 ") + commanderName.take(18),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlayer) Color(0xFFFFD54F) else Color(0xFFE2E8F0)
                                )
                                Text(
                                    text = "$wins W - $losses L (${(winPct * 100).toInt()}%)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (winPct >= 0.5f) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Stacked Win/Loss Horizontal Chart Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF334155))
                            ) {
                                if (wins > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(wins.toFloat())
                                            .fillMaxHeight()
                                            .background(Color(0xFF10B981))
                                    )
                                }
                                if (losses > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(losses.toFloat())
                                            .fillMaxHeight()
                                            .background(Color(0xFFEF4444))
                                    )
                                }
                            }

                            // Round Timeline Dots
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Rounds: ", fontSize = 8.sp, color = Color(0xFF64748B))
                                results.takeLast(10).forEach { roundRes ->
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(if (roundRes.isWin) Color(0xFF10B981) else Color(0xFFEF4444)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (roundRes.isWin) "W" else "L",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top header containing battle progression (Round, Fight, Turn) and quick actions.
 */
@Composable
private fun BattleHeaderSection(
    battleState: BattleState,
    onBack: (() -> Unit)?,
    onStepTurn: (() -> Unit)?,
    onNextFight: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            if (onBack != null) {
                OutlinedButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("back_to_board_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                ) {
                    Text("← Board", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Center Match Status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ROUND ${battleState.round} • FIGHT ${battleState.fight}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )

                val (statusText, statusColor) = when {
                    battleState.battleResult == BattleResult.PLAYER_WIN -> "🏆 VICTORY" to Color(0xFF10B981)
                    battleState.battleResult == BattleResult.AI_WIN -> "💀 DEFEAT" to Color(0xFFEF4444)
                    battleState.battleResult == BattleResult.DRAW -> "⚖️ DRAW" to Color(0xFFF59E0B)
                    battleState.isBattleActive -> "⚔️ TURN ${battleState.turn}" to Color(0xFFFBBF24)
                    else -> "WAITING" to Color(0xFF94A3B8)
                }

                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
            }

            // Action Button (Step Turn or Next Fight)
            if (battleState.battleResult != null && onNextFight != null) {
                Button(
                    onClick = onNextFight,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("next_fight_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Next Fight ⏩", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else if (onStepTurn != null) {
                Button(
                    onClick = onStepTurn,
                    enabled = battleState.isBattleActive || battleState.battleResult == null,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("step_turn_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("Step ⚔️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

/**
 * Overview comparing Commander HP and aggregate Army Health with visual gauge.
 */
@Composable
private fun ArmyHealthOverviewCard(battleState: BattleState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player Commander Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val pImg = battleState.playerCommander?.imageRes ?: 0
                    if (pImg != 0) {
                        Image(
                            painter = painterResource(id = pImg),
                            contentDescription = "Player Commander",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF065F46), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛡️", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = battleState.playerCommander?.name?.substringBefore(" (") ?: "Player",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Commander HP: ${battleState.playerCommander?.health ?: 100}",
                            fontSize = 9.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "VS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Opponent Commander Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = battleState.opponentCommander?.name?.substringBefore(" (") ?: "Opponent",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Commander HP: ${battleState.opponentCommander?.health ?: 100}",
                            fontSize = 9.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val aImg = battleState.opponentCommander?.imageRes ?: 0
                    if (aImg != 0) {
                        Image(
                            painter = painterResource(id = aImg),
                            contentDescription = "Opponent Commander",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF7F1D1D), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👹", fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Total Army Health Bar Indicator
            val playerTotalHp = battleState.totalPlayerHealth
            val opponentTotalHp = battleState.totalOpponentHealth
            val sumHp = (playerTotalHp + opponentTotalHp).coerceAtLeast(1)
            val playerHpRatio = playerTotalHp.toFloat() / sumHp

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Army HP: $playerTotalHp (${battleState.livingPlayerFighters.size} living)",
                    fontSize = 10.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Army HP: $opponentTotalHp (${battleState.livingOpponentFighters.size} living)",
                    fontSize = 10.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Dual Army Health Tug-of-War Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF334155))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(playerHpRatio.coerceAtLeast(0.01f))
                            .background(Color(0xFF10B981))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight((1f - playerHpRatio).coerceAtLeast(0.01f))
                            .background(Color(0xFFEF4444))
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Commander Tactical Energy Gauges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "⚡ Tactical Energy: ${battleState.playerCommanderEnergy}/100",
                    fontSize = 9.sp,
                    color = if (battleState.playerCommanderEnergy >= 100) Color(0xFFFFD700) else Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "⚡ Opponent Energy: ${battleState.aiCommanderEnergy}/100",
                    fontSize = 9.sp,
                    color = if (battleState.aiCommanderEnergy >= 100) Color(0xFFEF4444) else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Tactical Formation Minimap showing unit positions on the 8x8 battlefield grid.
 */
@Composable
private fun TacticalFormationGrid(
    playerFighters: List<Fighter>,
    opponentFighters: List<Fighter>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFFEF4444))))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TACTICAL ARENA FORMATION (8x8)",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.2f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val cellWidth = maxWidth / 8
                    val cellHeight = maxHeight / 8

                    // Center dividing scrimmage line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .align(Alignment.Center)
                            .background(Color(0xFF475569))
                    )

                    // Draw living player units
                    playerFighters.filter { it.isAlive && it.startY >= 0 }.forEach { fighter ->
                        Box(
                            modifier = Modifier
                                .offset(x = cellWidth * fighter.x, y = cellHeight * fighter.y)
                                .size(cellWidth, cellHeight)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fighter.name.take(2),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Draw living enemy units
                    opponentFighters.filter { it.isAlive }.forEach { fighter ->
                        Box(
                            modifier = Modifier
                                .offset(x = cellWidth * fighter.x, y = cellHeight * fighter.y)
                                .size(cellWidth, cellHeight)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fighter.name.take(2),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed card for an individual fighter showing unit badge, health bar, mana bar,
 * stats, and special abilities.
 */
@Composable
fun FighterCombatCard(
    fighter: Fighter,
    modifier: Modifier = Modifier
) {
    val isPlayer = fighter.isPlayer
    val isAlive = fighter.isAlive

    val borderColor = when {
        !isAlive -> Color(0xFF475569)
        isPlayer -> Color(0xFF10B981)
        else -> Color(0xFFEF4444)
    }

    val cardBg = when {
        !isAlive -> Color(0xFF1E293B).copy(alpha = 0.6f)
        isPlayer -> Color(0xFF064E3B).copy(alpha = 0.35f)
        else -> Color(0xFF7F1D1D).copy(alpha = 0.35f)
    }

    Card(
        modifier = modifier
            .width(175.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .testTag(if (isPlayer) "player_fighter_card_${fighter.id}" else "enemy_fighter_card_${fighter.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Top Row: Unit portrait, name, and star level
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPlayer) Color(0xFF047857) else Color(0xFFB91C1C)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = getFighterImage(fighter.name)),
                        contentDescription = fighter.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = if (isAlive) 0.85f else 0.3f
                    )
                    if (!isAlive) {
                        Text("💀", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fighter.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAlive) Color.White else Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "⭐".repeat(fighter.starLevel),
                            fontSize = 8.sp,
                            color = Color(0xFFFBBF24)
                        )
                    }

                    // Faction and Class chips
                    Text(
                        text = "${fighter.fighterClass.displayName} • ${fighter.faction.displayName}",
                        fontSize = 8.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Health Bar Section
            val healthRatio = (fighter.health.toFloat() / maxOf(1, fighter.maxHp)).coerceIn(0f, 1f)
            val animatedHealth by animateFloatAsState(targetValue = healthRatio, label = "health_anim")

            val healthBarColor = when {
                !isAlive -> Color(0xFF475569)
                healthRatio > 0.5f -> Color(0xFF22C55E) // Green
                healthRatio > 0.25f -> Color(0xFFF59E0B) // Amber
                else -> Color(0xFFEF4444) // Red
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "HP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = healthBarColor
                )
                Text(
                    text = if (isAlive) "${fighter.health}/${fighter.maxHp}" else "DEFEATED",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlive) healthBarColor else Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF334155))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedHealth)
                        .background(healthBarColor)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Mana & Cooldown Bar Section
            val manaRatio = (fighter.mana.toFloat() / 100f).coerceIn(0f, 1f)
            val isCooldownReady = fighter.abilityCooldown <= 0
            val isAbilityCastReady = fighter.isAbilityReady && isAlive

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isAbilityCastReady) "⚡ READY!" else if (!isCooldownReady) "⏳ CD: ${fighter.abilityCooldown}t" else "MP",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAbilityCastReady) Color(0xFF38BDF8) else if (!isCooldownReady) Color(0xFFF59E0B) else Color(0xFF64748B)
                )
                Text(
                    text = "${fighter.mana}/100",
                    fontSize = 8.sp,
                    color = Color(0xFF38BDF8)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF334155))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(manaRatio)
                        .background(if (isAbilityCastReady) Color(0xFF38BDF8) else Color(0xFF0284C7))
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Fighter Combat Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val atk = maxOf(fighter.physAttack, fighter.magAttack, fighter.damage)
                val def = maxOf(fighter.physDefense, fighter.magDefense, fighter.defense)
                Text("⚔️ $atk", fontSize = 8.sp, color = Color.White)
                Text("🛡️ $def", fontSize = 8.sp, color = Color.White)
                Text("⚡ ${fighter.attackSpeed}", fontSize = 8.sp, color = Color(0xFFFBBF24))
                Text("🎯 ${fighter.attackRange}", fontSize = 8.sp, color = Color(0xFF38BDF8))
            }

            // Special Ability Chip with Cooldown & Effect Badge
            val ability = fighter.effectiveAbility
            val effectBadge = when {
                ability.effectType == AbilityEffectType.HEAL || ability.healAmount > 0 -> "💚 Heal"
                ability.effectType == AbilityEffectType.MULTI_ATTACK || ability.multiAttackHits > 1 -> "⚡ ${ability.multiAttackHits}x"
                ability.shieldAmount > 0 -> "🛡️ Shield"
                else -> "✨ Burst"
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF334155).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("✨", fontSize = 8.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = ability.name,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = effectBadge,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ability.healAmount > 0) Color(0xFF34D399) else Color(0xFFFBBF24)
                )
            }
        }
    }
}

/**
 * Chronological combat log section visualizing attacks, special abilities, dodges, and defeats.
 */
@Composable
private fun CombatLogSection(
    logs: List<String>,
    selectedFilter: LogFilterType,
    onFilterSelected: (LogFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("combat_actions_log_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Log Header & Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📜 COMBAT ACTION LOG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = selectedFilter == LogFilterType.ALL,
                        onClick = { onFilterSelected(LogFilterType.ALL) },
                        label = { Text("All", fontSize = 9.sp) },
                        modifier = Modifier.height(26.dp)
                    )
                    FilterChip(
                        selected = selectedFilter == LogFilterType.ABILITIES,
                        onClick = { onFilterSelected(LogFilterType.ABILITIES) },
                        label = { Text("Abilities ✨", fontSize = 9.sp) },
                        modifier = Modifier.height(26.dp)
                    )
                    FilterChip(
                        selected = selectedFilter == LogFilterType.COMMANDER,
                        onClick = { onFilterSelected(LogFilterType.COMMANDER) },
                        label = { Text("Commander 👑", fontSize = 9.sp) },
                        modifier = Modifier.height(26.dp)
                    )
                    FilterChip(
                        selected = selectedFilter == LogFilterType.DEFEATS,
                        onClick = { onFilterSelected(LogFilterType.DEFEATS) },
                        label = { Text("Defeats 💀", fontSize = 9.sp) },
                        modifier = Modifier.height(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val filteredLogs = logs.filter { log ->
                when (selectedFilter) {
                    LogFilterType.ALL -> true
                    LogFilterType.ABILITIES -> log.contains("casts") || log.contains("unleashes") || log.contains("✨") || log.contains("activates")
                    LogFilterType.COMMANDER -> log.contains("COMMANDER") || log.contains("TACTICAL") || log.contains("buffs") || log.contains("strikes") || log.contains("shouts")
                    LogFilterType.CRITICALS -> log.contains("CRITICALLY") || log.contains("💥")
                    LogFilterType.DEFEATS -> log.contains("defeated") || log.contains("💀") || log.contains("Victory") || log.contains("Defeat")
                }
            }

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No combat actions matching current filter.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredLogs) { logMessage ->
                        CombatLogItem(message = logMessage)
                    }
                }
            }
        }
    }
}

/**
 * Individual action item entry within the combat log.
 */
@Composable
private fun CombatLogItem(message: String) {
    val (icon, tintColor, bgTint) = when {
        message.contains("✨") || message.contains("casts") || message.contains("unleashes") ->
            Triple("✨", Color(0xFF38BDF8), Color(0xFF0369A1).copy(alpha = 0.2f))
        message.contains("CRITICALLY") || message.contains("💥") ->
            Triple("💥", Color(0xFFFBBF24), Color(0xB45309).copy(alpha = 0.2f))
        message.contains("💀") || message.contains("defeated") ->
            Triple("💀", Color(0xFFEF4444), Color(0xFF7F1D1D).copy(alpha = 0.25f))
        message.contains("🛡️") || message.contains("reflects") ->
            Triple("🛡️", Color(0xFF34D399), Color(0xFF065F46).copy(alpha = 0.2f))
        message.contains("⚡") || message.contains("dodged") ->
            Triple("⚡", Color(0xFFA78BFA), Color(0xFF5B21B6).copy(alpha = 0.2f))
        message.contains("Victory") ->
            Triple("🏆", Color(0xFF10B981), Color(0xFF064E3B).copy(alpha = 0.3f))
        message.contains("Defeat") ->
            Triple("❌", Color(0xFFEF4444), Color(0xFF7F1D1D).copy(alpha = 0.3f))
        else ->
            Triple("⚔️", Color(0xFFE2E8F0), Color(0xFF334155).copy(alpha = 0.3f))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgTint)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 11.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = message,
            fontSize = 10.sp,
            color = tintColor,
            fontWeight = FontWeight.Medium,
            lineHeight = 13.sp
        )
    }
}

@Composable
fun CombatLogOverlay(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        logs.forEachIndexed { index, log ->
            val alpha = (1f - (index.toFloat() * 0.15f)).coerceAtLeast(0.3f)
            Surface(
                color = Color.Black.copy(alpha = 0.6f * alpha),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = log,
                    color = Color.White.copy(alpha = alpha),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CinematicBattleView(
    playerFighters: List<Fighter>,
    opponentFighters: List<Fighter>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Draw Arena Floor Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / 8
            val cellHeight = size.height / 8

            // Draw stone tiles feel
            for (x in 0 until 8) {
                for (y in 0 until 8) {
                    val tileColor = if ((x + y) % 2 == 0) Color(0xFF1E293B) else Color(0xFF1A2233)
                    drawRect(
                        color = tileColor,
                        topLeft = Offset(x * cellWidth, y * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                }
            }

            // Draw grid lines (vibrant energy look)
            for (i in 0..8) {
                val alpha = if (i == 4) 0.8f else 0.2f
                val color = if (i == 4) Color(0xFF38BDF8) else Color(0xFF334155)
                val width = if (i == 4) 2f else 1f
                
                drawLine(
                    color = color.copy(alpha = alpha),
                    start = Offset(cellWidth * i, 0f),
                    end = Offset(cellWidth * i, size.height),
                    strokeWidth = width
                )
                drawLine(
                    color = color.copy(alpha = alpha),
                    start = Offset(0f, cellHeight * i),
                    end = Offset(size.width, cellHeight * i),
                    strokeWidth = width
                )
            }

            // Draw player units as vibrant tokens
            playerFighters.filter { it.isAlive && it.startY >= 0 }.forEach { fighter ->
                val centerX = cellWidth * fighter.x + cellWidth / 2
                val centerY = cellHeight * fighter.y + cellHeight / 2
                val radius = minOf(cellWidth, cellHeight) * 0.45f

                // Glow effect
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF10B981).copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = radius * 1.5f
                    ),
                    center = Offset(centerX, centerY),
                    radius = radius * 1.5f
                )

                // Unit body
                drawCircle(
                    color = Color(0xFF10B981),
                    center = Offset(centerX, centerY),
                    radius = radius
                )
                drawCircle(
                    color = Color.White,
                    center = Offset(centerX, centerY),
                    radius = radius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )

                // Health bar
                val healthWidth = radius * 1.6f
                val healthHeight = 8f
                val healthRatio = fighter.hp.toFloat() / maxOf(1, fighter.maxHp)
                
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(centerX - healthWidth / 2, centerY + radius + 4f),
                    size = Size(healthWidth, healthHeight)
                )
                drawRect(
                    color = Color(0xFF22C55E),
                    topLeft = Offset(centerX - healthWidth / 2, centerY + radius + 4f),
                    size = Size(healthWidth * healthRatio, healthHeight)
                )
                
                // Mana bar
                val manaRatio = fighter.mana.toFloat() / 100f
                drawRect(
                    color = Color(0xFF38BDF8),
                    topLeft = Offset(centerX - healthWidth / 2, centerY + radius + 14f),
                    size = Size(healthWidth * manaRatio, 4f)
                )
            }

            // Draw opponent units as menacing tokens
            opponentFighters.filter { it.isAlive }.forEach { fighter ->
                val centerX = cellWidth * fighter.x + cellWidth / 2
                val centerY = cellHeight * fighter.y + cellHeight / 2
                val radius = minOf(cellWidth, cellHeight) * 0.45f

                // Glow effect
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFEF4444).copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = radius * 1.5f
                    ),
                    center = Offset(centerX, centerY),
                    radius = radius * 1.5f
                )

                // Unit body
                drawCircle(
                    color = Color(0xFFEF4444),
                    center = Offset(centerX, centerY),
                    radius = radius
                )
                drawCircle(
                    color = Color.White,
                    center = Offset(centerX, centerY),
                    radius = radius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )

                // Health bar
                val healthWidth = radius * 1.6f
                val healthHeight = 8f
                val healthRatio = fighter.hp.toFloat() / maxOf(1, fighter.maxHp)
                
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(centerX - healthWidth / 2, centerY + radius + 4f),
                    size = Size(healthWidth, healthHeight)
                )
                drawRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(centerX - healthWidth / 2, centerY + radius + 4f),
                    size = Size(healthWidth * healthRatio, healthHeight)
                )
                
                // Mana bar
                val manaRatio = fighter.mana.toFloat() / 100f
                drawRect(
                    color = Color(0xFF818CF8),
                    topLeft = Offset(centerX - healthWidth / 2, centerY + radius + 14f),
                    size = Size(healthWidth * manaRatio, 4f)
                )
            }
        }
        
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MYTHIC ARENA BATTLEGROUND",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
