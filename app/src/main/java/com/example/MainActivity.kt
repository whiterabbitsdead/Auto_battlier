package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.BattleBoardScreen
import com.example.game.BattleVisualizationScreen
import com.example.game.CommanderSelectionScreen
import com.example.game.GameOverScreen
import com.example.game.GameViewModel
import com.example.game.Screen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          GameApp(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun GameApp(modifier: Modifier = Modifier) {
    val viewModel: GameViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (state.currentScreen) {
            Screen.COMMANDER_SELECTION -> CommanderSelectionScreen(viewModel)
            Screen.BATTLE_BOARD -> BattleBoardScreen(viewModel)
            Screen.BATTLE_VISUALIZATION -> {
                if (state.isAdmin) {
                    BattleVisualizationScreen(viewModel)
                } else {
                    BattleBoardScreen(viewModel)
                }
            }
            Screen.GAME_OVER -> GameOverScreen(viewModel)
        }
    }
}

