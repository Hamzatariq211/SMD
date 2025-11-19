<?php
// Database configuration
define('DB_HOST', 'localhost');
define('DB_USER', 'root');
define('DB_PASS', '');
define('DB_NAME', 'instagram_clone');

// JWT Secret Key (change this to a random secure string in production)
define('JWT_SECRET', 'your-secret-key-change-this-in-production-2024');
define('JWT_EXPIRATION', 86400 * 30); // 30 days

// Base URL
define('BASE_URL', 'http://localhost/instagram_api/');

// Upload directories
define('UPLOAD_DIR', __DIR__ . '/uploads/');
define('PROFILE_IMAGES_DIR', UPLOAD_DIR . 'profiles/');
define('POST_IMAGES_DIR', UPLOAD_DIR . 'posts/');
define('STORY_IMAGES_DIR', UPLOAD_DIR . 'stories/');
define('MESSAGE_MEDIA_DIR', UPLOAD_DIR . 'messages/');

// Timezone
date_default_timezone_set('UTC');

// Error reporting (disable in production)
error_reporting(E_ALL);
ini_set('display_errors', 1);
?>
