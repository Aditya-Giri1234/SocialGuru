package com.aditya.socialguru.domain_layer.repository

import android.adservices.adid.AdId
import com.aditya.socialguru.domain_layer.service.FirebaseManager

class MainRepository {
    suspend fun subscribeToCurrentUser(userId: String)= FirebaseManager.subscribeToCurrentUser(userId)
}