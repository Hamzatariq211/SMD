<?php
require_once __DIR__ . '/../../config/Database.php';
require_once __DIR__ . '/../../utils/JWT.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'PUT') {
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

try {
    $db = Database::getInstance()->getConnection();

    $updates = [];
    $params = [];

    // Build dynamic update query
    $allowedFields = [
        'firstName' => 'first_name',
        'lastName' => 'last_name',
        'username' => 'username',
        'bio' => 'bio',
        'website' => 'website',
        'email' => 'email',
        'phone' => 'phone',
        'gender' => 'gender',
        'profileImageUrl' => 'profile_image_url',
        'isPrivate' => 'is_private'
    ];

    foreach ($allowedFields as $jsonField => $dbField) {
        if (isset($data[$jsonField])) {
            $updates[] = "$dbField = ?";
            $params[] = $data[$jsonField];
        }
    }

    // Always set is_profile_setup to true when updating profile
    $updates[] = "is_profile_setup = TRUE";

    if (empty($updates)) {
        http_response_code(400);
        echo json_encode(['error' => 'No fields to update']);
        exit();
    }

    // Check if username is being changed and if it's already taken
    if (isset($data['username'])) {
        $checkStmt = $db->prepare("SELECT id FROM users WHERE username = ? AND id != ?");
        $checkStmt->execute([$data['username'], $userId]);
        if ($checkStmt->fetch()) {
            http_response_code(409);
            echo json_encode(['error' => 'Username already taken']);
            exit();
        }
    }

    $params[] = $userId;
    $sql = "UPDATE users SET " . implode(', ', $updates) . " WHERE id = ?";

    $stmt = $db->prepare($sql);
    $stmt->execute($params);

    http_response_code(200);
    echo json_encode(['message' => 'Profile updated successfully']);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
