package com.aditya.socialguru.domain_layer.service

import android.net.Uri
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.UploadingResponse
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.story.Stories
import com.aditya.socialguru.data_layer.model.story.StoryText
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.domain_layer.service.firebase_service.StoryManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.system.measureTimeMillis

object FirebaseManager {


    private val tagPost = Constants.LogTag.Post


    //region:: Auth related work here

    suspend fun login(email: String, password: String): Pair<String?,String?> = AuthManager.loginUser(email,password)


    suspend fun signUp(user: User): Pair<FirebaseUser?,String?> = AuthManager.createUser(user)


    suspend fun saveUserToDatabase(user: User): Pair<Boolean, String?> =UserManager.saveUser(user)

    suspend fun getUser(userId: String): Flow<Resource<User>> = UserManager.getUserByIdAsync(userId)

    fun isUserLogin():Boolean=AuthManager.isUserLogin()


    //endregion

    //region:: Story Related work here
    suspend fun uploadStory(storyType: Constants.StoryTpye, uri:Uri?=null, text: StoryText?=null, user: User) = StoryManager.uploadStory(storyType,uri,text,user)

    suspend fun getAllStories(userId:String) = StoryManager.getAndListenTop30Friend(userId = userId)


    //endregion


    //region:: Uploading Post

    suspend fun uploadingPost(post: Post): UploadingResponse {
        MyLogger.v(tagPost, isFunctionCall = true)
        val isImagePresent = post.imageUrl != null
        val isVideoPresent = post.videoUrl != null

        return withContext(Dispatchers.IO) {
            try {
                val (onlineImageUri, onlineVideoUri) = if (isImagePresent || isVideoPresent) {
                    uploadMedia(post, isImagePresent, isVideoPresent)
                } else {
                    Pair(null, null)
                }
                savePostToDatabase(post, onlineImageUri, onlineVideoUri)
                AppBroadcastHelper.setPostUploadState(Constants.PostUploadState.PostUploaded, 0)
                UploadingResponse(true)
            } catch (e: Exception) {
                MyLogger.e(tagPost, "Failed to upload media: ${e.message}")
                AppBroadcastHelper.setPostUploadState(Constants.PostUploadState.PostUploaded, 0)
                UploadingResponse(false, errorMessage = e.message)
            }
        }
    }

    // Upload media (both video and image)
    private suspend fun uploadMedia(
        post: Post,
        isImagePresent: Boolean,
        isVideoPresent: Boolean
    ): Pair<Uri?, Uri?> {
        MyLogger.v(tagPost, isFunctionCall = true)
        var videoUri: Uri? = null
        var imageUri: Uri? = null

        if (isVideoPresent) {
            MyLogger.w(tagPost, msg = "Now video is uploading !")
            videoUri = uploadVideo(post.videoUrl!!)
            if (videoUri == null) {
                MyLogger.e(tagPost, msg = "Failed to upload video !")
                throw Exception("Failed to upload video")
            }
        }

        if (isImagePresent) {
            MyLogger.w(tagPost, msg = "Now image is uploading !")
            imageUri = uploadImage(post.imageUrl!!)
            if (imageUri == null) {
                MyLogger.e(tagPost, msg = "Failed to upload image !")
                throw Exception("Failed to upload image")
            }
        }

        return Pair(imageUri, videoUri)
    }

    // Upload video to Firebase Storage
    private suspend fun uploadVideo(videoUrl: String): Uri? {
//        MyLogger.v(tagPost, isFunctionCall = true)
//        val videoUri = videoUrl.convertParseUri()
//        val storageReferenceChild = storageReference.child(Constants.Table.Post.name)
//            .child(Constants.FolderName.PostVideo.name)
//            .child(videoUri.lastPathSegment ?: "video.mp4")
//
//        return try {
//            storageReferenceChild.putFile(videoUri).addOnProgressListener {
//                val progress = calculateProgress(it)
//                MyLogger.d(tagPost, msg = "Video uploading in progress .... $progress")
//                AppBroadcastHelper.setPostUploadState(
//                    Constants.PostUploadState.VideoUploading,
//                    progress
//                )
//            }.await()
//            val downloadUrl = storageReferenceChild.downloadUrl.await()
//            AppBroadcastHelper.setPostUploadState(Constants.PostUploadState.VideoUploaded, 0)
//            downloadUrl
//        } catch (e: Exception) {
//            MyLogger.e(tagPost, msg = "Some error occurred during uploading video:- ${e.message}")
//            handleUploadException(e)
//            null
//        }
        return null
    }


