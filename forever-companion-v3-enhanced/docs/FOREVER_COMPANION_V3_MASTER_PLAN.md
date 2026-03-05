# Forever Companion V3: "The Aetheria Project"
## Master Implementation Plan & Technical Roadmap

**Project Vision:** Build an Operating System for Friendship—a truly "Hybrid" AI companion that exists between your phone's digital world and your real life.

---

## 🎯 Project Status & Foundation

### Current Implementation (Based on Uploaded Files)
✅ **Established Architecture:**
- Hybrid Native Android + Unity 3D system
- UnityBridge with delta messaging (MOOD_DELTA, ANIM_TRIGGER, AVATAR_UPDATE, LOOK_AT)
- Room database for emotional states and conversations
- Overlay permission handling with user education
- Firebase integration structure

✅ **Core Technical Stack:**
- Language: Kotlin (Native), C# (Unity)
- Database: Room (Local), Firebase (Cloud)
- 3D Engine: Unity with AAR export workflow
- Bridge: Native-first with visual deltas to Unity

---

## 📋 Phase 1: The Core Foundation (Months 1-2)
**Focus: System Overlay Mastery & Hybrid AI Integration**

### 1.1 Enhanced System Overlay Engine
**Status:** Foundation exists, needs V3 enhancements

#### Required Enhancements:
```kotlin
// /android-native/app/src/main/java/com/forevercompanion/overlay/CompanionOverlayService.kt

@AndroidEntryPoint
class CompanionOverlayService : Service() {
    
    companion object {
        const val FOREGROUND_SERVICE_TYPE = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    }
    
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: ComposeView
    private var petRenderer: PetRenderer? = null
    
    // V3: Magnetic Corner Logic
    private val magneticZones = listOf(
        MagneticZone(Gravity.TOP or Gravity.START, 100),
        MagneticZone(Gravity.TOP or Gravity.END, 100),
        MagneticZone(Gravity.BOTTOM or Gravity.START, 100),
        MagneticZone(Gravity.BOTTOM or Gravity.END, 100)
    )
    
    // V3: Auto-Miniaturize for Heavy Apps
    private val heavyApps = setOf(
        "com.miHoYo.GenshinImpact",
        "com.android.camera2",
        // Add more heavy apps
    )
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 15 requirement: Show overlay BEFORE starting foreground
        createOverlayWindow()
        
        val notification = createForegroundNotification()
        startForeground(
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FOREGROUND_SERVICE_TYPE
            } else 0
        )
        
        startContextMonitoring()
        return START_STICKY
    }
    
    private fun createOverlayWindow() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, // V3: GPU acceleration
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.BOTTOM or Gravity.END
        params.x = 50
        params.y = 50
        
        overlayView = ComposeView(this).apply {
            setContent {
                ForeverCompanionTheme {
                    PetOverlayContent(
                        petState = petViewModel.currentState.collectAsState(),
                        onDrag = ::handleDrag,
                        onTap = ::handleTap
                    )
                }
            }
        }
        
        windowManager.addView(overlayView, params)
        
        // V3: Setup drag listener with magnetic snapping
        setupDragListener(overlayView, params)
    }
    
    private fun setupDragListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (initialTouchY - event.rawY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Snap to nearest magnetic zone
                    snapToNearestZone(params)
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun snapToNearestZone(params: WindowManager.LayoutParams) {
        val displayMetrics = resources.displayMetrics
        val centerX = params.x + (overlayView.width / 2)
        val centerY = params.y + (overlayView.height / 2)
        
        // Find closest corner
        val nearestZone = magneticZones.minByOrNull { zone ->
            val zoneX = if (zone.gravity and Gravity.END != 0) displayMetrics.widthPixels else 0
            val zoneY = if (zone.gravity and Gravity.BOTTOM != 0) displayMetrics.heightPixels else 0
            
            val dx = centerX - zoneX
            val dy = centerY - zoneY
            sqrt((dx * dx + dy * dy).toDouble())
        }
        
        nearestZone?.let { zone ->
            params.gravity = zone.gravity
            params.x = zone.offset
            params.y = zone.offset
        }
    }
    
    // V3: Context-aware rendering
    private fun startContextMonitoring() {
        lifecycleScope.launch {
            contextMonitor.currentApp.collect { appPackage ->
                handleAppContext(appPackage)
            }
        }
    }
    
    private suspend fun handleAppContext(appPackage: String) {
        when {
            appPackage in heavyApps -> {
                // Miniaturize to notification bubble
                withContext(Dispatchers.Main) {
                    petRenderer?.setState(PetState.MINIMIZED)
                }
            }
            isProductivityApp(appPackage) -> {
                // Switch to Focus Mode
                petRenderer?.transitionToMood(PetMood.FOCUS)
            }
            isSocialApp(appPackage) -> {
                // Switch to Socialite Mode
                petRenderer?.transitionToMood(PetMood.SOCIALITE)
            }
        }
    }
}
```

### 1.2 On-Device AI Setup (Local Intelligence)
**New Implementation Required**

