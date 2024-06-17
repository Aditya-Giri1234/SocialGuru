package com.aditya.socialguru.domain_layer.repository.post

import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.PostManager

class FollowingPostRepository {

    suspend fun getFollowingPost()= FirebaseManager.getFollowingPost()
    suspend fun getUser(userId:String)= FirebaseManager.getUser(userId)

    suspend fun updateLikeCount(postId: String ,postCreatorUserId:String, isLiked: Boolean) = FirebaseManager.updateLikeCount(postId ,postCreatorUserId,isLiked)

}