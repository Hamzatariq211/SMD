# Testing & Migration Checklist

## 🧪 Complete Testing Guide

### Phase 1: Backend Setup & Testing (15 minutes)

#### 1.1 Database Setup
```sql
-- In phpMyAdmin SQL tab
1. Execute schema.sql
2. Verify tables created:
   SELECT COUNT(*) as table_count FROM information_schema.tables 
   WHERE table_schema = 'instagram_clone';
   -- Should return 15

3. Check default admin user:
   SELECT * FROM users LIMIT 1;
```

#### 1.2 Test Backend APIs (Using Postman or curl)

**Test Signup**
```bash
POST http://localhost/instagram_api/api/auth/signup.php
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "test123",
  "username": "testuser",
  "firstName": "Test",
  "lastName": "User"
}

Expected Response:
{
  "message": "Registration successful",
  "userId": "uuid-here",
  "token": "jwt-token-here",
  "isProfileSetup": false
}
```

**Test Login**
```bash
POST http://localhost/instagram_api/api/auth/login.php
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "test123"
}

Expected Response:
{
  "message": "Login successful",
  "userId": "uuid-here",
  "token": "jwt-token-here",
  "isProfileSetup": false
}
```

**Test Get Profile (needs token)**
```bash
GET http://localhost/instagram_api/api/users/me.php
Authorization: Bearer YOUR_JWT_TOKEN

Expected Response:
{
  "id": "uuid",
  "email": "test@example.com",
  "username": "testuser",
  ...
}
```

### Phase 2: Android App Testing (30 minutes)

#### 2.1 Clean Build
```kotlin
1. File → Invalidate Caches / Restart
2. Build → Clean Project
3. Build → Rebuild Project
4. Check for compilation errors
```

#### 2.2 Test Authentication Flow

**Signup Test:**
1. Open app on emulator/device
2. Click "Sign Up"
3. Fill form:
   - Email: test2@example.com
   - Password: test123
   - Username: testuser2
   - First Name: Test
   - Last Name: User
4. Click Register
5. Should navigate to EditProfile screen
6. ✅ Verify in database: `SELECT * FROM users WHERE email = 'test2@example.com'`

**Login Test:**
1. Restart app
2. Enter email and password
3. Click Login
4. Should navigate to HomePage
5. ✅ Check session: `SELECT * FROM user_sessions ORDER BY created_at DESC LIMIT 1`

**Profile Update Test:**
1. In EditProfile screen
2. Fill all fields (name, bio, website, etc.)
3. Click Done
4. Should navigate to HomePage
5. ✅ Verify: `SELECT * FROM users WHERE email = 'test2@example.com'`
6. Check is_profile_setup = 1

#### 2.3 Test Posts Feature

**Create Post:**
```kotlin
// Test in app:
1. Click + button
2. Select image from gallery
3. Add caption: "My first MySQL post!"
4. Click Post

// Verify in database:
SELECT * FROM posts ORDER BY created_at DESC LIMIT 1;
```

**Like Post:**
```kotlin
// Test in app:
1. Double-tap post to like
2. Check like count increases

// Verify in database:
SELECT * FROM post_likes WHERE post_id = 'your-post-id';
SELECT like_count FROM posts WHERE id = 'your-post-id';
```

**Comment on Post:**
```kotlin
// Test in app:
1. Click on post
2. Add comment
3. Submit

// Verify in database:
SELECT * FROM comments WHERE post_id = 'your-post-id';
SELECT comment_count FROM posts WHERE id = 'your-post-id';
```

#### 2.4 Test Stories Feature

**Upload Story:**
```kotlin
// Test in app:
1. Click profile picture with + icon
2. Select image
3. Share as story

// Verify in database:
SELECT * FROM stories WHERE user_id = 'your-user-id';
SELECT TIMESTAMPDIFF(HOUR, NOW(), expires_at) as hours_until_expiry 
FROM stories WHERE id = 'story-id';
-- Should be ~24 hours
```

#### 2.5 Test Follow System

**Follow Public User:**
```kotlin
// Setup: Create user with is_private = 0
1. Search for user
2. Click Follow button
3. Should immediately show "Following"

// Verify:
SELECT * FROM follows 
WHERE follower_id = 'your-id' AND following_id = 'other-user-id';
```

