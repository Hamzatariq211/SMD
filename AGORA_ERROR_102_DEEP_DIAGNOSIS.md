# 🔍 Agora Error -102 Deep Diagnosis & Advanced Fix

## ⚠️ Issue Status: Error -102 Persists After Initial Fixes

This means the problem is **NOT** just channel name formatting. Let me dig deeper into what error -102 actually means in Agora's SDK.

---

## 🎯 Error -102 Root Causes (In Order of Probability)

### 1. ❌ **Token Validation Algorithm Mismatch** (MOST LIKELY)
The `agora-access-token` library version might not match your Agora SDK version.

**Current Setup:**
- Token server library: `agora-access-token@2.0.10`
- Android SDK: `io.agora.rtc2:rtc-android:X.X.X`

**Problem:** If versions don't match, the token encoding/decoding will fail.

### 2. ❌ **Agora Server Time Sync**
Token expiration is based on server time. If your token server's time is out of sync with Agora's servers, tokens are rejected.

### 3. ❌ **APP_ID/APP_CERT Mismatch**
Even though credentials look correct, there might be a subtle difference (space, character encoding, etc.)

### 4. ❌ **Channel Name Special Characters**
Despite sanitization, there might be other invalid characters in the channel name being used.

---

## ✅ Step 1: Verify Agora SDK Version

Check your `app/build.gradle.kts`:

```kotlin
dependencies {
    // Find this line and note the version number
    implementation("io.agora.rtc2:rtc-android:4.X.X")  // <- Note the version
}
```

This version **MUST** be compatible with `agora-access-token@2.0.10`.

---

## ✅ Step 2: Run Token Generation Test

Let me create a test to verify token generation works correctly:

```bash
cd D:\agora-token-server
node test-token-generation.js
```

**Expected Output:**
```
=== AGORA TOKEN GENERATION TEST ===

[Test 1] Simple channel: test_call
  ├─ UID: 12345
  ├─ Role: PUBLISHER
  ├─ Privilege Expire: 1763587133
  ├─ Token generated: YES
  ├─ Token length: 139
  ├─ Token format: 007
  ├─ Full token: 007fc45bacc392b45c58b...
  └─ Status: ✅ SUCCESS
```

If you see **❌ ERROR**, there's a problem with the library installation. If all tests pass, the library is working correctly.

---

## ✅ Step 3: Check System Time

Token generation uses Unix timestamps. If your system time is wrong, tokens will be rejected.

**Check in PowerShell:**
```powershell
# Show current time
Get-Date

# Should match your system clock within a few seconds
```

**If system time is wrong:**
- Right-click clock → Adjust date/time
- Ensure "Set time automatically" is enabled

---

## ✅ Step 4: Verify Token Server Logs

When making a call, the server should log complete token details. Share this output exactly as it appears:

```
[Token Request] Raw channel: ...
[Token Generation] ...
[Token Success]
  ├─ Length: 139 characters
  ├─ First 25 chars: 007fc45bacc392b45c58b8c
  ├─ Format check: 007
  └─ Ready to use ✅
```

Look for:
- ✅ Does it show "Format check: 007" or "Format check: 006"?
- ✅ Is the token length exactly 139 characters?
- ✅ Does the first 3 characters match the format?

---

## ✅ Step 5: Enable Agora Advanced Logging

The Agora Android SDK has extensive logging. Add this to `callScreen.kt` to capture why it's rejecting the token:

```kotlin
private fun initializeAndJoinChannel() {
    try {
        // ... existing code ...
        
        // ADD THIS: Enable detailed Agora logging
        mRtcEngine?.setLogLevel(io.agora.rtc2.Constants.LOG_LEVEL_FULL)
        mRtcEngine?.setLogFileSize(512 * 1024)
        
        // ... rest of code ...
    }
}
```

This will create detailed logs that might reveal why Agora is rejecting the token.

---

## 🔬 Critical Debugging: Capture Full Token Exchange

Add this comprehensive logging to track the exact token being sent:

```kotlin
private fun joinChannelWithToken(token: String, channelName: String, uid: Int, options: ChannelMediaOptions) {
    runOnUiThread {
        try {
            // ... validation code ...
            
            // ADD THIS: Log every byte of information
            android.util.Log.d("CallScreen", "=== COMPLETE TOKEN DETAILS ===")
            android.util.Log.d("CallScreen", "Token MD5: ${android.util.Base64.encodeToString(token.toByteArray(), android.util.Base64.DEFAULT)}")
            android.util.Log.d("CallScreen", "Token length: ${token.length}")
            android.util.Log.d("CallScreen", "Channel length: ${channelName.length}")
            android.util.Log.d("CallScreen", "Channel bytes: ${channelName.toByteArray().joinToString(",") { it.toString() }}")
            android.util.Log.d("CallScreen", "UID: $uid (hex: ${uid.toString(16)})")
            
            // Log the exact raw token for analysis
            for (i in token.indices step 50) {
                val end = minOf(i + 50, token.length)
                android.util.Log.d("CallScreen", "Token[$i-$end]: ${token.substring(i, end)}")
            }
            
            val result = mRtcEngine?.joinChannel(token, channelName, uid, options)
            
            // ... rest of code ...
        }
    }
}
```

