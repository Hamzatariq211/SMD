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
$requestId = $data['requestId'] ?? null;
$action = $data['action'] ?? null; // 'accept' or 'reject'

if (!$requestId || !$action) {
    http_response_code(400);
    echo json_encode(['error' => 'Request ID and action are required']);
    exit();
}

if (!in_array($action, ['accept', 'reject'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Invalid action. Use "accept" or "reject"']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    // Get request details
    $stmt = $db->prepare("SELECT from_user_id, to_user_id FROM follow_requests WHERE id = ? AND to_user_id = ?");
    $stmt->execute([$requestId, $userId]);
    $request = $stmt->fetch();

    if (!$request) {
        http_response_code(404);
        echo json_encode(['error' => 'Follow request not found']);
        exit();
    }

    if ($action === 'accept') {
        // Add to follows
        $followStmt = $db->prepare("
            INSERT INTO follows (follower_id, following_id)
            VALUES (?, ?)
        ");
        $followStmt->execute([$request['from_user_id'], $request['to_user_id']]);

        // Update request status
        $updateStmt = $db->prepare("UPDATE follow_requests SET status = 'accepted' WHERE id = ?");
        $updateStmt->execute([$requestId]);

        // Create notification
        $notifId = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff),
            mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000,
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
        );

        $userStmt = $db->prepare("SELECT username FROM users WHERE id = ?");
        $userStmt->execute([$userId]);
        $currentUser = $userStmt->fetch();

        $notifStmt = $db->prepare("
            INSERT INTO notifications (id, user_id, from_user_id, type, title, body)
            VALUES (?, ?, ?, 'follow_accept', ?, ?)
        ");
        $notifStmt->execute([
            $notifId,
            $request['from_user_id'],
            $userId,
            'Follow Request Accepted',
            $currentUser['username'] . ' accepted your follow request'
        ]);

        // Send FCM push notification
        $requesterStmt = $db->prepare("SELECT fcm_token FROM users WHERE id = ?");
        $requesterStmt->execute([$request['from_user_id']]);
        $requesterData = $requesterStmt->fetch();

        if ($requesterData && !empty($requesterData['fcm_token'])) {
            $accepterProfileStmt = $db->prepare("SELECT profile_picture FROM users WHERE id = ?");
            $accepterProfileStmt->execute([$userId]);
            $accepterProfile = $accepterProfileStmt->fetch();
            $accepterImage = $accepterProfile['profile_picture'] ?? '';

            FCMNotification::sendNewFollowerNotification(
                $requesterData['fcm_token'],
                $currentUser['username'],
                $userId,
                $accepterImage
            );
        }

        $message = 'Follow request accepted';
    } else {
        // Update request status
        $updateStmt = $db->prepare("UPDATE follow_requests SET status = 'rejected' WHERE id = ?");
        $updateStmt->execute([$requestId]);

        $message = 'Follow request rejected';
    }

    http_response_code(200);
    echo json_encode(['message' => $message]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
