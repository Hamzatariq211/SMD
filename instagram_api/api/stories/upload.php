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

$input = json_decode(file_get_contents('php://input'), true);

if (!isset($input['storyImageBase64']) || empty($input['storyImageBase64'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Story image is required']);
    exit();
}

$storyImageBase64 = $input['storyImageBase64'];

try {
    $db = Database::getInstance()->getConnection();

    // Generate UUID for story ID
    $storyId = sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
        mt_rand(0, 0xffff), mt_rand(0, 0xffff),
        mt_rand(0, 0xffff),
        mt_rand(0, 0x0fff) | 0x4000,
        mt_rand(0, 0x3fff) | 0x8000,
        mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
    );

    // Calculate expiry time (24 hours from now)
    $expiryTime = date('Y-m-d H:i:s', strtotime('+24 hours'));

    // Insert story
    $stmt = $db->prepare("
        INSERT INTO stories (id, user_id, story_url, story_type, created_at, expires_at)
        VALUES (?, ?, ?, 'image', NOW(), ?)
    ");
    $stmt->execute([$storyId, $userId, $storyImageBase64, $expiryTime]);

    http_response_code(201);
    echo json_encode([
        'message' => 'Story uploaded successfully',
        'storyId' => $storyId
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