**Follow Private User:**
```kotlin
// Setup: Create user with is_private = 1
1. Search for private user
2. Click Follow button
3. Should show "Requested"

// Verify:
SELECT * FROM follow_requests 
WHERE from_user_id = 'your-id' AND to_user_id = 'private-user-id';
```

**Accept Follow Request:**
```kotlin
// Test in app:
1. Login as private user
2. Go to follow requests
3. Accept request

// Verify:
SELECT * FROM follows 
WHERE follower_id = 'requester-id' AND following_id = 'your-id';

SELECT status FROM follow_requests WHERE id = 'request-id';
-- Should be 'accepted'
```

#### 2.6 Test Messaging System

**Send Message:**
```kotlin
// Test in app:
1. Open chat with user
2. Type message: "Hello from MySQL!"
3. Send

// Verify:
SELECT * FROM messages 
WHERE sender_id = 'your-id' AND receiver_id = 'other-id'
ORDER BY created_at DESC LIMIT 1;

SELECT * FROM chat_rooms 
WHERE (user1_id = 'your-id' AND user2_id = 'other-id')
   OR (user1_id = 'other-id' AND user2_id = 'your-id');
```

**Edit Message (within 5 minutes):**
```kotlin
// Test in app:
1. Long press on your message
2. Select Edit
3. Change text
4. Save

// Verify:
SELECT message_text, is_edited, edited_at 
FROM messages WHERE id = 'message-id';
-- is_edited should be 1
```

**Delete Message (within 5 minutes):**
```kotlin
// Test in app:
1. Long press on your message
2. Select Delete
3. Confirm

// Verify:
SELECT is_deleted FROM messages WHERE id = 'message-id';
-- is_deleted should be 1
```

#### 2.7 Test Search Feature

```kotlin
// Test in app:
1. Go to search/explore
2. Type username: "test"
3. Results should appear

// Verify query works:
SELECT id, username, first_name, last_name 
FROM users 
WHERE username LIKE '%test%' 
   OR first_name LIKE '%test%' 
   OR last_name LIKE '%test%'
LIMIT 50;
```

#### 2.8 Test Online Status

```kotlin
// Test in app:
1. User logs in → should set is_online = 1
2. User closes app → should set is_online = 0
3. View other user profile → should show green dot if online

// Verify:
SELECT username, is_online, FROM_UNIXTIME(last_seen/1000) as last_active
FROM users 
WHERE id = 'user-id';
```

### Phase 3: Error Handling Tests

#### 3.1 Test Invalid Inputs

**Invalid Email:**
```kotlin
Try signup with: "notanemail"
Expected: "Invalid email format" error
```

**Duplicate Username:**
```kotlin
Try signup with existing username
Expected: "Username already taken" error
```

**Wrong Password:**
```kotlin
Try login with wrong password
Expected: "Invalid email or password" error
```

**Edit Old Message:**
```kotlin
Try editing message older than 5 minutes
Expected: "Messages can only be edited within 5 minutes" error
```

#### 3.2 Test Authorization

**Without Token:**
```bash
GET http://localhost/instagram_api/api/users/me.php
# No Authorization header

Expected: 401 Unauthorized
```

**With Invalid Token:**
```bash
GET http://localhost/instagram_api/api/users/me.php
Authorization: Bearer invalid-token

Expected: 401 Unauthorized
```

### Phase 4: Performance Testing

#### 4.1 Database Indexes Check
```sql
-- Verify indexes exist
SHOW INDEXES FROM posts;
SHOW INDEXES FROM follows;
SHOW INDEXES FROM messages;

-- Should see indexes on:
-- posts: user_id, created_at
-- follows: follower_id, following_id
-- messages: chat_room_id, created_at
```

#### 4.2 Query Performance
```sql
-- Test feed query performance
EXPLAIN SELECT * FROM posts 
WHERE user_id IN (
    SELECT following_id FROM follows WHERE follower_id = 'your-id'
)
ORDER BY created_at DESC LIMIT 20;

-- Should use indexes, not full table scan
```

### Phase 5: Migration Verification Checklist

#### Complete Feature Migration Status

- [x] **User Authentication**
  - [x] Signup works with MySQL
  - [x] Login works with MySQL
  - [x] Logout invalidates session
  - [x] JWT tokens are generated
  - [x] Session persists across app restarts

- [ ] **Profile Management**
  - [x] EditProfile saves to MySQL
  - [x] Profile loads from MySQL
  - [ ] Profile picture uploads
  - [ ] Cover photo uploads
  - [x] Privacy toggle works

