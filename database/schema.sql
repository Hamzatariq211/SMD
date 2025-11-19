-- Instagram Clone Database Schema
-- Created for MySQL/MariaDB

-- Drop existing database and create fresh
DROP DATABASE IF EXISTS instagram_clone;
CREATE DATABASE instagram_clone CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE instagram_clone;

-- 1. Users Table
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    date_of_birth DATE,
    profile_image_url LONGTEXT,
    cover_image_url LONGTEXT,
    bio TEXT,
    website VARCHAR(255),
    phone VARCHAR(20),
    gender VARCHAR(20),
    is_profile_setup BOOLEAN DEFAULT FALSE,
    is_private BOOLEAN DEFAULT FALSE,
    is_online BOOLEAN DEFAULT FALSE,
    last_seen BIGINT DEFAULT 0,
    fcm_token VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_is_online (is_online)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Posts Table
CREATE TABLE posts (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    image_url LONGTEXT NOT NULL,
    caption TEXT,
    location VARCHAR(255),
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Post Likes Table
CREATE TABLE post_likes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_like (post_id, user_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Comments Table
CREATE TABLE comments (
    id VARCHAR(36) PRIMARY KEY,
    post_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    comment_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Stories Table (FIXED: expires_at default value)
CREATE TABLE stories (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    story_url LONGTEXT NOT NULL,
    story_type VARCHAR(20) DEFAULT 'image',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    view_count INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Story Views Table
CREATE TABLE story_views (
    id INT AUTO_INCREMENT PRIMARY KEY,
    story_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_view (story_id, user_id),
    FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_story_id (story_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Follows Table
CREATE TABLE follows (
    id INT AUTO_INCREMENT PRIMARY KEY,
    follower_id VARCHAR(36) NOT NULL,
    following_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_follow (follower_id, following_id),
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_follower_id (follower_id),
    INDEX idx_following_id (following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Follow Requests Table
CREATE TABLE follow_requests (
    id VARCHAR(36) PRIMARY KEY,
    from_user_id VARCHAR(36) NOT NULL,
    to_user_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_request (from_user_id, to_user_id),
    FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_to_user_status (to_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Chat Rooms Table
CREATE TABLE chat_rooms (
    id VARCHAR(36) PRIMARY KEY,
    user1_id VARCHAR(36) NOT NULL,
    user2_id VARCHAR(36) NOT NULL,
    last_message TEXT,
    last_message_time BIGINT DEFAULT 0,
    last_message_sender_id VARCHAR(36),
    user1_unread INT DEFAULT 0,
    user2_unread INT DEFAULT 0,
    vanish_mode BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_chat (user1_id, user2_id),
    FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user1_id (user1_id),
    INDEX idx_user2_id (user2_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Messages Table
CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    chat_room_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    receiver_id VARCHAR(36) NOT NULL,
    message_text TEXT,
    message_type VARCHAR(20) DEFAULT 'text',
    media_url LONGTEXT,
    post_id VARCHAR(36),
    is_edited BOOLEAN DEFAULT FALSE,
    edited_at BIGINT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_seen BOOLEAN DEFAULT FALSE,
    seen_at BIGINT DEFAULT 0,
    is_vanish_mode BOOLEAN DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_chat_room_id (chat_room_id),
    INDEX idx_created_at (created_at),
    INDEX idx_sender_id (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Notifications Table
CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    from_user_id VARCHAR(36),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    reference_id VARCHAR(36),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id_read (user_id, is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Call History Table
CREATE TABLE call_history (
    id VARCHAR(36) PRIMARY KEY,
    caller_id VARCHAR(36) NOT NULL,
    receiver_id VARCHAR(36) NOT NULL,
    call_type VARCHAR(20) NOT NULL,
    channel_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'ringing',
    duration INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    FOREIGN KEY (caller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_caller_id (caller_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Offline Queue Table
CREATE TABLE offline_queue (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_status (user_id, status),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Media Files Table
CREATE TABLE media_files (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    reference_type VARCHAR(50),
    reference_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_reference (reference_type, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. User Sessions Table (FIXED: expires_at default value)
CREATE TABLE user_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    device_id VARCHAR(255),
    device_type VARCHAR(50),
    ip_address VARCHAR(45),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id_active (user_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create triggers to update counts

DELIMITER //

-- Trigger to increment like_count when a like is added
CREATE TRIGGER increment_like_count AFTER INSERT ON post_likes
FOR EACH ROW
BEGIN
    UPDATE posts SET like_count = like_count + 1 WHERE id = NEW.post_id;
END//

-- Trigger to decrement like_count when a like is removed
CREATE TRIGGER decrement_like_count AFTER DELETE ON post_likes
FOR EACH ROW
BEGIN
    UPDATE posts SET like_count = like_count - 1 WHERE id = OLD.post_id;
END//

-- Trigger to increment comment_count when a comment is added
CREATE TRIGGER increment_comment_count AFTER INSERT ON comments
FOR EACH ROW
BEGIN
    UPDATE posts SET comment_count = comment_count + 1 WHERE id = NEW.post_id;
END//

-- Trigger to decrement comment_count when a comment is removed
CREATE TRIGGER decrement_comment_count AFTER DELETE ON comments
FOR EACH ROW
BEGIN
    UPDATE posts SET comment_count = comment_count - 1 WHERE id = OLD.post_id;
END//

-- Trigger to increment story view_count
CREATE TRIGGER increment_story_view_count AFTER INSERT ON story_views
FOR EACH ROW
BEGIN
    UPDATE stories SET view_count = view_count + 1 WHERE id = NEW.story_id;
END//

DELIMITER ;

-- Create stored procedures for common operations

DELIMITER //

-- Get user followers count
CREATE PROCEDURE GetFollowersCount(IN userId VARCHAR(36))
BEGIN
    SELECT COUNT(*) as count FROM follows WHERE following_id = userId;
END//

-- Get user following count
CREATE PROCEDURE GetFollowingCount(IN userId VARCHAR(36))
BEGIN
    SELECT COUNT(*) as count FROM follows WHERE follower_id = userId;
END//

-- Get user posts count
CREATE PROCEDURE GetPostsCount(IN userId VARCHAR(36))
BEGIN
    SELECT COUNT(*) as count FROM posts WHERE user_id = userId;
END//

-- Check if user1 follows user2
CREATE PROCEDURE IsFollowing(IN followerId VARCHAR(36), IN followingId VARCHAR(36))
BEGIN
    SELECT COUNT(*) > 0 as is_following FROM follows
    WHERE follower_id = followerId AND following_id = followingId;
END//

-- Clean up expired stories (should be called periodically)
CREATE PROCEDURE CleanupExpiredStories()
BEGIN
    DELETE FROM stories WHERE expires_at < NOW();
END//

-- Clean up expired sessions
CREATE PROCEDURE CleanupExpiredSessions()
BEGIN
    UPDATE user_sessions SET is_active = FALSE WHERE expires_at < NOW() AND is_active = TRUE;
END//

DELIMITER ;

-- Create views for common queries

-- View for user profiles with counts
CREATE VIEW user_profiles AS
SELECT
    u.id,
    u.email,
    u.username,
    u.first_name,
    u.last_name,
    u.profile_image_url,
    u.cover_image_url,
    u.bio,
    u.website,
    u.is_private,
    u.is_online,
    u.last_seen,
    (SELECT COUNT(*) FROM posts WHERE user_id = u.id) as posts_count,
    (SELECT COUNT(*) FROM follows WHERE following_id = u.id) as followers_count,
    (SELECT COUNT(*) FROM follows WHERE follower_id = u.id) as following_count
FROM users u;

-- View for posts with user info
CREATE VIEW posts_with_user AS
SELECT
    p.*,
    u.username,
    u.first_name,
    u.last_name,
    u.profile_image_url
FROM posts p
JOIN users u ON p.user_id = u.id;

-- View for active stories
CREATE VIEW active_stories AS
SELECT
    s.*,
    u.username,
    u.profile_image_url
FROM stories s
JOIN users u ON s.user_id = u.id
WHERE s.expires_at > NOW()
ORDER BY s.created_at DESC;

-- Insert default admin user for testing
INSERT INTO users (id, email, password_hash, username, first_name, last_name, is_profile_setup)
VALUES (
    UUID(),
    'admin@instagram.com',
    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password
    'admin',
    'Admin',
    'User',
    TRUE
);

-- Show table structure
SHOW TABLES;
