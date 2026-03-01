package com.aetheria.forevercompanion.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetheria.forevercompanion.data.local.entities.EvolutionStage
import com.aetheria.forevercompanion.data.local.entities.PetMood
import com.aetheria.forevercompanion.overlay.PetState
import kotlinx.coroutines.delay

@Composable
fun PetOverlayView(
    petState: PetState,
    isMinimized: Boolean,
    currentMood: PetMood,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    // Breathing / idle animation
    val infiniteTransition = rememberInfiniteTransition(label = "idle")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Speech bubble state
    var showBubble by remember { mutableStateOf(false) }
    var bubbleText by remember { mutableStateOf("") }

    val petEmoji = when (petState.evolutionStage) {
        EvolutionStage.WISP -> "🔮"
        EvolutionStage.KIT -> "🦊"
        EvolutionStage.ASTRAL_SENTINEL -> "✨🦊✨"
    }

    val moodColor = when (currentMood) {
        PetMood.HAPPY -> Color(0xFF7B2FBE)
        PetMood.FOCUS -> Color(0xFF1565C0)
        PetMood.EXCITED -> Color(0xFFE91E63)
        PetMood.SLEEPY -> Color(0xFF37474F)
        PetMood.PROTECTIVE -> Color(0xFF2E7D32)
        PetMood.CURIOUS -> Color(0xFFF57F17)
        PetMood.PROUD -> Color(0xFFFFD700)
        PetMood.CONCERNED -> Color(0xFF6A1B9A)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.wrapContentSize()
    ) {
        // Speech bubble
        if (showBubble && bubbleText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .offset(y = (-60).dp)
                    .background(
                        color = Color.White.copy(alpha = 0.95f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = bubbleText,
                    fontSize = 12.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 3
                )
            }
        }

        // Pet avatar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (isMinimized) 40.dp else 80.dp)
                .scale(breathScale)
                .clip(CircleShape)
                .background(moodColor.copy(alpha = 0.85f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { onLongPress() }
                    )
                }
        ) {
            Text(
                text = if (isMinimized) "🔮" else petEmoji,
                fontSize = if (isMinimized) 20.sp else 36.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
