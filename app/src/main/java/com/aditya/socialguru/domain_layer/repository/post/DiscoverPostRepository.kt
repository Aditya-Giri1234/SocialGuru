package com.aditya.socialguru.domain_layer.repository.post

import com.aditya.socialguru.domain_layer.service.FirebaseManager

class DiscoverPostRepository {

    suspend fun getDiscoverPost()=FirebaseManager.getDiscoverPost()
    suspend fun getUser(userId:String)=FirebaseManager.getUser(userId)

    suspend fun updateLikeCount(postId: String ,postCreatorUserId:String, isLiked: Boolean) = FirebaseManager.updateLikeCount(postId ,postCreatorUserId,isLiked)

}