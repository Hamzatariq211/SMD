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

try {
    $db = Database::getInstance()->getConnection();

    // Get all users except current user with their follow status and counts
    $stmt = $db->prepare("
        SELECT
            u.id,
            u.username,
            u.first_name as firstName,
            u.last_name as lastName,
            u.profile_image_url as profileImageUrl,
            u.bio,
            u.is_private as isPrivate,
            u.is_online as isOnline,
            u.last_seen as lastSeen,
            (SELECT COUNT(*) FROM follows WHERE following_id = u.id) as followersCount,
            (SELECT COUNT(*) FROM follows WHERE follower_id = u.id) as followingCount,
            EXISTS(SELECT 1 FROM follows WHERE follower_id = ? AND following_id = u.id) as isFollowing,
            EXISTS(SELECT 1 FROM follow_requests WHERE from_user_id = ? AND to_user_id = u.id AND status = 'pending') as hasPendingRequest
        FROM users u
        WHERE u.id != ?
        ORDER BY u.username ASC
    ");
    $stmt->execute([$currentUserId, $currentUserId, $currentUserId]);
    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Convert boolean fields
    foreach ($users as &$user) {
        $user['isPrivate'] = (bool)$user['isPrivate'];
        $user['isOnline'] = (bool)$user['isOnline'];
        $user['isFollowing'] = (bool)$user['isFollowing'];
        $user['hasPendingRequest'] = (bool)$user['hasPendingRequest'];
        $user['followersCount'] = (int)$user['followersCount'];
        $user['followingCount'] = (int)$user['followingCount'];
        $user['lastSeen'] = (int)$user['lastSeen'];
    }

    http_response_code(200);
    echo json_encode($users);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

