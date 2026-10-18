package com.ranzan.morphebuilds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.ranzan.morphebuilds.ui.home.HomeScreen
import com.ranzan.morphebuilds.ui.home.HomeViewModel
import com.ranzan.morphebuilds.ui.settings.SettingsScreen
import com.ranzan.morphebuilds.ui.settings.SettingsViewModel
import com.ranzan.morphebuilds.ui.theme.MorpheBuildsTheme
import com.ranzan.morphebuilds.ui.theme.PureBlack
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MorpheBuildsTheme {
                val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PureBlack)
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        entryProvider = entryProvider {
                            entry<Screen.Home> {
                                val homeViewModel: HomeViewModel = koinViewModel()
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onNavigateToSettings = {
                                        backStack.add(Screen.Settings)
                                    }
                                )
                            }
                            entry<Screen.Settings> {
                                val settingsViewModel: SettingsViewModel = koinViewModel()
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onNavigateBack = {
                                        if (backStack.size > 1) {
                                            backStack.removeAt(backStack.lastIndex)
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}