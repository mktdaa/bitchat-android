package com.bitchat.android.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.bitchat.android.mesh.BluetoothMeshDelegate
import com.bitchat.android.mesh.BluetoothMeshService
import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.model.DeliveryAck
import com.bitchat.android.model.ReadReceipt
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.*
import kotlin.random.Random

/**
 * ViewModel رئيسي لتطبيق بلو للرسائل
 * يدير جميع عمليات الدردشة والاتصال بين الأجهزة
 */
class ChatViewModel(application: Application) : AndroidViewModel(application), BluetoothMeshDelegate {
    
    private val context: Context = application.applicationContext
    
    // الخدمات الأساسية
    val meshService = BluetoothMeshService(context)
    
    // إدارة الحالة
    private val state = ChatState()
    
    // المديرون المتخصصون
    private val dataManager = DataManager(context)
    private val messageManager = MessageManager(state)
    private val channelManager = ChannelManager(state, messageManager, dataManager, viewModelScope)
    val privateChatManager = PrivateChatManager(state, messageManager, dataManager)
    private val commandProcessor = CommandProcessor(state, messageManager, channelManager, privateChatManager)
    private val notificationManager = NotificationManager(application.applicationContext)
    
    // معالج تفويض اتصالات الشبكة
    private val meshDelegateHandler = MeshDelegateHandler(
        state = state,
        messageManager = messageManager,
        channelManager = channelManager,
        privateChatManager = privateChatManager,
        notificationManager = notificationManager,
        coroutineScope = viewModelScope,
        onHapticFeedback = { ChatViewModelUtils.triggerHapticFeedback(context) },
        getMyPeerID = { meshService.myPeerID }
    )
    
    // تعريض الحالة عبر LiveData
    val messages: LiveData<List<BitchatMessage>> = state.messages
    val connectedPeers: LiveData<List<String>> = state.connectedPeers
    val nickname: LiveData<String> = state.nickname
    val isConnected: LiveData<Boolean> = state.isConnected
    val privateChats: LiveData<Map<String, List<BitchatMessage>>> = state.privateChats
    val selectedPrivateChatPeer: LiveData<String?> = state.selectedPrivateChatPeer
    val unreadPrivateMessages: LiveData<Set<String>> = state.unreadPrivateMessages
    val joinedChannels: LiveData<Set<String>> = state.joinedChannels
    val currentChannel: LiveData<String?> = state.currentChannel
    val channelMessages: LiveData<Map<String, List<BitchatMessage>>> = state.channelMessages
    val unreadChannelMessages: LiveData<Map<String, Int>> = state.unreadChannelMessages
    val passwordProtectedChannels: LiveData<Set<String>> = state.passwordProtectedChannels
    val showPasswordPrompt: LiveData<Boolean> = state.showPasswordPrompt
    val passwordPromptChannel: LiveData<String?> = state.passwordPromptChannel
    val showSidebar: LiveData<Boolean> = state.showSidebar
    val hasUnreadChannels = state.hasUnreadChannels
    val hasUnreadPrivateMessages = state.hasUnreadPrivateMessages
    val showCommandSuggestions: LiveData<Boolean> = state.showCommandSuggestions
    val commandSuggestions: LiveData<List<CommandSuggestion>> = state.commandSuggestions
    val favoritePeers: LiveData<Set<String>> = state.favoritePeers
    
    init {
        meshService.delegate = this
        loadAndInitialize()
    }
    
