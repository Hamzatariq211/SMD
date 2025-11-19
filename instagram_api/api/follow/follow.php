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
$targetUserId = $data['userId'] ?? null;

if (!$targetUserId) {
    http_response_code(400);
    echo json_encode(['error' => 'Target user ID is required']);
    exit();
}

if ($userId === $targetUserId) {
    http_response_code(400);
    echo json_encode(['error' => 'Cannot follow yourself']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    // Check if target user is private
    $userStmt = $db->prepare("SELECT is_private FROM users WHERE id = ?");
    $userStmt->execute([$targetUserId]);
    $targetUser = $userStmt->fetch();

    if (!$targetUser) {
        http_response_code(404);
        echo json_encode(['error' => 'User not found']);
        exit();
    }

    if ($targetUser['is_private']) {
        // Create follow request
        $requestId = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff),
            mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000,
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
        );

        $stmt = $db->prepare("
            INSERT INTO follow_requests (id, from_user_id, to_user_id, status)
            VALUES (?, ?, ?, 'pending')
            ON DUPLICATE KEY UPDATE status = 'pending'
        ");
        $stmt->execute([$requestId, $userId, $targetUserId]);

        // Create notification
        $notifId = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff),
            mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000,
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
        );

        $currentUserStmt = $db->prepare("SELECT username FROM users WHERE id = ?");
        $currentUserStmt->execute([$userId]);
        $currentUser = $currentUserStmt->fetch();

        $notifStmt = $db->prepare("
            INSERT INTO notifications (id, user_id, from_user_id, type, title, body, reference_id)
            VALUES (?, ?, ?, 'follow_request', ?, ?, ?)
        ");
        $notifStmt->execute([
            $notifId,
            $targetUserId,
            $userId,
            'Follow Request',
            $currentUser['username'] . ' wants to follow you',
            $requestId
        ]);

        // Send FCM push notification for follow request
        $targetUserStmt = $db->prepare("SELECT fcm_token, profile_picture FROM users WHERE id = ?");
        $targetUserStmt->execute([$targetUserId]);
        $targetUserData = $targetUserStmt->fetch();

        if ($targetUserData && !empty($targetUserData['fcm_token'])) {
            $requesterProfileStmt = $db->prepare("SELECT profile_picture FROM users WHERE id = ?");
            $requesterProfileStmt->execute([$userId]);
            $requesterProfile = $requesterProfileStmt->fetch();
            $requesterImage = $requesterProfile['profile_picture'] ?? '';

            FCMNotification::sendFollowRequestNotification(
                $targetUserData['fcm_token'],
                $currentUser['username'],
                $userId,
                $requesterImage
            );
        }

        $message = 'Follow request sent';
    } else {
        // Direct follow
        $stmt = $db->prepare("
            INSERT INTO follows (follower_id, following_id)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE follower_id = follower_id
        ");
        $stmt->execute([$userId, $targetUserId]);

        // Create notification
        $notifId = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff),
            mt_rand(0, 0x0fff) | 0x4000, mt_rand(0, 0x3fff) | 0x8000,
            mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
        );

        $currentUserStmt = $db->prepare("SELECT username FROM users WHERE id = ?");
        $currentUserStmt->execute([$userId]);
        $currentUser = $currentUserStmt->fetch();

        $notifStmt = $db->prepare("
            INSERT INTO notifications (id, user_id, from_user_id, type, title, body)
            VALUES (?, ?, ?, 'follow', ?, ?)
        ");
        $notifStmt->execute([
            $notifId,
            $targetUserId,
            $userId,
            'New Follower',
            $currentUser['username'] . ' started following you'
        ]);

        // Send FCM push notification for new follower
        $targetUserStmt = $db->prepare("SELECT fcm_token FROM users WHERE id = ?");
        $targetUserStmt->execute([$targetUserId]);
        $targetUserData = $targetUserStmt->fetch();

        if ($targetUserData && !empty($targetUserData['fcm_token'])) {
            $followerProfileStmt = $db->prepare("SELECT profile_picture FROM users WHERE id = ?");
            $followerProfileStmt->execute([$userId]);
            $followerProfile = $followerProfileStmt->fetch();
            $followerImage = $followerProfile['profile_picture'] ?? '';

            FCMNotification::sendNewFollowerNotification(
                $targetUserData['fcm_token'],
                $currentUser['username'],
                $userId,
                $followerImage
            );
        }

        $message = 'Following successfully';
    }

    http_response_code(200);
    echo json_encode(['message' => $message]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
