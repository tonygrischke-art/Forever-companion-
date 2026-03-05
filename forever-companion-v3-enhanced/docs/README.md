# Forever Companion - Hybrid Architecture v3

Native Android + Unity 3D hybrid with AAR workflow.

## Structure
- android-native/: Kotlin app with Firebase, Room, Billing
- unity-module/: Unity project exports to AAR
- unity-aar-import/: Drop-in AAR module

## Architecture
Android (AI/Memory) → Deltas → Unity (Visuals only)

## Quick Start
1. Build android-native (2D mode works immediately)
2. Build Unity → AAR → drop in unity-aar-import/libs/
3. Toggle 3D mode

## Delta Messages
Unity receives: MOOD_DELTA, ANIM_TRIGGER, AVATAR_UPDATE, LOOK_AT
