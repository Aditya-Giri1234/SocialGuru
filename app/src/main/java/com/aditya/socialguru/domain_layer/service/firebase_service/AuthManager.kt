package com.aditya.socialguru.domain_layer.service.firebase_service

import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


/**
 * [AuthManager] This class only do authentication part of app.
 * */
object AuthManager {

    //Firebase Auth reference
    private val auth = FirebaseAuth.getInstance()
    private val userRef by lazy {
        FirebaseFirestore.getInstance().collection(Constants.Table.User.name)
            .document(currentUserId()!!)
    }

    private val tagLogin = Constants.LogTag.LogIn
    private val tagDelete = Constants.LogTag.ForceLogout


    suspend fun createUser(user: User): Pair<FirebaseUser?, String?> {
        if (user.userPassword != null && user.userEmailId != null) {
            return try {
                val result =
                    auth.createUserWithEmailAndPassword(user.userEmailId, user.userPassword).await()
                MyLogger.v(tagLogin, msg = "Create user successfully !")
                Pair(result.user, null)
            } catch (e: Exception) {
                e.printStackTrace()
                MyLogger.e(tagLogin, msg = "Some error occurred during create user :- ${e.message}")
                Pair(null, e.message)
            }
        }
        return Pair(null, "Email and Password must not null !")

    }

