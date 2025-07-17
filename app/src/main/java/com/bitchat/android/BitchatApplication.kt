package com.bitchat.android

import android.app.Application
import android.content.Context
import com.bitchat.android.util.LocaleHelper

/**
 * Main application class for bitchat Android
 */
class BitchatApplication : Application() {

    override fun attachBaseContext(base: Context) {
        // فرض اللغة العربية دائمًا
        super.attachBaseContext(LocaleHelper.setLocale(base))
    }

    override fun onCreate() {
        super.onCreate()

        // أي تهيئة عامة
    }
}
