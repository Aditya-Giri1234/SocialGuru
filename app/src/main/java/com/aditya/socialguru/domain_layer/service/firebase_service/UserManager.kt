package com.aditya.socialguru.domain_layer.service.firebase_service

import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.user_action.FollowerData
import com.aditya.socialguru.data_layer.model.user_action.FollowingData
import com.aditya.socialguru.data_layer.model.user_action.FriendData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.android.material.tabs.TabLayout.Tab
import com.google.common.collect.Table
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
        callbackFlow<ListenerEmissionType<FollowerData, FollowerData>> {

            val followerRef = userRef.document(userId).collection(Constants.Table.Follower.name)

            var followerList =
                followerRef.get().await().toObjects(FollowerData::class.java)
            followerList = followerList.map {
                it.copy(
                    user = it.userId?.let { it1 -> getUserById(it1) }
                )
            }.toMutableList()

            followerList.sortBy {
                it.timeStamp
            }

            MyLogger.v(Constants.LogTag.Profile , msg = followerList)

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
                                            singleResponse = it.document.toObject<FollowerData>().apply {
                                                user= getUserById(it.document.id)
                                            }
                                        )
                                    )
                                }
                            }

                            DocumentChange.Type.REMOVED -> {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Removed,
                                        singleResponse = it.document.toObject<FollowerData>()
                                    )
                                )
                            }

                            DocumentChange.Type.MODIFIED -> {}
                        }
                    }
                }

            }




            awaitClose {
                isFirstTimeFollowerListListener = true// This is singleton variable if function again call then this variable contain old value so that reset here
                followerListListener?.remove()
                close()
            }
        }

    suspend fun getFollowingListAndListenChange(userId: String) =
        callbackFlow<ListenerEmissionType<FollowingData, FollowingData>> {

            val followingRef = userRef.document(userId).collection(Constants.Table.Following.name)

            val followingList =
                followingRef.get().await().toObjects(FollowingData::class.java)
            followingList.sortBy {
                it.timeStamp
            }

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting,
                    followingList
                )
            )

            followingList.clear()
            followingListListener?.remove()
            followingListListener = followingRef.addSnapshotListener { value, error ->

                if (error != null) return@addSnapshotListener

                if (value != null) {
                    if (isFirstTimeFollowerListListener) {
                        isFirstTimeFollowerListListener = false
                        return@addSnapshotListener
                    }

                    value.documentChanges.forEach {
                        when (it.type) {
                            DocumentChange.Type.ADDED -> {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Added,
                                        singleResponse = it.document.toObject<FollowingData>()
                                    )
                                )
                            }

                            DocumentChange.Type.REMOVED -> {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Removed,
                                        singleResponse = it.document.toObject<FollowingData>()
                                    )
                                )
                            }

                            DocumentChange.Type.MODIFIED -> {}
                        }
                    }
                }

            }




            awaitClose {
                followingListListener?.remove()
                close()
            }
        }

    suspend fun getFriendListAndListenChange(userId: String) =
        callbackFlow<ListenerEmissionType<FriendData, FriendData>> {

            val friendRef = userRef.document(userId).collection(Constants.Table.Friend.name)

            val friendList =
                friendRef.get().await().toObjects(FriendData::class.java)
            friendList.sortBy {
                it.timeStamp
            }

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting,
                    friendList
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
                                        singleResponse = it.document.toObject<FriendData>()
                                    )
                                )
                            }

                            DocumentChange.Type.REMOVED -> {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Removed,
                                        singleResponse = it.document.toObject<FriendData>()
                                    )
                                )
                            }

                            DocumentChange.Type.MODIFIED -> {}
                        }
                    }
                }

            }




            awaitClose {
                friendListListener?.remove()
                close()
            }
        }

    suspend fun removeFollower(userId: String)= callbackFlow<UpdateResponse> {
        val followerRef= userRef.document(AuthManager.currentUserId()!!).collection(Constants.Table.Follower.name).document(userId)

        followerRef.delete().addOnSuccessListener {
            trySend(UpdateResponse(true,""))
        }.addOnFailureListener {
            trySend(UpdateResponse(false,it.message.toString()))
        }.await()

        awaitClose{
            close()
        }
    }


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