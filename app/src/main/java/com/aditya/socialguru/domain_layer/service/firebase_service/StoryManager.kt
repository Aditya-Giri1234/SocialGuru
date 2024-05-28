package com.aditya.socialguru.domain_layer.service.firebase_service

import android.net.Uri
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.story.Stories
import com.aditya.socialguru.data_layer.model.story.StoryListenerEmissionType
import com.aditya.socialguru.data_layer.model.story.StoryText
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.Table
import com.aditya.socialguru.domain_layer.helper.Constants.UserTable
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow


/**
 * [StoryManager] This class only do crud  operation on firebase firestore [com.aditya.socialguru.data_layer.model.story.Stories] collection and its subCollection..
 * */
object StoryManager {

    private val tagStory = Constants.LogTag.Story

    private val myStory: UserStories? = null

    //Firebase firestore
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var storyListener: ListenerRegistration? = null
    private var isFirstTimeStoryListnerCall = true

    private val StoryImageUploadingPath = "${Constants.Table.Stories.name}/Image/"


    //region:: Upload Story

    suspend fun uploadStory(
        storyType: Constants.StoryTpye,
        uri: Uri? = null,
        text: StoryText? = null,
        user: User
    ) {

        when (storyType) {
            Constants.StoryTpye.Text -> {
                uploadTextStory(text, user)
            }

            Constants.StoryTpye.Image -> {
                uploadImageStory(uri, user)
            }

            Constants.StoryTpye.Video -> {
                uploadVideoStory(uri, user)
            }
        }
    }

    private suspend fun uploadImageStory(uri: Uri?, user: User) {
        uri ?: return
        startUploading()
        uploadToStorage(uri).collect {
            val storyUrl = it.first
            val error = it.second
            if (storyUrl != null) {
                addStoryToDatabase(
                    storyUrl,
                    storyType = Constants.StoryTpye.Image.name,
                    user = user
                )
            }
        }
    }

    private suspend fun uploadVideoStory(uri: Uri?, user: User) {
        uri ?: return
        startUploading()
        uploadToStorage(uri).collect {
            val storyUrl = it.first
            val error = it.second
            if (storyUrl != null) {
                addStoryToDatabase(
                    storyUrl,
                    storyType = Constants.StoryTpye.Video.name,
                    user = user
                )
            }
        }
    }

    private fun uploadTextStory(text: StoryText?, user: User) {
        text ?: return
        startUploading()
        addStoryToDatabase(text = text, storyType = Constants.StoryTpye.Text.name, user = user)
    }

    private fun startUploading() {
        MyLogger.v(tagStory, isFunctionCall = true)
        MyLogger.v(tagStory, msg = "Story uploading starting ...")
        AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.StartUploading)
    }


    private suspend fun uploadToStorage(uri: Uri) = callbackFlow<Pair<String?, String?>> {
        StorageManager.uploadImageToServer(
            "$StoryImageUploadingPath${uri.lastPathSegment}",
            uri
        ).collect { status ->
            when (status.state) {
                Constants.StorageManagerState.InProgress -> {
                    MyLogger.v(tagStory, msg = "Story uploading in progress ... ${status.progress}")
                    AppBroadcastHelper.setStoryUploadState(
                        Constants.StoryUploadState.Uploading,
                        status.progress
                    )
                }

                Constants.StorageManagerState.Error -> {
                    MyLogger.e(tagStory, msg = "Story uploaded failed ...")
                    AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.UploadingFail)
                    trySend(Pair(null, status.error))
                }

                Constants.StorageManagerState.UrlNotGet -> {
                    MyLogger.e(tagStory, msg = "Story url download failed  ...")
                    AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.UrlNotGet)
                    trySend(Pair(null, status.error))
                }

                Constants.StorageManagerState.Success -> {
                    //Success means file stored in storage and url get successfully.
                    trySend(Pair(status.url, null))
                }
            }
        }
        awaitClose {
            channel.close()
        }
    }


    private fun addStoryToDatabase(
        storyUrl: String? = null,
        text: StoryText? = null,
        storyType: String,
        user: User
    ) {
        user.userId?.let { userId ->
            val storyId = Helper.getStoryId()
            val timeStamp = System.currentTimeMillis()
            val story = Stories(
                storyId = storyId,
                userId = userId,
                storyUploadingTimeInTimeStamp = timeStamp,
                storyUploadingTimeInText = Helper.formatTimestampToDateAndTime(timeStamp),
                storyType = storyType,
                storyUri = storyUrl,
                text = text?.text,
                textBackGroundColor = text?.textBackGroundColor,
                textFontFamily = text?.textFontFamily,
                storyViewCount = 0,
                storySeenUsersId = arrayListOf()
            )

            firestore.collection(Constants.Table.Stories.name)
                .document(storyId)
                .set(story)
                .addOnCompleteListener { addDataBase ->
                    if (addDataBase.isSuccessful) {
                        MyLogger.i(tagStory, msg = "Story add in database successful !")
                        AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.StoryUploadedSuccessfully)
                    } else {
                        MyLogger.e(tagStory, msg = "Story add in database failed !")
                        AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.UploadingFail)
                    }
                }
        }
    }


