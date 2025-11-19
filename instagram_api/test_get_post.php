<?php
// Test script to verify getPost.php endpoint
require_once __DIR__ . '/config/Database.php';

echo "<h2>Testing getPost.php endpoint</h2>";

try {
    $db = Database::getInstance()->getConnection();

    // Get a sample post ID
    $stmt = $db->prepare("SELECT id FROM posts LIMIT 1");
    $stmt->execute();
    $result = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($result) {
        $testPostId = $result['id'];
        echo "<p>Found test post ID: $testPostId</p>";

        // Test the getPost endpoint
        echo "<p>Testing: GET /api/posts/getPost.php?postId=$testPostId</p>";
        echo "<p>You can test this endpoint manually with a valid JWT token</p>";
    } else {
        echo "<p>No posts found in database. Please create a post first.</p>";
    }

} catch (Exception $e) {
    echo "<p style='color: red;'>Error: " . $e->getMessage() . "</p>";
}
?>

