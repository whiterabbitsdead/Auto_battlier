package com.example.game

import com.example.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.ui.graphics.Brush

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.zIndex
import androidx.compose.runtime.*

@Composable
fun CommanderSelectionScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    var filterTab by remember { mutableStateOf(0) } // 0: All, 1: Unlocked, 2: Locked

    val unlockedNames = state.unlockedCommanderNames
    val filteredCommanders = remember(filterTab, unlockedNames) {
        when (filterTab) {
            1 -> availableCommanders.filter { unlockedNames.contains(it.name) }
            2 -> availableCommanders.filter { !unlockedNames.contains(it.name) }
            else -> availableCommanders
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Commanders & Fighters",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { viewModel.toggleAdmin() }
            )
            if (state.isAdmin) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color.Red,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "ADMIN",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Text(
            text = "Comic Roster: 20 Heroes across 4 Roles & 7 Factions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )

        // Battle Credits & Demo Controls Bar
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "🪙 ${state.battleCredits} Battle Credits",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Unlocked: ${unlockedNames.size}/20 Commanders",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }

                Button(
                    onClick = { viewModel.addDemoCredits(100) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("add_demo_credits_btn")
                ) {
                    Text("[Demo] +100 Credits", fontSize = 11.sp)
                }
            }
        }

        // Feedback Banner (e.g. Unlocked Brus Li or Insufficient credits)
        state.unlockFeedbackMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (msg.contains("Need")) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = msg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (msg.contains("Need")) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF2E7D32),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterTab == 0,
                onClick = { filterTab = 0 },
                label = { Text("All (20)", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = filterTab == 1,
                onClick = { filterTab = 1 },
                label = { Text("Unlocked (${unlockedNames.size})", fontSize = 11.sp) },
                modifier = Modifier.weight(1.1f)
            )
            FilterChip(
                selected = filterTab == 2,
                onClick = { filterTab = 2 },
                label = { Text("Locked (${20 - unlockedNames.size})", fontSize = 11.sp) },
                modifier = Modifier.weight(1.1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredCommanders) { commander ->
                val isUnlocked = unlockedNames.contains(commander.name)
                val unlockInfo = CommanderUnlockManager.UNLOCK_CATALOG[commander.name]
                var isInspected by remember { mutableStateOf(false) }

                // Distinct solid background color per commander
                val solidCardColor = when (commander.preferredFaction) {
                    Faction.REPTILIANS -> Color(0xFF1B4332)
                    Faction.EBONS -> Color(0xFF241442)
                    Faction.P45 -> Color(0xFF1F2937)
                    Faction.FEET_WORK -> Color(0xFF450A0A)
                    Faction.ELEMOS -> Color(0xFF0C4A6E)
                    Faction.TULS -> Color(0xFF3F2B1D)
                    Faction.WIND_WALKER -> Color(0xFF134E4A)
                    else -> Color(0xFF1E293B)
                }

                // Card container with solid color background and invisible full-area functional button behind graphic
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isInspected = !isInspected }
                        .testTag("commander_${commander.name.lowercase().replace(' ', '_')}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) solidCardColor else solidCardColor.copy(alpha = 0.55f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 4.dp else 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Visible Commander Graphic and details content layer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Commander Graphic Container
                            Box(contentAlignment = Alignment.Center) {
                                if (commander.imageRes != 0) {
                                    Image(
                                        painter = painterResource(id = commander.imageRes),
                                        contentDescription = commander.name,
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                2.dp,
                                                if (isUnlocked) Color(0xFFFFD700) else Color.Gray,
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Custom artistic graphic representation with commander symbol & solid faction theme
                                    val graphicIcon = when (commander.preferredClass) {
                                        FighterClass.KNUCKLE_POWER -> "🥊"
                                        FighterClass.HUNTERS -> "🏹"
                                        FighterClass.ILLUMINS -> "🔮"
                                        FighterClass.CONVICT -> "⛓️"
                                        else -> "⚔️"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .background(
                                                if (isUnlocked) Color(0xFF0F172A) else Color(0xFF1E293B),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                2.dp,
                                                if (isUnlocked) Color(0xFFFFD700) else Color.DarkGray,
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(if (isUnlocked) graphicIcon else "🔒", fontSize = 28.sp)
                                            Text(
                                                commander.name.take(6).uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.LightGray
                                            )
                                        }
                                    }
                                }
                                if (!isUnlocked) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(76.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("🔒", fontSize = 26.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = commander.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) Color.White else Color.LightGray
                                    )
                                    if (isUnlocked) {
                                        Surface(
                                            color = Color(0xFF2E7D32),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "UNLOCKED",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (isInspected) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${commander.preferredFaction.displayName} • ${commander.preferredClass.displayName} (${commander.cost} Gold)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFFE082),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (commander.weapon.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                                            Text("⚔️ Weapon: ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                                            Text(commander.weapon, fontSize = 10.sp, color = Color.White)
                                        }
                                    }

                                    Text(
                                        text = commander.abilityDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE2E8F0),
                                        maxLines = 2
                                    )

                                    if (commander.activeAbility != null || commander.passiveAbility != null) {
                                        Column(modifier = Modifier.padding(top = 4.dp)) {
                                            commander.activeAbility?.let { active ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("✨ ${active.name}: ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4FD1C5))
                                                    Text(active.description, fontSize = 9.sp, color = Color.LightGray, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                                }
                                            }
                                            commander.passiveAbility?.let { passive ->
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 1.dp)) {
                                                    Text("🛡️ Passive: ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF6AD55))
                                                    Text(passive.description, fontSize = 9.sp, color = Color.LightGray, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Click to view stats & abilities",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.LightGray.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                if (isUnlocked) {
                                    Button(
                                        onClick = { viewModel.selectCommander(commander) },
                                        modifier = Modifier
                                            .height(30.dp)
                                            .testTag("select_commander_${commander.name.lowercase().replace(' ', '_')}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                    ) {
                                        Text("Deploy Commander", fontSize = 11.sp, color = Color.White)
                                    }
                                } else if (unlockInfo != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.openUnlockDialog(commander) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                            modifier = Modifier.height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("Unlock / Buy", fontSize = 11.sp, color = Color.White)
                                        }
                                        Text(
                                            text = "${unlockInfo.unlockCreditCost} Credits or $${unlockInfo.cashPrice}",
                                            fontSize = 10.sp,
                                            color = Color(0xFFCBD5E1)
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

    // Unlock Dialog
    state.showUnlockDialogFor?.let { commanderToUnlock ->
        val info = CommanderUnlockManager.UNLOCK_CATALOG[commanderToUnlock.name]
        AlertDialog(
            onDismissRequest = { viewModel.dismissUnlockDialog() },
            title = {
                Text(text = "Unlock ${commanderToUnlock.name}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Role: ${commanderToUnlock.preferredClass.displayName} | Faction: ${commanderToUnlock.preferredFaction.displayName} (${commanderToUnlock.cost} Gold)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = commanderToUnlock.abilityDescription, fontSize = 12.sp)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Text(
                        text = "🎮 Option 1: Gameplay Unlock",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Cost: ${info?.unlockCreditCost ?: 100} Battle Credits\nYour Credits: ${state.battleCredits}\nRequirement: ${info?.unlockRequirement ?: "Round progression"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Button(
                        onClick = { viewModel.unlockWithGameplayCredits(commanderToUnlock) },
                        enabled = state.battleCredits >= (info?.unlockCreditCost ?: 100),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unlock with Credits (${info?.unlockCreditCost ?: 100} 🪙)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "💵 Option 2: Cash for Play Purchase",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "[DEMO DATA] Price: $${info?.cashPrice ?: "1.99"} USD\nNote: This is a simulated demo transaction for gameplay testing.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedButton(
                        onClick = { viewModel.unlockWithCashPurchase(commanderToUnlock) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simulate Cash Purchase ($${info?.cashPrice ?: "1.99"})")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.addDemoCredits(100) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("[Demo Action] Add +100 Credits")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissUnlockDialog() }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun BattleBoardScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showLobbyDetails by remember { mutableStateOf(false) }
    var showShopModal by remember { mutableStateOf(false) }
    var showEquipmentModal by remember { mutableStateOf(false) }
    var showEnhancementModal by remember { mutableStateOf(false) }

    // Drag and Drop State
    var draggingFighterId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Matchup Header with Commander Portraits
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player Commander Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1.2f)
                ) {
                    val pImg = state.playerCommander?.imageRes ?: 0
                    if (pImg != 0) {
                        Image(
                            painter = painterResource(id = pImg),
                            contentDescription = state.playerCommander?.name ?: "Player",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        val playerHp = state.playerCommander?.hp ?: 100
                        val hpColor = if (playerHp > 50) Color(0xFF2E7D32) else if (playerHp > 25) Color(0xFFF57C00) else Color.Red
                        Text(
                            text = state.playerCommander?.title?.ifEmpty { state.playerCommander?.name?.substringBefore(" (") } ?: "Player",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "HP: $playerHp/100",
                            color = hpColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Lvl ${state.playerLevel} | 🪙 ${state.playerGold}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.toggleAdmin() }) {
                            Text(text = "Admin: ", fontSize = 9.sp, color = Color.Gray)
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (state.isAdmin) Color.Green else Color.Red))
                        }
                    }
                }

                // Middle: Round & Action
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "R${state.round}-${state.fight}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (state.battleResult != null) {
                        Button(
                            onClick = { viewModel.nextFight() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Next Fight", fontSize = 11.sp)
                        }
                    } else if (!state.isBattleActive) {
                        Button(
                            onClick = { viewModel.startBattle() },
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("start_battle"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Fight (${state.prepTimer}s)", fontSize = 11.sp)
                        }
                    } else {
                        Text("Battling...", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    if (state.isAdmin) {
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedButton(
                            onClick = { viewModel.navigateToBattleVisualization() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("open_battle_visualizer_button")
                        ) {
                            Text("Visualizer ⚔️", fontSize = 9.sp)
                        }
                    }
                }

                // Opponent Commander Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1.2f)
                ) {
                    val aImg = state.aiCommander?.imageRes ?: 0
                    val isNpc = (state.round in 2..4 && state.fight == 3)
                    val aiHp = state.aiCommander?.hp ?: 100
                    val aiHpColor = if (aiHp > 50) Color.Red else Color.Magenta

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isNpc) "Boss Dragon" else (state.aiCommander?.title?.ifEmpty { state.aiCommander?.name?.substringBefore(" (") } ?: "Opponent"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isNpc) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "HP: $aiHp/100",
                            color = aiHpColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        val oppLevel = state.aiPlayers.find { it.id == state.currentOpponentAiId }?.level ?: 3
                        Text(
                            text = if (isNpc) "👹 Boss" else "Lvl $oppLevel Opponent",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    if (aImg != 0) {
                        Image(
                            painter = painterResource(id = aImg),
                            contentDescription = state.aiCommander?.name ?: "Opponent",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, Color.Red, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Red, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isNpc) "🐲" else "🤖", fontSize = 24.sp)
                        }
                    }
                }
            }
        }

        // Lobby Standings Bar (Player + 3 AI Opponents)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clickable { showLobbyDetails = !showLobbyDetails },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val livingCount = (if ((state.playerCommander?.hp ?: 0) > 0) 1 else 0) + state.aiPlayers.count { it.isAlive }
                    Text(
                        text = "🏆 Lobby Arena ($livingCount/4 Active) • ${if (showLobbyDetails) "Hide ▲" else "Standings ▼"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Cap: ${state.playerFighters.count { it.startY >= 0 }}/${state.playerLevel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showLobbyDetails) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val playerEntry = LobbyEntry(
                        name = "${state.playerCommander?.title?.ifEmpty { state.playerCommander?.name?.substringBefore(" (") } ?: "Player"} (You)",
                        imageRes = state.playerCommander?.imageRes ?: 0,
                        hp = state.playerCommander?.hp ?: 0,
                        level = state.playerLevel,
                        gold = state.playerGold,
                        isCurrentOpponent = false,
                        isAlive = (state.playerCommander?.hp ?: 0) > 0,
                        strategy = "Player Army"
                    )
                    val aiEntries = state.aiPlayers.map { ai ->
                        LobbyEntry(
                            name = ai.name,
                            imageRes = ai.commander.imageRes,
                            hp = ai.hp,
                            level = ai.level,
                            gold = ai.gold,
                            isCurrentOpponent = ai.id == state.currentOpponentAiId,
                            isAlive = ai.isAlive,
                            strategy = ai.strategyName
                        )
                    }
                    val allEntries = (listOf(playerEntry) + aiEntries).sortedByDescending { it.hp }

                    allEntries.forEachIndexed { rank, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${rank + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
                            if (entry.imageRes != 0) {
                                Image(
                                    painter = painterResource(id = entry.imageRes),
                                    contentDescription = entry.name,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = entry.name + (if (entry.isCurrentOpponent) " ⚔️" else ""),
                                fontSize = 10.sp,
                                fontWeight = if (entry.isCurrentOpponent) FontWeight.Bold else FontWeight.Normal,
                                color = if (!entry.isAlive) Color.Gray else if (entry.isCurrentOpponent) Color.Red else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (entry.isAlive) {
                                Text("Lvl ${entry.level}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🪙${entry.gold}", fontSize = 9.sp, color = Color(0xFFD4AF37))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("HP: ${entry.hp}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (entry.hp > 30) Color(0xFF2E7D32) else Color.Red)
                            } else {
                                Text("💀 ELIMINATED", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        
        // Show Active Synergies
        if (state.activeSynergies.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(
                        text = "ACTIVE SYNERGIES: " + state.activeSynergies.joinToString(" • "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                }
            }
        }

        // Commander Tactical Decision Console (Active skill activation with resource bar)
        state.playerCommander?.let { cmd ->
            CommanderTacticalConsole(
                commander = cmd,
                energy = state.playerCommanderEnergy,
                isBattleActive = state.isBattleActive,
                onActivateSkill = { viewModel.activateCommanderSkill(it) },
                onAddDemoEnergy = if (state.isAdmin) { { viewModel.addDemoCommanderEnergy(50) } } else null,
                lastSkillCast = state.lastTacticalSkillCast,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Show Stacked Synergy Special Skills Result
        if (state.activeSynergySkills.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(
                        text = "⚡ STACKED SYNERGY SPECIAL SKILLS (${state.activeSynergySkills.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    state.activeSynergySkills.forEach { skill ->
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(skill.icon, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${skill.name} [${skill.synergyTag}]: ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = skill.description,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Synergy Highlighting Compatibility Info
        if (state.selectedFighterId != null && !state.isBattleActive && state.battleResult == null) {
            val selected = state.playerFighters.find { it.id == state.selectedFighterId }
            if (selected != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "COMPATIBILITY: ${selected.name.uppercase()}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.background(Color(0xFF3B82F6), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                    Text(text = selected.faction.displayName, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.background(Color(0xFF10B981), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                    Text(text = selected.fighterClass.displayName, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ADJACENCY BONUS",
                                color = Color(0xFFFACC15),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Link icons show active buffs",
                                color = Color.LightGray,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Trait Sidebar
            Surface(
                color = Color.Black.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .padding(end = 8.dp)
            ) {
                LazyColumn(modifier = Modifier.padding(4.dp)) {
                    item {
                        Text(
                            text = "TRAITS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.LightGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    
                    val boardFighters = state.playerFighters.filter { it.startY >= 0 }.distinctBy { it.name }
                    val factionCounts = boardFighters.groupingBy { it.faction }.eachCount()
                    val classCounts = boardFighters.groupingBy { it.fighterClass }.eachCount()
                    
                    // Display Active Traits first, then others
                    val allFactions = Faction.values().filter { it != Faction.NPC && it != Faction.ORC }
                    val allClasses = FighterClass.values().filter { it != FighterClass.NPC && it != FighterClass.TANK && it != FighterClass.ASSASSIN && it != FighterClass.MARKSMAN && it != FighterClass.MAGE }
                    
                    items(allFactions) { faction ->
                        val count = factionCounts.getOrDefault(faction, 0)
                        val isActive = count >= 2
                        TraitItem(name = faction.displayName, count = count, icon = "🚩", isActive = isActive)
                    }
                    
                    items(allClasses) { cls ->
                        val count = classCounts.getOrDefault(cls, 0)
                        val isActive = count >= 2
                        TraitItem(name = cls.displayName, count = count, icon = "⚔️", isActive = isActive)
                    }
                }
            }

            // Board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
            val allFighters = state.playerFighters + state.aiFighters
            
            // Draw grid lines
            for (i in 1..7) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.Gray.copy(alpha = 0.5f))
                        .align(Alignment.CenterStart)
                        .offset(x = (i * 100 / 8).dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.Gray.copy(alpha = 0.5f))
                        .align(Alignment.TopCenter)
                        .offset(y = (i * 100 / 8).dp)
                )
            }
            // Divider for player/ai sides
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.Red.copy(alpha = 0.5f))
                    .align(Alignment.Center)
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cellWidth = maxWidth / 8
                val cellHeight = maxHeight / 8

                // Clickable areas for player setup
                if (!state.isBattleActive && state.battleResult == null) {
                    for (x in 0..7) {
                        for (y in 4..7) {
                            Box(
                                modifier = Modifier
                                    .offset(x = cellWidth * x, y = cellHeight * y)
                                    .size(cellWidth, cellHeight)
                                    .clickable { viewModel.moveSelectedFighterTo(x, y) }
                            )
                        }
                    }
                }

                allFighters.filter { it.isAlive && it.y >= 0 }.forEach { fighter ->
                    val isSelected = fighter.id == state.selectedFighterId
                    val selectedFighter = state.playerFighters.find { it.id == state.selectedFighterId }
                    
                    var highlightColor: Color? = null
                    var synergyLabel: String? = null
                    
                    if (isSelected && state.potentialStatBonus != null) {
                        synergyLabel = state.potentialStatBonus
                    }

                    if (selectedFighter != null && fighter.isPlayer && fighter.id != selectedFighter.id) {
                        val sharesFaction = fighter.faction == selectedFighter.faction
                        val sharesClass = fighter.fighterClass == selectedFighter.fighterClass
                        
                        if (sharesFaction || sharesClass) {
                            highlightColor = if (sharesFaction && sharesClass) Color(0xFFF59E0B) // Gold for both
                                            else if (sharesFaction) Color(0xFF3B82F6) // Blue for faction
                                            else Color(0xFF10B981) // Green for class
                            
                            // Check adjacency for "LINKED" status
                            val isAdjacent = Math.abs(fighter.x - selectedFighter.x) <= 1 && 
                                           Math.abs(fighter.y - selectedFighter.y) <= 1
                            
                            if (isAdjacent) {
                                synergyLabel = if (sharesFaction && sharesClass) "DUAL LINK" 
                                               else if (sharesFaction) "FACTION"
                                               else "CLASS"
                            }
                        }
                    }

                    val animatedX by animateDpAsState(
                        targetValue = cellWidth * fighter.x,
                        animationSpec = tween(durationMillis = 300),
                        label = "x_anim"
                    )
                    val animatedY by animateDpAsState(
                        targetValue = cellHeight * fighter.y,
                        animationSpec = tween(durationMillis = 300),
                        label = "y_anim"
                    )

                    FighterToken(
                        fighter = fighter,
                        isSelected = isSelected,
                        highlightColor = highlightColor,
                        synergyLabel = synergyLabel,
                        modifier = Modifier
                            .offset(x = animatedX, y = animatedY)
                            .then(
                                if (draggingFighterId == fighter.id) {
                                    Modifier.zIndex(1f).offset {
                                        IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                    }
                                } else Modifier
                            )
                            .size(cellWidth, cellHeight)
                            .pointerInput(fighter.id) {
                                if (!state.isBattleActive && state.battleResult == null && fighter.isPlayer) {
                                    detectDragGestures(
                                        onDragStart = { 
                                            draggingFighterId = fighter.id 
                                            viewModel.selectFighter(fighter.id)
                                        },
                                        onDragEnd = {
                                            if (draggingFighterId != null) {
                                                // Calculate drop position
                                                // Assuming board is at some relative position...
                                                // Actually, a simpler way is to check the offset relative to the cell
                                                val dropX = fighter.x + (dragOffset.x / cellWidth.toPx()).roundToInt()
                                                val dropY = fighter.y + (dragOffset.y / cellHeight.toPx()).roundToInt()
                                                
                                                val finalX = dropX.coerceIn(0, 7)
                                                val finalY = dropY.coerceIn(-1, 7) // -1 is bench
                                                
                                                viewModel.moveSelectedFighterTo(finalX, finalY)
                                                draggingFighterId = null
                                                dragOffset = androidx.compose.ui.geometry.Offset.Zero
                                            }
                                        },
                                        onDragCancel = {
                                            draggingFighterId = null
                                            dragOffset = androidx.compose.ui.geometry.Offset.Zero
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount
                                        }
                                    )
                                }
                            }
                            .clickable(enabled = !state.isBattleActive && state.battleResult == null && fighter.isPlayer) {
                                if (state.selectedEquipmentId != null) {
                                    viewModel.equipToFighter(fighter.id)
                                } else {
                                    viewModel.selectFighter(fighter.id)
                                }
                            }
                    )
                }

                // Damage Effects
                state.damageEffects.forEach { effect ->
                    val isHeal = effect.damage < 0
                    val text = if (isHeal) "+${-effect.damage}" else "${effect.damage}"
                    val color = if (isHeal) Color(0xFF22C55E) else if (effect.isCrit) Color(0xFFF59E0B) else Color(0xFFEF4444)
                    val fontSize = if (effect.isCrit) 16.sp else 12.sp
                    
                    Box(
                        modifier = Modifier
                            .offset(x = cellWidth * effect.x + (cellWidth / 4), y = cellHeight * effect.y)
                            .animateContentSize()
                    ) {
                        Text(
                            text = text,
                            color = color,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }

                // Real-time Log Overlay
                if (state.isBattleActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(80.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.padding(6.dp),
                                reverseLayout = true
                            ) {
                                items(state.battleLogs.take(15)) { log ->
                                    Text(
                                        text = log,
                                        color = when {
                                            log.contains("defeated") || log.contains("💀") -> Color(0xFFEF4444)
                                            log.contains("Victory") || log.contains("🏆") -> Color(0xFF10B981)
                                            log.contains("✨") || log.contains("casts") -> Color(0xFF38BDF8)
                                            else -> Color.White
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = if (log.contains("💀")) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        Spacer(modifier = Modifier.height(8.dp))

        // Bench Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(8f)
                .background(Color.DarkGray, RoundedCornerShape(4.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
        ) {
            val cellWidth = maxWidth / 8
            val cellHeight = maxHeight
            
            if (!state.isBattleActive && state.battleResult == null) {
                for (x in 0..7) {
                    Box(
                        modifier = Modifier
                            .offset(x = cellWidth * x)
                            .size(cellWidth, cellHeight)
                            .border(1.dp, Color.Gray)
                            .clickable { viewModel.moveSelectedFighterTo(x, -1) }
                    )
                }
            }

            state.playerFighters.filter { it.startY == -1 }.forEach { fighter ->
                val isSelected = fighter.id == state.selectedFighterId
                val selectedFighter = state.playerFighters.find { it.id == state.selectedFighterId }
                
                var highlightColor: Color? = null
                if (selectedFighter != null && fighter.id != selectedFighter.id) {
                    if (fighter.faction == selectedFighter.faction || fighter.fighterClass == selectedFighter.fighterClass) {
                        highlightColor = Color(0xFF3B82F6)
                    }
                }

                Box(
                    modifier = Modifier
                        .offset(x = cellWidth * fighter.startX)
                        .size(cellWidth, cellHeight)
                        .padding(2.dp)
                        .background(
                            if (highlightColor != null) highlightColor.copy(alpha = 0.4f)
                            else Color.Blue.copy(alpha = 0.7f), 
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            if (isSelected || highlightColor != null) 3.dp else 1.dp, 
                            if (isSelected) Color.Yellow else if (highlightColor != null) highlightColor else Color.Black, 
                            RoundedCornerShape(4.dp)
                        )
                        .clickable(enabled = !state.isBattleActive && state.battleResult == null) {
                            if (state.selectedEquipmentId != null) {
                                viewModel.equipToFighter(fighter.id)
                            } else {
                                viewModel.selectFighter(fighter.id)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "⭐".repeat(fighter.starLevel), fontSize = 6.sp, color = Color.Yellow)
                        Text(text = fighter.name.take(3), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Action Bar for Opening Separate Windows (Shop Window & Equipment Inventory Window)
        if (!state.isBattleActive && state.battleResult == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showShopModal = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("open_shop_window_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("🛒 Open Shop Window", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                }

                Button(
                    onClick = { showEquipmentModal = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("open_inventory_window_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("🎒 Equip (${state.playerInventory.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }

                if (state.playerEnhancements.isNotEmpty()) {
                    Button(
                        onClick = { showEnhancementModal = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("open_enhancement_window_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("✨ Bonus (${state.playerEnhancements.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Sell UI / Sell Zone
        if (!state.isBattleActive && state.battleResult == null && state.selectedFighterId != null) {
            val selectedFighter = state.playerFighters.find { it.id == state.selectedFighterId }
            if (selectedFighter != null) {
                val multiplier = when (selectedFighter.starLevel) {
                    3 -> 9
                    2 -> 3
                    else -> 1
                }
                val refund = selectedFighter.cost * multiplier
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.sellSelectedFighter() }
                        .testTag("sell_zone"),
                    color = Color(0xFF991B1B), // Dark red
                    border = BorderStroke(2.dp, Color(0xFFF87171)),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                            contentDescription = "Sell",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SELL ${selectedFighter.name.uppercase()}",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Refund: 🪙 $refund Gold",
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else if (!state.isBattleActive && state.battleResult == null) {
            // Empty Sell Zone placeholder to keep UI stable
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(56.dp),
                color = Color.Gray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Select a hero to sell",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Logs
        CombatLogSection(
            logs = state.battleLogs,
            maxBufferCapacity = MAX_COMBAT_LOG_HISTORY,
            title = "📜 COMBAT LOG",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }

    // Dedicated Shop Modal Window with Shared Pool of Fighters
    if (showShopModal) {
        AlertDialog(
            onDismissRequest = { showShopModal = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🛒 RECRUITMENT SHOP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "🪙 ${state.playerGold} Gold",
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Shared pool of fighters across all commanders in the lobby (Lvl ${state.playerLevel})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShopComponent(state = state, viewModel = viewModel)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showShopModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Close Window")
                }
            }
        )
    }

    // Dedicated Enhancement Modal
    if (showEnhancementModal) {
        AlertDialog(
            onDismissRequest = { showEnhancementModal = false },
            title = {
                Text("✨ ABILITY ENHANCEMENTS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Apply specialty buffs to your fighters. These are lost after application but permanently buff the fighter.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Available Enhancements (${state.playerEnhancements.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (state.playerEnhancements.isEmpty()) {
                        Text("No enhancements available.", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(state.playerEnhancements) { enh ->
                                val isSelected = enh.id == state.selectedEnhancementId
                                Card(
                                    modifier = Modifier
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFA78BFA) else Color.Gray,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.selectEnhancement(enh.id) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFF2E1065) else Color(0xFF1E293B)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(8.dp).width(120.dp)) {
                                        Text(text = enh.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(text = enh.description, color = Color.LightGray, fontSize = 9.sp, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Mini Lineup (Select fighter to buff):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val lineupFighters = state.playerFighters
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(lineupFighters) { fighter ->
                            val isSelected = fighter.id == state.selectedFighterId
                            Card(
                                modifier = Modifier
                                    .width(96.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF475569),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (state.selectedEnhancementId != null) {
                                            viewModel.applyEnhancementToFighter(fighter.id)
                                        } else {
                                            viewModel.selectFighter(fighter.id)
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (fighter.startY == -1) Color(0xFF1E3A8A) else Color(0xFF065F46)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("⭐".repeat(fighter.starLevel), fontSize = 8.sp, color = Color.Yellow)
                                    Text(text = fighter.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                    Text(text = "HP: ${fighter.maxHp}", fontSize = 8.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showEnhancementModal = false; viewModel.selectEnhancement(null) }) {
                    Text("Done")
                }
            }
        )
    }

    // Dedicated Equipment Modal Window
    if (showEquipmentModal) {
        AlertDialog(
            onDismissRequest = { showEquipmentModal = false },
            title = {
                Text("🎒 EQUIPMENT ARSENAL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "1. Select an item from your stash\n2. Tap a fighter in your mini lineup below to equip",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Inventory Items (${state.playerInventory.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (state.playerInventory.isEmpty()) {
                        Text("No equipment in stash. Win rounds or boss fights to loot items!", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(state.playerInventory) { item ->
                                val isEqSelected = item.id == state.selectedEquipmentId
                                Card(
                                    modifier = Modifier
                                        .border(
                                            width = if (isEqSelected) 2.dp else 1.dp,
                                            color = if (isEqSelected) Color(0xFFF59E0B) else Color.Gray,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.selectEquipment(item.id) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isEqSelected) Color(0xFF451A03) else Color(0xFF1E293B)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(text = item.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "Type: ${item.type}", color = Color.LightGray, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mini Lineup of Fighters Able to Be Equipped
                    Text("Mini Lineup (Select fighter to equip):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val lineupFighters = state.playerFighters
                    if (lineupFighters.isEmpty()) {
                        Text("No fighters recruited yet. Buy from the shop first!", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(lineupFighters) { fighter ->
                                val isSelected = fighter.id == state.selectedFighterId
                                Card(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF475569),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (state.selectedEquipmentId != null) {
                                                viewModel.equipToFighter(fighter.id)
                                            } else {
                                                viewModel.selectFighter(fighter.id)
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (fighter.startY == -1) Color(0xFF1E3A8A) else Color(0xFF065F46)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (fighter.startY == -1) "BENCH" else "BOARD",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.LightGray
                                        )
                                        Text("⭐".repeat(fighter.starLevel), fontSize = 8.sp, color = Color.Yellow)
                                        Text(
                                            text = fighter.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${fighter.equipment.size}/3 Items",
                                            fontSize = 8.sp,
                                            color = Color(0xFFFFD54F)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sell Selected Fighter in Inventory UI
                    if (state.selectedFighterId != null) {
                        val selectedFighter = state.playerFighters.find { it.id == state.selectedFighterId }
                        if (selectedFighter != null) {
                            val multiplier = when (selectedFighter.starLevel) {
                                3 -> 9
                                2 -> 3
                                else -> 1
                            }
                            val refund = selectedFighter.cost * multiplier
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.sellSelectedFighter() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sell ${selectedFighter.name} for 🪙 $refund Gold", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showEquipmentModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun GameOverScreen(viewModel: GameViewModel) {
    val state = viewModel.uiState.collectAsState().value
    val playerHp = state.playerCommander?.hp ?: 100
    val placement = state.playerPlacement ?: if (playerHp > 0) 1 else (1 + state.aiPlayers.count { it.isAlive })
    val isWin = placement == 1
    
    val (titleText, titleColor) = if (isWin) {
        "🏆 Champion! (1st Place)" to Color(0xFF009900)
    } else {
        "Eliminated (#$placement of 4)" to Color.Red
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val pImg = state.playerCommander?.imageRes ?: 0
        if (pImg != 0) {
            Image(
                painter = painterResource(id = pImg),
                contentDescription = state.playerCommander?.name ?: "Player",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(3.dp, if (isWin) Color(0xFF009900) else Color.Red, RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(titleText, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = titleColor)
        Text(
            text = "Final Round: R${state.round}-${state.fight} • Level ${state.playerLevel}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Final Lobby Standings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Final Arena Standings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val playerEntry = LobbyEntry(
                    name = "${state.playerCommander?.title?.ifEmpty { state.playerCommander?.name?.substringBefore(" (") } ?: "Player"} (You)",
                    imageRes = state.playerCommander?.imageRes ?: 0,
                    hp = maxOf(0, playerHp),
                    level = state.playerLevel,
                    gold = state.playerGold,
                    isCurrentOpponent = false,
                    isAlive = playerHp > 0,
                    strategy = "Player Army"
                )
                val aiEntries = state.aiPlayers.map { ai ->
                    LobbyEntry(
                        name = ai.name,
                        imageRes = ai.commander.imageRes,
                        hp = maxOf(0, ai.hp),
                        level = ai.level,
                        gold = ai.gold,
                        isCurrentOpponent = false,
                        isAlive = ai.isAlive,
                        strategy = ai.strategyName
                    )
                }
                val allSorted = (listOf(playerEntry) + aiEntries).sortedByDescending { it.hp }

                allSorted.forEachIndexed { rank, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (rank == 0) "🥇 #1" else if (rank == 1) "🥈 #2" else if (rank == 2) "🥉 #3" else "4️⃣ #4",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(44.dp)
                        )
                        if (entry.imageRes != 0) {
                            Image(
                                painter = painterResource(id = entry.imageRes),
                                contentDescription = entry.name,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = entry.name,
                            fontSize = 12.sp,
                            fontWeight = if (entry.name.contains("(You)")) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (entry.hp > 0) "${entry.hp} HP" else "ELIMINATED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (entry.hp > 0) Color(0xFF2E7D32) else Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.resetGame() },
            modifier = Modifier.testTag("play_again_button")
        ) {
            Text("Play Again")
        }
    }
}

fun getFighterImage(name: String): Int {
    val cleanName = name.substringBefore(" (").ifBlank { name }
    return when {
        cleanName.contains("Grunt") -> R.drawable.ic_orc_grunt
        cleanName.contains("Rogue") || cleanName.contains("Stalker") || cleanName.contains("Viper") || cleanName.contains("Kael") -> R.drawable.ic_dark_rogue
        cleanName.contains("Archer") || cleanName.contains("Marksman") || cleanName.contains("Sentinel") -> R.drawable.ic_elf_archer
        cleanName.contains("Apprentice") || cleanName.contains("Seer") || cleanName.contains("Empress") || cleanName.contains("Elara") || cleanName.contains("Weaver") -> R.drawable.ic_mage_apprentice
        cleanName.contains("Warden") || cleanName.contains("Bastion") || cleanName.contains("Magnus") || cleanName.contains("Apex") -> R.drawable.ic_orc_grunt // Using grunt as placeholder for heavy units
        else -> R.drawable.ic_placeholder_fighter
    }
}

@Composable
fun FighterToken(
    fighter: Fighter,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    highlightColor: Color? = null,
    synergyLabel: String? = null
) {
    val borderColor = when {
        isSelected -> Color.Yellow
        highlightColor != null -> highlightColor
        else -> Color.Black
    }
    val borderWidth = if (isSelected || highlightColor != null) 3.dp else 1.dp

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
            .background(
                if (highlightColor != null) highlightColor.copy(alpha = 0.2f)
                else if (fighter.isPlayer) Color.Blue.copy(alpha = 0.3f)
                else Color.Red.copy(alpha = 0.3f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = getFighterImage(fighter.name)),
            contentDescription = fighter.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = if (highlightColor != null) 0.7f else 0.5f
        )
        
        // Synergy Adjacency Badge
        if (synergyLabel != null) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF10B981), RoundedCornerShape(2.dp))
                        .padding(horizontal = 2.dp)
                ) {
                    Text(text = synergyLabel, fontSize = 6.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 2.dp)
                ) {
                    Text(text = "BOOSTED", fontSize = 5.sp, color = Color(0xFFFACC15), fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "⭐".repeat(fighter.starLevel),
                fontSize = 6.sp,
                color = Color.Yellow
            )
            Text(
                text = fighter.name.take(3),
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "HP:${fighter.hp}",
                fontSize = 8.sp,
                color = Color.White
            )
            Text(
                text = "MP:${fighter.mana}/100",
                fontSize = 7.sp,
                color = Color.Cyan
            )
            val mainAtk = maxOf(fighter.physAttack, fighter.magAttack)
            val mainDef = maxOf(fighter.physDefense, fighter.magDefense)
            Text(
                text = "A:$mainAtk D:$mainDef",
                fontSize = 7.sp,
                color = Color.White
            )
        }
    }
}


@Composable
fun CommanderTacticalConsole(
    commander: Commander,
    energy: Int,
    isBattleActive: Boolean,
    onActivateSkill: (CommanderTacticalSkillType) -> Unit,
    modifier: Modifier = Modifier,
    onAddDemoEnergy: (() -> Unit)? = null,
    lastSkillCast: String? = null
) {
    val skills = remember(commander) { commander.getTacticalSkills() }
    val ultimateSkill = skills.find { it.type == CommanderTacticalSkillType.SIGNATURE_ULTIMATE }
    val rallySkill = skills.find { it.type == CommanderTacticalSkillType.TACTICAL_RALLY }
    val strikeSkill = skills.find { it.type == CommanderTacticalSkillType.PRECISION_STRIKE }

    val isUltimateReady = energy >= 100 && isBattleActive
    val isRallyReady = energy >= 50 && isBattleActive
    val isStrikeReady = energy >= 50 && isBattleActive

    val energyRatio = (energy / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = energyRatio, label = "energy_progress")

    val containerBorder = when {
        isUltimateReady -> BorderStroke(2.dp, Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFF59E0B), Color(0xFF10B981))))
        isRallyReady || isStrikeReady -> BorderStroke(1.5.dp, Color(0xFF38BDF8))
        else -> BorderStroke(1.dp, Color(0xFF334155))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("commander_tactical_console"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = containerBorder,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header with Commander Portrait, Resource Status, and Demo Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val img = commander.imageRes
                    if (img != 0) {
                        Image(
                            painter = painterResource(id = img),
                            contentDescription = commander.name,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("👑", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "COMMANDER TACTICAL DECISION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = commander.name.substringBefore(" ("),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = when {
                            energy >= 100 -> Color(0xFFB45309)
                            energy >= 50 -> Color(0xFF0369A1)
                            else -> Color(0xFF334155)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (energy >= 100) "⚡ ULTIMATE READY!" else "⚡ $energy/100 ENERGY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (energy >= 100) Color(0xFFFFD700) else Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (onAddDemoEnergy != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedButton(
                            onClick = onAddDemoEnergy,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp).testTag("add_demo_energy_btn")
                        ) {
                            Text("+50⚡", fontSize = 8.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Resource Bar (Tactical Energy Meter)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E293B))
            ) {
                val progressBrush = when {
                    energy >= 100 -> Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFFD700), Color(0xFF10B981)))
                    energy >= 50 -> Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF38BDF8)))
                    else -> Brush.horizontalGradient(listOf(Color(0xFF475569), Color(0xFF64748B)))
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(progressBrush)
                )
            }

            if (lastSkillCast != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✨ Last Activated: $lastSkillCast",
                    fontSize = 9.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tactical Decision Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tactical Maneuver 1: Rally (50 Energy)
                rallySkill?.let { skill ->
                    Button(
                        onClick = { onActivateSkill(skill.type) },
                        enabled = isRallyReady,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("activate_rally_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF065F46),
                            disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.7f)
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${skill.icon} Rally [50⚡]",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRallyReady) Color.White else Color.Gray
                            )
                            Text(
                                text = "+45 HP • Cleanse",
                                fontSize = 8.sp,
                                color = if (isRallyReady) Color(0xFFA7F3D0) else Color.DarkGray
                            )
                        }
                    }
                }

                // Tactical Maneuver 2: Precision Strike (50 Energy)
                strikeSkill?.let { skill ->
                    Button(
                        onClick = { onActivateSkill(skill.type) },
                        enabled = isStrikeReady,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("activate_precision_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF831843),
                            disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.7f)
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${skill.icon} Strike [50⚡]",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isStrikeReady) Color.White else Color.Gray
                            )
                            Text(
                                text = "70 DMG • Disarm",
                                fontSize = 8.sp,
                                color = if (isStrikeReady) Color(0xFFFBCFE8) else Color.DarkGray
                            )
                        }
                    }
                }

                // Signature Ultimate (100 Energy)
                ultimateSkill?.let { skill ->
                    Button(
                        onClick = { onActivateSkill(skill.type) },
                        enabled = isUltimateReady,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("activate_ultimate_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB45309),
                            disabledContainerColor = Color(0xFF1E293B).copy(alpha = 0.7f)
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${skill.icon} ${skill.name.take(12)} [100⚡]",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isUltimateReady) Color(0xFFFFD700) else Color.Gray,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isUltimateReady) "READY TO UNLEASH!" else "Requires 100⚡",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUltimateReady) Color.White else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopComponent(
    state: GameState,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Shop Header & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Shop (Lvl ${state.playerLevel})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                Row {
                    val levelCost = CommanderLeveling.getCostToLevelUp(state.playerLevel, state.playerExp)
                    Button(
                        onClick = { viewModel.buyLevelUp() },
                        enabled = state.playerLevel < CommanderLeveling.MAX_LEVEL && state.playerGold >= levelCost && levelCost > 0,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("buy_level_up_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(if (state.playerLevel < CommanderLeveling.MAX_LEVEL) "Lvl Up ($levelCost G)" else "Max Lvl", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = { viewModel.refreshShop() },
                        enabled = state.playerGold >= 2,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("refresh_shop_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Refresh (2 G)", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = { viewModel.toggleShopLock() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (state.shopLocked) Color.Red else MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("toggle_shop_lock_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(if (state.shopLocked) "Unlock" else "Lock", fontSize = 11.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Fighters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                state.shopFighters.forEach { shopFighter ->
                    val benchOccupied = state.playerFighters.count { it.startY == -1 }
                    val tierColor = when (shopFighter.cost) {
                        1 -> Color(0xFFE0E0E0)
                        2 -> Color(0xFFA5D6A7)
                        3 -> Color(0xFF90CAF9)
                        4 -> Color(0xFFCE93D8)
                        5 -> Color(0xFFFFD54F)
                        else -> Color.LightGray
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .fillMaxHeight()
                            .clickable(enabled = state.playerGold >= shopFighter.cost && benchOccupied < 8) {
                                viewModel.buyFighter(shopFighter)
                            },
                        colors = CardDefaults.cardColors(containerColor = tierColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(2.dp).fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = shopFighter.name, 
                                fontSize = 9.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color.Black,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${shopFighter.cost}G • ${shopFighter.fighterClass.displayName.take(6)}", 
                                fontSize = 8.sp, 
                                color = Color(0xFF212121),
                                maxLines = 1
                            )
                            Text(
                                text = shopFighter.faction.displayName.take(6), 
                                fontSize = 8.sp, 
                                color = Color(0xFF424242),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TraitItem(name: String, count: Int, icon: String, isActive: Boolean) {
    val bgColor = if (isActive) Color(0xFFF59E0B) else Color.DarkGray.copy(alpha = 0.5f)
    val textColor = if (isActive) Color.Black else Color.Gray
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 8.sp)
            Spacer(modifier = Modifier.width(2.dp))
            Column {
                Text(
                    text = name,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "$count/2+",
                    fontSize = 6.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
