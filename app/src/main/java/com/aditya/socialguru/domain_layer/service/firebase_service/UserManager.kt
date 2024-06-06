package com.aditya.socialguru.domain_layer.service.firebase_service

import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


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