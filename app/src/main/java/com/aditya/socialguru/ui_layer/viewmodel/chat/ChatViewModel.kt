package com.aditya.socialguru.ui_layer.viewmodel.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.chat.ChatRepo
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class ChatViewModel(val app: Application) : AndroidViewModel(app) {
    private val tagChat = Constants.LogTag.Chats

    private val repository = ChatRepo()

    private var _isDataLoaded = false

    val isDataLoaded get() = _isDataLoaded

    private val _friendList =
        MutableSharedFlow<Resource<List<FriendCircleData>>>(
            1, 64, BufferOverflow.DROP_OLDEST
        )
    val friendList get() = _friendList.asSharedFlow()

    private val _userDetails = MutableSharedFlow<Resource<User>>(
        1, 64, BufferOverflow.DROP_OLDEST
    )
    val userDetails get() = _userDetails.asSharedFlow()

    private val _sendMessage =
        MutableSharedFlow<Resource<UpdateResponse>>(0, 64, BufferOverflow.DROP_OLDEST)
    val sendMessage get() = _sendMessage.asSharedFlow()

    private val _chatMessage =
        MutableSharedFlow<Resource<List<Message>>>(1, 64, BufferOverflow.DROP_OLDEST)
    val chatMessage get() = _chatMessage.asSharedFlow()

    //region:: Friend Operation
    fun getFriendListAndListenChange() = viewModelScope.myLaunch {
        _friendList.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getFriendList().onEach {
                _friendList.tryEmit(handleFriendResponse(it))
            }.launchIn(this)

        } else {
            _friendList.tryEmit(Resource.Error("No Internet Available !"))
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
                    msg = listenerHandling.responseList, isJson = true,
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
                    msg = listenerHandling.singleResponse, isJson = true,
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
            _userDetails.tryEmit(Resource.Error("No Internet Available !"))
        }
    }
    //endregion

    //region:: Send Message

    fun sendMessage(message: Message, charRoomId: String, isUserOnline: Boolean) =
        viewModelScope.myLaunch {
            _sendMessage.tryEmit(Resource.Loading())

            if (SoftwareManager.isNetworkAvailable(app)) {
                repository.sendMessage(message, charRoomId, isUserOnline).onEach {
                    MyLogger.i(
                        tagChat,
                        msg = it,
                        isJson = true,
                        jsonTitle = "Message sent response come"
                    )
                    if (it.isSuccess) {
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
                MyLogger.v(tagChat, msg = it, isJson = true, jsonTitle = "Current Chat Message Updated ")
                _chatMessage.tryEmit(handleChatMessageResponse(it))
            }.launchIn(this)
        } else {
            _chatMessage.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    private fun handleChatMessageResponse(it: ListenerEmissionType<Message, Message>): Resource<List<Message>> {

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
                }
            }

            Constants.ListenerEmitType.Removed -> {
                it.singleResponse?.messageId?.let {removedMessageId->
                    chatMessageList.forEach {
                        val messageId=it.messageId
                        if (messageId != null&&messageId==removedMessageId) {
                            chatMessageList.remove(it)
                            chatMessageList.sortBy { it.messageSentTimeInTimeStamp }
                            return@let
                        }
                    }
                }
            }

            Constants.ListenerEmitType.Modify -> {
                it.singleResponse?.let { modifiedMessage ->
                    val existingMessage = chatMessageList.find { it.messageId == modifiedMessage.messageId }
                    existingMessage?.apply {
                        seenStatus = modifiedMessage.seenStatus
                        // Ensure the list remains sorted by message timestamp
                        chatMessageList.sortBy { message -> message.messageSentTimeInTimeStamp }
                    }
                }
            }
        }

        return Resource.Success(addDateHeaders(chatMessageList))

    }
    private fun addDateHeaders(messages: List<Message>): List<Message> {
        if (messages.isEmpty()) return messages

        val messagesWithHeaders = mutableListOf<Message>()
        var lastDateHeader: String? = null

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

        messages.forEach { message ->
            val messageDate = message.messageSentTimeInTimeStamp?.let {
                Calendar.getInstance().apply { timeInMillis = it }
            }
            val currentDateHeader = when {
                message.messageType == Constants.MessageType.DateHeader.type -> {
                    lastDateHeader = message.text // Update lastDateHeader for date headers
                    message.text // Return current date header text as-is
                }
                messageDate == null -> null
                isSameDay(messageDate, today) -> "Today"
                isSameDay(messageDate, yesterday) -> "Yesterday"
                else -> dateFormatter.format(messageDate.time)
            }

            // Add new date header if currentDateHeader is different from lastDateHeader
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
    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    //endregion

    fun setDataLoadedStatus(status: Boolean) {
        _isDataLoaded = status
    }
}