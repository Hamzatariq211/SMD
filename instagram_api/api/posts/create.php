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
    // Enhanced error message for debugging
    $headers = function_exists('getallheaders') ? getallheaders() : [];
    $authHeader = isset($headers['Authorization']) ? 'Present' : 'Missing';

    http_response_code(401);
    echo json_encode([
        'error' => 'Unauthorized - Please login first',
        'debug' => [
            'authHeader' => $authHeader,
            'message' => 'No valid authentication token found'
        ]
    ]);
    exit();
}

$data = json_decode(file_get_contents('php://input'), true);

if (!isset($data['imageBase64']) || empty($data['imageBase64'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Image is required']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    // Generate post ID
    $postId = sprintf(
        '%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
        mt_rand(0, 0xffff), mt_rand(0, 0xffff),
        mt_rand(0, 0xffff),
        mt_rand(0, 0x0fff) | 0x4000,
        mt_rand(0, 0x3fff) | 0x8000,
        mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
    );

    $imageBase64 = $data['imageBase64'];
    $caption = $data['caption'] ?? '';
    $location = $data['location'] ?? '';

    // Insert post
    $stmt = $db->prepare("
        INSERT INTO posts (id, user_id, image_url, caption, location)
        VALUES (?, ?, ?, ?, ?)
    ");
    $stmt->execute([$postId, $userId, $imageBase64, $caption, $location]);

    // Get user info for response
    $userStmt = $db->prepare("SELECT username, first_name, last_name, profile_image_url FROM users WHERE id = ?");
    $userStmt->execute([$userId]);
    $user = $userStmt->fetch();

    http_response_code(201);
    echo json_encode([
        'message' => 'Post created successfully',
        'postId' => $postId,
        'post' => [
            'postId' => $postId,
            'userId' => $userId,
            'username' => $user['username'],
            'userProfileImage' => $user['profile_image_url'],
            'postImageBase64' => $imageBase64,
            'caption' => $caption,
            'timestamp' => time() * 1000
        ]
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
