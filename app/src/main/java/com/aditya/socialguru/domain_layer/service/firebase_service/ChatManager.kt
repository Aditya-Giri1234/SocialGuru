package com.aditya.socialguru.domain_layer.service.firebase_service

import com.aditya.socialguru.data_layer.model.chat.ChatMediaData
import com.aditya.socialguru.data_layer.model.chat.ChatMuteNotificationData
import com.aditya.socialguru.data_layer.model.chat.LastMessage
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.data_layer.model.chat.RecentChat
import com.aditya.socialguru.data_layer.model.chat.UpdateChatResponse
import com.aditya.socialguru.data_layer.model.chat.UserRecentModel
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo
import com.aditya.socialguru.data_layer.model.chat.group.GroupLastMessage
import com.aditya.socialguru.data_layer.model.chat.group.GroupMember
import com.aditya.socialguru.data_layer.model.chat.group.GroupMemberDetails
import com.aditya.socialguru.data_layer.model.chat.group.GroupMessage
import com.aditya.socialguru.data_layer.model.chat.group.UnSeenMessageModel
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.InfoType
import com.aditya.socialguru.domain_layer.helper.Constants.NotificationType
import com.aditya.socialguru.domain_layer.helper.Constants.Table
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.helper.isExist
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.helper.safeUpdate
import com.aditya.socialguru.domain_layer.helper.toNormalMap
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.NotificationSendingManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

/**
 * [ChatManager] This class only do crud  operation on firebase firestore chat collection.
 * */
object ChatManager {

    private val tagChat = Constants.LogTag.Chats


    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val firebaseStorage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
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

    private var groupMemberListener: ListenerRegistration? = null
    private var isFirstTimeGroupMemberListener = true

    private var listenNewMessageListener: ListenerRegistration? = null

