package com.aditya.socialguru.domain_layer.repository.post.friend_circle

import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager

class FollowerRepository {
    suspend fun getFollowerAndListenChangeEvent()=FirebaseManager.getFollowerListAndListenChange()
    suspend fun removeFollower(userId: String)= FirebaseManager.removeFollower(userId)
}