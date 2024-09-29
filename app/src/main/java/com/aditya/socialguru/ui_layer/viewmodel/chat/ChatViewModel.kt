package com.aditya.socialguru.ui_layer.viewmodel.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.ChatMediaData
import com.aditya.socialguru.data_layer.model.chat.LastMessage
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.data_layer.model.chat.UpdateChatResponse
import com.aditya.socialguru.data_layer.model.chat.UserRecentModel
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo
import com.aditya.socialguru.data_layer.model.chat.group.GroupLastMessage
import com.aditya.socialguru.data_layer.model.chat.group.GroupMember
import com.aditya.socialguru.data_layer.model.chat.group.GroupMemberDetails
import com.aditya.socialguru.data_layer.model.chat.group.GroupMessage
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.manager.SoftwareManager.isNetworkAvailable
import com.aditya.socialguru.domain_layer.repository.chat.ChatRepo
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.domain_layer.service.firebase_service.ChatManager
import com.bumptech.glide.load.Transformation
import com.google.rpc.ErrorInfo
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ChatViewModel(val app: Application) : AndroidViewModel(app) {
    private val tagChat = Constants.LogTag.Chats

    private val repository = ChatRepo()
    private val senderId by lazy {
        AuthManager.currentUserId()!!
    }

    private var _isDataLoaded = false

    val isDataLoaded get() = _isDataLoaded

    private var _imageUri: Uri? = null
    val imageUri get() = _imageUri

    private var _videoUri: Uri? = null
    val videoUri get() = _videoUri

    private val _friendList = MutableSharedFlow<Resource<List<FriendCircleData>>>(
        1, 64, BufferOverflow.DROP_OLDEST
    )
    val friendList get() = _friendList.asSharedFlow()

    private val _userDetails = MutableSharedFlow<Resource<User>>(
        1, 64, BufferOverflow.DROP_OLDEST
    )
    val userDetails get() = _userDetails.asSharedFlow()

    private val _sendMessage =
        MutableSharedFlow<Resource<UpdateChatResponse>>(0, 64, BufferOverflow.DROP_OLDEST)
    val sendMessage get() = _sendMessage.asSharedFlow()

    private val _deleteMessage =
        MutableSharedFlow<Resource<UpdateResponse>>(0, 64, BufferOverflow.DROP_OLDEST)
    val deleteMessage get() = _deleteMessage.asSharedFlow()

    private val _clearChat =
        MutableSharedFlow<Resource<UpdateResponse>>(0, 64, BufferOverflow.DROP_OLDEST)
    val clearChat get() = _clearChat.asSharedFlow()

    private val _recentChat = MutableSharedFlow<Resource<List<UserRecentModel>>>(
        1, 64, BufferOverflow.DROP_OLDEST
    )
    val recentChat get() = _recentChat.asSharedFlow()

    private val _chatMessage =
        MutableSharedFlow<Resource<List<Message>>>(1, 64, BufferOverflow.DROP_OLDEST)
    val chatMessage get() = _chatMessage.asSharedFlow()

    private val _lastMessage = MutableSharedFlow<LastMessage>(1, 64, BufferOverflow.DROP_OLDEST)
    val lastMessage = _lastMessage.asSharedFlow()

    private val _isMuted = MutableSharedFlow<Resource<Boolean>>(1, 64, BufferOverflow.DROP_OLDEST)
    val isMuted = _isMuted.asSharedFlow()

    private val _muteOperation = MutableSharedFlow<Resource<UpdateResponse>>(0, 64, BufferOverflow.DROP_OLDEST)
    val muteOperation = _muteOperation.asSharedFlow()

    private val _chatMedia = MutableSharedFlow<Resource<List<ChatMediaData>>>(1 , 64 ,BufferOverflow.DROP_OLDEST)
    val chatMedia  = _chatMedia.asSharedFlow()

    //For Group
    private val _sendGroupMessage =
        MutableSharedFlow<Resource<UpdateChatResponse>>(0, 64, BufferOverflow.DROP_OLDEST)
    val sendGroupMessage get() = _sendGroupMessage.asSharedFlow()
    private val _sendGroupInfoMessage =
        MutableSharedFlow<Resource<UpdateResponse>>(0, 64, BufferOverflow.DROP_OLDEST)
    val sendGroupInfoMessage get() = _sendGroupInfoMessage.asSharedFlow()

    private val _deleteGroupMessage =
        MutableSharedFlow<Resource<UpdateResponse>>(0, 64, BufferOverflow.DROP_OLDEST)
    val deleteGroupMessage get() = _deleteGroupMessage.asSharedFlow()

    private val _clearGroupChat =
        MutableSharedFlow<Resource<UpdateResponse>>(0, 64, BufferOverflow.DROP_OLDEST)
    val clearGroupChat get() = _clearGroupChat.asSharedFlow()

    private val _groupChatMessage =
        MutableSharedFlow<Resource<List<GroupMessage>>>(1, 64, BufferOverflow.DROP_OLDEST)
    val groupChatMessage get() = _groupChatMessage.asSharedFlow()

    private val _groupMemberDetails=
        MutableSharedFlow<Resource<List<GroupMemberDetails>>>(1, 64, BufferOverflow.DROP_OLDEST)
    val groupMemberDetails get() = _groupMemberDetails.asSharedFlow()

    private val _groupInfo=
        MutableSharedFlow<Resource<GroupInfo>>(1, 64, BufferOverflow.DROP_OLDEST)
    val groupInfo get() = _groupInfo.asSharedFlow()

    private val _updateGroupDetails=
        MutableSharedFlow<Resource<UpdateResponse>>(1, 64, BufferOverflow.DROP_OLDEST)
    val updateGroupDetails get() = _updateGroupDetails.asSharedFlow()

    private val _userListDetailsByIds = MutableSharedFlow<Resource<List<FriendCircleData>>>(1,64, BufferOverflow.DROP_OLDEST)
    val userDetailsByIds get() = _userListDetailsByIds.asSharedFlow()

    private val _deleteRecentChat = MutableSharedFlow<Resource<UpdateResponse>>(0, 64, BufferOverflow.DROP_OLDEST)
    val deleteRecentChat = _deleteRecentChat.asSharedFlow()






    val textCount: MutableLiveData<Int> = MutableLiveData(0)  // type :-> 0
    val groupNameTextCount: MutableLiveData<Int> = MutableLiveData(0) // type :-> 1
    val groupDescTextCount: MutableLiveData<Int> = MutableLiveData(0) // type :-> 2


    init {
        MyLogger.w(tagChat , msg = "chat view model is make with hashcode :-> ${this.hashCode()}")
    }

    fun setDefaultTextCount(count:Int, type:Int){
        when(type){
            0->{
                textCount.postValue(count)
            }
            1-> {
                groupNameTextCount.postValue(count)
            }
            2-> {
                groupDescTextCount.postValue(count)
            }
        }
    }


    fun onTextChanged(
        s: CharSequence, start: Int, before: Int,
        count: Int
    ) {
        textCount.postValue(s.length)
    }
    fun onGroupNameTextChanged(
        s: CharSequence, start: Int, before: Int,
        count: Int
    ) {
        groupNameTextCount.postValue(s.length)
    }
    fun onGroupDescTextChanged(
        s: CharSequence, start: Int, before: Int,
        count: Int
    ) {
        groupDescTextCount.postValue(s.length)
    }


    //region:: Friend Operation
    fun getFriendListAndListenChange() = viewModelScope.myLaunch {
        _friendList.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getFriendList().onEach {
                _friendList.tryEmit(handleFriendResponse(it))
            }.launchIn(this)

        } else {
            _friendList.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }

    private fun handleFriendResponse(listenerHandling: ListenerEmissionType<FriendCircleData, FriendCircleData>): Resource<List<FriendCircleData>> {
        MyLogger.v(tagChat, isFunctionCall = true)

        val friendList =
            friendList.replayCache[0].data?.toMutableList() ?: mutableListOf<FriendCircleData>()

        when (listenerHandling.emitChangeType) {


            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagChat,
                    msg = listenerHandling.responseList,
                    isJson = true,
                    jsonTitle = "This is starting friend type"
                )

                friendList.clear()
                listenerHandling.responseList?.let {
                    friendList.addAll(it.toMutableList() as ArrayList<FriendCircleData>)
                    friendList.sortByDescending { it.timeStamp }
                }
            }


            Constants.ListenerEmitType.Added -> {
                MyLogger.v(
                    tagChat,
                    msg = listenerHandling.singleResponse,
                    isJson = true,
                    jsonTitle = "This is added friend type"
                )

                listenerHandling.singleResponse?.let {
                    friendList.add(it)
                    friendList.sortByDescending { it.timeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagChat, msg = "This is removed friend type")

                listenerHandling.singleResponse?.let { follower ->
                    follower.userId?.let { userId ->
                        friendList.forEach { temp ->
                            if (temp.userId == userId) {
                                friendList.remove(temp)
                                friendList.sortBy { it.timeStamp }
                                return@let
                            }
                        }
                    }
                }
            }


            else -> {}
        }

        return Resource.Success(friendList.toList())
    }

    //endregion

    //region:: Get User Detail
    fun getUser(userId: String) = viewModelScope.myLaunch {
        _userDetails.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getUser(userId).onEach {
                _userDetails.tryEmit(it)
            }.launchIn(this)
        } else {
            _userDetails.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }
    //endregion

    //region:: Send Message

    fun sendMessage(
        message: Message, lastMessage: LastMessage, charRoomId: String, isUserOnline: Boolean , onSend:()->Unit
    ) = viewModelScope.myLaunch {
        val isImagePresent = message.imageUri != null
        val isVideoPresent = message.videoUri != null

        if (SoftwareManager.isNetworkAvailable(app)) {
            if (isVideoPresent || isImagePresent) {
                _sendMessage.tryEmit(Resource.Loading())
            }else{
                onSend.invoke()
            }
            repository.sendMessage(message, lastMessage, charRoomId, isUserOnline).onEach {
                MyLogger.i(
                    tagChat, msg = it, isJson = true, jsonTitle = "Message sent response come"
                )
                if (it.isSuccess || it.isSending) {
                    _sendMessage.tryEmit(Resource.Success(it))
                } else {
                    _sendMessage.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        } else {
            _sendMessage.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    //endregion

    //region:: Get Chat Message

    fun getChatMessage(chatRoomId: String) = viewModelScope.myLaunch {
        _chatMessage.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getChatMessageAndListen(chatRoomId).onEach {
                MyLogger.v(
                    tagChat, msg = it, isJson = true, jsonTitle = "Current Chat Message Updated "
                )
                _chatMessage.tryEmit(handleChatMessageResponse(it, chatRoomId))
            }.launchIn(this)
        } else {
            _chatMessage.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }

    private fun handleChatMessageResponse(
        it: ListenerEmissionType<Message, Message>, chatRoomId: String
    ): Resource<List<Message>> {

        val chatMessageList = chatMessage.replayCache[0].data?.toMutableList() ?: mutableListOf()

        when (it.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                chatMessageList.clear()
                it.responseList?.let {
                    chatMessageList.addAll(it)
                    chatMessageList.sortBy { it.messageSentTimeInTimeStamp }
                }
            }

            Constants.ListenerEmitType.Added -> {
                it.singleResponse?.let {
                    chatMessageList.add(it)
                    chatMessageList.sortBy { it.messageSentTimeInTimeStamp }
                    if (it.senderId != senderId) {
                        FirebaseManager.updateSeenStatus(
                            Constants.SeenStatus.MessageSeen.status,
                            it.messageId!!,
                            chatRoomId,
                            it.senderId!!
                        )
                    }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                it.singleResponse?.messageId?.let { removedMessageId ->
                    chatMessageList.forEach {
                        val messageId = it.messageId
                        if (messageId != null && messageId == removedMessageId) {
                            chatMessageList.remove(it)
                            chatMessageList.sortBy { it.messageSentTimeInTimeStamp }
                            return@let
                        }
                    }
                }
            }

            Constants.ListenerEmitType.Modify -> {
                it.singleResponse?.let { modifiedMessage ->
                    val existingMessage =
                        chatMessageList.find { it.messageId == modifiedMessage.messageId }
                    existingMessage?.apply {
                        seenStatus = modifiedMessage.seenStatus
                        // Ensure the list remains sorted by message timestamp
                        chatMessageList.sortBy { message -> message.messageSentTimeInTimeStamp }
                    }
                }
            }
        }

        return Resource.Success(addDateHeaders(chatMessageList.filter { it.messageType != Constants.MessageType.DateHeader.type }))

    }

    private fun addDateHeaders(messages: List<Message>): List<Message> {
        if (messages.isEmpty()) return messages

        val messagesWithHeaders = mutableListOf<Message>()
        var lastDateHeader: String? = null

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
//        MyLogger.d(tagChat, msg = messages, isJson = true, jsonTitle = "Messages List")

        messages.forEach { message ->
            val messageDate = message.messageSentTimeInTimeStamp?.let {
                Calendar.getInstance().apply { timeInMillis = it }
            }
            val currentDateHeader = when {
                messageDate == null -> null
                isSameDay(messageDate, today) -> "Today"
                isSameDay(messageDate, yesterday) -> "Yesterday"
                else -> dateFormatter.format(messageDate.time)
            }

            // Add new date header if currentDateHeader is different from lastDateHeader
//            MyLogger.w(tagChat, msg = "lastDateHeader:- $lastDateHeader  and currentDateHeader:- $currentDateHeader , currentDateHeader != lastDateHeader :=> ${currentDateHeader != lastDateHeader}")
            if (currentDateHeader != null && currentDateHeader != lastDateHeader) {
                lastDateHeader = currentDateHeader
                messagesWithHeaders.add(
                    Message(
                        messageType = Constants.MessageType.DateHeader.type,
                        text = currentDateHeader
                    )
                )
            }

            messagesWithHeaders.add(message)
        }

        return messagesWithHeaders
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(
            Calendar.DAY_OF_YEAR
        )
    }
    //endregion

    //region:: Listen Last Message Status

    fun listenLastMessage(chatRoomId: String) = viewModelScope.myLaunch {
        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.listenLastMessage(chatRoomId).onEach {
                MyLogger.w(tagChat, msg = it, isJson = true, jsonTitle = "Last Message Updated !")
                _lastMessage.tryEmit(it)
            }.launchIn(this)
        }else{
            MyLogger.e(tagChat, msg = "Last Message listen failed due to no network available!")
        }
    }

    //endregion

    //region::Update Message Seen Availability
    fun updateMessageSeenAvailability(message: List<Message>, chatRoomId: String) =
        viewModelScope.myLaunch {
            message.forEach {
                if (it.messageId != null) {
                    repository.updateMessageChatAvailability(
                        Constants.SeenStatus.MessageSeen.status,
                        it.messageId,
                        chatRoomId,
                        it.senderId!!
                    )
                }
            }
        }

    //endregion

    //region:: Set User online visibility

    fun updateUserAvailabilityForChatRoom(
        chatRoomId: String, isIAmUser1: Boolean, status: Boolean
    ) = viewModelScope.myLaunch {
        repository.updateUserAvailabilityForChatRoom(chatRoomId, isIAmUser1, status)
    }
    //endregion

    //region:: Get Recent Chat

    fun getRecentChat() = viewModelScope.myLaunch {
        _recentChat.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getRecentChatAndListen().onEach {
                MyLogger.v(
                    tagChat, msg = it, isJson = true, jsonTitle = "Recent Chat List  "
                )
                _recentChat.tryEmit(handleRecentChatMessageResponse(it))
            }.launchIn(this)
        } else {
            _recentChat.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }

    private fun handleRecentChatMessageResponse(
        it: ListenerEmissionType<UserRecentModel, UserRecentModel>
    ): Resource<List<UserRecentModel>> {

        val recentChatList = recentChat.replayCache[0].data?.toMutableList() ?: mutableListOf()

        when (it.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                recentChatList.clear()
                it.responseList?.let {
                    recentChatList.addAll(it)
                    recentChatList.sortByDescending { it.recentChat?.lastMessageTimeInTimeStamp }
                }
            }

            Constants.ListenerEmitType.Added -> {
                it.singleResponse?.let {
                    recentChatList.add(it)
                    recentChatList.sortByDescending { it.recentChat?.lastMessageTimeInTimeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                if (it.singleResponse?.groupInfo!=null){
                    //For Group Chat
                    val groupInfo = it.singleResponse.groupInfo!!
                    val deleteRecentChat: UserRecentModel? = recentChatList.find { it.groupInfo?.chatRoomId == groupInfo.chatRoomId }
                    if(deleteRecentChat!=null){
                        recentChatList.remove(deleteRecentChat)
                        recentChatList.sortByDescending { it.recentChat?.lastMessageTimeInTimeStamp }
                    }
                }else{
                    //For Single chat
                    it.singleResponse?.recentChat?.receiverId?.let { removedMessageId ->
                        recentChatList.forEach {
                            val userId = it.user?.userId
                            if (userId != null && userId == removedMessageId) {
                                recentChatList.remove(it)
                                recentChatList.sortByDescending { it.recentChat?.lastMessageTimeInTimeStamp }
                                return@let
                            }
                        }
                    }
                }

            }

            Constants.ListenerEmitType.Modify -> {
                it.singleResponse?.let { modifiedMessage ->
                    val existingMessage =
                        recentChatList.find { it.user?.userId == modifiedMessage.user?.userId }
                    existingMessage?.apply {
                        recentChat = recentChat?.copy(
                            chatRoomId = modifiedMessage.recentChat?.chatRoomId,
                            lastMessageTimeInTimeStamp = modifiedMessage.recentChat?.lastMessageTimeInTimeStamp,
                            lastMessageTimeInText = modifiedMessage.recentChat?.lastMessageTimeInText,
                            unSeenMessageCount = modifiedMessage.recentChat?.unSeenMessageCount,
                            message = modifiedMessage.recentChat?.message,
                            lastMessageType = modifiedMessage.recentChat?.lastMessageType,
                            receiverId = modifiedMessage.recentChat?.receiverId,
                            senderId = modifiedMessage.recentChat?.senderId,
                            userId = modifiedMessage.recentChat?.userId,
                            lastMessageSeen = modifiedMessage.recentChat?.lastMessageSeen,
                            infoMessageType = modifiedMessage.recentChat?.infoMessageType,
                            isGroupChat = modifiedMessage.recentChat?.isGroupChat,
                            addedOrRemovedUserId = modifiedMessage.recentChat?.addedOrRemovedUserId
                        )
                        MyLogger.w(
                            tagChat,
                            msg = existingMessage,
                            isJson = true,
                            jsonTitle = "Existing Message"
                        )
                        // Ensure the list remains sorted by message timestamp
                        recentChatList.sortByDescending { recentChat -> recentChat.recentChat?.lastMessageTimeInTimeStamp }
                    }
                }
            }
        }

        return Resource.Success(recentChatList)

    }


    //endregion

    //region:: Delete Message

    fun deleteMessage(
        message: Message,
        chatRoomId: String,
        userId: String,
        lastMessage: LastMessage?,
        secondLastMessage: Message? = null
    ) = viewModelScope.myLaunch {
        _deleteMessage.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.deleteMessage(
                message, chatRoomId, userId, lastMessage, secondLastMessage
            ).onEach {
                MyLogger.i(
                    tagChat, msg = it, isJson = true, jsonTitle = "Delete message response come"
                )
                if (it.isSuccess) {
                    _deleteMessage.tryEmit(Resource.Success(it))
                } else {
                    _deleteMessage.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        } else {
            _deleteMessage.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    //endregion

    //region:: Clear chat

    fun clearChat(
        chatRoomId: String, receiverId: String
    ) = viewModelScope.myLaunch {
        _clearChat.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.clearChats(
                chatRoomId, receiverId
            ).onEach {
                MyLogger.i(
                    tagChat, msg = it, isJson = true, jsonTitle = "Message sent response come"
                )
                if (it.isSuccess) {
                    _clearChat.tryEmit(Resource.Success(it))
                } else {
                    _clearChat.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        } else {
            _clearChat.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    //endregion

    //region:: Get And Listen Muted state of user
    fun isUserMutedAndListen(userId: String) = viewModelScope.myLaunch {
        _isMuted.tryEmit(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.isUserMutedAndListen(userId).onEach {
                _isMuted.tryEmit(Resource.Success(it))
            }.launchIn(this)
        } else {
            _isMuted.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }
    //endregion

    //region:: Mute or Un-mute user

    fun muteChatNotification(userId: String,isMute:Boolean) = viewModelScope.myLaunch {
        _muteOperation.tryEmit(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.muteChatNotification(userId,isMute).onEach {
                if (it.isSuccess){
                    _muteOperation.tryEmit(Resource.Success(it))
                }else{
                    _muteOperation.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        }else{
            MyLogger.e(tagChat, msg = "Internet Off")
            _muteOperation.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    //endregion

    //region:: Get All Media For specific chat

    fun getAllMediaForChat(chatRoomId: String , category: Int=0) = viewModelScope.myLaunch {
        _chatMedia.tryEmit(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)){
            repository.getAllMediaOfChat(chatRoomId).onEach {
                _chatMedia.tryEmit(Resource.Success(handleMediaResponse(it,category)))
            }.launchIn(this)
        }else{
            _chatMedia.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }

    private fun handleMediaResponse(media: List<ChatMediaData>, category: Int): List<ChatMediaData> {
        if (media.isEmpty()) return emptyList()
        val sortedMedia=media.sortedByDescending { it.mediaUploadingTimeInTimeStamp}

        return when(category){
            0->{
                // All Media
                giveMeMediaWithDate(sortedMedia)
            }
            1->{
                // Only Image
                giveMeMediaWithDate(sortedMedia.filter { it.isImage==true })
            }
            2->{
                //Only Video
                giveMeMediaWithDate(sortedMedia.filter { it.isImage==false })
            }
            else->{
                //By default All Media
                giveMeMediaWithDate(sortedMedia)
            }
        }
    }

    private fun giveMeMediaWithDate(media: List<ChatMediaData>): List<ChatMediaData> {
        if (media.isEmpty()) return media

        val messagesWithHeaders = mutableListOf<ChatMediaData>()
        var lastDateHeader: String? = null

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
//        MyLogger.d(tagChat, msg = messages, isJson = true, jsonTitle = "Messages List")

        media.forEach { message ->
            val messageDate = message.mediaUploadingTimeInTimeStamp?.let {
                Calendar.getInstance().apply { timeInMillis = it }
            }
            val currentDateHeader = when {
                messageDate == null -> null
                isSameDay(messageDate, today) -> "Today"
                isSameDay(messageDate, yesterday) -> "Yesterday"
                else -> dateFormatter.format(messageDate.time)
            }

            if (currentDateHeader != null && currentDateHeader != lastDateHeader) {
                lastDateHeader = currentDateHeader
                messagesWithHeaders.add(
                    ChatMediaData(
                        mediaUri = null,
                        date = currentDateHeader
                    )
                )
            }

            messagesWithHeaders.add(message)
        }

        return messagesWithHeaders
    }

    //endregion


    //region:: Get User List by ids

     fun getAllUserByIds(userIds:List<String>) = viewModelScope.myLaunch {
        _userListDetailsByIds.tryEmit(Resource.Loading())

        if (isNetworkAvailable(app)){
            repository.getAllUserByIds(userIds).onEach {
                _userListDetailsByIds.tryEmit(Resource.Success(it))
            }.launchIn(this)
        }else{
            _userListDetailsByIds.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }

    //endregion


    //region:: Group Message

     fun sendGroupMessage(
         message: GroupMessage,
         lastMessage: GroupLastMessage,
         chatRoomId: String,
         users: List<GroupMember>,// Just for add  message recent chat
         action: Constants.InfoType? = null,
         addedOrRemovedUserId: String? = null,
         groupInfo: GroupInfo? = null ,
         callback:()->Unit,
     ) = viewModelScope.myLaunch {
        val isImagePresent = message.imageUri != null
        val isVideoPresent = message.videoUri != null

        if (SoftwareManager.isNetworkAvailable(app)) {
            if (isVideoPresent || isImagePresent|| groupInfo!=null) {
                MyLogger.d(tagChat, msg = "Now Loading state is set !")
                _sendGroupMessage.tryEmit(Resource.Loading())
            }else{
                MyLogger.d(tagChat, msg = "call back is call !")
                callback.invoke()
            }

            repository.sentGroupMessage(
                message,
                lastMessage,
                chatRoomId,
                users,
                action,
                addedOrRemovedUserId,
                groupInfo
            ).onEach {
                MyLogger.i(
                    tagChat, msg = it, isJson = true, jsonTitle = "Message sent response come"
                )
                if (it.isSuccess || it.isSending|| groupInfo!=null) {
                    _sendGroupMessage.tryEmit(Resource.Success(it))
                } else {
                    _sendGroupMessage.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        } else {
            _sendGroupMessage.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    fun getGroupChatMessage(chatRoomId: String) = viewModelScope.myLaunch {
        _groupChatMessage.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getGroupChatMessageAndListen(chatRoomId).onEach {
                MyLogger.v(
                    tagChat, msg = it, isJson = true, jsonTitle = "Current Chat Message Updated "
                )
                _groupChatMessage.tryEmit(handleGroupChatMessageResponse(it, chatRoomId))
            }.launchIn(this)
        } else {
            _groupChatMessage.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }

    private fun handleGroupChatMessageResponse(
        it: ListenerEmissionType<GroupMessage, GroupMessage>, chatRoomId: String
    ): Resource<List<GroupMessage>> {

        val chatMessageList = groupChatMessage.replayCache[0].data?.toMutableList() ?: mutableListOf()

        when (it.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                chatMessageList.clear()
                it.responseList?.let {
                    chatMessageList.addAll(it)
                    chatMessageList.sortBy { it.messageSentTimeInTimeStamp }
                }
            }

            Constants.ListenerEmitType.Added -> {
                it.singleResponse?.let {
                    chatMessageList.add(it)
                    chatMessageList.sortBy { it.messageSentTimeInTimeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                it.singleResponse?.messageId?.let { removedMessageId ->
                    chatMessageList.forEach {
                        val messageId = it.messageId
                        if (messageId != null && messageId == removedMessageId) {
                            chatMessageList.remove(it)
                            chatMessageList.sortBy { it.messageSentTimeInTimeStamp }
                            return@let
                        }
                    }
                }
            }

            Constants.ListenerEmitType.Modify -> {
                it.singleResponse?.let { modifiedMessage ->
                    val existingMessage =
                        chatMessageList.find { it.messageId == modifiedMessage.messageId }
                    existingMessage?.apply {
                        seenStatus = modifiedMessage.seenStatus
                        // Ensure the list remains sorted by message timestamp
                        chatMessageList.sortBy { message -> message.messageSentTimeInTimeStamp }
                    }
                }
            }
        }

        return Resource.Success(addDateHeadersInGroup(chatMessageList.filter { it.messageType != Constants.MessageType.DateHeader.type }))

    }

    private fun addDateHeadersInGroup(messages: List<GroupMessage>): List<GroupMessage> {
        if (messages.isEmpty()) return messages

        val messagesWithHeaders = mutableListOf<GroupMessage>()
        var lastDateHeader: String? = null

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
//        MyLogger.d(tagChat, msg = messages, isJson = true, jsonTitle = "Messages List")

        messages.forEach { message ->
            val messageDate = message.messageSentTimeInTimeStamp?.let {
                Calendar.getInstance().apply { timeInMillis = it }
            }
            val currentDateHeader = when {
                messageDate == null -> null
                isSameDay(messageDate, today) -> "Today"
                isSameDay(messageDate, yesterday) -> "Yesterday"
                else -> dateFormatter.format(messageDate.time)
            }

            // Add new date header if currentDateHeader is different from lastDateHeader
//            MyLogger.w(tagChat, msg = "lastDateHeader:- $lastDateHeader  and currentDateHeader:- $currentDateHeader , currentDateHeader != lastDateHeader :=> ${currentDateHeader != lastDateHeader}")
            if (currentDateHeader != null && currentDateHeader != lastDateHeader) {
                lastDateHeader = currentDateHeader
                messagesWithHeaders.add(
                    GroupMessage(
                        messageType = Constants.MessageType.DateHeader.type,
                        text = currentDateHeader
                    )
                )
            }

            messagesWithHeaders.add(message)
        }

        return messagesWithHeaders
    }


    fun getGroupMemberDetails(chatRoomId: String)= viewModelScope.myLaunch {
        _groupMemberDetails.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)){
            repository.getGroupMemberInfo(chatRoomId).onEach {
                MyLogger.i(tagChat, msg = it, isJson = true , jsonTitle = "Group Member Details response come !")
                _groupMemberDetails.tryEmit(handleGroupMemberDetails(it))
            }.launchIn(this)
        }else{
            _groupMemberDetails.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }

    private fun handleGroupMemberDetails(it: ListenerEmissionType<GroupMemberDetails, GroupMemberDetails>): Resource<List<GroupMemberDetails>> {
        val groupMemberDetails = groupMemberDetails.replayCache[0].data?.toMutableList() ?: mutableListOf()

        when (it.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                groupMemberDetails.clear()
                it.responseList?.let {
                    groupMemberDetails.addAll(it)
                }
            }

            Constants.ListenerEmitType.Added -> {
                it.singleResponse?.let {
                    groupMemberDetails.add(it)
                }
            }

            Constants.ListenerEmitType.Removed -> {
                it.singleResponse?.member?.memberId?.let { memberId ->
                    groupMemberDetails.forEach {
                        if (it.member.memberId == memberId) {
                            groupMemberDetails.remove(it)
                            return@let
                        }
                    }
                }
            }

            Constants.ListenerEmitType.Modify -> {
                it.singleResponse?.member?.memberId?.let { memberId ->
                    groupMemberDetails.forEach {details->
                        if (details.member.memberId == memberId) {
                             it.singleResponse.member.let { it1 ->
                                details.member= details.member.copy(
                                    isOnline = it1.isOnline
                                )
                            }
                            return@let
                        }
                    }
                }
            }
        }

        MyLogger.i(tagChat, msg = groupMemberDetails, isJson = true , jsonTitle = "Group Member Details updated !")

        return Resource.Success(groupMemberDetails)

    }

    fun getGroupInfo(chatRoomId: String) = viewModelScope.myLaunch {
        _groupInfo.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)){
            repository.getGroupInfo(chatRoomId).onEach {
                MyLogger.i(tagChat, msg = it, isJson = true , jsonTitle = "Group Profile Update response come !")
                if (it!=null){
                    _groupInfo.tryEmit(Resource.Success(it))
                }else{
                    _groupInfo.tryEmit(Resource.Error("Group Info Not Found !"))
                }
            }.launchIn(this)
        }else{
            _groupInfo.tryEmit(Resource.Error(Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }

     fun updateGroupMemberOnlineStatus(chatRoomId: String,status:Boolean) = viewModelScope.myLaunch {
         if (SoftwareManager.isNetworkAvailable(app)){
             repository.updateGroupMemberOnlineStatus(chatRoomId,status)
         }else{
             MyLogger.e(tagChat, msg = "Internet Off so that user status update failed!")
         }
     }

     fun listenNewMessage(chatRoomId: String) = viewModelScope.myLaunch {
         if (isNetworkAvailable(app)){
             repository.listenNewMessage(chatRoomId).onEach {
                    // Don't do anything here
             }.launchIn(this)
         }else{
             MyLogger.e(tagChat, msg = "Internet Off so that user status update failed!")
         }
     }

    fun deleteGroupMessage(
        message: GroupMessage,
        chatRoomId: String,
        users: List<String>,
        lastMessage: GroupLastMessage?,
        secondLastMessage: GroupMessage? = null
    ) = viewModelScope.myLaunch {
        _deleteGroupMessage.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.deleteMessage(
                message, chatRoomId, users, lastMessage, secondLastMessage
            ).onEach {
                MyLogger.i(
                    tagChat, msg = it, isJson = true, jsonTitle = "Delete Message Response Come"
                )
                if (it.isSuccess) {
                    _deleteGroupMessage.tryEmit(Resource.Success(it))
                } else {
                    _deleteGroupMessage.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        } else {
            _deleteGroupMessage.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    fun clearGroupChat(
        chatRoomId: String, users: List<String>,lastMessage: GroupLastMessage,
        secondLastMessage: GroupMessage
    ) = viewModelScope.myLaunch {
        _clearGroupChat.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.clearChats(
                chatRoomId, users ,lastMessage,secondLastMessage
            ).onEach {
                MyLogger.i(
                    tagChat, msg = it, isJson = true, jsonTitle = "Clear all message response come"
                )
                if (it.isSuccess) {
                    _clearGroupChat.tryEmit(Resource.Success(it))
                } else {
                    _clearGroupChat.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        } else {
            _clearGroupChat.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    fun updateGroupDetails( message: GroupMessage,
                            lastMessage: GroupLastMessage,
                            chatRoomId: String,
                            users: List<GroupMember>, // Just for add  message recent chat
                            groupInfo: GroupInfo? = null ,
                            deleteImage:String?=null ,
                            uploadingImage:String?=null) = viewModelScope.myLaunch {
                                MyLogger.d(tagChat, isFunctionCall = true)
                                _updateGroupDetails.tryEmit(Resource.Loading())
        if(isNetworkAvailable(app)){
            MyLogger.d(tagChat, msg = "Network Available so that api call !")
            repository.updateGroupInfo(message, lastMessage, chatRoomId, users, groupInfo, deleteImage , uploadingImage).onEach {
                MyLogger.d(tagChat, msg = it , isJson = true)
                if (it.isSuccess){
                    _updateGroupDetails.tryEmit(Resource.Success(it))
                }else{
                    _updateGroupDetails.tryEmit(Resource.Error(it.errorMessage))
                }
            }.launchIn(this)
        }else{
            _updateGroupDetails.tryEmit(Resource.Error("Internet Not Available !"))
        }
    }

     fun sentGroupInfoMessage(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        users: List<GroupMember>,// Just for add  message recent chat
        action: Constants.InfoType,
        addedOrRemovedUserId: String? = null,
        newMembers:List<String>?=null,
        groupInfo: GroupInfo? = null
    ) = viewModelScope.myLaunch {
             _sendGroupInfoMessage.tryEmit(Resource.Loading())
         if(isNetworkAvailable(app)){
             repository.sentGroupInfoMessage(message, lastMessage, chatRoomId, users,action,addedOrRemovedUserId,newMembers ,groupInfo).onEach {
                 MyLogger.i(
                     tagChat, msg = it, isJson = true, jsonTitle = "Message sent response come"
                 )
                 if (it.isSuccess) {
                     _sendGroupInfoMessage.tryEmit(Resource.Success(it))
                 } else {
                     _sendGroupInfoMessage.tryEmit(Resource.Error(it.errorMessage))
                 }
             }.launchIn(this)
         }else{
             _sendGroupInfoMessage.tryEmit(Resource.Error("Internet Not Available !"))
         }
     }

    //endregion


    //region :: Recent Chat

     fun deleteRecentChat(chatRoomId: String) = viewModelScope.myLaunch {
         _deleteRecentChat.tryEmit(Resource.Loading())
         if (isNetworkAvailable(app)){
             repository.deleteRecentChat(chatRoomId).onEach {
                 if (it.isSuccess){
                     _deleteRecentChat.tryEmit(Resource.Success(it))
                 }else{
                     _deleteRecentChat.tryEmit(Resource.Error(it.errorMessage))
                 }
             }.launchIn(this)
         }else{
             _deleteRecentChat.tryEmit(Resource.Error("Internet Not Available !"))
         }
     }

    //endregion

    fun setDataLoadedStatus(status: Boolean) {
        _isDataLoaded = status
    }

    fun setImageUriData(uri: Uri?) {
        _imageUri = uri
    }

    fun setVideoUriData(uri: Uri?) {
        _videoUri = uri
    }

    override fun onCleared() {
        MyLogger.e(tagChat , msg = "Chat View Model destroyed with hashcode :-> ${this.hashCode()}")
        super.onCleared()
    }
}