    // Upload image to Firebase Storage
    private suspend fun uploadImage(imageUrl: String): Uri? {
//        MyLogger.v(tagPost, isFunctionCall = true)
//        val imageUri = imageUrl.convertParseUri()
//        val storageReferenceChild = storageReference.child(Constants.Table.Post.name)
//            .child(Constants.FolderName.PostImage.name)
//            .child(imageUri.lastPathSegment ?: "image.jpg")
//
//        return try {
//            storageReferenceChild.putFile(imageUri).addOnProgressListener {
//                val progress = calculateProgress(it)
//                MyLogger.d(tagPost, msg = "Image uploading in progress .... $progress")
//                AppBroadcastHelper.setPostUploadState(
//                    Constants.PostUploadState.ImageUploading,
//                    progress
//                )
//            }.await()
//            val downloadUrl = storageReferenceChild.downloadUrl.await()
//            AppBroadcastHelper.setPostUploadState(Constants.PostUploadState.ImageUploaded, 0)
//            downloadUrl
//        } catch (e: Exception) {
//            MyLogger.e(tagPost, msg = "Some error occurred during uploading image:- ${e.message}")
//            handleUploadException(e)
//            null
//        }
        return null
    }

    private suspend fun savePostToDatabase(post: Post, imageUrl: Uri?, videoUrl: Uri?) {
//        MyLogger.v(tagPost, isFunctionCall = true)
//        AppBroadcastHelper.setPostUploadState(Constants.PostUploadState.PostUploading, 0)
//        val saveDatabaseReference =
//            firestore.collection(Constants.Table.Post.name).document(post.userId!!)
//                .collection(Helper.getPostId(post.userId))
//                .document(post.postId!!)
//        val updatedPost = post.copy(
//            imageUrl = imageUrl?.toString(),
//            videoUrl = videoUrl?.toString(),
//            postUploadingTimeInTimeStamp = System.currentTimeMillis()
//        )
//        try {
//            saveDatabaseReference.set(updatedPost).await()
//            MyLogger.i(tagPost, msg = "Post saved successfully.")
//        } catch (e: Exception) {
//            MyLogger.e(tagPost, msg = "Failed to save post: ${e.message}")
//            throw e
//        }
    }

    // Handle upload exceptions
    private fun handleUploadException(e: Exception) {
        when (e) {
            is StorageException -> {
                when (e.errorCode) {
                    StorageException.ERROR_QUOTA_EXCEEDED -> {
                        MyLogger.e(tagPost, msg = "Upload failed: Quota exceeded.")
                    }

                    StorageException.ERROR_NOT_AUTHENTICATED -> {
                        MyLogger.e(tagPost, msg = "Upload failed: User not authenticated.")
                    }

                    StorageException.ERROR_NOT_AUTHORIZED -> {
                        MyLogger.e(tagPost, msg = "Upload failed: User not authorized.")
                    }

                    else -> {
                        MyLogger.e(tagPost, msg = "Upload failed: ${e.message}")
                    }
                }
            }

            is IllegalArgumentException -> {
                MyLogger.e(tagPost, msg = "Upload failed: Invalid argument - ${e.message}")
            }

            is IOException -> {
                MyLogger.e(tagPost, msg = "Upload failed: Network error - ${e.message}")
            }

            else -> {
                MyLogger.e(tagPost, msg = "Upload failed: ${e.message}")
            }
        }
    }

    //endregion



}