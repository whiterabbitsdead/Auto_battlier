package com.example.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.CombatEngine.Combatant
import com.example.game.CombatEngine.Team

/**
 * Creates default sample teams showcasing upgraded heroes and 5-gold vs 3-star 1-gold heroes.
 */
fun createSampleCombatTeams(): Pair<List<Combatant>, List<Combatant>> {
    val stock1Gold = CombatEngine.createStockHero(1, "Steel Bastion")
    val threeStar1Gold = CombatEngine.upgradeCombatant(stock1Gold, 3).copy(
        team = Team.TEAM_A,
        specialAbility = getFighterSpecialAbility("Steel Bastion", FighterClass.CONVICT, 84)
    )

    val teamA = listOf(
        threeStar1Gold, // 3-star 1-gold hero (~560 HP, 84 Atk)
        Combatant(
            name = "Brus Li",
            hp = 340,
            maxHp = 340,
            attack = 55,
            speed = 18,
            defense = 24,
            team = Team.TEAM_A,
            cost = 2,
            starLevel = 2,
            specialAbility = getFighterSpecialAbility("Brus Li", FighterClass.KNUCKLE_POWER, 55)
        ),
        Combatant(
            name = "Gale Marksman",
            hp = 140,
            maxHp = 140,
            attack = 30,
            speed = 12,
            defense = 10,
            team = Team.TEAM_A,
            cost = 1,
            starLevel = 1,
            specialAbility = getFighterSpecialAbility("Gale Marksman", FighterClass.HUNTERS, 30)
        )
    )

    // 5-gold 1-star hero (~650 HP, 88 Atk) close in stats to 3-star 1-gold (~560 HP, 84 Atk)
    val fiveGoldHero = CombatEngine.createStockHero(5, "Solar Aegis").copy(
        team = Team.TEAM_B,
        specialAbility = getFighterSpecialAbility("Solar Aegis", FighterClass.ILLUMINS, 88)
    )

    val teamB = listOf(
        fiveGoldHero, // 5-gold 1-star hero
        Combatant(
            name = "Night Stalker",
            hp = 320,
            maxHp = 320,
            attack = 60,
            speed = 15,
            defense = 18,
            team = Team.TEAM_B,
            cost = 3,
            starLevel = 2,
            specialAbility = getFighterSpecialAbility("Night Stalker", FighterClass.HUNTERS, 60)
        ),
        Combatant(
            name = "Mad Grin",
            hp = 130,
            maxHp = 130,
            attack = 26,
            speed = 14,
            defense = 8,
            team = Team.TEAM_B,
            cost = 1,
            starLevel = 1,
            specialAbility = getFighterSpecialAbility("Mad Grin", FighterClass.CONVICT, 26)
        )
    )

    return Pair(teamA, teamB)
}

