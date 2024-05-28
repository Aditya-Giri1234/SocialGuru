package com.aditya.socialguru.domain_layer.repository

import android.net.Uri
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.story.StoryText
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.StoryManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager
import kotlinx.coroutines.flow.Flow

class HomeRepository {

    suspend fun uploadStory(storyType: Constants.StoryTpye, uri:Uri?=null, text: StoryText?=null, user: User)=
        FirebaseManager.uploadStory(storyType,uri,text,user)

    suspend fun getAllStory(userId:String)=FirebaseManager.getAllStories(userId)

    suspend fun getUser(userId: String): Flow<Resource<User>> = FirebaseManager.getUser(userId)


}