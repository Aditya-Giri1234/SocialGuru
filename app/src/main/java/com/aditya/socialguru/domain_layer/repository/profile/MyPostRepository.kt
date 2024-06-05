package com.aditya.socialguru.domain_layer.repository.profile

import com.aditya.socialguru.domain_layer.service.FirebaseManager

class MyPostRepository {
    suspend fun getMyPost(userId:String)=FirebaseManager.getMyPost(userId)
    suspend fun getUser(userId: String)=FirebaseManager.getUser(userId)
}