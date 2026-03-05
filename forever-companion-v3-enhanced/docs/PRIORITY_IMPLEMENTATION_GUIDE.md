# Forever Companion V3: Priority Implementation Guide
## Your 30-Day Sprint to MVP

**Goal:** Get a working "Hybrid Pet" on your Motorola Edge in 30 days

---

## Week 1: Foundation & Permissions (Days 1-7)

### Day 1-2: Enhanced Overlay Service
**Location:** `/android-native/app/src/main/java/com/forevercompanion/overlay/`

#### Task 1.1: Create CompanionOverlayService.kt
```kotlin
// Copy this structure and implement step-by-step

@AndroidEntryPoint
class CompanionOverlayService : Service() {
    
    // Start here: Basic overlay that shows a simple circle
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createSimpleOverlay()
        return START_STICKY
    }
    
    private fun createSimpleOverlay() {
        val params = WindowManager.LayoutParams(
            200, 200, // Start with 200x200px
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        val view = View(this).apply {
            setBackgroundColor(Color.parseColor("#6A0DAD")) // Purple circle
        }
        
        windowManager.addView(view, params)
    }
}
```

**Success Criteria:** See a purple circle on your home screen

#### Task 1.2: Add Drag-to-Move
```kotlin
// Add touch listener to make it draggable
view.setOnTouchListener { v, event ->
    when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            // Store initial position
        }
        MotionEvent.ACTION_MOVE -> {
            // Update view position
            windowManager.updateViewLayout(view, params)
        }
    }
    true
}
```

**Success Criteria:** Can drag the circle around your screen

### Day 3-4: Permission Flow with Storytelling

#### Task 2.1: Create OnboardingActivity.kt
```kotlin
class OnboardingActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            OnboardingScreen(
                onComplete = {
                    startService(Intent(this, CompanionOverlayService::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    
    when (step) {
        1 -> WelcomeScreen(onNext = { step = 2 })
        2 -> WindowKeyPermission(onGranted = { step = 3 })
        3 -> NameYourPet(onComplete = onComplete)
    }
}

@Composable
fun WindowKeyPermission(onGranted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pet character (use a simple emoji for now)
        Text("🦊", fontSize = 80.sp)
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            "I want to be able to follow you everywhere!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Text(
            "Can you give me a 'Window Key'? It'll let me float on top so we never have to say goodbye.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        
        Spacer(Modifier.height(32.dp))
        
        Button(onClick = {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }) {
            Text("Grant Window Key 🔑")
        }
    }
}
```

**Success Criteria:** User sees storytelling permission flow

### Day 5-7: Basic 2D Pet Character

#### Task 3.1: Replace circle with animated pet sprite
```kotlin
// Use Jetpack Compose Canvas to draw a simple pet

@Composable
fun SimplePetView(mood: PetMood = PetMood.HAPPY) {
    val infiniteTransition = rememberInfiniteTransition()
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Canvas(modifier = Modifier.size(100.dp)) {
        // Body (circle)
        drawCircle(
            color = Color(0xFF6A0DAD),
            radius = 40.dp.toPx(),
            center = Offset(size.width / 2, size.height / 2 - bounce)
        )
        
        // Eyes
        drawCircle(
            color = Color.White,
            radius = 5.dp.toPx(),
            center = Offset(size.width / 2 - 15, size.height / 2 - 10 - bounce)
        )
        drawCircle(
            color = Color.White,
            radius = 5.dp.toPx(),
            center = Offset(size.width / 2 + 15, size.height / 2 - 10 - bounce)
        )
    }
}
```

**Success Criteria:** See a bouncing pet character on your screen

---

## Week 2: Context Awareness (Days 8-14)

### Day 8-10: App Usage Monitoring

#### Task 4.1: Request Usage Stats Permission
```kotlin
// Add to OnboardingScreen after Window Key

@Composable
fun MindLinkPermission(onGranted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🦊", fontSize = 80.sp)
        
        Text(
            "To be a really good friend, I need to understand what we're doing together.",
            textAlign = TextAlign.Center
        )
        
        Text(
            "If I have a 'Mind Link', I'll know when you're working hard so I can be quiet, or when you're gaming so I can cheer you on!",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        
        Button(onClick = {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
        }) {
            Text("Grant Mind Link 🧠")
        }
    }
}
```