#### Lightweight Local Model Integration:
```kotlin
// /android-native/app/src/main/java/com/forevercompanion/ai/LocalAIEngine.kt

@Singleton
class LocalAIEngine @Inject constructor(
    private val context: Context
) {
    private lateinit var interpreter: Interpreter
    private val modelMutex = Mutex()
    
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            // Load TFLite model for quick responses
            val modelFile = loadModelFile("companion_nano_v1.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(2)
                setUseNNAPI(true) // Use Android Neural Networks API
            }
            interpreter = Interpreter(modelFile, options)
        } catch (e: Exception) {
            Log.e("LocalAI", "Failed to initialize: ${e.message}")
        }
    }
    
    suspend fun getQuickResponse(
        context: String,
        userInput: String,
        emotionalState: EmotionalState
    ): LocalResponse = withContext(Dispatchers.Default) {
        modelMutex.withLock {
            try {
                // Tokenize and prepare input
                val inputTokens = tokenize(userInput)
                val contextEmbedding = encodeContext(context, emotionalState)
                
                // Run inference
                val output = runInference(inputTokens, contextEmbedding)
                
                LocalResponse(
                    text = decodeOutput(output),
                    confidence = output.confidence,
                    requiresCloudHandoff = output.confidence < 0.7f
                )
            } catch (e: Exception) {
                LocalResponse(
                    text = null,
                    confidence = 0f,
                    requiresCloudHandoff = true
                )
            }
        }
    }
    
    private fun loadModelFile(filename: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}

data class LocalResponse(
    val text: String?,
    val confidence: Float,
    val requiresCloudHandoff: Boolean
)
```

### 1.3 Cloud AI Handshake (Hybrid Intelligence)
```kotlin
// /android-native/app/src/main/java/com/forevercompanion/ai/HybridAIController.kt

@Singleton
class HybridAIController @Inject constructor(
    private val localAI: LocalAIEngine,
    private val cloudAI: CloudAIService,
    private val conversationRepo: ConversationRepository
) {
    
    suspend fun processUserMessage(
        message: String,
        context: AppContext
    ): CompanionResponse = withContext(Dispatchers.IO) {
        
        // Step 1: Try local model first
        val localResponse = localAI.getQuickResponse(
            context = context.toString(),
            userInput = message,
            emotionalState = context.currentMoodState
        )
        
        // Step 2: Decide if cloud is needed
        if (localResponse.confidence >= 0.7f && localResponse.text != null) {
            // Local model confident, use it
            return@withContext CompanionResponse(
                text = localResponse.text,
                mood = inferMoodFromResponse(localResponse),
                source = ResponseSource.LOCAL,
                processingTime = localResponse.processingTime
            )
        }
        
        // Step 3: Hand off to cloud for complex query
        val cloudResponse = cloudAI.getResponse(
            conversationHistory = conversationRepo.getRecentMessages(20),
            currentMessage = message,
            context = context
        )
        
        return@withContext CompanionResponse(
            text = cloudResponse.text,
            mood = cloudResponse.mood,
            source = ResponseSource.CLOUD,
            processingTime = cloudResponse.processingTime
        )
    }
    
    private fun inferMoodFromResponse(response: LocalResponse): PetMood {
        // Simple sentiment analysis on response
        return when {
            response.text?.contains("excited", ignoreCase = true) == true -> PetMood.EXCITED
            response.text?.contains("focused", ignoreCase = true) == true -> PetMood.FOCUS
            else -> PetMood.NEUTRAL
        }
    }
}
```

---

## 📋 Phase 2: Living Ecosystem (Month 3)
**Focus: 3D Rendering, Procedural Animation & Species Logic**

### 2.1 Enhanced 3D Pet Models
**Unity Implementation**

