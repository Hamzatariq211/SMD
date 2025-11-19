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
$targetUserId = $data['userId'] ?? null;

if (!$targetUserId) {
    http_response_code(400);
    echo json_encode(['error' => 'Target user ID is required']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    // Remove follow
    $stmt = $db->prepare("DELETE FROM follows WHERE follower_id = ? AND following_id = ?");
    $stmt->execute([$userId, $targetUserId]);

    // Also remove any pending follow requests
    $requestStmt = $db->prepare("DELETE FROM follow_requests WHERE from_user_id = ? AND to_user_id = ?");
    $requestStmt->execute([$userId, $targetUserId]);

    http_response_code(200);
    echo json_encode(['message' => 'Unfollowed successfully']);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

