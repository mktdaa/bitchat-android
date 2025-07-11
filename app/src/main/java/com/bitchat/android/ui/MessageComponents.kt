package com.bitchat.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.model.DeliveryStatus
import com.bitchat.android.mesh.BluetoothMeshService
import java.text.SimpleDateFormat
import java.util.*

/**
 * Message display components for ChatScreen
 * Extracted from ChatScreen.kt for better organization
 */

@Composable
fun MessagesList(
    messages: List<BitchatMessage>,
    currentUserNickname: String,
    meshService: BluetoothMeshService,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageItem(
                message = message,
                currentUserNickname = currentUserNickname,
                meshService = meshService
            )
        }
    }
}

@Composable
fun MessageItem(
    message: BitchatMessage,
    currentUserNickname: String,
    meshService: BluetoothMeshService
) {
    val colorScheme = MaterialTheme.colorScheme
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Message content with proper formatting
        Text(
            text = formatMessageAsAnnotatedString(
                message = message,
                currentUserNickname = currentUserNickname,
                meshService = meshService,
                colorScheme = colorScheme,
                timeFormatter = timeFormatter
            ),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            fontFamily = FontFamily.Monospace,
            softWrap = true,
            overflow = TextOverflow.Visible
        )
        
        // Message actions (copy button and delivery status)
        MessageActions(
            message = message,
            currentUserNickname = currentUserNickname,
            onCopyClick = {
                copyMessageToClipboard(
                    context = context,
                    messageContent = message.content
                )
            }
        )
    }
}

@Composable
private fun MessageActions(
    message: BitchatMessage,
    currentUserNickname: String,
    onCopyClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Copy button is always visible
        CopyButton(onClick = onCopyClick)
        
        // Delivery status only for private messages sent by current user
        if (shouldShowDeliveryStatus(message, currentUserNickname)) {
            Spacer(modifier = Modifier.width(4.dp))
            message.deliveryStatus?.let { status ->
                DeliveryStatusIcon(status = status)
            }
        }
    }
}

private fun shouldShowDeliveryStatus(
    message: BitchatMessage,
    currentUserNickname: String
): Boolean {
    return message.isPrivate && message.sender == currentUserNickname
}

private fun copyMessageToClipboard(
    context: Context,
    messageContent: String
) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("message", messageContent)
        clipboard.setPrimaryClip(clip)
        showToast(context, "Message copied to clipboard")
    } catch (e: Exception) {
        showToast(context, "Failed to copy message")
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Composable
private fun CopyButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(24.dp),
        content = {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy message",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
        }
    )
}

@Composable
private fun DeliveryStatusIcon(status: DeliveryStatus) {
    val (text, color, fontWeight) = when (status) {
        is DeliveryStatus.Sending -> Triple("○", MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), FontWeight.Normal)
        is DeliveryStatus.Sent -> Triple("✓", MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), FontWeight.Normal)
        is DeliveryStatus.Delivered -> Triple("✓✓", MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), FontWeight.Normal)
        is DeliveryStatus.Read -> Triple("✓✓", Color(0xFF007AFF), FontWeight.Bold)
        is DeliveryStatus.Failed -> Triple("⚠", Color.Red.copy(alpha = 0.8f), FontWeight.Normal)
        is DeliveryStatus.PartiallyDelivered -> Triple(
            "✓${status.reached}/${status.total}",
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            FontWeight.Normal
        )
    }

    Text(
        text = text,
        fontSize = 10.sp,
        color = color,
        fontWeight = fontWeight
    )
}
