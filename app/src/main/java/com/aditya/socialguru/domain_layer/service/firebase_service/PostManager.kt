package com.aditya.socialguru.domain_layer.service.firebase_service

import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.post.PostListenerEmissionType
import com.aditya.socialguru.data_layer.model.post.PostUploadingResponse
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.data_layer.model.post.comment.CommentedPost
import com.aditya.socialguru.data_layer.model.post.post_meta_data.CommentedUserPostModel
import com.aditya.socialguru.data_layer.model.post.post_meta_data.LikedPostModel
import com.aditya.socialguru.data_layer.model.post.post_meta_data.SavedPostModel
import com.aditya.socialguru.data_layer.model.post.post_meta_data.SavedUserPostModel
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.FolderName
import com.aditya.socialguru.domain_layer.helper.Constants.PostTable
import com.aditya.socialguru.domain_layer.helper.Constants.Table
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.helper.mapAsync
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.NotificationSendingManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


/**
 * [PostManager] This class only do crud  operation on firebase firestore [com.aditya.socialguru.data_layer.model.post.Post] collection and its subCollection.
 * */
object PostManager {

    private val tagPost = Constants.LogTag.Post
    private val tagDelete = Constants.LogTag.ForceLogout


    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val postRef = firestore.collection(Table.Post.name)
    private val userRef = firestore.collection(Table.User.name)


    private var discoverPostListener: ListenerRegistration? = null
    private var isFirstTimeDiscoverPostListnerCall = true


    private var myPostListener: ListenerRegistration? = null
    private var isFirstTimeMyPostListenerCall = true

    private var idPostListener: ListenerRegistration? = null
    private var isFirstTimeIdPostListenerCall = true

    //This is for detail post fragment
    private var likePostistener: ListenerRegistration? = null
    private var isFirstTimeLikePostListenerCall = true

    //This is for my acitvity fragment
    private var myLikedPostistener: ListenerRegistration? = null
    private var isFirstTimeMyLikePostListenerCall = true

    private var listenMySavedPostListener: ListenerRegistration? = null
    private var listenMyLikedPostListener: ListenerRegistration? = null
    private var listenMyCommentedPost: ListenerRegistration? = null

    private var listenMySavedPostForScreenViewListener: ListenerRegistration? = null
    private var listenMyLikedPostForScreenViewListener: ListenerRegistration? = null

    //region:: Like or Unlike a post or MyLiked Post or Update Like count

    suspend fun updateLikeCount(postId: String, postCreatorUserId: String, isLiked: Boolean) =
        callbackFlow<UpdateResponse>
        {
            val postIdRef = postRef.document(postId)
            val likeRef = postIdRef.collection(Table.Like.name)
            // This is for showing liked user on post in detail post fragment to liked user screen
            val deleteUserFromLike = likeRef.document(AuthManager.currentUserId()!!)
            val insertUserIntoLike = likeRef.document(AuthManager.currentUserId()!!)


            val timeStamp = System.currentTimeMillis()
            val notificationId = Helper.getNotificationId()
            val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
            val notificationData = NotificationData(
                notificationId = notificationId,
                friendOrFollowerId = AuthManager.currentUserId()!!,
                notificationTimeInText = timeInText,
                notificationTimeInTimeStamp = timeStamp.toString(),
                type = Constants.NotificationType.LIKE_IN_POST.name,
                postId = postId
            )
            val notificationRef =
                userRef.document(postCreatorUserId).collection(Table.Notification.name)
                    .document(notificationId)

            val post = postIdRef.get().await().toObject<Post>()

            if (post != null) {
                // First We Add or Remove From My Liked Post Then do other thing
                updatePostLikeStatus(postId).onEach {
                    if (it.isSuccess) {
                        val currentCount = post.likeCount ?: 0
                        val newCount =
                            if (isLiked) currentCount + 1 else if (currentCount == 0) 0 else currentCount - 1
                        firestore.runBatch { batch ->

                            val userId = AuthManager.currentUserId()!!

                            val updatedLikedUserList =
                                (post.likedUserList?.toMutableList() ?: mutableListOf()).apply {
                                    if (isLiked) {
                                        batch.set(
                                            insertUserIntoLike,
                                            FriendCircleData(userId, timeStamp)
                                        )
                                        if (!contains(userId)) add(userId)
                                    } else {
                                        batch.delete(deleteUserFromLike)
                                        remove(userId)
                                    }
                                }


                            batch.update(
                                postIdRef, mapOf(
                                    PostTable.LIKE_COUNT.fieldName to newCount,
                                    PostTable.LIKED_USER_LIST.fieldName to updatedLikedUserList
                                )
                            )
                            if (postCreatorUserId != AuthManager.currentUserId() && isLiked) {
                                batch.set(notificationRef, notificationData)
                            }
                        }.addOnSuccessListener {
                            if (postCreatorUserId != AuthManager.currentUserId()!! && isLiked) {
                                NotificationSendingManager.sendNotification(
                                    postCreatorUserId,
                                    notificationData
                                )
                            }
                            MyLogger.i(tagPost, msg = "Like count updated successfully!")
                            trySend(UpdateResponse(true, ""))
                        }.addOnFailureListener { e ->
                            MyLogger.e(tagPost, msg = "Error updating like count: $e")
                            trySend(UpdateResponse(false, e.message))
                        }.await()
                    } else {
                        trySend(it)
                    }
                }.launchIn(this)

            } else {
                trySend(UpdateResponse(false, "Something went wrong!"))
            }


            awaitClose {
                close()
            }
        }


