package com.aetheria.forevercompanion.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetheria.forevercompanion.permissions.OverlayPermissionManager
import com.aetheria.forevercompanion.ui.home.HomeActivity
import com.aetheria.forevercompanion.ui.theme.ForeverCompanionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {

    private val viewModel: OnboardingViewModel by viewModels()
    private lateinit var permissionManager: OverlayPermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = OverlayPermissionManager(this)

        setContent {
            ForeverCompanionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OnboardingScreen(
                        onComplete = { companionName ->
                            viewModel.createCompanion(companionName)
                            requestOverlayPermission()
                        }
                    )
                }
            }
        }
    }

    private fun requestOverlayPermission() {
        permissionManager.requestPermission {
            // Permission granted — go to home
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}

@Composable
fun OnboardingScreen(onComplete: (String) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var companionName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (step) {
            0 -> WelcomeStep(onNext = { step = 1 })
            1 -> NameStep(
                name = companionName,
                onNameChange = { companionName = it },
                onNext = { if (companionName.isNotBlank()) step = 2 }
            )
            2 -> PermissionStep(onComplete = { onComplete(companionName) })
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Text("✨", fontSize = 80.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    Text(
        "Meet Your Forever Companion",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "A little friend who lives on your screen — always there, always learning, always yours.",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text("Let's Begin")
    }
}

@Composable
fun NameStep(name: String, onNameChange: (String) -> Unit, onNext: () -> Unit) {
    Text("🦊", fontSize = 80.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    Text("Name Your Companion", fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Companion Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onNext,
        enabled = name.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Continue")
    }
}

@Composable
fun PermissionStep(onComplete: () -> Unit) {
    Text("🪟", fontSize = 80.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    Text("The Window Key", fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Text(
        "To stay by your side across all apps, your companion needs permission to float above other windows. We'll ask for it next.",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
        Text("Grant the Window Key")
    }
}
