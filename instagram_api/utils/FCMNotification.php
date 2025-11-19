<?php

class FCMNotification {
    private static $projectId = 'smdassignments'; // Your Firebase project ID
    private static $serviceAccountPath = __DIR__ . '/../config/firebase-service-account.json'; // Path to service account JSON
    private static $fcmUrl = 'https://fcm.googleapis.com/v1/projects/smdassignments/messages:send';

    /**
     * Get OAuth 2.0 access token from service account
     */
    private static function getAccessToken() {
        if (!file_exists(self::$serviceAccountPath)) {
            error_log('Firebase service account JSON file not found at: ' . self::$serviceAccountPath);
            return null;
        }

        $serviceAccount = json_decode(file_get_contents(self::$serviceAccountPath), true);

        $now = time();
        $header = json_encode(['alg' => 'RS256', 'typ' => 'JWT']);
        $claimSet = json_encode([
            'iss' => $serviceAccount['client_email'],
            'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
            'aud' => 'https://oauth2.googleapis.com/token',
            'exp' => $now + 3600,
            'iat' => $now
        ]);

        $base64UrlHeader = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($header));
        $base64UrlClaimSet = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($claimSet));
        $signature = '';

        openssl_sign(
            $base64UrlHeader . '.' . $base64UrlClaimSet,
            $signature,
            $serviceAccount['private_key'],
            'SHA256'
        );

        $base64UrlSignature = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($signature));
        $jwt = $base64UrlHeader . '.' . $base64UrlClaimSet . '.' . $base64UrlSignature;

        // Exchange JWT for access token
        $ch = curl_init('https://oauth2.googleapis.com/token');
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query([
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion' => $jwt
        ]));

        $response = curl_exec($ch);
        curl_close($ch);

        $tokenData = json_decode($response, true);
        return $tokenData['access_token'] ?? null;
    }

    /**
     * Send a push notification via FCM HTTP v1 API
     */
    public static function send($fcmToken, $title, $body, $data = []) {
        if (empty($fcmToken)) {
            return false;
        }

        $accessToken = self::getAccessToken();
        if (!$accessToken) {
            error_log('Failed to get FCM access token');
            return false;
        }

        // Add title and body to data payload for custom handling
        $data['title'] = $title;
        $data['body'] = $body;

        $payload = [
            'message' => [
                'token' => $fcmToken,
                'data' => $data,
                'android' => [
                    'priority' => 'high'
                ]
            ]
        ];

        return self::sendRequest($payload, $accessToken);
    }

    /**
     * Send data-only notification (no notification tray, handled by app)
     */
    public static function sendData($fcmToken, $data) {
        if (empty($fcmToken)) {
            return false;
        }

        $accessToken = self::getAccessToken();
        if (!$accessToken) {
            error_log('Failed to get FCM access token');
            return false;
        }

        $payload = [
            'message' => [
                'token' => $fcmToken,
                'data' => $data,
                'android' => [
                    'priority' => 'high'
                ]
            ]
        ];

        return self::sendRequest($payload, $accessToken);
    }

    /**
     * Send notification for new message
     */
    public static function sendMessageNotification($fcmToken, $senderName, $messageText, $senderId, $senderImage = '') {
        $data = [
            'type' => 'new_message',
            'title' => $senderName,
            'body' => $messageText,
            'senderId' => (string)$senderId,
            'senderName' => $senderName,
            'senderImage' => $senderImage
        ];

        return self::sendData($fcmToken, $data);
    }

    /**
     * Send notification for follow request
     */
    public static function sendFollowRequestNotification($fcmToken, $requesterName, $requesterId, $requesterImage = '') {
        $data = [
            'type' => 'follow_request',
            'title' => 'New Follow Request',
            'body' => "$requesterName wants to follow you",
            'senderId' => (string)$requesterId,
            'senderName' => $requesterName,
            'senderImage' => $requesterImage
        ];

        return self::sendData($fcmToken, $data);
    }

    /**
     * Send notification for new follower
     */
    public static function sendNewFollowerNotification($fcmToken, $followerName, $followerId, $followerImage = '') {
        $data = [
            'type' => 'new_follower',
            'title' => 'New Follower',
            'body' => "$followerName started following you",
            'senderId' => (string)$followerId,
            'senderName' => $followerName,
            'senderImage' => $followerImage
        ];

        return self::sendData($fcmToken, $data);
    }

    /**
     * Send notification for screenshot alert
     */
    public static function sendScreenshotAlert($fcmToken, $screenshotterName, $screenshotterId) {
        $data = [
            'type' => 'screenshot_alert',
            'title' => 'Screenshot Alert',
            'body' => "$screenshotterName took a screenshot of your chat",
            'senderId' => (string)$screenshotterId,
            'senderName' => $screenshotterName
        ];

        return self::sendData($fcmToken, $data);
    }

    /**
     * Send notification for like
     */
    public static function sendLikeNotification($fcmToken, $likerName, $likerId, $likerImage = '') {
        $data = [
            'type' => 'like',
            'title' => 'New Like',
            'body' => "$likerName liked your post",
            'senderId' => (string)$likerId,
            'senderName' => $likerName,
            'senderImage' => $likerImage
        ];

        return self::sendData($fcmToken, $data);
    }

    /**
     * Send notification for comment
     */
    public static function sendCommentNotification($fcmToken, $commenterName, $commentText, $commenterId, $commenterImage = '') {
        $data = [
            'type' => 'comment',
            'title' => 'New Comment',
            'body' => "$commenterName commented: $commentText",
            'senderId' => (string)$commenterId,
            'senderName' => $commenterName,
            'senderImage' => $commenterImage
        ];

        return self::sendData($fcmToken, $data);
    }

    /**
     * Send notification for incoming call
     */
    public static function sendCallNotification($fcmToken, $callerName, $callerId, $callerImage, $callId, $callType, $channelName) {
        $data = [
            'type' => 'incoming_call',
            'title' => 'Incoming Call',
            'body' => "$callerName is calling you...",
            'senderId' => (string)$callerId,
            'senderName' => $callerName,
            'senderImage' => $callerImage,
            'callId' => $callId,
            'callType' => $callType,
            'channelName' => $channelName
        ];

        return self::sendData($fcmToken, $data);
    }

    /**
     * Make the actual HTTP request to FCM HTTP v1 API
     */
    private static function sendRequest($payload, $accessToken) {
        $url = str_replace('YOUR_FIREBASE_PROJECT_ID', self::$projectId, self::$fcmUrl);

        $headers = [
            'Authorization: Bearer ' . $accessToken,
            'Content-Type: application/json'
        ];

        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));

        $result = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

        if ($result === false) {
            error_log('FCM Error: ' . curl_error($ch));
            curl_close($ch);
            return false;
        }

        curl_close($ch);

        $response = json_decode($result, true);

        if ($httpCode == 200 && isset($response['name'])) {
            return true;
        } else {
            error_log('FCM Response Error: ' . $result);
            return false;
        }
    }

    /**
     * Set the Firebase project ID
     */
    public static function setProjectId($projectId) {
        self::$projectId = $projectId;
        self::$fcmUrl = "https://fcm.googleapis.com/v1/projects/{$projectId}/messages:send";
    }

    /**
     * Set the service account file path
     */
    public static function setServiceAccountPath($path) {
        self::$serviceAccountPath = $path;
    }
}
?>
