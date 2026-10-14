package com.example.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MockBattleScreen(onBack: () -> Unit) {
    val templates = remember { mutableStateListOf<FighterTemplate>().apply { addAll(presetTemplates) } }
    val left = remember { mutableStateMapOf<String, Int>("Chief Kong" to 1) }
    val right = remember { mutableStateMapOf<String, Int>("Mike Tyson" to 3) }
    var result by remember { mutableStateOf<MockBattleResult?>(null) }
    var odds by remember { mutableStateOf<MockBattleOdds?>(null) }

    fun army(name: String, m: Map<String, Int>) = MockArmy(
        name, templates.filter { (m[it.name] ?: 0) > 0 }.map { ArmyEntry(it, m[it.name] ?: 0) }
    )

    var nName by remember { mutableStateOf("") }
    var nHp by remember { mutableStateOf("150") }
    var nAtk by remember { mutableStateOf("20") }
    var nDef by remember { mutableStateOf("10") }
    var nSpd by remember { mutableStateOf("10") }
    var nRange by remember { mutableStateOf("1") }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Back") }
                Text("Mock Battle", style = MaterialTheme.typography.headlineMedium)
            }
            Text("Pick any number of each fighter per side. Left vs Right.")
        }
        items(templates.toList()) { t ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    Text("${t.name}  HP ${t.hp} ATK ${t.attack} DEF ${t.defense} SPD ${t.speed} RNG ${t.range}")
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Left: ${left[t.name] ?: 0}")
                        TextButton(onClick = { left[t.name] = maxOf(0, (left[t.name] ?: 0) - 1) }) { Text("-") }
                        TextButton(onClick = { left[t.name] = (left[t.name] ?: 0) + 1 }) { Text("+") }
                        TextButton(onClick = { left[t.name] = (left[t.name] ?: 0) + 10 }) { Text("+10") }
                        Spacer(Modifier.width(16.dp))
                        Text("Right: ${right[t.name] ?: 0}")
                        TextButton(onClick = { right[t.name] = maxOf(0, (right[t.name] ?: 0) - 1) }) { Text("-") }
                        TextButton(onClick = { right[t.name] = (right[t.name] ?: 0) + 1 }) { Text("+") }
                        TextButton(onClick = { right[t.name] = (right[t.name] ?: 0) + 10 }) { Text("+10") }
                    }
                }
            }
        }
        item {
            Text("Create a fighter", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(nName, { nName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("HP" to (nHp to { v: String -> nHp = v }), "ATK" to (nAtk to { v: String -> nAtk = v }),
                    "DEF" to (nDef to { v: String -> nDef = v }), "SPD" to (nSpd to { v: String -> nSpd = v }),
                    "RNG" to (nRange to { v: String -> nRange = v })).forEach { (label, p) ->
                    OutlinedTextField(p.first, { p.second(it.filter(Char::isDigit).take(5)) },
                        label = { Text(label) }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            Button(
                enabled = nName.isNotBlank() && templates.none { it.name.equals(nName.trim(), true) },
                onClick = {
                    templates.add(FighterTemplate(nName.trim(), nHp.toIntOrNull() ?: 100, nAtk.toIntOrNull() ?: 10,
                        nDef.toIntOrNull() ?: 5, nSpd.toIntOrNull() ?: 10, (nRange.toIntOrNull() ?: 1).coerceAtLeast(1)))
                    nName = ""
                }
            ) { Text("Add fighter") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    result = MockBattleSimulator.simulate(army("Left", left), army("Right", right))
                    odds = null
                }) { Text("Fight!") }
                OutlinedButton(onClick = {
                    odds = MockBattleSimulator.simulateOdds(army("Left", left), army("Right", right), 100)
                }) { Text("Odds (100 runs)") }
            }
            odds?.let { Text("Left wins ${it.leftWins}, Right wins ${it.rightWins}, draws ${it.draws}") }
            result?.let {
                Text("Winner: ${it.winner} • ${it.rounds} rounds • survivors L${it.leftSurvivors}/R${it.rightSurvivors}")
            }
        }
        result?.let { r -> items(r.log) { Text(it, style = MaterialTheme.typography.bodySmall) } }
    }
}
