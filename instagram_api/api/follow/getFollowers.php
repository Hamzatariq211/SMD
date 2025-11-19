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

$userId = $_GET['userId'] ?? $currentUserId;

try {
    $db = Database::getInstance()->getConnection();

    // Get followers of the specified user
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
        FROM follows f
        JOIN users u ON f.follower_id = u.id
        WHERE f.following_id = ?
        ORDER BY f.created_at DESC
    ");
    $stmt->execute([$currentUserId, $currentUserId, $userId]);
    $followers = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Convert boolean fields
    foreach ($followers as &$follower) {
        $follower['isPrivate'] = (bool)$follower['isPrivate'];
        $follower['isOnline'] = (bool)$follower['isOnline'];
        $follower['isFollowing'] = (bool)$follower['isFollowing'];
        $follower['hasPendingRequest'] = (bool)$follower['hasPendingRequest'];
        $follower['followersCount'] = (int)$follower['followersCount'];
        $follower['followingCount'] = (int)$follower['followingCount'];
        $follower['lastSeen'] = (int)$follower['lastSeen'];
    }

    http_response_code(200);
    echo json_encode($followers);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

