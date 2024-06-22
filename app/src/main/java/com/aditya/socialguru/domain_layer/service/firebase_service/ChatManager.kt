package com.aditya.socialguru.domain_layer.service.firebase_service

import com.aditya.socialguru.data_layer.model.chat.LastMessage
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.NotificationType
import com.aditya.socialguru.domain_layer.helper.Constants.LastMessageTable
import com.aditya.socialguru.domain_layer.helper.Constants.Table
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.helper.toNormalMap
import com.aditya.socialguru.domain_layer.manager.NotificationSendingManager
import com.google.firebase.database.snapshot.BooleanNode
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

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

    suspend fun sentMessage(message: Message, chatRoomId: String, isUserOnline: Boolean = false) =
        callbackFlow<UpdateResponse> {

            val timeStamp = System.currentTimeMillis()
            val notificationId = Helper.getNotificationId()
            val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
            val isMessageContainText = message.text != null
            val type =
                if (isMessageContainText) NotificationType.TextChat.name else NotificationType.MediaChat.name
            val notificationData = NotificationData(
                notificationId = notificationId,
                friendOrFollowerId = AuthManager.currentUserId()!!,
                notificationTimeInText = timeInText,
                notificationTimeInTimeStamp = timeStamp.toString(),
                type = type,
                chatRoomId = chatRoomId,
                messageId = message.messageId
            )
            val lastMessage = LastMessage(
                senderId = message.senderId,
                receiverId = message.receiverId,
                messageType = message.messageType,
                chatType = message.chatType,
                message = message.text,
                lastMessageSentTimeInTimeStamp = message.messageSentTimeInTimeStamp,
                lastMessageSentTimeInText = message.messageSendTimeInText,
                unSeenMessageCount = 0
            )

            val lastMessageRef = chatRef.document(chatRoomId)
            val notificationRef =
                userRef.document(message.receiverId!!).collection(Table.Notification.name)
                    .document(notificationId)
            val messageRef = chatRef.document(chatRoomId).collection(Table.Messages.name)
                .document(message.messageId!!)

            val isLastMessageExist = lastMessageRef.get().await().exists()

            firestore.runBatch {
                if (isLastMessageExist) {
                    it.update(lastMessageRef, lastMessage.toNormalMap().filterValues { it != null })
                } else {
                    it.set(lastMessageRef, lastMessage)
                }
                it.set(messageRef, message)
                it.set(notificationRef, notificationData)

            }.addOnSuccessListener {
                updateSeenStatus(Constants.SeenStatus.Send.status, message.messageId, chatRoomId)

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

    fun updateSeenStatus(status: String, messageId: String, chatRoomId: String) {
        launchCoroutineInIOThread {
            chatRef.document(chatRoomId).collection(Table.Messages.name).document(messageId)
                .update(Constants.MessageTable.SEEN_STATUS.fieldName, status).await()
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
                it.toObject<LastMessage>()?.let { it1 -> trySend(it1) }
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


}
private typealias MessageType = Constants.PostType
