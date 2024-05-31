package com.aditya.socialguru.domain_layer.service

import android.net.Uri
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.UploadingResponse
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.story.Stories
import com.aditya.socialguru.data_layer.model.story.StoryText
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.domain_layer.service.firebase_service.PostManager
import com.aditya.socialguru.domain_layer.service.firebase_service.StoryManager
import com.aditya.socialguru.domain_layer.service.firebase_service.UserManager
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.system.measureTimeMillis

object FirebaseManager {


    private val tagPost = Constants.LogTag.Post


    //region:: Auth related work here

    suspend fun login(email: String, password: String): Pair<String?,String?> = AuthManager.loginUser(email,password)


    suspend fun signUp(user: User): Pair<FirebaseUser?,String?> = AuthManager.createUser(user)


    suspend fun saveUserToDatabase(user: User): Pair<Boolean, String?> =UserManager.saveUser(user)

    suspend fun getUser(userId: String): Flow<Resource<User>> = UserManager.getUserByIdAsync(userId)

    fun isUserLogin():Boolean=AuthManager.isUserLogin()


    //endregion

    //region:: Story Related work here
    suspend fun uploadStory(storyType: Constants.StoryTpye, uri:Uri?=null, text: StoryText?=null, user: User) = StoryManager.uploadStory(storyType,uri,text,user)

    suspend fun getAllStories(userId:String) = StoryManager.getAndListenTop30Friend(userId = userId)

    suspend fun getMyStory(userId: String)=StoryManager.getMyStory(userId)

    suspend fun deleteStoryById(storyId: String)=StoryManager.deleteStoryById(storyId)

    //endregion


    //region::  Post Related Work Here

    suspend fun uploadingPost(post: Post)= PostManager.uploadPost(post)

    suspend fun getDiscoverPost()=PostManager.getDiscoverPost()


    //endregion



}