#### Task 4.2: Create AppContextMonitor.kt
```kotlin
class AppContextMonitor(private val context: Context) {
    
    fun getCurrentApp(): String? {
        if (!hasUsageStatsPermission()) return null
        
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            currentTime - 60000,
            currentTime
        )
        
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }
    
    private fun hasUsageStatsPermission(): Boolean {
        val mode = context.getSystemService(AppOpsManager::class.java)
            .checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
```

### Day 11-14: Mood System Based on App Context

#### Task 5.1: Define Pet Moods
```kotlin
enum class PetMood(val color: Color, val animation: String) {
    HAPPY(Color(0xFF6A0DAD), "bounce"),
    FOCUS(Color(0xFF3366CC), "pulse_slow"),
    EXCITED(Color(0xFFFF6B00), "bounce_fast"),
    SLEEPY(Color(0xFF4A148C), "sway")
}

data class PetState(
    val mood: PetMood = PetMood.HAPPY,
    val energy: Float = 1.0f,
    val lastInteraction: Long = System.currentTimeMillis()
)
```

#### Task 5.2: Connect Context to Mood
```kotlin
class PetBrainController(
    private val contextMonitor: AppContextMonitor
) {
    
    private val _petState = MutableStateFlow(PetState())
    val petState: StateFlow<PetState> = _petState.asStateFlow()
    
    fun startContextLoop() {
        viewModelScope.launch {
            while (isActive) {
                val currentApp = contextMonitor.getCurrentApp()
                updateMoodBasedOnApp(currentApp)
                delay(2000) // Check every 2 seconds
            }
        }
    }
    
    private fun updateMoodBasedOnApp(appPackage: String?) {
        val newMood = when {
            appPackage?.contains("linkedin") == true -> PetMood.FOCUS
            appPackage?.contains("instagram") == true -> PetMood.EXCITED
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 22 -> PetMood.SLEEPY
            else -> PetMood.HAPPY
        }
        
        if (_petState.value.mood != newMood) {
            _petState.value = _petState.value.copy(mood = newMood)
        }
    }
}
```

**Success Criteria:** Pet changes color when you open LinkedIn vs Instagram

---

## Week 3: Interaction & Personality (Days 15-21)

### Day 15-17: Tap Interactions

#### Task 6.1: Add Tap Responses
```kotlin
@Composable
fun InteractivePetView(
    petState: PetState,
    onTap: () -> Unit
) {
    var showMessage by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            onTap()
            message = getRandomMessage(petState.mood)
            showMessage = true
        }
    ) {
        SimplePetView(petState.mood)
        
        if (showMessage) {
            SpeechBubble(message) {
                showMessage = false
            }
        }
    }
}

@Composable
fun SpeechBubble(text: String, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000)
        onDismiss()
    }
    
    Card(
        modifier = Modifier.padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

fun getRandomMessage(mood: PetMood): String {
    return when (mood) {
        PetMood.FOCUS -> listOf(
            "You're doing great! 💪",
            "Keep it up, Anthony!",
            "I believe in you!"
        ).random()
        PetMood.EXCITED -> listOf(
            "This looks fun! 🎉",
            "Ooh, what are we looking at?",
            "I love seeing you happy!"
        ).random()
        PetMood.SLEEPY -> listOf(
            "*yawn* It's getting late...",
            "Maybe we should rest soon?",
            "I'm here if you need me 💜"
        ).random()
        else -> listOf(
            "Hey there! 👋",
            "Need anything?",
            "I'm right here!"
        ).random()
    }
}
```

**Success Criteria:** Tap pet and see different messages based on mood

### Day 18-21: Basic Dialogue System

