# Agora Video & Audio Calls Implementation

## Overview
This document describes the implementation of real-time video and audio calls using the Agora RTC SDK integrated with Firebase for call management and FCM for notifications.

## Agora Configuration

### Credentials
- **App ID**: `09999ad42b32a41002db5a46c39d025bb3f38e0bd9a9a11be930c05a8a5ce3d7`
- **Server Secret**: `434962a4ac1fc962e9854fa5aa579c94`
- **Callback Secret**: `09999ad42b32a41002db5a46c39d025b`
- **Server URL**: `wss://webliveroom460418059-api.coolzcloud.com/ws`

These credentials are configured in `AgoraConfig.kt`.

## Architecture

### Components

1. **CallActivity**: Initiates outgoing calls
2. **IncomingCallActivity**: Handles incoming call notifications
3. **callScreen**: Main call interface with video/audio controls
4. **CallService**: Firebase Realtime Database integration for call management
5. **MyFirebaseMessagingService**: FCM notification handler for incoming calls
6. **NotificationHelper**: Sends call notifications to users

## Call Flow

### Outgoing Call Flow
1. User clicks video/audio call button in chat screen
2. `CallActivity` is launched with `userId` and `callType`
3. System generates unique `callId` and `channelName`
4. Call request is saved to Firebase Realtime Database
5. FCM notification is sent to receiver
6. Caller joins Agora channel and `callScreen` is displayed

### Incoming Call Flow
1. Receiver receives FCM notification
2. `IncomingCallActivity` is launched (full-screen notification)
3. User can accept or reject the call
4. If accepted, receiver joins same Agora channel
5. Both users are connected in `callScreen`

### Call Termination
1. Either user clicks end call button
2. Call status is updated to "ended" in Firebase
3. Agora channel is left
4. Call record is removed from Firebase

## Firebase Database Structure

```
calls/
  └── {callId}/
      ├── callId: String
      ├── callerId: String
      ├── callerName: String
      ├── callerImageUrl: String
      ├── receiverId: String
      ├── callType: String (video/audio)
      ├── channelName: String
      ├── status: String (ringing/accepted/rejected/ended)
      └── timestamp: Long
```

## Features Implemented

### Call Features
- ✅ Video calls with real-time video streaming
- ✅ Audio-only calls
- ✅ Mute/unmute microphone
- ✅ Speaker on/off toggle
- ✅ Switch camera (front/back)
- ✅ Toggle video on/off during video calls
- ✅ Call duration timer
- ✅ End call functionality

### Notification Features
- ✅ Full-screen incoming call notification
- ✅ Ringtone playback for incoming calls
- ✅ Call rejection handling
- ✅ Call cancellation detection
- ✅ Automatic notification dismissal

### Permission Handling
- ✅ Camera permission request
- ✅ Microphone permission request
- ✅ Runtime permission handling
- ✅ Graceful handling of denied permissions

## Permissions Required

The following permissions are declared in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

## Usage

### Initiating a Call from Chat Screen

```kotlin
// Video Call
val intent = Intent(this, CallActivity::class.java).apply {
    putExtra("userId", receiverUserId)
    putExtra("callType", "video")
}
startActivity(intent)

// Audio Call
val intent = Intent(this, CallActivity::class.java).apply {
    putExtra("userId", receiverUserId)
    putExtra("callType", "audio")
}
startActivity(intent)
```

### Handling Incoming Calls

Incoming calls are automatically handled by the FCM service. When a notification is received with type "incoming_call", the `IncomingCallActivity` is launched.

## Agora SDK Integration

### Dependencies
```kotlin
implementation("io.agora.rtc:full-sdk:4.3.0")
implementation("com.google.firebase:firebase-database:20.3.0")
implementation("com.google.firebase:firebase-messaging:23.4.0")
```

### Channel Configuration
- **Profile**: CHANNEL_PROFILE_COMMUNICATION (optimized for calls)
- **Role**: CLIENT_ROLE_BROADCASTER (both users can send/receive)
- **Audio Route**: Speaker by default for video calls

## Security Considerations

### Token Authentication
Currently, the implementation uses null tokens for development. For production:

1. **Implement Backend Token Generation**
   - Create a backend endpoint that generates Agora tokens
   - Use the APP_CERTIFICATE to sign tokens
   - Set appropriate expiration times

2. **Update callScreen.kt**
   ```kotlin
   // Replace null with actual token
   mRtcEngine?.joinChannel(token, channelName, 0, options)
   ```

### Best Practices
- Never expose APP_CERTIFICATE in client code (move to backend)
- Implement token refresh mechanism
- Add call history logging
- Implement call quality metrics
- Add network quality indicators

## Testing

### Testing Calls
1. Build and install app on two devices
2. Log in with different accounts
3. Navigate to chat screen
4. Click video/audio call button
5. Accept call on receiving device
6. Test all controls (mute, speaker, camera switch, video toggle)
7. End call from either device

### Testing Notifications
1. Ensure FCM tokens are properly stored in Firestore
2. Test call notifications when app is in foreground
3. Test call notifications when app is in background
4. Test call notifications when app is killed
5. Verify full-screen notification on Android 10+

## Troubleshooting

### Common Issues

1. **No Video/Audio**
   - Check camera/microphone permissions
   - Verify Agora APP_ID is correct
   - Check network connectivity
   - Ensure both devices are on same channel

2. **Notifications Not Received**
   - Verify FCM token is saved in Firestore
   - Check notification permissions
   - Verify notification payload format
   - Check Firebase Cloud Messaging setup

3. **Call Not Connecting**
   - Verify Firebase Realtime Database rules
   - Check channel name generation
   - Ensure Agora project is active
   - Verify network connectivity

4. **Echo or Audio Feedback**
   - Ensure only one device plays through speaker
   - Check audio route settings
   - Test with headphones

## Future Enhancements

### Recommended Features
- [ ] Group video calls (multi-party)
- [ ] Screen sharing
- [ ] Call recording
- [ ] Virtual backgrounds
- [ ] Call quality indicators
- [ ] Network quality monitoring
- [ ] Call history storage
- [ ] Missed call notifications
- [ ] Busy status handling
- [ ] Call waiting/hold functionality
- [ ] Beauty filters
- [ ] Noise suppression
- [ ] Echo cancellation settings

## References

- [Agora Video SDK Documentation](https://docs.agora.io/en/video-calling/overview/product-overview)
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)
- [Firebase Realtime Database](https://firebase.google.com/docs/database)

## Support

For issues related to:
- **Agora SDK**: Check Agora Console and documentation
- **Firebase**: Verify Firebase project configuration
- **App-specific**: Check logcat for detailed error messages

## Version History

- **v1.0.0**: Initial implementation with video/audio calls
  - Basic call functionality
  - FCM notifications
  - Firebase integration
  - Call controls (mute, speaker, camera)

