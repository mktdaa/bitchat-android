package com.bitchat.android.onboarding

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * يدير حالة تفعيل/تعطيل البلوتوث وطلبات المستخدم
 * يتحقق من حالة البلوتوث عند كل تشغيل للتطبيق
 */
class BluetoothStatusManager(
    private val activity: ComponentActivity,
    private val context: Context,
    private val onBluetoothEnabled: () -> Unit,
    private val onBluetoothDisabled: (String) -> Unit
) {

    companion object {
        private const val TAG = "BluetoothStatusManager"
    }

    private var bluetoothEnableLauncher: ActivityResultLauncher<Intent>? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    init {
        setupBluetoothAdapter()
        setupBluetoothEnableLauncher()
    }

    /**
     * إعداد محول البلوتوث
     */
    private fun setupBluetoothAdapter() {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
            Log.d(TAG, "تم تهيئة محول البلوتوث: ${bluetoothAdapter != null}")
        } catch (e: Exception) {
            Log.e(TAG, "فشل في تهيئة محول البلوتوث", e)
            bluetoothAdapter = null
        }
    }

    /**
     * إعداد النافذة لطلب تفعيل البلوتوث
     */
    private fun setupBluetoothEnableLauncher() {
        bluetoothEnableLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val isEnabled = bluetoothAdapter?.isEnabled == true
            Log.d(TAG, "نتيجة طلب تفعيل البلوتوث: $isEnabled (رمز النتيجة: ${result.resultCode})")
            if (isEnabled) {
                onBluetoothEnabled()
            } else {
                onBluetoothDisabled("البلوتوث مطلوب لبرنامج بلو للرسائل لاكتشاف والتواصل مع المستخدمين القريبين. يرجى تفعيل البلوتوث للمتابعة.")
            }
        }
    }

    /**
     * التحقق مما إذا كان البلوتوث مدعومًا على هذا الجهاز
     */
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    /**
     * التحقق مما إذا كان البلوتوث مفعلاً حالياً (مع مراعاة الصلاحيات)
     */
    fun isBluetoothEnabled(): Boolean {
        return try {
            bluetoothAdapter?.isEnabled == true
        } catch (securityException: SecurityException) {
            // إذا لم نتمكن من التحقق بسبب الصلاحيات، نفترض أنه معطل
            Log.w(TAG, "لا يمكن التحقق من حالة البلوتوث بسبب نقص الصلاحيات")
            false
        } catch (e: Exception) {
            Log.w(TAG, "خطأ في التحقق من حالة البلوتوث: ${e.message}")
            false
        }
    }

    /**
     * التحقق من حالة البلوتوث والتعامل معها (مع مراعاة الصلاحيات)
     * يجب استدعاء هذه الوظيفة عند كل تشغيل للتطبيق
     */
    fun checkBluetoothStatus(): BluetoothStatus {
        Log.d(TAG, "جارٍ التحقق من حالة البلوتوث")
        
        return when {
            bluetoothAdapter == null -> {
                Log.e(TAG, "البلوتوث غير مدعوم على هذا الجهاز")
                BluetoothStatus.NOT_SUPPORTED
            }
            !isBluetoothEnabled() -> {
                Log.w(TAG, "البلوتوث معطل أو لا يمكن التحقق منه")
                BluetoothStatus.DISABLED
            }
            else -> {
                Log.d(TAG, "البلوتوث مفعّل وجاهز")
                BluetoothStatus.ENABLED
            }
        }
    }

    /**
     * طلب تفعيل البلوتوث من المستخدم (مع مراعاة الصلاحيات)
     */
    fun requestEnableBluetooth() {
        Log.d(TAG, "طلب تفعيل البلوتوث من المستخدم")
        
        try {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher?.launch(enableBluetoothIntent)
        } catch (securityException: SecurityException) {
            // الصلاحية غير ممنوحة بعد - هذا متوقع أثناء عملية الإعداد الأولي
            Log.w(TAG, "لا يمكن طلب تفعيل البلوتوث بسبب نقص صلاحية BLUETOOTH_CONNECT")
            onBluetoothDisabled("صلاحيات البلوتوث مطلوبة قبل التفعيل. يرجى منح الصلاحيات أولاً.")
        } catch (e: Exception) {
            Log.e(TAG, "فشل في طلب تفعيل البلوتوث", e)
            onBluetoothDisabled("فشل في طلب تفعيل البلوتوث: ${e.message}")
        }
    }

    /**
     * التعامل مع نتيجة التحقق من حالة البلوتوث
     */
    fun handleBluetoothStatus(status: BluetoothStatus) {
        when (status) {
            BluetoothStatus.ENABLED -> {
                Log.d(TAG, "البلوتوث مفعّل، جارٍ المتابعة")
                onBluetoothEnabled()
            }
            BluetoothStatus.DISABLED -> {
                Log.d(TAG, "البلوتوث معطل، جارٍ طلب التفعيل")
                requestEnableBluetooth()
            }
            BluetoothStatus.NOT_SUPPORTED -> {
                Log.e(TAG, "البلوتوث غير مدعوم")
                onBluetoothDisabled("هذا الجهاز لا يدعم البلوتوث، وهو مطلوب لعمل برنامج بلو للرسائل.")
            }
        }
    }

    /**
     * الحصول على رسالة حالة سهلة الفهم للمستخدم
     */
    fun getStatusMessage(status: BluetoothStatus): String {
        return when (status) {
            BluetoothStatus.ENABLED -> "البلوتوث مفعّل وجاهز للاستخدام"
            BluetoothStatus.DISABLED -> "البلوتوث معطل. يرجى تفعيل البلوتوث لاستخدام بلو للرسائل."
            BluetoothStatus.NOT_SUPPORTED -> "هذا الجهاز لا يدعم البلوتوث."
        }
    }

    /**
     * الحصول على تشخيص مفصل (مع مراعاة الصلاحيات)
     */
    fun getDiagnostics(): String {
        return buildString {
            appendLine("تشخيص حالة البلوتوث:")
            appendLine("المحول متاح: ${bluetoothAdapter != null}")
            appendLine("البلوتوث مدعوم: ${isBluetoothSupported()}")
            appendLine("البلوتوث مفعّل: ${isBluetoothEnabled()}")
            appendLine("الحالة الحالية: ${checkBluetoothStatus()}")
            
            // الوصول إلى تفاصيل المحول فقط إذا كانت الصلاحية متاحة والمحول متاح
            bluetoothAdapter?.let { adapter ->
                try {
                    // هذه الاستدعاءات تتطلب صلاحية BLUETOOTH_CONNECT على أندرويد 12+
                    appendLine("اسم المحول: ${adapter.name ?: "غير معروف"}")
                    appendLine("عنوان المحول: ${adapter.address ?: "غير معروف"}")
                } catch (securityException: SecurityException) {
                    // الصلاحية غير ممنوحة بعد، تخطي المعلومات التفصيلية
                    appendLine("تفاصيل المحول: [صلاحية مطلوبة]")
                } catch (e: Exception) {
                    appendLine("تفاصيل المحول: [خطأ: ${e.message}]")
                }
                appendLine("حالة المحول: ${getAdapterStateName(adapter.state)}")
            }
        }
    }

    private fun getAdapterStateName(state: Int): String {
        return when (state) {
            BluetoothAdapter.STATE_OFF -> "معطل"
            BluetoothAdapter.STATE_TURNING_ON -> "جارٍ التفعيل"
            BluetoothAdapter.STATE_ON -> "مفعّل"
            BluetoothAdapter.STATE_TURNING_OFF -> "جارٍ التعطيل"
            else -> "غير معروف($state)"
        }
    }

    /**
     * تسجيل حالة البلوتوث الحالية لأغراض التصحيح
     */
    fun logBluetoothStatus() {
        Log.d(TAG, getDiagnostics())
    }
}

/**
 * تعداد لحالة البلوتوث
 */
enum class BluetoothStatus {
    ENABLED,  // مفعّل
    DISABLED,  // معطل
    NOT_SUPPORTED  // غير مدعوم
}