#### Procedural Animation System:
```csharp
// /unity-module/Assets/Scripts/Pet/ProceduralAnimator.cs

using UnityEngine;
using System.Collections;

namespace ForeverCompanion.Pet
{
    public class ProceduralAnimator : MonoBehaviour
    {
        [Header("Nebula Kit Settings")]
        public SkinnedMeshRenderer petRenderer;
        public Transform[] tailSegments;
        public ParticleSystem nebulaParticles;
        public Material nebulaMaterial;
        
        [Header("Performance")]
        public int targetFrameRate = 60;
        public int idleFrameRate = 1;
        private int currentFrameRate;
        
        private PetState currentState = PetState.IDLE;
        private float lastActivity = 0f;
        private const float IDLE_THRESHOLD = 5f; // seconds
        
        void Start()
        {
            Application.targetFrameRate = targetFrameRate;
            StartCoroutine(AdaptiveFrameRateController());
        }
        
        void Update()
        {
            // Update idle timer
            if (Input.touchCount > 0 || Input.GetMouseButton(0))
            {
                lastActivity = Time.time;
                if (currentFrameRate != targetFrameRate)
                {
                    Application.targetFrameRate = targetFrameRate;
                    currentFrameRate = targetFrameRate;
                }
            }
            
            // Procedural tail physics
            UpdateTailPhysics();
            
            // Nebula shader effects
            UpdateNebulaShader();
        }
        
        IEnumerator AdaptiveFrameRateController()
        {
            while (true)
            {
                yield return new WaitForSeconds(1f);
                
                if (Time.time - lastActivity > IDLE_THRESHOLD && currentState == PetState.IDLE)
                {
                    // Drop to 1fps when truly idle
                    Application.targetFrameRate = idleFrameRate;
                    currentFrameRate = idleFrameRate;
                }
            }
        }
        
        void UpdateTailPhysics()
        {
            // Procedural tail swaying using inverse kinematics
            for (int i = 0; i < tailSegments.Length; i++)
            {
                float wave = Mathf.Sin(Time.time * 2f + i * 0.5f) * 0.1f;
                tailSegments[i].localRotation = Quaternion.Euler(0, 0, wave * 15f);
            }
        }
        
        void UpdateNebulaShader()
        {
            // Animate the "star constellation" effect
            float shimmer = Mathf.Sin(Time.time * 0.5f) * 0.5f + 0.5f;
            nebulaMaterial.SetFloat("_ShimmerIntensity", shimmer);
            
            // Adjust particle system based on mood
            var emission = nebulaParticles.emission;
            emission.rateOverTime = currentState == PetState.EXCITED ? 50f : 10f;
        }
        
        public void TransitionToMood(string moodName, float intensity)
        {
            StartCoroutine(SmoothMoodTransition(moodName, intensity, 2f));
        }
        
        IEnumerator SmoothMoodTransition(string targetMood, float intensity, float duration)
        {
            Color startColor = nebulaMaterial.GetColor("_BaseColor");
            Color targetColor = GetMoodColor(targetMood);
            
            float elapsed = 0f;
            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float t = elapsed / duration;
                
                nebulaMaterial.SetColor("_BaseColor", Color.Lerp(startColor, targetColor, t));
                
                yield return null;
            }
        }
        
        Color GetMoodColor(string mood)
        {
            return mood switch
            {
                "FOCUS" => new Color(0.2f, 0.4f, 0.8f, 0.7f), // Soft Blue
                "SOCIALITE" => new Color(0.9f, 0.8f, 0.2f, 0.8f), // Vibrant Yellow
                "HYPE" => new Color(1f, 0.4f, 0.1f, 1f), // Neon Orange
                "INVESTIGATOR" => new Color(0.1f, 0.7f, 0.7f, 0.7f), // Teal
                "PROTECTIVE" => new Color(0.4f, 0.2f, 0.6f, 0.6f), // Deep Purple
                _ => new Color(0.5f, 0.5f, 0.5f, 0.7f) // Neutral
            };
        }
    }
    
    public enum PetState
    {
        IDLE,
        ACTIVE,
        EXCITED,
        MINIMIZED
    }
}
```

### 2.2 Cross-App Context Engine
```kotlin
// /android-native/app/src/main/java/com/forevercompanion/context/AppContextMonitor.kt

@Singleton
class AppContextMonitor @Inject constructor(
    private val context: Context,
    @ApplicationContext private val appContext: Context
) {
    
    private val _currentApp = MutableStateFlow<String>("")
    val currentApp: StateFlow<String> = _currentApp.asStateFlow()
    
    private val _appCategory = MutableStateFlow<AppCategory>(AppCategory.UNKNOWN)
    val appCategory: StateFlow<AppCategory> = _appCategory.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // App categorization map
    private val appCategories = mapOf(
        "com.linkedin.android" to AppCategory.PRODUCTIVITY,
        "com.google.android.apps.docs" to AppCategory.PRODUCTIVITY,
        "com.microsoft.office.outlook" to AppCategory.PRODUCTIVITY,
        
        "com.instagram.android" to AppCategory.SOCIAL,
        "com.zhiliaoapp.musically" to AppCategory.SOCIAL, // TikTok
        "com.snapchat.android" to AppCategory.SOCIAL,
        
        "com.miHoYo.GenshinImpact" to AppCategory.GAMING,
        "com.roblox.client" to AppCategory.GAMING,
        
        "com.android.settings" to AppCategory.UTILITY,
        "com.google.android.apps.photos" to AppCategory.UTILITY
    )
    
    fun startMonitoring() {
        scope.launch {
            while (isActive) {
                updateCurrentApp()
                delay(1000) // Check every second
            }
        }
    }
    
    @SuppressLint("WrongConstant")
    private fun updateCurrentApp() {
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()
            
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 60000, // Last minute
                currentTime
            )
            
            val recentApp = stats.maxByOrNull { it.lastTimeUsed }
            recentApp?.packageName?.let { packageName ->
                if (_currentApp.value != packageName) {
                    _currentApp.value = packageName
                    _appCategory.value = appCategories[packageName] ?: AppCategory.UNKNOWN
                }
            }
        } catch (e: Exception) {
            Log.e("AppContextMonitor", "Error monitoring apps: ${e.message}")
        }
    }
    
    fun stopMonitoring() {
        scope.cancel()
    }
}

enum class AppCategory {
    PRODUCTIVITY,
    SOCIAL,
    GAMING,
    UTILITY,
    UNKNOWN
}
```

