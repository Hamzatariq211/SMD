<?php
require_once __DIR__ . '/../../config/Database.php';
require_once __DIR__ . '/../../utils/JWT.php';

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
    exit();
}

$data = json_decode(file_get_contents('php://input'), true);

if (!isset($data['email']) || !isset($data['password'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Email and password are required']);
    exit();
}

$email = trim($data['email']);
$password = $data['password'];

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    http_response_code(400);
    echo json_encode(['error' => 'Invalid email format']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    $stmt = $db->prepare("SELECT * FROM users WHERE email = ?");
    $stmt->execute([$email]);
    $user = $stmt->fetch();

    if (!$user || !password_verify($password, $user['password_hash'])) {
        http_response_code(401);
        echo json_encode(['error' => 'Invalid email or password']);
        exit();
    }

    // Update online status
    $updateStmt = $db->prepare("UPDATE users SET is_online = TRUE, last_seen = ? WHERE id = ?");
    $updateStmt->execute([time() * 1000, $user['id']]);

    // Generate JWT token
    $token = JWT::encode([
        'userId' => $user['id'],
        'email' => $user['email']
    ]);

    // Create session record
    $sessionStmt = $db->prepare("
        INSERT INTO user_sessions (user_id, token, device_id, device_type, ip_address, expires_at)
        VALUES (?, ?, ?, ?, ?, FROM_UNIXTIME(?))
    ");
    $deviceId = $_SERVER['HTTP_USER_AGENT'] ?? 'unknown';
    $expiresAt = time() + JWT_EXPIRATION;
    $sessionStmt->execute([
        $user['id'],
        $token,
        $deviceId,
        'mobile',
        $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0',
        $expiresAt
    ]);

    http_response_code(200);
    echo json_encode([
        'message' => 'Login successful',
        'userId' => $user['id'],
        'token' => $token,
        'isProfileSetup' => (bool)$user['is_profile_setup']
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