    private fun loadAndInitialize() {
        // تحميل اسم المستخدم
        val nickname = dataManager.loadNickname()
        state.setNickname(nickname)
        
        // تحميل بيانات القنوات
        val (joinedChannels, protectedChannels) = channelManager.loadChannelData()
        state.setJoinedChannels(joinedChannels)
        state.setPasswordProtectedChannels(protectedChannels)
        
        // تهيئة رسائل القنوات
        joinedChannels.forEach { channel ->
            if (!state.getChannelMessagesValue().containsKey(channel)) {
                val updatedChannelMessages = state.getChannelMessagesValue().toMutableMap()
                updatedChannelMessages[channel] = emptyList()
                state.setChannelMessages(updatedChannelMessages)
            }
        }
        
        // تحميل بيانات أخرى
        dataManager.loadFavorites()
        state.setFavoritePeers(dataManager.favoritePeers)
        dataManager.loadBlockedUsers()
        
        // بدء خدمات الشبكة
        meshService.startServices()
        
        // عرض رسالة ترحيبية إذا لم يكن هناك أقران متصلين
        viewModelScope.launch {
            delay(3000)
            if (state.getConnectedPeersValue().isEmpty() && state.getMessagesValue().isEmpty()) {
                val welcomeMessage = BitchatMessage(
                    sender = "system",
                    content = "احصل على الأشخاص حولك لتنزيل بلو للرسائل... وتحدث معهم هنا!",
                    timestamp = Date(),
                    isRelay = false
                )
                messageManager.addMessage(welcomeMessage)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        meshService.stopServices()
    }
    
    // MARK: - إدارة اسم المستخدم
    
    fun setNickname(newNickname: String) {
        state.setNickname(newNickname)
        dataManager.saveNickname(newNickname)
        meshService.sendBroadcastAnnounce()
    }
    
    // MARK: - إدارة القنوات
    
    fun joinChannel(channel: String, password: String? = null): Boolean {
        return channelManager.joinChannel(channel, password, meshService.myPeerID)
    }
    
    fun switchToChannel(channel: String?) {
        channelManager.switchToChannel(channel)
    }
    
    fun leaveChannel(channel: String) {
        channelManager.leaveChannel(channel)
        meshService.sendMessage("left $channel")
    }
    
    // MARK: - إدارة الدردشة الخاصة
    
    fun startPrivateChat(peerID: String) {
        val success = privateChatManager.startPrivateChat(peerID, meshService)
        if (success) {
            setCurrentPrivateChatPeer(peerID)
            clearNotificationsForSender(peerID)
        }
    }
    
    fun endPrivateChat() {
        privateChatManager.endPrivateChat()
        setCurrentPrivateChatPeer(null)
    }
    
    // MARK: - إرسال الرسائل
    
    fun sendMessage(content: String) {
        if (content.isEmpty()) return
        
        // التحقق من الأوامر
        if (content.startsWith("/")) {
            commandProcessor.processCommand(content, meshService, meshService.myPeerID) { messageContent, mentions, channel ->
                meshService.sendMessage(messageContent, mentions, channel)
            }
            return
        }
        
        val mentions = messageManager.parseMentions(content, meshService.getPeerNicknames().values.toSet(), state.getNicknameValue())
        val channels = messageManager.parseChannels(content)
        
        // الانضمام التلقائي للقنوات المذكورة
        channels.forEach { channel ->
            if (!state.getJoinedChannelsValue().contains(channel)) {
                joinChannel(channel)
            }
        }
        
        val selectedPeer = state.getSelectedPrivateChatPeerValue()
        val currentChannelValue = state.getCurrentChannelValue()
        
        if (selectedPeer != null) {
            // إرسال رسالة خاصة
            val recipientNickname = meshService.getPeerNicknames()[selectedPeer]
            privateChatManager.sendPrivateMessage(
                content, 
                selectedPeer, 
                recipientNickname,
                state.getNicknameValue(),
                meshService.myPeerID
            ) { messageContent, peerID, recipientNicknameParam, messageId ->
                meshService.sendPrivateMessage(messageContent, peerID, recipientNicknameParam, messageId)
            }
        } else {
            // إرسال رسالة عامة/قناة
            val message = BitchatMessage(
                sender = state.getNicknameValue() ?: meshService.myPeerID,
                content = content,
                timestamp = Date(),
                isRelay = false,
                senderPeerID = meshService.myPeerID,
                mentions = if (mentions.isNotEmpty()) mentions else null,
                channel = currentChannelValue
            )
            
            if (currentChannelValue != null) {
                channelManager.addChannelMessage(currentChannelValue, message, meshService.myPeerID)
                
                // التحقق مما إذا كانت القناة مشفرة
                if (channelManager.hasChannelKey(currentChannelValue)) {
                    channelManager.sendEncryptedChannelMessage(
                        content, 
                        mentions, 
                        currentChannelValue, 
                        state.getNicknameValue(),
                        meshService.myPeerID,
                        onEncryptedPayload = { encryptedData ->
                            meshService.sendMessage(content, mentions, currentChannelValue)
                        },
                        onFallback = {
                            meshService.sendMessage(content, mentions, currentChannelValue)
                        }
                    )
                } else {
                    meshService.sendMessage(content, mentions, currentChannelValue)
                }
            } else {
                messageManager.addMessage(message)
                meshService.sendMessage(content, mentions, null)
            }
        }
    }
    
    // MARK: - وظائف مساعدة
    
    fun getPeerIDForNickname(nickname: String): String? {
        return meshService.getPeerNicknames().entries.find { it.value == nickname }?.key
    }
    
    fun toggleFavorite(peerID: String) {
        privateChatManager.toggleFavorite(peerID)
    }
    
    fun registerPeerPublicKey(peerID: String, publicKeyData: ByteArray) {
        privateChatManager.registerPeerPublicKey(peerID, publicKeyData)
    }
    
    // MARK: - تصحيح الأخطاء
    
    fun getDebugStatus(): String {
        return meshService.getDebugStatus()
    }
    
    fun restartMeshServices() {
        viewModelScope.launch {
            meshService.stopServices()
            delay(1000)
            meshService.startServices()
        }
    }
    
    fun setAppBackgroundState(inBackground: Boolean) {
        meshService.connectionManager.setAppBackgroundState(inBackground)
        notificationManager.setAppBackgroundState(inBackground)
    }
    
    fun setCurrentPrivateChatPeer(peerID: String?) {
        notificationManager.setCurrentPrivateChatPeer(peerID)
    }
    
    fun clearNotificationsForSender(peerID: String) {
        notificationManager.clearNotificationsForSender(peerID)
    }
    
    // MARK: - الإكمال التلقائي للأوامر
    
    fun updateCommandSuggestions(input: String) {
        commandProcessor.updateCommandSuggestions(input)
    }
    
    fun selectCommandSuggestion(suggestion: CommandSuggestion): String {
        return commandProcessor.selectCommandSuggestion(suggestion)
    }
    
    // MARK: - تنفيذ BluetoothMeshDelegate
    
    override fun didReceiveMessage(message: BitchatMessage) {
        meshDelegateHandler.didReceiveMessage(message)
    }
    
    override fun didConnectToPeer(peerID: String) {
        meshDelegateHandler.didConnectToPeer(peerID)
    }
    
    override fun didDisconnectFromPeer(peerID: String) {
        meshDelegateHandler.didDisconnectFromPeer(peerID)
    }
    
    override fun didUpdatePeerList(peers: List<String>) {
        meshDelegateHandler.didUpdatePeerList(peers)
    }
    
    override fun didReceiveChannelLeave(channel: String, fromPeer: String) {
        meshDelegateHandler.didReceiveChannelLeave(channel, fromPeer)
    }
    
    override fun didReceiveDeliveryAck(ack: DeliveryAck) {
        meshDelegateHandler.didReceiveDeliveryAck(ack)
    }
    
    override fun didReceiveReadReceipt(receipt: ReadReceipt) {
        meshDelegateHandler.didReceiveReadReceipt(receipt)
    }
    
    override fun decryptChannelMessage(encryptedContent: ByteArray, channel: String): String? {
        return meshDelegateHandler.decryptChannelMessage(encryptedContent, channel)
    }
    
    override fun getNickname(): String? {
        return meshDelegateHandler.getNickname()
    }
    
    override fun isFavorite(peerID: String): Boolean {
        return meshDelegateHandler.isFavorite(peerID)
    }
    
    // MARK: - مسح الطوارئ
    
    fun panicClearAllData() {
        messageManager.clearAllMessages()
        channelManager.clearAllChannels()
        privateChatManager.clearAllPrivateChats()
        dataManager.clearAllData()
        
        val newNickname = "anon${Random.nextInt(1000, 9999)}"
        state.setNickname(newNickname)
        dataManager.saveNickname(newNickname)
        
        meshService.stopServices()
        
        viewModelScope.launch {
            delay(500)
            meshService.startServices()
        }
    }
}
