package com.aetheria.forevercompanion.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aetheria.forevercompanion.ForeverCompanionApplication
import com.aetheria.forevercompanion.R
import com.aetheria.forevercompanion.context.AppContextMonitor
import com.aetheria.forevercompanion.data.local.entities.*
import com.aetheria.forevercompanion.ui.components.PetOverlayView
import com.aetheria.forevercompanion.ui.home.HomeActivity
import com.aetheria.forevercompanion.ui.theme.ForeverCompanionTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * CompanionOverlayService - The heart of the Aetheria Project
 * 
 * This service maintains the always-visible pet companion that floats
 * over all apps, providing emotional support and context-aware interactions.
 * 
 * Key Features:
 * - Android 15 compliant foreground service
 * - Magnetic corner snapping
 * - Auto-miniaturize for heavy apps
 * - Context-aware mood transitions
 * - Battery-optimized rendering
 * - Smooth drag interactions
 */
@AndroidEntryPoint
class CompanionOverlayService : LifecycleService() {

    @Inject
    lateinit var contextMonitor: AppContextMonitor
    
    @Inject
    lateinit var petStateManager: PetStateManager
    
    @Inject
    lateinit var interactionHandler: InteractionHandler
    
    @Inject
    lateinit var renderingOptimizer: RenderingOptimizer

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    
    // State
    private val _isMinimized = MutableStateFlow(false)
    private val _currentMood = MutableStateFlow(PetMood.HAPPY)
    private var lastInteractionTime = System.currentTimeMillis()
    
    // Configuration
    private val magneticZones = listOf(
        MagneticZone(Gravity.TOP or Gravity.START, 20),
        MagneticZone(Gravity.TOP or Gravity.END, 20),
        MagneticZone(Gravity.BOTTOM or Gravity.START, 20),
        MagneticZone(Gravity.BOTTOM or Gravity.END, 20)
    )
    
    private val heavyApps = setOf(
        "com.miHoYo.GenshinImpact",
        "com.roblox.client",
        "com.android.camera2",
        "com.google.android.GoogleCamera",
        "com.android.chrome",
        "com.netflix.mediaclient"
    )

    override fun onCreate() {
        super.onCreate()
        Timber.d("CompanionOverlayService - Creating...")
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Start context monitoring
        contextMonitor.startMonitoring()

        // Start observing context
        observeAppContext()
        observePetState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        Timber.d("CompanionOverlayService - Starting foreground service")
        
        when (intent?.action) {
            ACTION_START_COMPANION -> {
                createOverlayWindow()
                startForeground()
            }
            ACTION_STOP_COMPANION -> {
                stopCompanion()
            }
            ACTION_SHOW_DIALOGUE -> {
                val message = intent.getStringExtra(EXTRA_MESSAGE)
                showDialogue(message)
            }
        }
        
        return START_STICKY
    }

    /**
     * Android 15 Requirement: Show overlay BEFORE starting foreground service
     */
    private fun createOverlayWindow() {
        if (overlayView != null) {
            Timber.w("Overlay already exists, skipping creation")
            return
        }
        
        layoutParams = createWindowLayoutParams()
        
        overlayView = ComposeView(this).apply {
            setContent {
                ForeverCompanionTheme {
                    val petState by petStateManager.currentState.collectAsState()
                    val isMinimized by _isMinimized.collectAsState()
                    val currentMood by _currentMood.collectAsState()
                    
                    PetOverlayContainer(
                        petState = petState,
                        isMinimized = isMinimized,
                        currentMood = currentMood,
                        onTap = ::handlePetTap,
                        onLongPress = ::handlePetLongPress
                    )
                }
            }
        }
        
        // Required for Compose inside WindowManager on Android 13+
        overlayView?.setViewTreeLifecycleOwner(this)
        overlayView?.setViewTreeSavedStateRegistryOwner(this)

        try {
            windowManager.addView(overlayView, layoutParams)
            setupDragListener()
            Timber.d("Overlay window created successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to create overlay window")
        }
    }

