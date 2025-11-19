# Firebase Service Account Setup Guide

## Step-by-Step Instructions

### 1. Download Service Account JSON

You're currently on the correct page in Firebase Console. Here's what to do:

**Click the "Generate new private key" button** ✅

This will:
- Download a JSON file (something like `smdassignments-firebase-adminsdk-xxxxx-xxxxxxxxxx.json`)
- This file contains your private key and credentials

### 2. Rename and Place the File

After downloading:

1. **Rename the file** to: `firebase-service-account.json`
2. **Move it** to: `E:\Mobile dev Projects\i210396\instagram_api\config\firebase-service-account.json`

### 3. Find Your Project ID

From the Admin SDK snippet you shared, your **Project ID** appears to be: `smdassignments`

(You can verify this in Firebase Console → Project Settings → General tab → Project ID)

### 4. Update FCMNotification.php

Open: `E:\Mobile dev Projects\i210396\instagram_api\utils\FCMNotification.php`

Change line 4 from:
```php
private static $projectId = 'YOUR_FIREBASE_PROJECT_ID';
```

To:
```php
private static $projectId = 'smdassignments';
```

### 5. Verify the Service Account Path

The file path in `FCMNotification.php` should be:
```php
private static $serviceAccountPath = __DIR__ . '/../config/firebase-service-account.json';
```

This is already correct! ✅

### 6. Final File Structure

Your folder structure should look like:
```
E:\Mobile dev Projects\i210396\
└── instagram_api\
    ├── config\
    │   ├── config.php
    │   ├── Database.php
    │   └── firebase-service-account.json  ← YOUR DOWNLOADED FILE (renamed)
    └── utils\
        ├── FCMNotification.php
        └── JWT.php
```

## Security Warning ⚠️

**IMPORTANT:** The `firebase-service-account.json` file contains sensitive credentials!

- ❌ **DO NOT** commit it to Git/GitHub
- ❌ **DO NOT** share it publicly
- ✅ **DO** add it to `.gitignore`

### Add to .gitignore:

Create or update `instagram_api/.gitignore`:
```
# Firebase Service Account (Sensitive!)
config/firebase-service-account.json
```

## What the JSON File Contains

The downloaded file will look something like:
```json
{
  "type": "service_account",
  "project_id": "smdassignments",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-xxxxx@smdassignments.iam.gserviceaccount.com",
  "client_id": "...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "..."
}
```

This is what the PHP code uses to authenticate with Firebase.

## Testing After Setup

Once you've completed the setup:

1. **Test the API manually:**
   - Try sending a message between users
   - Check server logs for any FCM errors

2. **Check Logcat:**
   - Look for "FCM token updated successfully"
   - Look for any Firebase authentication errors

3. **Verify Database:**
   ```sql
   SELECT id, username, fcm_token FROM users LIMIT 5;
   ```
   - Make sure FCM tokens are being saved

## Common Issues

### "Service account file not found"
- Verify the file is at: `instagram_api/config/firebase-service-account.json`
- Check file name is exactly: `firebase-service-account.json`

### "Failed to get FCM access token"
- Check the JSON file is valid (not corrupted)
- Verify project_id in JSON matches your Firebase project

### "Permission denied"
- Make sure file permissions allow PHP to read it
- On Windows, check file isn't read-only

## Summary

✅ **Action:** Click "Generate new private key" button  
✅ **Rename:** Downloaded file to `firebase-service-account.json`  
✅ **Move:** To `instagram_api/config/` folder  
✅ **Update:** Project ID in `FCMNotification.php` to `smdassignments`  
✅ **Secure:** Add to `.gitignore`  

That's it! Your push notifications will be ready to work! 🚀

