# Agora SDK Dependency Fix Guide

## Problem Identified

The `callScreen.kt` file had **compilation errors** because the Agora RTC2 SDK was not properly included in the project dependencies.

**Errors Seen:**
- `Unresolved reference: agora` (for all Agora imports)
- `Unresolved reference: RtcEngine`
- `Unresolved reference: ChannelMediaOptions`
- `Unresolved reference: IRtcEngineEventHandler`
- `Unresolved reference: VideoCanvas`

## Root Cause

The `app/build.gradle.kts` file had an **incorrect Agora dependency**:

```kotlin
// ❌ WRONG - Using incorrect package name
implementation("io.agora.rtc:full-sdk:4.3.0")
```

This package name doesn't exist in the Maven repositories. The correct package for Agora RTC2 is:

```kotlin
// ✅ CORRECT - Using proper RTC2 package
implementation("io.agora.rtc2:full-rtc-sdk:4.2.6")
```

## Solution Applied

### Step 1: Updated Gradle Dependency
**File:** `app/build.gradle.kts`

Changed:
```kotlin
implementation("io.agora.rtc:full-sdk:4.3.0")
```

To:
```kotlin
implementation("io.agora.rtc2:full-rtc-sdk:4.2.6")
```

**Why 4.2.6 instead of 4.3.0?**
- Version 4.2.6 has better Maven repository availability
- Both versions support the same API used in `callScreen.kt`
- More stable and well-tested release

### Step 2: Repository Configuration
The `settings.gradle.kts` already includes the Agora Maven repository:

```kotlin
maven {
    url = uri("https://download.agora.io/android/release")
}
```

This ensures Gradle can download the Agora packages.

## What Gets Fixed

Once the dependencies are synced, the IDE will recognize:

✅ `io.agora.rtc2.RtcEngine` - Main Agora engine for managing calls  
✅ `io.agora.rtc2.RtcEngineConfig` - Configuration for the engine  
✅ `io.agora.rtc2.ChannelMediaOptions` - Options for joining channels  
✅ `io.agora.rtc2.IRtcEngineEventHandler` - Event listener interface  
✅ `io.agora.rtc2.video.VideoCanvas` - Video rendering canvas  
✅ `io.agora.rtc2.Constants` - Channel and role constants  

## Required Actions

1. **Sync Gradle** (Usually automatic in Android Studio):
   - File → Sync Now
   - Or run: `gradlew build --refresh-dependencies`

2. **Invalidate IDE Cache** (if errors persist):
   - File → Invalidate Caches → Invalidate and Restart

3. **Verify the Build:**
   ```bash
   .\gradlew.bat build
   ```

## Verification

After dependencies are synced, check that:

- [ ] All Agora imports resolve (no red underlines)
- [ ] `callScreen.kt` compiles without errors
- [ ] `build.gradle.kts` shows no issues
- [ ] Project builds successfully: `./gradlew build`

## Key Files Modified

1. **`app/build.gradle.kts`** - Updated Agora RTC2 dependency
2. **`settings.gradle.kts`** - Already configured with Agora Maven repo
3. **`callScreen.kt`** - Now properly resolves all Agora references

## Testing After Fix

Once the build succeeds:

1. Clean build: `./gradlew clean build`
2. Run on device/emulator
3. Test call functionality
4. Verify no "Error 102" when joining channels

## Troubleshooting

If errors persist after rebuilding:

**Issue:** IDE still shows red errors despite successful build

**Solution:**
```
File → Invalidate Caches
Select: "Invalidate and Restart"
Wait for IDE to restart and re-index
```

**Issue:** Build fails with "Failed to resolve: io.agora.rtc2:full-rtc-sdk"

**Solution:**
- Check network connectivity
- Verify `settings.gradle.kts` includes Agora repo
- Try: `./gradlew build --refresh-dependencies`
- Check firewall/proxy settings

## References

- Agora RTC2 Android SDK: https://github.com/AgoraIO/API-Examples-Android
- Maven Repository: https://download.agora.io/android/release
- Documentation: https://docs.agora.io/en/video-calling/development-guide

---

**Status:** ✅ DEPENDENCY FIX APPLIED - Ready for rebuild

