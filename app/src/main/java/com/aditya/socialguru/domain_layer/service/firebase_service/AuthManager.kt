package com.aditya.socialguru.domain_layer.service.firebase_service

import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


/**
 * [AuthManager] This class only do authentication part of app.
 * */
object AuthManager {

    //Firebase Auth reference
    private val auth = FirebaseAuth.getInstance()

    private val tagLogin = Constants.LogTag.LogIn


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

    fun signOutUser(): UpdateResponse {
        return if (auth.currentUser != null) {
            auth.signOut()
            UpdateResponse(true, "")
        } else {
            UpdateResponse(false, "User not found !")
        }
    }


    fun currentUserId() = auth.currentUser?.uid


}