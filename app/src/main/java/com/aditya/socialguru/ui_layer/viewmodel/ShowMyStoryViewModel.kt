package com.aditya.socialguru.ui_layer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.ShowMyStoryRepo
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ShowMyStoryViewModel(val app : Application) : AndroidViewModel(app) {

    private val tagStory = Constants.LogTag.Story
    private val repository=ShowMyStoryRepo()
    private val _myStories = MutableStateFlow<Resource<UserStories?>>(
        Resource.Success(
            null
        )
    )
    val myStories: StateFlow<Resource<UserStories?>> get() = _myStories.asStateFlow()

    private val _deleteStories = MutableSharedFlow<Resource<Pair<Boolean,String?>>>(
        0,
        64,
        BufferOverflow.DROP_OLDEST
    )
    val deleteStories: SharedFlow<Resource<Pair<Boolean,String?>>> get() = _deleteStories.asSharedFlow()


    //region::My stories

    fun getMyStory(userId: String) = viewModelScope.launch {
        _myStories.tryEmit(Resource.Loading())
        MyLogger.v(tagStory, msg = "Request sending ....")
        if (SoftwareManager.isNetworkAvailable(app)) {
            MyLogger.v(tagStory, msg = "Network available !")
            _myStories.value=Resource.Success(repository.getMyStory(userId).first())
        } else {
            MyLogger.v(tagStory, msg = "Network not available !")
            _myStories.tryEmit(
                Resource.Error<UserStories?>(message = "Internet not available ."))
        }
    }

     fun deleteStoryById(storyId: String)= viewModelScope.launch {
        _deleteStories.tryEmit(Resource.Loading())
        MyLogger.v(tagStory, msg = "Request sending ....")
        if (SoftwareManager.isNetworkAvailable(app)) {
            MyLogger.v(tagStory, msg = "Network available !")
            repository.deleteStoryById(storyId).onEach {
               _deleteStories.tryEmit(if (it.first){
                    Resource.Success()
                }else{
                    Resource.Error(it.second)
                })
            }.launchIn(this)
        } else {
            MyLogger.v(tagStory, msg = "Network not available !")
            _deleteStories.tryEmit(
                Resource.Error(message = "Internet not available ."))
        }


    }

    //endregion
}