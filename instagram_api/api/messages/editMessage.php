<?php
require_once __DIR__ . '/../../config/Database.php';
require_once __DIR__ . '/../../utils/JWT.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'PUT') {
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
$messageId = $data['messageId'] ?? null;
$newText = $data['messageText'] ?? null;

if (!$messageId || !$newText) {
    http_response_code(400);
    echo json_encode(['error' => 'Message ID and new text are required']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    // Get message details
    $stmt = $db->prepare("SELECT sender_id, created_at FROM messages WHERE id = ?");
    $stmt->execute([$messageId]);
    $message = $stmt->fetch();

    if (!$message) {
        http_response_code(404);
        echo json_encode(['error' => 'Message not found']);
        exit();
    }

    if ($message['sender_id'] !== $userId) {
        http_response_code(403);
        echo json_encode(['error' => 'You can only edit your own messages']);
        exit();
    }

    // Check if within 5 minutes
    $currentTime = time() * 1000;
    $messageTime = $message['created_at'];
    $fiveMinutes = 5 * 60 * 1000;

    if (($currentTime - $messageTime) > $fiveMinutes) {
        http_response_code(400);
        echo json_encode(['error' => 'Messages can only be edited within 5 minutes']);
        exit();
    }

    // Update message
    $updateStmt = $db->prepare("
        UPDATE messages
        SET message_text = ?, is_edited = TRUE, edited_at = ?
        WHERE id = ?
    ");
    $updateStmt->execute([$newText, $currentTime, $messageId]);

    http_response_code(200);
    echo json_encode(['message' => 'Message edited successfully']);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