/**
 * Basic Composable UI that displays the current health status of two teams of fighters,
 * using the CombatLog history to show a list of recent combat events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombatHealthStatusScreen(
    initialTeamA: List<Combatant>? = null,
    initialTeamB: List<Combatant>? = null,
    combatLog: CombatLog = remember { CombatLog() },
    engine: CombatEngine = remember { CombatEngine(combatLog = combatLog) },
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sampleTeams = remember { createSampleCombatTeams() }
    val baseTeamA = remember(initialTeamA) { initialTeamA ?: sampleTeams.first }
    val baseTeamB = remember(initialTeamB) { initialTeamB ?: sampleTeams.second }

    var combatState by remember {
        mutableStateOf(engine.initCombat(baseTeamA, baseTeamB))
    }
    var updateTrigger by remember { mutableIntStateOf(0) }

    val recentEvents = remember(updateTrigger, combatState) {
        combatLog.getRecentHistory(count = 15)
    }

    val teamAFighters = combatState.teamAFighters
    val teamBFighters = combatState.teamBFighters

    val teamATotalHp = teamAFighters.sumOf { it.hp }
    val teamAMaxHp = teamAFighters.sumOf { it.maxHp }.coerceAtLeast(1)
    val teamBTotalHp = teamBFighters.sumOf { it.hp }
    val teamBMaxHp = teamBFighters.sumOf { it.maxHp }.coerceAtLeast(1)

    val teamAAlive = teamAFighters.count { it.isAlive }
    val teamBAlive = teamBFighters.count { it.isAlive }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("combat_health_status_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "⚔️ TEAM COMBAT ARENA",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                        Text(
                            text = "Round ${combatState.round} • Status: ${combatState.outcome}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate Back",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        bottomBar = {
            CombatControlBar(
                isGameOver = combatState.isGameOver,
                onSimulateTurn = {
                    if (!combatState.isGameOver) {
                        combatState = engine.processTurn(combatState, combatLog)
                        updateTrigger++
                    }
                },
                onAutoFight = {
                    while (!combatState.isGameOver) {
                        combatState = engine.processTurn(combatState, combatLog)
                    }
                    updateTrigger++
                },
                onReset = {
                    combatState = engine.initCombat(baseTeamA, baseTeamB)
                    updateTrigger++
                }
            )
        },
        containerColor = Color(0xFF0B1120)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Team Total Health Comparison Meter
            item {
                Spacer(modifier = Modifier.height(4.dp))
                TeamHealthComparisonCard(
                    teamATotalHp = teamATotalHp,
                    teamAMaxHp = teamAMaxHp,
                    teamAAlive = teamAAlive,
                    teamASize = teamAFighters.size,
                    teamBTotalHp = teamBTotalHp,
                    teamBMaxHp = teamBMaxHp,
                    teamBAlive = teamBAlive,
                    teamBSize = teamBFighters.size
                )
            }

            // 2. Team A Health Status
            item {
                TeamHealthSection(
                    teamTitle = "TEAM A (BLUE)",
                    teamColor = Color(0xFF38BDF8),
                    badgeColor = Color(0xFF0369A1),
                    fighters = teamAFighters,
                    aliveCount = teamAAlive
                )
            }

            // 3. Team B Health Status
            item {
                TeamHealthSection(
                    teamTitle = "TEAM B (RED)",
                    teamColor = Color(0xFFF87171),
                    badgeColor = Color(0xFFB91C1C),
                    fighters = teamBFighters,
                    aliveCount = teamBAlive
                )
            }

            // 4. CombatLog Recent History
            item {
                CombatRecentEventsCard(events = recentEvents)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Top comparison card showcasing total HP and alive counts for both teams.
 */
@Composable
fun TeamHealthComparisonCard(
    teamATotalHp: Int,
    teamAMaxHp: Int,
    teamAAlive: Int,
    teamASize: Int,
    teamBTotalHp: Int,
    teamBMaxHp: Int,
    teamBAlive: Int,
    teamBSize: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("team_health_comparison_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team A summary
                Column {
                    Text(
                        text = "TEAM A",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "$teamATotalHp / $teamAMaxHp HP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "$teamAAlive/$teamASize Alive",
                        fontSize = 11.sp,
                        color = if (teamAAlive > 0) Color(0xFF4ADE80) else Color(0xFFEF4444)
                    )
                }

                Text(
                    text = "VS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF64748B)
                )

                // Team B summary
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TEAM B",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF87171)
                    )
                    Text(
                        text = "$teamBTotalHp / $teamBMaxHp HP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "$teamBAlive/$teamBSize Alive",
                        fontSize = 11.sp,
                        color = if (teamBAlive > 0) Color(0xFF4ADE80) else Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual HP balance bar
            val totalCurrent = (teamATotalHp + teamBTotalHp).coerceAtLeast(1)
            val teamARatio = teamATotalHp.toFloat() / totalCurrent

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF334155))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(teamARatio.coerceAtLeast(0.01f))
                        .background(Color(0xFF0284C7))
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight((1f - teamARatio).coerceAtLeast(0.01f))
                        .background(Color(0xFFDC2626))
                )
            }
        }
    }
}

/**
 * Team Health section displaying all fighters on a team.
 */
@Composable
fun TeamHealthSection(
    teamTitle: String,
    teamColor: Color,
    badgeColor: Color,
    fighters: List<Combatant>,
    aliveCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(teamColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = teamTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = "$aliveCount / ${fighters.size} Alive",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            fighters.forEach { fighter ->
                FighterHealthCard(fighter = fighter)
            }
        }
    }
}

/**
 * Individual Fighter health card displaying HP, health bar, star level, and attributes.
 */
