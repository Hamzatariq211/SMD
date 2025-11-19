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

if (!isset($data['postId']) || !isset($data['commentText'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Post ID and comment text are required']);
    exit();
}

$postId = $data['postId'];
$commentText = trim($data['commentText']);

if (empty($commentText)) {
    http_response_code(400);
    echo json_encode(['error' => 'Comment cannot be empty']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    // Generate comment ID
    $commentId = sprintf(
        '%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
        mt_rand(0, 0xffff), mt_rand(0, 0xffff),
        mt_rand(0, 0xffff),
        mt_rand(0, 0x0fff) | 0x4000,
        mt_rand(0, 0x3fff) | 0x8000,
        mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
    );

    // Insert comment
    $stmt = $db->prepare("
        INSERT INTO comments (id, post_id, user_id, comment_text)
        VALUES (?, ?, ?, ?)
    ");
    $stmt->execute([$commentId, $postId, $userId, $commentText]);

    // Get user info
    $userStmt = $db->prepare("SELECT username, profile_image_url FROM users WHERE id = ?");
    $userStmt->execute([$userId]);
    $user = $userStmt->fetch();

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

        $notifStmt = $db->prepare("
            INSERT INTO notifications (id, user_id, from_user_id, type, title, body, reference_id)
            VALUES (?, ?, ?, 'comment', ?, ?, ?)
        ");
        $notifStmt->execute([
            $notifId,
            $post['user_id'],
            $userId,
            'New Comment',
            $user['username'] . ' commented on your post',
            $postId
        ]);
    }

    http_response_code(201);
    echo json_encode([
        'message' => 'Comment added successfully',
        'comment' => [
            'commentId' => $commentId,
            'userId' => $userId,
            'username' => $user['username'],
            'userProfileImage' => $user['profile_image_url'],
            'commentText' => $commentText,
            'timestamp' => time() * 1000
        ]
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

