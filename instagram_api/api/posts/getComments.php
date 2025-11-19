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

$postId = $_GET['postId'] ?? null;

if (!$postId) {
    http_response_code(400);
    echo json_encode(['error' => 'Post ID is required']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    $stmt = $db->prepare("
        SELECT
            c.id as commentId,
            c.user_id as userId,
            u.username,
            u.profile_image_url as userProfileImage,
            c.comment_text as commentText,
            UNIX_TIMESTAMP(c.created_at) * 1000 as timestamp
        FROM comments c
        JOIN users u ON c.user_id = u.id
        WHERE c.post_id = ?
        ORDER BY c.created_at DESC
    ");
    $stmt->execute([$postId]);
    $comments = $stmt->fetchAll();

    http_response_code(200);
    echo json_encode($comments);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

