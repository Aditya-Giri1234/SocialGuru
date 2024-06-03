package com.aditya.socialguru.domain_layer.service

import android.net.Uri
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.story.StoryText
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.domain_layer.service.firebase_service.PostManager
import com.aditya.socialguru.domain_layer.service.firebase_service.StoryManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

object FirebaseManager {


    private val tagPost = Constants.LogTag.Post


    //region:: Auth related work here

    suspend fun login(email: String, password: String): Pair<String?,String?> = AuthManager.loginUser(email,password)


    suspend fun signUp(user: User): Pair<FirebaseUser?,String?> = AuthManager.createUser(user)




    fun isUserLogin():Boolean=AuthManager.isUserLogin()


    //endregion


    //region:: User related work here
    suspend fun saveUserToDatabase(user: User): Pair<Boolean, String?> =UserManager.saveUser(user)

    suspend fun getUser(userId: String): Flow<Resource<User>> = UserManager.getUserByIdAsync(userId)

    suspend fun subscribeToFollowerCount(userId: String) = UserManager.subscribeToFollowerCount(userId)

    suspend fun subscribeToFollowingCount(userId: String) = UserManager.subscribeToFollowingCount(userId)



    //endregion

    //region:: Story Related work here
    suspend fun uploadStory(storyType: Constants.StoryType, uri:Uri?=null, text: StoryText?=null, user: User) = StoryManager.uploadStory(storyType,uri,text,user)

    suspend fun getAllStories(userId:String) = StoryManager.getAndListenTop30Friend(userId = userId)

    suspend fun getMyStory(userId: String)=StoryManager.getMyStory(userId)

    suspend fun deleteStoryById(storyId: String)=StoryManager.deleteStoryById(storyId)

    //endregion


    //region::  Post Related Work Here

    suspend fun uploadingPost(post: Post)= PostManager.uploadPost(post)

    suspend fun getDiscoverPost()=PostManager.getDiscoverPost()
    suspend fun getFollowingPost()=PostManager.getFollowingPost()

    suspend fun subscribeToPostCount(userId: String) = UserManager.subscribeToPostCount(userId)

    suspend fun subscribeToLikeCount(userId: String) = UserManager.subscribeToLikeCount(userId)

    //endregion



}