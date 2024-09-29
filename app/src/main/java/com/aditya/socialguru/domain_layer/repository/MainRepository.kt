package com.aditya.socialguru.domain_layer.repository

import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.RetrofitInstance
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.domain_layer.service.firebase_service.PostManager
import com.aditya.socialguru.domain_layer.service.firebase_service.StoryManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager

class MainRepository {
    suspend fun subscribeToCurrentUser(userId: String) =
        FirebaseManager.subscribeToCurrentUser(userId)

    suspend fun setFcmToken(token: String?) = FirebaseManager.setFcmToken(token)

    suspend fun updateUserAvailability(isUserAvailable:Boolean) = UserManager.updateUserAvailability(isUserAvailable)

    suspend fun listenMySavedPost() = FirebaseManager.listenMySavedPost()
    suspend fun listenMyLikedPost() = FirebaseManager.listenMyLikedPost()

    suspend fun listenAuthOfUser() = FirebaseManager.listenAuthOfUser()

    suspend fun getLatestAppUpdate(packageName:String) = RetrofitInstance.updateService(packageName).latestRelease(Constants.DEFAULT_USER,Constants.DEFAULT_REPO)
}