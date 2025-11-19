# 🔍 Debugging Error 102 - Complete Troubleshooting Guide

## ✅ What We've Done

1. ✅ Fixed the token server endpoint URL in the app
2. ✅ Added logging to token server to see requests
3. ✅ App now fetches tokens from `/rtcToken?channel=X&uid=Y&role=publisher&expire=3600`

---

## 🚀 Complete Testing Steps

### **Step 1: Restart Token Server with New Logging**

```bash
# Stop the old server (Ctrl+C in PowerShell)
Ctrl+C

# Start the server again
PS D:\agora-token-server> npm start
Agora token server on : 8080
```

Now you should see logs when the app makes requests.

---

### **Step 2: Build and Run the App**

```bash
# In Android Studio or cmd, build the app
cd E:\Mobile dev Projects\i210396
gradlew.bat build
```

---

### **Step 3: Open 3 Terminals/Windows**

You need to monitor:

1. **Terminal 1 - Token Server Logs**
   ```
   PS D:\agora-token-server> npm start
   Agora token server on : 8080
   [Waiting for requests...]
   ```

2. **Terminal 2 - Android Logcat (from Android Studio)**
   ```
   Filter: callScreen
   [Waiting for logs...]
   ```

3. **Terminal 3 - Your Device/Emulator**
   ```
   App running on device
   [Waiting for call action...]
   ```

---

### **Step 4: Make a Test Call and Observe All 3 Terminals**

**On Your Device:**
1. Open the app
2. Go to Messages/Chat
3. Tap **📹 Video** button

**Watch Terminal 1 (Token Server):**
You should see:
```
[Token Request] Channel: call_user1_user2, UID: 1234567890, Role: publisher
[Token Success] Generated token for channel: call_user1_user2, uid: 1234567890, token length: 200
```

**Watch Terminal 2 (Android Logcat):**
You should see:
```
D/CallScreen: Fetching token from: http://10.0.2.2:8080/rtcToken?channel=call_user1_user2&uid=1234567890&role=publisher&expire=3600
D/CallScreen: Token server response code: 200
D/CallScreen: Token server raw response: {"token":"007fc45bacc392b45c58b8c0b3fc4e8b5e3..."}
D/CallScreen: ✅ Token fetched successfully!
D/CallScreen: joinChannel result: 0
D/CallScreen: onJoinChannelSuccess fired!
```

---

## 🔴 If You See Error 102 in Logcat

The error 102 means Agora rejected the token. This could happen if:

### **Scenario A: Token is being generated but Agora rejects it**

**What to check:**
1. **Verify the token is NOT empty:**
   ```
   D/CallScreen: Token fetched successfully!
   D/CallScreen: Token length: 200   ← Should be > 100
   ```

2. **The token should start with "007":**
   ```
   D/CallScreen: Token preview: 007fc45bacc392b45c58b8c0b3fc4e...   ← ✅ Correct
   D/CallScreen: Token preview: [empty or garbage]                    ← ❌ Wrong
   ```

3. **If token looks good but still error 102:**
   - This might be an Agora account configuration issue
   - Check your Agora Console settings
   - Token might have wrong privileges

---

### **Scenario B: Token server is NOT responding**

**What to check:**

If you see in Logcat:
```
E/CallScreen: Failed to fetch token: HTTP 404
E/CallScreen: Exception fetching token: Connection refused
```

**And Terminal 1 shows NOTHING**, then:
- Token server isn't receiving requests
- Network issue between app and server
- Try using `localhost:8080` instead of `10.0.2.2:8080`

---

### **Scenario C: Token server error**

**What to check in Terminal 1:**

If you see:
```
[Token Error] Missing APP_ID or APP_CERT
[Token Exception] Error: ...
```

Then the token server itself has an issue. Restart it:
```bash
npm stop
npm start
```

---

## 📋 Step-by-Step Debugging Checklist

After making a call, verify in order:

### **Terminal 1 (Token Server):**
- [ ] Do you see `[Token Request]` log? If NO → network issue
- [ ] Do you see `[Token Success]`? If NO → token generation failed
- [ ] Is token length > 100? If NO → invalid token

### **Terminal 2 (Logcat):**
- [ ] `Token server response code: 200`? If not → server error
- [ ] `✅ Token fetched successfully!`? If NO → parsing error
- [ ] `joinChannel result: 0`? If 102 → token rejected by Agora

### **Your Phone:**
- [ ] Call screen appears?
- [ ] Status shows "Calling..."?
- [ ] Error message appears? If YES, read it carefully

---

## 🎯 Most Likely Solution

If you're getting error 102 **after the token is fetched successfully**, the issue might be:

### **Solution 1: Agora Account Settings**

Your Agora Console might require additional setup:

1. Go to https://console.agora.io
2. Check your project settings
3. Look for "Certificate" or "Token" settings
4. Ensure token authentication is properly configured

### **Solution 2: Use UID = 0**

Try changing the app to use UID = 0 instead of a hash:

In `callScreen.kt`, change:
```kotlin
// OLD (hash-based)
val uid = otherUserId.hashCode() and 0x7FFFFFFF

// NEW (use 0 to let Agora assign)
val uid = 0
```

Token server already supports `uid=0`, and this tells Agora to auto-assign a UID.

### **Solution 3: Check if you need to use the other endpoint**

Try using the REST endpoint instead:
```
http://10.0.2.2:8080/rtc/call_user1_user2/uid/0
```

---

## 🧪 Alternative Quick Test

**Test your token server directly from your browser:**

Open this URL in your browser while token server is running:
```
http://localhost:8080/rtcToken?channel=testchannel&uid=0&role=publisher&expire=3600
```

You should see:
```json
{
  "token": "007fc45bacc392b45c58b8c0b3fc4e8b5e3..."
}
```

If you don't see a token, the token server has an issue.

---

## 📞 Information I Need to Help Further

Please provide:

1. **Token Server Terminal Output** when you make a call:
   ```
   Copy/paste the [Token Request] and [Token Success] lines
   ```

2. **Android Logcat Output**:
   ```
   Copy/paste from "Fetching token from:" to "joinChannel result:"
   ```

3. **Error Message** shown on device (if any)

4. **Your Agora App ID** (already have: `fc45bacc392b45c58b8c0b3fc4e8b5e3`)

---

## ✅ Next Steps

1. **Restart token server** with new logging code
2. **Rebuild app** with latest code
3. **Make a test call** and observe all 3 terminals
4. **Copy the exact logs** and share them with me
5. I can then pinpoint the exact issue

**The new logging will tell us exactly where the problem is!** 🔍

