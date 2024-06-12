package com.aditya.socialguru.domain_layer.service.firebase_service

import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.data_layer.model.user_action.UserRelationshipStatus
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.toObject
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
            val docRef = firestore.collection(Constants.Table.User.name).document(userId)
            try {
                val documentSnapshot = docRef.get().await()
                if (!documentSnapshot.exists()) {
                    trySend(Resource.Error("User not found!"))
                }

                val user = documentSnapshot.toObject(User::class.java)

                if (user != null) {
                    trySend(Resource.Success(user))
                } else {
                    trySend(Resource.Error("User document found but data is null!"))
                }

            } catch (e: Exception) {
                trySend(Resource.Error("Error getting user: ${e.message}"))
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
            null
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

            MyLogger.v(Constants.LogTag.Profile, msg = followerList)

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
            MyLogger.v(tagProfile, msg = followingList, isJson = true)

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
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Added,
                                        singleResponse = it.document.toObject<FriendCircleData>()
                                    )
                                )
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

            MyLogger.v(Constants.LogTag.Profile, msg = friendList)

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
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Added,
                                        singleResponse = it.document.toObject<FriendCircleData>()
                                    )
                                )
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

        val myUserRef =
            userRef.document(userId).collection(Constants.Table.Following.name).document(followedId)
        val followedRef =
            userRef.document(followedId).collection(Constants.Table.Follower.name).document(userId)

        firestore.runBatch { batch ->
            batch.set(myUserRef, followedData)
            batch.set(followedRef, myData)
        }.addOnSuccessListener {
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

        val myUserRef =
            userRef.document(userId).collection(Constants.Table.PendingRequest.name)
                .document(friendId)
        val friendRef =
            userRef.document(friendId).collection(Constants.Table.FriendRequest.name)
                .document(userId)

        firestore.runBatch { batch ->
            batch.set(myUserRef, friendData)
            batch.set(friendRef, myData)
        }.addOnSuccessListener {
            trySend(UpdateResponse(true, ""))
        }.addOnFailureListener {
            trySend(UpdateResponse(false, it.message))
        }.await()


        awaitClose {
            close()
        }
    }

    suspend fun deleteFriendRequest(userId: String, friendId: String) = callbackFlow<UpdateResponse> {

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

    suspend fun acceptFriendRequest(userId: String, friendId: String) =
        callbackFlow<UpdateResponse> {
            val timeStamp = System.currentTimeMillis()
            val friendData = FriendCircleData(friendId, timeStamp)
            val myData = FriendCircleData(userId, timeStamp)

            val myPendingRef =
                userRef.document(userId).collection(Constants.Table.PendingRequest.name)
                    .document(friendId)
            val friendRequestRef =
                userRef.document(friendId).collection(Constants.Table.FriendRequest.name)
                    .document(userId)
            val myUserFriendRef =
                userRef.document(userId).collection(Constants.Table.Friend.name).document(friendId)
            val friendFriendRef =
                userRef.document(friendId).collection(Constants.Table.Friend.name).document(userId)

            firestore.runBatch { batch ->
                batch.delete(myPendingRef)
                batch.delete(friendRequestRef)
                batch.set(myUserFriendRef, friendData)
                batch.set(friendFriendRef, myData)
            }.addOnSuccessListener {
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
            val isFollowing = isUserInCollection(currentUserId, targetUserId, "Following")
            val friendStatus = getFriendStatus(currentUserId, targetUserId)
            trySend(UserRelationshipStatus(isFollowing, friendStatus))
            awaitClose {
                close()
            }
        }

    private suspend fun getFriendStatus(currentUserId: String, targetUserId: String): Constants.FriendStatus {
        return when {
            isUserInCollection(currentUserId, targetUserId, Constants.Table.Friend.name) -> Constants.FriendStatus.FRIEND
            isUserInCollection(currentUserId, targetUserId, Constants.Table.PendingRequest.name) -> Constants.FriendStatus.PENDING_REQUEST
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


}