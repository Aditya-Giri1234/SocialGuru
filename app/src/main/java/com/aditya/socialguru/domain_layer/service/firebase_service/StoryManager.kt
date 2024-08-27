package com.aditya.socialguru.domain_layer.service.firebase_service

import android.net.Uri
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.story.Stories
import com.aditya.socialguru.data_layer.model.story.StoryListenerEmissionType
import com.aditya.socialguru.data_layer.model.story.StoryText
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * [StoryManager] This class only do crud  operation on firebase firestore [com.aditya.socialguru.data_layer.model.story.Stories] collection and its subCollection..
 * */
object StoryManager {

    private val tagStory = Constants.LogTag.Story
    private val tagDelete = Constants.LogTag.ForceLogout


    private val myStory: UserStories? = null

    //Firebase firestore
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var storyListener: ListenerRegistration? = null
    private var isFirstTimeStoryListnerCall = true
    private var top30FriendListener: ListenerRegistration? = null

    private val StoryImageUploadingPath = "${Constants.Table.Stories.name}/Image/"
    private val StoryVideoUploadingPath = "${Constants.Table.Stories.name}/Video/"


    //region:: Upload Story

    suspend fun uploadStory(
        storyType: Constants.StoryType,
        uri: Uri? = null,
        text: StoryText? = null,
        user: User
    ) {

        when (storyType) {
            Constants.StoryType.Text -> {
                uploadTextStory(text, user)
            }

            Constants.StoryType.Image -> {
                uploadImageStory(uri, user)
            }

            Constants.StoryType.Video -> {
                uploadVideoStory(uri, user)
            }
        }
    }

    private suspend fun uploadImageStory(uri: Uri?, user: User) {
        uri ?: return
        startUploading()
        uploadToStorage(
            "${user.userId ?: AuthManager.currentUserId()}/${Constants.FolderName.StoryImage.name}",
            uri
        ).collect {
            val storyUrl = it.first
            val error = it.second
            if (storyUrl != null) {
                addStoryToDatabase(
                    storyUrl,
                    storyType = Constants.StoryType.Image.name,
                    user = user
                )
            }
        }
    }

    private suspend fun uploadVideoStory(uri: Uri?, user: User) {
        uri ?: return
        startUploading()
        uploadToStorage(
            "${user.userId ?: AuthManager.currentUserId()}/${Constants.FolderName.StoryVideo.name}",
            uri
        ).collect {
            val storyUrl = it.first
            val error = it.second
            if (storyUrl != null) {
                addStoryToDatabase(
                    storyUrl,
                    storyType = Constants.StoryType.Video.name,
                    user = user
                )
            }
        }
    }

    private fun uploadTextStory(text: StoryText?, user: User) {
        text ?: return
        startUploading()
        addStoryToDatabase(text = text, storyType = Constants.StoryType.Text.name, user = user)
    }

    private fun startUploading() {
        MyLogger.v(tagStory, isFunctionCall = true)
        MyLogger.v(tagStory, msg = "Story uploading starting ...")
        AppBroadcastHelper.setStoryUploadState(Constants.StoryUploadState.StartUploading)
    }


