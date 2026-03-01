package com.aetheria.forevercompanion.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetheria.forevercompanion.overlay.CompanionOverlayService
import com.aetheria.forevercompanion.ui.theme.ForeverCompanionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the overlay service
        startCompanionService()

        setContent {
            ForeverCompanionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun startCompanionService() {
        val intent = Intent(this, CompanionOverlayService::class.java).apply {
            action = CompanionOverlayService.ACTION_START_COMPANION
        }
        startForegroundService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Service continues running — don't stop it here
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val petState by viewModel.petState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Forever Companion") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Bond") },
                    label = { Text("Bond") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Shop") },
                    label = { Text("Shop") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (selectedTab) {
                0 -> CompanionDashboard(petState = petState)
                1 -> BondScreen(petState = petState)
                2 -> ShopScreen()
                3 -> SettingsScreen()
            }
        }
    }
}

@Composable
fun CompanionDashboard(petState: com.aetheria.forevercompanion.overlay.PetState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Text("✨", fontSize = 80.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            petState.companion?.name ?: "Your Companion",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Evolution: ${petState.evolutionStage}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Mood: ${petState.currentMood}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        petState.bondProgress?.let { bond ->
            Text("Bond Level ${bond.currentLevel}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { bond.currentXP.toFloat() / bond.xpToNextLevel.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Text("${bond.currentXP} / ${bond.xpToNextLevel} XP",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun BondScreen(petState: com.aetheria.forevercompanion.overlay.PetState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Text("Bond & Evolution", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Days Together: ${petState.bondProgress?.daysTogether ?: 0}")
        Text("Relationship: ${petState.bondProgress?.relationshipPhase}")
        Text("Evolution Path: ${petState.bondProgress?.evolutionPath}")
    }
}

@Composable
fun ShopScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text("Shop — Coming Soon ✨", fontSize = 18.sp)
    }
}

@Composable
fun SettingsScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text("Settings — Coming Soon ⚙️", fontSize = 18.sp)
    }
}
