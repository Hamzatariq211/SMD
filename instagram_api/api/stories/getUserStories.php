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

    // Get active stories (not expired)
    $stmt = $db->prepare("
        SELECT
            s.id as storyId,
            s.user_id as userId,
            u.username,
            u.profile_image_url as userProfileImage,
            s.story_url as storyImageBase64,
            UNIX_TIMESTAMP(s.created_at) * 1000 as timestamp,
            UNIX_TIMESTAMP(s.expires_at) * 1000 as expiryTime,
            s.view_count as viewCount
        FROM stories s
        JOIN users u ON s.user_id = u.id
        WHERE s.user_id = ?
        AND s.expires_at > NOW()
        ORDER BY s.created_at ASC
    ");
    $stmt->execute([$targetUserId]);
    $stories = $stmt->fetchAll(PDO::FETCH_ASSOC);

    http_response_code(200);
    echo json_encode($stories);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
