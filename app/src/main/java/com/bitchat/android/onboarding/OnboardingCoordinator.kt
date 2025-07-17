package com.bitchat.android.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * ينسق عملية الإعداد الأولي الكاملة بما في ذلك شرح الصلاحيات،
 * طلبات الصلاحيات، وتهيئة خدمة الشبكة اللاسلكية
 */
class OnboardingCoordinator(
    private val activity: ComponentActivity,
    private val permissionManager: PermissionManager,
    private val onOnboardingComplete: () -> Unit,
    private val onOnboardingFailed: (String) -> Unit
) {

    companion object {
        private const val TAG = "OnboardingCoordinator"
    }

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    init {
        setupPermissionLauncher()
    }

    /**
     * إعداد نافذة طلب الصلاحيات
     */
    private fun setupPermissionLauncher() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResults(permissions)
        }
    }

    /**
     * بدء عملية الإعداد الأولي
     */
    fun startOnboarding() {
        Log.d(TAG, "بدء عملية الإعداد الأولي")
        permissionManager.logPermissionStatus()

        if (permissionManager.areAllPermissionsGranted()) {
            Log.d(TAG, "تم منح جميع الصلاحيات بالفعل، إكمال الإعداد الأولي")
            completeOnboarding()
        } else {
            Log.d(TAG, "هناك صلاحيات مفقودة، يجب بدء تدفق الشرح")
            // سيتم عرض شاشة الشرح بواسطة النشاط الذي يستدعي
        }
    }

    /**
     * يتم استدعاؤه عندما يقبل المستخدم شرح الصلاحيات
     */
    fun requestPermissions() {
        Log.d(TAG, "قبل المستخدم شرح الصلاحيات، طلب الصلاحيات")
        
        val missingPermissions = permissionManager.getMissingPermissions()
        if (missingPermissions.isEmpty()) {
            completeOnboarding()
            return
        }

        Log.d(TAG, "طلب ${missingPermissions.size} صلاحيات")
        permissionLauncher?.launch(missingPermissions.toTypedArray())
    }

    /**
     * معالجة نتائج طلب الصلاحيات
     */
    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        Log.d(TAG, "تم استلام نتائج الصلاحيات:")
        permissions.forEach { (permission, granted) ->
            Log.d(TAG, "  $permission: ${if (granted) "ممنوح" else "مرفوض"}")
        }

        val allGranted = permissions.values.all { it }
        val criticalPermissions = getCriticalPermissions()
        val criticalGranted = criticalPermissions.all { permissions[it] == true }

        when {
            allGranted -> {
                Log.d(TAG, "تم منح جميع الصلاحيات بنجاح")
                completeOnboarding()
            }
            criticalGranted -> {
                Log.d(TAG, "تم منح الصلاحيات الحرجة، يمكن المتابعة بوظائف محدودة")
                showPartialPermissionWarning(permissions)
            }
            else -> {
                Log.d(TAG, "تم رفض الصلاحيات الحرجة")
                handlePermissionDenial(permissions)
            }
        }
    }

    /**
     * الحصول على قائمة الصلاحيات الحرجة المطلوبة تمامًا
     */
    private fun getCriticalPermissions(): List<String> {
        // بالنسبة لـ بلو للرسائل، صلاحيات البلوتوث والموقع حرجة
        // صلاحيات الإشعارات جيدة ولكنها ليست حرجة
        return permissionManager.getRequiredPermissions().filter { permission ->
            !permission.contains("POST_NOTIFICATIONS")
        }
    }

    /**
     * عرض تحذير عند منح بعض الصلاحيات ورفض البعض الآخر
     */
    private fun showPartialPermissionWarning(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filter { !it.value }.keys
        val message = buildString {
            append("تم رفض بعض الصلاحيات:\n")
            deniedPermissions.forEach { permission ->
                append("- ${getPermissionDisplayName(permission)}\n")
            }
            append("\nقد لا يعمل بلو للرسائل بشكل صحيح بدون جميع الصلاحيات.")
        }
        
        Log.w(TAG, "تم منح صلاحيات جزئية: $message")
        
        // للمتابعة رغم ذلك وترك المستخدم يواجه القيود
        completeOnboarding()
    }

    /**
     * معالجة سيناريوهات رفض الصلاحيات
     */
    private fun handlePermissionDenial(permissions: Map<String, Boolean>) {
        val deniedCritical = permissions.filter { !it.value && getCriticalPermissions().contains(it.key) }
        
        if (deniedCritical.isNotEmpty()) {
            val message = buildString {
                append("تم رفض الصلاحيات الحرجة. بلو للرسائل يحتاج هذه الصلاحيات للعمل:\n")
                deniedCritical.keys.forEach { permission ->
                    append("- ${getPermissionDisplayName(permission)}\n")
                }
                append("\nالرجاء منح هذه الصلاحيات في الإعدادات لاستخدام بلو للرسائل.")
            }
            
            Log.e(TAG, "تم رفض الصلاحيات الحرجة: $deniedCritical")
            onOnboardingFailed(message)
        } else {
            // يجب ألا يحدث بالنظر لمنطقنا أعلاه، ولكن التعامل معه بأمان
            completeOnboarding()
        }
    }

    /**
     * إكمال عملية الإعداد الأولي وتهيئة التطبيق
     */
    private fun completeOnboarding() {
        Log.d(TAG, "إكمال عملية الإعداد الأولي")
        
        // وضع علامة على اكتمال الإعداد الأولي
        permissionManager.markOnboardingComplete()
        
        // تسجيل حالة الصلاحيات النهائية
        permissionManager.logPermissionStatus()
        
        // إعلام بالاكتمال مع تأخير بسيط لضمان جاهزية كل شيء
        activity.lifecycleScope.launch {
            kotlinx.coroutines.delay(100) // تأخير بسيط لتسوية حالة واجهة المستخدم
            onOnboardingComplete()
        }
    }

    /**
     * فتح إعدادات التطبيق للإدارة اليدوية للصلاحيات
     */
    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", activity.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
            Log.d(TAG, "فتح إعدادات التطبيق للإدارة اليدوية للصلاحيات")
        } catch (e: Exception) {
            Log.e(TAG, "فشل في فتح إعدادات التطبيق", e)
        }
    }

    /**
     * تحويل نص الصلاحية إلى اسم سهل للمستخدم
     */
    private fun getPermissionDisplayName(permission: String): String {
        return when {
            permission.contains("BLUETOOTH") -> "البلوتوث/الأجهزة القريبة"
            permission.contains("LOCATION") -> "الموقع (للبحث عن البلوتوث)"
            permission.contains("NOTIFICATION") -> "الإشعارات"
            else -> permission.substringAfterLast(".")
        }
    }

    /**
     * الحصول على معلومات تشخيصية لاستكشاف الأخطاء
     */
    fun getDiagnostics(): String {
        return buildString {
            appendLine("تشخيص منسق الإعداد الأولي:")
            appendLine("النشاط: ${activity::class.simpleName}")
            appendLine("نافذة طلب الصلاحيات: ${permissionLauncher != null}")
            appendLine()
            append(permissionManager.getPermissionDiagnostics())
        }
    }
}
