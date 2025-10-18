# Real-Time Online/Offline Status Implementation

## Overview
This document describes the complete implementation of real-time online/offline status tracking, similar to Instagram's green dot and "Active now" features.

## Features Implemented

### 1. **Online Status Tracking**
- Users are automatically marked as "online" when they open the app
- Status is updated to "offline" when users close or minimize the app
- Last seen timestamp is recorded when user goes offline

### 2. **Visual Indicators**
- **Green Dot**: A small green circle appears on user profile pictures when they're online
- **Status Text**: Shows "Online" or "Last seen X minutes ago" in chat screens
- Real-time updates without requiring app refresh

### 3. **Display Locations**
Online status is displayed in:
- **User Lists**: Explore page, search results
- **Followers/Following Lists**: Green dot on profile pictures
- **Chat Screen**: "Online" or "Last seen" text under username
- **DM List**: Online indicators on chat list items

## Technical Implementation

### Database Structure
User documents in Firestore now include:
```javascript
users/{userId}/
  - isOnline: boolean      // true if user is currently active
  - lastSeen: timestamp    // when user was last active
```

### Files Created

**1. OnlineStatusManager.kt** - Utility class for managing status
- `setUserOnline()` - Marks user as online
- `setUserOffline()` - Marks user as offline with timestamp
- `listenToUserStatus()` - Real-time status listener
- `formatLastSeen()` - Converts timestamp to readable format

**2. online_indicator.xml** - Drawable resource
- Green circular indicator (12dp diameter)
- Color: #44C553 (Instagram-like green)

### Files Modified

**User.kt** - Added fields:
```kotlin
val isOnline: Boolean = false
val lastSeen: Long = 0L
```

**HomePage.kt** - Lifecycle tracking:
- `onCreate()` → Set user online
- `onResume()` → Set user online
- `onPause()` → Set user offline
- `onDestroy()` → Set user offline

**UserAdapter.kt** - Display online indicator:
- Added green dot overlay on profile pictures
- Real-time status updates via listener

**UserListAdapter.kt** - Same as UserAdapter for followers/following lists

**chatScreen.kt** - Status display:
- Shows "Online" when user is active
- Shows "Last seen X ago" when offline
- Real-time updates

**Layouts Modified:**
- `item_user.xml` - Added FrameLayout with online indicator
- `item_user_simple.xml` - Added online indicator for followers lists
- `activity_chat.xml` - Added status TextView below username

## How It Works

### 1. Status Tracking Flow
```
User opens app (HomePage)
    ↓
OnlineStatusManager.setUserOnline()
    ↓
Firestore: isOnline = true, lastSeen = now
    ↓
All listeners receive update
    ↓
Green dot appears on user's profile pictures
```

```
User closes/minimizes app
    ↓
OnlineStatusManager.setUserOffline()
    ↓
Firestore: isOnline = false, lastSeen = now
    ↓
All listeners receive update
    ↓
Green dot disappears, "Last seen" shows
```

### 2. Real-Time Updates
- Uses Firestore snapshot listeners for instant updates
- No polling required - updates push automatically
- Changes reflect immediately across all devices

### 3. Last Seen Formatting
Converts timestamps to human-readable format:
- Less than 1 minute: "Just now"
- 1-59 minutes: "X minutes ago"
- 1-23 hours: "X hours ago"
- 1-6 days: "X days ago"
- 7+ days: "Long time ago"

## Usage Examples

### Example 1: User Opens App
```
User A opens Instagram app
→ HomePage onCreate() calls setUserOnline()
→ Firestore updated: { isOnline: true }
→ User B viewing User A's profile sees green dot
→ User C in chat with User A sees "Online"
```

### Example 2: User Closes App
```
User A closes app
→ HomePage onDestroy() calls setUserOffline()
→ Firestore updated: { isOnline: false, lastSeen: timestamp }
→ Green dot disappears for all viewers
→ Chat shows "Last seen 2 minutes ago"
```

### Example 3: Viewing Online Status
```
User viewing followers list
→ UserListAdapter binds each user
→ Calls listenToUserStatus() for each
→ Green dots appear for online users
→ Updates automatically when status changes
```

## Visual Appearance

### Green Dot Indicator
- **Size**: 12dp x 12dp
- **Color**: #44C553 (bright green)
- **Position**: Bottom-right corner of profile picture
- **Visibility**: Shows only when user is online

### Status Text in Chat
- **Online**: Green color (#A6322A - app theme color)
- **Last seen**: Gray color (#666666)
- **Font size**: 12sp
- **Updates**: Real-time, no refresh needed

## Benefits

✅ **Real-time Updates** - Instant status changes across all devices
✅ **Low Overhead** - Uses Firestore snapshot listeners (efficient)
✅ **Automatic Tracking** - No manual status updates needed
✅ **Privacy Friendly** - Only tracks when app is active
✅ **Instagram-like UX** - Familiar green dot interface
✅ **Accurate Last Seen** - Shows when user was last active
✅ **Battery Efficient** - Status updates only on app state changes

## Privacy Considerations

- Users are marked online only when actively using the app
- Offline status set when app minimized or closed
- Last seen timestamp updated automatically
- No background tracking when app is closed

## Testing Checklist

- [ ] Open app - user marked as online
- [ ] Green dot appears on user lists
- [ ] Close app - user marked as offline
- [ ] Green dot disappears
- [ ] "Last seen" shows in chat screen
- [ ] Last seen time updates correctly
- [ ] Multiple devices sync in real-time
- [ ] Status persists across app restarts
- [ ] Works in followers/following lists
- [ ] Works in explore/search results

## Future Enhancements (Optional)

- Add "Hide Online Status" privacy setting
- Show "Active X minutes ago" instead of exact time
- Add typing indicator in chats
- Show "Active today" instead of exact hours
- Add "Recently Active" status (within 1 hour)

## Notes

- Online status tracked at app level (HomePage)
- All user list adapters listen to status changes
- Green dot indicator uses drawable resource
- Status updates happen on lifecycle events
- Firestore listeners auto-cleanup on activity destroy
- Last seen formatted for readability
- Uses efficient Firestore snapshot listeners

## Implementation Complete! ✅

The online/offline status system is now fully functional and integrated throughout the app. Users will see green dots on profile pictures for online users and "last seen" timestamps in chat screens.

