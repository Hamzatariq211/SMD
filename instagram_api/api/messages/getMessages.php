<?php
require_once __DIR__ . '/../../config/Database.php';
require_once __DIR__ . '/../../utils/JWT.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
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

$otherUserId = $_GET['userId'] ?? null;

if (!$otherUserId) {
    http_response_code(400);
    echo json_encode(['error' => 'Other user ID is required']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    // Get chat room
    $roomStmt = $db->prepare("
        SELECT id, vanish_mode FROM chat_rooms
        WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)
    ");
    $roomStmt->execute([$userId, $otherUserId, $otherUserId, $userId]);
    $chatRoom = $roomStmt->fetch();

    if (!$chatRoom) {
        http_response_code(200);
        echo json_encode([]);
        exit();
    }

    $chatRoomId = $chatRoom['id'];
    $isVanishMode = $chatRoom['vanish_mode'];

    // Get messages
    $stmt = $db->prepare("
        SELECT
            id as messageId,
            sender_id as senderId,
            receiver_id as receiverId,
            message_text as messageText,
            message_type as messageType,
            media_url as imageBase64,
            post_id as postId,
            is_edited as isEdited,
            edited_at as editedAt,
            is_deleted as isDeleted,
            is_seen as isSeen,
            created_at as timestamp
        FROM messages
        WHERE chat_room_id = ? AND is_deleted = FALSE
        ORDER BY created_at ASC
    ");
    $stmt->execute([$chatRoomId]);
    $messages = $stmt->fetchAll();

    // Convert boolean fields
    foreach ($messages as &$message) {
        $message['isEdited'] = (bool)$message['isEdited'];
        $message['isDeleted'] = (bool)$message['isDeleted'];
        $message['isSeen'] = (bool)$message['isSeen'];
    }

    // Mark messages as seen
    $updateStmt = $db->prepare("
        UPDATE messages SET is_seen = TRUE, seen_at = ?
        WHERE chat_room_id = ? AND receiver_id = ? AND is_seen = FALSE
    ");
    $updateStmt->execute([time() * 1000, $chatRoomId, $userId]);

    // Reset unread count
    $unreadField = ($chatRoom['user1_id'] ?? null) === $userId ? 'user1_unread' : 'user2_unread';
    $resetStmt = $db->prepare("UPDATE chat_rooms SET $unreadField = 0 WHERE id = ?");
    $resetStmt->execute([$chatRoomId]);

    http_response_code(200);
    echo json_encode($messages);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

