package com.bitchat.android.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * إدارة مركزية لصلاحيات تطبيق بلو للرسائل
 * يتعامل مع جميع صلاحيات البلوتوث والإشعارات المطلوبة لعمل التطبيق
 */
class PermissionManager(private val context: Context) {

    companion object {
        private const val TAG = "PermissionManager"
        private const val PREFS_NAME = "bloo_permissions"
        private const val KEY_FIRST_TIME_COMPLETE = "first_time_onboarding_complete"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * التحقق مما إذا كانت هذه أول مرة يتم فيها تشغيل التطبيق
     */
    fun isFirstTimeLaunch(): Boolean {
        return !sharedPrefs.getBoolean(KEY_FIRST_TIME_COMPLETE, false)
    }

    /**
     * وضع علامة على اكتمال عملية الإعداد الأولي
     */
    fun markOnboardingComplete() {
        sharedPrefs.edit()
            .putBoolean(KEY_FIRST_TIME_COMPLETE, true)
            .apply()
        Log.d(TAG, "تم وضع علامة على اكتمال الإعداد الأولي")
    }

    /**
     * الحصول على جميع الصلاحيات المطلوبة للتطبيق
     */
    fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        // صلاحيات البلوتوث (تعتمد على مستوى واجهة برمجة التطبيقات)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.addAll(listOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            ))
        } else {
            permissions.addAll(listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            ))
        }

        // صلاحيات الموقع (مطلوبة لفحص البلوتوث منخفض الطاقة)
        permissions.addAll(listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))

        // صلاحية الإشعارات (أندرويد 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions
    }

    /**
     * التحقق مما إذا كانت صلاحية معينة ممنوحة
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * التحقق مما إذا كانت جميع الصلاحيات المطلوبة ممنوحة
     */
    fun areAllPermissionsGranted(): Boolean {
        return getRequiredPermissions().all { isPermissionGranted(it) }
    }

    /**
     * الحصول على قائمة الصلاحيات المفقودة
     */
    fun getMissingPermissions(): List<String> {
        return getRequiredPermissions().filter { !isPermissionGranted(it) }
    }

    /**
     * الحصول على معلومات الصلاحيات المصنفة للعرض
     */
    fun getCategorizedPermissions(): List<PermissionCategory> {
        val categories = mutableListOf<PermissionCategory>()

        // فئة البلوتوث/الأجهزة القريبة
        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        categories.add(
            PermissionCategory(
                name = "الأجهزة القريبة",
                description = "مطلوبة لاكتشاف والتواصل مع مستخدمي بلو للرسائل عبر البلوتوث",
                permissions = bluetoothPermissions,
                isGranted = bluetoothPermissions.all { isPermissionGranted(it) },
                systemDescription = "السماح لبلو للرسائل بالاتصال بالأجهزة القريبة"
            )
        )

        // فئة الموقع
        val locationPermissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        categories.add(
            PermissionCategory(
                name = "الموقع الدقيق",
                description = "مطلوب بواسطة أندرويد لفحص البلوتوث",
                permissions = locationPermissions,
                isGranted = locationPermissions.all { isPermissionGranted(it) },
                systemDescription = "السماح لبلو للرسائل بالوصول إلى موقع الجهاز"
            )
        )

        // فئة الإشعارات (إذا كانت مطبقة)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            categories.add(
                PermissionCategory(
                    name = "الإشعارات",
                    description = "عرض الإشعارات عند استلام رسائل خاصة أثناء وجود التطبيق في الخلفية",
                    permissions = listOf(Manifest.permission.POST_NOTIFICATIONS),
                    isGranted = isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS),
                    systemDescription = "السماح لبلو للرسائل بإرسال الإشعارات لك"
                )
            )
        }

        return categories
    }

    /**
     * الحصول على معلومات تشخيصية مفصلة حول حالة الصلاحيات
     */
    fun getPermissionDiagnostics(): String {
        return buildString {
            appendLine("تشخيص الصلاحيات:")
            appendLine("إصدار أندرويد: ${Build.VERSION.SDK_INT}")
            appendLine("أول تشغيل: ${isFirstTimeLaunch()}")
            appendLine("جميع الصلاحيات ممنوحة: ${areAllPermissionsGranted()}")
            appendLine()
            
            getCategorizedPermissions().forEach { category ->
                appendLine("${category.name}: ${if (category.isGranted) "✅ ممنوحة" else "❌ مفقودة"}")
                category.permissions.forEach { permission ->
                    val granted = isPermissionGranted(permission)
                    appendLine("  - ${permission.substringAfterLast(".")}: ${if (granted) "✅" else "❌"}")
                }
                appendLine()
            }
            
            val missing = getMissingPermissions()
            if (missing.isNotEmpty()) {
                appendLine("الصلاحيات المفقودة:")
                missing.forEach { permission ->
                    appendLine("- $permission")
                }
            }
        }
    }

    /**
     * تسجيل حالة الصلاحيات لأغراض التصحيح
     */
    fun logPermissionStatus() {
        Log.d(TAG, getPermissionDiagnostics())
    }
}

/**
 * فئة بيانات تمثل فئة من الصلاحيات ذات الصلة
 */
data class PermissionCategory(
    val name: String,
    val description: String,
    val permissions: List<String>,
    val isGranted: Boolean,
    val systemDescription: String
)
