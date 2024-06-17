package com.aditya.socialguru.domain_layer.repository.profile

import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.PostManager

class MyPostRepository {
    suspend fun getMyPost(userId:String)=FirebaseManager.getMyPost(userId)
    suspend fun getUser(userId: String)=FirebaseManager.getUser(userId)
    suspend fun getMyLikedPost() = PostManager.getMyLikedPost()

    suspend fun updateLikeCount(postId: String ,postCreatorUserId:String, isLiked: Boolean) = FirebaseManager.updateLikeCount(postId ,postCreatorUserId,isLiked)
}