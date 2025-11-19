# 🔧 Agora Error -102 FIX - Token Server Complete Solution

## ✅ Problem Analysis

Your logs show:
```
D/CallScreen: Token fetched successfully!
D/CallScreen: Token length: 139
D/CallScreen: Token preview: 006fc45bacc392b45c58b8c0b3fc4e8b5e3IACi5Shx4/T0EzX...
D/CallScreen: joinChannel result: 0
E/CallScreen: joinChannel failed with error code: -102
```

**Error -102 means:** "Invalid token or credentials mismatch"

---

## 🎯 Root Causes (in order of likelihood)

### 1. ❌ Token Format Issue (MOST LIKELY)
The `agora-access-token` v2.0.x library generates tokens, but the Agora Android SDK may expect a different format.

### 2. ❌ UID Mismatch
Token generated with one UID, but `joinChannel` called with different UID

### 3. ❌ APP_ID or APP_CERT Incorrect
Credentials don't match between server and SDK

### 4. ❌ Token Expiration
Token generated but already expired before use

---

## ✅ COMPLETE FIX - Step by Step

### STEP 1: Update Token Server with Agora SDK v3+ Compatible Token Generation

The fix uses the correct token structure for modern Agora SDK. Replace your `index.js`:

```javascript
require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { RtcTokenBuilder, RtcRole } = require('agora-access-token');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 8080;
const APP_ID = process.env.AGORA_APP_ID || "fc45bacc392b45c58b8c0b3fc4e8b5e3";
const APP_CERT = process.env.AGORA_APP_CERT || "0708667746bd4b8eb95ad1105e4b56fe";

console.log('[Server Start] Agora Token Server Configuration');
console.log(`[Config] APP_ID: ${APP_ID ? APP_ID.substring(0, 8) + '...' : 'MISSING'}`);
console.log(`[Config] APP_CERT: ${APP_CERT ? APP_CERT.substring(0, 8) + '...' : 'MISSING'}`);

if (!APP_ID || !APP_CERT) {
  console.error('[CRITICAL ERROR] Missing AGORA_APP_ID or AGORA_APP_CERT');
  process.exit(1);
}

// Health check
app.get('/', (_req, res) => {
  res.json({ status: 'running', version: '1.0.0', timestamp: new Date().toISOString() });
});

/**
 * Main token endpoint
 * GET /rtcToken?channel=testRoom&uid=123&role=publisher&expire=3600
 */
app.get('/rtcToken', (req, res) => {
  try {
    const channel = req.query.channel;
    const uidParam = req.query.uid ?? '0';
    const roleParam = String(req.query.role || 'publisher').toLowerCase();
    const expireParam = req.query.expire ?? '3600';

    console.log(`\n[Token Request] Channel: ${channel}, UID: ${uidParam}, Role: ${roleParam}, Expire: ${expireParam}s`);

    // Validation
    if (!channel) {
      console.warn('[Token Error] Missing channel parameter');
      return res.status(400).json({ error: 'channel is required' });
    }

    // Parse and validate UID
    let uid = parseInt(uidParam, 10);
    if (isNaN(uid) || uid < 0) {
      console.warn(`[Token Error] Invalid UID: ${uidParam}`);
      return res.status(400).json({ error: 'uid must be a non-negative integer' });
    }

    // Map role
    const role = roleParam === 'subscriber' ? RtcRole.SUBSCRIBER : RtcRole.PUBLISHER;

    // Parse expiration
    const expire = parseInt(expireParam, 10);
    if (isNaN(expire) || expire < 60) {
      console.warn(`[Token Error] Invalid expire time: ${expireParam}`);
      return res.status(400).json({ error: 'expire must be >= 60 seconds' });
    }

    // Calculate expiration timestamp
    const now = Math.floor(Date.now() / 1000);
    const privilegeExpire = now + expire;

    console.log(`[Token Generation]`);
    console.log(`  ├─ APP_ID: ${APP_ID.substring(0, 8)}...`);
    console.log(`  ├─ APP_CERT: ${APP_CERT.substring(0, 8)}...`);
    console.log(`  ├─ Channel: ${channel}`);
    console.log(`  ├─ UID: ${uid}`);
    console.log(`  ├─ Role: ${role === RtcRole.PUBLISHER ? 'PUBLISHER' : 'SUBSCRIBER'}`);
    console.log(`  ├─ Current Time (unix): ${now}`);
    console.log(`  ├─ Expire Time (unix): ${privilegeExpire}`);
    console.log(`  └─ TTL: ${expire} seconds`);

    // Generate token - CRITICAL: Must use buildTokenWithUid for SDK v3+
    const token = RtcTokenBuilder.buildTokenWithUid(
      APP_ID,
      APP_CERT,
      channel,
      uid,                // Must match joinChannel UID
      role,
      privilegeExpire
    );

    if (!token || token.length === 0) {
      console.error('[Token Error] Token generation returned empty');
      return res.status(500).json({ error: 'Failed to generate token' });
    }

    console.log(`[Token Success]`);
    console.log(`  ├─ Length: ${token.length} characters`);
    console.log(`  ├─ Prefix: ${token.substring(0, 25)}...`);
    console.log(`  ├─ Format: RTC v2 (starts with "007" or "006")`);
    console.log(`  └─ Ready to use ✅`);

    return res.json({
      token: token,
      channel: channel,
      uid: uid,
      role: roleParam,
      expiresIn: expire,
      expiresAt: new Date(privilegeExpire * 1000).toISOString()
    });

  } catch (e) {
    console.error('[Token Exception]', e.message);
    console.error('[Token Stack]', e.stack);
    return res.status(500).json({ error: 'Failed to create token', message: e.message });
  }
});

/**
 * Alternative endpoint format
 * GET /rtc/:channel/publisher/:role/userAccount/:uid?expire=3600
 */
app.get('/rtc/:channel/publisher/:role/userAccount/:uid', (req, res) => {
  console.log(`[REST Endpoint] Channel: ${req.params.channel}, UID: ${req.params.uid}, Role: ${req.params.role}`);
  
  const query = new URLSearchParams({
    channel: req.params.channel,
    uid: req.params.uid,
    role: req.params.role,
    expire: req.query.expire || '3600'
  });

  req.query = Object.fromEntries(query);
  const handler = app._router.stack.find(layer => layer.route && layer.route.path === '/rtcToken');
  if (handler) {
    handler.handle(req, res);
  } else {
    return res.status(500).json({ error: 'Internal routing error' });
  }
});

/**
 * Legacy endpoint
 * GET /rtc/:channel/uid/:uid
 */
app.get('/rtc/:channel/uid/:uid', (req, res) => {
  console.log(`[Legacy Endpoint] Channel: ${req.params.channel}, UID: ${req.params.uid}`);
  req.query.channel = req.params.channel;
  req.query.uid = req.params.uid;
  req.query.role = req.query.role || 'publisher';
  req.query.expire = req.query.expire || '3600';
  
  const handler = app._router.stack.find(layer => layer.route && layer.route.path === '/rtcToken');
  if (handler) {
    handler.handle(req, res);
  } else {
    return res.status(500).json({ error: 'Internal routing error' });
  }
});

// Error handler
app.use((err, req, res, next) => {
  console.error('[Middleware Error]', err);
  res.status(500).json({ error: 'Server error', message: err.message });
});

// Start server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`\n${'='.repeat(60)}`);
  console.log(`[Server Ready] Agora Token Server is running! 🚀`);
  console.log(`${'='.repeat(60)}`);
  console.log(`📍 Server: http://0.0.0.0:${PORT}`);
  console.log(`📍 Health Check: http://localhost:${PORT}/`);
  console.log(`📍 Token Endpoint: http://localhost:${PORT}/rtcToken?channel=test&uid=123&role=publisher&expire=3600`);
  console.log(`📍 Alt Endpoint: http://localhost:${PORT}/rtc/test/publisher/publisher/userAccount/123`);
  console.log(`${'='.repeat(60)}\n`);
});
```

### STEP 2: Verify Node Modules Are Installed

```bash
# Navigate to token server
cd D:\agora-token-server

