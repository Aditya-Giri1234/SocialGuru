package com.aditya.socialguru.domain_layer.repository

import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager

class MainRepository {
    suspend fun subscribeToCurrentUser(userId: String) =
        FirebaseManager.subscribeToCurrentUser(userId)

    suspend fun setFcmToken(token: String?) = FirebaseManager.setFcmToken(token)

    suspend fun updateUserAvailability(isUserAvailable:Boolean) = UserManager.updateUserAvailability(isUserAvailable)
}