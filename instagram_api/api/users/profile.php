<?php
require_once __DIR__ . '/../../config/Database.php';
require_once __DIR__ . '/../../utils/JWT.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
    exit();
}

$currentUserId = JWT::getUserIdFromToken();

if (!$currentUserId) {
    http_response_code(401);
    echo json_encode(['error' => 'Unauthorized']);
    exit();
}

$targetUserId = $_GET['userId'] ?? null;

if (!$targetUserId) {
    http_response_code(400);
    echo json_encode(['error' => 'User ID is required']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    // Get user profile
    $stmt = $db->prepare("
        SELECT
            id,
            username,
            first_name as firstName,
            last_name as lastName,
            profile_image_url as profileImageUrl,
            cover_image_url as coverImageUrl,
            bio,
            website,
            is_private as isPrivate,
            is_online as isOnline,
            last_seen as lastSeen,
            (SELECT COUNT(*) FROM posts WHERE user_id = ?) as postsCount,
            (SELECT COUNT(*) FROM follows WHERE following_id = ?) as followersCount,
            (SELECT COUNT(*) FROM follows WHERE follower_id = ?) as followingCount
        FROM users WHERE id = ?
    ");
    $stmt->execute([$targetUserId, $targetUserId, $targetUserId, $targetUserId]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user) {
        http_response_code(404);
        echo json_encode(['error' => 'User not found']);
        exit();
    }

    // Convert boolean fields
    $user['isPrivate'] = (bool)$user['isPrivate'];
    $user['isOnline'] = (bool)$user['isOnline'];
    $user['lastSeen'] = (int)$user['lastSeen'];
    $user['postsCount'] = (int)$user['postsCount'];
    $user['followersCount'] = (int)$user['followersCount'];
    $user['followingCount'] = (int)$user['followingCount'];

    // Check if current user follows target user
    $followStmt = $db->prepare("SELECT id FROM follows WHERE follower_id = ? AND following_id = ?");
    $followStmt->execute([$currentUserId, $targetUserId]);
    $user['isFollowing'] = $followStmt->fetch() ? true : false;

    // Check if there's a pending follow request
    $requestStmt = $db->prepare("SELECT id FROM follow_requests WHERE from_user_id = ? AND to_user_id = ? AND status = 'pending'");
    $requestStmt->execute([$currentUserId, $targetUserId]);
    $user['hasPendingRequest'] = $requestStmt->fetch() ? true : false;

    http_response_code(200);
    echo json_encode($user);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
