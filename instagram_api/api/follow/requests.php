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
            fr.id as requestId,
            fr.from_user_id as fromUserId,
            u.username as fromUsername,
            u.profile_image_url as fromProfileImageUrl,
            UNIX_TIMESTAMP(fr.created_at) * 1000 as timestamp
        FROM follow_requests fr
        JOIN users u ON fr.from_user_id = u.id
        WHERE fr.to_user_id = ? AND fr.status = 'pending'
        ORDER BY fr.created_at DESC
    ");
    $stmt->execute([$userId]);
    $requests = $stmt->fetchAll();

    http_response_code(200);
    echo json_encode($requests);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

