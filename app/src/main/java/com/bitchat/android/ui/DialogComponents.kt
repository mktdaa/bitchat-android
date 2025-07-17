package com.bitchat.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Dialog components for ChatScreen
 * Extracted from ChatScreen.kt for better organization
 */

@Composable
fun PasswordPromptDialog(
    show: Boolean,
    channelName: String?,
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (show && channelName != null) {
        val colorScheme = MaterialTheme.colorScheme
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "ادخل كلمة سر القناة",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Channel $channelName is password protected. ادخل كلمة السر للانضمام.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = onPasswordChange,
                        label = { Text("كلمة السر", style = MaterialTheme.typography.bodyMedium) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outline
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = "انضمام",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "الغاء",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface
                    )
                }
            },
            containerColor = colorScheme.surface,
            tonalElevation = 8.dp
        )
    }
}

@Composable
fun AppInfoDialog(
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (show) {
        val colorScheme = MaterialTheme.colorScheme
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "بلو للرسائل",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "رسائل لاسلكية لا مركزية عبر بلوتوث منخفض الطاقة\n\n" +
                            "• لا حاجة لخوادم أو اتصال بالإنترنت\n" +
                            "• رسائل خاصة مشفرة من الطرف للطرف\n" +
                            "• قنوات محمية بكلمة مرور\n" +
                            "• إمكانية التخزين وإعادة الإرسال للأجهزة غير المتصلة\n\n" +
                            "اضغط ثلاث مرات على العنوان لمسح جميع البيانات في حالات الطوارئ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "موافق",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.primary
                    )
                }
            },
            containerColor = colorScheme.surface,
            tonalElevation = 8.dp
        )
    }
}
