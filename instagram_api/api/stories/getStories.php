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

    // Get stories from users that current user follows + own stories
    $stmt = $db->prepare("
        SELECT
            s.id as storyId,
            s.user_id as userId,
            u.username,
            u.profile_image_url as userProfileImage,
            s.story_url as storyImageBase64,
            UNIX_TIMESTAMP(s.created_at) * 1000 as timestamp,
            UNIX_TIMESTAMP(s.expires_at) * 1000 as expiryTime,
            s.view_count
        FROM stories s
        JOIN users u ON s.user_id = u.id
        WHERE s.expires_at > NOW()
        AND s.user_id IN (
            SELECT following_id FROM follows WHERE follower_id = ?
            UNION
            SELECT ?
        )
        ORDER BY s.created_at DESC
    ");
    $stmt->execute([$currentUserId, $currentUserId]);
    $stories = $stmt->fetchAll();

    // Group stories by user
    $userStories = [];
    foreach ($stories as $story) {
        $userId = $story['userId'];
        if (!isset($userStories[$userId])) {
            $userStories[$userId] = [
                'userId' => $userId,
                'username' => $story['username'],
                'userProfileImage' => $story['userProfileImage'],
                'stories' => []
            ];
        }
        $userStories[$userId]['stories'][] = $story;
    }

    http_response_code(200);
    echo json_encode(array_values($userStories));

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

