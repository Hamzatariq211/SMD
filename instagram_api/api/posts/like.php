<?php
require_once __DIR__ . '/../../config/Database.php';
require_once __DIR__ . '/../../utils/JWT.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
    exit();
}

$userId = JWT::getUserIdFromToken();

if (!$userId) {
    http_response_code(401);
    echo json_encode(['error' => 'Unauthorized']);
    exit();
}

$data = json_decode(file_get_contents('php://input'), true);
$postId = $data['postId'] ?? null;

if (!$postId) {
    http_response_code(400);
    echo json_encode(['error' => 'Post ID is required']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    // Check if already liked
    $checkStmt = $db->prepare("SELECT id FROM post_likes WHERE post_id = ? AND user_id = ?");
    $checkStmt->execute([$postId, $userId]);

    if ($checkStmt->fetch()) {
        // Unlike
        $stmt = $db->prepare("DELETE FROM post_likes WHERE post_id = ? AND user_id = ?");
        $stmt->execute([$postId, $userId]);
        $action = 'unliked';
    } else {
        // Like
        $stmt = $db->prepare("INSERT INTO post_likes (post_id, user_id) VALUES (?, ?)");
        $stmt->execute([$postId, $userId]);
        $action = 'liked';

        // Create notification for post owner
        $postStmt = $db->prepare("SELECT user_id FROM posts WHERE id = ?");
        $postStmt->execute([$postId]);
        $post = $postStmt->fetch();

        if ($post && $post['user_id'] !== $userId) {
            $notifId = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
                mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff),
                mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000,
                mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
            );

            $userStmt = $db->prepare("SELECT username FROM users WHERE id = ?");
            $userStmt->execute([$userId]);
            $user = $userStmt->fetch();

            $notifStmt = $db->prepare("
                INSERT INTO notifications (id, user_id, from_user_id, type, title, body, reference_id)
                VALUES (?, ?, ?, 'like', ?, ?, ?)
            ");
            $notifStmt->execute([
                $notifId,
                $post['user_id'],
                $userId,
                'New Like',
                $user['username'] . ' liked your post',
                $postId
            ]);
        }
    }

    // Get updated like count
    $countStmt = $db->prepare("SELECT like_count FROM posts WHERE id = ?");
    $countStmt->execute([$postId]);
    $result = $countStmt->fetch();

    http_response_code(200);
    echo json_encode([
        'message' => 'Post ' . $action,
        'likeCount' => $result['like_count']
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

