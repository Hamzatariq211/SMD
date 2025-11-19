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

try {
    $db = Database::getInstance()->getConnection();

    // Update online status
    $stmt = $db->prepare("UPDATE users SET is_online = FALSE, last_seen = ? WHERE id = ?");
    $stmt->execute([time() * 1000, $userId]);

    // Invalidate all active sessions for this user
    $sessionStmt = $db->prepare("UPDATE user_sessions SET is_active = FALSE WHERE user_id = ? AND is_active = TRUE");
    $sessionStmt->execute([$userId]);

    http_response_code(200);
    echo json_encode(['message' => 'Logged out successfully']);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

