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
$storyId = $data['storyId'] ?? null;

if (!$storyId) {
    http_response_code(400);
    echo json_encode(['error' => 'Story ID is required']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    // Check if already viewed
    $checkStmt = $db->prepare("SELECT id FROM story_views WHERE story_id = ? AND user_id = ?");
    $checkStmt->execute([$storyId, $userId]);

    if (!$checkStmt->fetch()) {
        // Add view
        $stmt = $db->prepare("INSERT INTO story_views (story_id, user_id) VALUES (?, ?)");
        $stmt->execute([$storyId, $userId]);
    }

    http_response_code(200);
    echo json_encode(['message' => 'Story view recorded']);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

