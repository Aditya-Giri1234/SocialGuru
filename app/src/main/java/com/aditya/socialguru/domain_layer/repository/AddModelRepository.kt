package com.aditya.socialguru.domain_layer.repository

import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.domain_layer.service.FirebaseManager

class AddModelRepository {


    suspend fun uploadPost(post: Post)=  FirebaseManager.uploadingPost(post)

}