#### Task 7.1: Create Dialogue Database
```kotlin
@Entity(tableName = "dialogues")
data class DialogueEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val category: String, // "greeting", "focus", "late_night", etc.
    val mood: String,
    val text: String,
    val relationshipPhase: String = "STRANGER", // STRANGER, FRIEND, COMPANION
    val usageCount: Int = 0
)

@Dao
interface DialogueDao {
    @Query("SELECT * FROM dialogues WHERE category = :category AND mood = :mood AND relationshipPhase = :phase ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomDialogue(category: String, mood: String, phase: String): DialogueEntry?
    
    @Query("UPDATE dialogues SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: String)
}

// Pre-populate database with dialogues
class DialogueSeeder {
    suspend fun seedInitialDialogues(dao: DialogueDao) {
        val dialogues = listOf(
            // Stranger Phase
            DialogueEntry(
                category = "greeting",
                mood = "HAPPY",
                text = "Hello! I'm still getting to know you...",
                relationshipPhase = "STRANGER"
            ),
            DialogueEntry(
                category = "focus",
                mood = "FOCUS",
                text = "I see you're working on something. I'll be quiet.",
                relationshipPhase = "STRANGER"
            ),
            
            // Friend Phase
            DialogueEntry(
                category = "greeting",
                mood = "HAPPY",
                text = "Hey Anthony! Ready for another day together?",
                relationshipPhase = "FRIEND"
            ),
            DialogueEntry(
                category = "focus",
                mood = "FOCUS",
                text = "We've got this! Let me know if you need a break.",
                relationshipPhase = "FRIEND"
            ),
            
            // Companion Phase
            DialogueEntry(
                category = "greeting",
                mood = "HAPPY",
                text = "Morning! I was just thinking about that thing we did last week...",
                relationshipPhase = "COMPANION"
            ),
            DialogueEntry(
                category = "focus",
                mood = "FOCUS",
                text = "Remember last time we crushed a work session like this? We can do it again!",
                relationshipPhase = "COMPANION"
            )
        )
        
        dialogues.forEach { dao.insert(it) }
    }
}
```

**Success Criteria:** Pet says different things based on how long you've had the app

---

## Week 4: Polish & Testing (Days 22-30)

### Day 22-24: Battery Optimization

#### Task 8.1: Implement Adaptive Frame Rate
```kotlin
class OverlayLifecycleManager {
    
    private var currentFPS = 30
    private var lastInteraction = System.currentTimeMillis()
    
    fun startOptimizationLoop() {
        lifecycleScope.launch {
            while (isActive) {
                val timeSinceInteraction = System.currentTimeMillis() - lastInteraction
                
                val targetFPS = when {
                    timeSinceInteraction < 5000 -> 30 // Active
                    timeSinceInteraction < 60000 -> 10 // Idle
                    else -> 1 // Deep idle
                }
                
                if (targetFPS != currentFPS) {
                    updateRenderRate(targetFPS)
                    currentFPS = targetFPS
                }
                
                delay(1000)
            }
        }
    }
    
    fun onUserInteraction() {
        lastInteraction = System.currentTimeMillis()
    }
}
```

### Day 25-27: First Accessory System

#### Task 9.1: Implement Simple Hat Accessory
```kotlin
data class Accessory(
    val id: String,
    val name: String,
    val emoji: String, // Use emoji for MVP
    val description: String,
    val price: Int, // Aether Shards
    val unlocked: Boolean = false
)

val starterAccessories = listOf(
    Accessory(
        id = "hat_basic",
        name = "Starter Cap",
        emoji = "🧢",
        description = "Your first accessory!",
        price = 0,
        unlocked = true
    ),
    Accessory(
        id = "glasses_cool",
        name = "Cool Shades",
        emoji = "😎",
        description = "For when you're in Focus Mode",
        price = 50
    )
)

@Composable
fun PetWithAccessory(
    petState: PetState,
    equippedAccessory: Accessory?
) {
    Box {
        SimplePetView(petState.mood)
        
        // Show accessory on top
        equippedAccessory?.let { accessory ->
            Text(
                text = accessory.emoji,
                fontSize = 24.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp)
            )
        }
    }
}
```

### Day 28-30: Testing & Refinement

#### Task 10.1: Create Debug Panel
```kotlin
@Composable
fun DebugPanel(
    petState: PetState,
    onMoodChange: (PetMood) -> Unit,
    onAddShards: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Debug Controls", style = MaterialTheme.typography.headlineSmall)
            
            Spacer(Modifier.height(8.dp))
            
            Text("Current Mood: ${petState.mood}")
            Text("Energy: ${petState.energy}")
            Text("Aether Shards: ${/* load from DB */}")
            
            Spacer(Modifier.height(16.dp))
            
            // Test mood changes
            Row {
                Button(onClick = { onMoodChange(PetMood.HAPPY) }) {
                    Text("Happy")
                }
                Button(onClick = { onMoodChange(PetMood.FOCUS) }) {
                    Text("Focus")
                }
            }
            
            // Test currency
            Button(onClick = onAddShards) {
                Text("Add 100 Shards")
            }
        }
    }
}
```