@Composable
fun FighterHealthCard(
    fighter: Combatant,
    modifier: Modifier = Modifier
) {
    val hpFraction = (fighter.hp.toFloat() / fighter.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedHpFraction by animateFloatAsState(targetValue = hpFraction, label = "hp_fraction")

    val hpBarColor by animateColorAsState(
        targetValue = when {
            fighter.hp <= 0 -> Color(0xFF64748B)
            hpFraction > 0.5f -> Color(0xFF10B981) // Emerald green
            hpFraction > 0.25f -> Color(0xFFF59E0B) // Amber
            else -> Color(0xFFEF4444) // Red
        },
        label = "hp_bar_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fighter_health_card_${fighter.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (fighter.isAlive) Color(0xFF0F172A) else Color(0xFF1E293B).copy(alpha = 0.6f)
        ),
        border = if (fighter.isAlive) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Row 1: Fighter name, star level, and Alive/Defeated status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = fighter.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (fighter.isAlive) Color.White else Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⭐".repeat(fighter.starLevel),
                        fontSize = 11.sp,
                        color = Color(0xFFFBBF24)
                    )
                    if (fighter.cost > 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${fighter.cost}🪙",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (fighter.isAlive) Color(0xFF065F46) else Color(0xFF7F1D1D)
                ) {
                    Text(
                        text = if (fighter.isAlive) "ALIVE" else "💀 KO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (fighter.isAlive) Color(0xFFA7F3D0) else Color(0xFFFECACA),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Health numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Health",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "${fighter.hp} / ${fighter.maxHp} HP (${(hpFraction * 100).toInt()}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (fighter.isAlive) Color.White else Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 3: Linear Health Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF334155))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedHpFraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(hpBarColor)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 4: Attributes badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttributeChip(label = "ATK", value = "${fighter.attack}")
                AttributeChip(label = "DEF", value = "${fighter.defense}")
                AttributeChip(label = "SPD", value = "${fighter.speed}")
                if (fighter.specialAbility != null) {
                    AttributeChip(
                        label = "SKILL",
                        value = fighter.specialAbility.name.substringBefore(" ").take(8)
                    )
                }
            }
        }
    }
}

@Composable
fun AttributeChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF1E293B)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = value,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF1F5F9)
            )
        }
    }
}

/**
 * Recent combat events log section using [CombatLog.getRecentHistory].
 */
@Composable
fun CombatRecentEventsCard(
    events: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("combat_recent_events_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📜 RECENT COMBAT LOG",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${events.size} events",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No combat events recorded yet.\nTap 'Simulate Turn' to initiate combat actions!",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    events.takeLast(10).reversed().forEachIndexed { index, eventText ->
                        CombatEventItem(eventText = eventText, index = index)
                    }
                }
            }
        }
    }
}

@Composable
fun CombatEventItem(
    eventText: String,
    index: Int,
    modifier: Modifier = Modifier
) {
    val isDefeat = eventText.contains("defeated") || eventText.contains("💀")
    val isAbility = eventText.contains("Special Ability") || eventText.contains("✨")
    val isRound = eventText.contains("Round") || eventText.contains("---")

    val (bgColor, textColor) = when {
        isDefeat -> Pair(Color(0xFF450A0A), Color(0xFFFECACA))
        isAbility -> Pair(Color(0xFF3B0764), Color(0xFFE9D5FF))
        isRound -> Pair(Color(0xFF172554), Color(0xFFBFDBFE))
        else -> Pair(Color(0xFF0F172A), Color(0xFFE2E8F0))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = eventText,
            fontSize = 11.sp,
            color = textColor,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/**
 * Bottom control bar to step turn, auto battle, or reset.
 */
@Composable
fun CombatControlBar(
    isGameOver: Boolean,
    onSimulateTurn: () -> Unit,
    onAutoFight: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("combat_control_bar"),
        color = Color(0xFF0F172A),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSimulateTurn,
                enabled = !isGameOver,
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("simulate_turn_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    disabledContainerColor = Color(0xFF334155)
                )
            ) {
                Text(
                    text = if (isGameOver) "Battle Ended" else "⚔️ Next Turn",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onAutoFight,
                enabled = !isGameOver,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("auto_fight_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED),
                    disabledContainerColor = Color(0xFF334155)
                )
            ) {
                Text(text = "⚡ Auto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("reset_battle_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "🔄 Reset", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

/**
 * Top-level Composable alias matching generic request signatures.
 */
@Composable
fun TeamCombatView(
    teamA: List<Combatant>,
    teamB: List<Combatant>,
    combatLog: CombatLog = remember { CombatLog() },
    modifier: Modifier = Modifier
) {
    CombatHealthStatusScreen(
        initialTeamA = teamA,
        initialTeamB = teamB,
        combatLog = combatLog,
        modifier = modifier
    )
}

@Composable
fun CombatStatusScreen(modifier: Modifier = Modifier) {
    CombatHealthStatusScreen(modifier = modifier)
}
