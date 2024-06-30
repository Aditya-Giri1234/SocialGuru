package com.aditya.socialguru.domain_layer.service.firebase_service

import com.aditya.socialguru.data_layer.model.chat.ChatMediaData
import com.aditya.socialguru.data_layer.model.chat.ChatMuteNotificationData
import com.aditya.socialguru.data_layer.model.chat.LastMessage
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.data_layer.model.chat.RecentChat
import com.aditya.socialguru.data_layer.model.chat.UpdateChatResponse
import com.aditya.socialguru.data_layer.model.chat.UserRecentModel
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.NotificationType
import com.aditya.socialguru.domain_layer.helper.Constants.Table
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.helper.toNormalMap
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.NotificationSendingManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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

    suspend fun sentMessage(
        message: Message,
        lastMessage: LastMessage,
        chatRoomId: String,
        isUserOnline: Boolean = false
    ) = callbackFlow<UpdateChatResponse> {

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
                        uploadImage(updatedMessage, lastMessage, chatRoomId, isUserOnline).onEach {
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
        isUserOnline: Boolean = false
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
            val receiverRecentRef =
                userRef.document(userId).collection(Table.RecentChat.name).document(chatRoomId)
            val myRecentChatRef =
                userRef.document(AuthManager.currentUserId()!!).collection(Table.RecentChat.name)
                    .document(chatRoomId)

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
                batch.update(lastMessageRef,
                    lastMessage!!.toNormalMap().filterValues { it != null })
                batch.update(receiverRecentRef, forReceiverRecentData.toNormalMap())
                batch.update(myRecentChatRef, forMyRecentData.toNormalMap())
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
                } catch (e: Exception) {
                    e.printStackTrace()
                    trySend(UpdateResponse(false, e.message)) // Failed to delete all documents
                }
            }
            awaitClose {
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
            firestore.runBatch {
                it.update(messageRef, Constants.MessageTable.SEEN_STATUS.fieldName, status)
                it.update(
                    myRecentChatRef, Constants.RecentChatTable.LAST_MESSAGE_SEEN.fieldName, status
                )
                it.update(
                    receiverRecentChatRef,
                    Constants.RecentChatTable.LAST_MESSAGE_SEEN.fieldName,
                    status
                )
                if (status == Constants.SeenStatus.MessageSeen.status) {
                    it.update(
                        myRecentChatRef, Constants.RecentChatTable.UNSEEN_MESSAGE_COUNT.fieldName, 0
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
        val lastMessageRef = chatRef.document(chatRoomId)
        if (isIAmUser1) {
            lastMessageRef.update(Constants.LastMessageTable.IS_USER_1_ONLINE.fieldName, status)
        } else {
            lastMessageRef.update(Constants.LastMessageTable.IS_USER_2_ONLINE.fieldName, status)
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
                recentChat.userId?.let {
                    UserManager.getUserById(it)?.let {
                        UserRecentModel(it, recentChat)
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
                    when (it.type) {
                        DocumentChange.Type.ADDED -> {
                            launchCoroutineInIOThread {
                                val recentChat = it.document.toObject<RecentChat>()
                                trySend(
                                    ListenerEmissionType(Constants.ListenerEmitType.Added,
                                        singleResponse = recentChat.userId?.let {
                                            UserManager.getUserById(it)?.let { user ->
                                                UserRecentModel(user, recentChat)
                                            }
                                        })
                                )
                            }
                        }

                        DocumentChange.Type.MODIFIED -> {
                            launchCoroutineInIOThread {
                                val recentChat = it.document.toObject<RecentChat>()
                                trySend(
                                    ListenerEmissionType(Constants.ListenerEmitType.Modify,
                                        singleResponse = recentChat.userId?.let {
                                            UserManager.getUserById(it)?.let { user ->
                                                UserRecentModel(user, recentChat)
                                            }
                                        })
                                )
                            }

                        }

                        DocumentChange.Type.REMOVED -> {
                            launchCoroutineInIOThread {
                                val recentChat = it.document.toObject<RecentChat>()
                                trySend(
                                    ListenerEmissionType(Constants.ListenerEmitType.Removed,
                                        singleResponse = recentChat.userId?.let {
                                            UserManager.getUserById(it)?.let { user ->
                                                UserRecentModel(user, recentChat)
                                            }
                                        })
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
        emit(chatRef.document(chatRoomId).collection(Table.Media.name).get().await().toObjects(ChatMediaData::class.java))
    }

}
private typealias MessageType = Constants.PostType
