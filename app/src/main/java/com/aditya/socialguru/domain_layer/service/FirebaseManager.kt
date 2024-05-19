package com.aditya.socialguru.domain_layer.service

import android.net.Uri
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.UploadingResponse
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.story.Stories
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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

    private val tagLogin = Constants.LogTag.LogIn
    private val tagStory = Constants.LogTag.Story
    private val tagPost=Constants.LogTag.Post

    // Firebase database reference
    private val databaseReference =
        FirebaseDatabase.getInstance("https://socialguru-706ba-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    // Firebase storage reference
    private val storageReference = FirebaseStorage.getInstance().reference

    //Firebase Auth reference
    private val auth = FirebaseAuth.getInstance()


    //region:: Utility Function
    fun isUserLogin(): Boolean {
        return auth.currentUser != null
    }


    //endregion


    //region:: Upload Story
    fun uploadStory(imageUri: Uri, user: User) {
        startUploading()
        uploadToStorage(imageUri, user)
    }

    private fun startUploading() {
        MyLogger.v(tagStory, isFunctionCall = true)
        MyLogger.v(tagStory, msg = "Story uploading starting ...")
        AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.StartUploading)
    }

    private fun uploadToStorage(imageUri: Uri, user: User) {
        val storageRef =
            storageReference.child("${Constants.FolderName.StoryImage.name}/${imageUri.lastPathSegment}")
        storageRef.putFile(imageUri)
            .addOnProgressListener { taskSnapshot ->
                val progress = calculateProgress(taskSnapshot)
                updateProgress(progress)
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    uploadSuccessful(task, user)
                } else {
                    uploadFailed()
                }
            }
    }

    private fun calculateProgress(taskSnapshot: UploadTask.TaskSnapshot): Int {
        return (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
    }

    private fun updateProgress(progress: Int) {
        MyLogger.v(tagStory, msg = "Story uploading in progress ... $progress")
        AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.Uploading, progress)
    }

    private fun uploadSuccessful(task: Task<UploadTask.TaskSnapshot>, user: User) {
        MyLogger.i(tagStory, msg = "Story uploaded successfully ...")
        saveStoryUrl(task.result, user)
    }

    private fun saveStoryUrl(taskSnapshot: UploadTask.TaskSnapshot, user: User) {
        taskSnapshot.storage.downloadUrl.addOnCompleteListener { urlResult ->
            if (urlResult.isSuccessful) {
                MyLogger.i(tagStory, msg = "Story url download successfully ...")
                addStoryToDatabase(urlResult.result.toString(), user)
            } else {
                MyLogger.e(tagStory, msg = "Story url download failed  ...")
                AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.UrlNotGet)
            }
        }
    }

    private fun addStoryToDatabase(imageUrl: String, user: User) {
        user.userId?.let { userId ->
            val storyId = "${user.userId}${Helper.generateUUID()}"
            val story = Stories(
                user.userId,
                storyId,
                user.userName,
                user.userProfileImage,
                imageUrl,
                0,
                System.currentTimeMillis()
            )
            databaseReference.child(Constants.Table.Stories.name)
                .child(userId)
                .child(storyId)
                .setValue(story)
                .addOnCompleteListener { addDataBase ->
                    if (addDataBase.isSuccessful) {
                        MyLogger.i(tagStory, msg = "Story add in database successful !")
                        AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.StoryUploadedSuccessfully)
                    } else {
                        MyLogger.e(tagStory, msg = "Story add in database failed !")
                        AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.UploadingFail)
                    }
                }
        }
    }

    private fun uploadFailed() {
        MyLogger.e(tagStory, msg = "Story uploaded failed ...")
        AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.UploadingFail)
    }


    //endregion


    //region:: Create User and save to database
    suspend fun createUser(user: User): Pair<FirebaseUser?, String?> {
        if (user.userEmailId == null || user.userPassword == null) {
            return Pair(null, "Email and Password must not null !")
        }
        try {
            val result =
                auth.createUserWithEmailAndPassword(user.userEmailId, user.userPassword).await()
            MyLogger.v(tagLogin, msg = "Create user successfully !")
            return Pair(result.user, null)
        } catch (e: Exception) {
            e.printStackTrace()
            MyLogger.e(tagLogin, msg = "Some error occurred during create user :- ${e.message}")
            return Pair(null, e.message)
        }
    }

    suspend fun saveUser(user: User): Pair<Boolean, String?> {
        MyLogger.v(tagLogin, isFunctionCall = true)
        try {
            databaseReference.child(Constants.Table.User.name).child(user.userId!!).setValue(user)
                .await()
            MyLogger.d(tagLogin, msg = "User saved successfully without error")
            return Pair(true, null)
        } catch (e: Exception) {
            e.printStackTrace()
            MyLogger.e(tagLogin, msg = "Some error occurred during save user :- ${e.message}")
            return Pair(false, e.message)
        }
    }

    //endregion


    //region:: Login user and get user data and send back to ui screen
    suspend fun loginUser(email: String, password: String): Pair<String?, String?> {
        MyLogger.v(tagLogin, isFunctionCall = true)
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                MyLogger.i(tagLogin, msg = "User is found !")
                Pair(it.uid, null)
            } ?: run {
                MyLogger.e(tagLogin, msg = "User is not found !")
                Pair(null, "No user found with this credentials !")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            MyLogger.e(tagLogin, msg = "Some error occurred during login :- ${e.message}")
            Pair(null, "Some error occurred during login :- ${e.message}")
        }
    }

    //endregion


    //region:: Get user by id
    suspend fun getUserById(userId: String): Flow<Resource<User>> {
        MyLogger.v(tagLogin, isFunctionCall = true)
        return callbackFlow<Resource<User>> {
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    MyLogger.v(tagLogin, msg = "Value is now send - user->$user !")
                    val result = trySend(Resource.Success(user))
                    MyLogger.v(tagLogin, msg = "Value is sending successful ${result.isSuccess} !")

                }

                override fun onCancelled(error: DatabaseError) {
                    MyLogger.e(
                        tagLogin,
                        msg = "Some error occurred during get User :- ${error.message}"
                    )
                    trySend(Resource.Error("Some error occurred during get User :- ${error.message}"))
                }
            }

            try {
                databaseReference.child(Constants.Table.User.name).child(userId)
                    .addListenerForSingleValueEvent(valueEventListener)
            } catch (e: Exception) {
                e.printStackTrace()
                MyLogger.e(tagLogin, msg = "Some error occurred during get User :- ${e.message}")
                trySend(Resource.Error("Some error occurred during get User :- ${e.message}"))
            }

            awaitClose {
                // This block is called when the flow is cancelled
                // Close any resources here
                databaseReference.removeEventListener(valueEventListener)
            }
        }
    }

    //endregion


    //region:: Logout the user
    fun signOutUser() {
        if (auth.currentUser != null) {
            auth.signOut()
        }
    }

    //endregion

    //region:: Get all story


    fun getAllStories(callback: (List<UserStories>?, String?) -> Unit) {
        val storiesRef = databaseReference.child(Constants.Table.Stories.name)
        storiesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allUserStories = mutableListOf<UserStories>()

                CoroutineScope(Dispatchers.IO).launch {

                    val time = measureTimeMillis {
                        val deferredList = snapshot.children.map { userSnapshot ->
                            async {
                                MyLogger.d(tagStory, msg = "User id :- ${userSnapshot.key}")
                                val userId = userSnapshot.key ?: return@async null
                                val user = getUserById(userId).first().data
                                val stories = userSnapshot.children.mapNotNull { storySnapshot ->
                                    storySnapshot.getValue(Stories::class.java)
                                }
                                UserStories(user, stories)
                            }
                        }


                        val userStoriesList = deferredList.awaitAll().filterNotNull()
                        MyLogger.v(tagStory, msg = userStoriesList)
                        callback(userStoriesList, null)
                    }

                    MyLogger.w(tagStory, msg = "Time taken to get story is $time ms  😎!")

                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null, "Error occurred: ${error.message}")
            }
        })
    }


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
                AppBroadcastHelper.setPostUploadState(Constants.PostUploadState.PostUploaded,0)
                UploadingResponse(true)
            } catch (e: Exception) {
                MyLogger.e(tagPost, "Failed to upload media: ${e.message}")
                AppBroadcastHelper.setPostUploadState(Constants.PostUploadState.PostUploaded,0)
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
        MyLogger.v(tagPost, isFunctionCall = true)
        val videoUri = videoUrl.convertParseUri()
        val storageReferenceChild = storageReference.child(Constants.Table.Post.name)
            .child(Constants.FolderName.PostVideo.name).child(videoUri.lastPathSegment ?: "video.mp4")

        return try {
            storageReferenceChild.putFile(videoUri).addOnProgressListener {
                val progress = calculateProgress(it)
                MyLogger.d(tagPost, msg = "Video uploading in progress .... $progress")
                AppBroadcastHelper.setPostUploadState(Constants.PostUploadState.VideoUploading,progress)
            }.await()
            val downloadUrl = storageReferenceChild.downloadUrl.await()
            AppBroadcastHelper.setPostUploadState(Constants.PostUploadState.PostUploaded,0)
            downloadUrl
        } catch (e: Exception) {
            MyLogger.e(tagPost, msg = "Some error occurred during uploading video:- ${e.message}")
            handleUploadException(e)
            null
        }
    }



    // Upload image to Firebase Storage
    private suspend fun uploadImage(imageUrl: String): Uri? {
        MyLogger.v(tagPost, isFunctionCall = true)
        val imageUri = imageUrl.convertParseUri()
        val storageReferenceChild = storageReference.child(Constants.Table.Post.name)
            .child(Constants.FolderName.PostImage.name).child(imageUri.lastPathSegment ?: "image.jpg")

        return try {
            storageReferenceChild.putFile(imageUri).addOnProgressListener {
                val progress = calculateProgress(it)
                MyLogger.d(tagPost, msg = "Image uploading in progress .... $progress")
                AppBroadcastHelper.setPostUploadState(Constants.PostUploadState.ImageUploading,progress)
            }.await()
            val downloadUrl = storageReferenceChild.downloadUrl.await()
            AppBroadcastHelper.setPostUploadState(Constants.PostUploadState.ImageUploaded,0)
            downloadUrl
        } catch (e: Exception) {
            MyLogger.e(tagPost, msg = "Some error occurred during uploading image:- ${e.message}")
            handleUploadException(e)
            null
        }
    }

    private suspend fun savePostToDatabase(post: Post, imageUrl: Uri?, videoUrl: Uri?) {
        MyLogger.v(tagPost, isFunctionCall = true)
        AppBroadcastHelper.setPostUploadState(Constants.PostUploadState.PostUploading,0)
        val saveDatabaseReference = databaseReference.child(Constants.Table.Post.name).child(post.postId!!).child(post.userId!!)
        val updatedPost = post.copy(
            imageUrl = imageUrl?.toString(),
            videoUrl = videoUrl?.toString(),
            uploadingTime = System.currentTimeMillis()
        )
        try {
            saveDatabaseReference.setValue(updatedPost).await()
            MyLogger.i(tagPost, msg = "Post saved successfully.")
        } catch (e: Exception) {
            MyLogger.e(tagPost, msg = "Failed to save post: ${e.message}")
            throw e
        }
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