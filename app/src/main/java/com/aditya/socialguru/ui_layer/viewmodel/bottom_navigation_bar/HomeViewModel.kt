package com.aditya.socialguru.ui_layer.viewmodel.bottom_navigation_bar

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.story.StoryListenerEmissionType
import com.aditya.socialguru.data_layer.model.story.StoryText
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.HomeRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel(val app: Application) : AndroidViewModel(app) {


    private val tagStory = Constants.LogTag.Story
    private val repository = HomeRepository()
    private val _uploadStories = MutableLiveData<Resource<String>>()
    val uploadStories: LiveData<Resource<String>> get() = _uploadStories

    private val _userStories = MutableStateFlow<Resource<List<UserStories>>>(
        Resource.Success(
            emptyList()
        )
    )
    val userStories: StateFlow<Resource<List<UserStories>>> get() = _userStories.asStateFlow()


    fun uploadStory(
        storyType: Constants.StoryTpye,
        uri: Uri? = null,
        text: StoryText? = null,
        user: User
    ) = viewModelScope.launch {

        _uploadStories.postValue(Resource.Loading())
        MyLogger.v(tagStory, msg = "Request sending ....")
        if (SoftwareManager.isNetworkAvailable(app)) {
            MyLogger.v(tagStory, msg = "Network available !")
            repository.uploadStory(storyType, uri, text, user)
        } else {
            MyLogger.v(tagStory, msg = "Network not available !")
            _uploadStories.postValue(Resource.Error(message = "Internet not available ."))
        }

    }

    //region:: Get all story

    @OptIn(FlowPreview::class)
    suspend fun getAllStory(userId: String) = viewModelScope.launch {
        _userStories.value = Resource.Loading()
        MyLogger.v(tagStory, msg = "Request sending ....")
        if (SoftwareManager.isNetworkAvailable(app)) {
            MyLogger.v(tagStory, msg = "Network available !")
            repository.getAllStory(userId).onEach {
                MyLogger.d(tagStory, msg = it.userStoryList, isJson = true)
                _userStories.value = handleGetAllStory(it)
            }.launchIn(this)
        } else {
            MyLogger.v(tagStory, msg = "Network not available !")
            _userStories.value = Resource.Error(message = "Internet not available .")
        }
    }

    private fun handleGetAllStory(storyHandling: StoryListenerEmissionType): Resource<List<UserStories>> {

        MyLogger.v(tagStory, isFunctionCall = true)

        val userStoryList = userStories.value.data?.toMutableList() ?: mutableListOf<UserStories>()

        when (storyHandling.emitChangeType) {
            Constants.StoryEmitType.Starting -> {
                MyLogger.v(
                    tagStory,
                    msg = "This is starting story type :- ${storyHandling.userStoryList}"
                )
                storyHandling.userStoryList?.let {
                    userStoryList.addAll(it.toMutableList() as ArrayList<UserStories>)
                }
            }

            Constants.StoryEmitType.Added -> {
                MyLogger.v(tagStory, msg = "This is added story type :- ${storyHandling.story}")
                storyHandling.story?.let { story ->
                    story.userId?.let { userId ->
                        userStoryList.map {
                            if (it.user == null || it.user.userId != userId) {
                                return@map it
                            }
                            it.stories?.add(story)
                            it.stories?.sortBy { it.storyUploadingTimeInTimeStamp }
                            it

                        }
                    }
                    MyLogger.d(tagStory, msg = userStoryList, isJson = true)
                }
                MyLogger.d(tagStory, msg = userStoryList, isJson = true)

            }

            Constants.StoryEmitType.Removed -> {
                MyLogger.v(tagStory, msg = "This is removed story type")

                storyHandling.story?.let { story ->
                    story.userId?.let { userId ->
                        userStoryList.map {
                            if (it.user == null || it.user.userId != userId) {
                                return@map it
                            }
                            it.stories?.remove(story)
                            it.stories?.sortBy { it.storyUploadingTimeInTimeStamp }
                            it

                        }
                    }
                    MyLogger.d(tagStory, msg = userStoryList, isJson = true)
                }
                MyLogger.d(tagStory, msg = userStoryList, isJson = true)

            }
        }

        return Resource.Success(userStoryList.toList())
    }

    //endregion
}