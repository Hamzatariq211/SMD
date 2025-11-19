<?php
require_once __DIR__ . '/../config/config.php';

class JWT {

    public static function encode($payload) {
        $header = json_encode(['typ' => 'JWT', 'alg' => 'HS256']);
        $header = self::base64UrlEncode($header);

        $payload['exp'] = time() + JWT_EXPIRATION;
        $payload = json_encode($payload);
        $payload = self::base64UrlEncode($payload);

        $signature = hash_hmac('sha256', "$header.$payload", JWT_SECRET, true);
        $signature = self::base64UrlEncode($signature);

        return "$header.$payload.$signature";
    }

    public static function decode($token) {
        $parts = explode('.', $token);

        if (count($parts) !== 3) {
            return null;
        }

        list($header, $payload, $signature) = $parts;

        $validSignature = hash_hmac('sha256', "$header.$payload", JWT_SECRET, true);
        $validSignature = self::base64UrlEncode($validSignature);

        if ($signature !== $validSignature) {
            return null;
        }

        $payload = json_decode(self::base64UrlDecode($payload), true);

        if (isset($payload['exp']) && $payload['exp'] < time()) {
            return null;
        }

        return $payload;
    }

    public static function getUserIdFromToken() {
        // More robust header retrieval for different server configurations
        $headers = self::getAllHeaders();
        $authHeader = isset($headers['Authorization']) ? $headers['Authorization'] :
                     (isset($headers['authorization']) ? $headers['authorization'] : null);

        if (!$authHeader || !preg_match('/Bearer\s+(.*)$/i', $authHeader, $matches)) {
            return null;
        }

        $token = $matches[1];
        $payload = self::decode($token);

        return $payload ? $payload['userId'] : null;
    }

    /**
     * Get all HTTP headers in a way that works across different server configurations
     */
    private static function getAllHeaders() {
        if (function_exists('getallheaders')) {
            return getallheaders();
        }

        // Fallback for servers where getallheaders() doesn't exist
        $headers = [];
        foreach ($_SERVER as $name => $value) {
            if (substr($name, 0, 5) == 'HTTP_') {
                $headerName = str_replace(' ', '-', ucwords(strtolower(str_replace('_', ' ', substr($name, 5)))));
                $headers[$headerName] = $value;
            }
        }
        return $headers;
    }

    private static function base64UrlEncode($data) {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    private static function base64UrlDecode($data) {
        return base64_decode(strtr($data, '-_', '+/'));
    }
}
?>