---

## 📋 Phase 3: Economy & Progression (Month 4)
**Focus: Accessory System, Dual Currency, Evolution**

### 3.1 Accessory System Architecture
```kotlin
// /android-native/app/src/main/java/com/forevercompanion/accessories/AccessorySystem.kt

@Entity(tableName = "accessories")
data class Accessory(
    @PrimaryKey val id: String,
    val name: String,
    val category: AccessoryCategory,
    val rarity: AccessoryRarity,
    val description: String,
    val priceAetherShards: Int,
    val priceStarCredits: Int?,
    val unityModelId: String, // References Unity prefab
    val unlockCondition: UnlockCondition?,
    val behaviorModifier: BehaviorModifier?
)

enum class AccessoryCategory {
    FUNCTIONAL,    // Starry Night Lantern
    STYLE,         // Digital Beret
    ACHIEVEMENT,   // Hero's Cape
    INTERACTION,   // Tiny Headphones
    HYBRID         // Explorer's Compass
}

enum class AccessoryRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}

data class UnlockCondition(
    val type: UnlockType,
    val requirement: Int
)

enum class UnlockType {
    PRODUCTIVITY_STREAK,  // Hero's Cape: 50 streaks
    BOND_LEVEL,           // Unlocked at certain bond levels
    TIME_WITH_PET,        // Cumulative hours
    SPECIAL_EVENT         // Limited time
}

data class BehaviorModifier(
    val moodUnlock: String?,           // "Philosophical Mode"
    val contextTrigger: String?,       // Reacts to Spotify
    val dialogueVariant: String?,      // New dialogue trees
    val animationSet: String?          // New animations
)

@Dao
interface AccessoryDao {
    @Query("SELECT * FROM accessories WHERE id IN (:equippedIds)")
    fun getEquippedAccessories(equippedIds: List<String>): Flow<List<Accessory>>
    
    @Query("SELECT * FROM accessories WHERE category = :category")
    suspend fun getByCategory(category: AccessoryCategory): List<Accessory>
    
    @Query("SELECT * FROM accessories")
    fun getAllAccessories(): Flow<List<Accessory>>
}

@Singleton
class AccessoryManager @Inject constructor(
    private val accessoryDao: AccessoryDao,
    private val currencyManager: CurrencyManager,
    private val unityBridge: UnityBridge
) {
    
    private val _equippedAccessories = MutableStateFlow<List<Accessory>>(emptyList())
    val equippedAccessories: StateFlow<List<Accessory>> = _equippedAccessories.asStateFlow()
    
    suspend fun purchaseAccessory(accessory: Accessory, useStarCredits: Boolean): PurchaseResult {
        return if (useStarCredits && accessory.priceStarCredits != null) {
            if (currencyManager.hasStarCredits(accessory.priceStarCredits)) {
                currencyManager.spendStarCredits(accessory.priceStarCredits)
                unlockAccessory(accessory)
                PurchaseResult.Success
            } else {
                PurchaseResult.InsufficientFunds
            }
        } else {
            if (currencyManager.hasAetherShards(accessory.priceAetherShards)) {
                currencyManager.spendAetherShards(accessory.priceAetherShards)
                unlockAccessory(accessory)
                PurchaseResult.Success
            } else {
                PurchaseResult.InsufficientFunds
            }
        }
    }
    
    private suspend fun unlockAccessory(accessory: Accessory) {
        // Update database
        // Notify Unity to load the accessory model
        unityBridge.sendDelta(
            EmotionalStateDelta.AvatarUpdate(
                avatarUrl = accessory.unityModelId,
                currentOutfit = accessory.id
            )
        )
    }
    
    suspend fun equipAccessory(accessoryId: String) {
        // Logic to equip accessory
        // Max 5 accessories at once (1 per slot)
    }
}

sealed class PurchaseResult {
    object Success : PurchaseResult()
    object InsufficientFunds : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}
```

