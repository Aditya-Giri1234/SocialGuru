package com.aditya.socialguru.domain_layer.repository

import android.net.Uri
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.FirebaseManager

class HomeRepository {

    fun storeImageInFirebaseStorage(image: Uri,user:User){
        MyLogger.v(Constants.LogTag.Story, isFunctionCall = true)

        FirebaseManager.uploadStory(image,user)
    }

    fun getAllStory(callback: (List<UserStories>?, error: String?) -> Unit)=FirebaseManager.getAllStories(callback)
}