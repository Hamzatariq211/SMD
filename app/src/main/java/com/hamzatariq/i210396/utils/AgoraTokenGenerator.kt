package com.hamzatariq.i210396.utils

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

    /**
     * Generate Agora RTC token
     * This is a simplified version - in production, use a backend server
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
                return ""
            }

            val privilegeExpired = if (privilegeExpiredTs == 0) {
                (System.currentTimeMillis() / 1000 + 3600).toInt() // 1 hour
            } else {
                privilegeExpiredTs
            }

            Log.d(TAG, "Generating token for channel: $channelName, uid: $uid")

            val token = buildTokenWithUid(appId, appCertificate, channelName, uid, privilegeExpired)

            Log.d(TAG, "Token generated successfully, length: ${token.length}")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate token: ${e.message}", e)
            ""
        }
    }

    fun getExpirationTimestamp(expirationTimeInSeconds: Int = 3600): Int {
        return (System.currentTimeMillis() / 1000 + expirationTimeInSeconds).toInt()
    }

    private fun buildTokenWithUid(
        appId: String,
        appCertificate: String,
        channelName: String,
        uid: Int,
        privilegeExpired: Int
    ): String {
        return AccessToken(appId, appCertificate, channelName, uid.toString()).apply {
            addPrivilege(AccessToken.Privileges.kJoinChannel, privilegeExpired)
            addPrivilege(AccessToken.Privileges.kPublishAudioStream, privilegeExpired)
            addPrivilege(AccessToken.Privileges.kPublishVideoStream, privilegeExpired)
            addPrivilege(AccessToken.Privileges.kPublishDataStream, privilegeExpired)
        }.build()
    }

    // AccessToken implementation based on Agora's official algorithm
    private class AccessToken(
        private val appId: String,
        private val appCertificate: String,
        private val channelName: String,
        private val uid: String
    ) {
        private val salt = (Math.random() * 99999999).toInt()
        private val ts = (System.currentTimeMillis() / 1000).toInt()
        private val messages = TreeMap<Short, Int>()

        object Privileges {
            const val kJoinChannel: Short = 1
            const val kPublishAudioStream: Short = 2
            const val kPublishVideoStream: Short = 3
            const val kPublishDataStream: Short = 4
        }

        fun addPrivilege(privilege: Short, expireTimestamp: Int) {
            messages[privilege] = expireTimestamp
        }

        fun build(): String {
            val msg = pack()
            val signature = hmacSign(appCertificate, msg)
            val crcChannelName = crc32(channelName.toByteArray())
            val crcUid = crc32(uid.toByteArray())

            val content = packContent(signature, crcChannelName, crcUid, msg)
            return "007" + Base64.encodeToString(content, Base64.NO_WRAP)
        }

        private fun pack(): ByteArray {
            val buffer = ByteBuffer.allocate(1024)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            buffer.putInt(salt)
            buffer.putInt(ts)
            buffer.putInt(messages.size)

            messages.forEach { (key, value) ->
                buffer.putShort(key)
                buffer.putInt(value)
            }

            val messageBytes = ByteArray(buffer.position())
            buffer.flip()
            buffer.get(messageBytes)

            return messageBytes
        }

        private fun packContent(
            signature: ByteArray,
            crcChannelName: Int,
            crcUid: Int,
            message: ByteArray
        ): ByteArray {
            val buffer = ByteBuffer.allocate(1024)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            // Pack signature
            buffer.putShort(signature.size.toShort())
            buffer.put(signature)

            // Pack crc channel name
            buffer.putInt(crcChannelName)

            // Pack crc uid
            buffer.putInt(crcUid)

            // Pack message
            buffer.putShort(message.size.toShort())
            buffer.put(message)

            val content = ByteArray(buffer.position())
            buffer.flip()
            buffer.get(content)

            return content
        }

        private fun hmacSign(key: String, message: ByteArray): ByteArray {
            val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(keySpec)
            return mac.doFinal(message)
        }

        private fun crc32(data: ByteArray): Int {
            val crc = CRC32()
            crc.update(data)
            return crc.value.toInt()
        }
    }
}
