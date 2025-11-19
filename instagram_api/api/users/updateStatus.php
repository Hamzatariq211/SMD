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

if (!isset($data['isOnline'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Online status is required']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    $isOnline = $data['isOnline'] ? 1 : 0;
    $lastSeen = time() * 1000;

    $stmt = $db->prepare("UPDATE users SET is_online = ?, last_seen = ? WHERE id = ?");
    $stmt->execute([$isOnline, $lastSeen, $userId]);

    http_response_code(200);
    echo json_encode(['message' => 'Status updated successfully']);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

