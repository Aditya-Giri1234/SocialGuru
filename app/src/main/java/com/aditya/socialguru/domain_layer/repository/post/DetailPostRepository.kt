package com.aditya.socialguru.domain_layer.repository.post

import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.PostManager

class DetailPostRepository {

    suspend fun getPostById(postId:String)= FirebaseManager.getPostById(postId)
}