package com.devs.i210396_i211384

import android.app.Application
import android.content.Intent
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.services.OfflineSyncService

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)

        // Start offline sync service
        startService(Intent(this, OfflineSyncService::class.java))
    }
}