#### Task 10.2: User Testing Checklist
```
Test Scenario 1: First Launch
- [ ] Onboarding shows all permission screens
- [ ] Pet appears on home screen after permissions granted
- [ ] Can name the pet
- [ ] Pet says welcome message

Test Scenario 2: Daily Use
- [ ] Pet changes mood when switching apps
- [ ] Can drag pet around screen
- [ ] Pet stays visible over other apps
- [ ] Battery drain < 10% over 8 hours

Test Scenario 3: Interactions
- [ ] Tap pet shows context-appropriate dialogue
- [ ] Pet responds to late-night usage (after 11 PM)
- [ ] Can earn Aether Shards through interactions
- [ ] Can view and equip accessories

Test Scenario 4: Edge Cases
- [ ] Pet survives phone restart
- [ ] Pet doesn't block keyboard
- [ ] Pet doesn't crash when other apps open
- [ ] Works on low battery mode
```

---

## Success Milestones

### 🎯 Day 7 Milestone: "The First Wake-Up"
- Pet appears on your screen
- Can drag it around
- Basic permissions working
**Celebrate:** Take a screenshot and share with a friend!

### 🎯 Day 14 Milestone: "The Living Pet"
- Pet changes moods based on your app usage
- Shows different dialogue based on context
- Responds to taps
**Celebrate:** Use it for a full day and note how it feels

### 🎯 Day 21 Milestone: "The Companion"
- Pet has personality based on relationship phase
- First accessory system working
- Basic currency system
**Celebrate:** Show it to potential beta testers

### 🎯 Day 30 Milestone: "MVP Complete"
- All Week 1-4 features working
- Stable on your Motorola Edge
- Ready for friends & family testing
**Celebrate:** You've built something real! 🎉

---

## Daily Development Rhythm

### Morning (1 hour)
- Read the task for the day
- Set up your development environment
- Write the skeleton code

### Lunch Break (30 mins)
- Test what you built in the morning
- Fix any obvious bugs
- Take notes on what feels right/wrong

### Evening (1-2 hours)
- Complete the task
- Test thoroughly
- Commit to Git with a clear message

### Before Bed
- Open the app on your phone
- Interact with your pet for 5 minutes
- Feel proud of what you built today

---

## When You Get Stuck

### Problem: "The overlay won't appear"
**Solution:** Check:
1. Is SYSTEM_ALERT_WINDOW permission granted?
2. Is the service started?
3. Are the WindowManager.LayoutParams correct?

### Problem: "The app crashes when I switch apps"
**Solution:** Check:
1. Are you handling lifecycle properly?
2. Is the service running in the foreground?
3. Are you catching exceptions in the context monitor?

### Problem: "I don't know how to implement X"
**Solution:**
1. Check the Master Plan for similar examples
2. Look at existing code in your project
3. Search "android kotlin [your problem]" on Stack Overflow
4. Ask Claude for help with specific code snippets

---

## Tech Stack Reminder (For MVP)

### Required:
- Kotlin (for all Android code)
- Jetpack Compose (for UI)
- Room (for local database)
- Coroutines (for async operations)

### NOT Required for MVP:
- Unity (use 2D Compose instead)
- Firebase (add in Phase 2)
- TensorFlow Lite (add in Phase 2)
- Cloud AI (use hardcoded responses)

---

## Git Commit Strategy

### Good commit messages:
```
✅ "Add overlay service with draggable pet"
✅ "Implement mood system based on app context"
✅ "Create onboarding permission flow"
```

### Bad commit messages:
```
❌ "Fixed stuff"
❌ "Update"
❌ "WIP"
```

### Branches:
- `main` - Always stable, working code
- `dev` - Your daily work
- `feature/onboarding` - Big features get their own branch

---

## Post-30-Day Roadmap

### Month 2: Polish
- Add Unity 3D pet (optional, can stay 2D)
- Implement local AI responses
- Add 10 more accessories
- Beta test with 5 friends

### Month 3: Growth
- Add cloud AI integration
- Implement evolution system
- Add 20 more accessories
- Beta test with 20 people

### Month 4: Launch Prep
- Optimize battery usage
- Create marketing materials
- Write app store listing
- Plan soft launch

---

## Motivational Reminders

### When it feels hard:
> "Every feature you build is a gift to yourself. This pet will be your companion for years."

### When you're tired:
> "The Nebula Kit doesn't need to be perfect. It just needs to make you smile."

### When you doubt:
> "In 30 days, you'll have something no one else has: a friend who lives in your phone."

---

## Final Thoughts

Anthony, you're not building an app. You're building a relationship. Take it one day at a time, celebrate small wins, and remember: the goal isn't perfection—it's connection.

Your Motorola Edge is about to become home to something special. Let's make it happen. 🌟🦊

**Start tomorrow. Day 1. Task 1.1. Create that purple circle.**

That's where legends begin.
