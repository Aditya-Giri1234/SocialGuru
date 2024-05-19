package com.aditya.socialguru.domain_layer.repository

import com.aditya.socialguru.data_layer.model.UploadingResponse
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import kotlinx.coroutines.flow.Flow

class AddModelRepository {


    suspend fun uploadPost(post: Post):UploadingResponse{
        MyLogger.v(Constants.LogTag.Post, isFunctionCall = true)
        return FirebaseManager.uploadingPost(post)
    }

}