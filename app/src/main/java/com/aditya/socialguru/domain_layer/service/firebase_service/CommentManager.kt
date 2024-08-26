package com.aditya.socialguru.domain_layer.service.firebase_service

import android.util.Log
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.UpdateChatResponse
import com.aditya.socialguru.data_layer.model.notification.NotificationData
import com.aditya.socialguru.data_layer.model.post.Comment
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.post.comment.CommentedPost
import com.aditya.socialguru.data_layer.model.post.comment.CommentersModel
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.NotificationSendingManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.system.measureTimeMillis

/**
 * [CommentManager] This class only do crud  operation on firebase firestore [com.aditya.socialguru.data_layer.model.post.Comment] collection and its subCollection..
 * */
object CommentManager {

    private val tagComment = Constants.LogTag.Comment
    private val tagDelete = Constants.LogTag.ForceLogout

    private var commentListener: ListenerRegistration? = null
    private var commentersListener: ListenerRegistration? = null

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val firebaseStorage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    private val userRef by lazy {
        firestore.collection(Constants.Table.User.name)
    }

    private val postRef by lazy {
        firestore.collection(Constants.Table.Post.name)
    }


    suspend fun sendComment(
        comment: Comment, isCreatorOnline: Boolean
    ) = callbackFlow<UpdateChatResponse> {

        val isImagePresent = comment.imageUri != null
        val isVideoPresent = comment.videoUri != null

        when {
            isVideoPresent -> {
                uploadVideo(
                    comment, isImagePresent, isCreatorOnline
                ).onEach {
                    trySend(it)
                }.launchIn(this)
            }

            isImagePresent -> {
                uploadImage(comment, isCreatorOnline).onEach {
                    trySend(it)
                }.launchIn(this)
            }

            else -> {
                saveCommentToDatabase(comment, isCreatorOnline).onEach {
                    trySend(it)
                }.launchIn(this)
            }
        }

        awaitClose {
            close()
        }
    }


