package com.devs.i210396_i211384.utils

object AgoraConfig {
    // Agora App ID - Successfully configured!
    const val APP_ID = "fc45bacc392b45c58b8c0b3fc4e8b5e3"

    // Agora App Certificate (Primary Certificate)
    // Enabled for token authentication
    const val APP_CERTIFICATE = "0708667746bd4b8eb95ad1105e4b56fe"

    // Callback Secret
    const val CALLBACK_SECRET = "09999ad42b32a41002db5a46c39d025b"

    // Server URL
    const val SERVER_URL = "wss://webliveroom460418059-api.coolzcloud.com/ws"

    // Channel name will be generated dynamically for each call
    fun generateChannelName(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "call_${userId1}_${userId2}"
        } else {
            "call_${userId2}_${userId1}"
        }
    }
}
