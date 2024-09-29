package com.aditya.socialguru.ui_layer.viewmodel.bottom_navigation_bar

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.story.StoryListenerEmissionType
import com.aditya.socialguru.data_layer.model.story.StoryText
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.HomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel(val app: Application) : AndroidViewModel(app) {


    private val tagStory = Constants.LogTag.Story
    private val repository = HomeRepository()
    private val _uploadStories = MutableSharedFlow<Resource<String>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val uploadStories: SharedFlow<Resource<String>> get() = _uploadStories.asSharedFlow()

    private val _userStories = MutableSharedFlow<Resource<List<UserStories>>>(
        1,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val userStories: SharedFlow<Resource<List<UserStories>>> get() = _userStories.asSharedFlow()

    private var _isDataLoaded = false
     val isDataLoaded get() = _isDataLoaded


    fun uploadStory(
        storyType: Constants.StoryType,
        uri: Uri? = null,
        text: StoryText? = null,
        user: User
    ) = viewModelScope. myLaunch{
        _uploadStories.tryEmit(Resource.Loading())
        MyLogger.v(tagStory, msg = "Request sending ....")
        if (SoftwareManager.isNetworkAvailable(app)) {
            MyLogger.v(tagStory, msg = "Network available !")
            repository.uploadStory(storyType, uri, text, user)
        } else {
            MyLogger.v(tagStory, msg = "Network not available !")
            _uploadStories.tryEmit(Resource.Error(message = "Internet not available ."))
        }

    }

    init {
        MyLogger.w(msg = "Home ViewModel is initialized !")
    }

    //region:: Get all story

    @OptIn(FlowPreview::class)
     fun getAllStory(userId: String) = viewModelScope.myLaunch {
        _userStories.tryEmit(Resource.Loading())
        MyLogger.v(tagStory, msg = "Request sending ....")
        if (SoftwareManager.isNetworkAvailable(app)) {
            MyLogger.v(tagStory, msg = "Network available !")
            repository.getAllStory(userId).onEach {
                MyLogger.v(tagStory, msg = it.userStoryList, isJson = true)
                _userStories.tryEmit(handleGetAllStory(it))
            }.launchIn(this)
        } else {
            MyLogger.v(tagStory, msg = "Network not available !")
            _userStories.tryEmit(Resource.Error(message = Constants.ErrorMessage.InternetNotAvailable.message))
        }
    }

    private suspend fun handleGetAllStory(storyHandling: StoryListenerEmissionType): Resource<List<UserStories>> {

        MyLogger.v(tagStory, isFunctionCall = true)

        val userStoryList =
            userStories.replayCache[0].data?.toMutableList() ?: mutableListOf<UserStories>()

        when (storyHandling.emitChangeType) {
            Constants.StoryEmitType.Starting -> {

                MyLogger.v(
                    tagStory,
                    msg = "This is starting story type "
                )
                userStoryList.clear()
                storyHandling.userStoryList?.let {
                    userStoryList.addAll(it.toMutableList() as ArrayList<UserStories>)
                }
            }

            Constants.StoryEmitType.Added -> {
                MyLogger.v(tagStory, msg = "This is added story type")
                storyHandling.story?.let { story ->
                    story.userId?.let { userId ->
                        if (userStoryList.isEmpty()) {
                            userStoryList.add(
                                UserStories(
                                    repository.getUser(userId).first().data,
                                    listOf(story).toMutableList()
                                )
                            )
                        } else {
                            userStoryList.map {
                                if (it.user == null || it.user.userId != userId) {
                                    return@map it
                                }
                                it.stories?.add(story)
                                it.stories?.sortBy { it.storyUploadingTimeInTimeStamp }
                                it
                            }
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
                        userStoryList.forEach { userStory ->
                            if (userStory.user?.userId == userId) {
                                userStory.stories?.remove(story)
                                if (userStory.stories.isNullOrEmpty()) {
                                    userStoryList.remove(userStory)
                                } else {
                                    userStory.stories.sortBy { it.storyUploadingTimeInTimeStamp }
                                }
                            }
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


    fun setDataLoadedStatus(status:Boolean){
        _isDataLoaded=status
    }

    override fun onCleared() {
        MyLogger.e(msg = "Home viewmodel is finished !")
        super.onCleared()
    }

}