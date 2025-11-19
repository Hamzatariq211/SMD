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

$page = isset($_GET['page']) ? (int)$_GET['page'] : 1;
$limit = 20;
$offset = ($page - 1) * $limit;

try {
    $db = Database::getInstance()->getConnection();

    // Get posts from users that current user follows + own posts
    $stmt = $db->prepare("
        SELECT
            p.id as postId,
            p.user_id as userId,
            u.username,
            u.first_name,
            u.last_name,
            u.profile_image_url as userProfileImage,
            p.image_url as postImageBase64,
            p.caption,
            p.like_count as likeCount,
            p.comment_count as commentCount,
            UNIX_TIMESTAMP(p.created_at) * 1000 as timestamp,
            EXISTS(SELECT 1 FROM post_likes WHERE post_id = p.id AND user_id = ?) as isLiked
        FROM posts p
        JOIN users u ON p.user_id = u.id
        WHERE p.user_id IN (
            SELECT following_id FROM follows WHERE follower_id = ?
            UNION
            SELECT ?
        )
        ORDER BY p.created_at DESC
        LIMIT ? OFFSET ?
    ");
    $stmt->execute([$currentUserId, $currentUserId, $currentUserId, $limit, $offset]);
    $posts = $stmt->fetchAll();

    // Convert isLiked to boolean
    foreach ($posts as &$post) {
        $post['isLiked'] = (bool)$post['isLiked'];
    }

    http_response_code(200);
    echo json_encode($posts);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