---

## 🎯 The Real Problem: Possible SDK Version Incompatibility

I suspect the issue is that the `agora-access-token` library was designed for an older SDK version.

**Check your Agora SDK version:**

1. Open Android Studio
2. Go to `app/build.gradle.kts`
3. Find the line with `io.agora.rtc2:rtc-android:X.X.X`
4. Note the version number (e.g., `4.0.0`, `4.1.0`, etc.)

**Then compare with Node library:**
- What version is `agora-access-token`? (Currently: 2.0.10)

**They need to be compatible!**

---

## 🔧 Alternative Solution: Use Agora's Official Token Generator

If the issue persists, try using Agora's recommended token generation approach. Here's a more robust version:

```javascript
// IN: D:\agora-token-server\index.js

app.get('/rtcToken', (req, res) => {
  try {
    let channel = req.query.channel;
    const uidParam = req.query.uid ?? '0';
    const roleParam = String(req.query.role || 'publisher').toLowerCase();
    const expireParam = req.query.expire ?? '3600';

    // Sanitize channel name
    const sanitizedChannel = String(channel).replace(/-/g, '_').toLowerCase();

    // Parse and validate UID - MUST be positive integer
    let uid = parseInt(uidParam, 10);
    if (isNaN(uid)) uid = 0;
    if (uid < 0) uid = uid >>> 0; // Convert to unsigned

    // Map role
    const role = roleParam === 'subscriber' ? RtcRole.SUBSCRIBER : RtcRole.PUBLISHER;

    // Parse expiration
    const expire = parseInt(expireParam, 10) || 3600;

    // CRITICAL: Use exact current time
    const now = Math.floor(Date.now() / 1000);
    const privilegeExpire = now + expire;

    console.log(`[Token Generation]`);
    console.log(`  Channel: ${sanitizedChannel}`);
    console.log(`  UID: ${uid}`);
    console.log(`  UID type: ${typeof uid} (should be: number)`);
    console.log(`  UID > 0: ${uid > 0}`);
    console.log(`  Current Unix Time: ${now}`);
    console.log(`  Token Expires At: ${privilegeExpire}`);
    console.log(`  Time Until Expiry: ${expire} seconds`);

    // Generate token
    const token = RtcTokenBuilder.buildTokenWithUid(
      APP_ID,
      APP_CERT,
      sanitizedChannel,
      uid,
      role,
      privilegeExpire
    );

    if (!token) {
      throw new Error('Token generation returned null');
    }

    console.log(`[Token Generated]`);
    console.log(`  Length: ${token.length}`);
    console.log(`  Starts with: ${token.substring(0, 10)}`);
    console.log(`  First char code: ${token.charCodeAt(0)}`);

    return res.json({
      token: token,
      channel: sanitizedChannel,
      uid: uid,
      role: roleParam,
      expiresIn: expire,
      expiresAt: new Date(privilegeExpire * 1000).toISOString(),
      generatedAt: new Date(now * 1000).toISOString()
    });

  } catch (e) {
    console.error('[Token Error]', e.message);
    console.error('[Stack]', e.stack);
    return res.status(500).json({
      error: 'Failed to create token',
      message: e.message,
      details: {
        channel: req.query.channel,
        uid: req.query.uid,
        role: req.query.role
      }
    });
  }
});
```

---

## 📋 Complete Diagnostic Checklist

Before we proceed, **please provide the following information**:

```
AGORA DASHBOARD INFO:
- [ ] Project App ID: ___________
- [ ] Project App Certificate: ___________
- [ ] Token authentication: Enabled? Yes/No
- [ ] Project type: RTC or RTM? ___________

ANDROID PROJECT INFO:
- [ ] Agora SDK version (from build.gradle.kts): ___________
- [ ] Android target SDK: ___________
- [ ] Android min SDK: ___________

TOKEN SERVER INFO:
- [ ] agora-access-token version: ___________
- [ ] Node.js version: ___________
- [ ] npm version: ___________

CURRENT ERROR:
- [ ] Error code: -102
- [ ] Channel name (at error): ___________
- [ ] UID (at error): ___________
- [ ] Token server returns 200? Yes/No
- [ ] Token length: ___________
```

---

## 🚀 Next Steps

1. **Verify SDK versions match**
   ```bash
   # Check token server library
   npm list agora-access-token
   
   # Check Android SDK (in build.gradle.kts)
   grep -r "rtc-android" E:\Mobile dev Projects\i210396\app\build.gradle.kts
   ```

2. **Run the token generation test**
   ```bash
   node D:\agora-token-server\test-token-generation.js
   ```

3. **Check system time**
   ```powershell
   Get-Date
   ```

4. **Capture full token details and share**
   - Token server console output (full)
   - Android logcat output (full)

5. **Verify Agora Dashboard credentials**
   - Login to https://console.agora.io
   - Verify App ID and Certificate match exactly

---

## 💡 If Nothing Works: Alternative Approach

If all else fails, we can try generating tokens **on the Android device itself** instead of the server. This bypasses the token server complexity entirely and might help us understand if it's a server issue or SDK issue.

But let's first complete the diagnostic checklist above and see what we find.


