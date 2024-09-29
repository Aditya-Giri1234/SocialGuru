package com.aditya.socialguru.domain_layer.repository.post

import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.PostManager

class DetailPostRepository {

    suspend fun getPostById(postId:String)= FirebaseManager.getPostById(postId)
    suspend fun updateLikeCount(postId: String ,postCreatorUserId:String, isLiked: Boolean) = FirebaseManager.updateLikeCount(postId ,postCreatorUserId,isLiked)

    suspend fun getPostLikeUser(postId: String) = PostManager.getPostLikeUser(postId)

    suspend fun updatePostSaveStatus(postId: String) = FirebaseManager.updatePostSaveStatus(postId)
}