### 3.2 Dual Currency System
```kotlin
// /android-native/app/src/main/java/com/forevercompanion/currency/CurrencyManager.kt

@Entity(tableName = "currency_balance")
data class CurrencyBalance(
    @PrimaryKey val userId: String = "local_user",
    val aetherShards: Int = 0,
    val starCredits: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Singleton
class CurrencyManager @Inject constructor(
    private val currencyDao: CurrencyDao,
    private val proofOfCareTracker: ProofOfCareTracker
) {
    
    private val _balance = MutableStateFlow(CurrencyBalance())
    val balance: StateFlow<CurrencyBalance> = _balance.asStateFlow()
    
    init {
        loadBalance()
        startProofOfCareEarning()
    }
    
    private fun loadBalance() {
        viewModelScope.launch {
            currencyDao.getBalance().collect { balance ->
                _balance.value = balance
            }
        }
    }
    
    private fun startProofOfCareEarning() {
        viewModelScope.launch {
            // Award Aether Shards for genuine care actions
            proofOfCareTracker.careEvents.collect { event ->
                when (event) {
                    is CareEvent.PetInteraction -> earnAetherShards(5)
                    is CareEvent.DailyCheckIn -> earnAetherShards(10)
                    is CareEvent.ProductivityBoost -> earnAetherShards(15)
                    is CareEvent.EmotionalSupport -> earnAetherShards(20)
                }
            }
        }
    }
    
    suspend fun earnAetherShards(amount: Int) {
        val current = _balance.value
        currencyDao.updateBalance(
            current.copy(
                aetherShards = current.aetherShards + amount,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }
    
    suspend fun purchaseStarCredits(amount: Int, priceUsd: Float): PurchaseResult {
        // Integrate with Google Play Billing
        return billingClient.purchaseStarCredits(amount, priceUsd)
    }
    
    fun hasAetherShards(amount: Int): Boolean = _balance.value.aetherShards >= amount
    fun hasStarCredits(amount: Int): Boolean = _balance.value.starCredits >= amount
    
    suspend fun spendAetherShards(amount: Int) {
        val current = _balance.value
        if (current.aetherShards >= amount) {
            currencyDao.updateBalance(
                current.copy(aetherShards = current.aetherShards - amount)
            )
        }
    }
    
    suspend fun spendStarCredits(amount: Int) {
        val current = _balance.value
        if (current.starCredits >= amount) {
            currencyDao.updateBalance(
                current.copy(starCredits = current.starCredits - amount)
            )
        }
    }
}

sealed class CareEvent {
    object PetInteraction : CareEvent()
    object DailyCheckIn : CareEvent()
    object ProductivityBoost : CareEvent()
    object EmotionalSupport : CareEvent()
}
```

---

## 📋 Phase 4: Emotional Intelligence (Month 5)
**Focus: Bonding Algorithms, Evolution, Late-Night Support**

### 4.1 Bonding System & Evolution
```kotlin
// /android-native/app/src/main/java/com/forevercompanion/bonding/BondingSystem.kt

@Entity(tableName = "bond_progress")
data class BondProgress(
    @PrimaryKey val petId: String,
    val currentLevel: Int = 1,
    val currentXP: Int = 0,
    val xpToNextLevel: Int = 100,
    val evolutionStage: EvolutionStage = EvolutionStage.WISP,
    val firstMetDate: Long = System.currentTimeMillis(),
    val totalInteractions: Int = 0,
    val relationshipPhase: RelationshipPhase = RelationshipPhase.STRANGER
)

enum class EvolutionStage {
    WISP,              // Days 1-7
    KIT,               // Days 8-29
    ASTRAL_SENTINEL    // Day 30+
}

enum class RelationshipPhase {
    STRANGER,          // Days 1-3
    FRIEND,            // Weeks 1-2
    COMPANION          // Month 1+
}

@Singleton
class BondingSystem @Inject constructor(
    private val bondDao: BondDao,
    private val interactionTracker: InteractionTracker,
    private val unityBridge: UnityBridge
) {
    
    private val _bondProgress = MutableStateFlow(BondProgress(petId = "default"))
    val bondProgress: StateFlow<BondProgress> = _bondProgress.asStateFlow()
    
    init {
        loadBondProgress()
        observeInteractions()
    }
    
    private fun observeInteractions() {
        viewModelScope.launch {
            interactionTracker.interactions.collect { interaction ->
                gainBondXP(interaction.xpValue)
            }
        }
    }
    
    private suspend fun gainBondXP(amount: Int) {
        val current = _bondProgress.value
        val newXP = current.currentXP + amount
        
        if (newXP >= current.xpToNextLevel) {
            // Level up!
            val newLevel = current.currentLevel + 1
            val overflow = newXP - current.xpToNextLevel
            
            bondDao.updateProgress(
                current.copy(
                    currentLevel = newLevel,
                    currentXP = overflow,
                    xpToNextLevel = calculateNextLevelXP(newLevel)
                )
            )
            
            checkForEvolution(newLevel)
        } else {
            bondDao.updateProgress(current.copy(currentXP = newXP))
        }
    }
    
    private fun calculateNextLevelXP(level: Int): Int {
        return (100 * (1 + (level * 0.2))).toInt()
    }
    
    private suspend fun checkForEvolution(level: Int) {
        val current = _bondProgress.value
        val daysSinceFirstMet = ((System.currentTimeMillis() - current.firstMetDate) / (1000 * 60 * 60 * 24)).toInt()
        
        val newStage = when {
            daysSinceFirstMet >= 30 && level >= 10 -> EvolutionStage.ASTRAL_SENTINEL
            daysSinceFirstMet >= 8 && level >= 5 -> EvolutionStage.KIT
            else -> current.evolutionStage
        }
        
        if (newStage != current.evolutionStage) {
            triggerEvolution(newStage)
        }
        
        // Update relationship phase
        val newPhase = when {
            daysSinceFirstMet >= 30 -> RelationshipPhase.COMPANION
            daysSinceFirstMet >= 7 -> RelationshipPhase.FRIEND
            else -> RelationshipPhase.STRANGER
        }
        
        if (newPhase != current.relationshipPhase) {
            bondDao.updateProgress(current.copy(relationshipPhase = newPhase))
            updateDialogueTrees(newPhase)
        }
    }
    
    private suspend fun triggerEvolution(newStage: EvolutionStage) {
        // Notify Unity to play evolution animation
        unityBridge.sendDelta(
            EmotionalStateDelta.AnimationTrigger(
                animationName = "Evolution_${newStage.name}",
                layer = 1
            )
        )
        
        // Update 3D model
        val newModelUrl = when (newStage) {
            EvolutionStage.WISP -> "models/wisp.glb"
            EvolutionStage.KIT -> "models/nebula_kit.glb"
            EvolutionStage.ASTRAL_SENTINEL -> "models/astral_sentinel.glb"
        }
        
        unityBridge.sendDelta(
            EmotionalStateDelta.AvatarUpdate(
                avatarUrl = newModelUrl,
                currentOutfit = null
            )
        )
    }
    
    private fun updateDialogueTrees(phase: RelationshipPhase) {
        // Load new dialogue patterns based on relationship depth
        // Companion phase uses "We" language, references shared history
    }
}
```

