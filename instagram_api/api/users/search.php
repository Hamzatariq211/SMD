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

$query = $_GET['query'] ?? '';

if (empty(trim($query))) {
    http_response_code(400);
    echo json_encode(['error' => 'Search query is required']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    $searchTerm = "%$query%";
    $stmt = $db->prepare("
        SELECT
            id, username, first_name, last_name, profile_image_url, bio,
            (SELECT COUNT(*) FROM follows WHERE follower_id = u.id) as following_count,
            (SELECT COUNT(*) FROM follows WHERE following_id = u.id) as followers_count
        FROM users u
        WHERE (username LIKE ? OR first_name LIKE ? OR last_name LIKE ?)
        AND id != ?
        LIMIT 50
    ");
    $stmt->execute([$searchTerm, $searchTerm, $searchTerm, $currentUserId]);
    $users = $stmt->fetchAll();

    http_response_code(200);
    echo json_encode($users);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

