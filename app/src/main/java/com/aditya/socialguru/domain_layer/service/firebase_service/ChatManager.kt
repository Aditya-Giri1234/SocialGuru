package com.aditya.socialguru.domain_layer.service.firebase_service

import com.aditya.socialguru.data_layer.model.chat.LastMessage
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.data_layer.model.chat.RecentChat
import com.aditya.socialguru.data_layer.model.chat.UserRecentModel
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.NotificationType
import com.aditya.socialguru.domain_layer.helper.Constants.Table
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.helper.toNormalMap
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.NotificationSendingManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlin.system.measureTimeMillis

/**
 * [ChatManager] This class only do crud  operation on firebase firestore chat collection.
 * */
object ChatManager {

    private val tagChat = Constants.LogTag.Chats


    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val userRef by lazy {
        firestore.collection(Constants.Table.User.name)
    }

    private val chatRef by lazy {
        firestore.collection(Constants.Table.Chats.name)
    }

    private var chatMessageListener: ListenerRegistration? = null
    private var isFirstTimeChatMessageListener = true

    private var lastMessageListener: ListenerRegistration? = null

    private var recentChatListener: ListenerRegistration? = null
    private var isFirstTimeRecentChatListener = true

    suspend fun sentMessage(
        message: Message,
        lastMessage: LastMessage,
        chatRoomId: String,
        isUserOnline: Boolean = false
    ) =
        callbackFlow<UpdateResponse> {


            val timeStamp = System.currentTimeMillis()
            val notificationId = Helper.getNotificationId()
            val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
            val isMessageContainText = message.text != null
            val type =
                if (isMessageContainText) NotificationType.TextChat.name else NotificationType.MediaChat.name
            val lastMessageType =
                if (isMessageContainText) Constants.LastMessageType.Text.type else Constants.LastMessageType.Media.type
            val notificationData = NotificationData(
                notificationId = notificationId,
                friendOrFollowerId = AuthManager.currentUserId()!!,
                notificationTimeInText = timeInText,
                notificationTimeInTimeStamp = timeStamp.toString(),
                type = type,
                chatRoomId = chatRoomId,
                messageId = message.messageId
            )
            //For My Recent Chat
            val forSenderRecentData = RecentChat(
                chatRoomId = chatRoomId,
                lastMessageTimeInTimeStamp = timeStamp,
                lastMessageTimeInText = timeInText,
                message = message.text,
                lastMessageType = lastMessageType,
                senderId = AuthManager.currentUserId()!!,
                receiverId = message.receiverId,
                userId = message.receiverId,
                lastMessageSeen = Constants.SeenStatus.Sending.status
            )

            //For Receiver Recent Chat
            var forReceiverRecentData = RecentChat(
                chatRoomId = chatRoomId,
                lastMessageTimeInTimeStamp = timeStamp,
                lastMessageTimeInText = timeInText,
                message = message.text,
                lastMessageType = lastMessageType,
                senderId = AuthManager.currentUserId()!!,
                receiverId = message.receiverId,
                userId = message.senderId,
                lastMessageSeen = Constants.SeenStatus.Sending.status
            )


            val lastMessageRef = chatRef.document(chatRoomId)

            val messageRef = chatRef.document(chatRoomId).collection(Table.Messages.name)
                .document(message.messageId!!)

            val isLastMessageExist = lastMessageRef.get().await().exists()
            val recentMessageRefForReceiver =
                userRef.document(message.receiverId!!).collection(Table.RecentChat.name)
                    .document(chatRoomId)
            val recentMessageRefForSender =
                userRef.document(AuthManager.currentUserId()!!).collection(Table.RecentChat.name)
                    .document(chatRoomId)


            val recentChatData = recentMessageRefForReceiver.get().await().toObject<RecentChat>()
            if (recentChatData == null) {
                //Document doesn't exist
                // Do nothing
            } else {
                //Document exist
                val unSeenMessageCount = recentChatData.unSeenMessageCount?.plus(1) ?: 0
                forReceiverRecentData = forReceiverRecentData.copy(
                    unSeenMessageCount = unSeenMessageCount
                )
            }

            //In transaction all read come first then after all write come
            // Firebase firestore transaction is much slower then batch write because transaction run sequence

            firestore.runBatch {
                val timeTakingToCalculate = measureTimeMillis {

                    if (isLastMessageExist) {
                        it.update(
                            lastMessageRef,
                            lastMessage.toNormalMap().filterValues { it != null })
                    } else {
                        it.set(lastMessageRef, lastMessage)
                    }
                    it.set(recentMessageRefForReceiver, forReceiverRecentData)
                    it.set(recentMessageRefForSender, forSenderRecentData)
                    it.set(messageRef, message)
                }
                MyLogger.v(tagChat, msg = "Time taken to calculate $timeTakingToCalculate")


            }.addOnSuccessListener {
                updateSeenStatus(
                    Constants.SeenStatus.Send.status,
                    message.messageId,
                    chatRoomId,
                    message.receiverId
                )

                if (!isUserOnline) {
                    NotificationSendingManager.sendNotification(
                        message.receiverId,
                        notificationData
                    )
                }

                trySend(UpdateResponse(true, ""))

            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))

            }.await()


            awaitClose {
                close()
            }
        }

    suspend fun deleteMessage(message: Message,chatRoomId:String,userId:String  , lastMessage: LastMessage?, secondLastMessage:Message?=null) = callbackFlow<UpdateResponse> {

        if (secondLastMessage!=null){
            // need to update last message , recent chat
            //For My Recent Chat
            val timeStamp=secondLastMessage.messageSentTimeInTimeStamp
            val timeInText=secondLastMessage.messageSendTimeInText
            val lastMessageType=secondLastMessage.messageType
            val forMyRecentData = RecentChat(
                chatRoomId = chatRoomId,
                lastMessageTimeInTimeStamp = timeStamp,
                lastMessageTimeInText = timeInText,
                message = secondLastMessage.text,
                lastMessageType = lastMessageType,
                senderId = secondLastMessage.senderId,
                receiverId = secondLastMessage.receiverId,
                userId = userId,
                lastMessageSeen = secondLastMessage.seenStatus
            )

            //For Receiver Recent Chat
            val forReceiverRecentData = RecentChat(
                chatRoomId = chatRoomId,
                lastMessageTimeInTimeStamp = timeStamp,
                lastMessageTimeInText = timeInText,
                message = secondLastMessage.text,
                lastMessageType = lastMessageType,
                senderId =secondLastMessage.senderId,
                receiverId = secondLastMessage.receiverId,
                userId = AuthManager.currentUserId()!!,
                lastMessageSeen = secondLastMessage.seenStatus
            )

            val messageRef= chatRef.document(chatRoomId).collection(Table.Messages.name).document(message.messageId!!)
            val lastMessageRef= chatRef.document(chatRoomId)
            val receiverRecentRef= userRef.document(userId).collection(Table.RecentChat.name).document(chatRoomId)
            val myRecentChatRef= userRef.document(AuthManager.currentUserId()!!).collection(Table.RecentChat.name).document(chatRoomId)

            firestore.runBatch { batch->
                batch.delete(messageRef)
                batch.update(lastMessageRef ,lastMessage!!.toNormalMap().filterValues { it!=null })
                batch.update(receiverRecentRef,forReceiverRecentData.toNormalMap())
                batch.update(myRecentChatRef,forMyRecentData.toNormalMap())
            }.addOnSuccessListener {
                trySend(UpdateResponse(true,""))
            }.addOnFailureListener {
                trySend(UpdateResponse(false,it.message))
            }.await()


        }else{
            // just delete message no need to update any thing
            chatRef.document(chatRoomId).collection(Table.Messages.name).document(message.messageId!!).delete().addOnSuccessListener {
                trySend(UpdateResponse(true,""))
            }.addOnFailureListener {
                trySend(UpdateResponse(false,it.message))
            }.await()
        }

        awaitClose {
            close()
        }
    }

    fun updateSeenStatus(
        status: String,
        messageId: String,
        chatRoomId: String,
        receiverId: String
    ) {
        launchCoroutineInIOThread {
            val messageRef =
                chatRef.document(chatRoomId).collection(Table.Messages.name).document(messageId)
            val myRecentChatRef =
                userRef.document(AuthManager.currentUserId()!!).collection(Table.RecentChat.name)
                    .document(chatRoomId)
            val receiverRecentChatRef =
                userRef.document(receiverId).collection(Table.RecentChat.name)
                    .document(chatRoomId)
            firestore.runBatch {
                it.update(messageRef, Constants.MessageTable.SEEN_STATUS.fieldName, status)
                it.update(
                    myRecentChatRef,
                    Constants.RecentChatTable.LAST_MESSAGE_SEEN.fieldName,
                    status
                )
                it.update(
                    receiverRecentChatRef,
                    Constants.RecentChatTable.LAST_MESSAGE_SEEN.fieldName,
                    status
                )
                if (status == Constants.SeenStatus.MessageSeen.status) {
                    it.update(
                        myRecentChatRef,
                        Constants.RecentChatTable.UNSEEN_MESSAGE_COUNT.fieldName,
                        0
                    )
                    it.update(
                        receiverRecentChatRef,
                        Constants.RecentChatTable.UNSEEN_MESSAGE_COUNT.fieldName,
                        0
                    )
                }
            }.await()
        }
    }

    suspend fun getChatMessageAndListen(chatRoomId: String) =
        callbackFlow<ListenerEmissionType<Message, Message>> {
            val messageRef = chatRef.document(chatRoomId).collection(Table.Messages.name)

            val messageList =
                messageRef.get().await().toObjects(Message::class.java).toMutableList()

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting,
                    responseList = messageList.toList()
                )
            )

            messageList.clear()
            chatMessageListener?.remove()
            chatMessageListener = messageRef.addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (isFirstTimeChatMessageListener) {
                    isFirstTimeChatMessageListener = false
                    return@addSnapshotListener
                }
                value?.documentChanges?.forEach {
                    when (it.type) {
                        DocumentChange.Type.ADDED -> {
                            trySend(
                                ListenerEmissionType(
                                    Constants.ListenerEmitType.Added,
                                    singleResponse = it.document.toObject<Message>()
                                )
                            )
                        }

                        DocumentChange.Type.MODIFIED -> {
                            trySend(
                                ListenerEmissionType(
                                    Constants.ListenerEmitType.Modify,
                                    singleResponse = it.document.toObject<Message>()
                                )
                            )
                        }

                        DocumentChange.Type.REMOVED -> {
                            trySend(
                                ListenerEmissionType(
                                    Constants.ListenerEmitType.Removed,
                                    singleResponse = it.document.toObject<Message>()
                                )
                            )
                        }
                    }
                }
            }

            awaitClose {
                chatMessageListener?.remove()
                isFirstTimeChatMessageListener = true
                close()
            }
        }


    suspend fun listenLastMessage(chatRoomId: String) = callbackFlow<LastMessage> {
        lastMessageListener?.remove()
        lastMessageListener = chatRef.document(chatRoomId).addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener

            value?.let {
                it.toObject<LastMessage>()?.let { it1 ->
                    MyLogger.v(tagChat, msg = it1)
                    trySend(it1)
                }
            }

        }

        awaitClose {
            lastMessageListener?.remove()
            close()
        }
    }

    suspend fun updateUserAvailabilityForChatRoom(
        chatRoomId: String,
        isIAmUser1: Boolean,
        status: Boolean
    ) {
        val lastMessageRef = chatRef.document(chatRoomId)
        if (isIAmUser1) {
            lastMessageRef.update(Constants.LastMessageTable.IS_USER_1_ONLINE.fieldName, status)
        } else {
            lastMessageRef.update(Constants.LastMessageTable.IS_USER_2_ONLINE.fieldName, status)
        }
    }

    suspend fun getMessageById(chatRoomId: String, messageId: String) = flow<Message> {
        chatRef.document(chatRoomId).collection(Table.Messages.name).document(messageId).get()
            .await().toObject<Message>()
            ?.let { emit(it) }
    }


    suspend fun getRecentChatAndListen() =
        callbackFlow<ListenerEmissionType<UserRecentModel, UserRecentModel>> {
            val recentChatRef =
                userRef.document(AuthManager.currentUserId()!!).collection(Table.RecentChat.name)
            val recentList =
                recentChatRef.get().await().toObjects(RecentChat::class.java).toMutableList()
            val userRecentModel = recentList.mapNotNull { recentChat ->
                recentChat.userId?.let {
                    UserManager.getUserById(it)?.let {
                        UserRecentModel(it, recentChat)
                    }
                }
            }
            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting,
                    responseList = userRecentModel.toList()
                )
            )
            recentList.clear()
            recentChatListener?.remove()
            recentChatListener = recentChatRef.addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                if (isFirstTimeRecentChatListener) {
                    isFirstTimeRecentChatListener = false
                    return@addSnapshotListener
                }
                value?.documentChanges?.forEach {
                    when (it.type) {
                        DocumentChange.Type.ADDED -> {
                            launchCoroutineInIOThread {
                                val recentChat = it.document.toObject<RecentChat>()
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Added,
                                        singleResponse = recentChat.userId?.let {
                                            UserManager.getUserById(it)?.let { user ->
                                                UserRecentModel(user, recentChat)
                                            }
                                        }
                                    )
                                )
                            }
                        }

                        DocumentChange.Type.MODIFIED -> {
                            launchCoroutineInIOThread {
                                val recentChat = it.document.toObject<RecentChat>()
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Modify,
                                        singleResponse = recentChat.userId?.let {
                                            UserManager.getUserById(it)?.let { user ->
                                                UserRecentModel(user, recentChat)
                                            }
                                        }
                                    )
                                )
                            }

                        }

                        DocumentChange.Type.REMOVED -> {
                            launchCoroutineInIOThread {
                                val recentChat = it.document.toObject<RecentChat>()
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Removed,
                                        singleResponse = recentChat.userId?.let {
                                            UserManager.getUserById(it)?.let { user ->
                                                UserRecentModel(user, recentChat)
                                            }
                                        }
                                    )
                                )
                            }

                        }

                    }
                }
            }


            awaitClose {
                recentChatListener?.remove()
                isFirstTimeRecentChatListener = true
                close()
            }
        }

}
private typealias MessageType = Constants.PostType
