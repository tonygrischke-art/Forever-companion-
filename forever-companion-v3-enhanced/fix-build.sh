#!/bin/bash

echo "=========================================="
echo "  Forever Companion - Build Fix Script"
echo "=========================================="

# Navigate to project
cd ~/forever-companion-v3-enhanced || { echo "Project directory not found!"; exit 1; }

echo ""
echo "[1/6] Stopping any running Gradle daemon..."
./gradlew --stop 2>/dev/null || true
pkill -f "gradle" 2>/dev/null || true
sleep 2

echo ""
echo "[2/6] Cleaning previous builds..."
./gradlew clean --no-daemon 2>/dev/null || rm -rf app/build/

echo ""
echo "[3/6] Checking/Creating colors.xml..."
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/values-night

cat > app/src/main/res/values/colors.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
    <color name="primary">#FF6200EE</color>
    <color name="primary_dark">#FF3700B3</color>
    <color name="accent">#FF03DAC5</color>
</resources>
EOF
echo "✓ colors.xml created"

echo ""
echo "[4/6] Creating themes.xml (light)..."
cat > app/src/main/res/values/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.ForeverCompanion" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">@color/purple_500</item>
        <item name="colorPrimaryDark">@color/purple_700</item>
        <item name="colorAccent">@color/teal_200</item>
        <item name="android:statusBarColor">@color/purple_700</item>
    </style>
</resources>
EOF
echo "✓ themes.xml (light) created"

echo ""
echo "[5/6] Creating themes.xml (night)..."
cat > app/src/main/res/values-night/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.ForeverCompanion" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">@color/purple_200</item>
        <item name="colorPrimaryDark">@color/black</item>
        <item name="colorAccent">@color/teal_200</item>
        <item name="android:statusBarColor">@color/black</item>
    </style>
</resources>
EOF
echo "✓ themes.xml (night) created"

echo ""
echo "[6/6] Checking Material dependency..."
if ! grep -q "com.google.android.material:material" app/build.gradle.kts; then
    echo "Adding Material3 dependency..."
    # Add after the first dependencies line
    sed -i '/dependencies {/a\    implementation("com.google.android.material:material:1.11.0")' app/build.gradle.kts
    echo "✓ Material dependency added"
else
    echo "✓ Material dependency already present"
fi

echo ""
echo "=========================================="
echo "  Starting Build..."
echo "=========================================="

# Start the build with daemon
./gradlew assembleDebug --daemon

BUILD_STATUS=$?

echo ""
echo "=========================================="
if [ $BUILD_STATUS -eq 0 ]; then
    echo "  ✅ BUILD SUCCESSFUL!"
    echo ""
    echo "  APK Location:"
    find app/build/outputs/apk/debug -name "*.apk" -type f 2>/dev/null | head -1
    echo ""
    echo "  Install with:"
    echo "  adb install app/build/outputs/apk/debug/app-debug.apk"
else
    echo "  ❌ BUILD FAILED"
    echo ""
    echo "  Check errors above. Common fixes:"
    echo "  - Check internet connection"
    echo "  - Run: ./gradlew --stop && ./gradlew assembleDebug --no-daemon"
fi
echo "=========================================="
