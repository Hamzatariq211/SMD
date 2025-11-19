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

if (!isset($data['chatRoomId'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Chat room ID is required']);
    exit();
}

$chatRoomId = $data['chatRoomId'];

try {
    $db = Database::getInstance()->getConnection();

    // Get chat room details
    $chatRoomStmt = $db->prepare("
        SELECT user1_id, user2_id FROM chat_rooms WHERE id = ?
    ");
    $chatRoomStmt->execute([$chatRoomId]);
    $chatRoom = $chatRoomStmt->fetch();

    if (!$chatRoom) {
        http_response_code(404);
        echo json_encode(['error' => 'Chat room not found']);
        exit();
    }

    // Determine the other user
    $otherUserId = ($chatRoom['user1_id'] == $userId) ? $chatRoom['user2_id'] : $chatRoom['user1_id'];

    // Get current user's username
    $userStmt = $db->prepare("SELECT username FROM users WHERE id = ?");
    $userStmt->execute([$userId]);
    $currentUser = $userStmt->fetch();

    // Create notification
    $notifId = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
        mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff),
        mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000,
        mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
    );

    $notifStmt = $db->prepare("
        INSERT INTO notifications (id, user_id, from_user_id, type, title, body, reference_id)
        VALUES (?, ?, ?, 'screenshot', ?, ?, ?)
    ");
    $notifStmt->execute([
        $notifId,
        $otherUserId,
        $userId,
        'Screenshot Alert',
        $currentUser['username'] . ' took a screenshot',
        $chatRoomId
    ]);

    // Send FCM push notification for screenshot alert
    $otherUserStmt = $db->prepare("SELECT fcm_token FROM users WHERE id = ?");
    $otherUserStmt->execute([$otherUserId]);
    $otherUserData = $otherUserStmt->fetch();

    if ($otherUserData && !empty($otherUserData['fcm_token'])) {
        FCMNotification::sendScreenshotAlert(
            $otherUserData['fcm_token'],
            $currentUser['username'],
            $userId
        );
    }

    http_response_code(200);
    echo json_encode([
        'message' => 'Screenshot alert sent successfully',
        'notificationId' => $notifId
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

