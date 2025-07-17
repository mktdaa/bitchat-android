package com.bitchat.android.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * شاشة شرح الصلاحيات التي تظهر قبل طلب الأذونات
 * تشرح سبب احتياج التطبيق لكل صلاحية وتطمئن المستخدمين حول الخصوصية
 */
@Composable
fun PermissionExplanationScreen(
    permissionCategories: List<PermissionCategory>,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // العنوان الرئيسي
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "مرحبًا بكم في بلو للرسائل",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3) // اللون الأزرق
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "رسائل لاسلكية لامركزية عبر البلوتوث",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.onSurface.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // قسم تأكيد الخصوصية
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔒",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "خصوصيتك محمية",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    )
                }
                
                Text(
                    text = "• بلو للرسائل لا يتتبعك أو يجمع بياناتك الشخصية\n" +
                            "• لا يوجد سيرفرات، لا حاجة للإنترنت، لا تسجيل للبيانات\n" +
                            "• صلاحية الموقع تستخدم فقط من قبل أندرويد لفحص البلوتوث\n" +
                            "• رسائلك تبقى على جهازك وأجهزة الأقران فقط",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "لكي يعمل التطبيق بشكل صحيح، يحتاج إلى هذه الصلاحيات:",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )
        )

        // فئات الصلاحيات
        permissionCategories.forEach { category ->
            PermissionCategoryCard(
                category = category,
                colorScheme = colorScheme
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // أزرار الإجراءات
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3) // اللون الأزرق
                )
            ) {
                Text(
                    text = "منح الصلاحيات",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colorScheme.onSurface.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "خروج من التطبيق",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionCategoryCard(
    category: PermissionCategory,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = getPermissionEmoji(category.name),
                    style = MaterialTheme.typography.titleLarge,
                    color = getPermissionIconColor(category.name),
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = when(category.name) {
                        "Nearby Devices" -> "الأجهزة القريبة"
                        "Precise Location" -> "الموقع الدقيق"
                        "Notifications" -> "الإشعارات"
                        else -> category.name
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                )
            }
            
            Text(
                text = when(category.name) {
                    "Nearby Devices" -> "للتواصل مع الأجهزة القريبة عبر البلوتوث بدون إنترنت"
                    "Precise Location" -> "مطلوبة من أندرويد لاكتشاف الأجهزة القريبة عبر البلوتوث"
                    "Notifications" -> "لإعلامك عند وصول رسائل جديدة"
                    else -> category.description
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            )

            if (category.name == "Precise Location") {
                // تأكيد إضافي حول صلاحية الموقع
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "بلو للرسائل لا يستخدم GPS ولا يتتبع موقعك",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF9800)
                        )
                    )
                }
            }
        }
    }
}

private fun getPermissionEmoji(categoryName: String): String {
    return when (categoryName) {
        "Nearby Devices" -> "📱"
        "Precise Location" -> "📍"
        "Notifications" -> "🔔"
        else -> "🔧"
    }
}

private fun getPermissionIconColor(categoryName: String): Color {
    return when (categoryName) {
        "Nearby Devices" -> Color(0xFF2196F3) // أزرق
        "Precise Location" -> Color(0xFFFF9800) // برتقالي
        "Notifications" -> Color(0xFF4CAF50) // أخضر
        else -> Color(0xFF9C27B0) // بنفسجي
    }
}
