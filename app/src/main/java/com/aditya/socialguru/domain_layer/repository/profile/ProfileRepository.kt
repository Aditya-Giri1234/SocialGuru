package com.aditya.socialguru.domain_layer.repository.profile

import com.aditya.socialguru.domain_layer.service.FirebaseManager

class ProfileRepository {
    suspend fun subscribeToFollowerCount(userId: String) =
        FirebaseManager.subscribeToFollowerCount(userId)

    suspend fun subscribeToFollowingCount(userId: String) =
        FirebaseManager.subscribeToFollowingCount(userId)


    suspend fun subscribeToPostCount(userId: String) = FirebaseManager.subscribeToPostCount(userId)

    suspend fun subscribeToLikeCount(userId: String) = FirebaseManager.subscribeToLikeCount(userId)
}