- [ ] **Posts**
  - [ ] Create post via API
  - [ ] Feed loads from MySQL
  - [ ] Like/unlike works
  - [ ] Comment works
  - [ ] Comments load from MySQL
  - [ ] User posts grid loads

- [ ] **Stories**
  - [ ] Upload story via API
  - [ ] Stories load from MySQL
  - [ ] Story view tracking works
  - [ ] 24-hour expiry works
  - [ ] Story ring shows correctly

- [ ] **Follow System**
  - [ ] Follow public user works
  - [ ] Follow private user sends request
  - [ ] Accept request works
  - [ ] Reject request works
  - [ ] Unfollow works
  - [ ] Followers/Following lists load

- [ ] **Messaging**
  - [ ] Send text message
  - [ ] Send image message
  - [ ] Edit message (5 min)
  - [ ] Delete message (5 min)
  - [ ] Vanish mode works
  - [ ] Chat list loads
  - [ ] Unread count works
  - [ ] Message seen status

- [ ] **Search**
  - [ ] Search users works
  - [ ] Results are relevant
  - [ ] Can navigate to profiles

- [ ] **Notifications**
  - [ ] Notifications are created in DB
  - [ ] FCM token is stored
  - [ ] Notification list loads
  - [ ] Mark as read works

- [ ] **Online Status**
  - [ ] Status updates on login
  - [ ] Status updates on logout
  - [ ] Last seen timestamp updates
  - [ ] Green dot shows correctly

### Phase 6: Cleanup & Optimization

#### 6.1 Remove Firebase Dependencies

**Check and remove:**
```kotlin
// Find all Firebase imports
grep -r "import com.google.firebase" app/src/

// Files to update (remove Firebase code):
- HomePage.kt
- AddPostScreen.kt
- Story.kt
- UploadStory.kt
- Messages.kt
- chatScreen.kt
- profileScreen.kt
- Explore.kt
- ExploreSearch.kt
- FollowRequestsActivity.kt
```

#### 6.2 Test XAMPP Restart
```
1. Stop Apache and MySQL in XAMPP
2. Restart both services
3. Test app again
4. Verify everything still works
```

#### 6.3 Database Backup
```sql
-- In phpMyAdmin:
1. Select instagram_clone database
2. Click Export
3. Select "Quick" export method
4. Click Go
5. Save backup file
```

### Phase 7: Production Readiness

#### 7.1 Security Checklist
- [ ] Change JWT_SECRET in config.php
- [ ] Disable error display in production
- [ ] Enable HTTPS for production
- [ ] Set proper file permissions (755 for folders, 644 for files)
- [ ] Review and strengthen password requirements
- [ ] Add rate limiting to prevent abuse
- [ ] Sanitize all user inputs

#### 7.2 Performance Optimization
- [ ] Enable MySQL query caching
- [ ] Add Redis for session storage (optional)
- [ ] Optimize images before Base64 encoding
- [ ] Implement CDN for media files (optional)
- [ ] Add database connection pooling

## 📊 Testing Report Template

After testing, document results:

```
TESTING REPORT
Date: ___________
Tester: ___________

BACKEND TESTS:
✅ Database created successfully
✅ All 15 tables created
✅ Signup API works
✅ Login API works
✅ Profile API works
___ Posts API works
___ Stories API works
___ Follow API works
___ Messages API works

ANDROID TESTS:
✅ Signup flow works
✅ Login flow works
✅ Profile update works
___ Posts creation works
___ Feed displays
___ Stories upload works
___ Messages send
___ Search works

ERRORS FOUND:
1. ___________________________
2. ___________________________
3. ___________________________

NOTES:
___________________________
___________________________
```

## 🎯 Final Checklist Before Submission

- [ ] All SQL tables created
- [ ] All API endpoints tested
- [ ] Android app compiles without errors
- [ ] User can signup and login
- [ ] Profile can be updated
- [ ] At least one post created successfully
- [ ] Firebase code removed/commented
- [ ] Code committed to GitHub
- [ ] Meaningful commit messages
- [ ] README.md updated
- [ ] API_DOCUMENTATION.md included
- [ ] Screenshots taken
- [ ] Demo video recorded (optional)

## 🚀 You're Ready!

Once all checkboxes are marked, your app is fully migrated and ready for submission!