    private fun createWindowLayoutParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        
        // Start in bottom-right corner
        params.gravity = Gravity.BOTTOM or Gravity.END
        params.x = 20
        params.y = 100
        
        return params
    }

    /**
     * Setup touch handling for drag-to-move with magnetic snapping
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener() {
        val view = overlayView ?: return
        val params = layoutParams ?: return
        
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        val dragThreshold = 10 // pixels
        
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    
                    // Check if movement exceeds threshold
                    if (!isDragging && (Math.abs(deltaX) > dragThreshold || Math.abs(deltaY) > dragThreshold)) {
                        isDragging = true
                    }
                    
                    if (isDragging) {
                        params.x = initialX + deltaX.toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        
                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to update view layout during drag")
                        }
                    }
                    true
                }
                
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // This was a tap, not a drag
                        handlePetTap()
                    } else {
                        // Snap to nearest magnetic zone
                        snapToNearestZone()
                        recordInteraction("drag")
                    }
                    true
                }
                
                else -> false
            }
        }
    }

    /**
     * Magnetic snapping to corners
     */
    private fun snapToNearestZone() {
        val params = layoutParams ?: return
        val view = overlayView ?: return
        
        val displayMetrics = resources.displayMetrics
        val centerX = params.x + (view.width / 2)
        val centerY = displayMetrics.heightPixels - params.y - (view.height / 2)
        
        // Find closest corner
        val nearestZone = magneticZones.minByOrNull { zone ->
            val zoneX = if (zone.gravity and Gravity.END != 0) displayMetrics.widthPixels else 0
            val zoneY = if (zone.gravity and Gravity.BOTTOM != 0) displayMetrics.heightPixels else 0
            
            val dx = centerX - zoneX
            val dy = centerY - zoneY
            sqrt((dx * dx + dy * dy).toDouble())
        }
        
        nearestZone?.let { zone ->
            lifecycleScope.launch {
                animateToPosition(zone.gravity, zone.offset, zone.offset)
            }
        }
    }

    /**
     * Smooth animation to target position
     */
    private suspend fun animateToPosition(gravity: Int, x: Int, y: Int) = withContext(Dispatchers.Main) {
        val params = layoutParams ?: return@withContext
        val view = overlayView ?: return@withContext
        
        val startGravity = params.gravity
        val startX = params.x
        val startY = params.y
        
        val animationDuration = 300L
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < animationDuration) {
            val progress = ((System.currentTimeMillis() - startTime).toFloat() / animationDuration)
                .coerceIn(0f, 1f)
            
            // Ease out cubic
            val easedProgress = 1f - (1f - progress).pow(3)
            
            params.gravity = gravity
            params.x = (startX + (x - startX) * easedProgress).toInt()
            params.y = (startY + (y - startY) * easedProgress).toInt()
            
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update view during animation")
                break
            }
            
            delay(16) // ~60fps
        }
        
        // Ensure final position is exact
        params.gravity = gravity
        params.x = x
        params.y = y
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set final position")
        }
    }

    /**
     * Context-aware behavior
     */
    private fun observeAppContext() {
        lifecycleScope.launch {
            contextMonitor.currentApp.collect { appPackage ->
                handleAppContextChange(appPackage)
            }
        }
    }

    private suspend fun handleAppContextChange(appPackage: String) {
        Timber.d("App context changed: $appPackage")
        
        // Auto-miniaturize for heavy apps
        if (appPackage in heavyApps) {
            _isMinimized.value = true
            Timber.d("Minimized for heavy app: $appPackage")
        } else {
            _isMinimized.value = false
        }
        
        // Update mood based on app category
        val category = contextMonitor.getAppCategory(appPackage)
        val newMood = mapCategoryToMood(category)
        
        if (_currentMood.value != newMood) {
            transitionToMood(newMood)
        }
    }

    private fun mapCategoryToMood(category: AppCategory): PetMood {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        return when {
            currentHour >= 23 || currentHour < 6 -> PetMood.SLEEPY
            category == AppCategory.PRODUCTIVITY -> PetMood.FOCUS
            category == AppCategory.SOCIAL -> PetMood.EXCITED
            category == AppCategory.GAMING -> PetMood.EXCITED
            else -> PetMood.HAPPY
        }
    }

    private suspend fun transitionToMood(newMood: PetMood) {
        Timber.d("Transitioning mood: ${_currentMood.value} -> $newMood")
        
        // Smooth mood transition
        withContext(Dispatchers.Main) {
            _currentMood.value = newMood
        }
        
        // Record emotional state change
        petStateManager.recordEmotionalState(
            mood = newMood,
            trigger = MoodTrigger.APP_CONTEXT,
            contextAppPackage = contextMonitor.currentApp.value
        )
    }

    /**
     * Pet state observation
     */
    private fun observePetState() {
        lifecycleScope.launch {
            petStateManager.currentState.collect { state ->
                // React to state changes (evolution, accessories, etc.)
                Timber.d("Pet state updated: $state")
            }
        }
    }

    /**
     * Interaction handlers
     */
    private fun handlePetTap() {
        Timber.d("Pet tapped")
        lastInteractionTime = System.currentTimeMillis()
        
        lifecycleScope.launch {
            val dialogue = interactionHandler.handleTap(
                mood = _currentMood.value,
                context = contextMonitor.currentApp.value
            )
            
            showDialogue(dialogue)
            recordInteraction("tap")
        }
    }

    private fun handlePetLongPress() {
        Timber.d("Pet long pressed")
        lastInteractionTime = System.currentTimeMillis()
        
        // Open customization menu
        val intent = Intent(this, com.aetheria.forevercompanion.ui.customization.CustomizationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        
        recordInteraction("long_press")
    }

    private fun showDialogue(message: String?) {
        message ?: return
        
        // Show speech bubble overlay
        // Implementation will be in PetOverlayView composable
        Timber.d("Showing dialogue: $message")
    }

    private fun recordInteraction(type: String) {
        lifecycleScope.launch {
            interactionHandler.recordInteraction(type)
        }
    }

    /**
     * Foreground service setup
     */
    private fun startForeground() {
        val notification = createForegroundNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ForeverCompanionApplication.NOTIFICATION_ID_FOREGROUND,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                ForeverCompanionApplication.NOTIFICATION_ID_FOREGROUND,
                notification
            )
        }
        
        Timber.d("Foreground service started")
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, ForeverCompanionApplication.CHANNEL_COMPANION_PRESENCE)
            .setContentTitle("Your companion is here")
            .setContentText("Tap to open Forever Companion")
            .setSmallIcon(R.drawable.ic_companion_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }

    /**
     * Cleanup
     */
    private fun stopCompanion() {
        Timber.d("Stopping companion overlay")
        
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove overlay view")
            }
        }
        
        overlayView = null
        layoutParams = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("CompanionOverlayService - Destroyed")
        stopCompanion()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        const val ACTION_START_COMPANION = "com.aetheria.forevercompanion.START_COMPANION"
        const val ACTION_STOP_COMPANION = "com.aetheria.forevercompanion.STOP_COMPANION"
        const val ACTION_SHOW_DIALOGUE = "com.aetheria.forevercompanion.SHOW_DIALOGUE"
        const val EXTRA_MESSAGE = "message"
    }
}

/**
 * Data class for magnetic zones
 */
data class MagneticZone(
    val gravity: Int,
    val offset: Int
)

/**
 * Composable for the pet overlay container
 */
@Composable
private fun PetOverlayContainer(
    petState: PetState,
    isMinimized: Boolean,
    currentMood: PetMood,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (isMinimized) 40.dp else 100.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        PetOverlayView(
            petState = petState,
            isMinimized = isMinimized,
            currentMood = currentMood,
            onTap = onTap,
            onLongPress = onLongPress
        )
    }
}