    private suspend fun uploadVideo(
        comment: Comment, isCreatorOnline: Boolean, isImagePresent: Boolean
    ) = callbackFlow<UpdateChatResponse> {
        trySend(UpdateChatResponse(true, "Video  Uploading "))
        val videoUri = comment.videoUri!!.convertParseUri()
        StorageManager.uploadVideoToServer(
            Constants.Table.Post.name,
            "${comment.postId}/${Constants.FolderName.CommentVideo.name}",
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
                    val updatedComment = comment.copy(
                        videoUri = onlineVideoUri
                    )
                    if (isImagePresent) {
                        uploadImage(
                            updatedComment, isCreatorOnline
                        ).onEach {
                            trySend(it)
                        }.launchIn(this)
                    } else {
                        saveCommentToDatabase(
                            updatedComment, isCreatorOnline
                        ).onEach {
                            trySend(it)
                        }.launchIn(this)
                    }
                }
            }
        }
    }

    private suspend fun uploadImage(
        comment: Comment, isCreatorOnline: Boolean
    ) = callbackFlow<UpdateChatResponse> {
        trySend(UpdateChatResponse(true, "Image Uploading"))
        val imageUri = comment.imageUri!!.convertParseUri()
        StorageManager.uploadImageToServer(
            Constants.Table.Post.name,
            "${comment.postId}/${Constants.FolderName.CommentImage.name}",
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
                    val updatedComment = comment.copy(
                        imageUri = onlineImageUri
                    )

                    saveCommentToDatabase(
                        updatedComment, isCreatorOnline
                    ).onEach {
                        trySend(it)
                    }.launchIn(this)


                }
            }
        }
    }

    private suspend fun saveCommentToDatabase(
        comment: Comment, isCreatorOnline: Boolean
    ) = callbackFlow<UpdateChatResponse> {
        val timeStamp = System.currentTimeMillis()
        val notificationId = Helper.getNotificationId()
        val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)

        val notificationData = NotificationData(
            notificationId = notificationId,
            friendOrFollowerId = AuthManager.currentUserId()!!,
            notificationTimeInText = timeInText,
            notificationTimeInTimeStamp = timeStamp.toString(),
            type = Constants.NotificationType.COMMENT_IN_POST.name,
            postId = comment.postId,
        )

        // This is updated message
        val updatedComment = comment.copy(
            commentUploadingTimeInTimestamp = timeStamp, commentUploadingTimeInText = timeInText
        )


        val commentRef = postRef.document(comment.postId!!).collection(Constants.Table.Comment.name)
            .document(comment.commentId!!)
        val postIdRef = postRef.document(comment.postId!!)
        val myCommentedPostRef =
            userRef.document(comment.userId!!).collection(Constants.Table.CommentedPost.name)
                .document(comment.postId)
        val commentersRef =
            postRef.document(comment.postId!!).collection(Constants.Table.Commenters.name)
        val myUserIdExistInCommentersRef = commentersRef.document(comment.userId!!)

        val isMyUserIdPresentInCommenters = myUserIdExistInCommentersRef.get().await().exists()


        //In transaction all read come first then after all write come
        // Firebase firestore transaction is much slower then batch write because transaction run sequence

        firestore.runBatch {
            val timeTakingToCalculate = measureTimeMillis {

                if (!isMyUserIdPresentInCommenters) {
                    it.set(myUserIdExistInCommentersRef, CommentersModel(comment.userId!!))
                }

                it.update(
                    postIdRef,
                    Constants.PostTable.COMMENT_COUNT.fieldName,
                    FieldValue.increment(1)
                )
                it.set(myCommentedPostRef, CommentedPost(comment.postId , timeStamp ,timeInText))
                it.set(commentRef, updatedComment)
            }
            MyLogger.v(tagComment, msg = "Time taken to calculate $timeTakingToCalculate")


        }.addOnSuccessListener {
            if (!isCreatorOnline) {
                NotificationSendingManager.sendNotification(
                    comment.postCreatorId!!, notificationData
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


    suspend fun deleteComment(
        comment: Comment
    ) = callbackFlow<UpdateResponse> {

        val isImagePresent = comment.imageUri != null
        val isVideoPresent = comment.videoUri != null

        val deleteImageUrl = comment.imageUri?.let { firebaseStorage.getReferenceFromUrl(it) }
        val deleteVideoUrl = comment.videoUri?.let { firebaseStorage.getReferenceFromUrl(it) }

        val commentRef = postRef.document(comment.postId!!).collection(Constants.Table.Comment.name)
            .document(comment.commentId!!)
        val commentersRef =
            postRef.document(comment.postId!!).collection(Constants.Table.Commenters.name)
                .document(comment.userId!!)
        val myCommentedPostRef =
            userRef.document(comment.userId!!).collection(Constants.Table.CommentedPost.name)
                .document(comment.postId)
        val isMyLastComment =
            postRef.document(comment.postId!!).collection(Constants.Table.Comment.name)
                .whereEqualTo(Constants.CommentTable.USER_ID.fieldName, comment.userId!!).get()
                .await().size() == 1

        val postIdRef = postRef.document(comment.postId!!)
        val isPostCountZero =
            postIdRef.get().await().getLong(Constants.PostTable.COMMENT_COUNT.fieldName) == 0L



        firestore.runBatch { batch ->
            if (isImagePresent) {
                deleteImageUrl?.delete()
            }
            if (isVideoPresent) {
                deleteVideoUrl?.delete()
            }
            if (isMyLastComment) {
                batch.delete(myCommentedPostRef)
                batch.delete(commentersRef)
            }

            if (!isPostCountZero) {
                batch.update(
                    postIdRef,
                    Constants.PostTable.COMMENT_COUNT.fieldName,
                    FieldValue.increment(-1)
                )
            }

            batch.delete(commentRef)

        }.addOnSuccessListener {
            trySend(UpdateResponse(true, ""))
            launchCoroutineInIOThread {

                //This is for update latest comment in my commented post collection database for showing latest comment post
                if (!isMyLastComment){
                    val previousComment = postRef.document(comment.postId!!)
                        .collection(Constants.Table.Comment.name)
                        .whereEqualTo(Constants.CommentTable.USER_ID.fieldName, comment.userId!!)
                        .orderBy(Constants.CommentTable.COMMENT_UPLOADING_TIME_IN_TIMESTAMP.fieldName, Query.Direction.DESCENDING)  // Replace "timestamp" with your actual timestamp field name
                        .limit(1)
                        .get().await().toObjects(Comment::class.java)
                    if (previousComment.size>0){
                        myCommentedPostRef.set(
                            previousComment[0]
                        )
                    }
                }
            }
        }.addOnFailureListener {
            trySend(UpdateResponse(false, it.message))
        }.await()

        awaitClose {
            close()
        }
    }

    suspend fun getCommentAndListen(postId: String) =
        callbackFlow<List<ListenerEmissionType<Comment, Comment>>> {
            val commentRef = postRef.document(postId).collection(Constants.Table.Comment.name)


            commentListener = commentRef.addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val commentsList = mutableListOf<ListenerEmissionType<Comment, Comment>>()
                value?.documentChanges?.forEach {
                    when (it.type) {
                        DocumentChange.Type.ADDED -> {
                            commentsList.add(
                                ListenerEmissionType(
                                    emitChangeType = Constants.ListenerEmitType.Added,
                                    singleResponse = it.document.toObject<Comment>()
                                )
                            )
                        }

                        DocumentChange.Type.MODIFIED -> {
                            // Do Nothing
                        }

                        DocumentChange.Type.REMOVED -> {
                            commentsList.add(
                                ListenerEmissionType(
                                    emitChangeType = Constants.ListenerEmitType.Removed,
                                    singleResponse = it.document.toObject<Comment>()
                                )
                            )
                        }
                    }
                }
                trySend(commentsList)

            }

            awaitClose {
                commentListener?.remove()
                close()
            }
        }


    suspend fun getCommentersAndListen(postId: String) =
        callbackFlow<List<ListenerEmissionType<User, User>>> {
            val commentersRef = postRef.document(postId).collection(Constants.Table.Commenters.name)
            commentersListener = commentersRef.addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                value?.documentChanges?.let {
                    launchCoroutineInIOThread {
                        val tempCommentersList = it.map { model ->
                            async {
                                val commenters = model.document.toObject<CommentersModel>()
                                val changeType = when (model.type) {
                                    DocumentChange.Type.ADDED -> Constants.ListenerEmitType.Added
                                    DocumentChange.Type.MODIFIED -> Constants.ListenerEmitType.Modify
                                    DocumentChange.Type.REMOVED -> Constants.ListenerEmitType.Removed
                                }

                                //This generic help to find which model store in this data class
                                ListenerEmissionType<User,User>(
                                    emitChangeType = changeType,
                                    singleResponse = UserManager.getUserById(commenters.userId!!)
                                )
                            }
                        }.awaitAll()
                        trySend(tempCommentersList)
                    }
                }
            }

            awaitClose {
                commentersListener?.remove()
                close()
            }
        }

    suspend fun updateMyOnlineStatus(postId: String, status:Boolean,post: Post?=null){
       val notNullPost = post ?: postRef.document(postId).get().await().toObject<Post>()

        if (notNullPost!=null){
            val isIAmCreatorOfThisPost = notNullPost.userId!! ==AuthManager.currentUserId()!!

            if (isIAmCreatorOfThisPost){
                postRef.document(notNullPost.postId!!).update(Constants.PostTable.IS_CREATOR_ONLINE.fieldName,status)
            }
        }

    }


    //region:: Delete My Comment from all post

     suspend fun deleteMyAllCommentFromEveryPost() = callbackFlow<UpdateResponse> {
        MyLogger.e(tagDelete, isFunctionCall = true)
        val userId = AuthManager.currentUserId()!!
        val commentRef = userRef.document(userId).collection(Constants.Table.CommentedPost.name)
        val commentList = commentRef.get().await().toObjects<CommentedPost>()

        val deleteTask = commentList.map {
            async {
                deleteCommentFromThisPost(it.postId!!)
            }
        }

        deleteTask.awaitAll()
        trySend(UpdateResponse(true, ""))
        awaitClose {
            close()
        }
    }

    private suspend fun deleteCommentFromThisPost(postId: String) {
        MyLogger.e(tagDelete, isFunctionCall = true)
        val postRef = firestore.collection(Constants.Table.Post.name).document(postId)
        val myCommentOnPost = postRef.collection(Constants.Table.Comment.name)
            .whereEqualTo(Constants.PostTable.USER_ID.fieldName, AuthManager.currentUserId()!!)
            .get().await()
        MyLogger.d(
            tagDelete,
            msg = "My Comment on post id :-> $postId is :- ${myCommentOnPost.size()}"
        )
        if (myCommentOnPost.isEmpty) return
        firestore.runBatch {
            myCommentOnPost.documents.forEach {
                it.reference.delete()
            }
        }.await()
    }

    //endregion


}