# Delete old node_modules (clean install)
rmdir /s /q node_modules

# Reinstall with correct versions
npm install

# Verify installation
npm list
```

### STEP 3: Update .env File

Ensure your `.env` file has these exact values (no quotes):

```
AGORA_APP_ID=fc45bacc392b45c58b8c0b3fc4e8b5e3
AGORA_APP_CERT=0708667746bd4b8eb95ad1105e4b56fe
PORT=8080
NODE_ENV=development
```

### STEP 4: Test the Token Server Locally

Before running the app, test your token server manually:

**PowerShell Command:**
```powershell
$response = Invoke-WebRequest -Uri "http://localhost:8080/rtcToken?channel=test_call&uid=12345&role=publisher&expire=3600"
$response.Content | ConvertFrom-Json | Format-List
```

**Expected Output:**
```
token  : 007fc45bacc392b45c58b8c0b3fc4e8b5e3IADi5Shx4/T0EzXoJdszFhef8c6...
channel: test_call
uid    : 12345
role   : publisher
expiresIn: 3600
expiresAt: 2025-11-20T01:50:00Z
```

### STEP 5: Verify Android App UID Generation

The Android app must generate a consistent UID. Check this in `callScreen.kt`:

```kotlin
// This is correct - generates positive integer from user ID
val uid = otherUserId.hashCode() and 0x7FFFFFFF

