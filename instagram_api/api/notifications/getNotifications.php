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

    $stmt = $db->prepare("
        SELECT
            n.id,
            n.type,
            n.title,
            n.body,
            n.reference_id as referenceId,
            n.is_read as isRead,
            UNIX_TIMESTAMP(n.created_at) * 1000 as timestamp,
            u.username as fromUsername,
            u.profile_image_url as fromProfileImage
        FROM notifications n
        LEFT JOIN users u ON n.from_user_id = u.id
        WHERE n.user_id = ?
        ORDER BY n.created_at DESC
        LIMIT 50
    ");
    $stmt->execute([$userId]);
    $notifications = $stmt->fetchAll();

    foreach ($notifications as &$notif) {
        $notif['isRead'] = (bool)$notif['isRead'];
    }

    http_response_code(200);
    echo json_encode($notifications);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

