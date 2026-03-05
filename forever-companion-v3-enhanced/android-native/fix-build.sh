#!/bin/bash

echo "=========================================="
echo "  Forever Companion - Build Fix Script"
echo "=========================================="

# Navigate to correct directory
cd ~/forever-companion-v3-enhanced/android-native || { 
    echo "ERROR: android-native directory not found!"; 
    exit 1; 
}

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
</resources>
EOF
echo "✓ colors.xml created"

echo ""
echo "[4/6] Creating themes.xml (light)..."
cat > app/src/main/res/values/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.ForeverCompanion" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/purple_500</item>
        <item name="colorPrimaryVariant">@color/purple_700</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/teal_200</item>
        <item name="colorSecondaryVariant">@color/teal_700</item>
        <item name="colorOnSecondary">@color/black</item>
        <item name="android:statusBarColor">?attr/colorPrimaryVariant</item>
    </style>
</resources>
EOF
echo "✓ themes.xml (light) created"

echo ""
echo "[5/6] Creating themes.xml (night)..."
cat > app/src/main/res/values-night/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.ForeverCompanion" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/purple_200</item>
        <item name="colorPrimaryVariant">@color/purple_700</item>
        <item name="colorOnPrimary">@color/black</item>
        <item name="colorSecondary">@color/teal_200</item>
        <item name="colorSecondaryVariant">@color/teal_200</item>
        <item name="colorOnSecondary">@color/black</item>
        <item name="android:statusBarColor">?attr/colorPrimaryVariant</item>
    </style>
</resources>
EOF
echo "✓ themes.xml (night) created"

echo ""
echo "[6/6] Checking Material dependency..."
BUILD_FILE="app/build.gradle"
if [ -f "app/build.gradle.kts" ]; then
    BUILD_FILE="app/build.gradle.kts"
fi

if ! grep -q "com.google.android.material:material" "$BUILD_FILE"; then
    echo "Adding Material dependency to $BUILD_FILE..."
    sed -i '/dependencies {/a\    implementation "com.google.android.material:material:1.11.0"' "$BUILD_FILE"
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
    echo "  Check errors above."
fi
echo "=========================================="