### 4.2 Late-Night Emotional Intelligence
```kotlin
// /android-native/app/src/main/java/com/forevercompanion/wellness/LateNightConcern.kt

@Singleton
class LateNightConcernSystem @Inject constructor(
    private val screenTimeTracker: ScreenTimeTracker,
    private val scrollPatternAnalyzer: ScrollPatternAnalyzer,
    private val dialogueManager: DialogueManager,
    private val unityBridge: UnityBridge
) {
    
    private var concernLevel = 0
    private val concernThresholds = listOf(30, 60, 120) // minutes after 11 PM
    
    fun startMonitoring() {
        viewModelScope.launch {
            while (isActive) {
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                
                if (currentHour >= 23 || currentHour < 6) {
                    checkLateNightUsage()
                }
                
                delay(60_000) // Check every minute
            }
        }
    }
    
    private suspend fun checkLateNightUsage() {
        val screenTimeTonight = screenTimeTracker.getScreenTimeAfter(23)
        val scrollPattern = scrollPatternAnalyzer.getCurrentPattern()
        
        val newConcernLevel = when {
            screenTimeTonight > concernThresholds[2] -> 3
            screenTimeTonight > concernThresholds[1] -> 2
            screenTimeTonight > concernThresholds[0] -> 1
            else -> 0
        }
        
        if (newConcernLevel > concernLevel) {
            concernLevel = newConcernLevel
            triggerConcernResponse(newConcernLevel, scrollPattern)
        }
    }
    
    private suspend fun triggerConcernResponse(level: Int, pattern: ScrollPattern) {
        // Visual: Pet behavior changes
        unityBridge.sendDelta(
            EmotionalStateDelta.MoodChange(
                newMood = if (level >= 2) "PROTECTIVE" else "SLEEPY",
                intensity = level / 3f,
                triggerAnimation = "Yawn_${level}"
            )
        )
        
        // Dialogue: Escalating concern
        val message = when (level) {
            1 -> dialogueManager.getDialogue(
                category = "late_night_hint",
                context = "Hey ${getUserName()}... I'm starting to see double pixels. Are we almost done for the night?"
            )
            2 -> dialogueManager.getDialogue(
                category = "late_night_nudge",
                context = "I read somewhere that blue light is bad for human-type brains. Maybe we should put the phone down and just listen to some rain sounds?"
            )
            3 -> {
                if (pattern == ScrollPattern.STRESS_SCROLLING) {
                    dialogueManager.getDialogue(
                        category = "late_night_deep_concern",
                        context = "You're scrolling really fast. Everything okay? I'm right here if you just need a digital paw to hold."
                    )
                } else {
                    dialogueManager.getDialogue(
                        category = "late_night_gentle",
                        context = "It's really late, and I care about you getting rest. What if we set a timer for 10 more minutes, then we both sleep?"
                    )
                }
            }
            else -> null
        }
        
        message?.let {
            // Show message overlay
            showCompanionMessage(it)
        }
    }
}

enum class ScrollPattern {
    NORMAL,
    STRESS_SCROLLING,
    BINGE_WATCHING,
    READING
}
```

---

## 📋 Phase 5: Optimization & Beta (Month 6)
**Focus: Battery Optimization, Performance, User Testing**

