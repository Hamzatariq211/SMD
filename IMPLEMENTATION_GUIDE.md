# Instagram Clone - Firebase to MySQL Migration Guide

## 🎯 Project Overview

This guide will help you migrate your Instagram Clone app from Firebase to MySQL backend with PHP RESTful APIs.

## 📋 Prerequisites

- XAMPP installed (Apache + MySQL)
- Android Studio
- Basic knowledge of PHP and MySQL
- Git for version control

## 🚀 Quick Start Guide

### Step 1: Database Setup

1. **Start XAMPP**
   - Open XAMPP Control Panel
   - Start Apache and MySQL services

2. **Create Database**
   - Open phpMyAdmin: http://localhost/phpmyadmin
   - Click on "SQL" tab
   - Copy the entire content from `database/schema.sql`
   - Paste and click "Go"
   - Database `instagram_clone` will be created with all tables

3. **Verify Tables**
   - You should see 15 tables created:
     - users, posts, post_likes, comments
     - stories, story_views
     - follows, follow_requests
     - chat_rooms, messages
     - notifications, call_history
     - offline_queue, media_files, user_sessions

### Step 2: Backend Setup

1. **Copy API Files**
   ```
   Copy the instagram_api folder to:
   C:\xampp\htdocs\instagram_api\
   ```

2. **Verify Directory Structure**
   ```
   htdocs/
   └── instagram_api/
       ├── config/
       │   ├── config.php
       │   └── Database.php
       ├── utils/
       │   └── JWT.php
       ├── api/
       │   ├── auth/
       │   │   ├── signup.php
       │   │   ├── login.php
       │   │   └── logout.php
       │   ├── users/
       │   ├── posts/
       │   ├── stories/
       │   ├── follow/
       │   ├── messages/
       │   └── notifications/
       └── uploads/ (auto-created)
   ```

3. **Test Backend**
   - Open browser: http://localhost/instagram_api/api/auth/login.php
   - You should see: `{"error":"Method not allowed"}` (This is correct - it expects POST)

### Step 3: Android App Configuration

1. **Update API URL**
   - File: `app/src/main/java/com/devs/i210396_i211384/network/ApiService.kt`
   - For Emulator: `http://10.0.2.2/instagram_api/`
   - For Real Device: `http://YOUR_COMPUTER_IP/instagram_api/`

2. **Find Your Computer IP (for real device)**
   ```
   Windows: ipconfig
   Look for: IPv4 Address (e.g., 192.168.1.100)
   ```

3. **Build Project**
   - Clean Project: Build > Clean Project
   - Rebuild Project: Build > Rebuild Project
   - Run on Emulator or Device

### Step 4: Testing

1. **Test Signup**
   - Open app > Register new account
   - Fill in details and signup
   - Check MySQL database for new user entry

2. **Test Login**
   - Login with registered credentials
   - Should navigate to EditProfile or HomePage

3. **Test Profile Update**
   - Update profile details
   - Check database for updated information

## 📊 Database Schema Overview

### Core Tables

**users**
- User authentication and profile data
- Stores: email, password_hash, username, profile info
- Tracks: online status, FCM tokens

**posts**
- User posts with images (Base64)
- Tracks: likes count, comments count
- Indexed by user_id and created_at

**stories**
- 24-hour expiring content
- Auto-deleted via stored procedure
- Tracks view count

**messages**
- Chat messages between users
- Supports: text, image, video, file
- Features: edit/delete within 5 minutes, vanish mode

**follows & follow_requests**
- Follow relationships
- Pending requests for private accounts

**notifications**
- Push notification data
- Types: like, comment, follow, message, etc.

## 🔄 Migration Checklist

### Already Completed ✅
- [x] Database schema created
- [x] PHP backend APIs created
- [x] JWT authentication implemented
- [x] ApiService.kt updated
- [x] SessionManager configured
- [x] Login/Signup migrated
- [x] Profile management migrated

### To Be Migrated 🔧

**Posts & Feed**
- [ ] Update `HomePage.kt` to fetch posts from MySQL API
- [ ] Update `AddPostScreen.kt` to upload via API
- [ ] Update like/comment functionality

**Stories**
- [ ] Update `UploadStory.kt` to use MySQL API
- [ ] Update `Story.kt` to fetch from API
- [ ] Update `ViewStoryActivity.kt`

**Messaging**
- [ ] Update `Messages.kt` to use MySQL API
- [ ] Update `chatScreen.kt` for message operations
- [ ] Implement edit/delete with 5-minute window
- [ ] Implement vanish mode

**Follow System**
- [ ] Update `FollowRequestsActivity.kt`
- [ ] Update follow/unfollow buttons in profiles
- [ ] Implement private account logic

**Search & Explore**
- [ ] Update `ExploreSearch.kt` to use API
- [ ] Update `Explore.kt`

**Notifications**
- [ ] Update FCM token registration
- [ ] Fetch notifications from MySQL

**Online Status**
- [ ] Update `OnlineStatusManager.kt`
- [ ] Call updateStatus API on app events

## 📝 Code Examples

### Example 1: Fetching Posts Feed

```kotlin
// In HomePage.kt
lifecycleScope.launch {
    try {
        val response = withContext(Dispatchers.IO) {
            apiService.getPostsFeed(page = 1)
        }
        
        if (response.isSuccessful) {
            val posts = response.body() ?: emptyList()
            // Update RecyclerView adapter
            postsAdapter.submitList(posts)
        }
    } catch (e: Exception) {
        Toast.makeText(this@HomePage, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
```

