package com.aditya.socialguru.domain_layer.service.firebase_service

import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.UserSetting
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.data_layer.model.notification.UserNotificationModel
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.data_layer.model.post.comment.CommentedPost
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.data_layer.model.user_action.UserRelationshipStatus
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.Table
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.NotificationSendingManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.core.OrderBy
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.google.rpc.context.AttributeContext.Auth
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


/**
 * [UserManager] This class only do crud  operation on firebase firestore [com.aditya.socialguru.data_layer.model.User] collection and its subCollection.
 * */
object UserManager {

    private val tagLogin = Constants.LogTag.LogIn
    private val tagProfile = Constants.LogTag.Profile
    private val tagDelete = Constants.LogTag.ForceLogout


    //Firebase firestore
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val userRef by lazy {
        firestore.collection(Constants.Table.User.name)
    }

    private var currentUserListener: ListenerRegistration? = null

    private var followerListListener: ListenerRegistration? = null
    private var isFirstTimeFollowerListListener = true

    private var followingListListener: ListenerRegistration? = null
    private var isFirstTimeFollowingListListener = true

    private var friendListListener: ListenerRegistration? = null
    private var isFirstTimeFriendListListener = true

    private var friendRequestListListener: ListenerRegistration? = null
    private var isFirstTimeFriendRequestListListener = true

    private var myNotificationListener: ListenerRegistration? = null
    private var isFirstTimeMyNotificationListener = true

    private val userRelationListeners = mutableListOf<ListenerRegistration>()
    private var isFirstTimeUserStatusUpdateListListener = true


    suspend fun saveUser(user: User): Pair<Boolean, String?> {
        MyLogger.v(tagLogin, isFunctionCall = true)
        return try {
            firestore.collection(Constants.Table.User.name).document(user.userId!!).set(user)
                .await()
            MyLogger.d(tagLogin, msg = "User saved successfully without error")
            Pair(true, null)
        } catch (e: Exception) {
            e.printStackTrace()
            MyLogger.e(tagLogin, msg = "Some error occurred during save user :- ${e.message}")
            Pair(false, e.message)
        }
    }

