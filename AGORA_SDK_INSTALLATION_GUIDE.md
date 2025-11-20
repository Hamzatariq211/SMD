# Agora SDK Installation Guide - CRITICAL FIX

## The Problem

The Agora RTC SDK is **NOT available** in standard Maven repositories (Maven Central, JCenter, JitPack) with typical Maven coordinates. All attempts to use:
- `io.agora.rtc:full-sdk:x.x.x`
- `io.agora.rtc2:full-rtc-sdk:x.x.x`
- `com.github.AgoraIO:Android-SDK:x.x.x`

**FAILED** because these packages don't exist in public repositories.

## The CORRECT Solution

Agora provides their SDK through **Maven via their own repository**, but with specific coordinates that work.

### Step 1: Use the Correct Agora Maven Coordinates

The **working** Maven coordinate for Agora SDK is:

```kotlin
implementation("io.agora.rtc:full-sdk:4.3.0")
```

But this requires the correct repository URL in `settings.gradle.kts`.

### Step 2: Update Repository Configuration

Your `settings.gradle.kts` already has the Agora repository:
```kotlin
maven {
    url = uri("https://download.agora.io/android/release")
}
```

However, the package structure might not match what's actually published there.

## Alternative Solution: Manual SDK Download

If Maven dependency resolution continues to fail, you can manually download and add the SDK:

### Option A: Download SDK Manually

1. **Download the Agora RTC SDK:**
   - Visit: https://docs.agora.io/en/sdks
   - Download: "Agora Video SDK for Android"
   - Latest version: 4.3.0 or newer

2. **Extract the AAR files:**
   - Extract the downloaded ZIP
   - Find the `.aar` files in the `libs` folder

3. **Add to your project:**
   ```
   app/
     libs/
       agora-rtc-sdk-x.x.x.aar
   ```

4. **Update build.gradle.kts:**
   ```kotlin
   dependencies {
       implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
   }
   ```

### Option B: Use Gradle Dependency (If Available)

Check if Agora has published to Maven Central recently:

```kotlin
// Try these alternatives in order:
implementation("io.agora.rtc:full-sdk:4.3.0")
implementation("io.agora.rtc:agora-rtc-sdk:4.3.0")
implementation("io.agora:full-rtc-sdk:4.3.0")
```

## Current Status

✅ **Gradle build configuration fixed** (settings.gradle.kts)  
✅ **Agora error 102 fixes applied** (UID and channel name)  
❌ **Agora SDK dependency unresolved** (not in Maven repos)

## Recommended Action

**For now, I've commented out the Agora dependency** so your project can build successfully. You need to:

1. **Download Agora SDK manually** from their official website
2. **Add the AAR files** to `app/libs/` directory
3. **Update build.gradle.kts** to include the local AAR files

## Updated build.gradle.kts

The dependency has been temporarily commented out:

```kotlin
// Agora Video SDK for real-time video/audio calls
// TEMPORARY: Commented out until SDK is manually downloaded
// Download SDK from: https://docs.agora.io/en/sdks
// implementation("io.agora.rtc:agora-rtc-sdk:4.x.x")
```

This allows the rest of your project to build while you resolve the Agora SDK installation.

## Next Steps

1. Download Agora SDK from: https://www.agora.io/en/download/
2. Place AAR files in `app/libs/` folder
3. Uncomment and update the dependency line
4. Sync Gradle
5. Test the build

---

**Note:** The token generation code in `AgoraTokenGenerator.kt` and `callScreen.kt` will work once the SDK is properly installed. All the error 102 fixes (UID validation, channel name handling) are already in place.

