# Instagram Clone - MySQL Backend Setup Guide

## Prerequisites
- XAMPP installed and running
- Android Studio
- Android device/emulator

## Step 1: Database Setup

1. **Start XAMPP**
   - Open XAMPP Control Panel
   - Start Apache and MySQL

2. **Create Database**
   - Open phpMyAdmin: http://localhost/phpmyadmin
   - Click "SQL" tab
   - Copy and paste the contents of `database/schema.sql`
   - Click "Go" to execute

3. **Verify Database**
   - You should see `instagram_clone` database created
   - It should contain 15 tables

## Step 2: PHP Backend Setup

1. **Copy API Files**
   - The `instagram_api` folder should be copied to: `C:\xampp\htdocs\instagram_api\`
   - Folder structure should be:
     ```
     C:\xampp\htdocs\instagram_api\
     ├── api\
     │   ├── auth\
     │   ├── follow\
     │   ├── messages\
     │   ├── notifications\
     │   ├── posts\
     │   ├── stories\
     │   └── users\
     ├── config\
     └── utils\
     ```

2. **Verify PHP Configuration**
   - Open: `instagram_api/config/config.php`
   - Ensure database credentials match your XAMPP setup:
     ```php
     define('DB_HOST', 'localhost');
     define('DB_USER', 'root');
     define('DB_PASS', '');
     define('DB_NAME', 'instagram_clone');
     ```

3. **Test API**
   - Open browser: http://localhost/instagram_api/api/auth/login.php
   - You should see: `{"error":"Method not allowed"}` (this is correct - GET not allowed)

## Step 3: Android App Configuration

1. **Update API Base URL**
   - Open: `app/src/main/java/com/devs/i210396_i211384/network/ApiService.kt`
   - For Android Emulator, use: `http://10.0.2.2/instagram_api/`
   - For Real Device, use your computer's IP: `http://192.168.x.x/instagram_api/`
   
2. **Find Your Computer's IP (for real device)**
   - Windows: Open CMD and type `ipconfig`
   - Look for "IPv4 Address" under your network adapter
   - Example: 192.168.1.100

## Step 4: Run the Application

1. **Build the App**
   - Open project in Android Studio
   - Wait for Gradle sync to complete
   - Click "Build" → "Make Project"

2. **Run on Emulator/Device**
   - Click Run button or Shift+F10
   - Wait for app to install and launch

## Application Flow

### First Time User (Signup)
1. **Splash Screen** (5 seconds)
2. **Login Screen** → Click "Sign up"
3. **Register Screen** → Fill details → Click "Register"
4. **Edit Profile Screen** → Complete profile setup → Click "Done"
5. **Home Page** → You're logged in!

### Returning User (Login)
1. **Splash Screen** (5 seconds)
2. **Login Screen** → Enter credentials → Click "Login"
3. If profile setup: **Home Page**
4. If profile incomplete: **Edit Profile Screen**

### Already Logged In User
1. **Splash Screen** (5 seconds)
2. Verifies session with server
3. If profile setup: **Home Page**
4. If profile incomplete: **Edit Profile Screen**

## Troubleshooting

### "Network error" when trying to login/signup
- **Check XAMPP**: Ensure Apache and MySQL are running (green indicators)
- **Check API URL**: Verify the BASE_URL in ApiService.kt
- **Test API**: Open http://localhost/instagram_api/api/auth/login.php in browser

### "Database error"
- **Verify Database**: Check phpMyAdmin to ensure database exists
- **Check Credentials**: Verify config.php has correct DB credentials
- **Run Schema**: Re-run the schema.sql file

### App shows EditProfile with old data after login
- This is now FIXED - the app verifies profile setup status with server on launch
- Clear app data: Settings → Apps → Instagram Clone → Storage → Clear Data

### Emulator can't connect to localhost
- Use `10.0.2.2` instead of `localhost` or `127.0.0.1`
- This is the special IP for emulator to access host machine

### Real device can't connect
- Ensure device and computer are on same WiFi network
- Use computer's IP address (from ipconfig)
- Update BASE_URL in ApiService.kt
- Check firewall settings

## Testing the App

### Test User Account
The database comes with a default admin account:
- Email: admin@instagram.com
- Password: password

### Create Your Own Account
1. Click "Sign up" on login screen
2. Fill in:
   - Username (unique, 3-20 characters)
   - First Name
   - Last Name
   - Email (valid format)
   - Password (min 6 characters)
3. Click "Register"
4. Complete profile setup
5. Start using the app!

## Key Features Implemented

### ✅ User Authentication (MySQL Backend)
- Signup with email/password
- Login with email/password
- Secure password hashing (bcrypt)
- JWT token-based authentication
- Session management
- Auto-login for returning users

### ✅ Profile Management
- Edit profile information
- Upload profile picture (Base64)
- Private account toggle
- Profile setup flow for first-time users

### ✅ Splash Screen
- 5-second splash screen (as per requirements)
- Smart navigation based on auth status
- Server verification of profile setup status

### ✅ Session Security
- JWT tokens stored securely
- Token expiration (30 days)
- Session verification on app launch
- Logout functionality

## Database Structure

The application uses 15 tables:
1. **users** - User accounts and profiles
2. **posts** - User posts
3. **post_likes** - Post likes
4. **comments** - Post comments
5. **stories** - 24-hour stories
6. **story_views** - Story view tracking
7. **follows** - Follow relationships
8. **follow_requests** - Pending follow requests
9. **chat_rooms** - Chat conversations
10. **messages** - Chat messages
11. **notifications** - Push notifications
12. **call_history** - Voice/video call logs
13. **offline_queue** - Offline action queue
14. **media_files** - Uploaded media tracking
15. **user_sessions** - Active user sessions

## Next Steps

After completing the authentication and profile setup, you can implement:
1. Posts feature (upload, view, like, comment)
2. Stories feature (24-hour temporary posts)
3. Follow system (follow/unfollow users)
4. Messaging system (text, media, vanish mode)
5. Voice & Video calls (Agora integration)
6. Push notifications (FCM)
7. Search & explore
8. Offline support with SQLite

## Important Notes

- Always keep XAMPP running when testing the app
- The app requires internet/local network to communicate with MySQL backend
- Profile images are stored as Base64 in the database (for simplicity)
- For production, consider using cloud storage for images
- JWT tokens expire after 30 days - users will need to login again

## Support

If you encounter issues:
1. Check logcat in Android Studio for errors
2. Check XAMPP error logs: `C:\xampp\apache\logs\error.log`
3. Verify database connection in phpMyAdmin
4. Test API endpoints directly in browser/Postman

---

**Remember**: The transition from Firebase to MySQL is complete for authentication and user management. Other features will be migrated step by step.

