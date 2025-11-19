package com.devs.i210396_i211384.utils

import android.util.Base64
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.TreeMap
import java.util.zip.CRC32

object AgoraTokenGenerator {

    private const val TAG = "AgoraTokenGenerator"
    private const val VERSION = "007"

    /**
     * Generate Agora RTC token
     * Based on Agora's official token generation algorithm
     */
    fun generateToken(
        channelName: String,
        uid: Int,
        role: Int = 1,
        privilegeExpiredTs: Int = 0
    ): String {
        return try {
            val appId = AgoraConfig.APP_ID
            val appCertificate = AgoraConfig.APP_CERTIFICATE

            if (appId.isEmpty() || appCertificate.isEmpty()) {
                Log.e(TAG, "App ID or Certificate is empty")
                Log.e(TAG, "APP_ID: $appId")
                Log.e(TAG, "APP_CERTIFICATE length: ${appCertificate.length}")
                return ""
            }

            // Calculate token expiration time (default 24 hours from now)
            val tokenExpirationTs = if (privilegeExpiredTs == 0) {
                (System.currentTimeMillis() / 1000 + 86400).toInt() // 24 hours
            } else {
                privilegeExpiredTs
            }

            Log.d(TAG, "=== Token Generation Details ===")
            Log.d(TAG, "Channel: $channelName")
            Log.d(TAG, "UID: $uid")
            Log.d(TAG, "Token Expiration: $tokenExpirationTs")
            Log.d(TAG, "Current Time: ${System.currentTimeMillis() / 1000}")

            val token = buildToken(appId, appCertificate, channelName, uid, tokenExpirationTs)

            Log.d(TAG, "Token generated successfully")
            Log.d(TAG, "Token length: ${token.length}")
            Log.d(TAG, "Token: $token")

            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate token: ${e.message}", e)
            e.printStackTrace()
            ""
        }
    }

    private fun buildToken(
        appId: String,
        appCertificate: String,
        channelName: String,
        uid: Int,
        privilegeExpiredTs: Int
    ): String {
        val uidStr = uid.toString()
        val salt = (Math.random() * 99999999).toInt()
        val ts = (System.currentTimeMillis() / 1000).toInt()

        // Build privileges map
        val messages = TreeMap<Short, Int>()
        messages[1] = privilegeExpiredTs // kJoinChannel
        messages[2] = privilegeExpiredTs // kPublishAudioStream
        messages[3] = privilegeExpiredTs // kPublishVideoStream
        messages[4] = privilegeExpiredTs // kPublishDataStream

        // Pack message
        val messageBuffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)
        messageBuffer.putInt(salt)
        messageBuffer.putInt(ts)
        messageBuffer.putInt(messages.size)

        for ((privilege, expireTime) in messages) {
            messageBuffer.putShort(privilege)
            messageBuffer.putInt(expireTime)
        }

        val messageBytes = ByteArray(messageBuffer.position())
        messageBuffer.flip()
        messageBuffer.get(messageBytes)

        // Generate signature
        val signature = hmacSha256(appCertificate.toByteArray(Charsets.UTF_8), messageBytes)

        // Calculate CRC32
        val crcChannelName = crc32(channelName.toByteArray())
        val crcUid = crc32(uidStr.toByteArray())

        // Pack content
        val contentBuffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN)

        // Signature
        contentBuffer.putShort(signature.size.toShort())
        contentBuffer.put(signature)

        // CRC channel name
        contentBuffer.putInt(crcChannelName)

        // CRC UID
        contentBuffer.putInt(crcUid)

        // Message
        contentBuffer.putShort(messageBytes.size.toShort())
        contentBuffer.put(messageBytes)

        val contentBytes = ByteArray(contentBuffer.position())
        contentBuffer.flip()
        contentBuffer.get(contentBytes)

        // Encode and build final token
        val base64Content = Base64.encodeToString(contentBytes, Base64.NO_WRAP)
        return VERSION + appId + base64Content
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKeySpec)
        return mac.doFinal(data)
    }

    private fun crc32(data: ByteArray): Int {
        val crc = CRC32()
        crc.update(data)
        return crc.value.toInt()
    }

    fun getExpirationTimestamp(expirationTimeInSeconds: Int = 3600): Int {
        return (System.currentTimeMillis() / 1000 + expirationTimeInSeconds).toInt()
    }
}