    suspend fun sentMessage(
        message: Message,
        lastMessage: LastMessage,
        chatRoomId: String,
        isUserOnline: Boolean = false,
    ) = callbackFlow<UpdateChatResponse> {

        try {


            val isImagePresent = message.imageUri != null
            val isVideoPresent = message.videoUri != null

            when {
                isVideoPresent -> {
                    uploadVideo(
                        message, lastMessage, chatRoomId, isUserOnline, isImagePresent
                    ).onEach {
                        trySend(it)
                    }.launchIn(this)
                }

                isImagePresent -> {
                    uploadImage(message, lastMessage, chatRoomId, isUserOnline).onEach {
                        trySend(it)
                    }.launchIn(this)
                }

                else -> {
                    saveMessageToDatabase(message, lastMessage, chatRoomId, isUserOnline).onEach {
                        trySend(it)
                    }.launchIn(this)
                }
            }
        } catch (e: FirebaseFirestoreException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        awaitClose {
            close()
        }
    }


    private suspend fun uploadVideo(
        message: Message,
        lastMessage: LastMessage,
        chatRoomId: String,
        isUserOnline: Boolean = false,
        isImagePresent: Boolean
    ) = callbackFlow<UpdateChatResponse> {
        trySend(UpdateChatResponse(true, "Video  Uploading "))
        val videoUri = message.videoUri!!.convertParseUri()
        StorageManager.uploadVideoToServer(
            Constants.Table.Chats.name,
            "$chatRoomId/${Constants.FolderName.ChatVideo.name}",
            videoUri
        ).collect {
            when (it.state) {
                Constants.StorageManagerState.InProgress -> {
                    trySend(UpdateChatResponse(true, "Video Uploading ", progress = it.progress))
                }

                Constants.StorageManagerState.Error -> {
                    trySend(UpdateChatResponse(isSuccess = false, errorMessage = it.error))
                }

                Constants.StorageManagerState.UrlNotGet -> {
                    trySend(UpdateChatResponse(isSuccess = false, errorMessage = it.error))
                }

                Constants.StorageManagerState.Success -> {
                    trySend(UpdateChatResponse(true, "Video Uploaded"))
                    val onlineVideoUri = it.url
                    val updatedMessage = message.copy(
                        videoUri = onlineVideoUri
                    )
                    if (isImagePresent) {
                        uploadImage(
                            updatedMessage,
                            lastMessage,
                            chatRoomId,
                            isUserOnline
                        ).onEach {
                            trySend(it)
                        }.launchIn(this)
                    } else {
                        saveMessageToDatabase(
                            updatedMessage, lastMessage, chatRoomId, isUserOnline
                        ).onEach {
                            trySend(it)
                        }.launchIn(this)
                    }
                }
            }
        }
    }

    private suspend fun uploadImage(
        message: Message,
        lastMessage: LastMessage,
        chatRoomId: String,
        isUserOnline: Boolean = false,
    ) = callbackFlow<UpdateChatResponse> {
        trySend(UpdateChatResponse(true, "Image Uploading"))
        val imageUri = message.imageUri!!.convertParseUri()
        StorageManager.uploadImageToServer(
            Constants.Table.Chats.name,
            "$chatRoomId/${Constants.FolderName.ChatImage.name}",
            imageUri
        ).collect {
            when (it.state) {
                Constants.StorageManagerState.InProgress -> {
                    trySend(UpdateChatResponse(true, "Image Uploading", progress = it.progress))
                }

                Constants.StorageManagerState.Error -> {
                    trySend(UpdateChatResponse(isSuccess = false, errorMessage = it.error))
                }

                Constants.StorageManagerState.UrlNotGet -> {
                    trySend(UpdateChatResponse(isSuccess = false, errorMessage = it.error))
                }

                Constants.StorageManagerState.Success -> {
                    trySend(UpdateChatResponse(true, "Image Uploaded"))
                    val onlineImageUri = it.url
                    val updatedMessage = message.copy(
                        imageUri = onlineImageUri
                    )

                    saveMessageToDatabase(
                        updatedMessage, lastMessage, chatRoomId, isUserOnline
                    ).onEach {
                        trySend(it)
                    }.launchIn(this)


                }
            }
        }
    }

    private suspend fun saveMessageToDatabase(
        message: Message,
        lastMessage: LastMessage,
        chatRoomId: String,
        isUserOnline: Boolean = false
    ) = callbackFlow<UpdateChatResponse> {
        val isImagePresent = message.imageUri != null
        val isVideoPresent = message.videoUri != null
        val timeStamp = System.currentTimeMillis()
        val notificationId = Helper.getNotificationId()
        val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
        val isMessageContainText = !message.text.isNullOrBlank()
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

        // This is updated message
        val updatedMessage = message.copy(
            messageSentTimeInTimeStamp = timeStamp, messageSendTimeInText = timeInText
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

        //For Media Reference
        val imageMediaId = Helper.getImageMediaId(timeStamp)
        val videoMediaId = Helper.getVideoMediaId(timeStamp)
        val imageMediaData = ChatMediaData(
            mediaId = imageMediaId,
            mediaUri = message.imageUri,
            mediaUploadingTimeInTimeStamp = timeStamp,
            mediaUploadingTimeInText = timeInText,
            isImage = true

        )
        val videoMediaData = ChatMediaData(
            mediaId = videoMediaId,
            mediaUri = message.videoUri,
            mediaUploadingTimeInTimeStamp = timeStamp,
            mediaUploadingTimeInText = timeInText,
            isImage = false
        )


        val imageMediaRef =
            chatRef.document(chatRoomId).collection(Table.Media.name).document(imageMediaId)
        val videoMediaRef =
            chatRef.document(chatRoomId).collection(Table.Media.name).document(videoMediaId)
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


        val recentChatData = recentMessageRefForReceiver.get().await()
        if (!recentChatData.exists()) {
            //Document doesn't exist
            // Do nothing
        } else {
            //Document exist
            val unSeenMessageCount =
                recentChatData.toObject<RecentChat>()?.unSeenMessageCount?.plus(1) ?: 0
            forReceiverRecentData = forReceiverRecentData.copy(
                unSeenMessageCount = unSeenMessageCount
            )
        }

        //In transaction all read come first then after all write come
        // Firebase firestore transaction is much slower then batch write because transaction run sequence

        firestore.runBatch {
            val timeTakingToCalculate = measureTimeMillis {
                if (isImagePresent) {
                    it.set(imageMediaRef, imageMediaData)
                }
                if (isVideoPresent) {
                    it.set(videoMediaRef, videoMediaData)
                }
                if (isLastMessageExist) {
                    it.update(lastMessageRef, lastMessage.toNormalMap().filterValues { it != null })
                } else {
                    it.set(lastMessageRef, lastMessage)
                }
                it.set(recentMessageRefForReceiver, forReceiverRecentData)
                it.set(recentMessageRefForSender, forSenderRecentData)
                it.set(messageRef, updatedMessage)
            }
            MyLogger.v(tagChat, msg = "Time taken to calculate $timeTakingToCalculate")


        }.addOnSuccessListener {
            updateSeenStatus(
                Constants.SeenStatus.Send.status, message.messageId, chatRoomId, message.receiverId
            )

            if (!isUserOnline) {
                NotificationSendingManager.sendNotification(
                    message.receiverId, notificationData
                )
            }


            trySend(UpdateChatResponse(isSuccess = true, errorMessage = ""))

        }.addOnFailureListener {
            trySend(UpdateChatResponse(isSuccess = false, errorMessage = it.message))

        }.await()

        awaitClose {
            close()
        }
    }

    suspend fun deleteMessage(
        message: Message,
        chatRoomId: String,
        userId: String,
        lastMessage: LastMessage?,
        secondLastMessage: Message? = null
    ) = callbackFlow<UpdateResponse> {

        try {
            val isImagePresent = message.imageUri != null
            val isVideoPresent = message.videoUri != null

            val deleteImageUrl = message.imageUri?.let { firebaseStorage.getReferenceFromUrl(it) }
            val deleteVideoUrl = message.videoUri?.let { firebaseStorage.getReferenceFromUrl(it) }

            val timeStamp = message.messageSentTimeInTimeStamp!!
            val imageMediaId = Helper.getImageMediaId(timeStamp)
            val videoMediaId = Helper.getVideoMediaId(timeStamp)

            val imageMediaRef =
                chatRef.document(chatRoomId).collection(Table.Media.name).document(imageMediaId)
            val videoMediaRef =
                chatRef.document(chatRoomId).collection(Table.Media.name).document(videoMediaId)
            val lastMessageRef = chatRef.document(chatRoomId)


            if (secondLastMessage != null) {
                // need to update last message , recent chat
                //For My Recent Chat
                val timeStamp = secondLastMessage.messageSentTimeInTimeStamp
                val timeInText = secondLastMessage.messageSendTimeInText
                val lastMessageType = secondLastMessage.messageType
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
                    senderId = secondLastMessage.senderId,
                    receiverId = secondLastMessage.receiverId,
                    userId = AuthManager.currentUserId()!!,
                    lastMessageSeen = secondLastMessage.seenStatus
                )

                val messageRef = chatRef.document(chatRoomId).collection(Table.Messages.name)
                    .document(message.messageId!!)
                val lastMessageRef = chatRef.document(chatRoomId)
                val isLastMessageExist = lastMessageRef.isExist()
                val receiverRecentRef =
                    userRef.document(userId).collection(Table.RecentChat.name).document(chatRoomId)
                val isReceiverRecentRefExist = receiverRecentRef.isExist()
                val myRecentChatRef =
                    userRef.document(AuthManager.currentUserId()!!)
                        .collection(Table.RecentChat.name)
                        .document(chatRoomId)
                val isMyRecentChatRef = myRecentChatRef.isExist()

                firestore.runBatch { batch ->
                    if (isImagePresent) {
                        deleteImageUrl?.delete()
                        batch.delete(imageMediaRef)
                    }
                    if (isVideoPresent) {
                        deleteVideoUrl?.delete()
                        batch.delete(videoMediaRef)
                    }

                    batch.delete(messageRef)
                    batch.safeUpdate(lastMessageRef,
                        lastMessage!!.toNormalMap().filterValues { it != null }, isLastMessageExist)
                    batch.safeUpdate(
                        receiverRecentRef,
                        forReceiverRecentData.toNormalMap(),
                        isReceiverRecentRefExist
                    )
                    batch.safeUpdate(
                        myRecentChatRef,
                        forMyRecentData.toNormalMap(),
                        isMyRecentChatRef
                    )

                }.addOnSuccessListener {
                    trySend(UpdateResponse(true, ""))
                }.addOnFailureListener {
                    trySend(UpdateResponse(false, it.message))
                }.await()


            } else {
                // just delete message no need to update any thing
                val messageDeleteRef = chatRef.document(chatRoomId).collection(Table.Messages.name)
                    .document(message.messageId!!)
                firestore.runBatch {
                    if (isImagePresent) {
                        deleteImageUrl?.delete()
                        it.delete(imageMediaRef)
                    }
                    if (isVideoPresent) {
                        deleteVideoUrl?.delete()
                        it.delete(videoMediaRef)
                    }

                    it.delete(messageDeleteRef)
                }.addOnSuccessListener {
                    trySend(UpdateResponse(true, ""))
                }.addOnFailureListener {
                    trySend(UpdateResponse(false, it.message))
                }.await()
            }
        } catch (e: FirebaseFirestoreException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }



        awaitClose {
            close()
        }
    }

    suspend fun clearChats(chatRoomId: String, receiverId: String) = callbackFlow<UpdateResponse> {

        StorageManager.deleteMediaOfChats(chatRoomId).onEach {
            if (it.isSuccess) {
                MyLogger.i(tagChat, msg = "All Media is deleted !!")
            } else {
                MyLogger.e(tagChat, msg = it.errorMessage)
            }

            val chatRoomIdRef = chatRef.document(chatRoomId)
            val chatMediaRef = chatRoomIdRef.collection(Table.Media.name)
            val chatMessageRef = chatRoomIdRef.collection(Table.Messages.name)
            val myRecentRef =
                userRef.document(AuthManager.currentUserId()!!).collection(Table.RecentChat.name)
                    .document(chatRoomId)
            val receiverRecentRef =
                userRef.document(receiverId).collection(Table.RecentChat.name).document(chatRoomId)

            firestore.runBatch {
                it.delete(chatRoomIdRef)
                it.delete(myRecentRef)
                it.delete(receiverRecentRef)
            }.addOnSuccessListener {
                launch {
                    deleteCollection(listOf(chatMediaRef, chatMessageRef)).onEach {
                        trySend(it)
                    }.launchIn(this)
                }

            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()

        }.launchIn(this)


        awaitClose {
            close()
        }
    }

    private suspend fun deleteCollection(collectionRefs: List<CollectionReference>) =
        callbackFlow<UpdateResponse> {
            withContext(Dispatchers.IO) {
                try {
                    for (collectionRef in collectionRefs) {
                        // Get all documents in the collection
                        val documents = collectionRef.get().await().documents

                        // Batch delete documents
                        val batchSize = 500 // Firestore batch limit
                        var batch = firestore.batch()
                        var counter = 0

                        for (document in documents) {
                            batch.delete(document.reference)
                            counter++

                            // Commit the batch every batchSize documents
                            if (counter % batchSize == 0) {
                                batch.commit().await()
                                batch = firestore.batch()
                            }
                        }

                        // Commit the remaining documents
                        if (counter % batchSize != 0) {
                            batch.commit().await()
                        }
                    }
                    trySend(UpdateResponse(true, "")) // All documents deleted successfully
                } catch (e: FirebaseFirestoreException) {
                    e.printStackTrace()
                    trySend(UpdateResponse(false, e.message)) // Failed to delete all documents
                } catch (e: Exception) {
                    e.printStackTrace()
                    trySend(UpdateResponse(false, e.message)) // Failed to delete all documents
                }
            }
            awaitClose {
                close()
            }
        }


    suspend fun getChatMessageAndListen(chatRoomId: String) =
        callbackFlow<ListenerEmissionType<Message, Message>> {
            val messageRef = chatRef.document(chatRoomId).collection(Table.Messages.name)

            val messageList =
                messageRef.get().await().toObjects(Message::class.java).toMutableList()

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting, responseList = messageList.toList()
                )
            )

            messageList.clear()
            chatMessageListener?.remove()
            isFirstTimeChatMessageListener =
                true  // This is need because we listen this flow on viewmodel and view model not destroy until view not destroy properly
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

    fun updateSeenStatus(
        status: String, messageId: String, chatRoomId: String, receiverId: String
    ) {
        launchCoroutineInIOThread {
            val messageRef =
                chatRef.document(chatRoomId).collection(Table.Messages.name).document(messageId)
            val myRecentChatRef =
                userRef.document(AuthManager.currentUserId()!!).collection(Table.RecentChat.name)
                    .document(chatRoomId)
            val receiverRecentChatRef =
                userRef.document(receiverId).collection(Table.RecentChat.name).document(chatRoomId)

            val isMessageRefExist = messageRef.isExist()
            val isMyRecentChatRefExist = myRecentChatRef.isExist()
            val isReceiverRecentChatRefExist = receiverRecentChatRef.isExist()


            firestore.runBatch {
                it.safeUpdate(messageRef , isMessageRefExist, Constants.MessageTable.SEEN_STATUS.fieldName, status)
                it.safeUpdate(
                    myRecentChatRef,
                    isMyRecentChatRefExist,
                    Constants.RecentChatTable.LAST_MESSAGE_SEEN.fieldName,
                    status
                )
                it.safeUpdate(
                    receiverRecentChatRef,
                    isReceiverRecentChatRefExist,
                    Constants.RecentChatTable.LAST_MESSAGE_SEEN.fieldName,
                    status
                )


                if (status == Constants.SeenStatus.MessageSeen.status) {
                    it.safeUpdate(
                        myRecentChatRef , isMyRecentChatRefExist, Constants.RecentChatTable.UNSEEN_MESSAGE_COUNT.fieldName, 0
                    )
                    it.safeUpdate(
                        receiverRecentChatRef,
                        isReceiverRecentChatRefExist,
                        Constants.RecentChatTable.UNSEEN_MESSAGE_COUNT.fieldName,
                        0
                    )
                }
            }.await()
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
        chatRoomId: String, isIAmUser1: Boolean, status: Boolean
    ) {
        try {
            val lastMessageRef = chatRef.document(chatRoomId)
            val timestamp = System.currentTimeMillis()
            if (isIAmUser1) {
                lastMessageRef.safeUpdate(
                    Constants.LastMessageTable.IS_USER_1_ONLINE.fieldName,
                    status
                )
                lastMessageRef.safeUpdate(
                    Constants.LastMessageTable.USER_1_LAST_ONLINE_TIMESTAMP.fieldName,
                    timestamp
                )
            } else {
                lastMessageRef.safeUpdate(
                    Constants.LastMessageTable.IS_USER_2_ONLINE.fieldName,
                    status
                )
                lastMessageRef.safeUpdate(
                    Constants.LastMessageTable.USER_2_LAST_ONLINE_TIMESTAMP.fieldName,
                    timestamp
                )
            }
        } catch (e: FirebaseFirestoreException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    suspend fun getMessageById(chatRoomId: String, messageId: String) = flow<Message> {
        chatRef.document(chatRoomId).collection(Table.Messages.name).document(messageId).get()
            .await().toObject<Message>()?.let { emit(it) }
    }


    suspend fun getRecentChatAndListen() =
        callbackFlow<ListenerEmissionType<UserRecentModel, UserRecentModel>> {
            val recentChatRef =
                userRef.document(AuthManager.currentUserId()!!).collection(Table.RecentChat.name)
            val recentList =
                recentChatRef.get().await().toObjects(RecentChat::class.java).toMutableList()
            val userRecentModel = recentList.mapNotNull { recentChat ->
                recentChat.isGroupChat?.takeIf { it }?.run {
                    recentChat.chatRoomId?.let {
                        getGroupInfoWithoutAsync(it)?.let { groupInfo ->
                            UserRecentModel(
                                recentChat = recentChat,
                                groupInfo = groupInfo
                            )
                        }
                    }
                } ?: run {
                    recentChat.userId?.let {
                        UserManager.getUserById(it)?.let { user ->
                            UserRecentModel(user, recentChat)
                        }
                    }
                }
            }
            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting, responseList = userRecentModel.toList()
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
                    launchCoroutineInIOThread {
                        val recentChat = it.document.toObject<RecentChat>()
                        val userRecentModel =
                            recentChat.isGroupChat?.takeIf { it }?.run {
                                recentChat.chatRoomId?.let {
                                    getGroupInfoWithoutAsync(it)?.let { groupInfo ->
                                        UserRecentModel(
                                            recentChat = recentChat,
                                            groupInfo = groupInfo
                                        )
                                    }
                                }
                            } ?: run {
                                recentChat.userId?.let {
                                    UserManager.getUserById(it)?.let { user ->
                                        UserRecentModel(user, recentChat)
                                    }
                                }
                            }

                        when (it.type) {
                            DocumentChange.Type.ADDED -> {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Added,
                                        singleResponse = userRecentModel
                                    )
                                )
                            }

                            DocumentChange.Type.MODIFIED -> {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Modify,
                                        singleResponse = userRecentModel
                                    )
                                )
                            }

                            DocumentChange.Type.REMOVED -> {

                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Removed,
                                        singleResponse = userRecentModel
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

    suspend fun muteChatNotification(userId: String, isMute: Boolean) =
        callbackFlow<UpdateResponse> {
            val muteUserRef = chatRef.document(AuthManager.currentUserId()!!)
                .collection(Table.ChatMuteNotification.name).document(userId)

            val timestamp = System.currentTimeMillis()
            val timeInText = Helper.formatTimestampToDateAndTime(timestamp)
            val chatMuteNotificationData = ChatMuteNotificationData(
                userId, timestamp, timeInText
            )
            firestore.runBatch {
                if (isMute) {
                    it.set(muteUserRef, chatMuteNotificationData)
                } else {
                    it.delete(muteUserRef)
                }
            }.addOnSuccessListener {
                trySend(UpdateResponse(true, ""))
            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()

            awaitClose {
                close()
            }
        }

    suspend fun isUserMuted(userId: String): Boolean =
        chatRef.document(AuthManager.currentUserId()!!)
            .collection(Table.ChatMuteNotification.name).document(userId).get().await().exists()

    suspend fun isReceiverMutedMe(userId: String): Boolean = chatRef.document(userId)
        .collection(Table.ChatMuteNotification.name).document(AuthManager.currentUserId()!!).get()
        .await().exists()

    suspend fun isUserMutedAndListen(userId: String) = callbackFlow<Boolean> {
        val muteUserRef = chatRef.document(AuthManager.currentUserId()!!)
            .collection(Table.ChatMuteNotification.name).document(userId)

        muteUserRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(false)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // User is muted
                trySend(true)
            } else {
                // User is not muted
                trySend(false)
            }
        }

        awaitClose { close() }
    }

    suspend fun getAllMediaOfChat(chatRoomId: String) = flow<List<ChatMediaData>> {
        emit(
            chatRef.document(chatRoomId).collection(Table.Media.name).get().await()
                .toObjects(ChatMediaData::class.java)
        )
    }

    //region:: Recent Chat

    suspend fun deleteRecentChat(chatRoomId: String) = callbackFlow<UpdateResponse> {

        try {
            userRef.document(AuthManager.currentUserId()!!).collection(Table.RecentChat.name)
                .document(chatRoomId).delete().addOnSuccessListener {
                    trySend(UpdateResponse(true, ""))
                }.addOnFailureListener {
                    trySend(UpdateResponse(false, it.message))
                }.await()

        } catch (e: FirebaseFirestoreException) {
            e.printStackTrace()
            trySend(UpdateResponse(false, e.message))
        } catch (e: Exception) {
            e.printStackTrace()

        }


        awaitClose {
            close()
        }
    }

    //endregion


    //region:: Group Chat

    suspend fun sentGroupMessage(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        users: List<GroupMember>,// Just for add  message recent chat
        action: InfoType? = null,
        addedOrRemovedUserId: String? = null,
        groupInfo: GroupInfo? = null
    ) = callbackFlow<UpdateChatResponse> {
        try {
            MyLogger.i(tagChat, isFunctionCall = true)
            val isImagePresent = message.imageUri != null || groupInfo?.groupPic != null
            val isVideoPresent = message.videoUri != null

            when {
                isVideoPresent -> {
                    uploadGroupVideo(
                        message,
                        lastMessage,
                        chatRoomId,
                        isImagePresent,
                        users,
                        action,
                        addedOrRemovedUserId
                    ).onEach {
                        trySend(it)
                    }.launchIn(this)
                }

                isImagePresent -> {
                    uploadGroupImage(
                        message,
                        lastMessage,
                        chatRoomId,
                        users,
                        action,
                        addedOrRemovedUserId,
                        groupInfo
                    ).onEach {
                        trySend(it)
                    }.launchIn(this)
                }

                else -> {
                    saveGroupMessageToDatabase(
                        message,
                        lastMessage,
                        chatRoomId,
                        users,
                        action,
                        addedOrRemovedUserId,
                        groupInfo = groupInfo
                    ).onEach {
                        trySend(it)
                    }.launchIn(this)

                }
            }
        } catch (e: FirebaseFirestoreException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }


        awaitClose {
            close()
        }
    }

    private suspend fun uploadGroupVideo(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        isImagePresent: Boolean,
        users: List<GroupMember>,
        action: InfoType?,
        addedOrRemovedUserId: String?
    ) = callbackFlow<UpdateChatResponse> {
        trySend(UpdateChatResponse(true, "Video  Uploading "))
        val videoUri = message.videoUri!!.convertParseUri()
        StorageManager.uploadVideoToServer(
            Constants.Table.Chats.name,
            "$chatRoomId/${Constants.FolderName.ChatVideo.name}",
            videoUri
        ).collect {
            when (it.state) {
                Constants.StorageManagerState.InProgress -> {
                    trySend(UpdateChatResponse(true, "Video Uploading ", progress = it.progress))
                }

                Constants.StorageManagerState.Error -> {
                    trySend(UpdateChatResponse(isSuccess = false, errorMessage = it.error))
                }

                Constants.StorageManagerState.UrlNotGet -> {
                    trySend(UpdateChatResponse(isSuccess = false, errorMessage = it.error))
                }

                Constants.StorageManagerState.Success -> {
                    trySend(UpdateChatResponse(true, "Video Uploaded"))
                    val onlineVideoUri = it.url
                    val updatedMessage = message.copy(
                        videoUri = onlineVideoUri
                    )
                    if (isImagePresent) {
                        uploadGroupImage(
                            updatedMessage,
                            lastMessage,
                            chatRoomId,
                            users,
                            action,
                            addedOrRemovedUserId
                        ).onEach {
                            trySend(it)
                        }.launchIn(this)
                    } else {
                        saveGroupMessageToDatabase(
                            updatedMessage,
                            lastMessage,
                            chatRoomId,
                            users!!,
                            action,
                            addedOrRemovedUserId
                        ).onEach {
                            trySend(it)
                        }.launchIn(this)
                    }
                }
            }
        }
    }

    private suspend fun uploadGroupImage(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        users: List<GroupMember>,
        action: InfoType?,
        addedOrRemovedUserId: String?,
        groupInfo: GroupInfo? = null
    ) = callbackFlow<UpdateChatResponse> {
        trySend(UpdateChatResponse(true, "Image Uploading"))
        val imageUri =
            message.imageUri?.convertParseUri() ?: groupInfo?.groupPic!!.convertParseUri()
        StorageManager.uploadImageToServer(
            Constants.Table.Chats.name,
            "$chatRoomId/${Constants.FolderName.ChatImage.name}",
            imageUri
        ).collect {
            when (it.state) {
                Constants.StorageManagerState.InProgress -> {
                    trySend(UpdateChatResponse(true, "Image Uploading", progress = it.progress))
                }

                Constants.StorageManagerState.Error -> {
                    trySend(UpdateChatResponse(isSuccess = false, errorMessage = it.error))
                }

                Constants.StorageManagerState.UrlNotGet -> {
                    trySend(UpdateChatResponse(isSuccess = false, errorMessage = it.error))
                }

                Constants.StorageManagerState.Success -> {
                    trySend(UpdateChatResponse(true, "Image Uploaded"))
                    val onlineImageUri = it.url

                    val updatedMessage = if (message.imageUri != null) {
                        message.copy(
                            imageUri = onlineImageUri
                        )
                    } else message

                    val updatedGroupInfo = if (groupInfo?.groupPic != null) {
                        groupInfo.copy(groupPic = onlineImageUri)
                    } else groupInfo

                    saveGroupMessageToDatabase(
                        updatedMessage,
                        lastMessage,
                        chatRoomId,
                        users,
                        action,
                        addedOrRemovedUserId,
                        updatedGroupInfo
                    ).onEach {
                        trySend(it)
                    }.launchIn(this)

                }
            }
        }
    }

    private suspend fun saveGroupMessageToDatabase(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        users: List<GroupMember>,
        action: InfoType?,
        addedOrRemovedUserId: String?,
        groupInfo: GroupInfo? = null
    ) = callbackFlow<UpdateChatResponse> {
        MyLogger.i(tagChat, isFunctionCall = true)
        val isImagePresent = message.imageUri != null
        val isVideoPresent = message.videoUri != null
        val timeStamp = System.currentTimeMillis()
        val notificationId = Helper.getNotificationId()
        val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
        val isMessageContainText = !message.text.isNullOrBlank() || message.infoMessageType != null
        val type =
            if (isMessageContainText) NotificationType.GroupTextChat.name else NotificationType.GroupMediaChat.name
        val lastMessageType =
            if (isMessageContainText) Constants.LastMessageType.Text.type else Constants.LastMessageType.Media.type
        val notificationData = NotificationData(
            notificationId = notificationId,
            friendOrFollowerId = AuthManager.currentUserId()!!,
            notificationTimeInText = timeInText,
            notificationTimeInTimeStamp = timeStamp.toString(),
            type = type,
            chatRoomId = chatRoomId,
            messageId = message.messageId,
            isGroupMessage = true.toString()
        )


        // This is updated message
        val updatedMessage = message.copy(
            messageSentTimeInTimeStamp = timeStamp, messageSendTimeInText = timeInText
        )
        val updatedLastMessage = lastMessage.copy(
            lastMessageSentTimeInTimeStamp = timeStamp, lastMessageSentTimeInText = timeInText
        )
        val updatedGroupInfo = groupInfo?.copy(
            groupCreationDateInString = timeInText,
            groupCreationDateInTimeStamp = timeStamp
        )

        val commonRecentChat = RecentChat(
            chatRoomId = chatRoomId,
            lastMessageTimeInTimeStamp = timeStamp,
            lastMessageTimeInText = timeInText,
            message = message.text,
            lastMessageType = lastMessageType,
            senderId = AuthManager.currentUserId()!!,
            lastMessageSeen = Constants.SeenStatus.Sending.status,
            isGroupChat = true,
            infoMessageType = action?.name,
            addedOrRemovedUserId = addedOrRemovedUserId
        )


        //For Media Reference
        val imageMediaId = Helper.getImageMediaId(timeStamp)
        val videoMediaId = Helper.getVideoMediaId(timeStamp)
        val imageMediaData = ChatMediaData(
            mediaId = imageMediaId,
            mediaUri = message.imageUri,
            mediaUploadingTimeInTimeStamp = timeStamp,
            mediaUploadingTimeInText = timeInText,
            isImage = true

        )
        val videoMediaData = ChatMediaData(
            mediaId = videoMediaId,
            mediaUri = message.videoUri,
            mediaUploadingTimeInTimeStamp = timeStamp,
            mediaUploadingTimeInText = timeInText,
            isImage = false
        )


        val imageMediaRef =
            chatRef.document(chatRoomId).collection(Table.Media.name).document(imageMediaId)
        val videoMediaRef =
            chatRef.document(chatRoomId).collection(Table.Media.name).document(videoMediaId)
        val lastMessageRef = chatRef.document(chatRoomId)

        val messageRef = chatRef.document(chatRoomId).collection(Table.Messages.name)
            .document(message.messageId!!)
        val groupInfoRef =
            chatRef.document(chatRoomId).collection(Table.GroupInfo.name).document(chatRoomId)
        val isChatRoomExist = chatRef.document(chatRoomId).get().await().exists()


        //In transaction all read come first then after all write come
        // Firebase firestore transaction is much slower then batch write because transaction run sequence

        firestore.runBatch { batch ->
            val timeTakingToCalculate = measureTimeMillis {
                if (!isChatRoomExist) {
                    users.mapNotNull { it.memberId }.forEach {
                        batch.set(
                            chatRef.document(chatRoomId).collection(Table.GroupMember.name)
                                .document(it), GroupMember(it)
                        )
                    }
                }

                if (updatedGroupInfo != null) {
                    batch.set(groupInfoRef, updatedGroupInfo)
                }
                if (isImagePresent) {
                    batch.set(imageMediaRef, imageMediaData)
                }
                if (isVideoPresent) {
                    batch.set(videoMediaRef, videoMediaData)
                }
                if (isChatRoomExist) {

                    batch.update(
                        lastMessageRef,
                        updatedLastMessage.toNormalMap().filterValues { it != null })
                } else {
                    batch.set(lastMessageRef, updatedLastMessage)
                }
                batch.set(messageRef, updatedMessage)
            }
            MyLogger.v(tagChat, msg = "Time taken to calculate $timeTakingToCalculate")


        }.addOnSuccessListener {

            trySend(UpdateChatResponse(isSuccess = true, errorMessage = ""))

            launch {
                // Update to send status
                messageRef.safeUpdate(
                    Constants.GroupMessageTable.SEEN_STATUS.fieldName,
                    Constants.SeenStatus.Send.status
                )

                updateRecentChat(
                    users.mapNotNull { it.memberId },
                    commonRecentChat.copy(lastMessageSeen = Constants.SeenStatus.Send.status),
                    chatRoomId
                )
            }
            if (groupInfo == null) {
                launchCoroutineInIOThread {
                    updateUnSeenMessageList(
                        users.mapNotNull { it.memberId }
                            .filter { it != AuthManager.currentUserId()!! },
                        updatedMessage,
                        chatRoomId
                    )
                }
            }

            if (groupInfo == null) {
                launch {
                    sendNotificationToAllOfflineMember(users, notificationData)
                }
            }


        }.addOnFailureListener {
            trySend(UpdateChatResponse(isSuccess = false, errorMessage = it.message))

        }.await()

        awaitClose {
            close()
        }
    }

    suspend fun sentGroupInfoMessage(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        users: List<GroupMember>,
        action: InfoType,
        addedOrRemovedUserId: String? = null,
        newMembers: List<String>? = null,
        groupInfo: GroupInfo? = null
    ) = callbackFlow<UpdateResponse> {
        val timeStamp = System.currentTimeMillis()
        val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
        val isMessageContainText = !message.text.isNullOrBlank() || message.infoMessageType != null
        val lastMessageType =
            if (isMessageContainText) Constants.LastMessageType.Text.type else Constants.LastMessageType.Media.type


        // This is updated message
        val updatedMessage = message.copy(
            messageSentTimeInTimeStamp = timeStamp, messageSendTimeInText = timeInText
        )
        val updatedLastMessage = lastMessage.copy(
            lastMessageSentTimeInTimeStamp = timeStamp, lastMessageSentTimeInText = timeInText
        )

        val commonRecentChat = RecentChat(
            chatRoomId = chatRoomId,
            lastMessageTimeInTimeStamp = timeStamp,
            lastMessageTimeInText = timeInText,
            message = message.text,
            lastMessageType = lastMessageType,
            senderId = AuthManager.currentUserId()!!,
            lastMessageSeen = Constants.SeenStatus.Sending.status,
            isGroupChat = true,
            infoMessageType = action?.name,
            addedOrRemovedUserId = addedOrRemovedUserId,
            newMembersName = message.newMembersName,
            newMembers = message.newMembers,
            receiverId = chatRoomId
        )

        val lastMessageRef = chatRef.document(chatRoomId)

        val messageRef = chatRef.document(chatRoomId).collection(Table.Messages.name)
            .document(message.messageId!!)
        val groupInfoRef =
            chatRef.document(chatRoomId).collection(Table.GroupInfo.name).document(chatRoomId)
        val isChatRoomExist = chatRef.document(chatRoomId).get().await().exists()
        val isThisFunctionCallForRemoveMember =
            (action.name == InfoType.MemberRemoved.name || action.name == InfoType.MemberExit.name)
        val isThisFunctionCallForAddNewMember = action.name == InfoType.MemberAdded.name


        //In transaction all read come first then after all write come
        // Firebase firestore transaction is much slower then batch write because transaction run sequence

        firestore.runBatch { batch ->
            val timeTakingToCalculate = measureTimeMillis {
                if (!isChatRoomExist) {
                    users.mapNotNull { it.memberId }.forEach {
                        batch.set(
                            chatRef.document(chatRoomId).collection(Table.GroupMember.name)
                                .document(it), GroupMember(it)
                        )
                    }
                }

                //Set Group Info
                if (groupInfo != null) {
                    batch.set(groupInfoRef, groupInfo)
                }

                //Set or update last message
                if (isChatRoomExist) {
                    batch.update(
                        lastMessageRef,
                        updatedLastMessage.toNormalMap().filterValues { it != null })
                } else {
                    batch.set(lastMessageRef, updatedLastMessage)
                }

                // This is for when new user added
                if (isThisFunctionCallForAddNewMember) {
                    newMembers?.forEach() {
                        batch.set(
                            chatRef.document(chatRoomId).collection(Table.GroupMember.name)
                                .document(it), GroupMember(
                                isOnline = false,
                                memberId = it,
                                groupJoiningDateInText = timeInText,
                                groupJoiningDateInTimeStamp = timeStamp
                            )
                        )
                    }
                }

                //This is for when user exit or remove from group
                if (isThisFunctionCallForRemoveMember) {
                    batch.delete(
                        chatRef.document(chatRoomId).collection(Table.GroupMember.name)
                            .document(addedOrRemovedUserId!!)
                    )
                    batch.delete(
                        userRef.document(addedOrRemovedUserId!!).collection(Table.RecentChat.name)
                            .document(chatRoomId)
                    )
                }

                //Add new info message in chat
                batch.set(messageRef, updatedMessage)
            }
            MyLogger.v(tagChat, msg = "Time taken to calculate $timeTakingToCalculate")


        }.addOnSuccessListener {

            trySend(UpdateResponse(isSuccess = true, errorMessage = ""))

            launch {
                updateRecentChat(
                    users.mapNotNull { it.memberId },
                    commonRecentChat,
                    chatRoomId
                )
            }


        }.addOnFailureListener {
            trySend(UpdateResponse(isSuccess = false, errorMessage = it.message))

        }.await()

        awaitClose {
            close()
        }
    }

    private fun updateUnSeenMessageList(
        userId: List<String>,
        updatedMessage: GroupMessage,
        chatRoomId: String
    ) {
        userId.forEach { id ->
            launchCoroutineInIOThread {
                userRef.document(id).collection(Table.UnSeenMessage.name).document(chatRoomId).get()
                    .addOnSuccessListener {
                        if (it.exists()) {
                            val unSeenMessage = it.toObject<UnSeenMessageModel>()
                            val tempUnSeenMessage =
                                unSeenMessage?.unSeenMessage?.toMutableList() ?: mutableListOf()
                            tempUnSeenMessage.add(updatedMessage)
                            userRef.document(id).collection(Table.UnSeenMessage.name)
                                .document(chatRoomId).set(UnSeenMessageModel(tempUnSeenMessage))
                        } else {
                            userRef.document(id).collection(Table.UnSeenMessage.name)
                                .document(chatRoomId)
                                .set(UnSeenMessageModel(unSeenMessage = listOf(updatedMessage)))
                        }
                    }
            }
        }
    }

    suspend fun listenNewMessage(chatRoomId: String) = callbackFlow<Unit> {
        try {
            listenNewMessageListener?.remove()
            listenNewMessageListener =
                userRef.document(AuthManager.currentUserId()!!).collection(Table.UnSeenMessage.name)
                    .document(chatRoomId).addSnapshotListener { value, error ->
                        if (error != null) return@addSnapshotListener

                        if (value != null) {
                            val unSeenMessageModel = value.toObject<UnSeenMessageModel>()
                            unSeenMessageModel?.unSeenMessage?.let {
                                updateUnseenMessagesToSeen(chatRoomId, it)
                                userRef.document(AuthManager.currentUserId()!!)
                                    .collection(Table.UnSeenMessage.name).document(chatRoomId)
                                    .delete()
                            }
                        }
                    }
        } catch (e: FirebaseFirestoreException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }


        awaitClose {
            listenNewMessageListener?.remove()
            close()
        }
    }

    private suspend fun sendNotificationToAllOfflineMember(
        users: List<GroupMember>,
        notificationData: NotificationData
    ) {
        users.mapNotNull { if (it.isOnline == false) it else null }.forEach {
            launchCoroutineInIOThread {
                NotificationSendingManager.sendNotification(
                    it.memberId!!, notificationData
                )
            }
        }
    }

    private suspend fun updateRecentChat(
        users: List<String>,
        commonRecentChat: RecentChat,
        chatRoomId: String
    ) {
        launchCoroutineInIOThread {
            // Use supervisorScope to launch child coroutines in parallel and handle their exceptions independently
            supervisorScope {
                users.forEach { userId ->
                    launch {
                        try {
                            val recentChatRef =
                                userRef.document(userId).collection(Table.RecentChat.name)
                                    .document(chatRoomId)
                            val previousRecentChatData =
                                recentChatRef.get().await().toObject<RecentChat>()
                            val updatedRecentChatData = if (previousRecentChatData == null) {
                                //Document doesn't exist
                                // Do nothing
                                commonRecentChat
                            } else {
                                //Document exist
                                var unSeenMessageCount =
                                    previousRecentChatData.unSeenMessageCount?.plus(1) ?: 0

                                unSeenMessageCount =
                                    if (AuthManager.currentUserId()!! == userId) 0 else unSeenMessageCount

                                commonRecentChat.copy(
                                    unSeenMessageCount = unSeenMessageCount
                                )
                            }
                            val recentMessageRefForReceiver = recentChatRef
                                .set(updatedRecentChatData).await()
                            // Handle the result if needed
                        } catch (e: FirebaseFirestoreException) {
                            e.printStackTrace()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    suspend fun deleteMessage(
        message: GroupMessage,
        chatRoomId: String,
        users: List<String>,
        lastMessage: GroupLastMessage?,
        secondLastMessage: GroupMessage? = null
    ) = callbackFlow<UpdateResponse> {

        val isImagePresent = message.imageUri != null
        val isVideoPresent = message.videoUri != null

        val deleteImageUrl = message.imageUri?.let { firebaseStorage.getReferenceFromUrl(it) }
        val deleteVideoUrl = message.videoUri?.let { firebaseStorage.getReferenceFromUrl(it) }

        val timeStamp = message.messageSentTimeInTimeStamp!!
        val imageMediaId = Helper.getImageMediaId(timeStamp)
        val videoMediaId = Helper.getVideoMediaId(timeStamp)

        val imageMediaRef =
            chatRef.document(chatRoomId).collection(Table.Media.name).document(imageMediaId)
        val videoMediaRef =
            chatRef.document(chatRoomId).collection(Table.Media.name).document(videoMediaId)
        val lastMessageRef = chatRef.document(chatRoomId)


        if (secondLastMessage != null) {
            // need to update last message , recent chat

            val timeStamp = secondLastMessage.messageSentTimeInTimeStamp
            val timeInText = secondLastMessage.messageSendTimeInText
            val lastMessageType = secondLastMessage.messageType
            val commonRecentChat = RecentChat(
                chatRoomId = chatRoomId,
                lastMessageTimeInTimeStamp = timeStamp,
                lastMessageTimeInText = timeInText,
                message = secondLastMessage.text,
                lastMessageType = lastMessageType,
                senderId = secondLastMessage.senderId,
                lastMessageSeen = secondLastMessage.seenStatus,
                isGroupChat = true,
            )


            val messageRef = chatRef.document(chatRoomId).collection(Table.Messages.name)
                .document(message.messageId!!)
            val lastMessageRef = chatRef.document(chatRoomId)
            val isLastMessageRefExist = lastMessageRef.isExist()

            firestore.runBatch { batch ->
                if (isImagePresent) {
                    deleteImageUrl?.delete()
                    batch.delete(imageMediaRef)
                }
                if (isVideoPresent) {
                    deleteVideoUrl?.delete()
                    batch.delete(videoMediaRef)
                }

                batch.delete(messageRef)
                batch.safeUpdate(lastMessageRef,
                    lastMessage!!.toNormalMap().filterValues { it != null } ,isLastMessageRefExist)
            }.addOnSuccessListener {
                trySend(UpdateResponse(true, ""))
                launch {
                    updateRecentChat(users, commonRecentChat, chatRoomId)
                }
            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()


        } else {
            // just delete message no need to update any thing
            val messageDeleteRef = chatRef.document(chatRoomId).collection(Table.Messages.name)
                .document(message.messageId!!)
            firestore.runBatch {
                if (isImagePresent) {
                    deleteImageUrl?.delete()
                    it.delete(imageMediaRef)
                }
                if (isVideoPresent) {
                    deleteVideoUrl?.delete()
                    it.delete(videoMediaRef)
                }

                it.delete(messageDeleteRef)
            }.addOnSuccessListener {
                trySend(UpdateResponse(true, ""))
            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()
        }

        awaitClose {
            close()
        }
    }

    suspend fun clearChats(
        chatRoomId: String, users: List<String>, lastMessage: GroupLastMessage,
        secondLastMessage: GroupMessage
    ) = callbackFlow<UpdateResponse> {

        StorageManager.deleteMediaOfChats(chatRoomId).onEach {
            if (it.isSuccess) {
                MyLogger.i(tagChat, msg = "All Media is deleted !!")
            } else {
                MyLogger.e(tagChat, msg = it.errorMessage)
            }

            val chatRoomIdRef = chatRef.document(chatRoomId)
            val chatMediaRef = chatRoomIdRef.collection(Table.Media.name)
            val chatMessageRef = chatRoomIdRef.collection(Table.Messages.name)

            val timeStamp = secondLastMessage.messageSentTimeInTimeStamp
            val timeInText = secondLastMessage.messageSendTimeInText
            val lastMessageType = secondLastMessage.messageType
            val commonRecentChat = RecentChat(
                chatRoomId = chatRoomId,
                lastMessageTimeInTimeStamp = timeStamp,
                lastMessageTimeInText = timeInText,
                message = secondLastMessage.text,
                lastMessageType = lastMessageType,
                senderId = secondLastMessage.senderId,
                lastMessageSeen = secondLastMessage.seenStatus,
                isGroupChat = true,
                addedOrRemovedUserId = secondLastMessage.addedOrRemovedUserId,
                infoMessageType = secondLastMessage.infoMessageType,
                unSeenMessageCount = 0
            )


            firestore.runBatch { batch ->
                batch.set(chatRoomIdRef, lastMessage)
                users.forEach {
                    batch.set(
                        userRef.document(it).collection(Table.RecentChat.name).document(chatRoomId),
                        commonRecentChat
                    )
                }
            }.addOnSuccessListener {
                launch {
                    deleteCollection(listOf(chatMediaRef)).onEach {
                        MyLogger.d(tagChat, msg = "Media Chat Collection is deleted!")
                        deleteAllMessageFromGroup(chatRoomId).onEach {
                            trySend(it)
                        }.launchIn(this)

                    }.launchIn(this)
                }

            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()

        }.launchIn(this)


        awaitClose {
            close()
        }
    }

    private fun deleteAllMessageFromGroup(chatRoomId: String) = callbackFlow<UpdateResponse> {
        val messagesRef = chatRef.document(chatRoomId).collection(Table.Messages.name)

        val scope = CoroutineScope(Dispatchers.IO) // Use IO dispatcher for parallel tasks
        val jobs = mutableListOf<Deferred<Unit>>()
        val BATCH_SIZE = 500

        try {
            var query = messagesRef.whereEqualTo(
                Constants.MessageTable.MESSAGE_TYPE.fieldName,
                Constants.MessageType.Chat.type
            )
            var shouldContinue = true

            while (shouldContinue) {
                // Fetch a batch of chat-type messages
                val querySnapshot = query.limit(BATCH_SIZE.toLong()).get().await()

                // If no documents are returned, exit the loop
                if (querySnapshot.isEmpty) {
                    shouldContinue = false
                    continue
                }

                // Create a batch for deletion
                val batch = firestore.batch()

                // Add each document to the batch for deletion
                for (document in querySnapshot.documents) {
                    batch.delete(document.reference)
                }

                // Launch the batch commit in parallel
                jobs.add(scope.async {
                    batch.commit().await()
                    //Please below logger not removed else get error because await return Void and jobs.add need Unit. So by add this logger get Unit.
                    MyLogger.i(tagChat, msg = "Deleted ${querySnapshot.size()} chat-type messages")
                })

                // Continue querying for the next batch
                query = query.startAfter(querySnapshot.documents.last())
            }

            // Await completion of all batch operations
            jobs.awaitAll()
            trySend(UpdateResponse(true, "All messages deleted successfully"))
        } catch (e: FirebaseFirestoreException) {
            e.printStackTrace()
            trySend(UpdateResponse(false, "Error deleting messages: ${e.message}"))
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(UpdateResponse(false, "Error deleting messages: ${e.message}"))
        }
        awaitClose { close() }
    }

    suspend fun getGroupChatMessageAndListen(chatRoomId: String) =
        callbackFlow<ListenerEmissionType<GroupMessage, GroupMessage>> {
            val messageRef = chatRef.document(chatRoomId).collection(Table.Messages.name)

            val messageList =
                messageRef.get().await().toObjects(GroupMessage::class.java).toMutableList()

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting, responseList = messageList.toList()
                )
            )

            messageList.clear()
            chatMessageListener?.remove()
            isFirstTimeChatMessageListener =
                true  // This is need because we listen this flow on viewmodel and view model not destroy until view not destroy properly
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
                                    singleResponse = it.document.toObject<GroupMessage>()
                                )
                            )
                        }

                        DocumentChange.Type.MODIFIED -> {
                            trySend(
                                ListenerEmissionType(
                                    Constants.ListenerEmitType.Modify,
                                    singleResponse = it.document.toObject<GroupMessage>()
                                )
                            )
                        }

                        DocumentChange.Type.REMOVED -> {
                            trySend(
                                ListenerEmissionType(
                                    Constants.ListenerEmitType.Removed,
                                    singleResponse = it.document.toObject<GroupMessage>()
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

    suspend fun getGroupMemberInfo(chatRoomId: String) =
        callbackFlow<ListenerEmissionType<GroupMemberDetails, GroupMemberDetails>> {
            val memberRef = chatRef.document(chatRoomId).collection(Table.GroupMember.name)

            val memberList =
                memberRef.get().await().toObjects(GroupMember::class.java).mapNotNull {
                    UserManager.getUserById(it.memberId!!)
                        ?.let { it1 -> GroupMemberDetails(it, it1) }
                }.toMutableList()

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting, responseList = memberList.toList()
                )
            )

            memberList.clear()
            groupMemberListener?.remove()
            isFirstTimeGroupMemberListener =
                true  // This is need because we listen this flow on viewmodel and view model not destroy until view not destroy properly
            groupMemberListener = memberRef.addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (isFirstTimeGroupMemberListener) {
                    isFirstTimeGroupMemberListener = false
                    return@addSnapshotListener
                }
                value?.documentChanges?.forEach {
                    when (it.type) {
                        DocumentChange.Type.ADDED -> {
                            launch {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Added,
                                        singleResponse = it.document.toObject<GroupMember>().run {
                                            GroupMemberDetails(
                                                this,
                                                UserManager.getUserById(this.memberId!!)!!
                                            )
                                        }
                                    )
                                )
                            }

                        }

                        DocumentChange.Type.REMOVED -> {
                            launch {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Removed,
                                        singleResponse = it.document.toObject<GroupMember>().run {
                                            GroupMemberDetails(
                                                this,
                                                UserManager.getUserById(this.memberId!!)!!
                                            )
                                        }
                                    )
                                )
                            }
                        }

                        DocumentChange.Type.MODIFIED -> {
                            launch {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Modify,
                                        singleResponse = it.document.toObject<GroupMember>().run {
                                            GroupMemberDetails(
                                                this,
                                                UserManager.getUserById(this.memberId!!)!!
                                            )
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
            awaitClose {
                groupMemberListener?.remove()
                isFirstTimeGroupMemberListener = true
                close()
            }
        }

    suspend fun getGroupInfo(chatRoomId: String) =
        callbackFlow<GroupInfo?> {
            val groupInfoRef =
                chatRef.document(chatRoomId).collection(Table.GroupInfo.name).document(chatRoomId)

            groupInfoRef.addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                value?.let {
                    trySend(
                        it.toObject<GroupInfo>()
                    )
                }
            }
            awaitClose {
                close()
            }
        }

    suspend fun updateGroupInfo(
        message: GroupMessage,
        lastMessage: GroupLastMessage,
        chatRoomId: String,
        users: List<GroupMember>,// Just for add  message recent chat
        groupInfo: GroupInfo? = null, deleteImage: String? = null, uploadingImage: String? = null
    ) = callbackFlow<UpdateResponse> {
        MyLogger.i(tagChat, isFunctionCall = true)
        if (deleteImage != null) {
            StorageManager.deleteImageFromServer(deleteImage).onEach {
                if (it.isSuccess) {
                    MyLogger.i(tagChat, msg = "Image Delete Successfully !")
                } else {
                    MyLogger.e(
                        tagChat,
                        msg = giveMeErrorMessage(
                            "Delete Group Profile Pic",
                            it.errorMessage?.toString() ?: ""
                        )
                    )
                }

            }.launchIn(this)
        }

        if (uploadingImage != null) {
            StorageManager.uploadImageToServer(
                Table.Chats.name,
                "$chatRoomId/${Constants.FolderName.ChatImage.name}",
                imageUri = uploadingImage.convertParseUri()
            ).onEach {
                when (it.state) {
                    Constants.StorageManagerState.InProgress -> {

                    }

                    Constants.StorageManagerState.Error -> {
                        trySend(UpdateResponse(false, it.error))
                    }

                    Constants.StorageManagerState.UrlNotGet -> {
                        trySend(UpdateResponse(false, it.error))
                    }

                    Constants.StorageManagerState.Success -> {
                        val onlineImageUri = it.url
                        val _groupInfo = groupInfo?.copy(
                            groupPic = onlineImageUri
                        )
                        saveGroupMessageToDatabase(
                            message,
                            lastMessage,
                            chatRoomId,
                            users,
                            InfoType.GroupDetailsChanged,
                            null,
                            groupInfo = _groupInfo
                        ).onEach {
                            if (it.isSuccess) {
                                trySend(UpdateResponse(true, ""))
                                return@onEach
                            }
                            if (it.errorMessage != null) {
                                trySend(UpdateResponse(false, it.errorMessage))
                                return@onEach
                            }
                        }.launchIn(this)
                    }
                }

            }.launchIn(this)
        } else {
            saveGroupMessageToDatabase(
                message,
                lastMessage,
                chatRoomId,
                users,
                InfoType.GroupDetailsChanged,
                null,
                groupInfo = groupInfo
            ).onEach {
                if (it.isSuccess) {
                    trySend(UpdateResponse(true, ""))
                    return@onEach
                }
                if (it.errorMessage != null) {
                    trySend(UpdateResponse(false, it.errorMessage))
                    return@onEach
                }
            }.launchIn(this)
        }




        awaitClose {
            close()
        }
    }


    suspend fun getGroupInfoWithoutAsync(chatRoomId: String) =
        chatRef.document(chatRoomId).collection(Table.GroupInfo.name).document(chatRoomId).get()
            .await().toObject<GroupInfo>()

    suspend fun updateGroupMemberOnlineStatus(chatRoomId: String, status: Boolean) {
        chatRef.document(chatRoomId).collection(Table.GroupMember.name)
            .document(AuthManager.currentUserId()!!)
            .set(GroupMember(AuthManager.currentUserId()!!, status)).await()
    }

    // This function handle update status of group message ( Receiving or Seen)
    private fun updateUnseenMessagesToSeen(chatRoomId: String, unseenMessages: List<GroupMessage>) {
        val currentUserId = AuthManager.currentUserId()!!

        // Create a coroutine scope for parallel processing
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            // Split unseen messages into batches of 500
            val batchSize = 250
            val batches = unseenMessages.chunked(batchSize)

            // Pre-fetch all senderRecentChatData
            val senderRecentChatDataMap = unseenMessages.map { message ->
                async {
                    message.messageId to userRef.document(message.senderId!!)
                        .collection(Table.RecentChat.name)
                        .document(chatRoomId).get().await().toObject<RecentChat>()
                }
            }.awaitAll().toMap()

            // Track all batch update jobs
            val batchJobs = batches.map { batch ->
                async {
                    // Create a batch update
                    firestore.runBatch { batchWrite ->
                        batch.forEach { message ->
                            val messageRef = chatRef.document(chatRoomId)
                                .collection(Table.Messages.name).document(message.messageId!!)
                            val updatedMessageData =
                                createUpdatedMessageData(message, currentUserId, message.senderId!!)
                            batchWrite.update(messageRef, updatedMessageData.toNormalMap())

                            // Check if recent chat update is needed
                            val senderRecentChatData = senderRecentChatDataMap[message.messageId]
                            if (senderRecentChatData != null &&
                                message.messageSentTimeInTimeStamp == senderRecentChatData.lastMessageTimeInTimeStamp
                            ) {
                                val senderRecentChatRef =
                                    userRef.document(message.senderId)
                                        .collection(Table.RecentChat.name).document(chatRoomId)
                                batchWrite.update(
                                    senderRecentChatRef,
                                    Constants.RecentChatTable.LAST_MESSAGE_SEEN.fieldName,
                                    updatedMessageData.seenStatus
                                )
                            }
                        }
                    }.await() // Wait for the batch to complete
                }
            }

            // Wait for all batch jobs to complete
            batchJobs.awaitAll()

            // Update UNSEEN_MESSAGE_COUNT after all batches are done
            val myRecentChatRef = userRef.document(currentUserId)
                .collection(Table.RecentChat.name).document(chatRoomId)
            val isMyRecentChatRefExist = myRecentChatRef.isExist()

            firestore.runBatch { batchWrite ->
                batchWrite.safeUpdate(
                    myRecentChatRef,
                    isMyRecentChatRefExist,
                    Constants.RecentChatTable.UNSEEN_MESSAGE_COUNT.fieldName,
                    0
                )
            }.await() // Wait for the update to complete

            println("All unseen messages updated and UNSEEN_MESSAGE_COUNT reset.")
        }
    }

    private fun createUpdatedMessageData(
        message: GroupMessage,
        currentUserId: String,
        senderId: String
    ): GroupMessage {
        val tempMessageReceivedByUsers =
            message.messageReceivedByUsers?.toMutableList() ?: mutableListOf()
        val tempMessageSeenByUsers = message.messageSeenByUsers?.toMutableList() ?: mutableListOf()

        if (!tempMessageReceivedByUsers.contains(currentUserId)) {
            tempMessageReceivedByUsers.add(currentUserId)
        }

        if (!tempMessageSeenByUsers.contains(currentUserId)) {
            tempMessageSeenByUsers.add(currentUserId)
        }

        val isMessageSeenByAllMembers = message.sendTimeUsers?.filter { it != senderId }?.all {
            tempMessageSeenByUsers.contains(it)
        } ?: false

        val tempStatus = when {
            isMessageSeenByAllMembers -> Constants.SeenStatus.MessageSeen.status
            tempMessageReceivedByUsers.contains(currentUserId) -> Constants.SeenStatus.Received.status
            else -> message.seenStatus!!
        }

        return message.copy(
            messageReceivedByUsers = tempMessageReceivedByUsers,
            messageSeenByUsers = tempMessageSeenByUsers,
            seenStatus = tempStatus
        )
    }


    // This is only for receiving chat by fcm
    fun updateGroupReceivedStatus(
        messageId: String,
        chatRoomId: String,
        senderId: String
    ) {
        try {
            launchCoroutineInIOThread {
                val messageRef =
                    chatRef.document(chatRoomId).collection(Table.Messages.name).document(messageId)
                val isMessageRefExist = messageRef.isExist()
                val messageData =
                    messageRef.get().await().toObject<GroupMessage>()
                        ?: return@launchCoroutineInIOThread
                val myRecentChatRef =
                    userRef.document(AuthManager.currentUserId()!!)
                        .collection(Table.RecentChat.name)
                        .document(chatRoomId)
                val senderRecentChatRef =
                    userRef.document(senderId).collection(Table.RecentChat.name)
                        .document(chatRoomId)

                val isRecentChatUpdateNeeded = async {
                    val senderRecentChatData =
                        userRef.document(senderId).collection(Table.RecentChat.name)
                            .document(chatRoomId).get().await().toObject<RecentChat>()
                            ?: return@async false

                    return@async messageData.messageSentTimeInTimeStamp == senderRecentChatData.lastMessageTimeInTimeStamp
                }.await()

                // Ensure the current user is in the received list if the status is Seen
                val tempMessageReceivedByUsers =
                    messageData.messageReceivedByUsers?.toMutableList() ?: mutableListOf()
                if (!tempMessageReceivedByUsers.contains(AuthManager.currentUserId()!!)) {
                    tempMessageReceivedByUsers.add(AuthManager.currentUserId()!!)
                }


                val isMessageReceivedByAllMembers =
                    messageData.sendTimeUsers?.filter { it != senderId }?.all {
                        tempMessageReceivedByUsers.contains(it)
                    } ?: false

                val tempStatus = when {
                    isMessageReceivedByAllMembers -> Constants.SeenStatus.Received.status
                    else -> messageData.seenStatus!!
                }

                val updatedMessageData = messageData.copy(
                    messageReceivedByUsers = tempMessageReceivedByUsers,
                    seenStatus = tempStatus
                )

                firestore.runBatch { batch ->
                    batch.safeUpdate(messageRef, updatedMessageData.toNormalMap() , isMessageRefExist)
                    if (isRecentChatUpdateNeeded) {
                        batch.update(
                            senderRecentChatRef,
                            Constants.RecentChatTable.LAST_MESSAGE_SEEN.fieldName,
                            tempStatus
                        )
                    }
                }.await()
            }
        } catch (e: FirebaseFirestoreException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


//endregion


}
private typealias MessageType = Constants.PostType