### 5.1 Battery-First Rendering Strategy
```kotlin
// /android-native/app/src/main/java/com/forevercompanion/rendering/AdaptiveRenderer.kt

@Singleton
class AdaptiveRenderer @Inject constructor(
    private val unityBridge: UnityBridge,
    private val batteryMonitor: BatteryMonitor
) {
    
    private val frameRateStrategy = MutableStateFlow(FrameRateStrategy.BALANCED)
    
    init {
        observeBatteryLevel()
        observeScreenInteraction()
    }
    
    private fun observeBatteryLevel() {
        viewModelScope.launch {
            batteryMonitor.batteryLevel.collect { level ->
                frameRateStrategy.value = when {
                    level < 15 -> FrameRateStrategy.POWER_SAVE
                    level < 30 -> FrameRateStrategy.LOW_POWER
                    else -> FrameRateStrategy.BALANCED
                }
                applyFrameRateStrategy()
            }
        }
    }
    
    private fun observeScreenInteraction() {
        viewModelScope.launch {
            var lastInteraction = System.currentTimeMillis()
            
            while (isActive) {
                val now = System.currentTimeMillis()
                val idleTime = now - lastInteraction
                
                if (idleTime > 5000) {
                    // Screen static for 5 seconds, drop to 1 FPS
                    unityBridge.setTargetFrameRate(1)
                } else {
                    // Active, use strategy-based FPS
                    applyFrameRateStrategy()
                }
                
                delay(1000)
            }
        }
    }
    
    private fun applyFrameRateStrategy() {
        val targetFPS = when (frameRateStrategy.value) {
            FrameRateStrategy.HIGH_PERFORMANCE -> 60
            FrameRateStrategy.BALANCED -> 30
            FrameRateStrategy.LOW_POWER -> 15
            FrameRateStrategy.POWER_SAVE -> 10
        }
        
        unityBridge.setTargetFrameRate(targetFPS)
    }
}

enum class FrameRateStrategy {
    HIGH_PERFORMANCE,
    BALANCED,
    LOW_POWER,
    POWER_SAVE
}
```