### Example 2: Creating a Post

```kotlin
// In AddPostScreen.kt
val request = CreatePostRequest(
    imageBase64 = imageBase64String,
    caption = captionText,
    location = locationText
)

lifecycleScope.launch {
    try {
        val response = withContext(Dispatchers.IO) {
            apiService.createPost(request)
        }
        
        if (response.isSuccessful) {
            Toast.makeText(this@AddPostScreen, "Post created!", Toast.LENGTH_SHORT).show()
            finish()
        }
    } catch (e: Exception) {
        Toast.makeText(this@AddPostScreen, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
```

### Example 3: Sending a Message

```kotlin
// In chatScreen.kt
val request = SendMessageRequest(
    receiverId = otherUserId,
    messageText = messageText,
    messageType = "text",
    isVanishMode = isVanishModeEnabled
)

lifecycleScope.launch {
    try {
        val response = withContext(Dispatchers.IO) {
            apiService.sendMessage(request)
        }
        
        if (response.isSuccessful) {
            // Message sent, update UI
        }
    } catch (e: Exception) {
        // Handle error
    }
}
```

### Example 4: Following a User

```kotlin
// In profileScreen.kt
val request = FollowRequest(userId = targetUserId)

lifecycleScope.launch {
    try {
        val response = withContext(Dispatchers.IO) {
            apiService.followUser(request)
        }
        
        if (response.isSuccessful) {
            val message = response.body()?.get("message") ?: "Success"
            Toast.makeText(this@profileScreen, message, Toast.LENGTH_SHORT).show()
            // Update UI (Following/Requested button)
        }
    } catch (e: Exception) {
        Toast.makeText(this@profileScreen, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
```

## 🔐 Security Features

1. **Password Hashing**: bcrypt with cost factor 10
2. **JWT Authentication**: 30-day expiration
3. **SQL Injection Protection**: Prepared statements
4. **Session Management**: Token-based with expiration
5. **Input Validation**: Server-side validation
6. **CORS**: Configured for API access

## 🐛 Common Issues & Solutions

### Issue 1: "Connection refused" error
**Solution**: 
- Check XAMPP Apache and MySQL are running
- Verify API URL (10.0.2.2 for emulator)
- For real device, use computer's IP address

### Issue 2: "Database connection failed"
**Solution**:
- Check MySQL is running in XAMPP
- Verify database credentials in `config/config.php`
- Ensure database `instagram_clone` exists

### Issue 3: "Unauthorized" error
**Solution**:
- Check JWT token is being sent in Authorization header
- Verify SessionManager.getToken() returns valid token
- Re-login to get fresh token

### Issue 4: Images not displaying
**Solution**:
- Check Base64 encoding is correct
- Verify LONGTEXT column can store large data
- Increase PHP upload limits if needed

## 📈 Performance Optimization

1. **Database Indexing**
   - Already indexed: user_id, created_at, expires_at
   - Composite indexes on frequently queried combinations

2. **Caching**
   - Implement Redis for session caching (optional)
   - Use Picasso for image caching on Android

3. **Pagination**
   - Posts feed: 20 posts per page
   - Implemented in API with LIMIT/OFFSET

4. **Image Optimization**
   - Resize images before Base64 encoding
   - Max dimensions: 800x800 for posts
   - Compress JPEG quality: 80%

## 🔄 Offline Support (To Be Implemented)

1. **Create SQLite Database Helper** (Android)
2. **Cache posts, messages, stories locally**
3. **Queue offline actions in `offline_queue` table**
4. **Background sync service** to process queue

## 📱 Push Notifications (FCM)

Already integrated with:
- FCM token storage in users table
- Notification creation in database
- API endpoint to update FCM token

Next steps:
- Implement server-side FCM push
- Handle notifications in Android app

## 🎨 Features Implemented

✅ User Authentication (Signup, Login, Logout)
✅ Profile Management
✅ Posts with Likes & Comments
✅ Stories (24-hour expiry)
✅ Follow System (Public/Private accounts)
✅ Messaging (Text, Image, Edit/Delete)
✅ Vanish Mode
✅ Search Users
✅ Online/Offline Status
✅ Notifications System
✅ Session Management
✅ JWT Authentication

## 🚧 Next Steps

1. Migrate remaining Firebase code to MySQL APIs
2. Implement offline queue processing
3. Add FCM server-side push
4. Implement story auto-cleanup cron job
5. Add real-time features (WebSocket or polling)
6. Implement call history tracking
7. Add media file cleanup service
8. Create admin panel for management

## 📞 Support

If you encounter any issues:
1. Check XAMPP error logs: `xampp/apache/logs/error.log`
2. Check PHP errors in browser console
3. Check Android Logcat for API errors
4. Verify database queries in phpMyAdmin

## 🎓 Learning Resources

- PHP PDO: https://www.php.net/manual/en/book.pdo.php
- JWT: https://jwt.io/
- Retrofit: https://square.github.io/retrofit/
- MySQL: https://dev.mysql.com/doc/

---

**Ready to Start Migration!** 🚀

Follow the steps above and migrate one feature at a time. Test each feature thoroughly before moving to the next.

