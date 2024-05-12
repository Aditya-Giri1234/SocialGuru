package com.aditya.socialguru.domain_layer.service

import android.net.Uri
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.Stories
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object FirebaseManager {

    private val tagLogin = Constants.LogTag.LogIn

    // Firebase database reference
    private val databaseReference =
        FirebaseDatabase.getInstance("https://socialguru-706ba-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    // Firebase storage reference
    private val storageReference = FirebaseStorage.getInstance().reference

    //Firebase Auth reference
    private val auth = FirebaseAuth.getInstance()


    fun isUserLogin():Boolean{
        return auth.currentUser!=null
    }

    fun uploadStory(imageUri: Uri) {
        AppBroadcastHelper.setUploadState(Constants.StoryUploadState.StartUploading)
        storageReference.child("${Constants.FolderName.StoryImage.name}/${imageUri.lastPathSegment}")
            .putFile(imageUri).addOnProgressListener { taskSnapshot ->
                val progress =
                    (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                AppBroadcastHelper.setUploadState(Constants.StoryUploadState.Uploading, progress)
            }.addOnCompleteListener {
                if (it.isSuccessful) {
                    AppBroadcastHelper.setUploadState(Constants.StoryUploadState.SavingStory)
                    it.result.storage.downloadUrl.addOnCompleteListener {
                        if (it.isSuccessful) {
                            databaseReference.child("file").push().setValue(
                                Stories(
                                    Constants.StoryType.AvailableStory,
                                    34,
                                    "dkfjk",
                                    "dkf",
                                    it.result,
                                    34,
                                    System.currentTimeMillis()
                                )
                            ).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    AppBroadcastHelper.setUploadState(Constants.StoryUploadState.StoryUploadedSuccessfully)
                                } else {
                                    AppBroadcastHelper.setUploadState(Constants.StoryUploadState.UploadingFail)
                                }
                            }
                        } else {
                            AppBroadcastHelper.setUploadState(Constants.StoryUploadState.UrlNotGet)
                        }
                    }
                } else {
                    AppBroadcastHelper.setUploadState(Constants.StoryUploadState.UploadingFail)
                }
            }
    }

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

    suspend fun getUserById(userId: String): Flow<Resource<User>> {
        MyLogger.v(tagLogin, isFunctionCall = true)
        return callbackFlow<Resource<User>> {
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    MyLogger.v(tagLogin, msg = "Value is now send !")
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

    fun signOutUser(){
        if (auth.currentUser!=null){
            auth.signOut()
        }
    }

}