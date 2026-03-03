package com.aetheria.forevercompanion.ui.customization

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.aetheria.forevercompanion.ui.theme.ForeverCompanionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomizationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ForeverCompanionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("Customize Your Companion 🎨", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
