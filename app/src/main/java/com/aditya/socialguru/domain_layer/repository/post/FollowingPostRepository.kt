package com.aditya.socialguru.domain_layer.repository.post

import com.aditya.socialguru.domain_layer.service.FirebaseManager

class FollowingPostRepository {

    suspend fun getFollowingPost()= FirebaseManager.getFollowingPost()
    suspend fun getUser(userId:String)= FirebaseManager.getUser(userId)
}