    suspend fun updateUser(
        user: User,
        deleteImage: String? = null,
        uploadingImage: String? = null
    ) = callbackFlow<UpdateResponse> {

        if (deleteImage != null) {
            StorageManager.deleteImageFromServer(deleteImage).onEach {
                if (it.isSuccess) {
                    MyLogger.i(Constants.LogTag.Profile, msg = "Image Delete Successfully !")
                } else {
                    MyLogger.e(
                        Constants.LogTag.Profile,
                        msg = giveMeErrorMessage(
                            "Delete Profile Pic",
                            it.errorMessage?.toString() ?: ""
                        )
                    )
                }

            }.launchIn(this)
        }

        if (uploadingImage != null) {
            StorageManager.uploadImageToServer(
                folderName = Constants.FolderName.ProfilePic.name,
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
                        val newUser = user.copy(
                            userProfileImage = onlineImageUri
                        )
                        val (isUpdate, error) = saveUser(newUser)
                        if (isUpdate) {
                            trySend(UpdateResponse(true, ""))
                        } else {
                            trySend(UpdateResponse(false, error))
                        }
                    }
                }

            }.launchIn(this)
        } else {
            val (isUpdate, error) = saveUser(user)
            if (isUpdate) {
                trySend(UpdateResponse(true, ""))
            } else {
                trySend(UpdateResponse(false, error))
            }
        }


        awaitClose {
            close()
        }
    }

    suspend fun getUserByIdAsync(userId: String): Flow<Resource<User>> {
        return callbackFlow {
            firestore.collection(Constants.Table.User.name).document(userId)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        trySend(Resource.Error(error.message))
                        return@addSnapshotListener
                    }
                    value?.let { documentSnapshot ->
                        try {
                            val user = documentSnapshot.toObject(User::class.java)

                            if (user != null) {
                                trySend(Resource.Success(user))
                            } else {
                                trySend(Resource.Error("User Not Found !"))
                            }

                        } catch (e: Exception) {
                            trySend(Resource.Error("Error getting user: ${e.message}"))
                        }
                    }
                }


            awaitClose {
                channel.close()
            }
        }

    }


    suspend fun getUserById(userId: String): User? {
        val docRef = firestore.collection(Constants.Table.User.name).document(userId)
        return try {
            docRef.get().await().toObject(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAllUserByIds(userIds: List<String>) = callbackFlow<List<FriendCircleData>> {
        try {
            val userListWork = userIds.map {
                async {
                    getUserById(it)
                }
            }
            val userList = userListWork.awaitAll().filterNotNull().map {
                FriendCircleData(it.userId!!, user = it)
            }
            trySend(userList)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        awaitClose {
            close()
        }
    }

    private suspend fun getAllUser(): Flow<List<User>> {
        return callbackFlow {
            val docRef = firestore.collection(Constants.Table.User.name)
            val userList = arrayListOf<User>()

            try {
                userList.addAll(docRef.get().await().documents.mapNotNull {
                    it.toObject(User::class.java)
                }.toList())

                trySend(userList)

            } catch (e: Exception) {
                trySend(userList)
            }

            awaitClose {
                channel.close()
            }
        }
    }

    suspend fun setFcmToken(token: String?) = callbackFlow<UpdateResponse> {

        userRef.document(AuthManager.currentUserId()!!)
            .update(Constants.UserTable.FCM_TOKEN.fieldName, token).addOnSuccessListener {
                MyLogger.v(msg = "Fcm send successfully !")
                trySend(UpdateResponse(true, ""))
            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()


        awaitClose {
            close()
        }
    }

    suspend fun logoutUser() = callbackFlow<UpdateResponse> {
        getUserById(AuthManager.currentUserId()!!)?.let {
            val timestamp = System.currentTimeMillis()
            val timeInText = Helper.formatTimestampToDateAndTime(timestamp)
            val updatedUser = it.copy(
                fcmToken = "",
                userAvailable = false,
                logoutTimeInText = timeInText,
                logoutTimeInTimeStamp = timestamp
            )
            userRef.document(AuthManager.currentUserId()!!).set(updatedUser).addOnSuccessListener {
                trySend(UpdateResponse(true, ""))
            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()
        } ?: trySend(UpdateResponse(false, "User Not Found"))
        awaitClose {
            close()
        }
    }


    //region:: Notification Related work here

    suspend fun getMyNotificationAndListen() =
        callbackFlow<List<ListenerEmissionType<UserNotificationModel, UserNotificationModel>>> {

            val notificationQuery = userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.Notification.name)

            myNotificationListener?.remove()
            myNotificationListener = notificationQuery.addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener


                value?.documentChanges?.let { data ->
                    launch {
                        // Define the chunk size, e.g., 20 documents per batch
                        val chunkSize = 20
                        val documentChunks = data.chunked(chunkSize)

                        for (chunk in documentChunks) {
                            val savedList = chunk.map {
                                async {
                                    val notificationData = it.document.toObject<NotificationData>()
                                    val changeType = when (it.type) {
                                        DocumentChange.Type.ADDED -> Constants.ListenerEmitType.Added
                                        DocumentChange.Type.MODIFIED -> Constants.ListenerEmitType.Modify
                                        DocumentChange.Type.REMOVED -> Constants.ListenerEmitType.Removed
                                    }

                                    val userTask = userRef.document(notificationData.friendOrFollowerId!!).get().await()
                                    if (!userTask.exists()) return@async null

                                    if (changeType == Constants.ListenerEmitType.Added) {
                                        ListenerEmissionType<UserNotificationModel,UserNotificationModel>(
                                            changeType, singleResponse = UserNotificationModel(user = userTask.toObject()!!, notificationData = notificationData)
                                        )
                                    } else {
                                        ListenerEmissionType(
                                            changeType, singleResponse = UserNotificationModel(user = userTask.toObject()!!, notificationData = notificationData)
                                        )
                                    }
                                }
                            }.awaitAll().filterNotNull()

                            // Send the processed chunk to the flow
                            if (savedList.isNotEmpty()) {
                                trySend(savedList)
                            }
                        }
                    }
                }
            }


            // Remember this callback listen in viewmodel scope means until viewmodel destroyed below part not called.
            awaitClose {
                myNotificationListener?.remove()
                close()
            }
        }

    suspend fun deleteSingleNotification(notificationId: String) = callbackFlow<UpdateResponse> {
        userRef.document(AuthManager.currentUserId()!!)
            .collection(Constants.Table.Notification.name).document(notificationId).delete()
            .addOnSuccessListener {
                trySend(UpdateResponse(true, ""))
            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()
        awaitClose {
            close()
        }
    }

    suspend fun deleteAllNotification() = callbackFlow<UpdateResponse> {

        val notificationRef = userRef.document(AuthManager.currentUserId()!!)
            .collection(Constants.Table.Notification.name)

        try {
            // Get all documents in the Notification collection
            val querySnapshot = notificationRef.get().await()
            val batch = firestore.batch()

            // Add delete operations to the batch
            for (document in querySnapshot.documents) {
                batch.delete(document.reference)
            }

            // Commit the batch
            batch.commit().addOnSuccessListener {
                trySend(UpdateResponse(true, ""))
            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()

        } catch (e: Exception) {
            trySend(UpdateResponse(false, e.message))
        }

        awaitClose {
            close()
        }
    }

    //endregion

    //region:: Listen user relation

    /**
     *This help to update status on  button of follow and friend by listening change below sub collection
     */
    fun listenUserRelationStatus(friendId: String) = callbackFlow<UpdateResponse> {
        removeUserStatusListeners()
        val collectionList = listOf(
            Constants.Table.Following.name,
            Constants.Table.Friend.name,
            Constants.Table.FriendRequest.name,
            Constants.Table.PendingRequest.name
        )
        val friendRef = userRef.document(AuthManager.currentUserId()!!)
        collectionList.forEachIndexed { index, name ->
            val listener = friendRef.collection(name).addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener


                //These is for prevent multiple time userStatusFind when profile view just initialized
                if (isFirstTimeUserStatusUpdateListListener) {
                    if (index == collectionList.size - 1) {
                        isFirstTimeUserStatusUpdateListListener = false
                    }
                    return@addSnapshotListener
                }
                value?.documentChanges?.forEach {
                    when (it.type) {
                        DocumentChange.Type.ADDED -> {
                            if (it.document.id == friendId) {
                                trySend(UpdateResponse(true, ""))
                            }
                        }

                        DocumentChange.Type.MODIFIED -> {}
                        DocumentChange.Type.REMOVED -> {
                            if (it.document.id == friendId) {
                                trySend(UpdateResponse(true, ""))
                            }
                        }
                    }
                }
            }
            userRelationListeners.add(listener)
        }

        awaitClose {
            removeUserStatusListeners()
            close()
        }

    }

    private fun removeUserStatusListeners() {
        isFirstTimeUserStatusUpdateListListener = true
        userRelationListeners.forEach { it.remove() }
        userRelationListeners.clear()
    }

    fun listenFriendRequestComeEvent() =
        callbackFlow<ListenerEmissionType<FriendCircleData, FriendCircleData>> {

            val friendRequestRef = userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.FriendRequest.name)
            var friendRequestList = friendRequestRef.get().await().toObjects<FriendCircleData>()

            friendRequestList = friendRequestList.map {
                it.copy(
                    user = it.userId?.let { it1 -> getUserById(it1) }
                )
            }.toMutableList()

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting,
                    responseList = friendRequestList.toList()
                )
            )

            friendRequestList.clear()
            friendRequestListListener?.remove()
            friendRequestListListener = friendRequestRef.addSnapshotListener { value, error ->

                if (error != null) return@addSnapshotListener

                if (value != null) {
                    if (isFirstTimeFriendRequestListListener) {
                        isFirstTimeFriendRequestListListener = false
                        return@addSnapshotListener
                    }

                    value.documentChanges.forEach {
                        when (it.type) {
                            DocumentChange.Type.ADDED -> {
                                launch {
                                    trySend(
                                        ListenerEmissionType(
                                            Constants.ListenerEmitType.Added,
                                            singleResponse = it.document.toObject<FriendCircleData>()
                                                .apply {
                                                    user = getUserById(it.document.id)
                                                }
                                        )
                                    )
                                }
                            }

                            DocumentChange.Type.REMOVED -> {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Removed,
                                        singleResponse = it.document.toObject<FriendCircleData>()
                                    )
                                )
                            }

                            DocumentChange.Type.MODIFIED -> {}
                        }
                    }
                }

            }

            awaitClose {
                isFirstTimeFriendRequestListListener = true
                friendRequestListListener?.remove()
                close()
            }
        }


    //endregion

    //region:: User  Action (Follow, Following, Friend)
    suspend fun subscribeToCurrentUserData(userId: String) = callbackFlow<User?> {

        currentUserListener?.remove()

        currentUserListener = userRef.document(userId).addSnapshotListener { value, error ->

            if (error != null) trySend(null)

            trySend(value?.toObject<User>())
        }

        awaitClose {
            close()
        }
    }

    suspend fun getFollowerListAndListenChange(userId: String) =
        callbackFlow<ListenerEmissionType<FriendCircleData, FriendCircleData>> {

            val followerRef = userRef.document(userId).collection(Constants.Table.Follower.name)

            var followerList =
                followerRef.get().await().toObjects(FriendCircleData::class.java)
            followerList = followerList.map {
                it.copy(
                    user = it.userId?.let { it1 -> getUserById(it1) }
                )
            }.toMutableList()

            followerList.sortBy {
                it.timeStamp
            }

            MyLogger.v(
                Constants.LogTag.Profile,
                msg = followerList,
                isJson = true,
                jsonTitle = "Follower List "
            )

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting,
                    responseList = followerList.toList()
                )
            )

            followerList.clear()
            followerListListener?.remove()
            followerListListener = followerRef.addSnapshotListener { value, error ->

                if (error != null) return@addSnapshotListener

                if (value != null) {
                    if (isFirstTimeFollowerListListener) {
                        isFirstTimeFollowerListListener = false
                        return@addSnapshotListener
                    }

                    value.documentChanges.forEach {
                        when (it.type) {
                            DocumentChange.Type.ADDED -> {
                                launch {
                                    trySend(
                                        ListenerEmissionType(
                                            Constants.ListenerEmitType.Added,
                                            singleResponse = it.document.toObject<FriendCircleData>()
                                                .apply {
                                                    user = getUserById(it.document.id)
                                                }
                                        )
                                    )
                                }
                            }

                            DocumentChange.Type.REMOVED -> {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Removed,
                                        singleResponse = it.document.toObject<FriendCircleData>()
                                    )
                                )
                            }

                            DocumentChange.Type.MODIFIED -> {}
                        }
                    }
                }

            }




            awaitClose {
                isFirstTimeFollowerListListener =
                    true// This is singleton variable if function again call then this variable contain old value so that reset here
                followerListListener?.remove()
                close()
            }
        }

    suspend fun getFollowingListAndListenChange(userId: String) =
        callbackFlow<ListenerEmissionType<FriendCircleData, FriendCircleData>> {

            MyLogger.d(tagProfile, msg = "User id :- $userId")
            val followingRef = userRef.document(userId).collection(Constants.Table.Following.name)

            var followingList = followingRef.get().await().map {
                MyLogger.v(tagProfile, msg = it.toObject<FriendCircleData>(), isJson = true)
                it.toObject<FriendCircleData>()
            }

            MyLogger.v(tagProfile, msg = followingList, isJson = true)
            followingList = followingList.map {
                MyLogger.v(tagProfile, msg = it, isJson = true)
                it.copy(
                    user = it.userId?.let { it1 -> getUserById(it1) }
                )

            }.toMutableList()

            followingList.sortBy {
                it.timeStamp
            }
            MyLogger.v(tagProfile, msg = followingList, isJson = true, jsonTitle = "Following List")

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting,
                    responseList = followingList.toList()
                )
            )

            followingList.clear()
            followingListListener?.remove()
            followingListListener = followingRef.addSnapshotListener { value, error ->

                if (error != null) return@addSnapshotListener

                if (value != null) {
                    if (isFirstTimeFollowingListListener) {
                        isFirstTimeFollowingListListener = false
                        return@addSnapshotListener
                    }

                    value.documentChanges.forEach {
                        when (it.type) {
                            DocumentChange.Type.ADDED -> {
                                launch {
                                    trySend(
                                        ListenerEmissionType(
                                            Constants.ListenerEmitType.Added,
                                            singleResponse = it.document.toObject<FriendCircleData>()
                                                .let {
                                                    it.copy(
                                                        user = it.userId?.let { getUserById(it) }
                                                    )
                                                }

                                        )
                                    )
                                }

                            }

                            DocumentChange.Type.REMOVED -> {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Removed,
                                        singleResponse = it.document.toObject<FriendCircleData>()
                                    )
                                )
                            }

                            DocumentChange.Type.MODIFIED -> {}
                        }
                    }
                }

            }




            awaitClose {
                isFirstTimeFollowingListListener =
                    true// This is singleton variable if function again call then this variable contain old value so that reset here
                followingListListener?.remove()
                close()
            }
        }

    suspend fun getFriendListAndListenChange(userId: String) =
        callbackFlow<ListenerEmissionType<FriendCircleData, FriendCircleData>> {

            val friendRef = userRef.document(userId).collection(Constants.Table.Friend.name)

            var friendList =
                friendRef.get().await().toObjects(FriendCircleData::class.java)
            friendList = friendList.map {
                it.copy(
                    user = it.userId?.let { it1 -> getUserById(it1) }
                )
            }.toMutableList()

            friendList.sortBy {
                it.timeStamp
            }

            MyLogger.v(
                Constants.LogTag.Profile,
                msg = friendList,
                isJson = true,
                jsonTitle = "Friend List "
            )

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting,
                    responseList = friendList.toList()
                )
            )

            friendList.clear()
            friendListListener?.remove()
            friendListListener = friendRef.addSnapshotListener { value, error ->

                if (error != null) return@addSnapshotListener

                if (value != null) {
                    if (isFirstTimeFriendListListener) {
                        isFirstTimeFriendListListener = false
                        return@addSnapshotListener
                    }

                    value.documentChanges.forEach {
                        when (it.type) {
                            DocumentChange.Type.ADDED -> {
                                launch {
                                    trySend(
                                        ListenerEmissionType(
                                            Constants.ListenerEmitType.Added,
                                            singleResponse = it.document.toObject<FriendCircleData>()
                                                .run {
                                                    copy(user = this.userId?.let { it1 ->
                                                        getUserById(
                                                            it1
                                                        )
                                                    })
                                                }
                                        )
                                    )
                                }
                            }

                            DocumentChange.Type.REMOVED -> {

                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Removed,
                                        singleResponse = it.document.toObject<FriendCircleData>()
                                    )
                                )

                            }

                            DocumentChange.Type.MODIFIED -> {}
                        }
                    }
                }

            }




            awaitClose {
                isFirstTimeFriendListListener =
                    true// This is singleton variable if function again call then this variable contain old value so that reset here
                friendListListener?.remove()
                close()
            }
        }

    suspend fun removeFollower(userId: String) = callbackFlow<UpdateResponse> {
        val followerRef = userRef.document(AuthManager.currentUserId()!!)
            .collection(Constants.Table.Follower.name).document(userId)
        val followingRef = userRef.document(userId).collection(Constants.Table.Following.name)
            .document(AuthManager.currentUserId()!!)

        firestore.runBatch { batch ->
            batch.delete(followingRef)
            batch.delete(followerRef)
        }.addOnSuccessListener {
            trySend(UpdateResponse(true, ""))
        }.addOnFailureListener {
            trySend(UpdateResponse(false, it.message.toString()))
        }.await()



        awaitClose {
            close()
        }
    }

    suspend fun unFollow(userId: String) = callbackFlow<UpdateResponse> {
        val followingRef = userRef.document(AuthManager.currentUserId()!!)
            .collection(Constants.Table.Following.name).document(userId)
        val followerRef = userRef.document(userId).collection(Constants.Table.Follower.name)
            .document(AuthManager.currentUserId()!!)

        firestore.runBatch { batch ->
            batch.delete(followingRef)
            batch.delete(followerRef)
        }.addOnSuccessListener {
            trySend(UpdateResponse(true, ""))
        }.addOnFailureListener {
            trySend(UpdateResponse(false, it.message.toString()))
        }.await()



        awaitClose {
            close()
        }
    }

    suspend fun removeFriend(userId: String) = callbackFlow<UpdateResponse> {
        val currentUserFriendRef =
            userRef.document(AuthManager.currentUserId()!!).collection(Constants.Table.Friend.name)
                .document(userId)
        val removeFriendCurrentRef =
            userRef.document(userId).collection(Constants.Table.Friend.name)
                .document(AuthManager.currentUserId()!!)

        firestore.runBatch { batch ->
            batch.delete(currentUserFriendRef)
            batch.delete(removeFriendCurrentRef)
        }.addOnSuccessListener {
            trySend(UpdateResponse(true, ""))
        }.addOnFailureListener {
            trySend(UpdateResponse(false, it.message.toString()))
        }.await()



        awaitClose {
            close()
        }
    }

    suspend fun followUser(userId: String, followedId: String) = callbackFlow<UpdateResponse> {
        val timeStamp = System.currentTimeMillis()
        val followedData = FriendCircleData(followedId, timeStamp)
        val myData = FriendCircleData(userId, timeStamp)

        val notificationId = Helper.getNotificationId()
        val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
        val notificationData = NotificationData(
            notificationId = notificationId,
            friendOrFollowerId = userId,
            notificationTimeInText = timeInText,
            notificationTimeInTimeStamp = timeStamp.toString(),
            type = Constants.NotificationType.NEW_FOLLOWER.name
        )

        val myUserRef =
            userRef.document(userId).collection(Constants.Table.Following.name).document(followedId)
        val followedRef =
            userRef.document(followedId).collection(Constants.Table.Follower.name).document(userId)
        val notificationRef =
            userRef.document(followedId).collection(Constants.Table.Notification.name)
                .document(notificationId)

        firestore.runBatch { batch ->
            batch.set(myUserRef, followedData)
            batch.set(followedRef, myData)
            batch.set(notificationRef, notificationData)
        }.addOnSuccessListener {
            NotificationSendingManager.sendNotification(followedId, notificationData)
            trySend(UpdateResponse(true, ""))
        }.addOnFailureListener {
            trySend(UpdateResponse(false, it.message))
        }.await()


        awaitClose {
            close()
        }
    }

    suspend fun sendFriendRequest(userId: String, friendId: String) = callbackFlow<UpdateResponse> {
        val timeStamp = System.currentTimeMillis()
        val friendData = FriendCircleData(friendId, timeStamp)
        val myData = FriendCircleData(userId, timeStamp)

        val notificationId = Helper.getNotificationId()
        val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
        val notificationData = NotificationData(
            notificationId = notificationId,
            friendOrFollowerId = userId,
            notificationTimeInText = timeInText,
            notificationTimeInTimeStamp = timeStamp.toString(),
            type = Constants.NotificationType.FRIEND_REQUEST_COME.name
        )

        val myUserRef =
            userRef.document(userId).collection(Constants.Table.PendingRequest.name)
                .document(friendId)
        val friendRef =
            userRef.document(friendId).collection(Constants.Table.FriendRequest.name)
                .document(userId)
        val notificationRef =
            userRef.document(friendId).collection(Constants.Table.Notification.name)
                .document(notificationId)

        firestore.runBatch { batch ->
            batch.set(myUserRef, friendData)
            batch.set(friendRef, myData)
            batch.set(notificationRef, notificationData)
        }.addOnSuccessListener {
            NotificationSendingManager.sendNotification(friendId, notificationData)
            trySend(UpdateResponse(true, ""))
        }.addOnFailureListener {
            trySend(UpdateResponse(false, it.message))
        }.await()


        awaitClose {
            close()
        }
    }

    suspend fun deleteFriendRequest(userId: String, friendId: String) =
        callbackFlow<UpdateResponse> {

            val myUserRef =
                userRef.document(userId).collection(Constants.Table.PendingRequest.name)
                    .document(friendId)
            val friendRef =
                userRef.document(friendId).collection(Constants.Table.FriendRequest.name)
                    .document(userId)

            firestore.runBatch { batch ->
                batch.delete(myUserRef)
                batch.delete(friendRef)
            }.addOnSuccessListener {
                trySend(UpdateResponse(true, ""))
            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()


            awaitClose {
                close()
            }
        }

    suspend fun declineFriendRequest(userId: String, friendId: String) =
        callbackFlow<UpdateResponse> {

            val friendRef =
                userRef.document(friendId).collection(Constants.Table.PendingRequest.name)
                    .document(userId)
            val myUserRef =
                userRef.document(userId).collection(Constants.Table.FriendRequest.name)
                    .document(friendId)

            firestore.runBatch { batch ->
                batch.delete(myUserRef)
                batch.delete(friendRef)
            }.addOnSuccessListener {
                trySend(UpdateResponse(true, ""))
            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()


            awaitClose {
                close()
            }
        }

    suspend fun acceptFriendRequest(userId: String, friendId: String) =
        callbackFlow<UpdateResponse> {
            val timeStamp = System.currentTimeMillis()
            val friendData = FriendCircleData(friendId, timeStamp)
            val myData = FriendCircleData(userId, timeStamp)
            val notificationId = Helper.getNotificationId()
            val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
            val notificationData = NotificationData(
                notificationId = notificationId,
                friendOrFollowerId = userId,
                notificationTimeInText = timeInText,
                notificationTimeInTimeStamp = timeStamp.toString(),
                type = Constants.NotificationType.ACCEPT_FRIEND_REQUEST.name
            )

            val myPendingRef =
                userRef.document(friendId).collection(Constants.Table.PendingRequest.name)
                    .document(userId)
            val friendRequestRef =
                userRef.document(userId).collection(Constants.Table.FriendRequest.name)
                    .document(friendId)
            val myUserFriendRef =
                userRef.document(userId).collection(Constants.Table.Friend.name).document(friendId)
            val friendFriendRef =
                userRef.document(friendId).collection(Constants.Table.Friend.name).document(userId)
            val notificationRef =
                userRef.document(friendId).collection(Constants.Table.Notification.name)
                    .document(notificationId)

            firestore.runBatch { batch ->
                batch.delete(myPendingRef)
                batch.delete(friendRequestRef)
                batch.set(myUserFriendRef, friendData)
                batch.set(friendFriendRef, myData)
                batch.set(notificationRef, notificationData)
            }.addOnSuccessListener {
                NotificationSendingManager.sendNotification(friendId, notificationData)
                trySend(UpdateResponse(true, ""))
            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()


            awaitClose {
                close()
            }
        }

    suspend fun getUserRelationshipStatus(currentUserId: String, targetUserId: String) =
        callbackFlow<UserRelationshipStatus> {
            val isFollowing =
                isUserInCollection(currentUserId, targetUserId, Constants.Table.Following.name)
            val friendStatus = getFriendStatus(currentUserId, targetUserId)
            trySend(UserRelationshipStatus(isFollowing, friendStatus))
            awaitClose {
                close()
            }
        }

    private suspend fun getFriendStatus(
        currentUserId: String,
        targetUserId: String
    ): Constants.FriendStatus {
        return when {
            isUserInCollection(
                currentUserId,
                targetUserId,
                Constants.Table.Friend.name
            ) -> Constants.FriendStatus.FRIEND

            isUserInCollection(
                currentUserId,
                targetUserId,
                Constants.Table.FriendRequest.name
            ) -> Constants.FriendStatus.FRIEND_REQUEST

            isUserInCollection(
                currentUserId,
                targetUserId,
                Constants.Table.PendingRequest.name
            ) -> Constants.FriendStatus.PENDING_REQUEST

            else -> Constants.FriendStatus.NOT_FRIEND
        }
    }

    private suspend fun isUserInCollection(
        currentUserId: String,
        targetUserId: String,
        collectionName: String
    ): Boolean {
        return try {
            val documentSnapshot = userRef.document(currentUserId)
                .collection(collectionName)
                .document(targetUserId)
                .get()
                .await()
            documentSnapshot.exists()
        } catch (e: FirebaseFirestoreException) {
            e.printStackTrace()
            false
        }
    }


    //endregion

    //region:: Subscribe to follower count

    suspend fun subscribeToFollowerCount(userId: String) = callbackFlow<Int> {
        userRef.document(userId).collection(Constants.Table.Follower.name)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(0)
                }

                trySend(value?.size() ?: 0)

            }


        awaitClose {
            close()
        }
    }

    //endregion

    //region:: Subscribe to following count

    suspend fun subscribeToFollowingCount(userId: String) = callbackFlow<Int> {

        userRef.document(userId).collection(Constants.Table.Following.name)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    trySend(0)
                }

                trySend(value?.size() ?: 0)

            }
        awaitClose {
            close()
        }
    }

    //endregion

    //region:: Subscribe to post count
    suspend fun subscribeToPostCount(userId: String) = callbackFlow<Int> {

        val postRef = firestore.collection(Constants.Table.Post.name)
        postRef.whereEqualTo(Constants.PostTable.USER_ID.fieldName, userId)
            .addSnapshotListener { value, error ->
                if (error != null) trySend(0)

                trySend(value?.size() ?: 0)
            }

        awaitClose {
            close()
        }
    }


    //endregion

    //region:: Subscribe to like count
    suspend fun subscribeToLikeCount(userId: String) = callbackFlow<Int> {
        val postRef = firestore.collection(Constants.Table.Post.name)
        postRef.whereEqualTo(Constants.PostTable.USER_ID.fieldName, userId)
            .addSnapshotListener { value, error ->
                if (error != null) trySend(0)

                trySend(value?.documents?.mapNotNull { doc ->
                    doc.toObject<Post>()?.likeCount
                }?.sum() ?: 0)

            }

        awaitClose {
            close()
        }
    }

    //endregion

    //region:: Update UserAvailability

    suspend fun updateUserAvailability(isUserAvailable: Boolean) {
        userRef.document(AuthManager.currentUserId()!!)
            .update(Constants.UserTable.USER_AVAILABLE.fieldName, isUserAvailable).await()
    }

    //endregion


    //region :: Search User

    suspend fun findUser(query: String) = callbackFlow<List<FriendCircleData>> {


        val resultList = mutableListOf<FriendCircleData>()
        val lowerCaseQuery = query.lowercase()

        userRef
            .whereGreaterThanOrEqualTo(Constants.UserTable.USERNAME_LOWERCASE.fieldName, lowerCaseQuery)
            .whereLessThanOrEqualTo(Constants.UserTable.USERNAME_LOWERCASE.fieldName, lowerCaseQuery + "\uf8ff")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.toObject(User::class.java).let {
                        resultList.add(FriendCircleData(it.userId, user = it))
                    }
                }
                resultList.sortBy { it.user?.userName }
                trySend(resultList)
            }
            .addOnFailureListener { exception ->
                // Handle the error here
                trySend(emptyList())
            }
        awaitClose {
            close()
        }
    }

    //endregion

    //region:: Delete My id from follower and following and friend and friend request

    suspend fun deleteMyUserRelation() {
        MyLogger.e(tagDelete, isFunctionCall = true)
        launchCoroutineInIOThread {
            val deleteWorkFollower = async {
                deleteMyFollower()
            }
            val deleteWorkFollowing = async {
                deleteMyFollowing()
            }
            val deleteWorkFriend = async {
                deleteMyFriend()
            }
            val deleteWorkPendingRequest = async {
                deleteMyPendingRequest()
            }
            val deleteWorkFriendRequest = async {
                deleteMyFriendRequest()
            }
            deleteWorkFollower.await()
            deleteWorkFollowing.await()
            deleteWorkFriend.await()
            deleteWorkPendingRequest.await()
            deleteWorkFriendRequest.await()

        }.join()
    }

    private suspend fun deleteMyFollower() {
        MyLogger.e(tagDelete, isFunctionCall = true)
        val myFollowerList = userRef.document(AuthManager.currentUserId()!!)
            .collection(Constants.Table.Follower.name).get().await()
        if (myFollowerList.isEmpty) return
        launchCoroutineInIOThread {
            MyLogger.w(tagDelete, msg = "Delete My Follower Start ....")
            val deleteWork = myFollowerList.map {
                async {
                    userRef.document(it.id).collection(Table.Following.name)
                        .document(AuthManager.currentUserId()!!).delete()
                }
            }
            deleteWork.awaitAll()
            MyLogger.w(tagDelete, msg = "Delete My Follower Done ....")
        }
    }

    private suspend fun deleteMyFollowing() {
        MyLogger.e(tagDelete, isFunctionCall = true)
        val myFollowingList = userRef.document(AuthManager.currentUserId()!!)
            .collection(Constants.Table.Following.name).get().await()
        if (myFollowingList.isEmpty) return
        launchCoroutineInIOThread {
            MyLogger.w(tagDelete, msg = "Delete My Following Start ....")
            val deleteWork = myFollowingList.map {
                async {
                    userRef.document(it.id).collection(Table.Follower.name)
                        .document(AuthManager.currentUserId()!!).delete()
                }
            }
            deleteWork.awaitAll()
            MyLogger.w(tagDelete, msg = "Delete My Following Done ....")
        }
    }

    private suspend fun deleteMyFriend() {
        MyLogger.e(tagDelete, isFunctionCall = true)
        val myFriendList =
            userRef.document(AuthManager.currentUserId()!!).collection(Constants.Table.Friend.name)
                .get().await()
        if (myFriendList.isEmpty) return
        launchCoroutineInIOThread {
            MyLogger.w(tagDelete, msg = "Delete My Friend Start ....")
            val deleteWork = myFriendList.map {
                async {
                    userRef.document(it.id).collection(Table.Friend.name)
                        .document(AuthManager.currentUserId()!!).delete()
                }
            }
            deleteWork.awaitAll()
            MyLogger.w(tagDelete, msg = "Delete My Friend Done ....")
        }
    }

    private suspend fun deleteMyPendingRequest() {
        MyLogger.e(tagDelete, isFunctionCall = true)
        val myPendingRequestList = userRef.document(AuthManager.currentUserId()!!)
            .collection(Constants.Table.PendingRequest.name).get().await()
        if (myPendingRequestList.isEmpty) return
        launchCoroutineInIOThread {
            MyLogger.w(tagDelete, msg = "Delete My Pending Request Start ....")
            val deleteWork = myPendingRequestList.map {
                async {
                    userRef.document(it.id).collection(Table.FriendRequest.name)
                        .document(AuthManager.currentUserId()!!).delete()
                }
            }
            deleteWork.awaitAll()
            MyLogger.w(tagDelete, msg = "Delete My Pending Request Done ....")
        }
    }

    private suspend fun deleteMyFriendRequest() {
        MyLogger.e(tagDelete, isFunctionCall = true)
        val myPendingRequestList = userRef.document(AuthManager.currentUserId()!!)
            .collection(Constants.Table.FriendRequest.name).get().await()
        if (myPendingRequestList.isEmpty) return
        launchCoroutineInIOThread {
            MyLogger.w(tagDelete, msg = "Delete My Friend Request Start ....")
            val deleteWork = myPendingRequestList.map {
                async {
                    userRef.document(it.id).collection(Table.PendingRequest.name)
                        .document(AuthManager.currentUserId()!!).delete()
                }
            }
            deleteWork.awaitAll()
            MyLogger.w(tagDelete, msg = "Delete My Friend Request Done ....")
        }
    }

    //endregion

    //region:: Delete All My Sub Collection

    suspend fun deleteAllMySubCollection() = callbackFlow<UpdateResponse> {
        MyLogger.e(tagDelete, isFunctionCall = true)
        val collectionRefs = listOf(
            userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.Follower.name),
            userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.Following.name),
            userRef.document(AuthManager.currentUserId()!!).collection(Constants.Table.Friend.name),
            userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.FriendRequest.name),
            userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.PendingRequest.name),
            userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.Notification.name),
            userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.MyLikedPost.name),
            userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.SavedPost.name),
            userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.LikedPost.name),
            userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.UnSeenMessage.name),
            userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.ChatMuteNotification.name),
            userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.RecentChat.name),
            userRef.document(AuthManager.currentUserId()!!)
                .collection(Constants.Table.CommentedPost.name)
        )
        deleteCollection(collectionRefs).onEach {
            MyLogger.i(tagDelete, msg = "All Collection is deleted !")
            trySend(it)
        }.launchIn(this)

        awaitClose {
            close()
        }
    }

     suspend fun deleteCollection(collectionRefs: List<CollectionReference>) =
        callbackFlow<UpdateResponse> {
            MyLogger.e(tagDelete, isFunctionCall = true)
            try {
                MyLogger.w(tagDelete, msg = "Delete Delete Collection Start ....")
                collectionRefs.map { collectionRef ->
                    async {
                        MyLogger.w(
                            tagDelete,
                            msg = "Delete collectionRef :- $collectionRef Start ...."
                        )
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
                        MyLogger.w(
                            tagDelete,
                            msg = "Delete collectionRef :- $collectionRef End ...."
                        )
                    }
                }.awaitAll()
                MyLogger.w(tagDelete, msg = "Delete Delete Collection End ....")
                trySend(UpdateResponse(true, "")) // All documents deleted successfully
            } catch (e: FirebaseFirestoreException) {
                e.printStackTrace()
                trySend(UpdateResponse(false, e.message)) // Failed to delete all documents
            } catch (e: Exception) {
                e.printStackTrace()
                trySend(UpdateResponse(false, e.message)) // Failed to delete all documents
            }
            awaitClose {
                close()
            }
        }



    //endregion

    //region:: User Setting

    suspend fun updateUserSetting(userSetting: UserSetting) = callbackFlow<UpdateResponse> {
        try {
            userRef.document(AuthManager.currentUserId()!!).update(Constants.UserTable.USER_SETTING.fieldName ,userSetting).addOnCompleteListener {
                if (it.isSuccessful){
                    trySend(UpdateResponse(true,""))
                }else{
                    trySend(UpdateResponse(false ,it.exception?.message.toString()))
                }
            }
        }catch (e:Exception){
            e.printStackTrace()
            trySend(UpdateResponse(true , ""))
        }
        awaitClose {
            close()
        }
    }

     suspend fun updateUserEmail(newEmailId:String) = callbackFlow<UpdateResponse> {

        userRef.document(AuthManager.currentUserId()!!).update(Constants.UserTable.USER_EMAIL_ID.fieldName ,newEmailId).addOnCompleteListener {
            if (it.isSuccessful){
                trySend(UpdateResponse(true ,""))
            }else{
                trySend(UpdateResponse(false, it.exception?.toString()))
            }
        }

        awaitClose {
            close()
        }
    }

    suspend fun updateUserPassword(newPassword:String) = callbackFlow<UpdateResponse> {

        userRef.document(AuthManager.currentUserId()!!).update(Constants.UserTable.USER_PASSWORD.fieldName ,newPassword).addOnCompleteListener {
            if (it.isSuccessful){
                trySend(UpdateResponse(true ,""))
            }else{
                trySend(UpdateResponse(false, it.exception?.toString()))
            }
        }

        awaitClose {
            close()
        }
    }

    //endregion

}