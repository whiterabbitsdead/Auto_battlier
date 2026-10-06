package com.example.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Filter categories for the combat action log.
 */
enum class LogFilterType {
    ALL,
    COMMANDER,
    ABILITIES,
    CRITICALS,
    DEFEATS
}

/**
 * High-performance combat action log section with:
 * 1. An auto-scrolling mechanism that scrolls smoothly to new incoming events.
 * 2. A limited history buffer (capped at [maxBufferCapacity]) to prevent memory leaks and UI lag.
 * 3. User toggle for auto-scroll and a "Jump to Latest" button when scrolled away.
 * 4. Rich semantic tags and visual categorization (Abilities, Heals, Combos, Defeats, Crits).
 */
@Composable
fun CombatLogSection(
    logs: List<String>,
    modifier: Modifier = Modifier,
    selectedFilter: LogFilterType = LogFilterType.ALL,
    onFilterSelected: ((LogFilterType) -> Unit)? = null,
    maxBufferCapacity: Int = MAX_COMBAT_LOG_HISTORY,
    title: String = "📜 COMBAT ACTION LOG",
    showFilters: Boolean = true,
    onClearLogs: (() -> Unit)? = null
) {
    var internalFilter by remember { mutableStateOf(selectedFilter) }
    val currentFilter = onFilterSelected?.let { selectedFilter } ?: internalFilter
    val handleFilterChange: (LogFilterType) -> Unit = { filter ->
        onFilterSelected?.invoke(filter) ?: run { internalFilter = filter }
    }

    var isAutoScrollEnabled by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Enforce history buffer limit to prevent unbounded memory growth and recomposition lag
    val bufferedLogs = remember(logs, maxBufferCapacity) {
        logs.take(maxBufferCapacity)
    }

    val filteredLogs = remember(bufferedLogs, currentFilter) {
        bufferedLogs.filter { log ->
            when (currentFilter) {
                LogFilterType.ALL -> true
                LogFilterType.COMMANDER -> log.contains("Commander") || log.contains("📢") ||
                        log.contains("🔥") || log.contains("Support") || log.contains("Strike")
                LogFilterType.ABILITIES -> log.contains("casts") || log.contains("unleashes") ||
                        log.contains("✨") || log.contains("activates") || log.contains("💚")
                LogFilterType.CRITICALS -> log.contains("CRITICALLY") || log.contains("💥") || log.contains("Combo")
                LogFilterType.DEFEATS -> log.contains("defeated") || log.contains("💀") ||
                        log.contains("Victory") || log.contains("Defeat")
            }
        }
    }

    // Auto-scroll mechanism: automatically animate to the newest combat action when new logs arrive
    LaunchedEffect(filteredLogs.size, isAutoScrollEnabled) {
        if (isAutoScrollEnabled && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // Detect if user has scrolled away from the newest log
    val isScrolledAwayFromLatest by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40
        }
    }

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
            // Header Row: Title, Buffer Indicator, Auto-Scroll Toggle, Clear Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )

                    // Buffer Status Indicator Pill
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag("combat_log_buffer_indicator")
                    ) {
                        Text(
                            text = "${bufferedLogs.size}/$maxBufferCapacity",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bufferedLogs.size >= maxBufferCapacity) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Auto-Scroll Toggle Chip
                    Surface(
                        onClick = {
                            isAutoScrollEnabled = !isAutoScrollEnabled
                            if (isAutoScrollEnabled && filteredLogs.isNotEmpty()) {
                                coroutineScope.launch { listState.animateScrollToItem(0) }
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isAutoScrollEnabled) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF334155),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAutoScrollEnabled) Color(0xFF38BDF8) else Color(0xFF475569)
                        ),
                        modifier = Modifier
                            .height(26.dp)
                            .testTag("combat_log_autoscroll_toggle")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = if (isAutoScrollEnabled) "⚡ Auto" else "⏸️ Pause",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAutoScrollEnabled) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Optional Clear Logs Action
                    if (onClearLogs != null) {
                        Surface(
                            onClick = onClearLogs,
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF334155),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("combat_log_clear_button")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Text("🗑️", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // Filter Chips Row (All, Abilities, Defeats)
            if (showFilters) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = currentFilter == LogFilterType.ALL,
                        onClick = { handleFilterChange(LogFilterType.ALL) },
                        label = { Text("All", fontSize = 9.sp) },
                        modifier = Modifier.height(26.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = currentFilter == LogFilterType.COMMANDER,
                        onClick = { handleFilterChange(LogFilterType.COMMANDER) },
                        label = { Text("Commander 👑", fontSize = 9.sp) },
                        modifier = Modifier.height(26.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = currentFilter == LogFilterType.ABILITIES,
                        onClick = { handleFilterChange(LogFilterType.ABILITIES) },
                        label = { Text("Abilities ✨", fontSize = 9.sp) },
                        modifier = Modifier.height(26.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = currentFilter == LogFilterType.DEFEATS,
                        onClick = { handleFilterChange(LogFilterType.DEFEATS) },
                        label = { Text("Defeats 💀", fontSize = 9.sp) },
                        modifier = Modifier.height(26.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Log List with Jump to Latest Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
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
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("combat_actions_log_list"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(filteredLogs) { index, logMessage ->
                            CombatLogItem(
                                message = logMessage,
                                isLatest = index == 0,
                                modifier = Modifier.testTag("combat_log_item_$index")
                            )
                        }
                    }
                }

                // "Jump to Latest" Floating Action Button when user has scrolled away
                androidx.compose.animation.AnimatedVisibility(
                    visible = isScrolledAwayFromLatest && filteredLogs.isNotEmpty(),
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                ) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                                isAutoScrollEnabled = true
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0284C7),
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.testTag("jump_to_latest_log_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("⬇", fontSize = 10.sp, color = Color.White)
                            Text(
                                text = "Latest Actions",
                                fontSize = 9.sp,
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
 * Individual action item entry within the combat log with semantic coloring and typography.
 */
@Composable
fun CombatLogItem(
    message: String,
    modifier: Modifier = Modifier,
    isLatest: Boolean = false
) {
    val (icon, tintColor, bgTint) = when {
        message.contains("💚") || message.contains("healing") || message.contains("healed") ->
            Triple("💚", Color(0xFF34D399), Color(0xFF065F46).copy(alpha = 0.25f))
        message.contains("✨") || message.contains("casts") || message.contains("unleashes") ->
            Triple("✨", Color(0xFF38BDF8), Color(0xFF0369A1).copy(alpha = 0.2f))
        message.contains("CRITICALLY") || message.contains("💥") || message.contains("Combo") ->
            Triple("💥", Color(0xFFFBBF24), Color(0xFFB45309).copy(alpha = 0.2f))
        message.contains("💀") || message.contains("defeated") ->
            Triple("💀", Color(0xFFEF4444), Color(0xFF7F1D1D).copy(alpha = 0.25f))
        message.contains("🛡️") || message.contains("barrier") || message.contains("reflects") ->
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
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgTint)
            .then(
                if (isLatest) Modifier.border(1.dp, tintColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 11.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = message,
            fontSize = 10.sp,
            color = tintColor,
            fontWeight = if (isLatest) FontWeight.SemiBold else FontWeight.Medium,
            lineHeight = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
