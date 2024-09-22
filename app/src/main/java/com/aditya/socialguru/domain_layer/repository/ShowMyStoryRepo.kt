package com.aditya.socialguru.domain_layer.repository

import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.firebase_service.StoryManager

class ShowMyStoryRepo {

    suspend fun getMyStory(userId: String)= FirebaseManager.getMyStory(userId)

    suspend fun deleteStoryById(storyId: String)= FirebaseManager.deleteStoryById(storyId)

    suspend fun deleteAllMyStories() = FirebaseManager.deleteAllMyStories()
}