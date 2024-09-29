package com.aditya.socialguru.ui_layer.broadcaster

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.LastMessage
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.data_layer.model.chat.UpdateChatResponse
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo
import com.aditya.socialguru.data_layer.model.chat.group.GroupLastMessage
import com.aditya.socialguru.data_layer.model.chat.group.GroupMember
import com.aditya.socialguru.data_layer.model.chat.group.GroupMessage
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.IntentTable
import com.aditya.socialguru.domain_layer.helper.Constants.NotificationRemoteInput
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.manager.MyNotificationManager
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.domain_layer.service.firebase_service.ChatManager
import com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.single_chat.ChatType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach

class NotificationEventListener : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            intent?.let {
                launchCoroutineInIOThread {
                    handleNotificationEvent(context, intent)
                }
            }
        }
    }

    private suspend fun handleNotificationEvent(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationRemoteInput.SingleChatReply.name -> {
                handleSingleChatNotificationEvent(context, intent)
            }

            NotificationRemoteInput.GroupChatReply.name -> {
                handleGroupChatNotificationEvent(context, intent)
            }
        }
    }

    private suspend fun handleGroupChatNotificationEvent(context: Context, intent: Intent) {
        context.apply {
            val message = RemoteInput.getResultsFromIntent(intent)
                ?.getCharSequence(IntentTable.ReplyMessage.name).toString()
            val chatRoomId = intent.getStringExtra(IntentTable.ChatRoomId.name) ?: return
            val myId = AuthManager.currentUserId()!!
            val user = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(IntentTable.UserData.name, User::class.java)
            } else {
                intent.getSerializableExtra(IntentTable.UserData.name) as? User
            }

            val groupInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(IntentTable.GroupInfo.name, GroupInfo::class.java)
            } else {
                intent.getSerializableExtra(IntentTable.GroupInfo.name) as? GroupInfo
            }

            val groupMembers =
                ChatManager.getGroupMemberInfo(chatRoomId).first().responseList ?: return

            if (message.isEmpty()) return
            if (user == null) return
            if (groupInfo == null) return
            if (SoftwareManager.isNetworkAvailable(this)) {
                FirebaseManager.updateSeenStatusForGroupChatForNotification(chatRoomId) // Update seen status

                sendGroupMessage(
                    message,
                    myId,
                    chatRoomId,
                    user.userName!!,
                    groupMembers.map { it.member }).collect{response->
                    when {
                        response.isSuccess -> {
                            MyNotificationManager.showGroupChatSendNotification(
                                message = "✅ Message sent successfully!",
                                chatRoomId = chatRoomId,
                                groupInfo =  groupInfo,
                                context = this@apply
                            )
                        }

                        response.errorMessage != null -> {
                            MyNotificationManager.showGroupChatSendNotification(
                                message = "❌ Error: ${response.errorMessage}. Please try again.",
                                chatRoomId = chatRoomId,
                                groupInfo =  groupInfo,
                                context = this@apply
                            )
                        }

                        else -> {
                            MyNotificationManager.showGroupChatSendNotification(
                                message = "❌ Unable to send the message. Please try again later.",
                                chatRoomId = chatRoomId,
                                groupInfo =  groupInfo,
                                context = this@apply
                            )
                        }
                    }
                }
            } else {
                MyNotificationManager.showGroupChatSendNotification(
                    message = "🚫 No internet connection. Message not sent.",
                    chatRoomId = chatRoomId,
                    groupInfo =  groupInfo,
                    context = this@apply
                )
            }

        }
    }

    private suspend fun handleSingleChatNotificationEvent(context: Context, intent: Intent) {
        context.apply {
            val message = RemoteInput.getResultsFromIntent(intent)
                ?.getCharSequence(IntentTable.ReplyMessage.name).toString()
            val senderId =
                intent.getStringExtra(IntentTable.SenderId.name) // who send this notification
            val chatRoomId = intent.getStringExtra(IntentTable.ChatRoomId.name)
            val myId = AuthManager.currentUserId()!!
            val lastMessage = FirebaseManager.listenLastMessage(chatRoomId!!).first()
            val user = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(IntentTable.UserData.name, User::class.java)
            } else {
                intent.getSerializableExtra(IntentTable.UserData.name) as? User
            }
            if (message.isEmpty()) return
            if (senderId == null) return
            if (user == null) return
            if (SoftwareManager.isNetworkAvailable(this)) {
                FirebaseManager.getChatMessageAndUpdateSeenStatusForNotification(chatRoomId) // Update seen status
                val response = sendMessage(user, lastMessage, message, myId, senderId, chatRoomId)

                when {
                    response.isSuccess -> {
                        MyNotificationManager.showSingleChatSendMessage(
                            user = user,
                            chatRoomId = chatRoomId,
                            receiverId = senderId,
                            message = "✅ Message sent successfully!",
                            context = this@apply
                        )
                    }

                    response.errorMessage != null -> {
                        MyNotificationManager.showSingleChatSendMessage(
                            user = user,
                            chatRoomId = chatRoomId,
                            receiverId = senderId,
                            message = "❌ Error: ${response.errorMessage}. Please try again.",
                            context = this@apply
                        )
                    }

                    else -> {
                        MyNotificationManager.showSingleChatSendMessage(
                            user = user,
                            chatRoomId = chatRoomId,
                            receiverId = senderId,
                            message = "❌ Unable to send the message. Please try again later.",
                            context = this@apply
                        )
                    }
                }
            } else {
                MyNotificationManager.showSingleChatSendMessage(
                    user = user,
                    chatRoomId = chatRoomId,
                    receiverId = senderId,
                    message = "🚫 No internet connection. Message not sent.",
                    context = this@apply
                )
            }

        }
    }

    //region:: For Single Chat
    private suspend fun sendMessage(
        user: User,
        prevLastMessage: LastMessage,
        message: String,
        myId: String,
        receiverId: String,
        chatRoomId: String
    ): UpdateChatResponse {

        val chatType = ChatType.OnlyText.name
        val timeStamp = System.currentTimeMillis()
        val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
        val isUserOnCurrentChatScreen = isUserCurrentChatScreen(prevLastMessage, myId, receiverId)
        val chatData = Message(
            messageId = Helper.getMessageId(),
            messageType = Constants.MessageType.Chat.type,
            chatType = chatType,
            text = message,
            senderId = myId,
            receiverId = receiverId,
            messageSentTimeInTimeStamp = timeStamp,
            messageSendTimeInText = timeInText,
            seenStatus = Constants.SeenStatus.Sending.status
        )
        val lastMessage = LastMessage(
            senderId = myId,
            receiverId = receiverId,
            messageType = Constants.MessageType.Chat.type,
            chatType = chatType,
            message = message,
            lastMessageSentTimeInTimeStamp = timeStamp,
            lastMessageSentTimeInText = timeInText,
            isUser1Online = findUserAvailability(true, myId, receiverId, isUserOnCurrentChatScreen),
            isUser2Online = findUserAvailability(
                false,
                myId,
                receiverId,
                isUserOnCurrentChatScreen
            ),
        )
        return FirebaseManager.sentMessage(
            chatData,
            lastMessage,
            chatRoomId,
            (user.userAvailable ?: false) && isUserOnCurrentChatScreen
        ).first()

    }

    private fun isUserCurrentChatScreen(
        lastMessage: LastMessage,
        myId: String,
        receiverId: String
    ): Boolean {
        return if (isIAmUser1(myId, receiverId)) {
            lastMessage.isUser2Online ?: false
        } else {
            lastMessage.isUser1Online ?: false
        }
    }

    private fun findUserAvailability(
        forUser1: Boolean,
        myId: String,
        receiverId: String,
        isUserActiveOnCurrentChat: Boolean
    ): Boolean {
        val isUser1Me = isIAmUser1(myId, receiverId)
        return if (forUser1) {
            if (isUser1Me) {
                false
            } else {
                isUserActiveOnCurrentChat
            }
        } else {
            if (isUser1Me) {
                isUserActiveOnCurrentChat
            } else {
                false
            }
        }
    }

    private fun isIAmUser1(myId: String, receiverId: String): Boolean {
        val list = listOf(myId, receiverId).sorted()
        return list[0] == myId
    }

    //endregion

    //region:: For Group Chat

    private suspend fun sendGroupMessage(
        message: String,
        myId: String,
        chatRoomId: String,
        myName: String,
        groupMembers: List<GroupMember>
    ): Flow<UpdateChatResponse> {

        val onlyGroupMembers = groupMembers.mapNotNull { it.memberId }.filter { it != myId }
        val chatType = ChatType.OnlyText.name
        val timeStamp = System.currentTimeMillis()
        val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
        val chatData = GroupMessage(
            messageId = Helper.getMessageId(),
            messageType = Constants.MessageType.Chat.type,
            chatType = chatType,
            text = message,
            senderId = myId,
            messageSentTimeInTimeStamp = timeStamp,
            messageSendTimeInText = timeInText,
            seenStatus = Constants.SeenStatus.Sending.status,
            sendTimeUsers = onlyGroupMembers,
            senderUserName = myName
        )
        val lastMessage = GroupLastMessage(
            senderId = myId,
            messageType = Constants.MessageType.Chat.type,
            chatType = chatType,
            message = message,
            lastMessageSentTimeInTimeStamp = timeStamp,
            lastMessageSentTimeInText = timeInText,
        )
        return FirebaseManager.sentGroupMessage(
            chatData,
            lastMessage,
            chatRoomId,
            groupMembers
        )
    }

    //endregion

}