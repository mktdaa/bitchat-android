package com.bitchat.android.ui

import com.bitchat.android.model.BitchatMessage
import java.util.*

/**
 * Handles processing of IRC-style commands
 */
class CommandProcessor(
    private val state: ChatState,
    private val messageManager: MessageManager,
    private val channelManager: ChannelManager,
    private val privateChatManager: PrivateChatManager
) {
    
    // Available commands list
    private val baseCommands = listOf(
        CommandSuggestion("/block", emptyList(), "[اسم المستخدم]", "حظر مستخدم أو عرض قائمة المحظورين"),
        CommandSuggestion("/channels", emptyList(), null, "عرض جميع القنوات المتاحة"),
        CommandSuggestion("/clear", emptyList(), null, "مسح جميع الرسائل"),
        CommandSuggestion("/hug", emptyList(), "<اسم المستخدم>", "إرسال عناق دافئ لشخص ما"),
        CommandSuggestion("/j", listOf("/join"), "<اسم القناة>", "الانضمام إلى قناة أو إنشائها"),
        CommandSuggestion("/m", listOf("/msg"), "<اسم المستخدم> [الرسالة]", "إرسال رسالة خاصة"),
        CommandSuggestion("/slap", emptyList(), "<اسم المستخدم>", "صفع شخص ما بسمكة كبيرة"),
        CommandSuggestion("/unblock", emptyList(), "<اسم المستخدم>", "إلغاء حظر مستخدم"),
        CommandSuggestion("/w", emptyList(), null, "عرض المستخدمين المتصلين")
    )
    
    // MARK: - Command Processing
    
    fun processCommand(command: String, meshService: Any, myPeerID: String, onSendMessage: (String, List<String>, String?) -> Unit): Boolean {
        if (!command.startsWith("/")) return false
        
        val parts = command.split(" ")
        val cmd = parts.first()
        
        when (cmd) {
            "/j", "/join" -> handleJoinCommand(parts, myPeerID)
            "/m", "/msg" -> handleMessageCommand(parts, meshService)
            "/w" -> handleWhoCommand(meshService)
            "/clear" -> handleClearCommand()
            "/block" -> handleBlockCommand(parts, meshService)
            "/unblock" -> handleUnblockCommand(parts, meshService)
            "/hug" -> handleActionCommand(parts, "يعانق", "بعناق دافئ 🫂", meshService, myPeerID, onSendMessage)
            "/slap" -> handleActionCommand(parts, "يصفع", "بسمكة كبيرة 🐟", meshService, myPeerID, onSendMessage)
            "/channels" -> handleChannelsCommand()
            else -> handleUnknownCommand(cmd)
        }
        
        return true
    }
    
    private fun handleJoinCommand(parts: List<String>, myPeerID: String) {
        if (parts.size > 1) {
            val channelName = parts[1]
            val channel = if (channelName.startsWith("")) channelName else "$channelName"
            val success = channelManager.joinChannel(channel, null, myPeerID)
            if (success) {
                val systemMessage = BitchatMessage(
                    sender = "النظام",
                    content = "لقد انضممت إلى القناة $channel",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(systemMessage)
            }
        } else {
            val systemMessage = BitchatMessage(
                sender = "النظام",
                content = "طريقة الاستخدام: /join <اسم القناة>",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleMessageCommand(parts: List<String>, meshService: Any) {
        if (parts.size > 1) {
            val targetName = parts[1].removePrefix("@")
            val peerID = getPeerIDForNickname(targetName, meshService)
            
            if (peerID != null) {
                val success = privateChatManager.startPrivateChat(peerID, meshService)
                
                if (success) {
                    if (parts.size > 2) {
                        val messageContent = parts.drop(2).joinToString(" ")
                        val recipientNickname = getPeerNickname(peerID, meshService)
                        privateChatManager.sendPrivateMessage(
                            messageContent, 
                            peerID, 
                            recipientNickname,
                            state.getNicknameValue(),
                            getMyPeerID(meshService)
                        ) { content, peerIdParam, recipientNicknameParam, messageId ->
                            // This would trigger the actual mesh service send
                            sendPrivateMessageVia(meshService, content, peerIdParam, recipientNicknameParam, messageId)
                        }
                    } else {
                        val systemMessage = BitchatMessage(
                            sender = "النظام",
                            content = "بدأت محادثة خاصة مع $targetName",
                            timestamp = Date(),
                            isRelay = false
                        )
                        messageManager.addMessage(systemMessage)
                    }
                }
            } else {
                val systemMessage = BitchatMessage(
                    sender = "النظام",
                    content = "لم يتم العثور على المستخدم '$targetName'. قد يكون غير متصل أو يستخدم اسمًا مختلفًا.",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(systemMessage)
            }
        } else {
            val systemMessage = BitchatMessage(
                sender = "النظام",
                content = "طريقة الاستخدام: /msg <اسم المستخدم> [الرسالة]",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleWhoCommand(meshService: Any) {
        val connectedPeers = state.getConnectedPeersValue()
        val peerList = connectedPeers.joinToString(", ") { peerID ->
            // Convert peerID to nickname using the mesh service
            getPeerNickname(peerID, meshService)
        }
        
        val systemMessage = BitchatMessage(
            sender = "النظام",
            content = if (connectedPeers.isEmpty()) {
                "لا يوجد مستخدمون آخرون متصلون الآن."
            } else {
                "المستخدمون المتصلون: $peerList"
            },
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(systemMessage)
    }
    
    private fun handleClearCommand() {
        when {
            state.getSelectedPrivateChatPeerValue() != null -> {
                // Clear private chat
                val peerID = state.getSelectedPrivateChatPeerValue()!!
                messageManager.clearPrivateMessages(peerID)
            }
            state.getCurrentChannelValue() != null -> {
                // Clear channel messages
                val channel = state.getCurrentChannelValue()!!
                messageManager.clearChannelMessages(channel)
            }
            else -> {
                // Clear main messages
                messageManager.clearMessages()
            }
        }
    }
    
    private fun handleBlockCommand(parts: List<String>, meshService: Any) {
        if (parts.size > 1) {
            val targetName = parts[1].removePrefix("@")
            privateChatManager.blockPeerByNickname(targetName, meshService)
        } else {
            // List blocked users
            val blockedInfo = privateChatManager.listBlockedUsers()
            val systemMessage = BitchatMessage(
                sender = "النظام",
                content = blockedInfo,
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleUnblockCommand(parts: List<String>, meshService: Any) {
        if (parts.size > 1) {
            val targetName = parts[1].removePrefix("@")
            privateChatManager.unblockPeerByNickname(targetName, meshService)
        } else {
            val systemMessage = BitchatMessage(
                sender = "النظام",
                content = "طريقة الاستخدام: /unblock <اسم المستخدم>",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleActionCommand(
        parts: List<String>, 
        verb: String, 
        object_: String, 
        meshService: Any,
        myPeerID: String,
        onSendMessage: (String, List<String>, String?) -> Unit
    ) {
        if (parts.size > 1) {
            val targetName = parts[1].removePrefix("@")
            val actionMessage = "* ${state.getNicknameValue() ?: "شخص ما"} $verb $targetName $object_ *"
            
            // Send as regular message
            if (state.getSelectedPrivateChatPeerValue() != null) {
                val peerID = state.getSelectedPrivateChatPeerValue()!!
                privateChatManager.sendPrivateMessage(
                    actionMessage,
                    peerID,
                    getPeerNickname(peerID, meshService),
                    state.getNicknameValue(),
                    myPeerID
                ) { content, peerIdParam, recipientNicknameParam, messageId ->
                    sendPrivateMessageVia(meshService, content, peerIdParam, recipientNicknameParam, messageId)
                }
            } else {
                val message = BitchatMessage(
                    sender = state.getNicknameValue() ?: myPeerID,
                    content = actionMessage,
                    timestamp = Date(),
                    isRelay = false,
                    senderPeerID = myPeerID,
                    channel = state.getCurrentChannelValue()
                )
                
                if (state.getCurrentChannelValue() != null) {
                    channelManager.addChannelMessage(state.getCurrentChannelValue()!!, message, myPeerID)
                    onSendMessage(actionMessage, emptyList(), state.getCurrentChannelValue())
                } else {
                    messageManager.addMessage(message)
                    onSendMessage(actionMessage, emptyList(), null)
                }
            }
        } else {
            val systemMessage = BitchatMessage(
                sender = "النظام",
                content = "طريقة الاستخدام: /${parts[0].removePrefix("/")} <اسم المستخدم>",
                timestamp = Date(),
                isRelay = false
            )
            messageManager.addMessage(systemMessage)
        }
    }
    
    private fun handleChannelsCommand() {
        val allChannels = channelManager.getJoinedChannelsList()
        val channelList = if (allChannels.isEmpty()) {
            "لم تنضم إلى أي قناة"
        } else {
            "القنوات التي أنت عضو فيها: ${allChannels.joinToString(", ")}"
        }
        
        val systemMessage = BitchatMessage(
            sender = "النظام",
            content = channelList,
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(systemMessage)
    }
    
    private fun handleUnknownCommand(cmd: String) {
        val systemMessage = BitchatMessage(
            sender = "النظام",
            content = "أمر غير معروف: $cmd. اكتب / لرؤية الأوامر المتاحة.",
            timestamp = Date(),
            isRelay = false
        )
        messageManager.addMessage(systemMessage)
    }
    
    // MARK: - Command Autocomplete
    
    fun updateCommandSuggestions(input: String) {
        if (!input.startsWith("/") || input.length < 1) {
            state.setShowCommandSuggestions(false)
            state.setCommandSuggestions(emptyList())
            return
        }
        
        // Get all available commands based on context
        val allCommands = getAllAvailableCommands()
        
        // Filter commands based on input
        val filteredCommands = filterCommands(allCommands, input.lowercase())
        
        if (filteredCommands.isNotEmpty()) {
            state.setCommandSuggestions(filteredCommands)
            state.setShowCommandSuggestions(true)
        } else {
            state.setShowCommandSuggestions(false)
            state.setCommandSuggestions(emptyList())
        }
    }
    
    private fun getAllAvailableCommands(): List<CommandSuggestion> {
        // Add channel-specific commands if in a channel
        val channelCommands = if (state.getCurrentChannelValue() != null) {
            listOf(
                CommandSuggestion("/pass", emptyList(), "[كلمة المرور]", "تغيير كلمة مرور القناة"),
                CommandSuggestion("/save", emptyList(), null, "حفظ رسائل القناة محليًا"),
                CommandSuggestion("/transfer", emptyList(), "<اسم المستخدم>", "نقل ملكية القناة")
            )
        } else {
            emptyList()
        }
        
        return baseCommands + channelCommands
    }
    
    private fun filterCommands(commands: List<CommandSuggestion>, input: String): List<CommandSuggestion> {
        return commands.filter { command ->
            // Check primary command
            command.command.startsWith(input) ||
            // Check aliases
            command.aliases.any { it.startsWith(input) }
        }.sortedBy { it.command }
    }
    
    fun selectCommandSuggestion(suggestion: CommandSuggestion): String {
        state.setShowCommandSuggestions(false)
        state.setCommandSuggestions(emptyList())
        return "${suggestion.command} "
    }
    
    // MARK: - Utility Functions (would access mesh service)
    
    private fun getPeerIDForNickname(nickname: String, meshService: Any): String? {
        return try {
            val method = meshService::class.java.getDeclaredMethod("getPeerNicknames")
            val peerNicknames = method.invoke(meshService) as? Map<String, String>
            peerNicknames?.entries?.find { it.value == nickname }?.key
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getPeerNickname(peerID: String, meshService: Any): String {
        return try {
            val method = meshService::class.java.getDeclaredMethod("getPeerNicknames")
            val peerNicknames = method.invoke(meshService) as? Map<String, String>
            peerNicknames?.get(peerID) ?: peerID
        } catch (e: Exception) {
            peerID
        }
    }
    
    private fun getMyPeerID(meshService: Any): String {
        return try {
            val field = meshService::class.java.getDeclaredField("myPeerID")
            field.isAccessible = true
            field.get(meshService) as? String ?: "غير معروف"
        } catch (e: Exception) {
            "غير معروف"
        }
    }
    
    private fun sendPrivateMessageVia(meshService: Any, content: String, peerID: String, recipientNickname: String, messageId: String) {
        try {
            val method = meshService::class.java.getDeclaredMethod(
                "sendPrivateMessage", 
                String::class.java, 
                String::class.java, 
                String::class.java, 
                String::class.java
            )
            method.invoke(meshService, content, peerID, recipientNickname, messageId)
        } catch (e: Exception) {
            // Handle error
        }
    }
}
