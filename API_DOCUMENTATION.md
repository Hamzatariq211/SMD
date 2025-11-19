# Instagram Clone - Complete Backend API Documentation

## Setup Instructions

### 1. Database Setup
1. Open XAMPP Control Panel
2. Start Apache and MySQL
3. Open phpMyAdmin (http://localhost/phpmyadmin)
4. Import the database schema:
   - Go to SQL tab
   - Copy and paste the contents of `database/schema.sql`
   - Click "Go" to execute

### 2. Backend Setup
1. Copy the `instagram_api` folder to your XAMPP `htdocs` directory:
   ```
   C:\xampp\htdocs\instagram_api\
   ```

2. Update the database configuration in `instagram_api/config/config.php` if needed

3. Test the API by accessing:
   ```
   http://localhost/instagram_api/api/auth/login.php
   ```

### 3. Android App Configuration
Update the BASE_URL in the Kotlin app's `ApiService.kt`:
- For emulator: `http://10.0.2.2/instagram_api/`
- For real device: `http://YOUR_COMPUTER_IP/instagram_api/`

## API Endpoints

### Authentication
- **POST** `/api/auth/signup.php` - Register new user
- **POST** `/api/auth/login.php` - User login
- **POST** `/api/auth/logout.php` - User logout (requires auth)

### Users
- **GET** `/api/users/me.php` - Get current user profile (requires auth)
- **PUT** `/api/users/update.php` - Update user profile (requires auth)
- **GET** `/api/users/profile.php?userId=XXX` - Get other user profile (requires auth)
- **GET** `/api/users/search.php?query=XXX` - Search users (requires auth)
- **POST** `/api/users/updateStatus.php` - Update online/offline status (requires auth)

### Posts
- **POST** `/api/posts/create.php` - Create new post (requires auth)
- **GET** `/api/posts/feed.php?page=1` - Get posts feed (requires auth)
- **GET** `/api/posts/userPosts.php?userId=XXX` - Get user's posts (requires auth)
- **POST** `/api/posts/like.php` - Like/unlike post (requires auth)
- **POST** `/api/posts/comment.php` - Add comment to post (requires auth)
- **GET** `/api/posts/getComments.php?postId=XXX` - Get post comments (requires auth)

### Stories
- **POST** `/api/stories/upload.php` - Upload story (requires auth)
- **GET** `/api/stories/getStories.php` - Get active stories from followed users (requires auth)
- **POST** `/api/stories/viewStory.php` - Mark story as viewed (requires auth)

### Follow System
- **POST** `/api/follow/follow.php` - Follow user (requires auth)
- **POST** `/api/follow/unfollow.php` - Unfollow user (requires auth)
- **GET** `/api/follow/requests.php` - Get follow requests (requires auth)
- **POST** `/api/follow/respondRequest.php` - Accept/reject follow request (requires auth)

### Messages
- **POST** `/api/messages/send.php` - Send message (requires auth)
- **GET** `/api/messages/getChatList.php` - Get chat list (requires auth)
- **GET** `/api/messages/getMessages.php?userId=XXX` - Get messages with user (requires auth)
- **PUT** `/api/messages/editMessage.php` - Edit message (requires auth, within 5 min)
- **DELETE** `/api/messages/deleteMessage.php` - Delete message (requires auth, within 5 min)

### Notifications
- **POST** `/api/notifications/updateFCMToken.php` - Update FCM token (requires auth)
- **GET** `/api/notifications/getNotifications.php` - Get notifications (requires auth)

## Request/Response Examples

### Signup Request
```json
POST /api/auth/signup.php
{
  "email": "user@example.com",
  "password": "password123",
  "username": "johndoe",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Login Request
```json
POST /api/auth/login.php
{
  "email": "user@example.com",
  "password": "password123"
}
```

### Response
```json
{
  "message": "Login successful",
  "userId": "uuid-here",
  "token": "jwt-token-here",
  "isProfileSetup": false
}
```

### Authorization Header
All protected endpoints require:
```
Authorization: Bearer YOUR_JWT_TOKEN
```

## Database Tables

1. **users** - User accounts and profiles
2. **posts** - User posts
3. **post_likes** - Post likes
4. **comments** - Post comments
5. **stories** - User stories (24h expiry)
6. **story_views** - Story views tracking
7. **follows** - Follow relationships
8. **follow_requests** - Pending follow requests
9. **chat_rooms** - Chat conversations
10. **messages** - Chat messages
11. **notifications** - User notifications
12. **call_history** - Voice/video call records
13. **offline_queue** - Offline action queue
14. **media_files** - Media file tracking
15. **user_sessions** - Active user sessions

## Features Implemented

✅ User Authentication (Signup, Login, Logout)
✅ Profile Management
✅ Post Creation with Likes & Comments
✅ Stories (24-hour expiry)
✅ Follow System with Private Accounts
✅ Messaging System with Edit/Delete (5-minute window)
✅ Vanish Mode for Messages
✅ Search Functionality
✅ Online/Offline Status
✅ Push Notifications (FCM integration ready)
✅ Session Management with JWT
✅ Offline Queue Support

## Security Features

- Password hashing with bcrypt
- JWT token authentication
- SQL injection protection with prepared statements
- Input validation
- CORS headers
- Session expiration tracking