    private suspend fun uploadToStorage(folderName: String, uri: Uri) =
        callbackFlow<Pair<String?, String?>> {
            StorageManager.uploadImageToServer(
                Table.Stories.name,
                folderName,
                uri
            ).collect { status ->
                when (status.state) {
                    Constants.StorageManagerState.InProgress -> {
                        MyLogger.v(
                            tagStory,
                            msg = "Story uploading in progress ... ${status.progress}"
                        )
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
        getTop30Friend(userId).onEach { listOfFriend ->
            // In this way handle because if try return@callBackFlow it give error because you don't use awaitClose and return it 😁😁.
            if (listOfFriend.isNotEmpty()) {
                MyLogger.v(
                    tagStory,
                    msg = "List of friend is not empty :- $listOfFriend and  ${listOf("5z3GqeOT8ZZxr7nE4Ny231cP68J3")}"
                )
                isFirstTimeStoryListnerCall =
                    true // Reset this because friend list change all process start with fresh .
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

                MyLogger.d(
                    tagStory,
                    msg = userStoriesMap,
                    isJson = true,
                    jsonTitle = "user story map "
                )

                val userStoriesList = userStoriesMap.mapNotNull { (userId, stories) ->
                    UserManager.getUserById(userId)
                        ?.let { UserStories(it, stories.toMutableList()) }
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
            } else {
                trySend(StoryListenerEmissionType(Constants.StoryEmitType.Starting, null))
            }
        }.launchIn(this)




        awaitClose {
            storyListener?.remove()
            channel.close()
        }

    }


    private suspend fun getTop30Friend(userId: String) = callbackFlow<List<String>> {
        top30FriendListener?.remove()
        top30FriendListener = firestore.collection(Constants.Table.User.name).document(userId)
            .collection(Constants.Table.Friend.name).addSnapshotListener { value, error ->
                if (error != null) trySend(emptyList())

                val friendIds = value?.documents?.map { it.id } ?: emptyList()

                launch { // Launch a new coroutine for the asynchronous operation
                    val followerCounts = withContext(Dispatchers.IO) {
                        friendIds.map { friendId ->
                            firestore.collection(Table.User.name).document(friendId)
                                .collection(Constants.Table.Follower.name).get()
                                .await().documents.size
                        }
                    }
                    val sortedFriends = friendIds.zip(followerCounts)
                        .sortedByDescending { it.second }
                        .take(30)
                        .map { it.first }

                    trySend(sortedFriends) // Send the processed data to the callbackFlow
                }
            }

        awaitClose {
            top30FriendListener?.remove()
            close()
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
        }.sortedByDescending { it.storyUploadingTimeInTimeStamp }.toMutableList()

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

    //region:: Delete All My Stories

    suspend fun deleteAllMyStories() = callbackFlow<UpdateResponse> {
        try {
            MyLogger.e(tagDelete, isFunctionCall = true)
            val storyRef = firestore.collection(Constants.Table.Stories.name)
                .whereEqualTo(Constants.StoryTable.USER_ID.fieldName, AuthManager.currentUserId()!!)
            val stories = storyRef.get().await()

            MyLogger.d(tagStory, msg = "Storied count :- ${stories.size()}")
            if (stories.isEmpty) {
                MyLogger.i(tagDelete, msg = "No Story found , so don't do any thing.")
                trySend(UpdateResponse(true, "All Story Delete Successfully !"))
            } else {
                StorageManager.deleteMyAllStoryMedia().onEach {
                    MyLogger.e(tagDelete, msg = "All Story Media Delete Successfully !")
                    launch {
                        MyLogger.w(tagDelete, msg = it.errorMessage)
                        val storyDeleteWork = stories.map {
                            async {
                                it.reference.delete()
                            }
                        }
                        storyDeleteWork.awaitAll()
                        MyLogger.e(tagDelete, msg = "All Story Ref Delete Successfully !")
                        trySend(UpdateResponse(true, "All Story Delete Successfully !"))
                    }
                }.launchIn(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(UpdateResponse(false, e.message))
        }

        awaitClose {
            close()
        }
    }

    //endregion

    //region:: Delete All Stale Stories

    suspend fun listenMyStoryCount() = callbackFlow<Int> {
        val storyRef = firestore.collection(Constants.Table.Stories.name)
            .whereEqualTo(Constants.StoryTable.USER_ID.fieldName, AuthManager.currentUserId()!!)
        val listener = storyRef.addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener

            val size = value?.documents?.size ?: 0

            trySend(size)
        }

        awaitClose {
            listener.remove()
            close()
        }
    }

    suspend fun deleteStaleStories() = callbackFlow<UpdateResponse> {
        try {
            val userId = AuthManager.currentUserId()!!
            val storyRef = firestore.collection(Constants.Table.Stories.name)
            val myStories =
                storyRef.whereEqualTo(Constants.StoryTable.USER_ID.fieldName, userId).get().await()
            if (myStories.isEmpty) {
                trySend(UpdateResponse(true, ""))
            } else {
                val staleStoriesDeleteWork = myStories.map {
                    async {
                        val story = it.toObject<Stories>()
                        if (Helper.isOneMinuteOldStory(story.storyUploadingTimeInTimeStamp!!)) {
                            it.reference.delete()
                        }
                    }
                }
                staleStoriesDeleteWork.awaitAll()
                trySend(UpdateResponse(true, ""))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(UpdateResponse(false, e.message))
        }
        awaitClose {
            close()
        }
    }

    //endregion
}