    suspend fun getPostLikeUser(postId: String) =
        callbackFlow<ListenerEmissionType<FriendCircleData, FriendCircleData>> {

            val postIdRef = postRef.document(postId)
            val likeRef = postIdRef.collection(Table.Like.name)

            val userIdList = likeRef.get().await().mapNotNull {
                UserManager.getUserById(it.id)?.let { user ->
                    it.toObject<FriendCircleData>().copy(
                        user = user
                    )
                }
            }.toMutableList()
            userIdList.sortByDescending { it.timeStamp }

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting,
                    responseList = userIdList.toList()
                )
            )

            userIdList.clear()
            likePostistener?.remove()
            likePostistener = likeRef.addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                if (isFirstTimeLikePostListenerCall) {
                    isFirstTimeLikePostListenerCall = false
                    return@addSnapshotListener
                }
                value?.documentChanges?.forEach {
                    when (it.type) {
                        DocumentChange.Type.ADDED -> {
                            launch {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Added,
                                        singleResponse = it.document.toObject<FriendCircleData>()
                                            .copy(
                                                user = UserManager.getUserById(it.document.id)
                                            )
                                    )
                                )
                            }

                        }

                        DocumentChange.Type.MODIFIED -> {

                        }

                        DocumentChange.Type.REMOVED -> {
                            launch {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Removed,
                                        singleResponse = it.document.toObject<FriendCircleData>()
                                            .copy(
                                                user = UserManager.getUserById(it.document.id)
                                            )
                                    )
                                )
                            }
                        }
                    }
                }
            }

            awaitClose {
                isFirstTimeLikePostListenerCall = true
                likePostistener?.remove()
                close()
            }
        }

    suspend fun getMyLikedPost(userId: String) =
        callbackFlow<ListenerEmissionType<UserPostModel, UserPostModel>> {

            val postIdRef = postRef.whereArrayContains(PostTable.LIKED_USER_LIST.fieldName, userId)

            val userPostList = postIdRef.get().await().mapNotNull {
                it.toObject<Post>().let {
                    it.userId?.run {
                        val user = UserManager.getUserById(this)
                        UserPostModel(user, it)
                    }
                }

            }.toMutableList()
            userPostList.sortByDescending { it.post?.postUploadingTimeInTimeStamp }

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting,
                    responseList = userPostList.toList()
                )
            )

            userPostList.clear()
            myLikedPostistener?.remove()
            myLikedPostistener = postIdRef.addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                if (isFirstTimeMyLikePostListenerCall) {
                    isFirstTimeMyLikePostListenerCall = false
                    return@addSnapshotListener
                }
                value?.documentChanges?.forEach {
                    when (it.type) {
                        DocumentChange.Type.ADDED -> {
                            launch {
                                trySend(
                                    ListenerEmissionType(
                                        Constants.ListenerEmitType.Added,
                                        singleResponse = it.document.toObject<Post>().let {
                                            it.userId?.run {
                                                val user = UserManager.getUserById(this)
                                                UserPostModel(user, it)
                                            }
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
                                        singleResponse = it.document.toObject<Post>().let {
                                            it.userId?.run {
                                                val user = UserManager.getUserById(this)
                                                UserPostModel(user, it)
                                            }
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
                                        singleResponse = it.document.toObject<Post>().let {
                                            it.userId?.run {
                                                val user = UserManager.getUserById(this)
                                                UserPostModel(user, it)
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
                isFirstTimeMyLikePostListenerCall = true
                myLikedPostistener?.remove()
                close()
            }
        }


    //endregion


    //region::Get Random Post (Discover)


    suspend fun getDiscoverPost() = callbackFlow<PostListenerEmissionType> {

        val discoverPostQuery = firestore.collection(Constants.Table.Post.name)
        val posts = discoverPostQuery.get().await()


        val userPostList = posts.mapNotNull {
            val post = it.toObject<Post>()
            post.userId?.let { id ->
                UserManager.getUserById(id)?.let { UserPostModel(it, post) }
            }
        }.toMutableList()


        MyLogger.i(
            tagPost,
            msg = userPostList,
            isJson = true,
            jsonTitle = "User Discover Post list !"
        )

        trySend(
            PostListenerEmissionType(
                Constants.ListenerEmitType.Starting,
                userPostList = userPostList.toList()
            )
        )
        userPostList.clear()
        discoverPostListener?.remove()
        discoverPostListener = discoverPostQuery.addSnapshotListener { value, error ->

            if (error != null) return@addSnapshotListener

            if (!isFirstTimeDiscoverPostListnerCall) {
                value?.documentChanges?.forEach { document ->
                    when (document.type) {
                        DocumentChange.Type.ADDED -> {
                            trySend(
                                PostListenerEmissionType(
                                    Constants.ListenerEmitType.Added,
                                    userPostModel = document.document.toObject<Post>()
                                )
                            )
                        }

                        DocumentChange.Type.REMOVED -> {
                            trySend(
                                PostListenerEmissionType(
                                    Constants.ListenerEmitType.Removed,
                                    userPostModel = document.document.toObject<Post>()
                                )
                            )
                        }

                        else -> {}
                    }
                }
            }
            isFirstTimeDiscoverPostListnerCall = false

        }


        awaitClose {
            isFirstTimeDiscoverPostListnerCall =
                true  // This is singleton variable if function again call then this variable contain old value so that reset here
            discoverPostListener?.remove()
            channel.close()
        }

    }


    //endregion

    //region::Get Following Post (Discover)

    suspend fun getFollowingPost(userIds: List<String>) = callbackFlow<PostListenerEmissionType> {
        // Define the batch size within the function scope
        val BATCH_SIZE = 10

        // Helper function to process document changes
        suspend fun processDocumentChange(change: DocumentChange) {
            val post = change.document.toObject<Post>()
            val emissionType = when (change.type) {
                DocumentChange.Type.ADDED -> Constants.ListenerEmitType.Added
                DocumentChange.Type.REMOVED -> Constants.ListenerEmitType.Removed
                DocumentChange.Type.MODIFIED -> Constants.ListenerEmitType.Modify
            }
            trySend(PostListenerEmissionType(emissionType, userPostModel = post)).isSuccess
        }

        // Split user IDs into chunks based on the batch size
        val userIdChunks = userIds.chunked(BATCH_SIZE)

        // Initialize a list to hold all registrations
        val registrations = mutableListOf<ListenerRegistration>()

        // Function to create and register listeners for chunks of user IDs
        suspend fun registerListeners(userIdChunks: List<List<String>>) {
            userIdChunks.forEach { chunk ->
                val query = firestore.collection(Table.Post.name)
                    .whereIn(PostTable.USER_ID.fieldName, chunk)

                val registration = query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Handle error if necessary
                        return@addSnapshotListener
                    }

                    // Process all document changes
                    launch {
                        snapshot?.documentChanges?.mapAsync {
                            processDocumentChange(it)
                        }
                    }
                }

                registrations.add(registration)
            }
        }

        // Register listeners for all chunks
        registerListeners(userIdChunks)

        // Ensure listeners are removed when the channel is closed
        awaitClose {
            registrations.forEach { it.remove() }
            channel.close()
        }
    }


    //endregion

    //region:: Get My Post

    suspend fun getMyPost(userId: String) = callbackFlow<PostListenerEmissionType> {

        val discoverPostQuery = firestore.collection(Constants.Table.Post.name)
            .whereEqualTo(Constants.PostTable.USER_ID.fieldName, userId)
        val posts = discoverPostQuery.get().await()


        val userPostList = posts.mapNotNull {
            val post = it.toObject<Post>()
            post.userId?.let { id ->
                UserManager.getUserById(id)?.let { UserPostModel(it, post) }
            }
        }.toMutableList()


        MyLogger.i(
            tagPost,
            msg = userPostList,
            isJson = true,
            jsonTitle = "My Post list !"
        )

        trySend(
            PostListenerEmissionType(
                Constants.ListenerEmitType.Starting,
                userPostList = userPostList.toList()
            )
        )
        userPostList.clear()
        myPostListener?.remove()
        myPostListener = discoverPostQuery.addSnapshotListener { value, error ->

            if (error != null) return@addSnapshotListener

            if (!isFirstTimeMyPostListenerCall) {
                value?.documentChanges?.forEach { document ->
                    when (document.type) {
                        DocumentChange.Type.MODIFIED -> {
                            trySend(
                                PostListenerEmissionType(
                                    Constants.ListenerEmitType.Modify,
                                    userPostModel = document.document.toObject<Post>()
                                )
                            )
                        }

                        DocumentChange.Type.REMOVED -> {
                            trySend(
                                PostListenerEmissionType(
                                    Constants.ListenerEmitType.Removed,
                                    userPostModel = document.document.toObject<Post>()
                                )
                            )
                        }

                        else -> {}
                    }
                }
            }
            isFirstTimeMyPostListenerCall = false

        }


        awaitClose {
            isFirstTimeMyPostListenerCall = true
            discoverPostListener?.remove()
            channel.close()
        }

    }

    //endregion

    //region:: Get Post By Id

    suspend fun getPostById(postId: String) =
        callbackFlow<ListenerEmissionType<UserPostModel, UserPostModel>> {

            val postQuery = firestore.collection(Constants.Table.Post.name).document(postId)
            var post = postQuery.get().await().toObject<Post>()?.run {
                userId?.let { id ->
                    UserManager.getUserById(id)?.let { UserPostModel(it, this) }
                }
            }


            MyLogger.i(
                tagPost,
                msg = post,
                isJson = true,
                jsonTitle = "Post By Id Get !"
            )

            trySend(
                ListenerEmissionType(
                    Constants.ListenerEmitType.Starting,
                    singleResponse = post
                )
            )
            idPostListener?.remove()
            idPostListener = postQuery.addSnapshotListener { value, error ->

                if (error != null) return@addSnapshotListener

                if (isFirstTimeIdPostListenerCall) {
                    isFirstTimeIdPostListenerCall = false
                    return@addSnapshotListener
                }

                value?.let {
                    launch {
                        post = it.toObject<Post>()?.run {
                            userId?.let { id ->
                                UserManager.getUserById(id)?.let { UserPostModel(it, this) }
                            }
                        }
                        MyLogger.v(tagPost, msg = "Post id $postId is modify !")
                        trySend(
                            ListenerEmissionType(
                                Constants.ListenerEmitType.Modify,
                                singleResponse = post
                            )
                        )
                    }
                }

            }


            awaitClose {
                isFirstTimeIdPostListenerCall = true
                idPostListener?.remove()
                channel.close()
            }

        }

    //endregion
    //region:: Post Uploading


    suspend fun uploadPost(post: Post) {
        val isImagePresent = post.imageUrl != null
        val isVideoPresent = post.videoUrl != null

        when {
            isVideoPresent -> {
                uploadVideo(post, isImagePresent)
            }

            isImagePresent -> {
                uploadImage(post)
            }

            else -> {
                savePostToDatabase(post)
            }
        }
    }


    private suspend fun uploadVideo(post: Post, imagePresent: Boolean) {
        AppBroadcastHelper.setPostUploadState(
            PostUploadingResponse(
                Constants.PostUploadState.VideoUploading
            )
        )
        MyLogger.v(tagPost, isFunctionCall = true)
        val videoUri = post.videoUrl!!.convertParseUri()
        StorageManager.uploadVideoToServer(
            Constants.Table.Post.name,
            "${post.userId ?: AuthManager.currentUserId()}/${FolderName.PostVideo.name}",
            videoUri
        ).collect {
            when (it.state) {
                Constants.StorageManagerState.InProgress -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.VideoUploading,
                            it.progress
                        )
                    )
                }

                Constants.StorageManagerState.Error -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.Error,
                            errorMessage = it.error
                        )
                    )
                }

                Constants.StorageManagerState.UrlNotGet -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.Error,
                            errorMessage = it.error
                        )
                    )
                }

                Constants.StorageManagerState.Success -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.VideoUploaded
                        )
                    )
                    if (imagePresent) {
                        uploadImage(post, it.url)
                    } else {
                        savePostToDatabase(post, it.url)
                    }
                }
            }
        }
    }


    private suspend fun uploadImage(post: Post, onLineVideoUri: String? = null) {
        AppBroadcastHelper.setPostUploadState(
            PostUploadingResponse(
                Constants.PostUploadState.ImageUploading
            )
        )

        MyLogger.v(tagPost, isFunctionCall = true)
        val imageUri = post.imageUrl!!.convertParseUri()
        StorageManager.uploadImageToServer(
            Constants.Table.Post.name,
            "${post.userId ?: AuthManager.currentUserId()}/${FolderName.PostImage.name}",
            imageUri
        ).collect {
            when (it.state) {
                Constants.StorageManagerState.InProgress -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.ImageUploading,
                            it.progress
                        )
                    )
                }

                Constants.StorageManagerState.Error -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.Error,
                            errorMessage = it.error
                        )
                    )
                }

                Constants.StorageManagerState.UrlNotGet -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.Error,
                            errorMessage = it.error
                        )
                    )
                }

                Constants.StorageManagerState.Success -> {
                    AppBroadcastHelper.setPostUploadState(
                        PostUploadingResponse(
                            Constants.PostUploadState.ImageUploaded
                        )
                    )
                    savePostToDatabase(post, onLineVideoUri, it.url)
                }
            }
        }
    }


    private suspend fun savePostToDatabase(
        post: Post,
        onlineVideoUri: String? = null,
        onlineImageUri: String? = null
    ) {
        MyLogger.v(tagPost, isFunctionCall = true)
        AppBroadcastHelper.setPostUploadState(
            PostUploadingResponse(
                Constants.PostUploadState.PostUploading
            )
        )

        val saveDatabaseReference =
            firestore.collection(Constants.Table.Post.name)
                .document(post.postId!!)
        val updatedPost = post.copy(
            imageUrl = onlineImageUri?.toString(),
            videoUrl = onlineVideoUri?.toString(),
            postUploadingTimeInTimeStamp = System.currentTimeMillis()
        )
        saveDatabaseReference.set(updatedPost).addOnCompleteListener {
            if (it.isSuccessful) {
                MyLogger.i(tagPost, msg = "Post saved successfully.")
                AppBroadcastHelper.setPostUploadState(
                    PostUploadingResponse(
                        Constants.PostUploadState.PostUploaded
                    )
                )

            } else {
                MyLogger.e(
                    tagPost,
                    msg = giveMeErrorMessage("Uploading post", it.exception.toString())
                )
                AppBroadcastHelper.setPostUploadState(
                    PostUploadingResponse(
                        Constants.PostUploadState.Error
                    )
                )
            }
        }.await()

    }

    //endregion


    //region:: Update Post Save

    suspend fun updatePostSaveStatus(postId: String) = callbackFlow<UpdateResponse> {
        try {
            val timestamp = System.currentTimeMillis()
            val timeInText = Helper.formatTimestampToDateAndTime(timestamp)
            val savedPostRef =
                userRef.document(AuthManager.currentUserId()!!).collection(Table.SavedPost.name)
                    .document(postId)
            val isPostPresentInMyPostSaveList = savedPostRef.get().await().exists()

            firestore.runBatch {
                if (isPostPresentInMyPostSaveList) {
                    //Un Save
                    savedPostRef.delete()

                } else {
                    //Save
                    savedPostRef.set(
                        SavedPostModel(
                            postId,
                            timestamp,
                            timeInText
                        )
                    )
                }
            }.addOnSuccessListener {
                trySend(
                    UpdateResponse(
                        true,
                        if (isPostPresentInMyPostSaveList) "Post Un-Save Successfully !" else "Post Save Successfully !"
                    )
                )
            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()


        } catch (e: Exception) {
            e.printStackTrace()
            trySend(UpdateResponse(false, e.message))
        }

        awaitClose {
            close()
        }
    }


    //endregion

    //region:: Update Post Save

    private suspend fun updatePostLikeStatus(postId: String) = callbackFlow<UpdateResponse> {
        try {
            val timestamp = System.currentTimeMillis()
            val timeInText = Helper.formatTimestampToDateAndTime(timestamp)
            val likedPostRef =
                userRef.document(AuthManager.currentUserId()!!).collection(Table.LikedPost.name)
                    .document(postId)
            val isPostPresentInMyPostLikedList = likedPostRef.get().await().exists()

            firestore.runBatch {
                if (isPostPresentInMyPostLikedList) {
                    //Dis Like
                    likedPostRef.delete()

                } else {
                    //Like
                    likedPostRef.set(
                        LikedPostModel(
                            postId,
                            timestamp,
                            timeInText
                        )
                    )
                }
            }.addOnSuccessListener {
                trySend(
                    UpdateResponse(
                        true,
                        ""
                    )
                )
            }.addOnFailureListener {
                trySend(UpdateResponse(false, it.message))
            }.await()


        } catch (e: Exception) {
            e.printStackTrace()
            trySend(UpdateResponse(false, e.message))
        }

        awaitClose {
            close()
        }
    }


    //endregion

    //region:: Listen My Saved And Like post ids for post Adapter

    suspend fun listenMySavedPost() =
        callbackFlow<List<ListenerEmissionType<SavedPostModel, SavedPostModel>>> {
            try {
                val savedPostRef =
                    userRef.document(AuthManager.currentUserId()!!).collection(Table.SavedPost.name)
                listenMySavedPostListener = savedPostRef.addSnapshotListener { value, error ->
                    if (error != null) return@addSnapshotListener

                    value?.documentChanges?.let { data ->
                        launch {
                            val savedList = data.map {
                                async {
                                    val savedPost = it.document.toObject<SavedPostModel>()
                                    val changeType = when (it.type) {
                                        DocumentChange.Type.ADDED -> Constants.ListenerEmitType.Added
                                        DocumentChange.Type.MODIFIED -> Constants.ListenerEmitType.Modify
                                        DocumentChange.Type.REMOVED -> Constants.ListenerEmitType.Removed
                                    }

                                    val postTask =
                                        postRef.document(savedPost.postId!!).get().await()
                                    if (!postTask.exists()) return@async null

                                    val post = postTask.toObject<Post>() ?: return@async null

                                    val userTask = userRef.document(post.userId!!).get().await()
                                    if (!userTask.exists()) return@async null
                                    ListenerEmissionType<SavedPostModel, SavedPostModel>(
                                        changeType, singleResponse = savedPost
                                    )
                                }
                            }.awaitAll().filterNotNull()

                            trySend(savedList)
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            awaitClose {
                listenMySavedPostListener?.remove()
                close()
            }
        }

    suspend fun listenMyLikedPost() =
        callbackFlow<List<ListenerEmissionType<LikedPostModel, LikedPostModel>>> {
            try {
                val savedPostRef =
                    userRef.document(AuthManager.currentUserId()!!).collection(Table.LikedPost.name)
                listenMyLikedPostListener = savedPostRef.addSnapshotListener { value, error ->
                    if (error != null) return@addSnapshotListener

                    value?.documentChanges?.let { data ->
                        launch {
                            val savedList = data.map {
                                async {
                                    val likedPost = it.document.toObject<LikedPostModel>()
                                    val changeType = when (it.type) {
                                        DocumentChange.Type.ADDED -> Constants.ListenerEmitType.Added
                                        DocumentChange.Type.MODIFIED -> Constants.ListenerEmitType.Modify
                                        DocumentChange.Type.REMOVED -> Constants.ListenerEmitType.Removed
                                    }

                                    val postTask =
                                        postRef.document(likedPost.postId!!).get().await()
                                    if (!postTask.exists()) return@async null

                                    val post = postTask.toObject<Post>() ?: return@async null

                                    val userTask = userRef.document(post.userId!!).get().await()
                                    if (!userTask.exists()) return@async null
                                    ListenerEmissionType<LikedPostModel, LikedPostModel>(
                                        changeType, singleResponse = likedPost
                                    )
                                }
                            }.awaitAll().filterNotNull()

                            trySend(savedList)
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            awaitClose {
                listenMyLikedPostListener?.remove()
                close()
            }
        }

    //endregion

    //region:: Get And Listen Commented Post

    suspend fun listenCommentedPost(userId: String) =
        callbackFlow<List<ListenerEmissionType<CommentedUserPostModel, CommentedUserPostModel>>> {
            try {
                val myCommentedRef = userRef.document(userId)
                    .collection(Table.CommentedPost.name)
                listenMyCommentedPost = myCommentedRef.addSnapshotListener { value, error ->
                    if (error != null) return@addSnapshotListener

                    value?.documentChanges?.let { data ->
                        launch {
                            val commentedList = data.map {
                                async {
                                    val commentedPost = it.document.toObject<CommentedPost>()
                                    val changeType = when (it.type) {
                                        DocumentChange.Type.ADDED -> Constants.ListenerEmitType.Added
                                        DocumentChange.Type.MODIFIED -> Constants.ListenerEmitType.Modify
                                        DocumentChange.Type.REMOVED -> Constants.ListenerEmitType.Removed
                                    }

                                    val postTask =
                                        postRef.document(commentedPost.postId!!).get().await()
                                    if (!postTask.exists()) return@async null

                                    val post = postTask.toObject<Post>() ?: return@async null

                                    val userTask = userRef.document(post.userId!!).get().await()
                                    if (!userTask.exists()) return@async null

                                    val user = userTask.toObject<User>()
                                    ListenerEmissionType<CommentedUserPostModel, CommentedUserPostModel>(
                                        changeType, singleResponse = CommentedUserPostModel(
                                            commentedPost, UserPostModel(user, post)
                                        )
                                    )
                                }
                            }.awaitAll().filterNotNull()

                            trySend(commentedList)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            awaitClose {
                listenMyCommentedPost?.remove()
                close()
            }
        }

//endregion

    //region:: Get And Listen My Save Post For Screen View (UserPostModel Type)

    suspend fun listenMySavedPostForScreenView() =
        callbackFlow<List<ListenerEmissionType<SavedUserPostModel, SavedUserPostModel>>> {
            try {
                val myCommentedRef = userRef.document(AuthManager.currentUserId()!!)
                    .collection(Table.SavedPost.name)
                listenMySavedPostForScreenViewListener =
                    myCommentedRef.addSnapshotListener { value, error ->
                        if (error != null) return@addSnapshotListener

                        value?.documentChanges?.let { data ->
                            launch {
                                val savedPostList = data.map {
                                    async {
                                        val savedPost = it.document.toObject<SavedPostModel>()
                                        val changeType = when (it.type) {
                                            DocumentChange.Type.ADDED -> Constants.ListenerEmitType.Added
                                            DocumentChange.Type.MODIFIED -> Constants.ListenerEmitType.Modify
                                            DocumentChange.Type.REMOVED -> Constants.ListenerEmitType.Removed
                                        }

                                        val postTask =
                                            postRef.document(savedPost.postId!!).get().await()
                                        if (!postTask.exists()) return@async null

                                        val post = postTask.toObject<Post>() ?: return@async null

                                        val userTask = userRef.document(post.userId!!).get().await()
                                        if (!userTask.exists()) return@async null

                                        val user = userTask.toObject<User>()
                                        ListenerEmissionType<SavedUserPostModel, SavedUserPostModel>(
                                            changeType, singleResponse = SavedUserPostModel(
                                                savedPost, UserPostModel(
                                                    user, post
                                                )
                                            )
                                        )
                                    }
                                }.awaitAll().filterNotNull()

                                trySend(savedPostList)
                            }
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            awaitClose {
                listenMySavedPostForScreenViewListener?.remove()
                close()
            }
        }

//endregion


    //region:: Delete All My Post

    suspend fun deleteAllMyPost() = callbackFlow<UpdateResponse> {
        try {
            MyLogger.e(tagDelete, isFunctionCall = true)
            val postRef = firestore.collection(Table.Post.name)
                .whereEqualTo(PostTable.USER_ID.fieldName, AuthManager.currentUserId()!!)
            val posts = postRef.get().await()
            MyLogger.e(tagDelete, msg = "posts is count :-> ${posts.size()}")
            if (posts.isEmpty) {
                MyLogger.i(tagDelete, msg = "No Post found , so don't do any thing.")
                trySend(UpdateResponse(true, "All Post Delete Successfully !"))
            } else {
                StorageManager.deleteMyAllPostMedia().onEach {
                    MyLogger.e(tagDelete, msg = "All Post Media Delete Successfully !")
                    launch {
                        MyLogger.w(tagPost, msg = it.errorMessage)
                        val postSubCollectionDeleteWork = posts.map {
                            async {
                                deleteAllThisPostSubCollection(it.id).first()
                            }
                        }
                        postSubCollectionDeleteWork.awaitAll()

                        val postDeleteWork = posts.map {
                            async {
                                it.reference.delete()
                            }
                        }
                        postDeleteWork.awaitAll()

                        MyLogger.e(tagDelete, msg = "All Post Ref Delete Successfully !")
                        trySend(UpdateResponse(true, "All Post Delete Successfully !"))
                    }
                }.launchIn(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(UpdateResponse(false, e.message))
        }

        awaitClose {
            close()
        }
    }

    private suspend fun deleteAllThisPostSubCollection(postId: String) =
        callbackFlow<UpdateResponse> {
            MyLogger.e(tagDelete, isFunctionCall = true)
            val postIdRef = postRef.document(postId)
            val collectionRefs = listOf(
                postIdRef.collection(Constants.Table.Like.name),
                postIdRef.collection(Constants.Table.Comment.name),
                postIdRef.collection(Constants.Table.Commenters.name)
            )
            UserManager.deleteCollection(collectionRefs).onEach {
                MyLogger.i(tagDelete, msg = "All Collection is deleted !")
                trySend(it)
            }.launchIn(this)

            awaitClose {
                close()
            }
        }

//endregion


}