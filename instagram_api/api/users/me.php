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
            id, email, username, first_name, last_name, date_of_birth,
            profile_image_url, cover_image_url, bio, website, phone, gender,
            is_profile_setup, is_private, is_online, last_seen
        FROM users WHERE id = ?
    ");
    $stmt->execute([$userId]);
    $user = $stmt->fetch();

    if (!$user) {
        http_response_code(404);
        echo json_encode(['error' => 'User not found']);
        exit();
    }

    // Format response to match Kotlin UserProfile data class
    $response = [
        'id' => $user['id'],
        'email' => $user['email'],
        'username' => $user['username'],
        'firstName' => $user['first_name'],
        'lastName' => $user['last_name'],
        'bio' => $user['bio'],
        'website' => $user['website'],
        'phone' => $user['phone'],
        'gender' => $user['gender'],
        'profileImageUrl' => $user['profile_image_url'],
        'coverImageUrl' => $user['cover_image_url'],
        'isProfileSetup' => (bool)$user['is_profile_setup'],
        'isPrivate' => (bool)$user['is_private'],
        'isOnline' => (bool)$user['is_online'],
        'lastSeen' => (int)$user['last_seen']
    ];

    http_response_code(200);
    echo json_encode($response);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