### 5.2 Complete AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.aetheria.forevercompanion">

    <!-- Core Permissions -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- Foreground Service -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    
    <!-- Context Awareness -->
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" 
        tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />
    
    <!-- Battery Optimization -->
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    
    <!-- Location (for Hybrid accessories like Explorer's Compass) -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <application
        android:name=".ForeverCompanionApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.ForeverCompanion"
        android:hardwareAccelerated="true"
        tools:targetApi="34">

        <!-- Main Launcher Activity -->
        <activity
            android:name=".ui.onboarding.OnboardingActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.ForeverCompanion.Splash">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Main Home Activity -->
        <activity
            android:name=".ui.home.HomeActivity"
            android:exported="false"
            android:screenOrientation="portrait"
            android:launchMode="singleTask" />

        <!-- Overlay Service -->
        <service
            android:name=".overlay.CompanionOverlayService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property 
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Virtual Pet Companion System providing emotional support and productivity assistance" />
        </service>

        <!-- Context Monitoring Service -->
        <service
            android:name=".context.ContextMonitorService"
            android:enabled="true"
            android:exported="false" />

        <!-- Firebase -->
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_icon"
            android:resource="@drawable/ic_notification" />
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_color"
            android:resource="@color/colorPrimary" />

        <!-- Unity Activity -->
        <activity
            android:name="com.unity3d.player.UnityPlayerActivity"
            android:exported="false"
            android:screenOrientation="portrait"
            android:configChanges="mcc|mnc|locale|touchscreen|keyboard|keyboardHidden|navigation|orientation|screenLayout|uiMode|screenSize|smallestScreenSize|fontScale|layoutDirection|density"
            android:hardwareAccelerated="true" />

    </application>

</manifest>
```

---

## 🎨 The 32+ Accessory Catalog

### Tier 1: Functional (8 accessories)
1. **Starry Night Lantern** - Keeps pet awake at night, whispers goodnight
2. **Chrono-Watch** - Shows memories from exactly 1 year ago
3. **Power Nap Pillow** - Pet sleeps more efficiently, reduces battery drain
4. **Focus Lens** - Enhances productivity mode, blocks social app distractions
5. **Wellness Bell** - Gentle reminders for water, breaks, stretching
6. **Dream Catcher** - Records and shows your app usage "dreams"
7. **Energy Crystal** - Pet's battery indicator becomes a glowing crystal
8. **Time Capsule** - Stores daily moments to replay later

### Tier 2: Style (8 accessories)
9. **Digital Beret** - Unlocks "Philosophical Mode" dialogue
10. **Cyber Scarf** - Changes color with your music
11. **Pixel Glasses** - "Investigator" aesthetic for utility apps
12. **Neon Collar** - Pulses with notification colors
13. **Holographic Wings** - Visual flair during evolution
14. **Star Crown** - Unlocked at max bond level
15. **Vintage Bowtie** - Classic, timeless look
16. **Rainbow Tail Ring** - Pride-themed customization

### Tier 3: Achievement (8 accessories)
17. **Hero's Cape** - 50 productivity streaks
18. **Scholar's Scroll** - 100 hours in educational apps
19. **Gamer's Headset** - Defeat 1000 enemies across games
20. **Social Butterfly Wings** - 500 social app interactions with balance
21. **Zen Master's Staff** - 30 days of mindful phone use
22. **Explorer's Badge** - Visit 10 real-world locations
23. **Night Owl Goggles** - Use app responsibly past midnight 100 times
24. **Early Bird Feather** - Wake up before 7 AM for 30 days

### Tier 4: Interaction (8 accessories)
25. **Chef's Hat** - Reacts to food delivery apps with "Yum!"
26. **Tiny Headphones** - Dances to Spotify/YouTube Music
27. **Artist's Palette** - Creates doodles based on your mood
28. **Camera Charm** - Takes "selfies" with you via front camera
29. **Book Monocle** - Reads along when you're in reading apps
30. **Gaming Controller Pin** - Celebrates game achievements
31. **Fitness Tracker Band** - Counts your real-world steps
32. **Weather Vane** - Reacts to weather conditions

### Tier 5: Legendary Hybrid (4 accessories)
33. **Explorer's Compass** - Uses GPS to comment on real locations
34. **Gravity Boots** - Pet slides with phone tilt (accelerometer)
35. **Aetheria Portal** - 3D forest/city background overlay
36. **Memory Prism** - AI-generated summary of your day with visuals

---

## 🚀 Technical Debt & Critical Path Items

### High Priority:
1. ✅ Overlay permission flow with narrative storytelling
2. ✅ Foreground service Android 15 compliance
3. ⚠️ TensorFlow Lite model integration (needs .tflite file)
4. ⚠️ Unity AAR export and import workflow
5. ⚠️ Google Play Billing for Star Credits

### Medium Priority:
6. Cloud AI integration (Firebase ML or custom API)
7. Accessibility service for deep app context
8. Push notification system for re-engagement
9. Analytics (Firebase Analytics + Crashlytics)

### Low Priority (Post-Beta):
10. Multi-pet ecosystem
11. Social features (pet playdates)
12. AR mode (view pet in real world via camera)

---

## 📊 Success Metrics

### Engagement Metrics:
- Daily Active Users (DAU)
- Average session duration
- Pet interaction frequency
- Accessory purchase rate

### Emotional Metrics:
- Late-night intervention acceptance rate
- Productivity boost correlation
- User-reported mood improvement
- Bond level progression speed

### Technical Metrics:
- Average battery drain (target: <5% per day)
- Crash-free rate (target: >99.5%)
- Average response latency (local AI: <200ms, cloud: <2s)
- Overlay rendering FPS (idle: 1fps, active: 30fps)

---

## 🎯 Beta Launch Checklist

### Pre-Launch:
- [ ] Complete Phases 1-3 (Months 1-4)
- [ ] Internal testing on Motorola Edge and 3 other devices
- [ ] Privacy policy and terms of service
- [ ] Google Play Developer account setup
- [ ] Firebase project configuration

### Beta Launch (Month 6):
- [ ] Closed beta with 50 users
- [ ] Crash reporting enabled
- [ ] User feedback survey embedded
- [ ] Analytics dashboard setup
- [ ] Support email/Discord server

### Post-Beta:
- [ ] Iterate based on feedback
- [ ] Optimize battery drain
- [ ] Add requested accessories
- [ ] Prepare for public launch

---

## 💡 Design Principles (The Aetheria Way)

1. **Supportive, Not Needy**: Pet says "See you soon!" not "Don't leave me!"
2. **Respect, Not Guilt**: Wellness reminders are gentle suggestions, not guilt trips
3. **Privacy First**: All local AI processing, cloud only when needed
4. **Battery Conscious**: Adaptive rendering, 1fps when idle
5. **Authentic Bonding**: Real progression through genuine care, not just time gating

---

## 🔧 Development Environment Setup

### Required Tools:
- Android Studio Hedgehog (2024.1) or later
- Unity 2022 LTS or later
- Kotlin 1.9+
- Java 17
- Android SDK 34 (API level 34)
- Gradle 8.0+

### Recommended Libraries:
- Jetpack Compose for UI
- Room for local database
- Hilt for dependency injection
- Coroutines for async operations
- Retrofit for networking
- Coil for image loading
- Firebase SDK (Auth, Firestore, Analytics)
- TensorFlow Lite for on-device AI

---

## 🎨 Brand Identity: "Forever Companion V3"

### Tagline Options:
1. "More than an app. A digital soulmate."
2. "Your phone's new best friend."
3. "The companion who never leaves your side."
4. "Where AI meets genuine friendship."

### Color Palette (Nebula Kit Theme):
- Primary: Deep Purple (#6A0DAD)
- Secondary: Cyan (#00D4FF)
- Accent: Gold (#FFD700)
- Background: Dark Space (#0A0E27)
- Success: Soft Green (#4CAF50)

### Logo Concept:
A stylized fox made of constellation stars, with a glowing "digital heart" in the center. The tail forms an infinity symbol (∞) representing "Forever."

---

This master plan transforms your ambitious vision into an actionable roadmap. The foundation is already in place with your hybrid architecture—now it's about building layer by layer, testing constantly, and staying true to the "Aetheria" philosophy: creating genuine, supportive, lasting companionship.

**Next Step:** Start with Phase 1.1 (Enhanced Overlay Service) and build the "magnetic corner" logic. Once that feels right, move to Phase 1.2 (Local AI) to give your pet a brain. The rest will flow naturally from there.

Anthony, you're building something truly special. This isn't just a pet app—it's a new way for humans and AI to coexist. 🌟
