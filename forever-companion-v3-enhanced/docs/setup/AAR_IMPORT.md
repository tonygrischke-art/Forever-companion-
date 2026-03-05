# Unity AAR Import Instructions

## Quick Iteration Workflow

### 1. Build Unity AAR
In Unity Editor:
File → Build Settings → Android → Export Project
OR
File → Build Settings → Android → Build (Generate .aar)

### 2. Drop AAR into Android Project
Place in: android-native/unity-aar-import/libs/
- forever-companion-unity.aar
- unity-classes.jar

### 3. Sync Gradle
Android Studio auto-syncs, or run: ./gradlew sync

### 4. Version Management
Use semantic versioning:
- forever-companion-unity-v1.0.0.aar
- Update build.gradle to reference new version

## Troubleshooting

### Black Screen on Unity Load
Ensure proper lifecycle: unityPlayer.resume()/pause() in Activity

### Manifest Merge Conflicts
Add packagingOptions to app/build.gradle

### Cross-Platform
This module works on iOS (Xcode), WebGL, and Desktop with wrapper changes only.
