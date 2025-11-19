<?php
require_once __DIR__ . '/../../config/Database.php';
require_once __DIR__ . '/../../utils/JWT.php';
require_once __DIR__ . '/../../utils/FCMNotification.php';

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

if (!isset($data['receiverId']) || !isset($data['messageText'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Receiver ID and message text are required']);
    exit();
}

$receiverId = $data['receiverId'];
$messageText = trim($data['messageText']);
$messageType = $data['messageType'] ?? 'text';
$mediaUrl = $data['mediaUrl'] ?? '';
$postId = $data['postId'] ?? '';
$isVanishMode = $data['isVanishMode'] ?? false;

try {
    $db = Database::getInstance()->getConnection();

    // Get or create chat room
    $chatRoomStmt = $db->prepare("
        SELECT id FROM chat_rooms
        WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)
    ");
    $chatRoomStmt->execute([$userId, $receiverId, $receiverId, $userId]);
    $chatRoom = $chatRoomStmt->fetch();

    if (!$chatRoom) {
        // Create new chat room
        $chatRoomId = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff),
            mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000,
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
        );

        $createRoomStmt = $db->prepare("
            INSERT INTO chat_rooms (id, user1_id, user2_id, vanish_mode)
            VALUES (?, ?, ?, ?)
        ");
        $createRoomStmt->execute([$chatRoomId, $userId, $receiverId, $isVanishMode]);
    } else {
        $chatRoomId = $chatRoom['id'];
    }

    // Generate message ID
    $messageId = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
        mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff),
        mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000,
        mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
    );

    $timestamp = time() * 1000;

    // Insert message
    $messageStmt = $db->prepare("
        INSERT INTO messages (id, chat_room_id, sender_id, receiver_id, message_text, message_type, media_url, post_id, is_vanish_mode, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ");
    $messageStmt->execute([
        $messageId,
        $chatRoomId,
        $userId,
        $receiverId,
        $messageText,
        $messageType,
        $mediaUrl,
        $postId,
        $isVanishMode,
        $timestamp
    ]);

    // Update chat room's last message
    $updateRoomStmt = $db->prepare("
        UPDATE chat_rooms
        SET last_message = ?, last_message_time = ?, last_message_sender_id = ?
        WHERE id = ?
    ");
    $updateRoomStmt->execute([$messageText, $timestamp, $userId, $chatRoomId]);

    // Increment unread count for receiver
    $unreadField = ($userId === $chatRoom['user1_id'] ?? $userId) ? 'user2_unread' : 'user1_unread';
    $incrementStmt = $db->prepare("UPDATE chat_rooms SET $unreadField = $unreadField + 1 WHERE id = ?");
    $incrementStmt->execute([$chatRoomId]);

    // Create notification
    $notifId = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
        mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff),
        mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000,
        mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
    );

    $senderStmt = $db->prepare("SELECT username FROM users WHERE id = ?");
    $senderStmt->execute([$userId]);
    $sender = $senderStmt->fetch();

    $notifStmt = $db->prepare("
        INSERT INTO notifications (id, user_id, from_user_id, type, title, body, reference_id)
        VALUES (?, ?, ?, 'message', ?, ?, ?)
    ");
    $notifStmt->execute([
        $notifId,
        $receiverId,
        $userId,
        'New Message',
        $sender['username'] . ': ' . substr($messageText, 0, 50),
        $messageId
    ]);

    // Send FCM push notification
    $receiverStmt = $db->prepare("SELECT fcm_token FROM users WHERE id = ?");
    $receiverStmt->execute([$receiverId]);
    $receiver = $receiverStmt->fetch();

    if ($receiver && !empty($receiver['fcm_token'])) {
        // Get sender profile image
        $senderProfileStmt = $db->prepare("SELECT profile_picture FROM users WHERE id = ?");
        $senderProfileStmt->execute([$userId]);
        $senderProfile = $senderProfileStmt->fetch();
        $senderImage = $senderProfile['profile_picture'] ?? '';

        // Send push notification for new message
        FCMNotification::sendMessageNotification(
            $receiver['fcm_token'],
            $sender['username'],
            $messageText,
            $userId,
            $senderImage
        );
    }

    http_response_code(201);
    echo json_encode([
        'message' => 'Message sent successfully',
        'messageId' => $messageId,
        'chatRoomId' => $chatRoomId,
        'timestamp' => $timestamp
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
