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

try {
    $db = Database::getInstance()->getConnection();

    // Get all chat rooms for current user with other user details
    $stmt = $db->prepare("
        SELECT
            cr.id as chatRoomId,
            CASE
                WHEN cr.user1_id = ? THEN cr.user2_id
                ELSE cr.user1_id
            END as otherUserId,
            u.username,
            u.profile_image_url as profileImageUrl,
            u.is_online,
            u.last_seen,
            cr.last_message as lastMessage,
            cr.last_message_time as lastMessageTime,
            CASE
                WHEN cr.user1_id = ? THEN cr.user1_unread
                ELSE cr.user2_unread
            END as unreadCount
        FROM chat_rooms cr
        JOIN users u ON u.id = CASE
            WHEN cr.user1_id = ? THEN cr.user2_id
            ELSE cr.user1_id
        END
        WHERE cr.user1_id = ? OR cr.user2_id = ?
        ORDER BY cr.last_message_time DESC
    ");
    $stmt->execute([$userId, $userId, $userId, $userId, $userId]);
    $chatList = $stmt->fetchAll();

    // Convert to expected format
    foreach ($chatList as &$chat) {
        $chat['isOnline'] = (bool)$chat['is_online'];
        $chat['lastSeen'] = (int)$chat['last_seen'];
        $chat['unreadCount'] = (int)$chat['unreadCount'];

        // Remove snake_case fields to avoid confusion
        unset($chat['is_online']);
        unset($chat['last_seen']);
    }

    http_response_code(200);
    echo json_encode($chatList);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
