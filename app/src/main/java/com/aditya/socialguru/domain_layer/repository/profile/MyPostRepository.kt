package com.aditya.socialguru.domain_layer.repository.profile

import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.PostManager

class MyPostRepository {
    suspend fun getMyPost(userId:String)=FirebaseManager.getMyPost(userId)
    suspend fun getUser(userId: String)=FirebaseManager.getUser(userId)
    suspend fun getMyLikedPost(userId: String) = FirebaseManager.getMyLikedPost(userId)
    suspend fun deletePostById(postId: String) = FirebaseManager.deletePostById(postId)
    suspend fun deleteAllMyPost() = FirebaseManager.deleteAllMyPost()
    suspend fun subscribeToPostCount(userId: String) = FirebaseManager.subscribeToPostCount(userId)

    suspend fun updateLikeCount(postId: String ,postCreatorUserId:String, isLiked: Boolean) = FirebaseManager.updateLikeCount(postId ,postCreatorUserId,isLiked)

    suspend fun updatePostSaveStatus(postId: String) = FirebaseManager.updatePostSaveStatus(postId)
    suspend fun listenCommentedPost(userId: String) = FirebaseManager.listenCommentedPost(userId)

    suspend fun listenMySavedPostForScreenView() = FirebaseManager.listenMySavedPostForScreenView()


}