package com.aditya.socialguru.ui_layer.viewmodel.bottom_navigation_bar

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.HomeRepository
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine

class HomeViewModel(val app: Application) : AndroidViewModel(app) {


    private val tagStory= Constants.LogTag.Story
    private val repository = HomeRepository()
    private val _uploadStories = MutableLiveData<Resource<String>>()
    val uploadStories: LiveData<Resource<String>> get() = _uploadStories

    private val _userStories = MutableLiveData<Resource<List<UserStories>?>>()
    val userStories: LiveData<Resource<List<UserStories>?>> get() = _userStories


    init {
       viewModelScope.launch {
           getAllStory()
       }
    }

    fun storeImageInFirebaseStorage(image: Uri ,user: User) = viewModelScope.launch {

        _uploadStories.postValue(Resource.Loading())
        MyLogger.v(tagStory, msg = "Request sending ....")
        if (SoftwareManager.isNetworkAvailable(app)) {
            MyLogger.v(tagStory, msg = "Network available !")
            repository.storeImageInFirebaseStorage(image ,user)
        } else {
            MyLogger.v(tagStory, msg = "Network not available !")
            _uploadStories.postValue(Resource.Error(message = "Internet not available ."))
        }

    }

    //region:: Get all story

    suspend fun getAllStory()=viewModelScope.launch {
        _userStories.postValue(Resource.Loading())
        MyLogger.v(tagStory, msg = "Request sending ....")
        if (SoftwareManager.isNetworkAvailable(app)) {
            MyLogger.v(tagStory, msg = "Network available !")
            repository.getAllStory { userStories, error ->
                if (userStories!=null){
                    MyLogger.v(tagStory, msg = userStories , isJson = true)
                    _userStories.postValue(Resource.Success(userStories))
                }else{
                    _userStories.postValue(Resource.Error(message = error))
                }
            }
        } else {
            MyLogger.v(tagStory, msg = "Network not available !")
            _userStories.postValue(Resource.Error(message = "Internet not available ."))
        }
    }

    //endregion
}