// IMPORTANT: Log it to verify
android.util.Log.d("CallScreen", "Generated UID: $uid (should be positive integer)")
```

### STEP 6: Run the App

**Build and Test:**
```bash
cd E:\Mobile dev Projects\i210396
gradlew.bat clean build
gradlew.bat installDebug
```

**Monitor Logcat:**
```
D/CallScreen: === TOKEN FETCH FROM SERVER ===
D/CallScreen: Fetching token from: http://10.0.2.2:8080/rtcToken?channel=...&uid=12345...
D/CallScreen: Token server response code: 200
D/CallScreen: ✅ Token fetched successfully!
D/CallScreen: Attempting to join channel: ... with UID: 12345
D/CallScreen: joinChannel result: 0
D/CallScreen: joinChannel call successful, waiting for callback...
D/CallScreen: onJoinChannelSuccess fired!  ← ✅ SUCCESS
```

---

## 🚨 Troubleshooting If Still Getting Error -102

### Check 1: Verify Token Format
```javascript
// In index.js, add this debug output:
console.log(`[Token Format Check]`);
console.log(`  - Starts with "007" or "006": ${token.substring(0, 3)}`);
console.log(`  - Contains APP_ID: ${token.includes(APP_ID.substring(0, 8))}`);
console.log(`  - Length > 100: ${token.length > 100}`);
```

### Check 2: Verify UID Consistency
In `callScreen.kt`, add this logging:
```kotlin
android.util.Log.d("CallScreen", "=== UID VERIFICATION ===")
android.util.Log.d("CallScreen", "otherUserId: $otherUserId")
android.util.Log.d("CallScreen", "UID generated: $uid")
android.util.Log.d("CallScreen", "UID type: ${uid.javaClass.simpleName}")
android.util.Log.d("CallScreen", "UID is positive: ${uid > 0}")
```

### Check 3: Verify Credentials
```bash
# Check if credentials are loaded in token server
# Look for this in console output when server starts:
# [Config] APP_ID: fc45bacc...
# [Config] APP_CERT: 07086677...

# If you see "MISSING", then .env file is not loaded!
```

### Check 4: Agora Dashboard Verification
1. Go to https://console.agora.io
2. Login to your project
3. Verify:
   - ✅ App ID matches: `fc45bacc392b45c58b8c0b3fc4e8b5e3`
   - ✅ Certificate matches: `0708667746bd4b8eb95ad1105e4b56fe`
   - ✅ Token authentication is ENABLED
   - ✅ Project uses RTC (not RTM)

---

## 📋 Files Modified/Created

1. ✅ `D:\agora-token-server\index.js` - Updated with better error handling
2. ✅ `D:\agora-token-server\.env` - Created with credentials
3. ✅ `D:\agora-token-server\package.json` - Created with dependencies
4. ✅ `e:\Mobile dev Projects\i210396\app\src\main\java\com\devs\i210396_i211384\callScreen.kt` - Already correct (token fetching logic)

---

## ✅ Final Verification Checklist

Before making the call:

- [ ] Token server is running (`npm start`)
- [ ] No errors in token server console
- [ ] Test endpoint returns valid token (see Check 1)
- [ ] Android app has latest code
- [ ] Emulator has internet access to `10.0.2.2:8080`
- [ ] Permissions granted (Camera + Microphone)
- [ ] Agora credentials in .env match Agora Dashboard
- [ ] Logcat shows token received successfully
- [ ] `joinChannel` returns 0 (not -102)
- [ ] `onJoinChannelSuccess` callback fires

If all checks pass → **Call should work! 🎉**

---

## 🔗 Next Steps

1. Apply all changes above
2. Rebuild and test
3. Monitor console output for errors
4. If still failing, share:
   - Token server console output (when generating token)
   - Android logcat (when making call)
   - App ID and Certificate from Agora Dashboard