    suspend fun loginUser(email: String, password: String): Pair<String?, String?> {
        MyLogger.v(tagLogin, isFunctionCall = true)
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                MyLogger.i(tagLogin, msg = "User is found !")
                UserManager.updateUserPassword(password)
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


    fun isUserLogin(): Boolean {
        return auth.currentUser != null
    }

    suspend fun signOutUser() = callbackFlow<UpdateResponse> {
        if (auth.currentUser != null) {
            UserManager.logoutUser().onEach {
                auth.signOut()
                trySend(UpdateResponse(true, ""))
            }.launchIn(this)
        } else {
            trySend(UpdateResponse(false, "User not found !"))
        }
        awaitClose {
            close()
        }
    }


    fun currentUserId() = auth.currentUser?.uid


    //region:: Delete Account
    suspend fun deleteAccount() = callbackFlow<UpdateResponse> {
        try {
            MyLogger.e(tagDelete, isFunctionCall = true)
            MyLogger.w(tagDelete, msg = "Delete Account Start ....")
            // Run the deletion tasks in parallel and await their completion
            val chatDeleteJob = async {
                MyLogger.d(tagDelete, msg = "Delete Chat is now calling ...")
                ChatManager.deleteUserChatInfo() // Collect the flow to wait for completion
                MyLogger.d(tagDelete, msg = "Delete Chat is already Called.")
            }
            val postDeleteJob = async {
                MyLogger.d(tagDelete, msg = "Delete Post is now calling ...")
                PostManager.deleteAllMyPost().first() // Collect the flow to wait for completion
                MyLogger.d(tagDelete, msg = "Delete Post is already Called.")
            }
            val storiesDeleteJob = async {
                MyLogger.d(tagDelete, msg = "Delete Stories is now calling ...")
                StoryManager.deleteAllMyStories().first() // Collect the flow to wait for completion
                MyLogger.d(tagDelete, msg = "Delete Stories is already Called.")
            }
            val myCommentDeleteJob = async {
                MyLogger.d(tagDelete, msg = "Delete Relation is now calling ...")
                CommentManager.deleteMyAllCommentFromEveryPost()
                    .first() // Assuming this does not return a flow
                MyLogger.d(tagDelete, msg = "Delete Relation is already Called.")
            }
            val relationDeleteJob = async {
                MyLogger.d(tagDelete, msg = "Delete Relation is now calling ...")
                UserManager.deleteMyUserRelation() // Assuming this does not return a flow
                MyLogger.d(tagDelete, msg = "Delete Relation is already Called.")
            }


// Wait for all deletion tasks to complete
            chatDeleteJob.await()
            postDeleteJob.await()
            storiesDeleteJob.await()
            myCommentDeleteJob.await()
            relationDeleteJob.await()
            MyLogger.w(tagDelete, msg = "Chat , Post , Stories , Relation Delete Successfully !")

            //Now Delete Sub Collection of My
            UserManager.deleteAllMySubCollection().onEach {
                // Now Delete User Collection
                MyLogger.w(tagDelete, msg = "Delete User Collection Start ....")
                userRef.delete().addOnCompleteListener {
                    if (it.isSuccessful) {
                        MyLogger.w(tagDelete, msg = "Delete User Collection Deleted Successfully !")
                        auth.currentUser!!.delete().addOnCompleteListener {
                            if (it.isSuccessful) {
                                MyLogger.w(tagDelete, msg = "Delete Account Deleted Successfully !")
                                trySend(UpdateResponse(true, ""))
                            } else {
                                MyLogger.w(tagDelete, msg = "Delete Account End ....")
                                trySend(UpdateResponse(false, it.exception?.message))
                            }
                        }
                    } else {
                        MyLogger.w(tagDelete, msg = "Delete Account End ....")
                        trySend(UpdateResponse(false, it.exception?.message))
                    }
                }
            }.launchIn(this)

        } catch (e: Exception) {
            e.printStackTrace()
        }


        awaitClose {
            close()
        }
    }
    //endregion


    //region :: User Setting

    suspend fun sendPasswordResetEmail(email: String?) =
        callbackFlow<UpdateResponse> {
            val finalEmail = email ?: auth.currentUser!!.email!!
            auth.sendPasswordResetEmail(finalEmail).addOnCompleteListener {
                if (it.isSuccessful) {
                    trySend(UpdateResponse(true, ""))
                } else {
                    trySend(UpdateResponse(false, it.exception?.message))
                }
            }

            awaitClose {
                close()
            }
        }


    suspend fun updateUserEmailId(newEmailId: String, password: String) =
        callbackFlow<UpdateResponse> {
            val credential = EmailAuthProvider
                .getCredential(auth.currentUser!!.email!!, password)
            auth.currentUser!!.reauthenticate(credential).addOnCompleteListener {
                if (it.isSuccessful) {
                    auth.currentUser!!.verifyBeforeUpdateEmail(newEmailId).addOnCompleteListener {
                        if (it.isSuccessful) {
                            launch {
                                UserManager.updateUserEmail(newEmailId).first()
                                trySend(UpdateResponse(true, ""))
                            }
                        } else {
                            trySend(UpdateResponse(false, it.exception?.message))
                        }
                    }
                } else {
                    trySend(UpdateResponse(false, it.exception?.message))
                }
            }
            awaitClose {
                close()
            }
        }

    suspend fun updateUserPassword(currentPassword: String, newPassword: String) =
        callbackFlow<UpdateResponse> {
            val credential = EmailAuthProvider
                .getCredential(auth.currentUser!!.email!!, currentPassword)
            auth.currentUser!!.reauthenticate(credential).addOnCompleteListener {
                if (it.isSuccessful) {
                    auth.currentUser!!.updatePassword(newPassword).addOnCompleteListener {
                        if (it.isSuccessful) {
                            launch {
                                val result = UserManager.updateUserPassword(newPassword).first()
                                if (result.isSuccess) {
                                    MyLogger.i(
                                        tagLogin,
                                        msg = "Password updated in Firestore: $newPassword"
                                    )
                                    trySend(UpdateResponse(true, ""))
                                } else {
                                    MyLogger.e(
                                        tagLogin,
                                        msg = "Failed to update password in Firestore :->  ${result.errorMessage}"
                                    )
                                    trySend(UpdateResponse(false, "Failed to update password in Firestore :->  ${result.errorMessage}"))
                                }
                            }
                        } else {
                            trySend(UpdateResponse(false, it.exception?.message))
                        }
                    }
                } else {
                    trySend(UpdateResponse(false, it.exception?.message))
                }
            }
            awaitClose {
                close()
            }
        }


    //endregion

    //region:: Auth State Listener

    suspend fun listenAuthOfUser() = callbackFlow<UpdateResponse> {

        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            user?.let {
                // Update Firestore with the new email
                val updatedEmail = it.email
                if (updatedEmail != null) {
                    launch {
                        val result = UserManager.updateUserEmail(updatedEmail).first()
                        if (result.isSuccess) {
                            MyLogger.i(tagLogin, msg = "Email updated in Firestore: $updatedEmail")
                        } else {
                            MyLogger.e(
                                tagLogin,
                                msg = "Failed to update email in Firestore :->  ${result.errorMessage}"
                            )
                        }
                    }
                }
            }
        }

        auth.addAuthStateListener(authListener)
        awaitClose {
            auth.removeAuthStateListener(authListener)
            close()
        }
    }


    //endregion
}