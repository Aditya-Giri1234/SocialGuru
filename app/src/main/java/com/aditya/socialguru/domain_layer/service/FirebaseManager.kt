package com.aditya.socialguru.domain_layer.service

import android.net.Uri
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.story.Stories
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
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
import kotlin.system.measureTimeMillis

object FirebaseManager {

    private val tagLogin = Constants.LogTag.LogIn
    private val tagStory = Constants.LogTag.Story

    // Firebase database reference
    private val databaseReference =
        FirebaseDatabase.getInstance("https://socialguru-706ba-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    // Firebase storage reference
    private val storageReference = FirebaseStorage.getInstance().reference

    //Firebase Auth reference
    private val auth = FirebaseAuth.getInstance()


    //region:: Utility Function
    fun isUserLogin():Boolean{
        return auth.currentUser!=null
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
        AppBroadcastHelper.setUploadState(Constants.StoryUploadState.StartUploading)
    }

    private fun uploadToStorage(imageUri: Uri, user: User) {
        val storageRef = storageReference.child("${Constants.FolderName.StoryImage.name}/${imageUri.lastPathSegment}")
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
        AppBroadcastHelper.setUploadState(Constants.StoryUploadState.Uploading, progress)
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
                AppBroadcastHelper.setUploadState(Constants.StoryUploadState.UrlNotGet)
            }
        }
    }

    private fun addStoryToDatabase(imageUrl: String, user: User) {
        user.userId?.let{ userId ->
            val storyId="${user.userId}${Helper.generateUUID()}"
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
                        AppBroadcastHelper.setUploadState(Constants.StoryUploadState.StoryUploadedSuccessfully)
                    } else {
                        MyLogger.e(tagStory, msg = "Story add in database failed !")
                        AppBroadcastHelper.setUploadState(Constants.StoryUploadState.UploadingFail)
                    }
                }
        }
    }

    private fun uploadFailed() {
        MyLogger.e(tagStory, msg = "Story uploaded failed ...")
        AppBroadcastHelper.setUploadState(Constants.StoryUploadState.UploadingFail)
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
                   val result= trySend(Resource.Success(user))
                    MyLogger.v(tagLogin, msg = "Value is sending successful ${result.isSuccess} !")

                }

                override fun onCancelled(error: DatabaseError) {
                    MyLogger.e(tagLogin, msg = "Some error occurred during get User :- ${error.message}")
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
    fun signOutUser(){
        if (auth.currentUser!=null){
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

                    val time= measureTimeMillis {
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

}