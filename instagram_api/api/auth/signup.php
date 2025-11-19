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

// Validate required fields
$required = ['email', 'password', 'username', 'firstName', 'lastName'];
foreach ($required as $field) {
    if (!isset($data[$field]) || empty(trim($data[$field]))) {
        http_response_code(400);
        echo json_encode(['error' => ucfirst($field) . ' is required']);
        exit();
    }
}

$email = trim($data['email']);
$password = $data['password'];
$username = trim($data['username']);
$firstName = trim($data['firstName']);
$lastName = trim($data['lastName']);

// Validate email
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    http_response_code(400);
    echo json_encode(['error' => 'Invalid email format']);
    exit();
}

// Validate password length
if (strlen($password) < 6) {
    http_response_code(400);
    echo json_encode(['error' => 'Password must be at least 6 characters']);
    exit();
}

// Validate username
if (!preg_match('/^[a-zA-Z0-9_]{3,20}$/', $username)) {
    http_response_code(400);
    echo json_encode(['error' => 'Username must be 3-20 characters (letters, numbers, underscore only)']);
    exit();
}

try {
    $db = Database::getInstance()->getConnection();

    // Check if email exists
    $stmt = $db->prepare("SELECT id FROM users WHERE email = ?");
    $stmt->execute([$email]);
    if ($stmt->fetch()) {
        http_response_code(409);
        echo json_encode(['error' => 'Email already registered']);
        exit();
    }

    // Check if username exists
    $stmt = $db->prepare("SELECT id FROM users WHERE username = ?");
    $stmt->execute([$username]);
    if ($stmt->fetch()) {
        http_response_code(409);
        echo json_encode(['error' => 'Username already taken']);
        exit();
    }

    // Generate UUID for user
    $userId = sprintf(
        '%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
        mt_rand(0, 0xffff), mt_rand(0, 0xffff),
        mt_rand(0, 0xffff),
        mt_rand(0, 0x0fff) | 0x4000,
        mt_rand(0, 0x3fff) | 0x8000,
        mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
    );

    // Hash password
    $passwordHash = password_hash($password, PASSWORD_BCRYPT);

    // Insert user
    $insertStmt = $db->prepare("
        INSERT INTO users (id, email, password_hash, username, first_name, last_name, is_profile_setup)
        VALUES (?, ?, ?, ?, ?, ?, FALSE)
    ");
    $insertStmt->execute([$userId, $email, $passwordHash, $username, $firstName, $lastName]);

    // Generate JWT token
    $token = JWT::encode([
        'userId' => $userId,
        'email' => $email
    ]);

    // Create session
    $sessionStmt = $db->prepare("
        INSERT INTO user_sessions (user_id, token, device_id, device_type, ip_address, expires_at)
        VALUES (?, ?, ?, ?, ?, FROM_UNIXTIME(?))
    ");
    $deviceId = $_SERVER['HTTP_USER_AGENT'] ?? 'unknown';
    $expiresAt = time() + JWT_EXPIRATION;
    $sessionStmt->execute([
        $userId,
        $token,
        $deviceId,
        'mobile',
        $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0',
        $expiresAt
    ]);

    http_response_code(201);
    echo json_encode([
        'message' => 'Registration successful',
        'userId' => $userId,
        'token' => $token,
        'isProfileSetup' => false
    ]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>