//endregion


    //region:: Get All Story

    suspend fun getAndListenTop30Friend(userId: String) = callbackFlow<StoryListenerEmissionType> {
        val listOfFriend = getTop30Friend(userId)

        // In this way handle because if try return@callBackFlow it give error because you don't use awaitClose and return it 😁😁.
        if (listOfFriend.isNotEmpty()) {
            MyLogger.v(
                tagStory,
                msg = "List of friend is not empty :- $listOfFriend and  ${listOf("5z3GqeOT8ZZxr7nE4Ny231cP68J3")}"
            )
            val storiesQuery = firestore.collection(Constants.Table.Stories.name)
                .whereIn(UserTable.USERID.fieldName, listOfFriend.toList())
            val stories = storiesQuery.get().await()


            //asSequence help to iterate original list and return when iteration over.
            val userStoriesMap = stories.asSequence()
                .mapNotNull {
                    it.toObject<Stories>().userId?.let { id ->
                        id to it
                    }
                }
                .groupBy({ it.first }, { it.second.toObject<Stories>() })

            MyLogger.d(tagStory, msg = userStoriesMap, isJson = true, jsonTitle = "user story map ")

            val userStoriesList = userStoriesMap.mapNotNull { (userId, stories) ->
                UserManager.getUserById(userId)?.let { UserStories(it, stories.toMutableList()) }
            }.toMutableList()


            MyLogger.i(
                tagStory,
                msg = userStoriesList,
                isJson = true,
                jsonTitle = "User Story list !"
            )

            trySend(
                StoryListenerEmissionType(
                    Constants.StoryEmitType.Starting,
                    userStoryList = userStoriesList.toList()
                )
            )
            userStoriesList.clear()
            storyListener?.remove()
            storyListener = storiesQuery.addSnapshotListener { value, error ->

                if (error != null) return@addSnapshotListener

                if (!isFirstTimeStoryListnerCall) {
                    value?.documentChanges?.forEach { document ->
                        when (document.type) {
                            DocumentChange.Type.ADDED -> {
                                trySend(
                                    StoryListenerEmissionType(
                                        Constants.StoryEmitType.Added,
                                        story = document.document.toObject<Stories>()
                                    )
                                )
                            }

                            DocumentChange.Type.REMOVED -> {
                                trySend(
                                    StoryListenerEmissionType(
                                        Constants.StoryEmitType.Removed,
                                        story = document.document.toObject<Stories>()
                                    )
                                )
                            }

                            else -> {}
                        }
                    }
                }
                isFirstTimeStoryListnerCall = false

            }
        }


        awaitClose {
            storyListener?.remove()
            channel.close()
        }

    }

    private suspend fun getTop30Friend(userId: String): List<String> {
        return firestore.collection(Constants.Table.User.name).document(userId)
            .collection(Constants.Table.Friend.name).get().await().documents.map {
                it.id to firestore.collection(Table.User.name).document(it.id)
                    .collection(Constants.Table.Follower.name).get().await().documents.size
            }.sortedByDescending {
                it.second
            }.take(30).map {
                it.first
            }
    }


    //endregion

    //region:: Get My Story

    suspend fun getMyStory(userId: String) = flow<UserStories> {
        val storiesQuery = firestore.collection(Constants.Table.Stories.name)
            .whereEqualTo(UserTable.USERID.fieldName, userId)

        val stories = storiesQuery.get().await()

        // Map the documents to Stories objects
        val userStoriesList = stories.documents.mapNotNull {
            it.toObject<Stories>()
        }.sortedBy { it.storyUploadingTimeInTimeStamp }.toMutableList()

        // Get the user object
        val user = UserManager.getUserById(userId)
        if (user != null) {
            val userStories = UserStories(user, userStoriesList)
            MyLogger.d(tagStory, msg = userStories, isJson = true, jsonTitle = "user story map ")
            emit(userStories)
        }

    }

    suspend fun deleteStoryById(storyId: String) = callbackFlow<Pair<Boolean, String?>> {
        val storiesQuery = firestore.collection(Constants.Table.Stories.name)
            .document(storyId)
        storiesQuery.delete().addOnCompleteListener {
            if (it.isSuccessful) {
                trySend(Pair(true, null))
            } else {
                trySend(Pair(false, it.exception?.message.toString()))
            }
        }.await()

        awaitClose {
            close()
        }
    }


    //endregion

}