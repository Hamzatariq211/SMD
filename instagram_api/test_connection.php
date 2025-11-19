<?php
// Simple MySQL connection test
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>MySQL Connection Test</h2>";

$host = 'localhost';
$user = 'root';
$pass = '';
$db = 'instagram_clone';

try {
    $conn = new PDO("mysql:host=$host", $user, $pass);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "<p style='color: green;'>✓ MySQL Server Connection: SUCCESS</p>";

    // Try to connect to the database
    $conn->exec("USE $db");
    echo "<p style='color: green;'>✓ Database '$db' Connection: SUCCESS</p>";

    // Test query
    $stmt = $conn->query("SELECT COUNT(*) as count FROM users");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "<p style='color: green;'>✓ Query Test: SUCCESS (Found {$result['count']} users)</p>";

    echo "<h3 style='color: green;'>All tests passed! MySQL is working correctly.</h3>";

} catch(PDOException $e) {
    echo "<p style='color: red;'>✗ Connection failed: " . $e->getMessage() . "</p>";

    if (strpos($e->getMessage(), "Connection refused") !== false) {
        echo "<p>MySQL server is not running. Start it in XAMPP Control Panel.</p>";
    } elseif (strpos($e->getMessage(), "Unknown database") !== false) {
        echo "<p>Database 'instagram_clone' doesn't exist. You need to import schema.sql</p>";
    }
}
?>

