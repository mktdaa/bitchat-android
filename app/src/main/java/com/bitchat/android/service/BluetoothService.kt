package com.bitchat.android.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.bluetooth.BluetoothAdapter
import com.bitchat.android.R

class BluetoothService : Service() {

    override fun onCreate() {
        super.onCreate()
        startBluetoothBroadcasting()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "bitchat_channel")
            .setContentTitle("BitChat يعمل بالخلفية")
            .setContentText("يتم بث اسم المستخدم الآن")
            .setSmallIcon(R.drawable.ic_notification) // تأكد من وجود هذا الأيقون
            .build()

        startForeground(1, notification)

        return START_STICKY
    }

    private fun startBluetoothBroadcasting() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            bluetoothAdapter.name = getUserName()
        }
    }

    private fun getUserName(): String {
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        return sharedPreferences.getString("username", "BitChatUser") ?: "BitChatUser"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "bitchat_channel",
                "BitChat Foreground",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
